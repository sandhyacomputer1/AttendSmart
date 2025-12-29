package com.sandhyyasofttech.attendsmart.Fragments;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.location.*;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    // UI Elements - NULL CHECKS ADDED
    private TextView tvWelcome, tvCompany, tvRole, tvShift, tvTodayStatus, tvCurrentTime, tvLocation, tvPendingAction;
    private MaterialButton btnCheckIn, btnCheckOut;
    private View fragmentView; // ✅ NEW: Store view reference

    // Firebase
    private DatabaseReference employeesRef, attendanceRef, shiftsRef;
    private StorageReference attendancePhotoRef;

    // Data
    private String companyKey, employeeMobile, shiftStart = "09:00 AM", shiftEnd = "06:00 PM";
    private String todayStatus = "Absent", pendingAction = "", currentAddress = "Getting location...";
    private double currentLat = 0, currentLng = 0;
    private Bitmap currentPhotoBitmap;
    private boolean locationReady = false;
    private boolean viewsInitialized = false; // ✅ NEW: Track view initialization

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Handler timeHandler;
    private Runnable timeRunnable;

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // ✅ FIXED: Initialize views FIRST, THEN everything else
        initViews(fragmentView);
        viewsInitialized = true; // ✅ Mark views ready

        setupFirebase();
        setupLocation();
        requestLocationPermission();
        loadEmployeeData();
        startClock();

        return fragmentView;
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvCompany = view.findViewById(R.id.tvCompany);
        tvRole = view.findViewById(R.id.tvRole);
        tvShift = view.findViewById(R.id.tvShift);
        tvTodayStatus = view.findViewById(R.id.tvTodayStatus);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvPendingAction = view.findViewById(R.id.tvPendingAction);
        btnCheckIn = view.findViewById(R.id.btnCheckIn);
        btnCheckOut = view.findViewById(R.id.btnCheckOut);

        // ✅ SAFE DEFAULTS
        if (tvLocation != null) tvLocation.setText("📍 Getting location...");
        if (tvTodayStatus != null) tvTodayStatus.setText("Today: Absent");
        if (tvPendingAction != null) tvPendingAction.setText("Check In Available");

        if (btnCheckIn != null) {
            btnCheckIn.setEnabled(false);
            btnCheckIn.setOnClickListener(v -> tryCheckIn());
        }
        if (btnCheckOut != null) {
            btnCheckOut.setEnabled(false);
            btnCheckOut.setOnClickListener(v -> tryCheckOut());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if ((requestCode == LOCATION_PERMISSION_CODE || requestCode == CAMERA_PERMISSION_CODE) && viewsInitialized) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                getCurrentLocation();
            } else if (tvLocation != null) {
                tvLocation.setText("📍 Location OFF");
                toast("📍 GPS & Camera permissions required");
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == getActivity().RESULT_OK &&
                data != null && data.getExtras() != null && viewsInitialized) {
            currentPhotoBitmap = (Bitmap) data.getExtras().get("data");
            uploadPhotoAndSaveAttendance();
        }
    }

    // ✅ FIXED: SAFE UI UPDATES with null checks
    private void safeUpdateLocationText(String text) {
        if (viewsInitialized && tvLocation != null) {
            requireActivity().runOnUiThread(() -> tvLocation.setText(text));
        }
    }

    private void setupFirebase() {
        PrefManager pref = new PrefManager(requireContext());
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

    private void setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && viewsInitialized) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    locationReady = true;
                    safeUpdateLocationText(String.format("📍 %.4f, %.4f", currentLat, currentLng));
                    getAddressFromLatLng(currentLat, currentLng);
                }
            }
        };
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (!viewsInitialized || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && viewsInitialized) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                        locationReady = true;
                        safeUpdateLocationText(String.format("📍 %.4f, %.4f", currentLat, currentLng));
                        getAddressFromLatLng(currentLat, currentLng);
                        startLocationUpdates();
                    } else {
                        startLocationUpdates();
                    }
                })
                .addOnFailureListener(e -> {
                    toast("GPS Error: " + e.getMessage());
                    if (viewsInitialized) startLocationUpdates();
                });
    }

    private void startLocationUpdates() {
        if (!viewsInitialized || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || locationCallback == null) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void getAddressFromLatLng(double lat, double lng) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String addr = address.getAddressLine(0);
                    if (addr != null) {
                        currentAddress = addr;
                        safeUpdateLocationText("📍 " + addr.substring(0, Math.min(40, addr.length())));
                    }
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void loadEmployeeData() {
        String email = new PrefManager(requireContext()).getEmployeeEmail();

        employeesRef.orderByChild("info/employeeEmail").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists() || !viewsInitialized) {
                            if (viewsInitialized) toast("Employee not found");
                            return;
                        }

                        for (DataSnapshot emp : snapshot.getChildren()) {
                            employeeMobile = emp.getKey();
                            DataSnapshot info = emp.child("info");

                            if (tvWelcome != null) {
                                tvWelcome.setText("Welcome, " + info.child("employeeName").getValue(String.class));
                            }
                            if (tvCompany != null) {
                                tvCompany.setText("Company: " + companyKey.replace(",", "."));
                            }
                            if (tvRole != null) {
                                tvRole.setText("Role: " + info.child("employeeRole").getValue(String.class));
                            }

                            String shiftKey = info.child("shiftKey").getValue(String.class);
                            if (shiftKey != null) {
                                loadShift(shiftKey);
                            } else {
                                loadTodayStatus();
                            }
                            if (btnCheckIn != null) {
                                btnCheckIn.setEnabled(true);
                            }
                            break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (viewsInitialized) toast("Failed to load employee");
                    }
                });
    }

    private void loadShift(String shiftKey) {
        shiftsRef.child(shiftKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (!viewsInitialized) return;

                shiftStart = s.child("startTime").getValue(String.class);
                shiftEnd = s.child("endTime").getValue(String.class);
                if (tvShift != null) {
                    tvShift.setText("Shift: " + shiftStart + " - " + shiftEnd);
                }
                loadTodayStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTodayStatus() {
        if (employeeMobile == null || !viewsInitialized) return;

        String today = getTodayDate();

        attendanceRef.child(today).child(employeeMobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        if (!viewsInitialized) return;

                        boolean in = s.hasChild("checkInTime");
                        boolean out = s.hasChild("checkOutTime");

                        if (!in) todayStatus = "Absent";
                        else if (in && !out) {
                            long late = getDiffMinutes(shiftStart, s.child("checkInTime").getValue(String.class));
                            todayStatus = late > 15 ? "Late" : "Present (In)";
                        } else todayStatus = "Present";

                        updateUI(in, out);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateUI(boolean in, boolean out) {
        if (!viewsInitialized) return;

        if (tvTodayStatus != null) {
            tvTodayStatus.setText("Today: " + todayStatus);
            int color = todayStatus.contains("Present") ? R.color.green :
                    todayStatus.equals("Late") ? R.color.orange : R.color.red;
            tvTodayStatus.setTextColor(ContextCompat.getColor(requireContext(), color));
        }

        if (btnCheckIn != null) btnCheckIn.setEnabled(!in && locationReady);
        if (btnCheckOut != null) btnCheckOut.setEnabled(in && !out);

        if (tvPendingAction != null) {
            tvPendingAction.setText(in ? "Check Out Available" : "Check In Available");
        }
    }

    private void tryCheckIn() {
        if (!viewsInitialized || !locationReady) {
            toast("⏳ Wait for GPS location");
            return;
        }
        if (!withinWindow(shiftStart, 60)) {
            toast("⏰ Check-in allowed ±60 min");
            return;
        }
        openCamera("checkIn");
    }

    private void tryCheckOut() {
        if (!viewsInitialized || !withinWindow(shiftEnd, 120)) {
            toast("⏰ Check-out allowed ±2 hrs");
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

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    CAMERA_PERMISSION_CODE);
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    private void uploadPhotoAndSaveAttendance() {
        if (employeeMobile == null || currentPhotoBitmap == null || !viewsInitialized) {
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

    private void saveAttendance(String photoUrl, String time) {
        String today = getTodayDate();
        DatabaseReference node = attendanceRef.child(today).child(employeeMobile);

        node.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (!viewsInitialized) return;

                if (pendingAction.equals("checkIn") && !s.hasChild("checkInTime")) {
                    long late = getDiffMinutes(shiftStart, time);

                    node.child("checkInTime").setValue(time);
                    node.child("checkInPhoto").setValue(photoUrl);
                    node.child("status").setValue(late > 15 ? "Late" : "Present");

                    node.child("checkInLat").setValue(currentLat);
                    node.child("checkInLng").setValue(currentLng);
                    node.child("checkInAddress").setValue(currentAddress);
                    node.child("checkInGPS").setValue(true);

                    toast("✅ Check-in saved with GPS location");

                } else if (pendingAction.equals("checkOut")
                        && s.hasChild("checkInTime")
                        && !s.hasChild("checkOutTime")) {

                    String inTime = s.child("checkInTime").getValue(String.class);
                    long mins = getDiffMinutes(inTime, time);

                    node.child("checkOutTime").setValue(time);
                    node.child("checkOutPhoto").setValue(photoUrl);
                    node.child("totalMinutes").setValue(mins);
                    node.child("totalHours").setValue(String.format(Locale.US, "%.2f", mins / 60.0));

                    node.child("checkOutLat").setValue(currentLat);
                    node.child("checkOutLng").setValue(currentLng);
                    node.child("checkOutAddress").setValue(currentAddress);
                    node.child("checkOutGPS").setValue(true);

                    toast("✅ Check-out saved with GPS location");
                }

                loadTodayStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

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
        if (!viewsInitialized || tvCurrentTime == null) return;

        timeHandler = new Handler();
        timeRunnable = () -> {
            if (viewsInitialized && tvCurrentTime != null) {
                tvCurrentTime.setText(
                        new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(new Date()));
            }
            timeHandler.postDelayed(timeRunnable, 1000);
        };
        timeHandler.post(timeRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewsInitialized && locationReady && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewsInitialized = false; // ✅ Reset flag
        if (timeHandler != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}
