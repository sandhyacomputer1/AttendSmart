package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CurrentEmploymentActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvCompanyName, tvDepartment, tvEmployeeId, tvDateOfJoining;
    private TextView tvEmployeeType, tvDesignation, tvShift;
    private TextView tvPfAccNo, tvEsiAccNo, tvUan;

    private DatabaseReference dbRef;
    private String companyKey, employeeMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_employment);

        // Get data from intent
        companyKey = getIntent().getStringExtra("companyKey");
        employeeMobile = getIntent().getStringExtra("employeeMobile");

        if (companyKey == null || employeeMobile == null) {
            toast("Error loading data");
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadEmploymentData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvDateOfJoining = findViewById(R.id.tvDateOfJoining);
        tvEmployeeType = findViewById(R.id.tvEmployeeType);
        tvDesignation = findViewById(R.id.tvDesignation);
        tvShift = findViewById(R.id.tvShift);
        tvPfAccNo = findViewById(R.id.tvPfAccNo);
        tvEsiAccNo = findViewById(R.id.tvEsiAccNo);
        tvUan = findViewById(R.id.tvUan);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadEmploymentData() {
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Load Company Name
        loadCompanyName();

        // Load Employee Info
        loadEmployeeInfo();
    }

    private void loadCompanyName() {
        dbRef.child("Companies")
                .child(companyKey)
                .child("companyInfo")
                .child("companyName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String companyName = snapshot.getValue(String.class);
                        if (companyName != null && !companyName.isEmpty()) {
                            tvCompanyName.setText(companyName);
                        } else {
                            tvCompanyName.setText(companyKey.replace(",", "."));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvCompanyName.setText(companyKey.replace(",", "."));
                    }
                });
    }

    private void loadEmployeeInfo() {
        dbRef.child("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("info")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            toast("Employee data not found");
                            return;
                        }

                        // Department
                        String department = snapshot.child("employeeDepartment").getValue(String.class);
                        tvDepartment.setText(department != null && !department.isEmpty() ? department : "—");

                        // Employee ID
                        String empId = snapshot.child("employeeId").getValue(String.class);
                        tvEmployeeId.setText(empId != null && !empId.isEmpty() ? empId : "—");

                        // Date of Joining
                        String doj = snapshot.child("joinDate").getValue(String.class);
                        if (doj != null && !doj.isEmpty()) {
                            tvDateOfJoining.setText(formatDate(doj));
                        } else {
                            tvDateOfJoining.setText("—");
                        }

                        // Employee Type
                        String empType = snapshot.child("employeeType").getValue(String.class);
                        tvEmployeeType.setText(empType != null && !empType.isEmpty() ? empType : "—");

                        // Designation / Role
                        String role = snapshot.child("employeeRole").getValue(String.class);
                        tvDesignation.setText(role != null && !role.isEmpty() ? role : "—");

                        // Shift
                        String shift = snapshot.child("employeeShift").getValue(String.class);
                        if (shift != null && !shift.isEmpty()) {
                            loadShiftDetails(shift);
                        } else {
                            tvShift.setText("—");
                        }

                        // PF Account No
                        String pfAcc = snapshot.child("pfAccountNo").getValue(String.class);
                        tvPfAccNo.setText(pfAcc != null && !pfAcc.isEmpty() ? pfAcc : "—");

                        // ESI Account No
                        String esiAcc = snapshot.child("esiAccountNo").getValue(String.class);
                        tvEsiAccNo.setText(esiAcc != null && !esiAcc.isEmpty() ? esiAcc : "—");

                        // UAN
                        String uan = snapshot.child("uan").getValue(String.class);
                        tvUan.setText(uan != null && !uan.isEmpty() ? uan : "—");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toast("Failed to load employee data");
                    }
                });
    }

    private void loadShiftDetails(String shiftName) {
        // Extract shift name if it contains timing in brackets
        String cleanShiftName = shiftName.contains("(") 
                ? shiftName.substring(0, shiftName.indexOf("(")).trim() 
                : shiftName;

        dbRef.child("Companies")
                .child(companyKey)
                .child("shifts")
                .child(cleanShiftName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String startTime = snapshot.child("startTime").getValue(String.class);
                            String endTime = snapshot.child("endTime").getValue(String.class);

                            if (startTime != null && endTime != null) {
                                tvShift.setText(cleanShiftName + " (" + startTime + " - " + endTime + ")");
                            } else {
                                tvShift.setText(cleanShiftName);
                            }
                        } else {
                            tvShift.setText(shiftName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvShift.setText(shiftName);
                    }
                });
    }

    private String formatDate(String dateStr) {
        try {
            // Try parsing common formats
            SimpleDateFormat inputFormat;
            
            if (dateStr.contains("-")) {
                if (dateStr.split("-")[0].length() == 4) {
                    inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                } else {
                    inputFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                }
            } else if (dateStr.contains("/")) {
                if (dateStr.split("/")[2].length() == 4) {
                    inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                } else {
                    inputFormat = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
                }
            } else {
                return dateStr;
            }

            Date date = inputFormat.parse(dateStr);
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            // Return original if parsing fails
        }
        return dateStr;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}