package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.MonthlyAttendanceSummary;
import com.sandhyyasofttech.attendsmart.Models.SalaryCalculationResult;
import com.sandhyyasofttech.attendsmart.Models.SalaryConfig;
import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import com.sandhyyasofttech.attendsmart.payroll.SalaryCalculator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class GenerateSalaryActivity extends AppCompatActivity {

    private EditText etMonth;
    private Spinner spEmployee;
    private Button btnGenerate;
    private Button btnViewSalary;
    private Toolbar toolbar;

    private String companyKey;
    private DatabaseReference companyRef;
    private final ArrayList<String> employeeMobiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_salary);

        // Initialize all views
        toolbar = findViewById(R.id.toolbar);
        etMonth = findViewById(R.id.etMonth);
        spEmployee = findViewById(R.id.spEmployee);
        btnGenerate = findViewById(R.id.btnGenerateSalary);
        btnViewSalary = findViewById(R.id.btnViewSalary);

        // Setup toolbar with back arrow
        setupToolbar();

        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey(); // Use getCompanyKey() instead of getUserEmail()

        companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey);

        setupMonthPicker();
        loadEmployees();

        btnGenerate.setOnClickListener(v -> validateAndGenerate());
    }

    // ================= TOOLBAR SETUP WITH BACK ARROW =================
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Generate Salary");
        }

        // Handle back arrow click
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    // ================= MONTH PICKER =================
    private void setupMonthPicker() {
        etMonth.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        String value = String.format(
                                Locale.getDefault(),
                                "%02d-%d",
                                month + 1, year);
                        etMonth.setText(value);
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    // ================= LOAD EMPLOYEES =================
    private void loadEmployees() {
        companyRef.child("employees")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        employeeMobiles.clear();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            employeeMobiles.add(s.getKey());
                        }

                        if (employeeMobiles.isEmpty()) {
                            Toast.makeText(GenerateSalaryActivity.this,
                                    "No employees found",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ArrayAdapter<String> adapter =
                                new ArrayAdapter<>(GenerateSalaryActivity.this,
                                        android.R.layout.simple_spinner_item,
                                        employeeMobiles);
                        adapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);
                        spEmployee.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(GenerateSalaryActivity.this,
                                "Failed to load employees",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ================= VALIDATE =================
    private void validateAndGenerate() {
        String month = etMonth.getText().toString().trim();
        if (month.isEmpty()) {
            etMonth.setError("Select month");
            return;
        }

        if (spEmployee.getSelectedItem() == null) {
            Toast.makeText(this, "Select employee", Toast.LENGTH_SHORT).show();
            return;
        }

        String employeeMobile = spEmployee.getSelectedItem().toString();
        fetchSalaryConfigAndAttendance(month, employeeMobile);
    }

    // ================= FETCH CONFIG + ATTENDANCE =================
    private void fetchSalaryConfigAndAttendance(
            String month,
            String employeeMobile
    ) {
        // Show loading
        btnGenerate.setEnabled(false);
        btnGenerate.setText("Processing...");

        companyRef.child("employees")
                .child(employeeMobile)
                .child("salaryConfig")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            Toast.makeText(GenerateSalaryActivity.this,
                                    "Salary config not set for this employee",
                                    Toast.LENGTH_LONG).show();
                            resetGenerateButton();
                            return;
                        }

                        SalaryConfig config = parseSalaryConfig(snapshot);

                        if (config.monthlySalary <= 0 || config.workingDays <= 0) {
                            Toast.makeText(GenerateSalaryActivity.this,
                                    "Invalid salary configuration",
                                    Toast.LENGTH_LONG).show();
                            resetGenerateButton();
                            return;
                        }

                        buildMonthlyAttendanceSummary(
                                month,
                                employeeMobile,
                                config,
                                new AttendanceCallback() {
                                    @Override
                                    public void onReady(MonthlyAttendanceSummary summary) {
                                        generateAndSaveSalary(
                                                month,
                                                employeeMobile,
                                                summary,
                                                config
                                        );
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(GenerateSalaryActivity.this,
                                                error,
                                                Toast.LENGTH_LONG).show();
                                        resetGenerateButton();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(GenerateSalaryActivity.this,
                                "Failed to fetch salary config",
                                Toast.LENGTH_SHORT).show();
                        resetGenerateButton();
                    }
                });
    }

    private void resetGenerateButton() {
        btnGenerate.setEnabled(true);
        btnGenerate.setText("Generate Salary");
    }

    // ================= ATTENDANCE SUMMARY =================
    private void buildMonthlyAttendanceSummary(
            String month,
            String employeeMobile,
            SalaryConfig config,
            AttendanceCallback callback
    ) {
        MonthlyAttendanceSummary summary = new MonthlyAttendanceSummary();

        // ================= 1️⃣ ATTENDANCE =================
        companyRef.child("attendance")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot attendanceSnap) {

                        for (DataSnapshot dateSnap : attendanceSnap.getChildren()) {
                            String dateKey = dateSnap.getKey(); // yyyy-MM-dd
                            if (dateKey == null || dateKey.length() < 7) continue;

                            String recordMonth =
                                    dateKey.substring(5, 7) + "-" +
                                            dateKey.substring(0, 4);

                            if (!recordMonth.equals(month)) continue;

                            DataSnapshot empSnap =
                                    dateSnap.child(employeeMobile);

                            if (!empSnap.exists()) continue;

                            String finalStatus =
                                    empSnap.child("finalStatus").getValue(String.class);

                            if (finalStatus == null) continue;

                            finalStatus = finalStatus.toLowerCase();

                            if (finalStatus.contains("present")) {
                                summary.presentDays++;
                            } else if (finalStatus.contains("half")) {
                                summary.halfDays++;
                            } else {
                                // summary.absentDays++;
                            }
                        }

                        // ================= 2️⃣ LEAVES =================
                        countLeaves(month, employeeMobile, config, summary, callback);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    private int countDaysInMonth(
            String fromDate,
            String toDate,
            String month
    ) {
        if (fromDate == null || toDate == null) return 0;

        String fromMonth =
                fromDate.substring(5, 7) + "-" + fromDate.substring(0, 4);

        if (!fromMonth.equals(month)) return 0;

        return 1; // OK for single-day leave
    }

    private void countLeaves(
            String month,
            String employeeMobile,
            SalaryConfig config,
            MonthlyAttendanceSummary summary,
            AttendanceCallback callback
    ) {
        companyRef.child("leaves")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        int paidLimit = config.paidLeaves;
                        int paidUsed = 0;

                        for (DataSnapshot l : snapshot.getChildren()) {
                            if (!employeeMobile.equals(
                                    l.child("employeeMobile").getValue(String.class)))
                                continue;

                            if (!"APPROVED".equals(
                                    l.child("status").getValue(String.class)))
                                continue;

                            boolean isPaid =
                                    Boolean.TRUE.equals(l.child("isPaid").getValue(Boolean.class));

                            int days = countDaysInMonth(
                                    l.child("fromDate").getValue(String.class),
                                    l.child("toDate").getValue(String.class),
                                    month
                            );

                            if (isPaid && paidUsed < paidLimit) {
                                int allowed = Math.min(days, paidLimit - paidUsed);
                                summary.paidLeavesUsed += allowed;
                                paidUsed += allowed;
                                summary.unpaidLeaves += (days - allowed);
                            } else {
                                summary.unpaidLeaves += days;
                            }
                            if (summary.absentDays < 0) summary.absentDays = 0;
                        }

                        // ✅ FINAL CALLBACK (ONLY ONCE)
                        callback.onReady(summary);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    // ================= GENERATE & SAVE =================
    private void generateAndSaveSalary(
            String month,
            String employeeMobile,
            MonthlyAttendanceSummary summary,
            SalaryConfig config
    ) {
        SalaryCalculationResult result =
                SalaryCalculator.calculateSalary(summary, config);

        SalarySnapshot snapshot = new SalarySnapshot();
        snapshot.month = month;
        snapshot.employeeMobile = employeeMobile;
        snapshot.generatedAt = System.currentTimeMillis();
        snapshot.attendanceSummary = summary;
        snapshot.calculationResult = result;
        snapshot.salaryConfigSnapshot = config;

        companyRef.child("salary")
                .child(month)
                .child(employeeMobile)
                .setValue(snapshot)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(
                            GenerateSalaryActivity.this,
                            "Salary generated successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    // ✅ Show View Salary button
                    btnViewSalary.setVisibility(View.VISIBLE);

                    btnViewSalary.setOnClickListener(v -> {
                        Intent intent = new Intent(
                                GenerateSalaryActivity.this,
                                SalaryDetailActivity.class
                        );
                        intent.putExtra("month", month);
                        intent.putExtra("employeeMobile", employeeMobile);
                        startActivity(intent);
                    });

                    resetGenerateButton();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            GenerateSalaryActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    resetGenerateButton();
                });
    }

    // ================= SAFE PARSING =================
    private SalaryConfig parseSalaryConfig(DataSnapshot s) {
        SalaryConfig c = new SalaryConfig();

        c.monthlySalary = getDouble(s, "monthlySalary");
        c.workingDays = getInt(s, "workingDays");
        c.paidLeaves = getInt(s, "paidLeaves");

        c.pfPercent = getDouble(s, "pfPercent");
        c.esiPercent = getDouble(s, "esiPercent");
        c.otherDeduction = getDouble(s, "otherDeduction");

        c.deductionEnabled =
                Boolean.TRUE.equals(
                        s.child("deductionEnabled").getValue(Boolean.class));

        c.lateRule = getString(s, "lateRule");
        c.effectiveFrom = getString(s, "effectiveFrom");
        c.deductionNote = getString(s, "deductionNote");

        return c;
    }

    private double getDouble(DataSnapshot s, String key) {
        try {
            Object v = s.child(key).getValue();
            if (v == null) return 0;
            if (v instanceof Number) return ((Number) v).doubleValue();
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private int getInt(DataSnapshot s, String key) {
        try {
            Object v = s.child(key).getValue();
            if (v == null) return 0;
            if (v instanceof Number) return ((Number) v).intValue();
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private String getString(DataSnapshot s, String key) {
        Object v = s.child(key).getValue();
        return v == null ? "" : v.toString();
    }

    // ================= CALLBACK =================
    private interface AttendanceCallback {
        void onReady(MonthlyAttendanceSummary summary);

        void onError(String error);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}