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
    private TextView tvDate, tvTotalCount, tvOnTimeCount, tvLateCount, tvAbsentCount;

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
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        rvDetails = findViewById(R.id.rvDetails);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

                EmployeePunchModel model = new EmployeePunchModel();
                model.mobile = employeeMobile;

                // ✅ FETCH ONLY THIS EMPLOYEE DATA
                model.checkInTime = empSnap.child("checkInTime").getValue(String.class);
                model.checkOutTime = empSnap.child("checkOutTime").getValue(String.class);
                model.checkInPhoto = empSnap.child("checkInPhoto").getValue(String.class);
                model.checkOutPhoto = empSnap.child("checkOutPhoto").getValue(String.class);
                model.checkInAddress = empSnap.child("checkInAddress").getValue(String.class);
                model.checkOutAddress = empSnap.child("checkOutAddress").getValue(String.class);
                model.checkInLat = empSnap.child("checkInLat").getValue(Double.class);
                model.checkInLng = empSnap.child("checkInLng").getValue(Double.class);
                model.checkOutLat = empSnap.child("checkOutLat").getValue(Double.class);
                model.checkOutLng = empSnap.child("checkOutLng").getValue(Double.class);
                model.checkInGPS = empSnap.child("checkInGPS").getValue(Boolean.class);
                model.checkOutGPS = empSnap.child("checkOutGPS").getValue(Boolean.class);
                model.status = empSnap.child("status").getValue(String.class);

                if (model.status == null || model.status.isEmpty()) {
                    model.calculateLateStatus(OFFICE_START_TIME);
                }

                model.calculateWorkingHours();
                model.updateLateStatus();

                employeeList.add(model);

                fetchEmployeeName(model);   // only one employee
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Attendance fetch failed: " + error.getMessage());
            }
        });
    }

    private void fetchEmployeeName(EmployeePunchModel model) {

        employeesRef.child(model.mobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()) {
                            model.employeeName =
                                    snapshot.child("name").getValue(String.class);
                        }

                        if (model.employeeName == null) {
                            model.employeeName = "Employee";
                        }

                        adapter.notifyDataSetChanged();
                        updateStatistics();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        adapter.notifyDataSetChanged();
                        updateStatistics();
                    }
                });
    }

    private void updateStatistics() {

        int present = 0;
        int late = 0;
        int absent = 0;

        if (employeeList.isEmpty()) {
            absent = 1;
        } else {
            EmployeePunchModel m = employeeList.get(0);

            String status = m.status != null ? m.status.toLowerCase() : "";
            String lateStatus = m.lateStatus != null ? m.lateStatus.toLowerCase() : "";

            boolean hasCheckIn = m.hasCheckedIn();
            boolean isHalfDay = status.contains("half");
            boolean isLate = lateStatus.equals("late") || status.contains("late");

            // ✅ ABSENT
            if (!hasCheckIn) {
                absent = 1;
            }
            // ✅ PRESENT (BASE)
            else {
                present = 1;
            }

            // ✅ LATE (INDEPENDENT)
            if (isLate) {
                late = 1;
            }
        }

        tvTotalCount.setText("1");
        tvOnTimeCount.setText(String.valueOf(present));
        tvLateCount.setText(String.valueOf(late));
        tvAbsentCount.setText(String.valueOf(absent));
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
