package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.Adapters.PunchDetailsAdapter;
import com.sandhyyasofttech.attendsmart.Models.EmployeePunchModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.List;

public class AttendanceDayDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceDetails";

    private RecyclerView rvDetails;
    private TextView tvDate, tvTotalCount, tvOnTimeCount, tvLateCount, tvSessionCount;

    private DatabaseReference attendanceRef, employeesRef;
    private PrefManager prefManager;
    private String companyKey, selectedDate;

    private PunchDetailsAdapter adapter;
    private final List<EmployeePunchModel> employeeList = new ArrayList<>();

    private static final String OFFICE_START_TIME = "09:30";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_day_details);

        selectedDate = getIntent().getStringExtra("date");
        if (selectedDate == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();

        prefManager = new PrefManager(this);
        companyKey = prefManager.getCompanyKey();

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        attendanceRef = db.getReference("Companies")
                .child(companyKey)
                .child("attendance")
                .child(selectedDate);

        employeesRef = db.getReference("Companies")
                .child(companyKey)
                .child("employees");

        loadAttendance();
    }

    private void initViews() {
        tvDate = findViewById(R.id.tvDateHeader);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvOnTimeCount = findViewById(R.id.tvOnTimeCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        tvSessionCount = findViewById(R.id.tvAbsentCount);
        rvDetails = findViewById(R.id.rvDetails);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        tvDate.setText("Attendance - " + selectedDate);
    }

    private void setupRecyclerView() {
        adapter = new PunchDetailsAdapter(employeeList);
        rvDetails.setLayoutManager(new LinearLayoutManager(this));
        rvDetails.setAdapter(adapter);
    }

    private void loadAttendance() {
        employeeList.clear();

        String employeeMobile = prefManager.getEmployeeMobile();
        if (employeeMobile == null || employeeMobile.isEmpty()) {
            Toast.makeText(this, "Employee not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference empDayRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("attendance")
                .child(selectedDate)
                .child(employeeMobile);

        empDayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot empSnap) {

                if (!empSnap.exists()) {
                    Toast.makeText(AttendanceDayDetailsActivity.this,
                            "No attendance for this day", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    updateStatistics();
                    return;
                }

                // Get main attendance info
                String mainLateStatus = empSnap.child("lateStatus").getValue(String.class);
                String mainStatus = empSnap.child("status").getValue(String.class);
                String markedBy = empSnap.child("markedBy").getValue(String.class);

                // ✅ STEP 1: Fetch from checkInOutPairs structure
                DataSnapshot pairsSnapshot = empSnap.child("checkInOutPairs");
                boolean hasPairs = pairsSnapshot.exists() && pairsSnapshot.getChildrenCount() > 0;

                Log.d(TAG, "========================================");
                Log.d(TAG, "Has pairs node: " + hasPairs);

                if (hasPairs) {
                    // ✅ Load from pairs structure
                    List<String> allCheckIns = new ArrayList<>();
                    List<String> allCheckOuts = new ArrayList<>();
                    List<PairData> allPairData = new ArrayList<>();

                    // Collect all data
                    for (DataSnapshot pairSnap : pairsSnapshot.getChildren()) {
                        PairData pairData = new PairData();
                        pairData.checkInTime = pairSnap.child("checkInTime").getValue(String.class);
                        pairData.checkOutTime = pairSnap.child("checkOutTime").getValue(String.class);
                        pairData.checkInPhoto = pairSnap.child("checkInPhoto").getValue(String.class);
                        pairData.checkOutPhoto = pairSnap.child("checkOutPhoto").getValue(String.class);
                        pairData.checkInAddress = pairSnap.child("checkInAddress").getValue(String.class);
                        pairData.checkOutAddress = pairSnap.child("checkOutAddress").getValue(String.class);

                        Double lat = pairSnap.child("checkInLat").getValue(Double.class);
                        pairData.checkInLat = lat != null ? lat : 0.0;
                        Double lng = pairSnap.child("checkInLng").getValue(Double.class);
                        pairData.checkInLng = lng != null ? lng : 0.0;

                        lat = pairSnap.child("checkOutLat").getValue(Double.class);
                        pairData.checkOutLat = lat != null ? lat : 0.0;
                        lng = pairSnap.child("checkOutLng").getValue(Double.class);
                        pairData.checkOutLng = lng != null ? lng : 0.0;

                        Boolean gps = pairSnap.child("checkInGPS").getValue(Boolean.class);
                        pairData.checkInGPS = gps != null ? gps : false;
                        gps = pairSnap.child("checkOutGPS").getValue(Boolean.class);
                        pairData.checkOutGPS = gps != null ? gps : false;

                        allPairData.add(pairData);

                        if (pairData.checkInTime != null && !pairData.checkInTime.isEmpty()) {
                            allCheckIns.add(pairData.checkInTime);
                        }
                        if (pairData.checkOutTime != null && !pairData.checkOutTime.isEmpty()) {
                            allCheckOuts.add(pairData.checkOutTime);
                        }

                        Log.d(TAG, pairSnap.getKey() + " - CheckIn: " + pairData.checkInTime +
                                ", CheckOut: " + pairData.checkOutTime);
                    }

                    Log.d(TAG, "Total check-ins found: " + allCheckIns.size());
                    Log.d(TAG, "Total check-outs found: " + allCheckOuts.size());

                    // ✅ STEP 2: Match and merge duplicate check-ins/check-outs
                    List<PairData> mergedPairs = new ArrayList<>();

                    for (PairData pairData : allPairData) {
                        if (pairData.checkInTime == null || pairData.checkInTime.isEmpty()) {
                            continue;
                        }

                        // Check if this check-in already exists in merged pairs
                        boolean alreadyMerged = false;
                        for (PairData merged : mergedPairs) {
                            if (pairData.checkInTime.equals(merged.checkInTime)) {
                                alreadyMerged = true;
                                // If current pair has check-out but merged doesn't, update it
                                if ((merged.checkOutTime == null || merged.checkOutTime.isEmpty()) &&
                                        pairData.checkOutTime != null && !pairData.checkOutTime.isEmpty()) {
                                    merged.checkOutTime = pairData.checkOutTime;
                                    merged.checkOutPhoto = pairData.checkOutPhoto;
                                    merged.checkOutAddress = pairData.checkOutAddress;
                                    merged.checkOutLat = pairData.checkOutLat;
                                    merged.checkOutLng = pairData.checkOutLng;
                                    merged.checkOutGPS = pairData.checkOutGPS;
                                    Log.d(TAG, "✓ Merged check-out into existing check-in: " +
                                            pairData.checkInTime);
                                }
                                break;
                            }
                        }

                        if (!alreadyMerged) {
                            mergedPairs.add(pairData);
                            Log.d(TAG, "✓ Added new pair: " + pairData.checkInTime +
                                    " → " + pairData.checkOutTime);
                        }
                    }

                    Log.d(TAG, "Total merged pairs: " + mergedPairs.size());

                    // ✅ STEP 3: Create models for ALL pairs (complete AND incomplete)
                    int pairIndex = 0;
                    for (PairData pairData : mergedPairs) {
                        // Add if check-in exists (check-out is optional)
                        if (pairData.checkInTime != null && !pairData.checkInTime.isEmpty()) {

                            pairIndex++;

                            EmployeePunchModel model = new EmployeePunchModel();
                            model.mobile = employeeMobile;
                            model.checkInTime = pairData.checkInTime;
                            model.checkOutTime = pairData.checkOutTime; // Can be null
                            model.checkInPhoto = pairData.checkInPhoto;
                            model.checkOutPhoto = pairData.checkOutPhoto;
                            model.checkInAddress = pairData.checkInAddress;
                            model.checkOutAddress = pairData.checkOutAddress;
                            model.checkInLat = pairData.checkInLat;
                            model.checkInLng = pairData.checkInLng;
                            model.checkOutLat = pairData.checkOutLat;
                            model.checkOutLng = pairData.checkOutLng;
                            model.checkInGPS = pairData.checkInGPS;
                            model.checkOutGPS = pairData.checkOutGPS;

                            // First pair gets late status
                            if (pairIndex == 1) {
                                model.lateStatus = mainLateStatus;
                                model.status = mainStatus;
                            } else {
                                model.lateStatus = "On Time";
                                model.status = "Present";
                            }

                            model.calculateWorkingHours();
                            employeeList.add(model);

                            if (pairData.checkOutTime != null && !pairData.checkOutTime.isEmpty()) {
                                Log.d(TAG, "✓✓ Added COMPLETE session " + pairIndex + ": " +
                                        pairData.checkInTime + " → " + pairData.checkOutTime);
                            } else {
                                Log.d(TAG, "✓ Added INCOMPLETE session " + pairIndex + ": " +
                                        pairData.checkInTime + " → (no checkout yet)");
                            }
                        }
                    }
                }

                // ✅ FALLBACK: Check main fields if no pairs found
                if (employeeList.isEmpty()) {
                    String checkInTime = empSnap.child("checkInTime").getValue(String.class);
                    String checkOutTime = empSnap.child("checkOutTime").getValue(String.class);

                    Log.d(TAG, "No pairs found. Checking main fields...");
                    Log.d(TAG, "Main CheckIn: " + checkInTime + ", CheckOut: " + checkOutTime);

                    // Add if check-in exists (check-out is optional)
                    if (checkInTime != null && !checkInTime.isEmpty()) {

                        EmployeePunchModel model = new EmployeePunchModel();
                        model.mobile = employeeMobile;
                        model.checkInTime = checkInTime;
                        model.checkOutTime = checkOutTime; // Can be null
                        model.checkInPhoto = empSnap.child("checkInPhoto").getValue(String.class);
                        model.checkOutPhoto = empSnap.child("checkOutPhoto").getValue(String.class);
                        model.checkInAddress = empSnap.child("checkInAddress").getValue(String.class);
                        model.checkOutAddress = empSnap.child("checkOutAddress").getValue(String.class);

                        Double lat = empSnap.child("checkInLat").getValue(Double.class);
                        model.checkInLat = lat != null ? lat : 0.0;
                        Double lng = empSnap.child("checkInLng").getValue(Double.class);
                        model.checkInLng = lng != null ? lng : 0.0;

                        lat = empSnap.child("checkOutLat").getValue(Double.class);
                        model.checkOutLat = lat != null ? lat : 0.0;
                        lng = empSnap.child("checkOutLng").getValue(Double.class);
                        model.checkOutLng = lng != null ? lng : 0.0;

                        Boolean gps = empSnap.child("checkInGPS").getValue(Boolean.class);
                        model.checkInGPS = gps != null ? gps : false;
                        gps = empSnap.child("checkOutGPS").getValue(Boolean.class);
                        model.checkOutGPS = gps != null ? gps : false;

                        model.status = mainStatus;
                        model.lateStatus = mainLateStatus;
                        model.calculateWorkingHours();

                        employeeList.add(model);

                        if (checkOutTime != null && !checkOutTime.isEmpty()) {
                            Log.d(TAG, "✓ Added COMPLETE from main fields");
                        } else {
                            Log.d(TAG, "✓ Added INCOMPLETE from main fields (no checkout)");
                        }
                    }
                }

                Log.d(TAG, "========================================");
                Log.d(TAG, "FINAL: Total complete sessions: " + employeeList.size());
                Log.d(TAG, "========================================");

                if (!employeeList.isEmpty()) {
                    fetchEmployeeName(employeeList.get(0));
                } else {
                    updateStatistics();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Attendance fetch failed: " + error.getMessage());
            }
        });
    }

    // Helper class to store pair data
    private static class PairData {
        String checkInTime;
        String checkOutTime;
        String checkInPhoto;
        String checkOutPhoto;
        String checkInAddress;
        String checkOutAddress;
        double checkInLat, checkInLng;
        double checkOutLat, checkOutLng;
        boolean checkInGPS, checkOutGPS;
    }

    private void fetchEmployeeName(EmployeePunchModel model) {
        employeesRef.child(model.mobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String employeeName = "Employee";

                        if (snapshot.exists()) {
                            employeeName = snapshot.child("info")
                                    .child("employeeName")
                                    .getValue(String.class);
                        }

                        if (employeeName == null) {
                            employeeName = "Employee";
                        }

                        for (EmployeePunchModel m : employeeList) {
                            m.employeeName = employeeName;
                        }

                        Log.d(TAG, "Employee name set: " + employeeName);

                        adapter.notifyDataSetChanged();
                        updateStatistics();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch employee name: " + error.getMessage());
                        adapter.notifyDataSetChanged();
                        updateStatistics();
                    }
                });
    }

    private void updateStatistics() {
        int onTime = 0;
        int late = 0;
        int totalSessions = employeeList.size();
        int completeSessions = 0;
        int incompleteSessions = 0;

        if (employeeList.isEmpty()) {
            totalSessions = 0;
        } else {
            // Count complete vs incomplete sessions
            for (EmployeePunchModel model : employeeList) {
                if (model.checkOutTime != null && !model.checkOutTime.isEmpty()) {
                    completeSessions++;
                } else {
                    incompleteSessions++;
                }
            }

            // Check first pair/entry for late status
            EmployeePunchModel firstEntry = employeeList.get(0);

            String lateStatus = firstEntry.lateStatus != null ? firstEntry.lateStatus.toLowerCase() : "";
            String status = firstEntry.status != null ? firstEntry.status.toLowerCase() : "";

            boolean isLate = lateStatus.equals("late") || status.contains("late");

            if (isLate) {
                late = 1;
            } else {
                onTime = 1;
            }
        }

        tvTotalCount.setText("1");
        tvOnTimeCount.setText(String.valueOf(onTime));
        tvLateCount.setText(String.valueOf(late));
        tvSessionCount.setText(String.valueOf(totalSessions));

        Log.d(TAG, "Statistics - Total Sessions: " + totalSessions +
                ", Complete: " + completeSessions +
                ", Incomplete: " + incompleteSessions +
                ", OnTime: " + onTime + ", Late: " + late);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}