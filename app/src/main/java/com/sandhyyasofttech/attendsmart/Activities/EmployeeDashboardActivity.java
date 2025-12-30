package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeDashboardActivity extends AppCompatActivity {

    // UI
    private TextView tvWelcome, tvCompany, tvRole, tvShift, tvTodayStatus, tvCurrentTime, tvLocation;
    private MaterialButton btnCheckIn, btnCheckOut;

    // Firebase
    private DatabaseReference employeesRef, attendanceRef, shiftsRef;
    private StorageReference attendancePhotoRef;

    // Data
    private String companyKey;
    private String employeeMobile;
    private String shiftStart;  // ✅ NULL until Firebase loads
    private String shiftEnd;    // ✅ NULL until Firebase loads

    private String todayStatus = "Absent";
    private String pendingAction = "";
    private String currentAddress = "Getting location...";
    private double currentLat = 0, currentLng = 0;

    private Bitmap currentPhotoBitmap;

    // Camera
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // Clock
    private Handler timeHandler;
    private Runnable timeRunnable;
    private boolean locationReady = false;
    private int LATE_MINUTES = 15;        // Late threshold
    private int HALF_DAY_MINUTES = 240;   // 4 hours minimum
    private int EARLY_OUT_MINUTES = 30;   // Must stay 30min before end

    // ----------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);

        initViews();
        setupFirebase();
        setupLocation();
        requestLocationPermission();
        loadEmployeeData();
        startClock();
    }

    // ----------------------------------------------------
    private String calculateAttendanceStatus(String shiftStart, String checkInTime, String checkOutTime) {
        if (checkInTime == null) return "Absent";

        long lateMins = getDiffMinutes(shiftStart, checkInTime);

        // Check-in Status (Live)
        if (checkOutTime == null) {
            if (lateMins <= 15) return "Present (In)";
            if (lateMins <= 60) return "Late (In)";
            return "Late (In)";
        }

        // Check-out Status (Final)
        long totalMins = getDiffMinutes(checkInTime, checkOutTime);
        if (totalMins < 240) return "Half Day";
        if (totalMins < 360) return "Present";
        return "Full Day";
    }
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvCompany = findViewById(R.id.tvCompany);
        tvRole = findViewById(R.id.tvRole);
        tvShift = findViewById(R.id.tvShift);
        tvTodayStatus = findViewById(R.id.tvTodayStatus);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvLocation = findViewById(R.id.tvLocation); // 🆕 Location TextView
        MaterialButton btnAttendanceReport = findViewById(R.id.btnAttendanceReport);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);  // ✅ Add this

        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnCheckOut = findViewById(R.id.btnCheckOut);

        btnCheckIn.setEnabled(false);
        btnCheckOut.setEnabled(false);

        btnCheckIn.setOnClickListener(v -> tryCheckIn());
        btnCheckOut.setOnClickListener(v -> tryCheckOut());
        btnAttendanceReport.setOnClickListener(v -> openAttendanceReport());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());  // ✅ Confirmation

    }

    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout?")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes, Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        PrefManager pref = new PrefManager(this);
        pref.logout();  // ✅ Clears all saved data

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    // 🆕 ATTENDANCE REPORT
    private void openAttendanceReport() {
        if (employeeMobile == null || companyKey == null) {
            toast("Loading profile first...");
            return;
        }

        Intent intent = new Intent(this, AttendanceReportActivity.class);
        intent.putExtra("companyKey", companyKey);
        intent.putExtra("employeeMobile", employeeMobile);
        intent.putExtra("employeeEmail", new PrefManager(this).getEmployeeEmail());
        startActivity(intent);
    }

    // 🆕 LOCATION SETUP
    private void setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    locationReady = true;
                    tvLocation.setText(String.format("📍 %.4f, %.4f", currentLat, currentLng));
                    getAddressFromLatLng(currentLat, currentLng);
                }
            }
        };
    }
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                        locationReady = true;
                        tvLocation.setText(String.format("📍 %.4f, %.4f", currentLat, currentLng));
                        getAddressFromLatLng(currentLat, currentLng);
                        startLocationUpdates(); // Continuous updates
                    } else {
                        startLocationUpdates(); // No last location, start updates
                    }
                })
                .addOnFailureListener(e -> {
                    toast("GPS Error: " + e.getMessage());
                    startLocationUpdates();
                });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_CODE);
        } else {
            getCurrentLocation(); // ✅ Start GPS immediately
        }
    }

    private void getAddressFromLatLng(double lat, double lng) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String addr = address.getAddressLine(0);
                    if (addr != null) {
                        currentAddress = addr;
                        runOnUiThread(() ->
                                tvLocation.setText("📍 " + addr.substring(0, 40) + "..."));
                    }
                }
            } catch (Exception ignored) {
                // Address fail - coordinates enough
            }
        }).start();
    }

    private void updateAddress(double lat, double lng) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String fullAddress = String.format("%s, %s",
                            address.getAddressLine(0),
                            address.getLocality());
                    runOnUiThread(() -> {
                        currentAddress = fullAddress;
                        tvLocation.setText("📍 " + currentAddress);
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> tvLocation.setText("📍 Lat: " + lat + ", Lng: " + lng));
            }
        }).start();
    }

    // ----------------------------------------------------

    private void setupFirebase() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        employeesRef = db.getReference("Companies").child(companyKey).child("employees");
        attendanceRef = db.getReference("Companies").child(companyKey).child("attendance");
        shiftsRef = db.getReference("Companies").child(companyKey).child("shifts");

        attendancePhotoRef = FirebaseStorage.getInstance()
                .getReference()
                .child("Companies")
                .child(companyKey)
                .child("attendance_photos");
    }
    private void loadEmployeeData() {
        String email = new PrefManager(this).getEmployeeEmail();

        // ✅ FIXED: Load ALL employees (only 3 in your DB) - NO query needed
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    toast("No employees found");
                    return;
                }

                // Find employee by looping (only 3 employees)
                for (DataSnapshot emp : snapshot.getChildren()) {
                    DataSnapshot info = emp.child("info");
                    String empEmail = info.child("employeeEmail").getValue(String.class);

                    if (email.equals(empEmail)) {
                        employeeMobile = emp.getKey();

                        // ✅ UI Update
                        tvWelcome.setText("Welcome, " + info.child("employeeName").getValue(String.class));
                        tvCompany.setText("Company: " + companyKey.replace(",", "."));
                        tvRole.setText("Role: " + info.child("employeeRole").getValue(String.class));

                        // ✅ SHIFT LOADING - Your exact Firebase structure
                        String employeeShift = info.child("employeeShift").getValue(String.class);
                        toast("DEBUG Found: " + employeeShift); // Remove later

                        loadShiftFromEmployeeData(emp); // Pass full employee snapshot
//                        btnCheckIn.setEnabled(true);
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toast("Load failed: " + error.getMessage());
            }
        });
    }

    // 🆕 NEW: Load shift from employee data
