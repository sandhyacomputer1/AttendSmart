package com.sandhyyasofttech.attendsmart.Registration;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Activities.DepartmentActivity;
import com.sandhyyasofttech.attendsmart.Activities.EmployeeListActivity;
import com.sandhyyasofttech.attendsmart.Activities.ShiftActivity;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeAdapter;
import com.sandhyyasofttech.attendsmart.Admin.AddEmployeeActivity;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    // Toolbar
    private MaterialToolbar topAppBar;

    // Dashboard Statistics TextViews
    private TextView tvTotalEmployees, tvPresent, tvAbsent, tvLate;

    // Management Section
    private TextView tvDepartmentTitle, tvDepartmentCount;
    private TextView tvShiftTitle, tvShiftCount;
    private MaterialButton btnManageDepartments, btnManageShifts, btnLogout;

    // Employees Section
    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private ArrayList<EmployeeModel> employeeList;

    // FAB
    private ExtendedFloatingActionButton fabAddEmployee;

    // Firebase References
    private DatabaseReference employeesRef, departmentsRef, shiftsRef, attendanceRef;
    private String companyKey;
    private MaterialButton btnViewReports;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize all views
        initializeViews();
        setupDrawer();  // ADD THIS

        // Setup toolbar
        setupToolbar();

        // Get logged-in company
        if (!setupCompanySession()) {
            return;
        }

        // Initialize Firebase references
        initializeFirebaseReferences();

        // Setup click listeners
        setupClickListeners();

        // Fetch all data
        fetchAllData();
    }
    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Set drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, topAppBar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                // Already on dashboard
            } else if (id == R.id.nav_employees) {
                startActivity(new Intent(this, EmployeeListActivity.class));
            } else if (id == R.id.nav_departments) {
                startActivity(new Intent(this, DepartmentActivity.class));
            } else if (id == R.id.nav_shifts) {
                startActivity(new Intent(this, ShiftActivity.class));
            } else if (id == R.id.nav_logout) {
                showLogoutConfirmation();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Set active item
        navigationView.setCheckedItem(R.id.nav_dashboard);

        // Update header with company info
        updateNavHeader();
    }
    private void updateNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView tvCompanyName = headerView.findViewById(R.id.tvCompanyName);
        TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);

        PrefManager prefManager = new PrefManager(this);
        tvUserEmail.setText(prefManager.getUserEmail());
        tvCompanyName.setText("Sandhya Soft Tech"); // Or fetch from prefs
    }
    /**
     * Initialize all views
     */
    private void initializeViews() {
        // Toolbar
        topAppBar = findViewById(R.id.topAppBar);

        // Dashboard statistics cards
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvPresent = findViewById(R.id.tvPresent);
        tvAbsent = findViewById(R.id.tvAbsent);
        tvLate = findViewById(R.id.tvLate);

        // Management section
        tvDepartmentTitle = findViewById(R.id.tvDepartmentTitle);
        tvDepartmentCount = findViewById(R.id.tvDepartmentCount);
        tvShiftTitle = findViewById(R.id.tvShiftTitle);
        tvShiftCount = findViewById(R.id.tvShiftCount);

        btnManageDepartments = findViewById(R.id.btnManageDepartments);
        btnManageShifts = findViewById(R.id.btnManageShifts);
        btnLogout = findViewById(R.id.btnLogout);

        // Employees RecyclerView
        rvEmployees = findViewById(R.id.rvEmployees);
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        rvEmployees.setHasFixedSize(true);

        employeeList = new ArrayList<>();
        adapter = new EmployeeAdapter(employeeList);
        rvEmployees.setAdapter(adapter);

        // FAB
        fabAddEmployee = findViewById(R.id.fabAddEmployee);
    }

    /**
     * Setup toolbar with back button if needed
     */
    private void setupToolbar() {
        setSupportActionBar(topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admin Dashboard");
        }

        // Handle hamburger menu click
        topAppBar.setNavigationOnClickListener(v -> {
            if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }


    /**
     * Setup company session and validate user
     */
    private boolean setupCompanySession() {
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();

        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return false;
        }

        companyKey = email.replace(".", ",");
        return true;
    }

    /**
     * Initialize all Firebase database references
     */
    private void initializeFirebaseReferences() {
        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey);

        employeesRef = companyRef.child("employees");
        departmentsRef = companyRef.child("departments");
        shiftsRef = companyRef.child("shifts");
        attendanceRef = companyRef.child("attendance");
    }

    /**
     * Setup all click listeners
     */
    private void setupClickListeners() {


        // FAB - Add Employee
        fabAddEmployee.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AddEmployeeActivity.class);
            startActivity(intent);
        });

        // Manage Departments
        btnManageDepartments.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, DepartmentActivity.class);
            startActivity(intent);
        });

        // Manage Shifts
        btnManageShifts.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ShiftActivity.class);
            startActivity(intent);
        });

        // Logout Button
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    /**
     * Fetch all data from Firebase
     */
    private void fetchAllData() {
        fetchEmployeeList();
        fetchDashboardData();
        fetchDepartmentCount();
        fetchShiftCount();
    }

    // ================= EMPLOYEE LIST =================

    /**
     * Fetch and display employee list
     */
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
// ✅ FIXED CODE
                        EmployeeModel model = parseEmployeeSafely(infoSnap);
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
                        "Failed to load employees: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    // ✅ ADD THESE METHODS (Copy exactly)
    private EmployeeModel parseEmployeeSafely(DataSnapshot infoSnap) {
        try {
            EmployeeModel model = new EmployeeModel();
            model.setEmployeeId(safeToString(infoSnap.child("employeeId")));
            model.setEmployeeName(safeToString(infoSnap.child("employeeName")));
            model.setEmployeeMobile(safeToString(infoSnap.child("employeeMobile")));
            model.setEmployeeRole(safeToString(infoSnap.child("employeeRole")));
            model.setEmployeeEmail(safeToString(infoSnap.child("employeeEmail")));
            model.setEmployeeDepartment(safeToString(infoSnap.child("employeeDepartment")));
            model.setEmployeeStatus(safeToString(infoSnap.child("employeeStatus")));
            model.setEmployeeShift(safeToString(infoSnap.child("employeeShift")));
            model.setCreatedAt(safeToString(infoSnap.child("createdAt")));
            model.setWeeklyHoliday(safeToString(infoSnap.child("weeklyHoliday")));
            model.setJoinDate(safeToString(infoSnap.child("joinDate")));

            if (model.getEmployeeName() == null || model.getEmployeeName().trim().isEmpty()) {
                return null;
            }
            return model;
        } catch (Exception e) {
            android.util.Log.e("Dashboard", "Parse error: " + e.getMessage());
            return null;
        }
    }

    private String safeToString(DataSnapshot dataSnap) {
        if (!dataSnap.exists()) return null;
        Object value = dataSnap.getValue();
        if (value == null) return null;
        if (value instanceof Long) return ((Long) value).toString();
        if (value instanceof Number) return String.valueOf(value);
        return value.toString();
    }

    // ================= DASHBOARD STATISTICS =================

    /**
     * Fetch dashboard statistics (Total, Present, Absent, Late)
     */
    private void fetchDashboardData() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Load both employees and attendance together
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot empSnapshot) {
                int totalEmployees = (int) empSnapshot.getChildrenCount();

                // Now fetch today's attendance
                attendanceRef.child(today).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot attSnapshot) {
                        int presentCount = 0;
                        int lateCount = 0;

                        // Count attendance records
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

                        // Calculate absent count
                        int absentCount = totalEmployees - (presentCount + lateCount);
                        if (absentCount < 0) absentCount = 0; // Safety check

                        // Update UI on main thread
                        updateDashboardUI(totalEmployees, presentCount, absentCount, lateCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminDashboardActivity.this,
                                "Failed to load attendance data",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Failed to load employee data",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Update dashboard UI with statistics
     */
    private void updateDashboardUI(int total, int present, int absent, int late) {
        tvTotalEmployees.setText(String.valueOf(total));
        tvPresent.setText(String.valueOf(present));
        tvAbsent.setText(String.valueOf(absent));
        tvLate.setText(String.valueOf(late));
    }

    // ================= DEPARTMENTS COUNT =================

    /**
     * Fetch and display department count
     */
    private void fetchDepartmentCount() {
        departmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                String countText = count + (count == 1 ? " department" : " departments");
                tvDepartmentCount.setText(countText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvDepartmentCount.setText("0 departments");
            }
        });
    }

    // ================= SHIFTS COUNT =================

    /**
     * Fetch and display shift count
     */
    private void fetchShiftCount() {
        shiftsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                String countText = count + (count == 1 ? " shift" : " shifts");
                tvShiftCount.setText(countText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvShiftCount.setText("0 shifts");
            }
        });
    }

    // ================= LOGOUT FUNCTIONALITY =================

    /**
     * Show logout confirmation dialog
     */
    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes, Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .setCancelable(true)
                .show();
    }

    /**
     * Perform logout operation
     */
    private void performLogout() {
        // Clear user session
        PrefManager prefManager = new PrefManager(this);
        prefManager.logout();

        // Show logout message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to login
        navigateToLogin();
    }

    /**
     * Navigate to login activity and clear back stack
     */
    private void navigateToLogin() {
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        fetchAllData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any listeners if needed
        if (employeesRef != null) {
            employeesRef.removeEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {}
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }
}