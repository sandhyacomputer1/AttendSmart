package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;

public class EmployeeDetailsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvName, tvId, tvMobile, tvEmail, tvRole, tvDepartment, tvShift,
            tvStatus, tvJoinDate, tvCreatedAt, tvWeeklyHoliday;

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
        EmployeeModel employee = (EmployeeModel) getIntent().getSerializableExtra("employee");
        if (employee != null) {
            toolbar.setTitle(employee.getEmployeeName());

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
        } else {
            Toast.makeText(this, "❌ Employee data not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String safeText(String value, String defaultValue) {
        return value != null && !value.trim().isEmpty() ? value : defaultValue;
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