// 🆕 NEW: Load shift from employee data - REAL shift based on name, no default
    private void loadShiftFromEmployeeData(DataSnapshot emp) {
        String employeeShift = emp.child("info").child("employeeShift").getValue(String.class);

        if (employeeShift == null || employeeShift.isEmpty()) {
            tvShift.setText("Shift: Not assigned");
            loadTodayStatus();
            return;
        }

        // 1) Extract shift name before "("  👉 e.g. "Morning (09:00 AM - 06:00 PM)" -> "Morning"
        String shiftName;
        int idx = employeeShift.indexOf("(");
        if (idx > 0) {
            shiftName = employeeShift.substring(0, idx).trim();
        } else {
            shiftName = employeeShift;
        }

        // 2) Read shift from Firebase: Companies/{companyKey}/shifts/{shiftName}
        shiftsRef.child(shiftName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (s.exists()) {
                    // ✅ Use ONLY real times from DB – no defaults
                    String dbStart = s.child("startTime").getValue(String.class);
                    String dbEnd = s.child("endTime").getValue(String.class);

                    if (dbStart != null && dbEnd != null) {
                        shiftStart = dbStart;
                        shiftEnd = dbEnd;

                        tvShift.setText("Shift: " + shiftName + " (" + shiftStart + " - " + shiftEnd + ")");
                        toast("Shift loaded: " + shiftStart + " - " + shiftEnd);
                    } else {
                        // If DB is broken, fall back to parsing label
                        parseShiftString(employeeShift);
                    }
                } else {
                    // If shift node not found, fall back to parsing label
                    parseShiftString(employeeShift);
                }

                // After shift is resolved, load today's status
                loadTodayStatus();
                btnCheckIn.setEnabled(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                parseShiftString(employeeShift);
                loadTodayStatus();
            }
        });
    }

    // 🆕 FIXED Parser
