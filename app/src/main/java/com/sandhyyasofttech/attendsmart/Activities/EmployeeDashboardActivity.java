
package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sandhyyasofttech.attendsmart.Models.GeoFencingConfig;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.GeoFencingHelper;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EmployeeDashboardActivity extends AppCompatActivity {

    // UI Elements
    // Add this with your other location variables

    private float currentAccuracy = 0.0f;
    private Handler trackingHandler;
    private Runnable trackingRunnable;
    private int snapshotCounter = 0;
    private GeoFencingConfig geoFencingConfig;
    private boolean geoFencingEnabled = false;
    private TextView tvWelcome, tvEmployeeName, tvCompany, tvRole, tvShift;
    private TextView tvTodayStatus, tvCurrentTime, tvLocation, tvWorkHours;
    private TextView tvCheckInTime, tvCheckOutTime;
    private CardView cardCheckIn, cardCheckOut, cardAttendanceReport, cardLogout;
    private View statusIndicator, locationStatusDot;
    private TextView tvChartTypeInfo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout legendItemsContainer;
    // Chart UI Elements
    private PieChart pieChart, donutChart;
    private BarChart barChart;
    private LineChart lineChart;
    private FrameLayout chartContainer;
    private LinearLayout legendContainer;
    private Button btnPieChart, btnBarChart, btnLineChart, btnDonutChart;
    private TextView tvChartPresent, tvChartLate, tvChartAbsent, tvChartHalfDay;
    private ProgressBar progressPresent, progressLate, progressAvgHours, progressOnTime;
    private TextView tvMonthPresent, tvMonthLate, tvAvgWorkHours, tvOnTimePercent;

    // Firebase
    private DatabaseReference employeesRef, attendanceRef, shiftsRef;
    private StorageReference attendancePhotoRef;

    // Data
    private String companyKey, employeeMobile, employeeName;
    private String shiftStart, shiftEnd;
    private int shiftDurationMinutes = 540;
    private int halfDayMinutes = 270;

    // Attendance tracking
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

    // Chart data
    private int presentDays = 0;
    private int lateDays = 0;
    private int absentDays = 0;
    private int halfDays = 0;
    private List<Float> weeklyHours = new ArrayList<>();
    private List<String> weekDays = new ArrayList<>();

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
    // Add this with other employee data variables
    private boolean requiresGeoFencing = true; // Default to true
    // Helper class
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
        loadGeoFencingConfig();
        fetchCompanyName();
        setupLocation();
        requestLocationPermission();
        loadEmployeeData();
        loadTodayAttendance();
        startClock();
        updateGreeting();

        // ===== FIXED: Initialize SwipeRefreshLayout FIRST =====
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Configure swipe refresh (only need to do this once)
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.blue_500),
                getResources().getColor(R.color.green_500),
                getResources().getColor(R.color.orange_500)
        );

        // Set background color for better visibility
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                getResources().getColor(android.R.color.white)
        );

        // Set size of the refresh indicator
        swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);

        // Set the refresh listener - only ONE listener needed
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Show a subtle toast for feedback
                Toast.makeText(EmployeeDashboardActivity.this,
                        "✓ Dashboard is up to date", Toast.LENGTH_SHORT).show();

                // Hide the refresh indicator after a short delay
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }, 1000); // 1 second delay
            }
        });

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.blue_800));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(
                        window.getDecorView().getSystemUiVisibility() &
                                ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }
    private void initViews() {
        // Initialize basic views
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
        tvChartTypeInfo = findViewById(R.id.tvChartTypeInfo);
        legendItemsContainer = findViewById(R.id.legendItemsContainer);
        // Initialize chart views
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        lineChart = findViewById(R.id.lineChart);
        donutChart = findViewById(R.id.donutChart);
        chartContainer = findViewById(R.id.chartContainer);
        legendContainer = findViewById(R.id.legendContainer);

        btnPieChart = findViewById(R.id.btnPieChart);
        btnBarChart = findViewById(R.id.btnBarChart);
        btnLineChart = findViewById(R.id.btnLineChart);
        btnDonutChart = findViewById(R.id.btnDonutChart);

        tvChartPresent = findViewById(R.id.tvChartPresent);
        tvChartLate = findViewById(R.id.tvChartLate);
        tvChartAbsent = findViewById(R.id.tvChartAbsent);
        tvChartHalfDay = findViewById(R.id.tvChartHalfDay);

        // Initialize progress bars and text views
        progressPresent = findViewById(R.id.progressPresent);
        progressLate = findViewById(R.id.progressLate);
        progressAvgHours = findViewById(R.id.progressAvgHours);
        progressOnTime = findViewById(R.id.progressOnTime);
        tvMonthPresent = findViewById(R.id.tvMonthPresent);
        tvMonthLate = findViewById(R.id.tvMonthLate);
        tvAvgWorkHours = findViewById(R.id.tvAvgWorkHours);
        tvOnTimePercent = findViewById(R.id.tvOnTimePercent);

        // Set click listeners
        cardCheckIn.setOnClickListener(v -> checkApprovedLeaveThenCheckIn());
        cardCheckOut.setOnClickListener(v -> tryCheckOut());
        cardAttendanceReport.setOnClickListener(v -> openAttendanceReport());
        cardLogout.setOnClickListener(v -> showLogoutConfirmation());

        View btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(EmployeeDashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        ImageView btnMyLeaves = findViewById(R.id.btnMyLeaves);
        btnMyLeaves.setOnClickListener(v -> openMyLeaves());

        findViewById(R.id.cardDailyReport).setOnClickListener(v -> openTodaysWork());
        findViewById(R.id.cardApplyLeave).setOnClickListener(v -> openApplyLeave());

        // Setup charts
        setupChartButtons();
        setupPieChart();
        setupBarChart();
        setupLineChart();
        setupDonutChart();
    }

    private void setupChartButtons() {
        View.OnClickListener chartTypeListener = v -> {
            // Reset all buttons
            btnPieChart.setSelected(false);
            btnBarChart.setSelected(false);
            btnLineChart.setSelected(false);
            btnDonutChart.setSelected(false);

            // Hide all charts
            pieChart.setVisibility(View.GONE);
            barChart.setVisibility(View.GONE);
            lineChart.setVisibility(View.GONE);
            donutChart.setVisibility(View.GONE);

            // Show selected chart and update info text
            if (v.getId() == R.id.btnPieChart) {
                btnPieChart.setSelected(true);
                pieChart.setVisibility(View.VISIBLE);
                tvChartTypeInfo.setText("Pie Chart - Shows percentage distribution");
                updatePieChart();
            } else if (v.getId() == R.id.btnBarChart) {
                btnBarChart.setSelected(true);
                barChart.setVisibility(View.VISIBLE);
                tvChartTypeInfo.setText("Bar Chart - Compares attendance categories");
                updateBarChart();
            } else if (v.getId() == R.id.btnLineChart) {
                btnLineChart.setSelected(true);
                lineChart.setVisibility(View.VISIBLE);
                tvChartTypeInfo.setText("Line Chart - Shows weekly work hours trend");
                updateLineChart();
            } else if (v.getId() == R.id.btnDonutChart) {
                btnDonutChart.setSelected(true);
                donutChart.setVisibility(View.VISIBLE);
                tvChartTypeInfo.setText("Donut Chart - Focuses on proportions with center details");
                updateDonutChart();
            }

            updateLegend();
        };

        btnPieChart.setOnClickListener(chartTypeListener);
        btnBarChart.setOnClickListener(chartTypeListener);
        btnLineChart.setOnClickListener(chartTypeListener);
        btnDonutChart.setOnClickListener(chartTypeListener);

        // Set pie chart as default
        btnPieChart.setSelected(true);
        pieChart.setVisibility(View.VISIBLE);
        tvChartTypeInfo.setText("Pie Chart - Shows percentage distribution");
    }

    // Update setupPieChart() - Show percentages
    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(20, 10, 20, 10);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(false); // No hole for pie chart
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Monthly\nPerformance");
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(Color.parseColor("#263238"));
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        // Don't draw legend inside chart
        pieChart.getLegend().setEnabled(false);

        // Animation
        pieChart.animateY(1400, Easing.EaseInOutQuad);
    }

    // Update setupBarChart() - Show values
    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setMaxVisibleValueCount(60);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setHighlightFullBarEnabled(false);
        barChart.setDrawBorders(false);
        barChart.setBorderColor(Color.LTGRAY);
        barChart.setBorderWidth(1f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(4);
        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.parseColor("#666666"));
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setAxisLineWidth(1f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.parseColor("#666666"));
        leftAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        leftAxis.setAxisLineWidth(1f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F0F0F0"));
        leftAxis.setGridLineWidth(1f);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        barChart.getLegend().setEnabled(false);

        // Animation
        barChart.animateY(1400, Easing.EaseInOutQuad);
    }

    // Update setupLineChart() - Show trend
    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawBorders(false);
        lineChart.setBorderColor(Color.LTGRAY);
        lineChart.setBorderWidth(1f);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#F0F0F0"));
        xAxis.setGridLineWidth(1f);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.parseColor("#666666"));
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setAxisLineWidth(1f);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.parseColor("#666666"));
        leftAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        leftAxis.setAxisLineWidth(1f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F0F0F0"));
        leftAxis.setGridLineWidth(1f);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        lineChart.getLegend().setEnabled(false);

        // Animation
        lineChart.animateY(1400, Easing.EaseInOutQuad);
    }

    // Update setupDonutChart() - Different from pie
    private void setupDonutChart() {
        donutChart.setUsePercentValues(true);
        donutChart.getDescription().setEnabled(false);
        donutChart.setExtraOffsets(20, 10, 20, 10);
        donutChart.setDragDecelerationFrictionCoef(0.95f);
        donutChart.setDrawHoleEnabled(true); // Donut has hole
        donutChart.setHoleColor(Color.WHITE);
        donutChart.setTransparentCircleColor(Color.WHITE);
        donutChart.setTransparentCircleAlpha(110);
        donutChart.setHoleRadius(58f);
        donutChart.setTransparentCircleRadius(61f);
        donutChart.setDrawCenterText(true);
        donutChart.setCenterText("Attendance\nOverview");
        donutChart.setCenterTextSize(14f);
        donutChart.setCenterTextColor(Color.parseColor("#666666"));
        donutChart.setRotationAngle(0);
        donutChart.setRotationEnabled(true);
        donutChart.setHighlightPerTapEnabled(true);

        donutChart.getLegend().setEnabled(false);

        // Animation
        donutChart.animateY(1400, Easing.EaseInOutQuad);
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

    private void loadGeoFencingConfig() {
        GeoFencingHelper.fetchGeoFencingConfig(companyKey,
                new GeoFencingHelper.GeoFencingConfigCallback() {
                    @Override
                    public void onSuccess(GeoFencingConfig config) {
                        geoFencingConfig = config;
                        geoFencingEnabled = config.isEnabled();

                        if (geoFencingEnabled) {
                            Log.d("GeoFencing", "Geo-fencing is ENABLED");
                            Log.d("GeoFencing", String.format("Office: %.6f, %.6f | Radius: %d m",
                                    config.getOfficeLat(), config.getOfficeLng(),
                                    config.getRadiusMeters()));
                        } else {
                            Log.d("GeoFencing", "Geo-fencing is DISABLED");
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("GeoFencing", "Failed to load config: " + error);
                        geoFencingEnabled = false;
                    }
                });
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
                    currentAccuracy = location.getAccuracy(); // Save the REAL accuracy

                    Log.d("GeoFencing", "Location - Lat: " + currentLat +
                            ", Lng: " + currentLng + ", Accuracy: " + currentAccuracy);

                    // Only mark as ready if accuracy is acceptable
                    if (currentAccuracy > 0 && currentAccuracy < 100) {
                        if (!locationReady) {
                            locationReady = true;
                            updateButtonStates();
                        }
                        getAddressFromLatLng(currentLat, currentLng);
                    } else {
                        Log.w("GeoFencing", "Poor accuracy: " + currentAccuracy);
                        if (currentAccuracy <= 0) {
                            // Request better location
                            requestFreshLocation();
                        }
                    }
                }
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
                if (!locationAvailability.isLocationAvailable()) {
                    Log.e("GeoFencing", "Location services became unavailable");
//                    toatryCheckst("📍 Location services unavailable");
                }
            }
        };
    }

    private void updateButtonStates() {
        boolean canCheckIn = locationReady && !isCurrentlyCheckedIn;
        boolean canCheckOut = locationReady && isCurrentlyCheckedIn;

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

        // Get current date
        String today = getTodayDate();

        // Get employee's weekly holiday and joining date first
        employeesRef.child(employeeMobile).child("info").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot infoSnapshot) {
                String joiningDate = infoSnapshot.child("joinDate").getValue(String.class);
                String weeklyHolidayStr = infoSnapshot.child("weeklyHoliday").getValue(String.class);

                // Convert weekly holiday string to Calendar constants
                int[] weeklyHolidays = convertDayNameToCalendarArray(weeklyHolidayStr);

                // Now load attendance data
                attendanceRef.orderByKey().startAt(currentMonth + "-01").endAt(currentMonth + "-31")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                int presentCount = 0;
                                int lateCount = 0;
                                int halfDayCount = 0;
                                int absentCount = 0;
                                int onTimeCount = 0;
                                long totalMinutes = 0;
                                int daysWithHours = 0;

                                // For charts
                                presentDays = 0;
                                lateDays = 0;
                                absentDays = 0;
                                halfDays = 0;
                                weeklyHours.clear();
                                weekDays.clear();

                                // Initialize weekly data
                                initWeeklyData();

                                // Track all dates in the month
                                Set<String> allDatesInMonth = new HashSet<>();
                                Calendar monthCal = Calendar.getInstance();
                                monthCal.set(Calendar.DAY_OF_MONTH, 1);

                                for (int i = 0; i < daysInMonth; i++) {
                                    monthCal.set(Calendar.DAY_OF_MONTH, i + 1);
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    String dateStr = sdf.format(monthCal.getTime());

                                    // Skip future dates
                                    if (dateStr.compareTo(today) > 0) {
                                        continue;
                                    }

                                    // Skip dates before joining
                                    if (joiningDate != null && dateStr.compareTo(joiningDate) < 0) {
                                        continue;
                                    }

                                    // Skip weekly holidays
                                    if (isHoliday(dateStr, weeklyHolidays)) {
                                        continue;
                                    }

                                    allDatesInMonth.add(dateStr);
                                }

                                // Process attendance records
                                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                                    String dateKey = dateSnapshot.getKey();

                                    // Skip if date is before joining or is holiday
                                    if (joiningDate != null && dateKey.compareTo(joiningDate) < 0) {
                                        continue;
                                    }
                                    if (isHoliday(dateKey, weeklyHolidays)) {
                                        continue;
                                    }
                                    if (dateKey.compareTo(today) > 0) {
                                        continue;
                                    }

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

                                        if (effectiveStatus != null) {
                                            Log.d("ATTENDANCE_DEBUG", "Date: " + dateKey +
                                                    ", Status: " + effectiveStatus +
                                                    ", LateStatus: " + lateStatus);

                                            // Remove this date from allDatesInMonth (it has attendance)
                                            allDatesInMonth.remove(dateKey);

                                            String statusLower = effectiveStatus.toLowerCase();

                                            // Check for HALF DAY first - this is the key fix
                                            if (statusLower.contains("half")) {
                                                halfDayCount++;
                                                halfDays++;  // Update chart variable
                                                presentCount++; // Half day counts as present for total attendance
                                                presentDays++;  // Update chart variable

                                                Log.d("HALF_DAY_DEBUG", "Half day counted for date: " + dateKey);
                                            }
                                            // Then check for PRESENT (including those with check-in)
                                            else if (statusLower.contains("present") ||
                                                    (checkInTime != null && !checkInTime.isEmpty())) {
                                                presentCount++;
                                                presentDays++;

                                                if (lateStatus != null && lateStatus.equals("Late") ||
                                                        statusLower.contains("late")) {
                                                    lateCount++;
                                                    lateDays++;
                                                } else {
                                                    onTimeCount++;
                                                }
                                            }
                                            // Check for ABSENT
                                            else if (statusLower.contains("absent")) {
                                                absentCount++;
                                                absentDays++;
                                            }

                                            // Calculate worked minutes (same as before)
                                            long dayMinutes = 0;

                                            if (minutes != null && minutes > 0 && minutes < 1440) {
                                                dayMinutes = minutes;
                                            } else if (checkInTime != null && !checkInTime.isEmpty() &&
                                                    checkOutTime != null && !checkOutTime.isEmpty()) {
                                                long calculatedMinutes = getDiffMinutes(checkInTime, checkOutTime);
                                                if (calculatedMinutes > 0 && calculatedMinutes < 1440) {
                                                    dayMinutes = calculatedMinutes;
                                                }
                                            } else if ("Admin".equals(markedBy)) {
                                                if (effectiveStatus.equals("Present")) {
                                                    dayMinutes = shiftDurationMinutes;
                                                } else if (effectiveStatus.equals("Half Day")) {
                                                    dayMinutes = shiftDurationMinutes / 2;
                                                }
                                            }

                                            if (dayMinutes > 0) {
                                                totalMinutes += dayMinutes;
                                                daysWithHours++;
                                            }

                                            // Collect weekly data for line chart
                                            collectWeeklyData(dateKey, dayMinutes);
                                        }
                                    }
                                }

                                // All remaining dates in allDatesInMonth are ABSENT (no attendance record)
                                for (String absentDate : allDatesInMonth) {
                                    // Skip future dates
                                    if (absentDate.compareTo(today) > 0) continue;

                                    absentCount++;
                                    absentDays++;
                                    Log.d("ABSENT_CALC", "Marked as absent: " + absentDate);
                                }

                                Log.d("STATS_DEBUG", "====================================");
                                Log.d("STATS_DEBUG", "Final Attendance Summary:");
                                Log.d("STATS_DEBUG", "Present Days: " + presentCount);
                                Log.d("STATS_DEBUG", "Late Days: " + lateCount);
                                Log.d("STATS_DEBUG", "Half Days: " + halfDayCount);
                                Log.d("STATS_DEBUG", "Absent Days: " + absentCount);
                                Log.d("STATS_DEBUG", "Half Days (Chart): " + halfDays);
                                Log.d("STATS_DEBUG", "====================================");

                                final int finalPresentCount = presentCount;
                                final int finalLateCount = lateCount;
                                final int finalHalfDayCount = halfDayCount;
                                final int finalAbsentCount = absentCount;
                                final int finalOnTimeCount = onTimeCount;
                                final long finalTotalMinutes = totalMinutes;
                                final int finalDaysWithHours = daysWithHours;
                                final int finalDaysInMonth = daysInMonth;

                                runOnUiThread(() -> {
                                    // Update chart summary
                                    tvChartPresent.setText(String.valueOf(presentDays));
                                    tvChartLate.setText(String.valueOf(lateDays));
                                    tvChartAbsent.setText(String.valueOf(absentDays));
                                    tvChartHalfDay.setText(String.valueOf(halfDays));

                                    // Update existing text views
                                    tvMonthPresent.setText(String.valueOf(finalPresentCount));
                                    tvMonthLate.setText(String.valueOf(finalLateCount));

                                    // Calculate working days in month (for progress bar max)
                                    int workingDays = getWorkingDaysInMonth();
                                    Log.d("WORKING_DAYS", "Working days in month: " + workingDays);

                                    // Update progress bars
                                    animateProgress(progressPresent, finalPresentCount, workingDays);

                                    int lateMax = finalPresentCount > 0 ? finalPresentCount : 1;
                                    animateProgress(progressLate, finalLateCount, lateMax);

                                    // Average hours calculation
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
                                        }
                                    } else {
                                        tvAvgWorkHours.setText("0.0");
                                        if (progressAvgHours != null) progressAvgHours.setProgress(0);
                                    }

                                    // On-time percentage
                                    if (finalPresentCount > 0) {
                                        int onTimePercent = (int) ((finalOnTimeCount * 100.0) / finalPresentCount);
                                        tvOnTimePercent.setText(String.valueOf(onTimePercent));
                                        animateProgress(progressOnTime, onTimePercent, 100);
                                    } else {
                                        tvOnTimePercent.setText("0");
                                        if (progressOnTime != null) progressOnTime.setProgress(0);
                                    }

                                    // Update all charts
                                    updatePieChart();
                                    updateBarChart();
                                    updateLineChart();
                                    updateDonutChart();
                                    updateLegend();
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("STATS_ERROR", "Firebase error: " + error.getMessage());
                                runOnUiThread(() -> {
                                    tvMonthPresent.setText("0");
                                    tvMonthLate.setText("0");
                                    tvAvgWorkHours.setText("0.0");
                                    tvOnTimePercent.setText("0");
                                    tvChartPresent.setText("0");
                                    tvChartLate.setText("0");
                                    tvChartAbsent.setText("0");
                                    tvChartHalfDay.setText("0");
                                    progressPresent.setProgress(0);
                                    progressLate.setProgress(0);
                                    progressAvgHours.setProgress(0);
                                    progressOnTime.setProgress(0);
                                });
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("STATS_ERROR", "Error loading employee info: " + error.getMessage());
            }
        });
    }
    private String getCheckInMessage() {
        if (!requiresGeoFencing) {
            return "📸 Taking photo for check-in (Field employee)...";
        } else if (firstCheckInTime == null) {
            boolean isLate = isLateCheckIn(shiftStart, getCurrentTime());
            return "📸 Taking photo for " + (isLate ? "Late" : "On Time") + " check-in...";
        } else {
            return "📸 Taking photo for check-in...";
        }
    }
    private boolean isHoliday(String dateStr, int[] weeklyHolidays) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date == null) return false;

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            for (int holiday : weeklyHolidays) {
                if (dayOfWeek == holiday) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e("HOLIDAY_CHECK", "Error checking holiday: " + e.getMessage());
        }
        return false;
    }

    // Convert day name to Calendar constants (same as in AttendanceReportActivity)
    private int[] convertDayNameToCalendarArray(String dayName) {
        if (dayName == null || dayName.trim().isEmpty()) {
            return new int[]{Calendar.SUNDAY}; // Default fallback
        }

        dayName = dayName.trim();

        switch (dayName) {
            case "Sunday":
                return new int[]{Calendar.SUNDAY};
            case "Monday":
                return new int[]{Calendar.MONDAY};
            case "Tuesday":
                return new int[]{Calendar.TUESDAY};
            case "Wednesday":
                return new int[]{Calendar.WEDNESDAY};
            case "Thursday":
                return new int[]{Calendar.THURSDAY};
            case "Friday":
                return new int[]{Calendar.FRIDAY};
            case "Saturday":
                return new int[]{Calendar.SATURDAY};
            default:
                Log.w("DAY_CONVERT", "Unknown day name: " + dayName + ", defaulting to Sunday");
                return new int[]{Calendar.SUNDAY};
        }
    }
    private void animateProgress(ProgressBar progressBar, int value, int max) {
        if (progressBar == null) return;

        progressBar.setMax(max);

        if (value > max) {
            value = max;
        }

        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", 0, value);
        animation.setDuration(1000);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    private int getWorkingDaysInMonth() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);

        // Get employee's weekly holiday
        employeesRef.child(employeeMobile).child("info").child("weeklyHoliday")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String weeklyHoliday = snapshot.getValue(String.class);
                        int[] holidays = convertDayNameToCalendarArray(weeklyHoliday);

                        cal.set(year, month, 1);
                        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

                        int workingDays = 0;

                        for (int day = 1; day <= lastDay; day++) {
                            cal.set(year, month, day);
                            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

                            boolean isHoliday = false;
                            for (int holiday : holidays) {
                                if (dayOfWeek == holiday) {
                                    isHoliday = true;
                                    break;
                                }
                            }

                            if (!isHoliday) {
                                workingDays++;
                            }
                        }

                        // Store in a final variable for use
                        final int finalWorkingDays = workingDays;
                        runOnUiThread(() -> {
                            // Update progress bar max if needed
                            if (progressPresent != null) {
                                progressPresent.setMax(finalWorkingDays);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("WORKING_DAYS", "Error loading weekly holiday");
                    }
                });

        // Default calculation without holiday info
        cal.set(year, month, 1);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int workingDays = 0;

        for (int day = 1; day <= lastDay; day++) {
            cal.set(year, month, day);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            // Exclude Saturday and Sunday by default
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                workingDays++;
            }
        }

        return workingDays;
    }

    // Also update the initWeeklyData() method
    private void initWeeklyData() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        // Get current week's dates
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        for (int i = 0; i < 7; i++) {
            String date = sdf.format(cal.getTime());
            String day = dayFormat.format(cal.getTime());
            weekDays.add(day);
            weeklyHours.add(0f);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
    private void collectWeeklyData(String date, Long minutes) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

            Date parsedDate = sdf.parse(date);
            if (parsedDate == null) return;

            String dayOfWeek = dayFormat.format(parsedDate);
            int dayIndex = weekDays.indexOf(dayOfWeek);

            if (dayIndex != -1 && minutes != null && minutes > 0) {
                float hours = minutes / 60.0f;
                float current = weeklyHours.get(dayIndex);
                weeklyHours.set(dayIndex, current + hours);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    private void updatePieChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();

        int totalDays = presentDays + lateDays + halfDays + absentDays;
        if (totalDays == 0) {
            pieChart.clear();
            pieChart.setCenterText("No Data\nAvailable");
            pieChart.setCenterTextSize(14f);
            pieChart.invalidate();
            return;
        }

        // Calculate percentages
        float presentPercent = (presentDays * 100f) / totalDays;
        float latePercent = (lateDays * 100f) / totalDays;
        float halfDayPercent = (halfDays * 100f) / totalDays;
        float absentPercent = (absentDays * 100f) / totalDays;

        // Only add entries with values > 0
        if (presentDays > 0) entries.add(new PieEntry(presentPercent, "Present"));
        if (lateDays > 0) entries.add(new PieEntry(latePercent, "Late"));
        if (halfDays > 0) entries.add(new PieEntry(halfDayPercent, "Half Day"));
        if (absentDays > 0) entries.add(new PieEntry(absentPercent, "Absent"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(8f);
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.5f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        // Colors with better contrast
        ArrayList<Integer> colors = new ArrayList<>();
        if (presentDays > 0) colors.add(Color.parseColor("#4CAF50")); // Green
        if (lateDays > 0) colors.add(Color.parseColor("#FF9800")); // Orange
        if (halfDays > 0) colors.add(Color.parseColor("#2196F3")); // Blue
        if (absentDays > 0) colors.add(Color.parseColor("#F44336")); // Red
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f%%", value);
            }
        });

        pieChart.setData(data);
        pieChart.setCenterText(String.format("Total\n%d Days", totalDays));
        pieChart.setCenterTextSize(16f);
        pieChart.invalidate();
        pieChart.animateY(1000, Easing.EaseInOutQuad);
    }

    // Update updateBarChart() - Show actual values
    private void updateBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();

        // Add data points
        entries.add(new BarEntry(0, presentDays));
        entries.add(new BarEntry(1, lateDays));
        entries.add(new BarEntry(2, halfDays));
        entries.add(new BarEntry(3, absentDays));

        BarDataSet dataSet = new BarDataSet(entries, "Attendance");
        dataSet.setColors(
                Color.parseColor("#4CAF50"), // Present - Green
                Color.parseColor("#FF9800"), // Late - Orange
                Color.parseColor("#2196F3"), // Half Day - Blue
                Color.parseColor("#F44336")  // Absent - Red
        );

        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // Enable highlighting
        dataSet.setHighLightColor(Color.parseColor("#FFD54F"));
        dataSet.setHighlightEnabled(true);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        barChart.setData(data);

        // Customize X-axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] labels = {"Present", "Late", "Half", "Absent"};
                int index = (int) value;
                if (index >= 0 && index < labels.length) {
                    return labels[index];
                }
                return "";
            }
        });
        xAxis.setLabelCount(4);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);

        // Set Y-axis minimum
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisMaximum(Math.max(10, getMaxValue() * 1.2f)); // Add 20% padding

        barChart.invalidate();
        barChart.animateY(1000, Easing.EaseInOutQuad);
    }
    private float getMaxValue() {
        return Math.max(Math.max(presentDays, lateDays), Math.max(halfDays, absentDays));
    }


    private void updateLineChart() {
        ArrayList<Entry> entries = new ArrayList<>();

        // Create entries for each day of week
        for (int i = 0; i < weeklyHours.size(); i++) {
            entries.add(new Entry(i, weeklyHours.get(i)));
        }

        if (entries.isEmpty()) {
            // Show default trend if no data
            for (int i = 0; i < 7; i++) {
                entries.add(new Entry(i, 0));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Weekly Hours");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#666666"));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#BBDEFB"));
        dataSet.setFillAlpha(100);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Enable highlighting
        dataSet.setHighLightColor(Color.parseColor("#FF9800"));
        dataSet.setHighlightEnabled(true);
        dataSet.setDrawHorizontalHighlightIndicator(true);
        dataSet.setDrawVerticalHighlightIndicator(false);

        LineData data = new LineData(dataSet);
        lineChart.setData(data);

        // Customize X-axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] labels = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
                int index = (int) value;
                if (index >= 0 && index < labels.length) {
                    return labels[index];
                }
                return "";
            }
        });
        xAxis.setLabelCount(7);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#F0F0F0"));

        // Customize Y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });
        leftAxis.setAxisMinimum(0f);

        lineChart.invalidate();
        lineChart.animateY(1000, Easing.EaseInOutQuad);
    }

    // Update updateDonutChart() - Different style from pie
    private void updateDonutChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();

        int totalDays = presentDays + lateDays + halfDays + absentDays;
        if (totalDays == 0) {
            donutChart.clear();
            donutChart.setCenterText("No Data");
            donutChart.setCenterTextSize(14f);
            donutChart.invalidate();
            return;
        }

        // Add entries with values > 0
        if (presentDays > 0) entries.add(new PieEntry(presentDays, "Present"));
        if (lateDays > 0) entries.add(new PieEntry(lateDays, "Late"));
        if (halfDays > 0) entries.add(new PieEntry(halfDays, "Half Day"));
        if (absentDays > 0) entries.add(new PieEntry(absentDays, "Absent"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(8f);
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.6f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        // Different colors than pie chart for distinction
        ArrayList<Integer> colors = new ArrayList<>();
        if (presentDays > 0) colors.add(Color.parseColor("#66BB6A")); // Light Green
        if (lateDays > 0) colors.add(Color.parseColor("#FFB74D")); // Light Orange
        if (halfDays > 0) colors.add(Color.parseColor("#64B5F6")); // Light Blue
        if (absentDays > 0) colors.add(Color.parseColor("#EF5350")); // Light Red
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.WHITE);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        donutChart.setData(data);

        // Show detailed info in center
        String centerText = String.format("Total: %d\n", totalDays);
        if (presentDays > 0) centerText += String.format("P: %d\n", presentDays);
        if (lateDays > 0) centerText += String.format("L: %d\n", lateDays);
        if (halfDays > 0) centerText += String.format("H: %d\n", halfDays);
        if (absentDays > 0) centerText += String.format("A: %d", absentDays);

        donutChart.setCenterText(centerText);
        donutChart.setCenterTextSize(11f);
        donutChart.setCenterTextColor(Color.parseColor("#666666"));

        donutChart.invalidate();
        donutChart.animateY(1000, Easing.EaseInOutQuad);
    }


    // Update updateLegend() - Better legend design

    private void updateLegend() {
        legendItemsContainer.removeAllViews();

        String[] labels = {"Present", "Late", "Half Day", "Absent"};
        int[] colors = {
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#FF9800"), // Orange
                Color.parseColor("#2196F3"), // Blue
                Color.parseColor("#F44336")  // Red
        };
        int[] values = {presentDays, lateDays, halfDays, absentDays};

        for (int i = 0; i < labels.length; i++) {
            if (values[i] > 0) {
                // Create legend item view
                View legendItem = LayoutInflater.from(this).inflate(R.layout.legend_item, null);
                View colorView = legendItem.findViewById(R.id.legendColor);
                TextView labelView = legendItem.findViewById(R.id.legendLabel);
                TextView valueView = legendItem.findViewById(R.id.legendValue);

                colorView.setBackgroundColor(colors[i]);
                labelView.setText(labels[i]);
                valueView.setText(String.valueOf(values[i]));

                // Add percentage if on pie/donut chart
                if (pieChart.getVisibility() == View.VISIBLE || donutChart.getVisibility() == View.VISIBLE) {
                    int total = presentDays + lateDays + halfDays + absentDays;
                    if (total > 0) {
                        float percentage = (values[i] * 100f) / total;
                        valueView.setText(String.format(Locale.getDefault(), "%d (%.1f%%)", values[i], percentage));
                    }
                }

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(12, 4, 12, 4);
                legendItem.setLayoutParams(params);

                legendItemsContainer.addView(legendItem);
            }
        }

        // If no data, show message
        if (legendItemsContainer.getChildCount() == 0) {
            TextView noDataText = new TextView(this);
            noDataText.setText("No attendance data available");
            noDataText.setTextSize(12f);
            noDataText.setTextColor(Color.GRAY);
            noDataText.setGravity(Gravity.CENTER);
            legendItemsContainer.addView(noDataText);
        }
    }

    // ... [REST OF YOUR EXISTING METHODS REMAIN THE SAME - loadTodayAttendance, checkApprovedLeaveThenCheckIn, tryCheckIn, tryCheckOut, openCamera, onActivityResult, saveAttendance, etc.]

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

                        Boolean requiresGeoFencingObj = info.child("requiresGeoFencing").getValue(Boolean.class);
                        requiresGeoFencing = requiresGeoFencingObj != null ? requiresGeoFencingObj : true;

                        saveEmployeeFcmToken();

                        tvEmployeeName.setText(employeeName != null ? employeeName : "User");
                        tvRole.setText(info.child("employeeRole").getValue(String.class));

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
                halfDayMinutes = shiftDurationMinutes / 2;
            }
        } catch (ParseException e) {
            shiftDurationMinutes = 540;
            halfDayMinutes = 270;
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
                        firstCheckInTime = snapshot.child("checkInTime").getValue(String.class);
                        lastCheckOutTime = snapshot.child("checkOutTime").getValue(String.class);
                        finalStatus = snapshot.child("status").getValue(String.class);
                        markedBy = snapshot.child("markedBy").getValue(String.class);
                        lateStatus = snapshot.child("lateStatus").getValue(String.class);

                        Long storedMinutes = snapshot.child("totalMinutes").getValue(Long.class);
                        totalWorkedMinutes = storedMinutes != null ? storedMinutes : 0;

                        checkInOutPairs.clear();
                        DataSnapshot pairsSnapshot = snapshot.child("checkInOutPairs");

                        boolean hasPairs = pairsSnapshot.exists() && pairsSnapshot.getChildrenCount() > 0;

                        if (hasPairs) {
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
                            CheckInOutPair pair = new CheckInOutPair(firstCheckInTime);
                            pair.checkInPhoto = snapshot.child("checkInPhoto").getValue(String.class);

                            Double lat = snapshot.child("checkInLat").getValue(Double.class);
                            pair.checkInLat = lat != null ? lat : 0;
                            Double lng = snapshot.child("checkInLng").getValue(Double.class);
                            pair.checkInLng = lng != null ? lng : 0;

                            pair.checkInAddress = snapshot.child("checkInAddress").getValue(String.class);

                            if (lastCheckOutTime != null && !lastCheckOutTime.isEmpty()) {
                                pair.checkOutTime = lastCheckOutTime;
                                pair.checkOutPhoto = snapshot.child("checkOutPhoto").getValue(String.class);

                                lat = snapshot.child("checkOutLat").getValue(Double.class);
                                pair.checkOutLat = lat != null ? lat : 0;
                                lng = snapshot.child("checkOutLng").getValue(Double.class);
                                pair.checkOutLng = lng != null ? lng : 0;

                                pair.checkOutAddress = snapshot.child("checkOutAddress").getValue(String.class);
                                pair.durationMinutes = getDiffMinutes(firstCheckInTime, lastCheckOutTime);

                                if (totalWorkedMinutes == 0) {
                                    totalWorkedMinutes = pair.durationMinutes;
                                }
                            }

                            checkInOutPairs.add(pair);
                        }

                        isCurrentlyCheckedIn = false;
                        currentActiveCheckIn = null;
                        if (!checkInOutPairs.isEmpty()) {
                            CheckInOutPair lastPair = checkInOutPairs.get(checkInOutPairs.size() - 1);
                            if (lastPair.checkOutTime == null || lastPair.checkOutTime.isEmpty()) {
                                isCurrentlyCheckedIn = true;
                                currentActiveCheckIn = lastPair.checkInTime;
                            }
                        }

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

    private void updateUI() {
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

        if (isCurrentlyCheckedIn && currentActiveCheckIn != null) {
            tvWorkHours.setText("Working...");
        } else if (totalWorkedMinutes > 0) {
            long hours = totalWorkedMinutes / 60;
            long mins = totalWorkedMinutes % 60;
            tvWorkHours.setText(String.format("%dh %dm", hours, mins));
        } else if (firstCheckInTime != null && lastCheckOutTime != null) {
            long calculatedMinutes = getDiffMinutes(firstCheckInTime, lastCheckOutTime);
            if (calculatedMinutes > 0 && calculatedMinutes < 1440) {
                long hours = calculatedMinutes / 60;
                long mins = calculatedMinutes % 60;
                tvWorkHours.setText(String.format("%dh %dm", hours, mins));
            } else {
                tvWorkHours.setText("0h 0m");
            }
        } else if (adminMarkedOnlyStatus) {
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


        // MODIFY THIS SECTION - Conditional geo-fencing check
        if (geoFencingEnabled && requiresGeoFencing && geoFencingConfig != null) {
            // Apply geo-fencing only if employee requires it
            // ... geo-fencing validation code ...
        } else if (geoFencingEnabled && !requiresGeoFencing) {
            // Field employee - show info message
            toast("🌍 Field employee: Can check-in from any location");
        }
        // NEW: Geofencing validation
        if (geoFencingEnabled && geoFencingConfig != null) {
            Log.d("GeoFencingHelper", "GPS Accuracy: " + String.format("%.2f", currentAccuracy) +
                    " meters (Threshold: " + geoFencingConfig.getAccuracyThreshold() + " meters)");

            // Check if we have valid accuracy from GPS
            if (currentAccuracy <= 0) {
                Log.e("GeoFencingHelper", "Invalid accuracy: " + currentAccuracy);
                toast("❌ GPS accuracy is 0. Please wait for GPS to stabilize.");

                // Request fresh location to get better accuracy
                requestFreshLocation();
                return;
            }

            // Check if accuracy is acceptable
            if (!GeoFencingHelper.isAccuracyAcceptable(currentAccuracy,
                    geoFencingConfig.getAccuracyThreshold())) {
                toast("❌ GPS accuracy too low (" + String.format("%.1f", currentAccuracy) +
                        "m). Need < " + geoFencingConfig.getAccuracyThreshold() + "m");
                return;
            }

            // Check if location coordinates are valid
            if (!GeoFencingHelper.isLocationValid(currentLat, currentLng, currentAccuracy)) {
                toast("❌ Invalid GPS coordinates.");
                return;
            }

            // Check if inside geofence
            boolean isInside = GeoFencingHelper.isInsideGeofence(
                    currentLat, currentLng,
                    geoFencingConfig.getOfficeLat(),
                    geoFencingConfig.getOfficeLng(),
                    geoFencingConfig.getRadiusMeters());

            if (!isInside) {
                float distance = GeoFencingHelper.calculateDistance(
                        currentLat, currentLng,
                        geoFencingConfig.getOfficeLat(),
                        geoFencingConfig.getOfficeLng());

                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Outside Office Area")
                        .setMessage("You are " + GeoFencingHelper.formatDistance(distance) +
                                " from office.\n\nAllowed radius: " +
                                geoFencingConfig.getRadiusMeters() + " meters" +
                                "\n\nYou must be inside the office area to check-in.")
                        .setPositiveButton("OK", null)
                        .show();

                return;
            }
        }

        String currentTime = getCurrentTime();

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

        // Geofencing validation for check-out
        if (geoFencingEnabled && geoFencingConfig != null) {
            Log.d("GeoFencingHelper", "GPS Accuracy: " + String.format("%.2f", currentAccuracy) +
                    " meters (Threshold: " + geoFencingConfig.getAccuracyThreshold() + " meters)");

            if (currentAccuracy <= 0) {
                Log.e("GeoFencingHelper", "Invalid accuracy: " + currentAccuracy);
                toast("❌ GPS accuracy is 0. Please wait for GPS to stabilize.");
                requestFreshLocation();
                return;
            }

            if (!GeoFencingHelper.isAccuracyAcceptable(currentAccuracy,
                    geoFencingConfig.getAccuracyThreshold())) {
                toast("❌ GPS accuracy too low (" + String.format("%.1f", currentAccuracy) +
                        "m). Need < " + geoFencingConfig.getAccuracyThreshold() + "m");
                return;
            }

            if (!GeoFencingHelper.isLocationValid(currentLat, currentLng, currentAccuracy)) {
                toast("❌ Invalid GPS coordinates.");
                return;
            }

            boolean isInside = GeoFencingHelper.isInsideGeofence(
                    currentLat, currentLng,
                    geoFencingConfig.getOfficeLat(),
                    geoFencingConfig.getOfficeLng(),
                    geoFencingConfig.getRadiusMeters());

            if (!isInside) {
                float distance = GeoFencingHelper.calculateDistance(
                        currentLat, currentLng,
                        geoFencingConfig.getOfficeLat(),
                        geoFencingConfig.getOfficeLng());

                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Outside Office Area")
                        .setMessage("You are " + GeoFencingHelper.formatDistance(distance) +
                                " from office.\n\nAllowed radius: " +
                                geoFencingConfig.getRadiusMeters() + " meters" +
                                "\n\nYou must be inside the office area to check-out.")
                        .setPositiveButton("OK", null)
                        .show();

                return;
            }
        }

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

        uploadPhotoAndSaveAttendance();
    }

    private void uploadPhotoAndSaveAttendance() {
        String today = getTodayDate();
        String time = getCurrentTime();

        // If photo is null, directly save attendance without photo
        if (currentPhotoBitmap == null) {
            toast("⚠️ Photo failed. Saving without photo...");
            saveAttendance(null, time);   // pass null photo
            return;
        }

        String photoName = employeeMobile + "_" + pendingAction + "_" +
                System.currentTimeMillis() + ".jpg";
        StorageReference photoRef = attendancePhotoRef.child(today).child(photoName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currentPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] data = baos.toByteArray();

        toast("⬆️ Uploading...");

        photoRef.putBytes(data)
                .addOnSuccessListener(task ->
                        photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Photo upload success → save with photo
                            saveAttendance(uri.toString(), time);
                        })
                )
                .addOnFailureListener(e -> {
                    // Photo upload failed → save WITHOUT photo
                    toast("⚠️ Photo upload failed. Saving attendance without photo.");
                    saveAttendance(null, time);
                });
    }
    // Modify saveAttendance() to include geofencing data
    private void saveAttendance(String photoUrl, String time) {
        if (employeeMobile == null) {
            toast("Profile not loaded");
            return;
        }

        String today = getTodayDate();
        DatabaseReference node = attendanceRef.child(today).child(employeeMobile);

        // Calculate geofencing data
        // Calculate geofencing data
        float distance;
        boolean insideGeofence;
        float accuracy;

        if (geoFencingEnabled && geoFencingConfig != null) {
            distance = GeoFencingHelper.calculateDistance(
                    currentLat, currentLng,
                    geoFencingConfig.getOfficeLat(),
                    geoFencingConfig.getOfficeLng());

            insideGeofence = GeoFencingHelper.isInsideGeofence(
                    currentLat, currentLng,
                    geoFencingConfig.getOfficeLat(),
                    geoFencingConfig.getOfficeLng(),
                    geoFencingConfig.getRadiusMeters());

            // Use the currentAccuracy from location updates
            accuracy = currentAccuracy;
        } else {
            insideGeofence = true;
            distance = 0;
            accuracy = 0;
        }

        node.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingMarkedBy = snapshot.child("markedBy").getValue(String.class);
                String existingFirstCheckIn = snapshot.child("checkInTime").getValue(String.class);

                if (pendingAction.equals("checkIn")) {
                    CheckInOutPair newPair = new CheckInOutPair(time);
                    newPair.checkInPhoto = photoUrl;
                    newPair.checkInLat = currentLat;
                    newPair.checkInLng = currentLng;
                    newPair.checkInAddress = currentAddress;

                    checkInOutPairs.add(newPair);
                    currentActiveCheckIn = time;
                    isCurrentlyCheckedIn = true;

                    Map<String, Object> updates = new HashMap<>();

                    final boolean isFirstCheckIn = (existingFirstCheckIn == null ||
                            existingFirstCheckIn.isEmpty());
                    final boolean isAdminMarked = "Admin".equals(existingMarkedBy);

                    if (isFirstCheckIn) {
                        firstCheckInTime = time;
                        updates.put("checkInTime", time);
                        updates.put("checkInPhoto", photoUrl);
                        updates.put("checkInLat", currentLat);
                        updates.put("checkInLng", currentLng);
                        updates.put("checkInAddress", currentAddress);

                        // NEW: Add geofencing data
                        updates.put("checkInAccuracy", accuracy);
                        updates.put("checkInDistance", distance);
                        updates.put("checkInInsideGeofence", insideGeofence);

                        boolean isLate = isLateCheckIn(shiftStart, time);
                        lateStatus = isLate ? "Late" : "On Time";
                        updates.put("lateStatus", lateStatus);

                        String newStatus = isLate ? "Present (Late)" : "Present";
                        updates.put("status", newStatus);
                        updates.put("finalStatus", "Present");
                    } else {
                        firstCheckInTime = existingFirstCheckIn;
                    }

                    if (isAdminMarked) {
                        updates.put("markedBy", "Admin+Employee");
                    } else {
                        updates.put("markedBy", "Employee");
                    }

                    final String finalLateStatus = lateStatus;
                    final int pairCount = checkInOutPairs.size();

                    node.updateChildren(updates).addOnSuccessListener(aVoid -> {
                        savePairsToDatabase(node, () -> {
                            String toastMessage = "✅ Checked In";

                            if (pairCount == 1) {
                                if (finalLateStatus != null && finalLateStatus.equals("Late")) {
                                    toastMessage += " - Late\n(More than 15 min after shift start)";
                                }
                            } else {
                                toastMessage += " (Session " + pairCount + ")";
                            }

                            if (geoFencingEnabled) {
                                toastMessage += "\n" + GeoFencingHelper.formatDistance(distance) +
                                        " from office";
                            }

                            toast(toastMessage);
                            startWorkTimer();

                            // Start periodic tracking if enabled
                            if (geoFencingEnabled && geoFencingConfig.isTrackingEnabled()) {
                                startLocationTracking();
                            }

                            new Handler().postDelayed(() -> {
                                loadTodayAttendance();
                                loadMonthlyAttendance();
                            }, 500);
                        });
                    }).addOnFailureListener(e -> {
                        toast("❌ Failed to save check-in");
                    });

                } else if (pendingAction.equals("checkOut")) {
                    if (!checkInOutPairs.isEmpty()) {
                        CheckInOutPair lastPair = checkInOutPairs.get(checkInOutPairs.size() - 1);

                        if (lastPair.checkOutTime == null || lastPair.checkOutTime.isEmpty()) {
                            lastPair.checkOutTime = time;
                            lastPair.checkOutPhoto = photoUrl;
                            lastPair.checkOutLat = currentLat;
                            lastPair.checkOutLng = currentLng;
                            lastPair.checkOutAddress = currentAddress;
                            lastPair.durationMinutes = getDiffMinutes(lastPair.checkInTime, time);

                            totalWorkedMinutes = 0;
                            for (CheckInOutPair pair : checkInOutPairs) {
                                if (pair.checkOutTime != null && !pair.checkOutTime.isEmpty()) {
                                    totalWorkedMinutes += pair.durationMinutes;
                                }
                            }

                            lastCheckOutTime = time;
                            isCurrentlyCheckedIn = false;
                            currentActiveCheckIn = null;

                            String finalStatusCalc = calculateFinalStatus(totalWorkedMinutes);
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

                            // NEW: Add geofencing data
                            updates.put("checkOutAccuracy", accuracy);
                            updates.put("checkOutDistance", distance);
                            updates.put("checkOutInsideGeofence", insideGeofence);

                            updates.put("finalStatus", finalStatusCalc);
                            updates.put("status", displayStatus);
                            updates.put("totalMinutes", totalWorkedMinutes);
                            updates.put("totalHours", String.format("%.1f", totalWorkedMinutes / 60.0));
                            updates.put("shiftStart", shiftStart);
                            updates.put("shiftEnd", shiftEnd);
                            updates.put("shiftDurationMinutes", shiftDurationMinutes);

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

                            node.updateChildren(updates).addOnSuccessListener(aVoid -> {
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

                                    if (geoFencingEnabled) {
                                        toastMsg += "\n" + GeoFencingHelper.formatDistance(distance) +
                                                " from office";
                                    }

                                    toast(toastMsg);
                                    stopWorkTimer();
                                    stopLocationTracking(); // Stop tracking on checkout

                                    new Handler().postDelayed(() -> {
                                        loadTodayAttendance();
                                        loadMonthlyAttendance();
                                    }, 500);
                                });
                            }).addOnFailureListener(e -> {
                                toast("❌ Failed to save check-out");
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
    }    private void savePairsToDatabase(DatabaseReference node, Runnable onComplete) {
        DatabaseReference pairsRef = node.child("checkInOutPairs");

        pairsRef.removeValue().addOnSuccessListener(aVoid -> {
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
                    if (savedCount[0] == totalPairs) {
                        if (onComplete != null) onComplete.run();
                    }
                }).addOnFailureListener(e -> {});
            }
        }).addOnFailureListener(e -> {
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
            return diffMinutes > 15;
        } catch (Exception e) {
            return false;
        }
    }

    private String calculateFinalStatus(long workedMinutes) {
        String status;

        if (workedMinutes >= shiftDurationMinutes) {
            status = "Present";
        } else if (workedMinutes > 0) {
            status = "Half Day";
        } else {
            status = "Absent";
        }

        return status;
    }

    private long getDiffMinutes(String start, String end) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date startDate = sdf.parse(start);
            Date endDate = sdf.parse(end);

            if (startDate == null || endDate == null) {
                return 0;
            }

            long diffMillis = endDate.getTime() - startDate.getTime();

            if (diffMillis < 0) {
                diffMillis += 24 * 60 * 60 * 1000;
            }

            long minutes = diffMillis / 60000;

            if (minutes > 1440) {
                return 0;
            }

            return Math.max(0, minutes);

        } catch (ParseException | NullPointerException e) {
            return 0;
        }
    }

    private void startWorkTimer() {
        stopWorkTimer();

        workTimerHandler = new Handler();
        workTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCurrentlyCheckedIn && currentActiveCheckIn != null) {
                    String currentTime = getCurrentTime();
                    long currentSessionMins = getDiffMinutes(currentActiveCheckIn, currentTime);

                    long completedMins = 0;
                    for (int i = 0; i < checkInOutPairs.size() - 1; i++) {
                        CheckInOutPair pair = checkInOutPairs.get(i);
                        if (pair.checkOutTime != null && !pair.checkOutTime.isEmpty()) {
                            completedMins += pair.durationMinutes;
                        }
                    }

                    long displayMins = completedMins + currentSessionMins;

                    if (displayMins > 0 && displayMins < 1440) {
                        long hours = displayMins / 60;
                        long mins = displayMins % 60;
                        tvWorkHours.setText(String.format("%dh %dm", hours, mins));
                    } else if (displayMins >= 1440) {
                        long hours = currentSessionMins / 60;
                        long mins = currentSessionMins % 60;
                        tvWorkHours.setText(String.format("%dh %dm", hours, mins));
                    }
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
            tvCurrentTime.setText(new SimpleDateFormat("MMM dd, h:mm a",
                    Locale.getDefault()).format(new Date()));
            timeHandler.postDelayed(timeRunnable, 1000);
        };
        timeHandler.post(timeRunnable);
    }

    private void getCurrentLocation() {
        // Check if GPS is enabled
        if (!isGpsEnabled()) {
            toast("⚠️ Please enable GPS");
            tvLocation.setText("GPS disabled");
            return;
        }

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
                float accuracy = location.getAccuracy();

                Log.d("GeoFencing", "Location obtained - Lat: " + currentLat +
                        ", Lng: " + currentLng + ", Accuracy: " + accuracy);

                // Check if accuracy is valid
                if (accuracy > 0) {
                    locationReady = true;
                    getAddressFromLatLng(currentLat, currentLng);
                    updateButtonStates();
                    toast("✓ GPS signal acquired (" + String.format("%.1f", accuracy) + "m accuracy)");
                } else {
                    // If accuracy is 0, try to get better location
                    Log.w("GeoFencing", "GPS accuracy is 0.0, requesting fresh location...");
                    toast("🔄 Getting better GPS signal...");
                    requestFreshLocation();
                }
            } else {
                Log.e("GeoFencing", "Location is null");
                toast("📍 Waiting for GPS signal...");
            }
            startLocationUpdates();
        }).addOnFailureListener(e -> {
            Log.e("GeoFencing", "GPS Error: " + e.getMessage());
            toast("⚠️ GPS Error: " + e.getMessage());
            startLocationUpdates();
        });
    }

    private boolean isGpsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            return locationManager != null && locationManager.isLocationEnabled();
        } else {
            int locationMode = Settings.Secure.LOCATION_MODE_OFF;
            try {
                locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
    }
    // Add this new method to request fresh location
    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        toast("🔄 Getting fresh GPS signal...");

        // Create a high-accuracy one-time request
        LocationRequest oneTimeRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateDistanceMeters(0)
                .build();

        LocationCallback oneTimeCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    currentAccuracy = location.getAccuracy(); // Update accuracy

                    Log.d("FreshLocation", "Fresh location - Lat: " + currentLat +
                            ", Lng: " + currentLng + ", Accuracy: " + currentAccuracy);

                    if (currentAccuracy > 0 && currentAccuracy < 50) {
                        locationReady = true;
                        getAddressFromLatLng(currentLat, currentLng);
                        updateButtonStates();
                        toast("✓ GPS signal acquired (" +
                                String.format("%.1f", currentAccuracy) + "m accuracy)");
                    } else {
                        Log.w("FreshLocation", "Accuracy still poor: " + currentAccuracy);
                        toast("⚠️ GPS accuracy still " + String.format("%.1f", currentAccuracy) + "m");
                    }
                }

                // Remove this callback after use
                fusedLocationClient.removeLocationUpdates(this);
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                if (!locationAvailability.isLocationAvailable()) {
                    Log.e("FreshLocation", "Location not available");
                    toast("📍 Location services unavailable");
                }
            }
        };

        // Request fresh location
        fusedLocationClient.requestLocationUpdates(oneTimeRequest, oneTimeCallback, null);

        // Auto-remove callback after 10 seconds
        new Handler().postDelayed(() -> {
            try {
                fusedLocationClient.removeLocationUpdates(oneTimeCallback);
            } catch (Exception e) {
                Log.e("FreshLocation", "Error removing callback: " + e.getMessage());
            }
        }, 10000);
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
                        // Display full address with 2 lines max
                        String address = currentAddress;
                        // You can optionally truncate if too long, but show as much as possible
                        if (address.length() > 80) {
                            address = address.substring(0, 77) + "...";
                        }
                        tvLocation.setText(address);
                        locationStatusDot.setBackgroundResource(R.drawable.status_dot_active);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvLocation.setText(String.format(Locale.getDefault(), "%.4f, %.4f", lat, lng));
                    locationStatusDot.setBackgroundResource(R.drawable.status_dot_active);
                });
            }
        }).start();
    }

    private boolean isDateBetween(String today, String from, String to) {
        if (from == null || to == null) return false;
        return today.compareTo(from) >= 0 && today.compareTo(to) <= 0;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationReady) startLocationUpdates();
        loadTodayAttendance();
        loadMonthlyAttendance();
    }

    private void startLocationTracking() {
        if (!geoFencingEnabled || !geoFencingConfig.isTrackingEnabled()) {
            return;
        }

        stopLocationTracking(); // Stop any existing tracking

        trackingHandler = new Handler();
        trackingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCurrentlyCheckedIn && locationReady) {
                    saveLocationSnapshot();
                }

                if (trackingHandler != null && isCurrentlyCheckedIn) {
                    trackingHandler.postDelayed(this, geoFencingConfig.getTrackingInterval());
                }
            }
        };

        // Start first snapshot after initial interval
        trackingHandler.postDelayed(trackingRunnable, geoFencingConfig.getTrackingInterval());

        Log.d("LocationTracking", "Started periodic tracking every " +
                (geoFencingConfig.getTrackingInterval() / 60000) + " minutes");
    }

    // Stop tracking
    private void stopLocationTracking() {
        if (trackingHandler != null && trackingRunnable != null) {
            trackingHandler.removeCallbacks(trackingRunnable);
            trackingHandler = null;
            trackingRunnable = null;
            snapshotCounter = 0;
            Log.d("LocationTracking", "Stopped periodic tracking");
        }
    }

    // Save location snapshot
    private void saveLocationSnapshot() {
        if (!locationReady || employeeMobile == null) {
            return;
        }

        String today = getTodayDate();
        String timestamp = new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date());

        float distance;
        boolean insideGeofence;
        float accuracy = 0;

        if (geoFencingConfig != null) {
            distance = GeoFencingHelper.calculateDistance(
                    currentLat, currentLng,
                    geoFencingConfig.getOfficeLat(),
                    geoFencingConfig.getOfficeLng());

            insideGeofence = GeoFencingHelper.isInsideGeofence(
                    currentLat, currentLng,
                    geoFencingConfig.getOfficeLat(),
                    geoFencingConfig.getOfficeLng(),
                    geoFencingConfig.getRadiusMeters());

            Location location = new Location("");
            location.setLatitude(currentLat);
            location.setLongitude(currentLng);
            accuracy = location.getAccuracy();
        } else {
            insideGeofence = true;
            distance = 0;
        }

        snapshotCounter++;

        Map<String, Object> snapshotData = new HashMap<>();
        snapshotData.put("timestamp", timestamp);
        snapshotData.put("lat", currentLat);
        snapshotData.put("lng", currentLng);
        snapshotData.put("accuracy", accuracy);
        snapshotData.put("distance", distance);
        snapshotData.put("insideGeofence", insideGeofence);

        attendanceRef.child(today)
                .child(employeeMobile)
                .child("locationSnapshots")
                .child("snapshot_" + snapshotCounter)
                .setValue(snapshotData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("LocationTracking", "Snapshot saved: " + timestamp +
                            " | Distance: " + String.format("%.1f", distance) + "m" +
                            " | Inside: " + insideGeofence);
                })
                .addOnFailureListener(e -> {
                    Log.e("LocationTracking", "Failed to save snapshot: " + e.getMessage());
                });
    }

    // Modify onDestroy() to stop tracking
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null) timeHandler.removeCallbacks(timeRunnable);
        stopWorkTimer();
        stopLocationTracking(); // ADD THIS LINE
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}