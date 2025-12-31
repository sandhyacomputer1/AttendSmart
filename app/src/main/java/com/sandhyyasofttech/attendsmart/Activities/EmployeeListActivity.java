package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeListAdapter;
import com.sandhyyasofttech.attendsmart.Admin.AddEmployeeActivity;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.AdminDashboardActivity;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;

public class EmployeeListActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private CircularProgressIndicator progressIndicator;
    private androidx.recyclerview.widget.RecyclerView rvEmployees;
    private EmployeeListAdapter adapter;
    private ArrayList<EmployeeModel> employeeList;
    private TextInputEditText etSearch;
    private View emptyStateLayout;

    // Firebase
    private DatabaseReference employeesRef;
    private ValueEventListener employeesListener;
    private String companyKey;
    private TextView tvEmployeeCount;  // ✅ TextView, NOT TextInputEditText

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_list);

        initializeAllViews();
        setupToolbarAndDrawer();

        if (validateUserSession()) {
            setupRecyclerViewAndSearch();
            loadEmployeesFromFirebase();
        }
    }

    private void initializeAllViews() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        progressIndicator = findViewById(R.id.progressIndicator);
        rvEmployees = findViewById(R.id.rvEmployees);
        etSearch = findViewById(R.id.etSearch);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        tvEmployeeCount = findViewById(R.id.tvEmployeeCount);  // ✅ UNCOMMENTED!
    }

    private void setupToolbarAndDrawer() {
        setSupportActionBar(toolbar);
        toolbar.setTitle("Employee Management");
        toolbar.setNavigationOnClickListener(v -> toggleNavigationDrawer());

        androidx.appcompat.app.ActionBarDrawerToggle toggle = new androidx.appcompat.app.ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigationItem(item.getItemId());
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        navigationView.setCheckedItem(R.id.nav_employees);
    }

    private boolean validateUserSession() {
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "⚠️ Session expired. Please login again.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return false;
        }
        companyKey = email.replace(".", ",");
        employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies").child(companyKey).child("employees");
        return true;
    }

    private void setupRecyclerViewAndSearch() {
        employeeList = new ArrayList<>();
        adapter = new EmployeeListAdapter(employeeList, this::onEmployeeClick);
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        rvEmployees.setHasFixedSize(false);
        rvEmployees.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                updateEmployeeCount();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadEmployeesFromFirebase() {
        showLoadingState();
        employeesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideLoadingState();
                employeeList.clear();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    showEmptyState();
                    return;
                }

                for (DataSnapshot empSnap : snapshot.getChildren()) {
                    DataSnapshot infoSnap = empSnap.child("info");
                    if (infoSnap.exists()) {
                        // ✅ SAFE PARSING - Convert Long to String automatically
                        EmployeeModel model = parseEmployeeSafely(infoSnap);
                        if (model != null) {
                            employeeList.add(model);
                        }
                    }
                }

                adapter.updateData(employeeList);
                updateEmployeeCount();
                if (employeeList.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingState();
                Toast.makeText(EmployeeListActivity.this,
                        "❌ Failed to load employees: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };
        employeesRef.addValueEventListener(employeesListener);
    }

    // ✅ NEW SAFE PARSING METHOD
    private EmployeeModel parseEmployeeSafely(DataSnapshot infoSnap) {
        try {
            EmployeeModel model = new EmployeeModel();

            // ✅ FIXED: Set mobile as employeeId (matches Firebase key)
            model.setEmployeeId(safeToString(infoSnap.child("employeeMobile")));  // Use mobile as ID

            model.setEmployeeName(safeToString(infoSnap.child("employeeName")));
            model.setEmployeeMobile(safeToString(infoSnap.child("employeeMobile")));
            model.setEmployeeEmail(safeToString(infoSnap.child("employeeEmail")));
            model.setEmployeeRole(safeToString(infoSnap.child("employeeRole")));
            model.setEmployeeDepartment(safeToString(infoSnap.child("employeeDepartment")));
            model.setEmployeeStatus(safeToString(infoSnap.child("employeeStatus")));
            model.setEmployeeShift(safeToString(infoSnap.child("employeeShift")));
            model.setWeeklyHoliday(safeToString(infoSnap.child("weeklyHoliday")));
            model.setJoinDate(safeToString(infoSnap.child("joinDate")));
            model.setCreatedAt(safeToString(infoSnap.child("createdAt")));

            if (model.getEmployeeName() == null || model.getEmployeeName().trim().isEmpty()) {
                return null;
            }

            return model;
        } catch (Exception e) {
            android.util.Log.e("EmployeeList", "Failed to parse employee: " + e.getMessage());
            return null;
        }
    }

    // ✅ UTILITY: Convert any DataSnapshot to String safely
    private String safeToString(DataSnapshot dataSnap) {
        if (!dataSnap.exists()) {
            return null;
        }

        Object value = dataSnap.getValue();
        if (value == null) {
            return null;
        }

        // ✅ Handle Long → String conversion
        if (value instanceof Long) {
            return ((Long) value).toString();
        } else if (value instanceof Number) {
            return String.valueOf(value);
        }

        return value.toString();
    }

    private void updateEmployeeCount() {
        int count = adapter != null ? adapter.getItemCount() : 0;
        if (tvEmployeeCount != null) {
            tvEmployeeCount.setText("👥 Total: " + count + " employees");
        }
    }

    // UI State Management
    private void showLoadingState() {
        progressIndicator.setVisibility(View.VISIBLE);
        rvEmployees.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        progressIndicator.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        rvEmployees.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        rvEmployees.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void toggleNavigationDrawer() {
        if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void handleNavigationItem(int itemId) {
        if (itemId == R.id.nav_dashboard) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
        } else if (itemId == R.id.nav_logout) {
            showLogoutConfirmation();
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("🚪 Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes, Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    private void performLogout() {
        PrefManager prefManager = new PrefManager(this);
        prefManager.logout();
        Toast.makeText(this, "✅ Logged out successfully", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ✅ CORRECT!
    private void onEmployeeClick(EmployeeModel employee) {
        Intent intent = new Intent(this, EmployeeDetailsActivity.class);
        intent.putExtra("employee", employee);  // ✅ Serializable works!
        startActivity(intent);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.employee_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add_employee) {
            startActivity(new Intent(this, AddEmployeeActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            loadEmployeesFromFirebase();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (employeesRef != null && employeesListener != null) {
            employeesRef.removeEventListener(employeesListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (employeesRef != null && employeesListener != null) {
            employeesRef.removeEventListener(employeesListener);
        }
    }
}
