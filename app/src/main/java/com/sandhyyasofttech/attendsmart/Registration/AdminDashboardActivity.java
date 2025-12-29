package com.sandhyyasofttech.attendsmart.Registration;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Activities.DepartmentActivity;
import com.sandhyyasofttech.attendsmart.Activities.ShiftActivity;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeAdapter;
import com.sandhyyasofttech.attendsmart.Admin.AddEmployeeActivity;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;import java.util.concurrent.atomic.AtomicInteger; // ✅ Add this import


public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalEmployees, tvPresent, tvAbsent, tvLate, tvDepartmentCount;
    private MaterialButton btnManageDepartments;

    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private ArrayList<EmployeeModel> employeeList;

    private FloatingActionButton fabAddEmployee;

    private DatabaseReference employeesRef, departmentsRef;
    private String companyKey;
    private TextView tvShiftCount;
    private MaterialButton btnManageShifts;
    private DatabaseReference shiftsRef;
    private DatabaseReference attendanceRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Dashboard cards
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvPresent = findViewById(R.id.tvPresent);
        tvAbsent = findViewById(R.id.tvAbsent);
        tvLate = findViewById(R.id.tvLate);
        tvDepartmentCount = findViewById(R.id.tvDepartmentCount);
        btnManageDepartments = findViewById(R.id.btnManageDepartments);

        // FAB
        fabAddEmployee = findViewById(R.id.fabAddEmployee);

        // RecyclerView
        rvEmployees = findViewById(R.id.rvEmployees);
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));

        employeeList = new ArrayList<>();
        adapter = new EmployeeAdapter(employeeList);
        rvEmployees.setAdapter(adapter);
        tvShiftCount = findViewById(R.id.tvShiftCount);
        btnManageShifts = findViewById(R.id.btnManageShifts);

        // ✅ Add these lines in onCreate()
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());


        // Get logged-in company
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();
        if (email == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        companyKey = email.replace(".", ",");

        // Firebase references
        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey);
        employeesRef = companyRef.child("employees");
        departmentsRef = companyRef.child("departments");
        shiftsRef = companyRef.child("shifts");
        attendanceRef = companyRef.child("attendance");

        fetchShiftCount();
        btnManageShifts.setOnClickListener(v ->
                startActivity(new Intent(this, ShiftActivity.class)));
        // Fetch data
        fetchEmployeeList();
        fetchDashboardData();
        fetchDepartmentCount();

        // FAB click → Add Employee
        fabAddEmployee.setOnClickListener(v ->
                startActivity(new Intent(this, AddEmployeeActivity.class)));

        // ✅ Department button click
        btnManageDepartments.setOnClickListener(v ->
                startActivity(new Intent(this, DepartmentActivity.class)));
    }

    // Logout with confirmation
    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout?")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes, Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        PrefManager pref = new PrefManager(this);
        pref.logout();  // ✅ Clears all saved data

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchShiftCount() {
        shiftsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                tvShiftCount.setText(count + " shifts");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    // ================= EMPLOYEE LIST =================
    private void fetchEmployeeList() {
        employeesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                employeeList.clear();

                if (!snapshot.exists()) {
                    adapter.notifyDataSetChanged();
                    return;
                }

                for (DataSnapshot empSnap : snapshot.getChildren()) {
                    DataSnapshot infoSnap = empSnap.child("info");
                    if (infoSnap.exists()) {
                        EmployeeModel model = infoSnap.getValue(EmployeeModel.class);
                        if (model != null) {
                            employeeList.add(model);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Failed to load employees",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= DASHBOARD COUNTS =================

    private void fetchDashboardData() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Load both employees and attendance together
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot empSnapshot) {
                int total = (int) empSnapshot.getChildrenCount();

                attendanceRef.child(today).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot attSnapshot) {
                        int presentCount = 0;
                        int lateCount = 0;

                        // Count attendance
                        for (DataSnapshot att : attSnapshot.getChildren()) {
                            if (att.hasChild("checkInTime")) {
                                String status = att.child("status").getValue(String.class);
                                if ("Present".equals(status) || status == null) {
                                    presentCount++;
                                } else if ("Late".equals(status)) {
                                    lateCount++;
                                }
                            }
                        }

                        int absentCount = total - (presentCount + lateCount);

                        // Update UI
                        tvTotalEmployees.setText(String.valueOf(total));
                        tvPresent.setText(String.valueOf(presentCount));
                        tvAbsent.setText(String.valueOf(absentCount));
                        tvLate.setText(String.valueOf(lateCount));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ================= DEPARTMENTS COUNT =================
    private void fetchDepartmentCount() {
        departmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                tvDepartmentCount.setText(count + " departments");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