// Backup parser only when node missing
    private void parseShiftString(String shiftString) {
        if (shiftString == null) {
            tvShift.setText("Shift: Not assigned");
            return;
        }

        try {
            String timePattern = "\\(([^)]+)\\)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(timePattern);
            java.util.regex.Matcher matcher = pattern.matcher(shiftString);

            if (matcher.find()) {
                String times = matcher.group(1);                 // "09:00 AM - 06:00 PM"
                String[] parts = times.split(" - ");
                if (parts.length == 2) {
                    shiftStart = parts[0].trim();
                    shiftEnd = parts[1].trim();
                    tvShift.setText("Shift: " + shiftString);
                    toast("Shift parsed from label");
                    return;
                }
            }
        } catch (Exception ignored) {}

        tvShift.setText("Shift: Not assigned");
    }

    private void loadTodayStatus() {
        if (employeeMobile == null) return;

        String today = getTodayDate();
        attendanceRef.child(today).child(employeeMobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        String checkIn = s.child("checkInTime").getValue(String.class);
                        String checkOut = s.child("checkOutTime").getValue(String.class);

                        todayStatus = calculateAttendanceStatus(shiftStart, checkIn, checkOut);
                        boolean in = checkIn != null;
                        boolean out = checkOut != null;
                        updateUI(in, out);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    private void updateUI(boolean in, boolean out) {
        tvTodayStatus.setText("Today: " + todayStatus);

        int color;
        switch (todayStatus) {
            case "Full Day": case "Present": case "Present (In)":
                color = R.color.green; break;
            case "Late": case "Late (In)": case "Multiple In":
                color = R.color.orange; break;
            case "Half Day":
                color = R.color.yellow; break;
            default:
                color = R.color.red; break;
        }
        tvTodayStatus.setTextColor(ContextCompat.getColor(this, color));

        // ✅ BOTH BUTTONS ALWAYS ENABLED (Multiple punches)
        btnCheckIn.setEnabled(locationReady);
        btnCheckOut.setEnabled(locationReady);
    }
//    private void tryCheckIn() {
//        if (!locationReady) {
//            toast("⏳ Wait for GPS location");
//            return;
//        }
//        if (!withinWindow(shiftStart, 60)) {
//            toast("⏰ Check-in allowed ±60 min");
//            return;
//        }
//        openCamera("checkIn");
//    }
//    private void tryCheckOut() {
//        if (!withinWindow(shiftEnd, 120)) {
//            toast("⏰ Check-out allowed ±2 hrs");
//            return;
//        }
//        openCamera("checkOut");
//    }

    private void tryCheckIn() {
        if (!isInternetAvailable()) {
            toast("❌ No Internet");
            return;
        }
        if (!locationReady) {
            toast("⏳ GPS Loading...");
            return;
        }
        openCamera("checkIn");
    }

    private void tryCheckOut() {
        if (!isInternetAvailable()) {
            toast("❌ No Internet");
            return;
        }
        if (!locationReady) {
            toast("⏳ GPS Loading...");
            return;
        }
        openCamera("checkOut");
    }


    private boolean withinWindow(String base, int grace) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Calendar now = Calendar.getInstance();
            Calendar baseCal = Calendar.getInstance();
            baseCal.setTime(sdf.parse(base));
            baseCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

            Calendar from = (Calendar) baseCal.clone();
            Calendar to = (Calendar) baseCal.clone();
            from.add(Calendar.MINUTE, -grace);
            to.add(Calendar.MINUTE, grace);

            return now.after(from) && now.before(to);
        } catch (Exception e) {
            return true;
        }
    }
    private void openCamera(String action) {
        pendingAction = action;

        // Check both permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_CODE);
            return;
        }

        startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                getCurrentLocation(); // ✅ GPS start
            } else {
                tvLocation.setText("📍 Location OFF");
                toast("📍 GPS permission required");
            }
        }
    }



    // 🆕 START LOCATION UPDATES
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (employeeMobile == null) {
            toast("Please wait, loading profile...");
            return;
        }

        if (requestCode == CAMERA_REQUEST_CODE &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getExtras() != null) {

            currentPhotoBitmap = (Bitmap) data.getExtras().get("data");

            uploadPhotoAndSaveAttendance();
        }
    }

    // ----------------------------------------------------

    private void uploadPhotoAndSaveAttendance() {

        if (employeeMobile == null || currentPhotoBitmap == null) {
            toast("Photo capture failed");
            return;
        }

        String today = getTodayDate();
        String time = getCurrentTime();

        String photoName = employeeMobile + "_" + pendingAction + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference photoRef = attendancePhotoRef.child(today).child(photoName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currentPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] data = baos.toByteArray();

        toast("📤 Uploading photo + GPS location...");

        photoRef.putBytes(data)
                .addOnSuccessListener(task ->
                        photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            saveAttendance(uri.toString(), time);
                        }))
                .addOnFailureListener(e -> toast("Photo upload failed"));
    }

    // 🆕 SAVE ATTENDANCE WITH LOCATION
    private void saveAttendance(String photoUrl, String time) {
        String today = getTodayDate();
        DatabaseReference node = attendanceRef.child(today).child(employeeMobile);

        node.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                boolean alreadyCheckedIn = s.hasChild("checkInTime");
                boolean alreadyCheckedOut = s.hasChild("checkOutTime");

                if (pendingAction.equals("checkIn")) {
                    // ✅ MULTIPLE CHECK-IN ALLOWED
                    String status = alreadyCheckedIn ? "Multiple In" : calculateAttendanceStatus(shiftStart, time, null);

                    // Save/Overwrite Check-in
                    node.child("checkInTime").setValue(time);
                    node.child("checkInPhoto").setValue(photoUrl);
                    node.child("status").setValue(status);

                    // GPS Data
                    node.child("checkInLat").setValue(currentLat);
                    node.child("checkInLng").setValue(currentLng);
                    node.child("checkInAddress").setValue(currentAddress);

                    toast("✅ Check-in: " + status);

                } else if (pendingAction.equals("checkOut")) {
                    // ✅ MULTIPLE CHECK-OUT ALLOWED
                    String inTime = s.child("checkInTime").getValue(String.class);
                    if (inTime == null) {
                        toast("⚠️ First Check-in करा!");
                        return;
                    }

                    // Calculate FINAL status based on total hours
                    String finalStatus = calculateAttendanceStatus(shiftStart, inTime, time);
                    long totalMins = getDiffMinutes(inTime, time);

                    // Save Check-out
                    node.child("checkOutTime").setValue(time);
                    node.child("checkOutPhoto").setValue(photoUrl);
                    node.child("status").setValue(finalStatus);
                    node.child("totalMinutes").setValue(totalMins);
                    node.child("totalHours").setValue(String.format("%.1f", totalMins / 60.0));

                    // GPS Data
                    node.child("checkOutLat").setValue(currentLat);
                    node.child("checkOutLng").setValue(currentLng);
                    node.child("checkOutAddress").setValue(currentAddress);

                    toast("✅ Check-out Complete!\nStatus: " + finalStatus + "\nTotal: " + (totalMins/60) + "h");
                }

                loadTodayStatus(); // Refresh UI
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toast("Save failed!");
            }
        });
    }

    // ----------------------------------------------------

    private long getDiffMinutes(String start, String end) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            return Math.max(0,
                    (sdf.parse(end).getTime() - sdf.parse(start).getTime()) / 60000);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date());
    }
    private void startClock() {
        timeHandler = new Handler();
        timeRunnable = () -> {
            tvCurrentTime.setText(
                    new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(new Date()));
            timeHandler.postDelayed(timeRunnable, 1000);
        };
        timeHandler.post(timeRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationReady && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null) timeHandler.removeCallbacks(timeRunnable);
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }


    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
