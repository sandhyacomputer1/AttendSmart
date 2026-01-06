package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import android.content.IntentSender;


public class EmployeeDashboardActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvWelcome, tvEmployeeName, tvCompany, tvRole, tvShift;
    private TextView tvTodayStatus, tvCurrentTime, tvLocation, tvWorkHours;
    private TextView tvCheckInTime, tvCheckOutTime;
    private CardView cardCheckIn, cardCheckOut, cardAttendanceReport, cardLogout;
    private View statusIndicator, locationStatusDot;

    // Firebase
    private DatabaseReference employeesRef, attendanceRef, shiftsRef;
    private StorageReference attendancePhotoRef;

    // Data
    private String companyKey, employeeMobile, employeeName;
    private String shiftStart, shiftEnd;
    private String checkInTime, checkOutTime, finalStatus = "Not Checked In";
    private String currentAddress = "Getting location...";
    private double currentLat = 0, currentLng = 0;
    private Bitmap currentPhotoBitmap;
    private boolean isCheckedIn = false;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean locationReady = false;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    private static final int REQUEST_CHECK_SETTINGS = 103;
    private String pendingAction = "";

    // Timers
    private Handler timeHandler, workTimerHandler;
    private Runnable timeRunnable, workTimerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);

        initViews();
        setupFirebase();
        fetchCompanyName();
        setupLocation();
        requestLocationPermission();
        loadEmployeeData();
        loadTodayAttendance();
        startClock();
        updateGreeting();
    }
    private void fetchCompanyName() {
        FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("companyInfo")
                .child("companyName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String companyName = snapshot.getValue(String.class);
                        if (companyName != null && !companyName.trim().isEmpty()) {
                            tvCompany.setText(companyName);  // "Sandhyaaaa"
                        } else {
                            tvCompany.setText(companyKey.replace(",", "."));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void saveEmployeeFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.isEmpty()) return;

                    FirebaseDatabase.getInstance()
                            .getReference("Companies")
                            .child(companyKey)
                            .child("employees")
                            .child(employeeMobile)
                            .child("info")
                            .child("fcmToken")
                            .setValue(token);
                });
    }

    private void initViews() {
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvLocation = findViewById(R.id.tvLocation);
        locationStatusDot = findViewById(R.id.locationStatusDot);
        tvTodayStatus = findViewById(R.id.tvTodayStatus);
        tvWorkHours = findViewById(R.id.tvWorkHours);
        tvShift = findViewById(R.id.tvShift);
        statusIndicator = findViewById(R.id.statusIndicator);
        cardCheckIn = findViewById(R.id.cardCheckIn);
        cardCheckOut = findViewById(R.id.cardCheckOut);
        tvCheckInTime = findViewById(R.id.tvCheckInTime);
        tvCheckOutTime = findViewById(R.id.tvCheckOutTime);
        tvCompany = findViewById(R.id.tvCompany);
        tvRole = findViewById(R.id.tvRole);
        cardAttendanceReport = findViewById(R.id.cardAttendanceReport);
        cardLogout = findViewById(R.id.cardLogout);

        cardCheckIn.setOnClickListener(v -> checkApprovedLeaveThenCheckIn());
        cardCheckOut.setOnClickListener(v -> tryCheckOut());
        cardAttendanceReport.setOnClickListener(v -> openAttendanceReport());
        cardLogout.setOnClickListener(v -> showLogoutConfirmation());

        View btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(EmployeeDashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        updateButtonStates();
    }

    private void updateGreeting() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good Morning" :
                hour < 17 ? "Good Afternoon" : "Good Evening";
        tvWelcome.setText(greeting);
    }

    private void setupFirebase() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        employeesRef = db.getReference("Companies").child(companyKey).child("employees");
        attendanceRef = db.getReference("Companies").child(companyKey).child("attendance");
        shiftsRef = db.getReference("Companies").child(companyKey).child("shifts");
        attendancePhotoRef = FirebaseStorage.getInstance()
                .getReference().child("Companies").child(companyKey).child("attendance_photos");
    }

    private void setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setWaitForAccurateLocation(true).setMinUpdateIntervalMillis(2000).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    if (!locationReady) {
                        locationReady = true;
                        updateButtonStates();
                    }
                    tvLocation.setText(String.format("%.4f, %.4f", currentLat, currentLng));
                    locationStatusDot.setBackgroundResource(R.drawable.status_dot_active);
                    getAddressFromLatLng(currentLat, currentLng);
                }
            }
        };
    }

    private void updateButtonStates() {

        boolean hasCheckIn = checkInTime != null && !checkInTime.isEmpty();
        boolean hasCheckOut = checkOutTime != null && !checkOutTime.isEmpty();

        boolean canCheckIn = locationReady && !hasCheckIn;
        boolean canCheckOut = locationReady && hasCheckIn && !hasCheckOut;

        cardCheckIn.setEnabled(canCheckIn);
        cardCheckIn.setAlpha(canCheckIn ? 1f : 0.4f);

        cardCheckOut.setEnabled(canCheckOut);
        cardCheckOut.setAlpha(canCheckOut ? 1f : 0.4f);

        Log.d("ATTENDANCE_LOCK",
                "locationReady=" + locationReady +
                        ", checkIn=" + checkInTime +
                        ", checkOut=" + checkOutTime +
                        ", canCheckIn=" + canCheckIn +
                        ", canCheckOut=" + canCheckOut);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            checkLocationSettings();
        }
    }

    private void getCurrentLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        201
                );
            }
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                locationReady = true;
                tvLocation.setText(String.format("%.4f, %.4f", currentLat, currentLng));
                locationStatusDot.setBackgroundResource(R.drawable.status_dot_active);
                getAddressFromLatLng(currentLat, currentLng);
            }
            startLocationUpdates();
            updateButtonStates();
        }).addOnFailureListener(e -> {
            toast("GPS Error");
            startLocationUpdates();
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void getAddressFromLatLng(double lat, double lng) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    currentAddress = addresses.get(0).getAddressLine(0);
                    runOnUiThread(() -> {
                        String shortAddr = currentAddress.length() > 35 ? currentAddress.substring(0, 35) + "..." : currentAddress;
                        tvLocation.setText(shortAddr);
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadEmployeeData() {
        String email = new PrefManager(this).getEmployeeEmail();

        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot emp : snapshot.getChildren()) {
                    DataSnapshot info = emp.child("info");
                    String empEmail = info.child("employeeEmail").getValue(String.class);

                    if (email.equals(empEmail)) {
                        employeeMobile = emp.getKey();
                        employeeName = info.child("employeeName").getValue(String.class);

                        saveEmployeeFcmToken();

                        tvEmployeeName.setText(employeeName != null ? employeeName : "User");
                        tvRole.setText(info.child("employeeRole").getValue(String.class));

                        loadCompanyName();
                        loadShiftFromEmployeeData(emp);
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

    private void loadCompanyName() {
        FirebaseDatabase.getInstance().getReference("Companies")
                .child(companyKey)
                .child("companyInfo")
                .child("companyName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String companyName = snapshot.getValue(String.class);
                        if (companyName != null && !companyName.isEmpty()) {
                            tvCompany.setText(companyName);
                        } else {
                            tvCompany.setText(companyKey.replace(",", "."));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvCompany.setText(companyKey.replace(",", "."));
                    }
                });
    }

    private void loadShiftFromEmployeeData(DataSnapshot emp) {
        String employeeShift = emp.child("info").child("employeeShift").getValue(String.class);
        if (employeeShift == null || employeeShift.isEmpty()) {
            tvShift.setText("Not assigned");
            loadTodayAttendance();
            return;
        }

        String shiftName = employeeShift.contains("(") ?
                employeeShift.substring(0, employeeShift.indexOf("(")).trim() : employeeShift;

        shiftsRef.child(shiftName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                shiftStart = s.child("startTime").getValue(String.class);
                shiftEnd = s.child("endTime").getValue(String.class);
                if (shiftStart != null && shiftEnd != null) {
                    tvShift.setText(shiftStart + " - " + shiftEnd);
                } else {
                    parseShiftString(employeeShift);
                }
                loadTodayAttendance();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                parseShiftString(employeeShift);
                loadTodayAttendance();
            }
        });
    }

    private void parseShiftString(String shiftString) {
        try {
            String timePattern = "\\(([^)]+)\\)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(timePattern);
            java.util.regex.Matcher matcher = pattern.matcher(shiftString);
            if (matcher.find()) {
                String[] parts = matcher.group(1).split(" - ");
                if (parts.length == 2) {
                    shiftStart = parts[0].trim();
                    shiftEnd = parts[1].trim();
                    tvShift.setText(shiftStart + " - " + shiftEnd);
                }
            }
        } catch (Exception ignored) {}
        if (shiftStart == null) tvShift.setText("Not assigned");
    }

    private void loadTodayAttendance() {

        if (employeeMobile == null) return;

        String today = getTodayDate();

        attendanceRef.child(today).child(employeeMobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        checkInTime = snapshot.child("checkInTime").getValue(String.class);
                        checkOutTime = snapshot.child("checkOutTime").getValue(String.class);
                        finalStatus = snapshot.child("status").getValue(String.class);

                        isCheckedIn = (checkInTime != null && !checkInTime.isEmpty())
                                && (checkOutTime == null || checkOutTime.isEmpty());

                        updateUI();

                        if (isCheckedIn) {
                            startWorkTimer();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateUI() {
        tvCheckInTime.setText(checkInTime != null ? checkInTime : "Not marked");
        tvCheckOutTime.setText(checkOutTime != null ? checkOutTime : "Not marked");

        String displayStatus;
        int statusColor = ContextCompat.getColor(this, R.color.red);
        int dotDrawable = R.drawable.status_dot_absent;

        if (checkInTime == null) {
            displayStatus = "Not Checked In";
        } else if (checkOutTime == null) {
            String lateStatus = finalStatus != null && finalStatus.equals("Late") ? "Late" : "Present";
            displayStatus = lateStatus;
            statusColor = lateStatus.equals("Late") ? ContextCompat.getColor(this, R.color.orange) :
                    ContextCompat.getColor(this, R.color.green);
            dotDrawable = lateStatus.equals("Late") ? R.drawable.status_dot_late : R.drawable.status_dot_present;
        } else {
            displayStatus = finalStatus != null ? finalStatus : "Completed";
            if (finalStatus != null && finalStatus.contains("Late")) {
                statusColor = ContextCompat.getColor(this, R.color.orange);
                dotDrawable = R.drawable.status_dot_late;
            } else {
                switch (finalStatus) {
                    case "Full Day":
                    case "Present":
                        statusColor = ContextCompat.getColor(this, R.color.green);
                        dotDrawable = R.drawable.status_dot_present;
                        break;
                    case "Half Day":
                        statusColor = ContextCompat.getColor(this, R.color.yellow);
                        dotDrawable = R.drawable.status_dot_halfday;
                        break;
                }
            }
        }

        tvTodayStatus.setText(displayStatus);
        tvTodayStatus.setTextColor(statusColor);
        statusIndicator.setBackgroundResource(dotDrawable);
        updateButtonStates();

        if (checkInTime != null && checkOutTime != null) {
            long totalMins = getDiffMinutes(checkInTime, checkOutTime);
            tvWorkHours.setText(String.format("%dh %dm", totalMins / 60, totalMins % 60));
        }
    }

    private void checkApprovedLeaveThenCheckIn() {
        String todayDate = getTodayDate();

        DatabaseReference leaveRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("leaves");

        leaveRef.orderByChild("employeeMobile")
                .equalTo(employeeMobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String status = s.child("status").getValue(String.class);
                            String fromDate = s.child("fromDate").getValue(String.class);
                            String toDate = s.child("toDate").getValue(String.class);

                            if ("APPROVED".equals(status) && isDateBetween(todayDate, fromDate, toDate)) {
                                toast("❌ You are on approved leave today.\nCheck-in is disabled");
                                return;
                            }
                        }
                        tryCheckIn();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tryCheckIn();
                    }
                });
    }

    private void tryCheckIn() {

        if (!isInternetAvailable()) {
            toast("❌ No Internet");
            return;
        }

        if (!locationReady) {
            toast("⏳ Waiting for GPS");
            return;
        }

        if (checkInTime != null && !checkInTime.isEmpty()) {
            toast("⚠️ Check-in already marked today");
            return;
        }

        String currentTime = getCurrentTime();
        String lateStatus = isLateCheckIn(shiftStart, currentTime) ? "Late" : "Present";

        tvTodayStatus.setText(lateStatus);
        tvTodayStatus.setTextColor(lateStatus.equals("Late")
                ? ContextCompat.getColor(this, R.color.orange)
                : ContextCompat.getColor(this, R.color.green));

        statusIndicator.setBackgroundResource(
                lateStatus.equals("Late")
                        ? R.drawable.status_dot_late
                        : R.drawable.status_dot_present
        );

        toast("📸 Taking photo for " + lateStatus + " check-in...");
        openCamera("checkIn");
    }

    private void tryCheckOut() {

        if (!isInternetAvailable()) {
            toast("❌ No Internet");
            return;
        }

        if (!locationReady) {
            toast("⏳ Waiting for GPS");
            return;
        }

        if (checkInTime == null || checkInTime.isEmpty()) {
            toast("⚠️ Please check-in first");
            return;
        }

        if (checkOutTime != null && !checkOutTime.isEmpty()) {
            toast("⚠️ Already checked out today");
            return;
        }

        openCamera("checkOut");
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private void openCamera(String action) {
        pendingAction = action;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
            return;
        }
        startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) granted = false;
            }
            if (granted) {
                checkLocationSettings();
            } else {
                tvLocation.setText("Location denied");
                toast("📍 GPS permission required");
            }
        }
    }

    private void checkLocationSettings() {
        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> getCurrentLocation());

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ((ResolvableApiException) e).startResolutionForResult(
                            EmployeeDashboardActivity.this,
                            REQUEST_CHECK_SETTINGS
                    );
                } catch (IntentSender.SendIntentException sendEx) {
                    // ignore
                }
            } else {
                toast("📍 Enable location to continue");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                getCurrentLocation();
            } else {
                tvLocation.setText("Location off");
                toast("📍 Please enable location to mark attendance");
            }
            return;
        }

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK
                && data != null && data.getExtras() != null) {
            currentPhotoBitmap = (Bitmap) data.getExtras().get("data");
            if (currentPhotoBitmap != null) uploadPhotoAndSaveAttendance();
        }
    }

    private void uploadPhotoAndSaveAttendance() {
        String today = getTodayDate();
        String time = getCurrentTime();
        String photoName = employeeMobile + "_" + pendingAction + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference photoRef = attendancePhotoRef.child(today).child(photoName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currentPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] data = baos.toByteArray();

        toast("📤 Uploading...");
        photoRef.putBytes(data).addOnSuccessListener(task ->
                        photoRef.getDownloadUrl().addOnSuccessListener(uri ->
                                saveAttendance(uri.toString(), time)))
                .addOnFailureListener(e -> toast("❌ Upload failed"));
    }

    private void saveAttendance(String photoUrl, String time) {

        if (employeeMobile == null) {
            toast("Profile not loaded");
            return;
        }

        String today = getTodayDate();
        DatabaseReference node = attendanceRef.child(today).child(employeeMobile);

        node.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // 🔒 HARD DB LOCK
                if (pendingAction.equals("checkIn")
                        && snapshot.child("checkInTime").exists()) {
                    toast("⚠️ Check-in already exists");
                    return;
                }

                if (pendingAction.equals("checkOut")
                        && snapshot.child("checkOutTime").exists()) {
                    toast("⚠️ Check-out already exists");
                    return;
                }

                if (pendingAction.equals("checkIn")) {

                    String lateStatus = isLateCheckIn(shiftStart, time) ? "Late" : "Present";

                    node.child("checkInTime").setValue(time);
                    node.child("checkInPhoto").setValue(photoUrl);
                    node.child("lateStatus").setValue(lateStatus);
                    node.child("status").setValue(lateStatus);
                    node.child("checkInLat").setValue(currentLat);
                    node.child("checkInLng").setValue(currentLng);
                    node.child("checkInAddress").setValue(currentAddress);
                    node.child("markedBy").setValue("Employee");

                    checkInTime = time;
                    isCheckedIn = true;

                    toast("✅ Checked In (" + lateStatus + ")");
                    startWorkTimer();
                }

                else if (pendingAction.equals("checkOut")) {

                    String existingCheckIn = snapshot.child("checkInTime").getValue(String.class);
                    String lateStatus = snapshot.child("lateStatus").getValue(String.class);
                    String markedBy = snapshot.child("markedBy").getValue(String.class);

                    long totalMins = getDiffMinutes(existingCheckIn, time);
                    String finalStatus = getFinalStatus(totalMins);
                    String displayStatus = finalStatus +
                            (lateStatus != null && lateStatus.equals("Late") ? " (Late)" : "");

                    node.child("checkOutTime").setValue(time);
                    node.child("checkOutPhoto").setValue(photoUrl);
                    node.child("finalStatus").setValue(finalStatus);
                    node.child("status").setValue(displayStatus);
                    node.child("totalMinutes").setValue(totalMins);
                    node.child("totalHours").setValue(String.format("%.1f", totalMins / 60.0));
                    node.child("checkOutLat").setValue(currentLat);
                    node.child("checkOutLng").setValue(currentLng);
                    node.child("checkOutAddress").setValue(currentAddress);
                    node.child("shiftStart").setValue(shiftStart);
                    node.child("shiftEnd").setValue(shiftEnd);

                    if ("Admin".equals(markedBy)) {
                        node.child("markedBy").setValue("Admin+Employee");
                    }

                    checkOutTime = time;
                    isCheckedIn = false;

                    toast("✅ Checked Out\n" + displayStatus);
                    stopWorkTimer();
                }

                loadTodayAttendance();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toast("❌ Save failed");
            }
        });
    }

    private boolean isLateCheckIn(String shiftStartTime, String checkInTime) {
        if (shiftStartTime == null || shiftStartTime.isEmpty()) {
            return false;
        }
        try {
            long diffMins = getDiffMinutes(shiftStartTime, checkInTime);
            return diffMins > 5;
        } catch (Exception e) {
            return false;
        }
    }

    private String getFinalStatus(long totalMinutes) {
        if (totalMinutes < 240) {
            return "Half Day";
        } else if (totalMinutes >= 480) {
            return "Full Day";
        } else {
            return "Present";
        }
    }

    private long getDiffMinutes(String start, String end) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date startDate = sdf.parse(start);
            Date endDate = sdf.parse(end);
            if (startDate == null || endDate == null) return 0;
            return Math.max(0, (endDate.getTime() - startDate.getTime()) / 60000);
        } catch (ParseException | NullPointerException e) {
            return 0;
        }
    }

    private void startWorkTimer() {
        workTimerHandler = new Handler();
        workTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCheckedIn && checkInTime != null) {
                    String currentTime = getCurrentTime();
                    long workMins = getDiffMinutes(checkInTime, currentTime);
                    tvWorkHours.setText(String.format("%dh %dm", workMins / 60, workMins % 60));
                }
                if (workTimerHandler != null) {
                    workTimerHandler.postDelayed(this, 60000);
                }
            }
        };
        workTimerHandler.post(workTimerRunnable);
    }

    private void stopWorkTimer() {
        if (workTimerHandler != null && workTimerRunnable != null) {
            workTimerHandler.removeCallbacks(workTimerRunnable);
        }
    }

    private void openAttendanceReport() {
        Intent intent = new Intent(this, AttendanceReportActivity.class);
        intent.putExtra("companyKey", companyKey);
        intent.putExtra("employeeMobile", employeeMobile);
        startActivity(intent);
    }

    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (d, w) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        new PrefManager(this).logout();
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
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
            tvCurrentTime.setText(new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(new Date()));
            timeHandler.postDelayed(timeRunnable, 1000);
        };
        timeHandler.post(timeRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationReady) startLocationUpdates();
        loadTodayAttendance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null) timeHandler.removeCallbacks(timeRunnable);
        stopWorkTimer();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private boolean isDateBetween(String today, String from, String to) {
        if (from == null || to == null) return false;
        return today.compareTo(from) >= 0 && today.compareTo(to) <= 0;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}