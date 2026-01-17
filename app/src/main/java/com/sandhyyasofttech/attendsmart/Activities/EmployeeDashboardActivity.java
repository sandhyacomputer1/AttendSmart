//
//package com.sandhyyasofttech.attendsmart.Activities;
//
//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.location.Address;
//import android.location.Geocoder;
//import android.location.Location;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.net.ConnectivityManager;
//import android.content.Context;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.cardview.widget.CardView;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.location.Priority;
//import com.google.firebase.database.*;
//import com.google.firebase.messaging.FirebaseMessaging;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//import java.io.ByteArrayOutputStream;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//
//import com.google.android.gms.common.api.ResolvableApiException;
//import com.google.android.gms.location.LocationSettingsRequest;
//import com.google.android.gms.location.LocationSettingsResponse;
//import com.google.android.gms.location.SettingsClient;
//import com.google.android.gms.tasks.Task;
//import android.content.IntentSender;
//
//public class EmployeeDashboardActivity extends AppCompatActivity {
//
//    // UI Elements
//    private TextView tvWelcome, tvEmployeeName, tvCompany, tvRole, tvShift;
//    private TextView tvTodayStatus, tvCurrentTime, tvLocation, tvWorkHours;
//    private TextView tvCheckInTime, tvCheckOutTime;
//    private CardView cardCheckIn, cardCheckOut, cardAttendanceReport, cardLogout;
//    private View statusIndicator, locationStatusDot;
//
//    // Firebase
//    private DatabaseReference employeesRef, attendanceRef, shiftsRef;
//    private StorageReference attendancePhotoRef;
//
//    // Data
//    private String companyKey, employeeMobile, employeeName;
//    private String shiftStart, shiftEnd;
//    private int shiftDurationMinutes = 540; // Default 9 hours (will be calculated dynamically)
//    private int halfDayMinutes = 270; // Default 4.5 hours (will be calculated as half of shift)
//    private String checkInTime, checkOutTime, finalStatus = "Not Checked In";
//    private String currentAddress = "Getting location...";
//    private double currentLat = 0, currentLng = 0;
//    private Bitmap currentPhotoBitmap;
//    private boolean isCheckedIn = false;
//    private String markedBy = "";
//
//    private String lateStatus = "";
//
//    // Location
//    private FusedLocationProviderClient fusedLocationClient;
//    private LocationCallback locationCallback;
//    private LocationRequest locationRequest;
//    private boolean locationReady = false;
//    private static final int LOCATION_PERMISSION_CODE = 101;
//    private static final int CAMERA_REQUEST_CODE = 102;
//    private static final int REQUEST_CHECK_SETTINGS = 103;
//    private String pendingAction = "";
//
//    // Timers
//    private Handler timeHandler, workTimerHandler;
//    private Runnable timeRunnable, workTimerRunnable;
//
//    private TextView tvMonthPresent, tvLeaveBalance;
//    private ImageView btnMyLeaves;
//    private TextView tvMonthLate, tvAvgWorkHours, tvOnTimePercent;
//    private ProgressBar progressPresent, progressLate, progressAvgHours, progressOnTime;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_employee_dashboard);
//
//        initViews();
//        setupFirebase();
//        fetchCompanyName();
//        setupLocation();
//        requestLocationPermission();
//        loadEmployeeData();
//        loadTodayAttendance();
//        startClock();
//        updateGreeting();
//    }
//
//    private void fetchCompanyName() {
//        FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("companyInfo")
//                .child("companyName")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        String companyName = snapshot.getValue(String.class);
//                        if (companyName != null && !companyName.trim().isEmpty()) {
//                            tvCompany.setText(companyName);
//                        } else {
//                            tvCompany.setText(companyKey.replace(",", "."));
//                        }
//                    }
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {}
//                });
//    }
//
//    private void saveEmployeeFcmToken() {
//        FirebaseMessaging.getInstance().getToken()
//                .addOnSuccessListener(token -> {
//                    if (token == null || token.isEmpty()) return;
//
//                    FirebaseDatabase.getInstance()
//                            .getReference("Companies")
//                            .child(companyKey)
//                            .child("employees")
//                            .child(employeeMobile)
//                            .child("info")
//                            .child("fcmToken")
//                            .setValue(token);
//                });
//    }
//
//    private void initViews() {
//        tvCurrentTime = findViewById(R.id.tvCurrentTime);
//        tvWelcome = findViewById(R.id.tvWelcome);
//        tvEmployeeName = findViewById(R.id.tvEmployeeName);
//        tvLocation = findViewById(R.id.tvLocation);
//        locationStatusDot = findViewById(R.id.locationStatusDot);
//        tvTodayStatus = findViewById(R.id.tvTodayStatus);
//        tvWorkHours = findViewById(R.id.tvWorkHours);
//        tvShift = findViewById(R.id.tvShift);
//        statusIndicator = findViewById(R.id.statusIndicator);
//        cardCheckIn = findViewById(R.id.cardCheckIn);
//        cardCheckOut = findViewById(R.id.cardCheckOut);
//        tvCheckInTime = findViewById(R.id.tvCheckInTime);
//        tvCheckOutTime = findViewById(R.id.tvCheckOutTime);
//        tvCompany = findViewById(R.id.tvCompany);
//        tvRole = findViewById(R.id.tvRole);
//        cardAttendanceReport = findViewById(R.id.cardAttendanceReport);
//        cardLogout = findViewById(R.id.cardLogout);
//
//        tvMonthPresent = findViewById(R.id.tvMonthPresent);
//        tvLeaveBalance = findViewById(R.id.tvLeaveBalance);
//        btnMyLeaves = findViewById(R.id.btnMyLeaves);
//
//        cardCheckIn.setOnClickListener(v -> checkApprovedLeaveThenCheckIn());
//        cardCheckOut.setOnClickListener(v -> tryCheckOut());
//        cardAttendanceReport.setOnClickListener(v -> openAttendanceReport());
//        cardLogout.setOnClickListener(v -> showLogoutConfirmation());
//
//        tvMonthLate = findViewById(R.id.tvMonthLate);
//        tvAvgWorkHours = findViewById(R.id.tvAvgWorkHours);
//        tvOnTimePercent = findViewById(R.id.tvOnTimePercent);
//        progressPresent = findViewById(R.id.progressPresent);
//        progressLate = findViewById(R.id.progressLate);
//        progressAvgHours = findViewById(R.id.progressAvgHours);
//        progressOnTime = findViewById(R.id.progressOnTime);
//
//        View btnMenu = findViewById(R.id.btnMenu);
//        btnMenu.setOnClickListener(v -> {
//            Intent intent = new Intent(EmployeeDashboardActivity.this, SettingsActivity.class);
//            startActivity(intent);
//        });
//        btnMyLeaves.setOnClickListener(v -> openMyLeaves());
//
//        findViewById(R.id.cardDailyReport).setOnClickListener(v -> openTodaysWork());
//        findViewById(R.id.cardApplyLeave).setOnClickListener(v -> openApplyLeave());
//
//        updateButtonStates();
//        loadMonthlyAttendance();
//        loadLeaveBalance();
//    }
//
//    private void updateGreeting() {
//        Calendar cal = Calendar.getInstance();
//        int hour = cal.get(Calendar.HOUR_OF_DAY);
//        String greeting = hour < 12 ? "Good Morning" :
//                hour < 17 ? "Good Afternoon" : "Good Evening";
//        tvWelcome.setText(greeting);
//    }
//
//    private void openTodaysWork() {
//        Intent intent = new Intent(this, EmployeeTodayWorkActivity.class);
//        intent.putExtra("companyKey", companyKey);
//        intent.putExtra("employeeMobile", employeeMobile);
//        startActivity(intent);
//    }
//
//    private void openApplyLeave() {
//        Intent intent = new Intent(this, ApplyLeaveActivity.class);
//        intent.putExtra("companyKey", companyKey);
//        intent.putExtra("employeeMobile", employeeMobile);
//        startActivity(intent);
//    }
//
//    private void openMyLeaves() {
//        Intent intent = new Intent(this, MyLeavesActivity.class);
//        startActivity(intent);
//    }
//
//    private void setupFirebase() {
//        PrefManager pref = new PrefManager(this);
//        companyKey = pref.getCompanyKey();
//        FirebaseDatabase db = FirebaseDatabase.getInstance();
//        employeesRef = db.getReference("Companies").child(companyKey).child("employees");
//        attendanceRef = db.getReference("Companies").child(companyKey).child("attendance");
//        shiftsRef = db.getReference("Companies").child(companyKey).child("shifts");
//        attendancePhotoRef = FirebaseStorage.getInstance()
//                .getReference().child("Companies").child(companyKey).child("attendance_photos");
//    }
//
//    private void setupLocation() {
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
//                .setWaitForAccurateLocation(true).setMinUpdateIntervalMillis(2000).build();
//
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(@NonNull LocationResult locationResult) {
//                Location location = locationResult.getLastLocation();
//                if (location != null) {
//                    currentLat = location.getLatitude();
//                    currentLng = location.getLongitude();
//                    if (!locationReady) {
//                        locationReady = true;
//                        updateButtonStates();
//                    }
//                    tvLocation.setText(String.format("%.4f, %.4f", currentLat, currentLng));
//                    locationStatusDot.setBackgroundResource(R.drawable.status_dot_active);
//                    getAddressFromLatLng(currentLat, currentLng);
//                }
//            }
//        };
//    }
//
//    private void updateButtonStates() {
//        boolean hasCheckIn = checkInTime != null && !checkInTime.isEmpty();
//        boolean hasCheckOut = checkOutTime != null && !checkOutTime.isEmpty();
//
//        boolean adminMarkedOnlyStatus = "Admin".equals(markedBy) &&
//                !hasCheckIn && !hasCheckOut &&
//                finalStatus != null && !finalStatus.isEmpty();
//
//        boolean adminMarkedCheckIn = "Admin".equals(markedBy) && hasCheckIn;
//        boolean adminMarkedComplete = "Admin".equals(markedBy) && hasCheckIn && hasCheckOut;
//
//        boolean canCheckIn = false;
//        boolean canCheckOut = false;
//
//        if (adminMarkedOnlyStatus) {
//            canCheckIn = false;
//            canCheckOut = false;
//        } else if (adminMarkedComplete) {
//            canCheckIn = false;
//            canCheckOut = false;
//        } else if (adminMarkedCheckIn) {
//            canCheckIn = false;
//            canCheckOut = locationReady && !hasCheckOut;
//        } else {
//            canCheckIn = locationReady && !hasCheckIn;
//            canCheckOut = locationReady && hasCheckIn && !hasCheckOut;
//        }
//
//        cardCheckIn.setEnabled(canCheckIn);
//        cardCheckIn.setAlpha(canCheckIn ? 1f : 0.4f);
//
//        cardCheckOut.setEnabled(canCheckOut);
//        cardCheckOut.setAlpha(canCheckOut ? 1f : 0.4f);
//    }
//
//    private void requestLocationPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                    LOCATION_PERMISSION_CODE);
//        } else {
//            checkLocationSettings();
//        }
//    }
//
//    private void loadMonthlyAttendance() {
//        if (employeeMobile == null) {
//            Log.e("STATS_ERROR", "employeeMobile is NULL!");
//            return;
//        }
//
//        Log.d("STATS_DEBUG", "Loading stats for: " + employeeMobile);
//
//        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
//        Log.d("STATS_DEBUG", "Current month: " + currentMonth);
//
//        Calendar cal = Calendar.getInstance();
//        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
//        Log.d("STATS_DEBUG", "Days in month: " + daysInMonth);
//
//        attendanceRef.orderByKey().startAt(currentMonth + "-01").endAt(currentMonth + "-31")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        int presentCount = 0;
//                        int lateCount = 0;
//                        int onTimeCount = 0;
//                        long totalMinutes = 0;
//                        int daysWithHours = 0;
//                        int totalWorkDays = 0;
//
//                        Log.d("STATS_DEBUG", "Total date snapshots: " + snapshot.getChildrenCount());
//
//                        for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
//                            DataSnapshot empData = dateSnapshot.child(employeeMobile);
//                            if (empData.exists()) {
//                                String finalStatus = empData.child("finalStatus").getValue(String.class);
//                                String lateStatus = empData.child("lateStatus").getValue(String.class);
//                                String checkInTime = empData.child("checkInTime").getValue(String.class);
//                                String status = empData.child("status").getValue(String.class);
//                                Long minutes = empData.child("totalMinutes").getValue(Long.class);
//
//                                Log.d("STATS_DEBUG", "Date: " + dateSnapshot.getKey() +
//                                        ", CheckInTime: " + checkInTime +
//                                        ", FinalStatus: " + finalStatus +
//                                        ", LateStatus: " + lateStatus +
//                                        ", Status: " + status +
//                                        ", Minutes: " + minutes);
//
//                                // Count Present days - use finalStatus as primary, fallback to status
//                                String effectiveStatus = (finalStatus != null) ? finalStatus : status;
//
//                                if (effectiveStatus != null &&
//                                        (effectiveStatus.equalsIgnoreCase("Present") ||
//                                                effectiveStatus.equalsIgnoreCase("Half Day"))) {
//
//                                    presentCount++;
//                                    totalWorkDays++;
//
//                                    // Determine if late - check both lateStatus and status suffix
//                                    boolean isLate = false;
//
//                                    // 1. Check lateStatus field first
//                                    if (lateStatus != null && lateStatus.equals("Late")) {
//                                        isLate = true;
//                                    }
//                                    // 2. Check if status field contains "(Late)" suffix
//                                    else if (status != null && status.contains("(Late)")) {
//                                        isLate = true;
//                                    }
//
//                                    if (isLate) {
//                                        lateCount++;
//                                    } else {
//                                        onTimeCount++;
//                                    }
//
//                                    Log.d("STATS_DEBUG", "Counted as: " + (isLate ? "Late" : "On-time"));
//                                } else {
//                                    Log.d("STATS_DEBUG", "Not counted as Present (effectiveStatus: " + effectiveStatus + ")");
//                                }
//
//                                // Sum work hours - use totalMinutes if available, otherwise calculate
//                                if (minutes != null && minutes > 0) {
//                                    totalMinutes += minutes;
//                                    daysWithHours++;
//                                    Log.d("STATS_DEBUG", "Added minutes: " + minutes + ", Total now: " + totalMinutes);
//                                } else if (checkInTime != null && finalStatus != null &&
//                                        (finalStatus.equals("Present") || finalStatus.equals("Half Day"))) {
//                                    // Calculate minutes from shift if checkOutTime not available but marked present
//                                    // Estimate based on shift duration
//                                    if (finalStatus.equals("Present")) {
//                                        totalMinutes += shiftDurationMinutes;
//                                        daysWithHours++;
//                                        Log.d("STATS_DEBUG", "Estimated full shift minutes: " + shiftDurationMinutes);
//                                    } else if (finalStatus.equals("Half Day")) {
//                                        totalMinutes += (shiftDurationMinutes / 2); // Half of shift
//                                        daysWithHours++;
//                                        Log.d("STATS_DEBUG", "Estimated half shift minutes: " + (shiftDurationMinutes / 2));
//                                    }
//                                }
//                            }
//                        }
//
//                        Log.d("STATS_DEBUG", "Final counts - Present: " + presentCount +
//                                ", Late: " + lateCount +
//                                ", OnTime: " + onTimeCount +
//                                ", TotalMinutes: " + totalMinutes +
//                                ", DaysWithHours: " + daysWithHours);
//
//                        // Create final variables for use in runOnUiThread
//                        final int finalPresentCount = presentCount;
//                        final int finalLateCount = lateCount;
//                        final int finalOnTimeCount = onTimeCount;
//                        final long finalTotalMinutes = totalMinutes;
//                        final int finalDaysWithHours = daysWithHours;
//                        final int finalDaysInMonth = daysInMonth;
//
//                        // Update UI on main thread
//                        runOnUiThread(() -> {
//                            // Update Present Days
//                            tvMonthPresent.setText(String.valueOf(finalPresentCount));
//                            animateProgress(progressPresent, finalPresentCount, finalDaysInMonth);
//
//                            // Update Late Days - max should be present count or 1 if no presents
//                            tvMonthLate.setText(String.valueOf(finalLateCount));
//                            int lateMax = finalPresentCount > 0 ? finalPresentCount : 1;
//                            animateProgress(progressLate, finalLateCount, lateMax);
//
//                            // Update Average Hours
//                            if (finalDaysWithHours > 0) {
//                                double avgHours = (double) finalTotalMinutes / finalDaysWithHours / 60.0;
//                                String avgText = String.format(Locale.getDefault(), "%.1f", avgHours);
//                                tvAvgWorkHours.setText(avgText);
//
//                                // Animate progress based on avg hours (max 9 hours)
//                                int progressValue = Math.min((int) (avgHours * 10), 90); // Convert to 0-90 scale
//                                animateProgress(progressAvgHours, progressValue, 90);
//                                Log.d("STATS_DEBUG", "Avg Hours: " + avgText + ", Progress: " + progressValue);
//                            } else {
//                                tvAvgWorkHours.setText("0.0");
//                                if (progressAvgHours != null) {
//                                    progressAvgHours.setProgress(0);
//                                }
//                            }
//
//                            // Update On-time Percentage
//                            if (finalPresentCount > 0) {
//                                int onTimePercent = (int) ((finalOnTimeCount * 100.0) / finalPresentCount);
//                                tvOnTimePercent.setText(String.valueOf(onTimePercent));
//                                animateProgress(progressOnTime, onTimePercent, 100);
//                                Log.d("STATS_DEBUG", "On-time %: " + onTimePercent + "% (" +
//                                        finalOnTimeCount + "/" + finalPresentCount + ")");
//                            } else {
//                                tvOnTimePercent.setText("0");
//                                if (progressOnTime != null) {
//                                    progressOnTime.setProgress(0);
//                                }
//                            }
//                        });
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e("STATS_ERROR", "Firebase error: " + error.getMessage());
//                        runOnUiThread(() -> {
//                            if (tvMonthPresent != null) tvMonthPresent.setText("0");
//                            if (tvMonthLate != null) tvMonthLate.setText("0");
//                            if (tvAvgWorkHours != null) tvAvgWorkHours.setText("0.0");
//                            if (tvOnTimePercent != null) tvOnTimePercent.setText("0");
//                            if (progressPresent != null) progressPresent.setProgress(0);
//                            if (progressLate != null) progressLate.setProgress(0);
//                            if (progressAvgHours != null) progressAvgHours.setProgress(0);
//                            if (progressOnTime != null) progressOnTime.setProgress(0);
//                        });
//                    }
//                });
//    }
//    private void animateProgress(ProgressBar progressBar, int value, int max) {
//        if (progressBar == null) return;
//
//        progressBar.setMax(max);
//
//        // Ensure value doesn't exceed max
//        if (value > max) {
//            value = max;
//        }
//
//        android.animation.ObjectAnimator animation = android.animation.ObjectAnimator.ofInt(
//                progressBar, "progress", 0, value);
//        animation.setDuration(1000); // 1 second animation
//        animation.setInterpolator(new android.view.animation.DecelerateInterpolator());
//        animation.start();
//    }
//
//    private void loadLeaveBalance() {
//        if (employeeMobile == null) return;
//
//        employeesRef.child(employeeMobile).child("info").child("leaveBalance")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        Integer balance = snapshot.getValue(Integer.class);
//                        if (balance != null) {
//                            tvLeaveBalance.setText(String.valueOf(balance));
//                        } else {
//                            // Fallback: calculate from attendance if leave balance not set
//                            calculateLeaveBalanceFromAttendance();
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        tvLeaveBalance.setText("0");
//                    }
//                });
//    }
//
//    private void calculateLeaveBalanceFromAttendance() {
//        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
//
//        attendanceRef.orderByKey().startAt(currentMonth + "-01").endAt(currentMonth + "-31")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        int absentCount = 0;
//                        int totalWorkingDays = 0;
//                        Calendar cal = Calendar.getInstance();
//
//                        // Count working days (excluding weekends - optional)
//                        // This is a simplified version, you might want to exclude holidays too
//                        for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
//                            DataSnapshot empData = dateSnapshot.child(employeeMobile);
//                            if (empData.exists()) {
//                                String finalStatus = empData.child("finalStatus").getValue(String.class);
//                                String status = empData.child("status").getValue(String.class);
//
//                                String effectiveStatus = (finalStatus != null) ? finalStatus : status;
//
//                                if (effectiveStatus != null) {
//                                    totalWorkingDays++;
//                                    if (effectiveStatus.equalsIgnoreCase("Absent")) {
//                                        absentCount++;
//                                    }
//                                }
//                            }
//                        }
//
//                        // Simple leave balance calculation: 1.75 days per month (21 days per year)
//                        int monthlyLeaveAllocation = 2; // Approximate
//                        int estimatedBalance = Math.max(0, monthlyLeaveAllocation - absentCount);
//                        tvLeaveBalance.setText(String.valueOf(estimatedBalance));
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        tvLeaveBalance.setText("0");
//                    }
//                });
//    }
//
//    private void logAttendanceData(String date, DataSnapshot empData) {
//        Log.d("STATS_DEBUG", "=== Attendance Data for " + date + " ===");
//        for (DataSnapshot child : empData.getChildren()) {
//            Log.d("STATS_DEBUG", child.getKey() + " = " + child.getValue());
//        }
//        Log.d("STATS_DEBUG", "=================================");
//    }
//
//    private void getCurrentLocation() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
//                    != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 201);
//            }
//        }
//        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
//            if (location != null) {
//                currentLat = location.getLatitude();
//                currentLng = location.getLongitude();
//                locationReady = true;
//                tvLocation.setText(String.format("%.4f, %.4f", currentLat, currentLng));
//                locationStatusDot.setBackgroundResource(R.drawable.status_dot_active);
//                getAddressFromLatLng(currentLat, currentLng);
//            }
//            startLocationUpdates();
//            updateButtonStates();
//        }).addOnFailureListener(e -> {
//            toast("GPS Error");
//            startLocationUpdates();
//        });
//    }
//
//    private void startLocationUpdates() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) return;
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
//    }
//
//    private void getAddressFromLatLng(double lat, double lng) {
//        new Thread(() -> {
//            try {
//                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
//                if (addresses != null && !addresses.isEmpty()) {
//                    currentAddress = addresses.get(0).getAddressLine(0);
//                    runOnUiThread(() -> {
//                        String shortAddr = currentAddress.length() > 35 ?
//                                currentAddress.substring(0, 35) + "..." : currentAddress;
//                        tvLocation.setText(shortAddr);
//                    });
//                }
//            } catch (Exception ignored) {}
//        }).start();
//    }
//
//    private void loadEmployeeData() {
//        String email = new PrefManager(this).getEmployeeEmail();
//
//        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot emp : snapshot.getChildren()) {
//                    DataSnapshot info = emp.child("info");
//                    String empEmail = info.child("employeeEmail").getValue(String.class);
//
//                    if (email.equals(empEmail)) {
//                        employeeMobile = emp.getKey();
//                        employeeName = info.child("employeeName").getValue(String.class);
//
//                        saveEmployeeFcmToken();
//
//                        tvEmployeeName.setText(employeeName != null ? employeeName : "User");
//                        tvRole.setText(info.child("employeeRole").getValue(String.class));
//
//                        loadCompanyName();
//                        loadMonthlyAttendance();
//
//                        loadShiftFromEmployeeData(emp);
//                        break;
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                toast("Load failed: " + error.getMessage());
//            }
//        });
//    }
//
//    private void loadCompanyName() {
//        FirebaseDatabase.getInstance().getReference("Companies")
//                .child(companyKey)
//                .child("companyInfo")
//                .child("companyName")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        String companyName = snapshot.getValue(String.class);
//                        if (companyName != null && !companyName.isEmpty()) {
//                            tvCompany.setText(companyName);
//                        } else {
//                            tvCompany.setText(companyKey.replace(",", "."));
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        tvCompany.setText(companyKey.replace(",", "."));
//                    }
//                });
//    }
//
//    private void loadShiftFromEmployeeData(DataSnapshot emp) {
//        String employeeShift = emp.child("info").child("employeeShift").getValue(String.class);
//        if (employeeShift == null || employeeShift.isEmpty()) {
//            tvShift.setText("Not assigned");
//            loadTodayAttendance();
//            return;
//        }
//
//        String shiftName = employeeShift.contains("(") ?
//                employeeShift.substring(0, employeeShift.indexOf("(")).trim() : employeeShift;
//
//        shiftsRef.child(shiftName).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot s) {
//                shiftStart = s.child("startTime").getValue(String.class);
//                shiftEnd = s.child("endTime").getValue(String.class);
//
//                if (shiftStart != null && shiftEnd != null) {
//                    tvShift.setText(shiftStart + " - " + shiftEnd);
//                    calculateShiftDuration(shiftStart, shiftEnd);
//                } else {
//                    parseShiftString(employeeShift);
//                }
//                loadTodayAttendance();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                parseShiftString(employeeShift);
//                loadTodayAttendance();
//            }
//        });
//    }
//
//    private void calculateShiftDuration(String startTime, String endTime) {
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//            Date start = sdf.parse(startTime);
//            Date end = sdf.parse(endTime);
//
//            if (start != null && end != null) {
//                long diffMillis = end.getTime() - start.getTime();
//
//                // Handle next day shift (night shift)
//                if (diffMillis < 0) {
//                    diffMillis += 24 * 60 * 60 * 1000; // Add 24 hours
//                }
//
//                shiftDurationMinutes = (int) (diffMillis / 60000);
//
//                // Half day is NOT half of shift - it's 1 minute less than full shift
//                // Employee must complete FULL shift to be marked Present
//                halfDayMinutes = 0; // We'll use direct comparison instead
//
//                Log.d("SHIFT_CALC", "Shift Duration: " + shiftDurationMinutes + " mins (" +
//                        (shiftDurationMinutes/60) + "h " + (shiftDurationMinutes%60) + "m)");
//            }
//        } catch (ParseException e) {
//            shiftDurationMinutes = 540; // Default 9 hours
//            halfDayMinutes = 0;
//            Log.e("SHIFT_CALC", "Parse error, using defaults");
//        }
//    }
//    private void parseShiftString(String shiftString) {
//        try {
//            String timePattern = "\\(([^)]+)\\)";
//            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(timePattern);
//            java.util.regex.Matcher matcher = pattern.matcher(shiftString);
//            if (matcher.find()) {
//                String[] parts = matcher.group(1).split(" - ");
//                if (parts.length == 2) {
//                    shiftStart = parts[0].trim();
//                    shiftEnd = parts[1].trim();
//                    tvShift.setText(shiftStart + " - " + shiftEnd);
//                    calculateShiftDuration(shiftStart, shiftEnd);
//                }
//            }
//        } catch (Exception ignored) {}
//        if (shiftStart == null) tvShift.setText("Not assigned");
//    }
//    private void loadTodayAttendance() {
//        if (employeeMobile == null) return;
//
//        String today = getTodayDate();
//
//        attendanceRef.child(today).child(employeeMobile)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        checkInTime = snapshot.child("checkInTime").getValue(String.class);
//                        checkOutTime = snapshot.child("checkOutTime").getValue(String.class);
//                        finalStatus = snapshot.child("status").getValue(String.class);
//                        markedBy = snapshot.child("markedBy").getValue(String.class);
//                        lateStatus = snapshot.child("lateStatus").getValue(String.class); // Fetch lateStatus
//
//                        isCheckedIn = (checkInTime != null && !checkInTime.isEmpty())
//                                && (checkOutTime == null || checkOutTime.isEmpty());
//
//                        updateUI();
//
//                        if (isCheckedIn) {
//                            startWorkTimer();
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {}
//                });
//    }
//    private void updateUI() {
//        tvCheckInTime.setText(checkInTime != null ? checkInTime : "Not marked");
//        tvCheckOutTime.setText(checkOutTime != null ? checkOutTime : "Not marked");
//
//        String displayStatus;
//        int statusColor = ContextCompat.getColor(this, R.color.red);
//        int dotDrawable = R.drawable.status_dot_absent;
//
//        if (checkInTime == null) {
//            // Not checked in yet
//            displayStatus = "Not Checked In";
//            statusColor = ContextCompat.getColor(this, R.color.red);
//            dotDrawable = R.drawable.status_dot_absent;
//        }
//        else if (checkOutTime == null) {
//            // Checked in but not checked out yet - show current status
//            if (lateStatus != null && lateStatus.equals("Late")) {
//                displayStatus = "Present (Late)";
//                statusColor = ContextCompat.getColor(this, R.color.orange);
//                dotDrawable = R.drawable.status_dot_late;
//            } else {
//                displayStatus = "Present";
//                statusColor = ContextCompat.getColor(this, R.color.green);
//                dotDrawable = R.drawable.status_dot_present;
//            }
//        }
//        else {
//            // Checked out - show final status
//            displayStatus = finalStatus != null ? finalStatus : "Completed";
//
//            if (finalStatus != null) {
//                // Check if late suffix exists
//                boolean hasLateSuffix = finalStatus.contains("(Late)");
//                String baseStatus = hasLateSuffix ?
//                        finalStatus.replace(" (Late)", "").trim() : finalStatus;
//
//                // Set color and dot based on base status
//                if (baseStatus.equals("Present")) {
//                    statusColor = ContextCompat.getColor(this, hasLateSuffix ? R.color.orange : R.color.green);
//                    dotDrawable = hasLateSuffix ? R.drawable.status_dot_late : R.drawable.status_dot_present;
//                }
//                else if (baseStatus.equals("Half Day")) {
//                    statusColor = ContextCompat.getColor(this, R.color.yellow);
//                    dotDrawable = R.drawable.status_dot_halfday;
//                }
//                else if (baseStatus.equals("Absent")) {
//                    statusColor = ContextCompat.getColor(this, R.color.red);
//                    dotDrawable = R.drawable.status_dot_absent;
//                }
//            }
//        }
//
//        tvTodayStatus.setText(displayStatus);
//        tvTodayStatus.setTextColor(statusColor);
//        statusIndicator.setBackgroundResource(dotDrawable);
//        updateButtonStates();
//
//        // Show work hours if available
//        if (checkInTime != null && checkOutTime != null) {
//            long totalMins = getDiffMinutes(checkInTime, checkOutTime);
//            tvWorkHours.setText(String.format("%dh %dm", totalMins / 60, totalMins % 60));
//        } else if (checkInTime != null && checkOutTime == null) {
//            // Show "Working..." or current work time during shift
//            tvWorkHours.setText("Working...");
//        }
//    }
//    private void checkApprovedLeaveThenCheckIn() {
//        String todayDate = getTodayDate();
//
//        DatabaseReference leaveRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("leaves");
//
//        leaveRef.orderByChild("employeeMobile")
//                .equalTo(employeeMobile)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        for (DataSnapshot s : snapshot.getChildren()) {
//                            String status = s.child("status").getValue(String.class);
//                            String fromDate = s.child("fromDate").getValue(String.class);
//                            String toDate = s.child("toDate").getValue(String.class);
//
//                            if ("APPROVED".equals(status) && isDateBetween(todayDate, fromDate, toDate)) {
//                                toast("❌ You are on approved leave today.\nCheck-in is disabled");
//                                return;
//                            }
//                        }
//                        tryCheckIn();
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        tryCheckIn();
//                    }
//                });
//    }
//
//    private void tryCheckIn() {
//        if (!isInternetAvailable()) {
//            toast("❌ No Internet");
//            return;
//        }
//
//        if (!locationReady) {
//            toast("⏳ Waiting for GPS");
//            return;
//        }
//
//        if (checkInTime != null && !checkInTime.isEmpty()) {
//            toast("⚠️ Check-in already marked today");
//            return;
//        }
//
//        if ("Admin".equals(markedBy) && finalStatus != null && !finalStatus.isEmpty()
//                && (checkInTime == null || checkInTime.isEmpty())) {
//            toast("⚠️ Attendance already marked by Admin");
//            return;
//        }
//
//        String currentTime = getCurrentTime();
//        boolean isLate = isLateCheckIn(shiftStart, currentTime);
//        String lateStatus = isLate ? "Late" : "On Time";
//
//        // Update UI immediately with late status
//        tvTodayStatus.setText(isLate ? "Present (Late)" : "Present");
//        tvTodayStatus.setTextColor(isLate
//                ? ContextCompat.getColor(this, R.color.orange)
//                : ContextCompat.getColor(this, R.color.green));
//
//        statusIndicator.setBackgroundResource(isLate
//                ? R.drawable.status_dot_late
//                : R.drawable.status_dot_present);
//
//        toast(" Taking photo for " + lateStatus + " check-in...");
//        openCamera("checkIn");
//    }
//
//    private void tryCheckOut() {
//        if (!isInternetAvailable()) {
//            toast("❌ No Internet");
//            return;
//        }
//
//        if (!locationReady) {
//            toast(" Waiting for GPS");
//            return;
//        }
//
//        if (checkInTime == null || checkInTime.isEmpty()) {
//            toast(" Please check-in first");
//            return;
//        }
//
//        if (checkOutTime != null && !checkOutTime.isEmpty()) {
//            toast(" Already checked out today");
//            return;
//        }
//
//        if ("Admin".equals(markedBy) && checkOutTime != null && !checkOutTime.isEmpty()) {
//            toast(" Attendance already completed by Admin");
//            return;
//        }
//
//        openCamera("checkOut");
//    }
//
//    private boolean isInternetAvailable() {
//        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
//    }
//
//    private void openCamera(String action) {
//        pendingAction = action;
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
//            return;
//        }
//
//        try {
//            Intent intent = new Intent(this, FrontCameraActivity.class);
//            intent.putExtra("action", action);
//            startActivityForResult(intent, CAMERA_REQUEST_CODE);
//        } catch (Exception e) {
//            toast("Using default camera");
//            Intent defaultCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            startActivityForResult(defaultCameraIntent, CAMERA_REQUEST_CODE);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == LOCATION_PERMISSION_CODE) {
//            boolean granted = true;
//            for (int result : grantResults) {
//                if (result != PackageManager.PERMISSION_GRANTED) granted = false;
//            }
//            if (granted) {
//                checkLocationSettings();
//            } else {
//                tvLocation.setText("Location denied");
//                toast("📍 GPS permission required");
//            }
//        }
//    }
//
//    private void checkLocationSettings() {
//        LocationSettingsRequest.Builder builder =
//                new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
//
//        SettingsClient client = LocationServices.getSettingsClient(this);
//        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
//
//        task.addOnSuccessListener(this, locationSettingsResponse -> getCurrentLocation());
//
//        task.addOnFailureListener(this, e -> {
//            if (e instanceof ResolvableApiException) {
//                try {
//                    ((ResolvableApiException) e).startResolutionForResult(
//                            EmployeeDashboardActivity.this, REQUEST_CHECK_SETTINGS);
//                } catch (IntentSender.SendIntentException sendEx) {
//                    // ignore
//                }
//            } else {
//                toast(" Enable location to continue");
//            }
//        });
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == REQUEST_CHECK_SETTINGS) {
//            if (resultCode == RESULT_OK) {
//                getCurrentLocation();
//            } else {
//                tvLocation.setText("Location off");
//                toast(" Please enable location to mark attendance");
//            }
//            return;
//        }
//
//        if (requestCode == CAMERA_REQUEST_CODE) {
//            if (resultCode == RESULT_OK && data != null) {
//                try {
//                    if (data.hasExtra("image_data")) {
//                        byte[] imageData = data.getByteArrayExtra("image_data");
//                        if (imageData != null && imageData.length > 0) {
//                            currentPhotoBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
//                            pendingAction = data.getStringExtra("action");
//                        }
//                        if (currentPhotoBitmap != null) {
//                            verifyAndProcessBitmap();
//                        }
//                    } else if (data.getExtras() != null && data.getExtras().containsKey("data")) {
//                        currentPhotoBitmap = (Bitmap) data.getExtras().get("data");
//                    }
//
//                    if (currentPhotoBitmap != null) {
//                        uploadPhotoAndSaveAttendance();
//                    } else {
//                        toast(" Failed to capture photo");
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    toast(" Error processing photo");
//                }
//            } else if (resultCode == RESULT_CANCELED) {
//                toast(" Photo capture cancelled");
//            }
//        }
//    }
//
//    private void verifyAndProcessBitmap() {
//        if (currentPhotoBitmap == null) {
//            toast(" Photo is null");
//            return;
//        }
//
//        Log.d("CAMERA_DEBUG", "Bitmap dimensions: " +
//                currentPhotoBitmap.getWidth() + "x" + currentPhotoBitmap.getHeight());
//        Log.d("CAMERA_DEBUG", "Bitmap size: " +
//                (currentPhotoBitmap.getByteCount() / 1024) + " KB");
//
//        uploadPhotoAndSaveAttendance();
//    }
//
//    private void uploadPhotoAndSaveAttendance() {
//        String today = getTodayDate();
//        String time = getCurrentTime();
//        String photoName = employeeMobile + "_" + pendingAction + "_" +
//                System.currentTimeMillis() + ".jpg";
//        StorageReference photoRef = attendancePhotoRef.child(today).child(photoName);
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        currentPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
//        byte[] data = baos.toByteArray();
//
//        toast(" Uploading...");
//        photoRef.putBytes(data).addOnSuccessListener(task ->
//                        photoRef.getDownloadUrl().addOnSuccessListener(uri ->
//                                saveAttendance(uri.toString(), time)))
//                .addOnFailureListener(e -> toast(" Upload failed"));
//    }
//
//    private void saveAttendance(String photoUrl, String time) {
//        if (employeeMobile == null) {
//            toast("Profile not loaded");
//            return;
//        }
//
//        String today = getTodayDate();
//        DatabaseReference node = attendanceRef.child(today).child(employeeMobile);
//
//        node.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                String existingMarkedBy = snapshot.child("markedBy").getValue(String.class);
//                String existingCheckIn = snapshot.child("checkInTime").getValue(String.class);
//                String existingCheckOut = snapshot.child("checkOutTime").getValue(String.class);
//                String existingStatus = snapshot.child("status").getValue(String.class);
//
//                // Block if admin marked only status
//                if ("Admin".equals(existingMarkedBy) &&
//                        (existingCheckIn == null || existingCheckIn.isEmpty()) &&
//                        (existingCheckOut == null || existingCheckOut.isEmpty()) &&
//                        existingStatus != null && !existingStatus.isEmpty()) {
//                    toast(" Attendance already marked by Admin");
//                    return;
//                }
//
//                if (pendingAction.equals("checkIn")) {
//                    // Hard lock for check-in
//                    if (snapshot.child("checkInTime").exists() &&
//                            existingCheckIn != null && !existingCheckIn.isEmpty()) {
//                        toast(" Check-in already exists");
//                        return;
//                    }
//
//                    boolean isLate = isLateCheckIn(shiftStart, time);
//                    String lateStatus = isLate ? "Late" : "On Time";
//
//                    node.child("checkInTime").setValue(time);
//                    node.child("checkInPhoto").setValue(photoUrl);
//                    node.child("lateStatus").setValue(lateStatus);  // ← This line saves it correctly
//
//                    node.child("lateStatus").setValue(lateStatus);
//                    node.child("status").setValue(isLate ? "Present (Late)" : "Present");
//                    node.child("finalStatus").setValue("Present");
//                    node.child("checkInLat").setValue(currentLat);
//                    node.child("checkInLng").setValue(currentLng);
//                    node.child("checkInAddress").setValue(currentAddress);
//                    node.child("markedBy").setValue("Employee");
//
//                    checkInTime = time;
//                    isCheckedIn = true;
//
//                    toast(" Checked In - " + lateStatus +
//                            (isLate ? "\n(More than 10 min after shift start)" : ""));
//                    startWorkTimer();
//                }
//                else if (pendingAction.equals("checkOut")) {
//                    // Hard lock for check-out
//                    if (snapshot.child("checkOutTime").exists() &&
//                            existingCheckOut != null && !existingCheckOut.isEmpty()) {
//                        toast(" Check-out already exists");
//                        return;
//                    }
//
//                    String lateStatus = snapshot.child("lateStatus").getValue(String.class);
//                    boolean wasLate = "Late".equals(lateStatus);
//
//                    // Calculate worked time
//                    long totalMins = getDiffMinutes(existingCheckIn, time);
//
//                    // Calculate final status based on shift duration
//                    String finalStatus = calculateFinalStatus(totalMins);
//
//                    // Add (Late) suffix if employee was late at check-in
//                    String displayStatus = finalStatus;
//                    if (wasLate) {
//                        displayStatus = finalStatus + " (Late)";
//                    }
//
//                    // Save all checkout data
//                    node.child("checkOutTime").setValue(time);
//                    node.child("checkOutPhoto").setValue(photoUrl);
//                    node.child("finalStatus").setValue(finalStatus);
//                    node.child("status").setValue(displayStatus);
//                    node.child("totalMinutes").setValue(totalMins);
//                    node.child("totalHours").setValue(String.format("%.1f", totalMins / 60.0));
//                    node.child("checkOutLat").setValue(currentLat);
//                    node.child("checkOutLng").setValue(currentLng);
//                    node.child("checkOutAddress").setValue(currentAddress);
//                    node.child("shiftStart").setValue(shiftStart);
//                    node.child("shiftEnd").setValue(shiftEnd);
//                    node.child("shiftDurationMinutes").setValue(shiftDurationMinutes);
//
//                    if ("Admin".equals(existingMarkedBy)) {
//                        node.child("markedBy").setValue("Admin+Employee");
//                    } else {
//                        node.child("markedBy").setValue("Employee");
//                    }
//
//                    checkOutTime = time;
//                    isCheckedIn = false;
//
//                    // Create detailed toast message
//                    String toastMsg = " Checked Out\n" + displayStatus +
//                            "\nWorked: " + (totalMins/60) + "h " + (totalMins%60) + "m";
//
//                    if (finalStatus.equals("Half Day")) {
//                        long shortBy = shiftDurationMinutes - totalMins;
//                        toastMsg += "\n Short by " + (shortBy/60) + "h " + (shortBy%60) + "m";
//                    }
//
//                    toast(toastMsg);
//                    stopWorkTimer();
//                }
//
//                loadTodayAttendance();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                toast(" Save failed");
//            }
//        });
//    }
//
//    private boolean isLateCheckIn(String shiftStartTime, String checkInTime) {
//        if (shiftStartTime == null || shiftStartTime.isEmpty()) {
//            return false;
//        }
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//            Date shiftStart = sdf.parse(shiftStartTime);
//            Date checkIn = sdf.parse(checkInTime);
//
//            if (shiftStart == null || checkIn == null) return false;
//
//            long diffMinutes = (checkIn.getTime() - shiftStart.getTime()) / 60000;
//
//            // Late if check-in is MORE than 10 minutes after shift start
//            boolean isLate = diffMinutes > 15;
//
//            Log.d("LATE_CHECK", "Shift: " + shiftStartTime + ", CheckIn: " + checkInTime +
//                    ", Diff: " + diffMinutes + " mins, Late: " + isLate);
//
//            return isLate;
//        } catch (Exception e) {
//            Log.e("LATE_CHECK", "Error: " + e.getMessage());
//            return false;
//        }
//    }
//
//    private String calculateFinalStatus(long workedMinutes) {
//        Log.d("STATUS_CALC", "========================================");
//        Log.d("STATUS_CALC", "Worked Minutes: " + workedMinutes);
//        Log.d("STATUS_CALC", "Shift Duration: " + shiftDurationMinutes + " mins");
//        Log.d("STATUS_CALC", "Shift Time: " + (shiftDurationMinutes/60) + "h " +
//                (shiftDurationMinutes%60) + "m");
//        Log.d("STATUS_CALC", "Worked Time: " + (workedMinutes/60) + "h " +
//                (workedMinutes%60) + "m");
//
//        String status;
//
//        if (workedMinutes >= shiftDurationMinutes) {
//            // Completed full shift or more
//            status = "Present";
//            Log.d("STATUS_CALC", "Result: PRESENT (worked >= shift duration)");
//        } else if (workedMinutes > 0) {
//            // Worked something but less than full shift
//            status = "Half Day";
//            Log.d("STATUS_CALC", "Result: HALF DAY (0 < worked < shift duration)");
//            Log.d("STATUS_CALC", "Short by: " + (shiftDurationMinutes - workedMinutes) + " mins");
//        } else {
//            // No work time
//            status = "Absent";
//            Log.d("STATUS_CALC", "Result: ABSENT (no work time)");
//        }
//
//        Log.d("STATUS_CALC", "========================================");
//        return status;
//    }
//
//    private long getDiffMinutes(String start, String end) {
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//            Date startDate = sdf.parse(start);
//            Date endDate = sdf.parse(end);
//
//            if (startDate == null || endDate == null) {
//                Log.e("TIME_DIFF", "Null dates - start: " + start + ", end: " + end);
//                return 0;
//            }
//
//            long diffMillis = endDate.getTime() - startDate.getTime();
//
//            // Handle next day (night shift case)
//            if (diffMillis < 0) {
//                diffMillis += 24 * 60 * 60 * 1000; // Add 24 hours
//            }
//
//            long minutes = diffMillis / 60000;
//
//            Log.d("TIME_DIFF", "Start: " + start + ", End: " + end +
//                    ", Diff: " + minutes + " mins (" + (minutes/60) + "h " + (minutes%60) + "m)");
//
//            return Math.max(0, minutes);
//
//        } catch (ParseException | NullPointerException e) {
//            Log.e("TIME_DIFF", "Parse error: " + e.getMessage());
//            return 0;
//        }
//    }
//    private void startWorkTimer() {
//        workTimerHandler = new Handler();
//        workTimerRunnable = new Runnable() {
//            @Override
//            public void run() {
//                if (isCheckedIn && checkInTime != null) {
//                    String currentTime = getCurrentTime();
//                    long workMins = getDiffMinutes(checkInTime, currentTime);
//                    tvWorkHours.setText(String.format("%dh %dm", workMins / 60, workMins % 60));
//                }
//                if (workTimerHandler != null) {
//                    workTimerHandler.postDelayed(this, 60000);
//                }
//            }
//        };
//        workTimerHandler.post(workTimerRunnable);
//    }
//
//    private void stopWorkTimer() {
//        if (workTimerHandler != null && workTimerRunnable != null) {
//            workTimerHandler.removeCallbacks(workTimerRunnable);
//        }
//    }
//
//    private void openAttendanceReport() {
//        Intent intent = new Intent(this, AttendanceReportActivity.class);
//        intent.putExtra("companyKey", companyKey);
//        intent.putExtra("employeeMobile", employeeMobile);
//        startActivity(intent);
//    }
//
//    private void showLogoutConfirmation() {
//        new androidx.appcompat.app.AlertDialog.Builder(this)
//                .setTitle("Logout")
//                .setMessage("Are you sure?")
//                .setPositiveButton("Yes", (d, w) -> logout())
//                .setNegativeButton("Cancel", null)
//                .show();
//    }
//
//    private void logout() {
//        new PrefManager(this).logout();
//        startActivity(new Intent(this, LoginActivity.class)
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//        finish();
//    }
//
//    private String getTodayDate() {
//        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
//    }
//
//    private String getCurrentTime() {
//        return new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date());
//    }
//
//    private void startClock() {
//        timeHandler = new Handler();
//        timeRunnable = () -> {
//            tvCurrentTime.setText(new SimpleDateFormat("MMM dd, h:mm a",
//                    Locale.getDefault()).format(new Date()));
//            timeHandler.postDelayed(timeRunnable, 1000);
//        };
//        timeHandler.post(timeRunnable);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (locationReady) startLocationUpdates();
//        loadTodayAttendance();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (timeHandler != null) timeHandler.removeCallbacks(timeRunnable);
//        stopWorkTimer();
//        if (fusedLocationClient != null && locationCallback != null) {
//            fusedLocationClient.removeLocationUpdates(locationCallback);
//        }
//    }
//
//    private boolean isDateBetween(String today, String from, String to) {
//        if (from == null || to == null) return false;
//        return today.compareTo(from) >= 0 && today.compareTo(to) <= 0;
//    }
//
//    private void toast(String msg) {
//        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
//    }
//} IN ABOVE CODE NOT WORKING MULTIPLE CHEK IN CHEEK OUT



//BELOW CODE WORKING MULTIPLE CHEK IN AND CHECK OUT PROPER


package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
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
    private int shiftDurationMinutes = 540;
    private int halfDayMinutes = 270;

    // Multiple check-in/out tracking
    private List<CheckInOutPair> checkInOutPairs = new ArrayList<>();
    private String currentActiveCheckIn = null;
    private long totalWorkedMinutes = 0;

    private String firstCheckInTime = null;
    private String lastCheckOutTime = null;
    private String finalStatus = "Not Checked In";
    private String currentAddress = "Getting location...";
    private double currentLat = 0, currentLng = 0;
    private Bitmap currentPhotoBitmap;
    private boolean isCurrentlyCheckedIn = false;
    private String markedBy = "";
    private String lateStatus = "";

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

    private TextView tvMonthPresent, tvLeaveBalance;
    private ImageView btnMyLeaves;
    private TextView tvMonthLate, tvAvgWorkHours, tvOnTimePercent;
    private ProgressBar progressPresent, progressLate, progressAvgHours, progressOnTime;

    // Helper class for check-in/out pairs
    private static class CheckInOutPair {
        String checkInTime;
        String checkOutTime;
        String checkInPhoto;
        String checkOutPhoto;
        double checkInLat, checkInLng;
        double checkOutLat, checkOutLng;
        String checkInAddress;
        String checkOutAddress;
        long durationMinutes;

        CheckInOutPair(String checkInTime) {
            this.checkInTime = checkInTime;
        }
    }

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
                            tvCompany.setText(companyName);
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

        tvMonthPresent = findViewById(R.id.tvMonthPresent);
        tvLeaveBalance = findViewById(R.id.tvLeaveBalance);
        btnMyLeaves = findViewById(R.id.btnMyLeaves);

        cardCheckIn.setOnClickListener(v -> checkApprovedLeaveThenCheckIn());
        cardCheckOut.setOnClickListener(v -> tryCheckOut());
        cardAttendanceReport.setOnClickListener(v -> openAttendanceReport());
        cardLogout.setOnClickListener(v -> showLogoutConfirmation());

        tvMonthLate = findViewById(R.id.tvMonthLate);
        tvAvgWorkHours = findViewById(R.id.tvAvgWorkHours);
        tvOnTimePercent = findViewById(R.id.tvOnTimePercent);
        progressPresent = findViewById(R.id.progressPresent);
        progressLate = findViewById(R.id.progressLate);
        progressAvgHours = findViewById(R.id.progressAvgHours);
        progressOnTime = findViewById(R.id.progressOnTime);

        View btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(EmployeeDashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        btnMyLeaves.setOnClickListener(v -> openMyLeaves());

        findViewById(R.id.cardDailyReport).setOnClickListener(v -> openTodaysWork());
        findViewById(R.id.cardApplyLeave).setOnClickListener(v -> openApplyLeave());

        // Don't call updateButtonStates here - will be called after data loads
        loadMonthlyAttendance();
        loadLeaveBalance();
    }

    private void updateGreeting() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good Morning" :
                hour < 17 ? "Good Afternoon" : "Good Evening";
        tvWelcome.setText(greeting);
    }

    private void openTodaysWork() {
        Intent intent = new Intent(this, EmployeeTodayWorkActivity.class);
        intent.putExtra("companyKey", companyKey);
        intent.putExtra("employeeMobile", employeeMobile);
        startActivity(intent);
    }

    private void openApplyLeave() {
        Intent intent = new Intent(this, ApplyLeaveActivity.class);
        intent.putExtra("companyKey", companyKey);
        intent.putExtra("employeeMobile", employeeMobile);
        startActivity(intent);
    }

    private void openMyLeaves() {
        Intent intent = new Intent(this, MyLeavesActivity.class);
        startActivity(intent);
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
        boolean hasFirstCheckIn = firstCheckInTime != null && !firstCheckInTime.isEmpty();
        boolean hasLastCheckOut = lastCheckOutTime != null && !lastCheckOutTime.isEmpty();

        // Admin marked only status without any check-in time - ALLOW employee to check in/out
        boolean adminMarkedOnlyStatus = "Admin".equals(markedBy) &&
                !hasFirstCheckIn &&
                finalStatus != null && !finalStatus.isEmpty();

        boolean canCheckIn = false;
        boolean canCheckOut = false;

        // Always allow check-in/out based on current state
        // Even if admin marked status, employee can still do their sessions
        canCheckIn = locationReady && !isCurrentlyCheckedIn;
        canCheckOut = locationReady && isCurrentlyCheckedIn;

        Log.d("BUTTON_STATE", "=================================");
        Log.d("BUTTON_STATE", "Location Ready: " + locationReady);
        Log.d("BUTTON_STATE", "Is Currently Checked In: " + isCurrentlyCheckedIn);
        Log.d("BUTTON_STATE", "Has First Check-In: " + hasFirstCheckIn);
        Log.d("BUTTON_STATE", "Has Last Check-Out: " + hasLastCheckOut);
        Log.d("BUTTON_STATE", "Marked By: " + markedBy);
        Log.d("BUTTON_STATE", "Admin Marked Only Status: " + adminMarkedOnlyStatus);
        Log.d("BUTTON_STATE", "Can Check In: " + canCheckIn);
        Log.d("BUTTON_STATE", "Can Check Out: " + canCheckOut);
        Log.d("BUTTON_STATE", "=================================");

        cardCheckIn.setEnabled(canCheckIn);
        cardCheckIn.setAlpha(canCheckIn ? 1f : 0.4f);

        cardCheckOut.setEnabled(canCheckOut);
        cardCheckOut.setAlpha(canCheckOut ? 1f : 0.4f);
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

    private void loadMonthlyAttendance() {
        if (employeeMobile == null) {
            Log.e("STATS_ERROR", "employeeMobile is NULL!");
            return;
        }

        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        Calendar cal = Calendar.getInstance();
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        attendanceRef.orderByKey().startAt(currentMonth + "-01").endAt(currentMonth + "-31")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int presentCount = 0;
                        int lateCount = 0;
                        int onTimeCount = 0;
                        long totalMinutes = 0;
                        int daysWithHours = 0;

                        for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                            DataSnapshot empData = dateSnapshot.child(employeeMobile);
                            if (empData.exists()) {
                                String finalStatus = empData.child("finalStatus").getValue(String.class);
                                String lateStatus = empData.child("lateStatus").getValue(String.class);
                                String checkInTime = empData.child("checkInTime").getValue(String.class);
                                String checkOutTime = empData.child("checkOutTime").getValue(String.class);
                                String status = empData.child("status").getValue(String.class);
                                String markedBy = empData.child("markedBy").getValue(String.class);
                                Long minutes = empData.child("totalMinutes").getValue(Long.class);

                                String effectiveStatus = (finalStatus != null) ? finalStatus : status;

                                if (effectiveStatus != null &&
                                        (effectiveStatus.equalsIgnoreCase("Present") ||
                                                effectiveStatus.equalsIgnoreCase("Half Day"))) {

                                    presentCount++;

                                    // Check late status
                                    boolean isLate = false;
                                    if (lateStatus != null && lateStatus.equals("Late")) {
                                        isLate = true;
                                    } else if (status != null && status.contains("(Late)")) {
                                        isLate = true;
                                    }

                                    if (isLate) {
                                        lateCount++;
                                    } else {
                                        onTimeCount++;
                                    }

                                    // Calculate worked minutes - FIXED
                                    long dayMinutes = 0;

                                    if (minutes != null && minutes > 0 && minutes < 1440) {
                                        // Use actual recorded minutes (valid range: 0-1440)
                                        dayMinutes = minutes;
                                    } else if (checkInTime != null && !checkInTime.isEmpty() &&
                                            checkOutTime != null && !checkOutTime.isEmpty()) {
                                        // Calculate from check-in and check-out times
                                        long calculatedMinutes = getDiffMinutes(checkInTime, checkOutTime);
                                        if (calculatedMinutes > 0 && calculatedMinutes < 1440) {
                                            dayMinutes = calculatedMinutes;
                                        }
                                    } else if ("Admin".equals(markedBy) && effectiveStatus.equals("Present")) {
                                        // Admin marked Present without times - use full shift
                                        dayMinutes = shiftDurationMinutes;
                                    } else if ("Admin".equals(markedBy) && effectiveStatus.equals("Half Day")) {
                                        // Admin marked Half Day without times - use half shift
                                        dayMinutes = shiftDurationMinutes / 2;
                                    }

                                    if (dayMinutes > 0) {
                                        totalMinutes += dayMinutes;
                                        daysWithHours++;
                                        Log.d("STATS_DEBUG", "Day: " + dateSnapshot.getKey() + ", Minutes: " + dayMinutes);
                                    }
                                }
                            }
                        }

                        Log.d("STATS_DEBUG", "Final - Present: " + presentCount + ", TotalMinutes: " + totalMinutes + ", DaysWithHours: " + daysWithHours);

                        final int finalPresentCount = presentCount;
                        final int finalLateCount = lateCount;
                        final int finalOnTimeCount = onTimeCount;
                        final long finalTotalMinutes = totalMinutes;
                        final int finalDaysWithHours = daysWithHours;
                        final int finalDaysInMonth = daysInMonth;

                        runOnUiThread(() -> {
                            tvMonthPresent.setText(String.valueOf(finalPresentCount));
                            animateProgress(progressPresent, finalPresentCount, finalDaysInMonth);

                            tvMonthLate.setText(String.valueOf(finalLateCount));
                            int lateMax = finalPresentCount > 0 ? finalPresentCount : 1;
                            animateProgress(progressLate, finalLateCount, lateMax);

                            // FIXED Average hours calculation
                            if (finalDaysWithHours > 0) {
                                double avgHours = (double) finalTotalMinutes / finalDaysWithHours / 60.0;

                                // Safety check - average should be between 0 and 24
                                if (avgHours >= 0 && avgHours <= 24) {
                                    String avgText = String.format(Locale.getDefault(), "%.1f", avgHours);
                                    tvAvgWorkHours.setText(avgText);

                                    // Progress: 0-9 hours maps to 0-90
                                    int progressValue = Math.min((int) (avgHours * 10), 90);
                                    animateProgress(progressAvgHours, progressValue, 90);
                                    Log.d("STATS_DEBUG", "Avg Hours: " + avgText);
                                } else {
                                    tvAvgWorkHours.setText("0.0");
                                    if (progressAvgHours != null) progressAvgHours.setProgress(0);
                                    Log.e("STATS_ERROR", "Invalid avg hours: " + avgHours);
                                }
                            } else {
                                tvAvgWorkHours.setText("0.0");
                                if (progressAvgHours != null) progressAvgHours.setProgress(0);
                            }

                            if (finalPresentCount > 0) {
                                int onTimePercent = (int) ((finalOnTimeCount * 100.0) / finalPresentCount);
                                tvOnTimePercent.setText(String.valueOf(onTimePercent));
                                animateProgress(progressOnTime, onTimePercent, 100);
                            } else {
                                tvOnTimePercent.setText("0");
                                if (progressOnTime != null) progressOnTime.setProgress(0);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("STATS_ERROR", "Firebase error: " + error.getMessage());
                        runOnUiThread(() -> {
                            if (tvMonthPresent != null) tvMonthPresent.setText("0");
                            if (tvMonthLate != null) tvMonthLate.setText("0");
                            if (tvAvgWorkHours != null) tvAvgWorkHours.setText("0.0");
                            if (tvOnTimePercent != null) tvOnTimePercent.setText("0");
                            if (progressPresent != null) progressPresent.setProgress(0);
                            if (progressLate != null) progressLate.setProgress(0);
                            if (progressAvgHours != null) progressAvgHours.setProgress(0);
                            if (progressOnTime != null) progressOnTime.setProgress(0);
                        });
                    }
                });
    }

    private void animateProgress(ProgressBar progressBar, int value, int max) {
        if (progressBar == null) return;

        progressBar.setMax(max);

        if (value > max) {
            value = max;
        }

        android.animation.ObjectAnimator animation = android.animation.ObjectAnimator.ofInt(
                progressBar, "progress", 0, value);
        animation.setDuration(1000);
        animation.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animation.start();
    }

    private void loadLeaveBalance() {
        if (employeeMobile == null) return;

        employeesRef.child(employeeMobile).child("info").child("leaveBalance")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Integer balance = snapshot.getValue(Integer.class);
                        if (balance != null) {
                            tvLeaveBalance.setText(String.valueOf(balance));
                        } else {
                            calculateLeaveBalanceFromAttendance();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvLeaveBalance.setText("0");
                    }
                });
    }

    private void calculateLeaveBalanceFromAttendance() {
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        attendanceRef.orderByKey().startAt(currentMonth + "-01").endAt(currentMonth + "-31")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int absentCount = 0;
                        int totalWorkingDays = 0;

                        for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                            DataSnapshot empData = dateSnapshot.child(employeeMobile);
                            if (empData.exists()) {
                                String finalStatus = empData.child("finalStatus").getValue(String.class);
                                String status = empData.child("status").getValue(String.class);

                                String effectiveStatus = (finalStatus != null) ? finalStatus : status;

                                if (effectiveStatus != null) {
                                    totalWorkingDays++;
                                    if (effectiveStatus.equalsIgnoreCase("Absent")) {
                                        absentCount++;
                                    }
                                }
                            }
                        }

                        int monthlyLeaveAllocation = 2;
                        int estimatedBalance = Math.max(0, monthlyLeaveAllocation - absentCount);
                        tvLeaveBalance.setText(String.valueOf(estimatedBalance));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvLeaveBalance.setText("0");
                    }
                });
    }

    private void getCurrentLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 201);
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
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
                        String shortAddr = currentAddress.length() > 35 ?
                                currentAddress.substring(0, 35) + "..." : currentAddress;
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
                        loadMonthlyAttendance();

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
                    calculateShiftDuration(shiftStart, shiftEnd);
                } else {
                    parseShiftString(employeeShift);
                }
                loadTodayAttendance();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                parseShiftString(employeeShift);
                loadTodayAttendance();
            }
        });
    }

    private void calculateShiftDuration(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date start = sdf.parse(startTime);
            Date end = sdf.parse(endTime);

            if (start != null && end != null) {
                long diffMillis = end.getTime() - start.getTime();

                if (diffMillis < 0) {
                    diffMillis += 24 * 60 * 60 * 1000;
                }

                shiftDurationMinutes = (int) (diffMillis / 60000);
                halfDayMinutes = 0;

                Log.d("SHIFT_CALC", "Shift Duration: " + shiftDurationMinutes + " mins (" +
                        (shiftDurationMinutes/60) + "h " + (shiftDurationMinutes%60) + "m)");
            }
        } catch (ParseException e) {
            shiftDurationMinutes = 540;
            halfDayMinutes = 0;
            Log.e("SHIFT_CALC", "Parse error, using defaults");
        }
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
                    calculateShiftDuration(shiftStart, shiftEnd);
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
                        // Load basic fields
                        firstCheckInTime = snapshot.child("checkInTime").getValue(String.class);
                        lastCheckOutTime = snapshot.child("checkOutTime").getValue(String.class);
                        finalStatus = snapshot.child("status").getValue(String.class);
                        markedBy = snapshot.child("markedBy").getValue(String.class);
                        lateStatus = snapshot.child("lateStatus").getValue(String.class);

                        // Load total worked minutes
                        Long storedMinutes = snapshot.child("totalMinutes").getValue(Long.class);
                        totalWorkedMinutes = storedMinutes != null ? storedMinutes : 0;

                        // Load check-in/out pairs
                        checkInOutPairs.clear();
                        DataSnapshot pairsSnapshot = snapshot.child("checkInOutPairs");

                        boolean hasPairs = pairsSnapshot.exists() && pairsSnapshot.getChildrenCount() > 0;

                        if (hasPairs) {
                            // Load from pairs
                            for (DataSnapshot pairSnap : pairsSnapshot.getChildren()) {
                                String checkIn = pairSnap.child("checkInTime").getValue(String.class);
                                if (checkIn != null) {
                                    CheckInOutPair pair = new CheckInOutPair(checkIn);
                                    pair.checkOutTime = pairSnap.child("checkOutTime").getValue(String.class);
                                    pair.checkInPhoto = pairSnap.child("checkInPhoto").getValue(String.class);
                                    pair.checkOutPhoto = pairSnap.child("checkOutPhoto").getValue(String.class);

                                    Double lat = pairSnap.child("checkInLat").getValue(Double.class);
                                    pair.checkInLat = lat != null ? lat : 0;
                                    Double lng = pairSnap.child("checkInLng").getValue(Double.class);
                                    pair.checkInLng = lng != null ? lng : 0;

                                    lat = pairSnap.child("checkOutLat").getValue(Double.class);
                                    pair.checkOutLat = lat != null ? lat : 0;
                                    lng = pairSnap.child("checkOutLng").getValue(Double.class);
                                    pair.checkOutLng = lng != null ? lng : 0;

                                    pair.checkInAddress = pairSnap.child("checkInAddress").getValue(String.class);
                                    pair.checkOutAddress = pairSnap.child("checkOutAddress").getValue(String.class);

                                    Long duration = pairSnap.child("durationMinutes").getValue(Long.class);
                                    pair.durationMinutes = duration != null ? duration : 0;

                                    checkInOutPairs.add(pair);
                                }
                            }
                        } else if (firstCheckInTime != null && !firstCheckInTime.isEmpty()) {
                            // No pairs structure - create from main fields (admin might have added)
                            CheckInOutPair pair = new CheckInOutPair(firstCheckInTime);
                            pair.checkInPhoto = snapshot.child("checkInPhoto").getValue(String.class);

                            Double lat = snapshot.child("checkInLat").getValue(Double.class);
                            pair.checkInLat = lat != null ? lat : 0;
                            Double lng = snapshot.child("checkInLng").getValue(Double.class);
                            pair.checkInLng = lng != null ? lng : 0;

                            pair.checkInAddress = snapshot.child("checkInAddress").getValue(String.class);

                            // Check if checkout exists
                            if (lastCheckOutTime != null && !lastCheckOutTime.isEmpty()) {
                                pair.checkOutTime = lastCheckOutTime;
                                pair.checkOutPhoto = snapshot.child("checkOutPhoto").getValue(String.class);

                                lat = snapshot.child("checkOutLat").getValue(Double.class);
                                pair.checkOutLat = lat != null ? lat : 0;
                                lng = snapshot.child("checkOutLng").getValue(Double.class);
                                pair.checkOutLng = lng != null ? lng : 0;

                                pair.checkOutAddress = snapshot.child("checkOutAddress").getValue(String.class);
                                pair.durationMinutes = getDiffMinutes(firstCheckInTime, lastCheckOutTime);

                                // Update totalWorkedMinutes if not set
                                if (totalWorkedMinutes == 0) {
                                    totalWorkedMinutes = pair.durationMinutes;
                                }
                            }

                            checkInOutPairs.add(pair);
                            Log.d("LOAD_DEBUG", "Created pair from main fields");
                        }

                        // Check if currently checked in (last pair has no checkout)
                        isCurrentlyCheckedIn = false;
                        currentActiveCheckIn = null;
                        if (!checkInOutPairs.isEmpty()) {
                            CheckInOutPair lastPair = checkInOutPairs.get(checkInOutPairs.size() - 1);
                            if (lastPair.checkOutTime == null || lastPair.checkOutTime.isEmpty()) {
                                isCurrentlyCheckedIn = true;
                                currentActiveCheckIn = lastPair.checkInTime;
                            }
                        }

                        Log.d("LOAD_DEBUG", "First Check-In: " + firstCheckInTime);
                        Log.d("LOAD_DEBUG", "Last Check-Out: " + lastCheckOutTime);
                        Log.d("LOAD_DEBUG", "Currently Checked In: " + isCurrentlyCheckedIn);
                        Log.d("LOAD_DEBUG", "Total Pairs: " + checkInOutPairs.size());
                        Log.d("LOAD_DEBUG", "Total Worked Minutes: " + totalWorkedMinutes);
                        Log.d("LOAD_DEBUG", "Marked By: " + markedBy);

                        updateUI();

                        if (isCurrentlyCheckedIn) {
                            startWorkTimer();
                        } else {
                            stopWorkTimer();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateUI() {        // Display first check-in and last check-out
        tvCheckInTime.setText(firstCheckInTime != null ? firstCheckInTime : "Not marked");
        tvCheckOutTime.setText(lastCheckOutTime != null ? lastCheckOutTime : "Not marked");

        String displayStatus;
        int statusColor = ContextCompat.getColor(this, R.color.red);
        int dotDrawable = R.drawable.status_dot_absent;

        boolean hasFirstCheckIn = firstCheckInTime != null && !firstCheckInTime.isEmpty();
        boolean adminMarkedOnlyStatus = "Admin".equals(markedBy) &&
                !hasFirstCheckIn &&
                finalStatus != null && !finalStatus.isEmpty();

        if (adminMarkedOnlyStatus && !isCurrentlyCheckedIn && checkInOutPairs.isEmpty()) {
            // Admin marked only status without times and employee hasn't checked in yet
            displayStatus = finalStatus + " (Admin)";

            if (finalStatus.equals("Present")) {
                statusColor = ContextCompat.getColor(this, R.color.green);
                dotDrawable = R.drawable.status_dot_present;
            } else if (finalStatus.equals("Half Day")) {
                statusColor = ContextCompat.getColor(this, R.color.yellow);
                dotDrawable = R.drawable.status_dot_halfday;
            } else if (finalStatus.equals("Absent")) {
                statusColor = ContextCompat.getColor(this, R.color.red);
                dotDrawable = R.drawable.status_dot_absent;
            }

        } else if (firstCheckInTime == null && checkInOutPairs.isEmpty()) {
            displayStatus = "Not Checked In";
            statusColor = ContextCompat.getColor(this, R.color.red);
            dotDrawable = R.drawable.status_dot_absent;
        } else if (isCurrentlyCheckedIn) {
            // Currently checked in
            if (lateStatus != null && lateStatus.equals("Late")) {
                displayStatus = "Present (Late)";
                statusColor = ContextCompat.getColor(this, R.color.orange);
                dotDrawable = R.drawable.status_dot_late;
            } else {
                displayStatus = "Present";
                statusColor = ContextCompat.getColor(this, R.color.green);
                dotDrawable = R.drawable.status_dot_present;
            }
        } else {
            // All checked out - show final status
            displayStatus = finalStatus != null ? finalStatus : "Completed";

            if (finalStatus != null) {
                boolean hasLateSuffix = finalStatus.contains("(Late)");
                String baseStatus = hasLateSuffix ?
                        finalStatus.replace(" (Late)", "").trim() : finalStatus;

                if (baseStatus.equals("Present")) {
                    statusColor = ContextCompat.getColor(this, hasLateSuffix ? R.color.orange : R.color.green);
                    dotDrawable = hasLateSuffix ? R.drawable.status_dot_late : R.drawable.status_dot_present;
                } else if (baseStatus.equals("Half Day")) {
                    statusColor = ContextCompat.getColor(this, R.color.yellow);
                    dotDrawable = R.drawable.status_dot_halfday;
                } else if (baseStatus.equals("Absent")) {
                    statusColor = ContextCompat.getColor(this, R.color.red);
                    dotDrawable = R.drawable.status_dot_absent;
                }
            }
        }

        tvTodayStatus.setText(displayStatus);
        tvTodayStatus.setTextColor(statusColor);
        statusIndicator.setBackgroundResource(dotDrawable);

        updateButtonStates();

        // Show work hours
        if (isCurrentlyCheckedIn && currentActiveCheckIn != null) {
            // Currently working - will be updated by timer
            tvWorkHours.setText("Working...");
        } else if (totalWorkedMinutes > 0) {
            // Has recorded time - display it
            long hours = totalWorkedMinutes / 60;
            long mins = totalWorkedMinutes % 60;
            tvWorkHours.setText(String.format("%dh %dm", hours, mins));
        } else if (firstCheckInTime != null && lastCheckOutTime != null) {
            // Calculate from times if totalMinutes not available
            long calculatedMinutes = getDiffMinutes(firstCheckInTime, lastCheckOutTime);
            if (calculatedMinutes > 0 && calculatedMinutes < 1440) {
                long hours = calculatedMinutes / 60;
                long mins = calculatedMinutes % 60;
                tvWorkHours.setText(String.format("%dh %dm", hours, mins));
            } else {
                tvWorkHours.setText("0h 0m");
            }
        } else if (adminMarkedOnlyStatus) {
            // Admin marked status only - show estimated
            if (finalStatus != null && finalStatus.equals("Present")) {
                long hours = shiftDurationMinutes / 60;
                long mins = shiftDurationMinutes % 60;
                tvWorkHours.setText(String.format("%dh %dm (Est.)", hours, mins));
            } else if (finalStatus != null && finalStatus.equals("Half Day")) {
                long halfShift = shiftDurationMinutes / 2;
                long hours = halfShift / 60;
                long mins = halfShift % 60;
                tvWorkHours.setText(String.format("%dh %dm (Est.)", hours, mins));
            } else {
                tvWorkHours.setText("0h 0m");
            }
        } else {
            tvWorkHours.setText("0h 0m");
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

        if (isCurrentlyCheckedIn) {
            toast("⚠️ Already checked in. Please check out first.");
            return;
        }

        // REMOVED - Allow check-in even if admin marked status
        // Employee can do their own sessions

        String currentTime = getCurrentTime();

        // Only check late status on first check-in
        if (firstCheckInTime == null) {
            boolean isLate = isLateCheckIn(shiftStart, currentTime);
            lateStatus = isLate ? "Late" : "On Time";

            tvTodayStatus.setText(isLate ? "Present (Late)" : "Present");
            tvTodayStatus.setTextColor(isLate
                    ? ContextCompat.getColor(this, R.color.orange)
                    : ContextCompat.getColor(this, R.color.green));

            statusIndicator.setBackgroundResource(isLate
                    ? R.drawable.status_dot_late
                    : R.drawable.status_dot_present);

            toast("📸 Taking photo for " + lateStatus + " check-in...");
        } else {
            toast("📸 Taking photo for check-in...");
        }

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

        if (!isCurrentlyCheckedIn) {
            toast("⚠️ Please check-in first");
            return;
        }

        // Remove admin blocking for check-out
        // Employee can always check-out if they're checked in

        toast("📸 Taking photo for check-out...");
        openCamera("checkOut");
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private void openCamera(String action) {
        pendingAction = action;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
            return;
        }

        try {
            Intent intent = new Intent(this, FrontCameraActivity.class);
            intent.putExtra("action", action);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } catch (Exception e) {
            toast("Using default camera");
            Intent defaultCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(defaultCameraIntent, CAMERA_REQUEST_CODE);
        }
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
                            EmployeeDashboardActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                }
            } else {
                toast("⚠️ Enable location to continue");
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
                toast("⚠️ Please enable location to mark attendance");
            }
            return;
        }

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    if (data.hasExtra("image_data")) {
                        byte[] imageData = data.getByteArrayExtra("image_data");
                        if (imageData != null && imageData.length > 0) {
                            currentPhotoBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                            pendingAction = data.getStringExtra("action");
                        }
                        if (currentPhotoBitmap != null) {
                            verifyAndProcessBitmap();
                        }
                    } else if (data.getExtras() != null && data.getExtras().containsKey("data")) {
                        currentPhotoBitmap = (Bitmap) data.getExtras().get("data");
                    }

                    if (currentPhotoBitmap != null) {
                        uploadPhotoAndSaveAttendance();
                    } else {
                        toast("❌ Failed to capture photo");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    toast("❌ Error processing photo");
                }
            } else if (resultCode == RESULT_CANCELED) {
                toast("❌ Photo capture cancelled");
            }
        }
    }

    private void verifyAndProcessBitmap() {
        if (currentPhotoBitmap == null) {
            toast("❌ Photo is null");
            return;
        }

        Log.d("CAMERA_DEBUG", "Bitmap dimensions: " +
                currentPhotoBitmap.getWidth() + "x" + currentPhotoBitmap.getHeight());
        Log.d("CAMERA_DEBUG", "Bitmap size: " +
                (currentPhotoBitmap.getByteCount() / 1024) + " KB");

        uploadPhotoAndSaveAttendance();
    }

    private void uploadPhotoAndSaveAttendance() {
        String today = getTodayDate();
        String time = getCurrentTime();
        String photoName = employeeMobile + "_" + pendingAction + "_" +
                System.currentTimeMillis() + ".jpg";
        StorageReference photoRef = attendancePhotoRef.child(today).child(photoName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currentPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] data = baos.toByteArray();

        toast("⬆️ Uploading...");
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
                String existingMarkedBy = snapshot.child("markedBy").getValue(String.class);
                String existingFirstCheckIn = snapshot.child("checkInTime").getValue(String.class);
                String existingLastCheckOut = snapshot.child("checkOutTime").getValue(String.class);
                String existingStatus = snapshot.child("status").getValue(String.class);
                String existingFinalStatus = snapshot.child("finalStatus").getValue(String.class);

                // REMOVED blocking - Allow employee to check-in/out even if admin marked status

                if (pendingAction.equals("checkIn")) {
                    // Create new check-in pair
                    CheckInOutPair newPair = new CheckInOutPair(time);
                    newPair.checkInPhoto = photoUrl;
                    newPair.checkInLat = currentLat;
                    newPair.checkInLng = currentLng;
                    newPair.checkInAddress = currentAddress;

                    checkInOutPairs.add(newPair);
                    currentActiveCheckIn = time;
                    isCurrentlyCheckedIn = true;

                    Map<String, Object> updates = new HashMap<>();

                    final String checkInTimeForLambda = time;
                    final boolean isFirstCheckIn = (existingFirstCheckIn == null || existingFirstCheckIn.isEmpty());
                    final boolean isAdminMarked = "Admin".equals(existingMarkedBy);

                    // Update first check-in if this is the first one
                    if (isFirstCheckIn) {
                        firstCheckInTime = time;
                        updates.put("checkInTime", time);
                        updates.put("checkInPhoto", photoUrl);
                        updates.put("checkInLat", currentLat);
                        updates.put("checkInLng", currentLng);
                        updates.put("checkInAddress", currentAddress);

                        // Calculate and save late status only on first check-in
                        boolean isLate = isLateCheckIn(shiftStart, time);
                        lateStatus = isLate ? "Late" : "On Time";
                        updates.put("lateStatus", lateStatus);

                        // Update status - override admin's status if exists
                        String newStatus = isLate ? "Present (Late)" : "Present";
                        updates.put("status", newStatus);
                        updates.put("finalStatus", "Present");
                    } else {
                        firstCheckInTime = existingFirstCheckIn;
                    }

                    // Update markedBy field
                    if (isAdminMarked) {
                        updates.put("markedBy", "Admin+Employee");
                    } else {
                        updates.put("markedBy", "Employee");
                    }

                    final String finalLateStatus = lateStatus;
                    final int pairCount = checkInOutPairs.size();

                    // Save main data first
                    node.updateChildren(updates).addOnSuccessListener(aVoid -> {
                        // Then save pairs
                        savePairsToDatabase(node, () -> {
                            String toastMessage = "✅ Checked In";

                            if (pairCount == 1) {
                                // First check-in
                                if (finalLateStatus != null && finalLateStatus.equals("Late")) {
                                    toastMessage += " - Late\n(More than 15 min after shift start)";
                                }
                            } else {
                                // Multiple check-ins
                                toastMessage += " (Session " + pairCount + ")";
                            }

                            toast(toastMessage);

                            Log.d("CHECKIN_SUCCESS", "Check-in saved successfully");
                            Log.d("CHECKIN_SUCCESS", "Total pairs now: " + checkInOutPairs.size());

                            startWorkTimer();

                            // Reload attendance to refresh UI
                            new Handler().postDelayed(() -> loadTodayAttendance(), 500);
                        });
                    }).addOnFailureListener(e -> {
                        toast("❌ Failed to save check-in");
                        Log.e("CHECKIN_ERROR", "Error: " + e.getMessage());
                    });

                } else if (pendingAction.equals("checkOut")) {
                    // Complete the last check-in pair
                    if (!checkInOutPairs.isEmpty()) {
                        CheckInOutPair lastPair = checkInOutPairs.get(checkInOutPairs.size() - 1);

                        if (lastPair.checkOutTime == null || lastPair.checkOutTime.isEmpty()) {
                            lastPair.checkOutTime = time;
                            lastPair.checkOutPhoto = photoUrl;
                            lastPair.checkOutLat = currentLat;
                            lastPair.checkOutLng = currentLng;
                            lastPair.checkOutAddress = currentAddress;
                            lastPair.durationMinutes = getDiffMinutes(lastPair.checkInTime, time);

                            // Calculate total worked minutes from all pairs
                            totalWorkedMinutes = 0;
                            for (CheckInOutPair pair : checkInOutPairs) {
                                if (pair.checkOutTime != null && !pair.checkOutTime.isEmpty()) {
                                    totalWorkedMinutes += pair.durationMinutes;
                                }
                            }

                            // Update last check-out
                            lastCheckOutTime = time;
                            isCurrentlyCheckedIn = false;
                            currentActiveCheckIn = null;

                            // Calculate final status
                            String finalStatusCalc = calculateFinalStatus(totalWorkedMinutes);

                            // Add (Late) suffix if first check-in was late
                            String displayStatus = finalStatusCalc;
                            if ("Late".equals(lateStatus)) {
                                displayStatus = finalStatusCalc + " (Late)";
                            }

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("checkOutTime", time);
                            updates.put("checkOutPhoto", photoUrl);
                            updates.put("checkOutLat", currentLat);
                            updates.put("checkOutLng", currentLng);
                            updates.put("checkOutAddress", currentAddress);
                            updates.put("finalStatus", finalStatusCalc);
                            updates.put("status", displayStatus);
                            updates.put("totalMinutes", totalWorkedMinutes);
                            updates.put("totalHours", String.format("%.1f", totalWorkedMinutes / 60.0));
                            updates.put("shiftStart", shiftStart);
                            updates.put("shiftEnd", shiftEnd);
                            updates.put("shiftDurationMinutes", shiftDurationMinutes);

                            // Update markedBy field
                            if ("Admin".equals(existingMarkedBy)) {
                                updates.put("markedBy", "Admin+Employee");
                            } else {
                                updates.put("markedBy", "Employee");
                            }

                            final String finalDisplayStatus = displayStatus;
                            final String finalStatusCalcForLambda = finalStatusCalc;
                            final long finalTotalWorkedMinutes = totalWorkedMinutes;
                            final int finalPairCount = checkInOutPairs.size();
                            final long finalShiftDurationMinutes = shiftDurationMinutes;

                            // Save main data first
                            node.updateChildren(updates).addOnSuccessListener(aVoid -> {
                                // Then save pairs
                                savePairsToDatabase(node, () -> {
                                    String toastMsg = "✅ Checked Out";
                                    if (finalPairCount > 1) {
                                        toastMsg += " (Session " + finalPairCount + ")";
                                    }
                                    toastMsg += "\n" + finalDisplayStatus +
                                            "\nTotal Worked: " + (finalTotalWorkedMinutes/60) + "h " +
                                            (finalTotalWorkedMinutes%60) + "m";

                                    if (finalStatusCalcForLambda.equals("Half Day")) {
                                        long shortBy = finalShiftDurationMinutes - finalTotalWorkedMinutes;
                                        toastMsg += "\n⚠️ Short by " + (shortBy/60) + "h " + (shortBy%60) + "m";
                                    }

                                    toast(toastMsg);

                                    Log.d("CHECKOUT_SUCCESS", "Check-out saved successfully");
                                    Log.d("CHECKOUT_SUCCESS", "Total worked minutes: " + finalTotalWorkedMinutes);

                                    stopWorkTimer();

                                    // Reload attendance to refresh UI
                                    new Handler().postDelayed(() -> loadTodayAttendance(), 500);
                                });
                            }).addOnFailureListener(e -> {
                                toast("❌ Failed to save check-out");
                                Log.e("CHECKOUT_ERROR", "Error: " + e.getMessage());
                            });
                        } else {
                            toast("⚠️ Already checked out");
                        }
                    } else {
                        toast("⚠️ No check-in found");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toast("❌ Save failed");
            }
        });
    }


    private void savePairsToDatabase(DatabaseReference node, Runnable onComplete) {
        DatabaseReference pairsRef = node.child("checkInOutPairs");

        // Clear existing pairs first
        pairsRef.removeValue().addOnSuccessListener(aVoid -> {
            // Save all pairs
            int totalPairs = checkInOutPairs.size();
            if (totalPairs == 0) {
                if (onComplete != null) onComplete.run();
                return;
            }

            final int[] savedCount = {0};

            for (int i = 0; i < checkInOutPairs.size(); i++) {
                CheckInOutPair pair = checkInOutPairs.get(i);
                Map<String, Object> pairData = new HashMap<>();

                pairData.put("checkInTime", pair.checkInTime);
                if (pair.checkOutTime != null && !pair.checkOutTime.isEmpty()) {
                    pairData.put("checkOutTime", pair.checkOutTime);
                }
                if (pair.checkInPhoto != null) pairData.put("checkInPhoto", pair.checkInPhoto);
                if (pair.checkOutPhoto != null) pairData.put("checkOutPhoto", pair.checkOutPhoto);
                pairData.put("checkInLat", pair.checkInLat);
                pairData.put("checkInLng", pair.checkInLng);
                pairData.put("checkOutLat", pair.checkOutLat);
                pairData.put("checkOutLng", pair.checkOutLng);
                if (pair.checkInAddress != null) pairData.put("checkInAddress", pair.checkInAddress);
                if (pair.checkOutAddress != null) pairData.put("checkOutAddress", pair.checkOutAddress);
                pairData.put("durationMinutes", pair.durationMinutes);

                pairsRef.child("pair_" + (i + 1)).setValue(pairData).addOnSuccessListener(aVoid1 -> {
                    savedCount[0]++;
                    Log.d("SAVE_PAIRS", "Saved pair " + savedCount[0] + " of " + totalPairs);

                    if (savedCount[0] == totalPairs) {
                        Log.d("SAVE_PAIRS", "All pairs saved successfully");
                        if (onComplete != null) onComplete.run();
                    }
                }).addOnFailureListener(e -> {
                    Log.e("SAVE_PAIRS", "Failed to save pair: " + e.getMessage());
                });
            }
        }).addOnFailureListener(e -> {
            Log.e("SAVE_PAIRS", "Failed to clear pairs: " + e.getMessage());
            if (onComplete != null) onComplete.run();
        });
    }

    private boolean isLateCheckIn(String shiftStartTime, String checkInTime) {
        if (shiftStartTime == null || shiftStartTime.isEmpty()) {
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date shiftStart = sdf.parse(shiftStartTime);
            Date checkIn = sdf.parse(checkInTime);

            if (shiftStart == null || checkIn == null) return false;

            long diffMinutes = (checkIn.getTime() - shiftStart.getTime()) / 60000;

            boolean isLate = diffMinutes > 15;

            Log.d("LATE_CHECK", "Shift: " + shiftStartTime + ", CheckIn: " + checkInTime +
                    ", Diff: " + diffMinutes + " mins, Late: " + isLate);

            return isLate;
        } catch (Exception e) {
            Log.e("LATE_CHECK", "Error: " + e.getMessage());
            return false;
        }
    }

    private String calculateFinalStatus(long workedMinutes) {
        Log.d("STATUS_CALC", "========================================");
        Log.d("STATUS_CALC", "Worked Minutes: " + workedMinutes);
        Log.d("STATUS_CALC", "Shift Duration: " + shiftDurationMinutes + " mins");
        Log.d("STATUS_CALC", "Shift Time: " + (shiftDurationMinutes/60) + "h " +
                (shiftDurationMinutes%60) + "m");
        Log.d("STATUS_CALC", "Worked Time: " + (workedMinutes/60) + "h " +
                (workedMinutes%60) + "m");

        String status;

        if (workedMinutes >= shiftDurationMinutes) {
            status = "Present";
            Log.d("STATUS_CALC", "Result: PRESENT (worked >= shift duration)");
        } else if (workedMinutes > 0) {
            status = "Half Day";
            Log.d("STATUS_CALC", "Result: HALF DAY (0 < worked < shift duration)");
            Log.d("STATUS_CALC", "Short by: " + (shiftDurationMinutes - workedMinutes) + " mins");
        } else {
            status = "Absent";
            Log.d("STATUS_CALC", "Result: ABSENT (no work time)");
        }

        Log.d("STATUS_CALC", "========================================");
        return status;
    }

    private long getDiffMinutes(String start, String end) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date startDate = sdf.parse(start);
            Date endDate = sdf.parse(end);

            if (startDate == null || endDate == null) {
                Log.e("TIME_DIFF", "Null dates - start: " + start + ", end: " + end);
                return 0;
            }

            long diffMillis = endDate.getTime() - startDate.getTime();

            // Handle next day scenario
            if (diffMillis < 0) {
                diffMillis += 24 * 60 * 60 * 1000;
            }

            long minutes = diffMillis / 60000;

            // Safety check - if more than 24 hours, something is wrong
            if (minutes > 1440) {
                Log.e("TIME_DIFF", "Invalid time difference: " + minutes + " mins for " + start + " to " + end);
                return 0;
            }

            Log.d("TIME_DIFF", "Start: " + start + ", End: " + end + ", Diff: " + minutes + " mins");

            return Math.max(0, minutes);

        } catch (ParseException | NullPointerException e) {
            Log.e("TIME_DIFF", "Parse error: " + e.getMessage());
            return 0;
        }
    }

    private void startWorkTimer() {
        stopWorkTimer(); // Stop any existing timer

        workTimerHandler = new Handler();
        workTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCurrentlyCheckedIn && currentActiveCheckIn != null) {
                    String currentTime = getCurrentTime();
                    long currentSessionMins = getDiffMinutes(currentActiveCheckIn, currentTime);

                    // Calculate total from all completed pairs
                    long completedMins = 0;
                    for (int i = 0; i < checkInOutPairs.size() - 1; i++) { // Exclude last (active) pair
                        CheckInOutPair pair = checkInOutPairs.get(i);
                        if (pair.checkOutTime != null && !pair.checkOutTime.isEmpty()) {
                            completedMins += pair.durationMinutes;
                        }
                    }

                    long displayMins = completedMins + currentSessionMins;

                    // Safety check - if more than 24 hours, something is wrong
                    if (displayMins > 0 && displayMins < 1440) {
                        long hours = displayMins / 60;
                        long mins = displayMins % 60;
                        tvWorkHours.setText(String.format("%dh %dm", hours, mins));
                    } else if (displayMins >= 1440) {
                        // Error - reset to current session only
                        long hours = currentSessionMins / 60;
                        long mins = currentSessionMins % 60;
                        tvWorkHours.setText(String.format("%dh %dm", hours, mins));
                        Log.e("WORK_TIMER", "Invalid total minutes: " + displayMins);
                    }
                }
                if (workTimerHandler != null) {
                    workTimerHandler.postDelayed(this, 60000); // Update every minute
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
            tvCurrentTime.setText(new SimpleDateFormat("MMM dd, h:mm a",
                    Locale.getDefault()).format(new Date()));
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