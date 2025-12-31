package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.sandhyyasofttech.attendsmart.Admin.AddEmployeeActivity;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.AdminDashboardActivity;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

public class EmployeeDetailsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvName, tvId, tvMobile, tvEmail, tvRole, tvDepartment, tvShift,
            tvStatus, tvJoinDate, tvCreatedAt, tvWeeklyHoliday;

    private EmployeeModel employee;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_details);

        initializeViews();
        setupToolbar();
        loadEmployeeData();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvName = findViewById(R.id.tvName);
        tvId = findViewById(R.id.tvId);
        tvMobile = findViewById(R.id.tvMobile);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvShift = findViewById(R.id.tvShift);
        tvStatus = findViewById(R.id.tvStatus);
        tvJoinDate = findViewById(R.id.tvJoinDate);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvWeeklyHoliday = findViewById(R.id.tvWeeklyHoliday);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void loadEmployeeData() {
        employee = (EmployeeModel) getIntent().getSerializableExtra("employee");
        if (employee != null) {
            toolbar.setTitle(employee.getEmployeeName());

            // Initialize company key for Firebase operations
            PrefManager prefManager = new PrefManager(this);
            String email = prefManager.getUserEmail();
            if (email != null) {
                companyKey = email.replace(".", ",");
            }

            populateViews();
        } else {
            Toast.makeText(this, "❌ Employee data not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void populateViews() {
        tvName.setText(safeText(employee.getEmployeeName(), "N/A"));
        tvId.setText(safeText(employee.getEmployeeId(), "N/A"));
        tvMobile.setText(safeText(employee.getEmployeeMobile(), "N/A"));
        tvEmail.setText(safeText(employee.getEmployeeEmail(), "N/A"));
        tvRole.setText(safeText(employee.getEmployeeRole(), "Staff"));
        tvDepartment.setText(safeText(employee.getEmployeeDepartment(), "N/A"));
        tvShift.setText(safeText(employee.getEmployeeShift(), "N/A"));
        tvStatus.setText(safeText(employee.getEmployeeStatus(), "Active"));
        tvJoinDate.setText(safeText(employee.getJoinDate(), "N/A"));
        tvCreatedAt.setText(safeText(employee.getCreatedAt(), "N/A"));
        tvWeeklyHoliday.setText(safeText(employee.getWeeklyHoliday(), "N/A"));

        // Update status color dynamically
        updateStatusColor();
    }

    private void updateStatusColor() {
        String status = safeText(employee.getEmployeeStatus(), "ACTIVE");  // ✅ Default "ACTIVE"
        int color = status.equalsIgnoreCase("ACTIVE") ?  // ✅ Firebase format
                getResources().getColor(android.R.color.holo_green_dark, null) :
                getResources().getColor(android.R.color.holo_red_dark, null);
        tvStatus.setTextColor(color);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.employee_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_edit_employee) {
            editEmployee();
            return true;
        } else if (id == R.id.action_deactivate_employee) {
            showDeactivateDialog();
            return true;
        }
//        else if (id == R.id.nav_reports) {  // ✅ ADD THIS LINE
//            startActivity(new Intent(this, ReportsActivity.class));
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    private void editEmployee() {
        Intent intent = new Intent(this, AddEmployeeActivity.class);
        intent.putExtra("employee", employee);
        intent.putExtra("mode", "EDIT"); // Pass edit mode
        startActivity(intent);
        finish(); // Close details after opening edit
    }

    private void showDeactivateDialog() {
        String currentStatus = safeText(employee.getEmployeeStatus(), "Active");
        String action = currentStatus.equalsIgnoreCase("Active") ? "Deactivate" : "Activate";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ " + action + " Employee")
                .setMessage("Are you sure you want to " + action.toLowerCase() + " " +
                        employee.getEmployeeName() + "?")
                .setPositiveButton("Yes, " + action, (dialog, which) -> toggleEmployeeStatus())
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    private void toggleEmployeeStatus() {
        // ✅ FIXED: Use mobile as ID (matches Firebase structure)
        String employeeMobile = safeText(employee.getEmployeeMobile(), null);

        if (companyKey == null || employeeMobile == null || employeeMobile.isEmpty()) {
            Toast.makeText(this, "❌ Invalid employee data", Toast.LENGTH_SHORT).show();
            return;
        }

        String newStatus = employee.getEmployeeStatus() != null &&
                employee.getEmployeeStatus().equalsIgnoreCase("ACTIVE") ?  // ✅ Firebase uses "ACTIVE"
                "INACTIVE" : "ACTIVE";

        // ✅ FIXED: Use MOBILE as key (matches your Firebase structure)
        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)  // ✅ Use mobile number as key
                .child("info")
                .child("employeeStatus")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    employee.setEmployeeStatus(newStatus);
                    populateViews(); // Refresh UI
                    String message = newStatus.equals("ACTIVE") ?
                            "✅ Employee activated successfully" :
                            "✅ Employee deactivated successfully";
                    Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String safeText(String value, String defaultValue) {
        return value != null && !value.trim().isEmpty() ? value : defaultValue;
    }
}
