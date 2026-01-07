package com.sandhyyasofttech.attendsmart.Registration;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.sandhyyasofttech.attendsmart.Activities.AdminLeaveListActivity;
import com.sandhyyasofttech.attendsmart.Activities.AllAttendanceActivity;
import com.sandhyyasofttech.attendsmart.Activities.DepartmentActivity;
import com.sandhyyasofttech.attendsmart.Activities.EmployeeListActivity;
import com.sandhyyasofttech.attendsmart.Activities.GenerateSalaryActivity;
import com.sandhyyasofttech.attendsmart.Activities.ProfileActivity;
import com.sandhyyasofttech.attendsmart.Activities.ReportsActivity;
import com.sandhyyasofttech.attendsmart.Activities.SalaryDetailActivity;
import com.sandhyyasofttech.attendsmart.Activities.SalaryListActivity;
import com.sandhyyasofttech.attendsmart.Settings.SettingsActivity;
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

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;


public class AdminDashboardActivity extends AppCompatActivity {

    // UI Views
    private MaterialToolbar topAppBar;
    private TextView tvTotalEmployees, tvPresent, tvAbsent, tvLate;
    private RecyclerView rvEmployees;
    private TextInputEditText etSearch;
    private EmployeeAdapter adapter;
    private ArrayList<EmployeeModel> employeeList;
    private ArrayList<EmployeeModel> fullEmployeeList;
    private ExtendedFloatingActionButton fabAddEmployee;
    private Menu navMenu;
    private MenuItem navLeavesItem;
    // Firebase
    private DatabaseReference employeesRef, attendanceRef;
    private String companyKey;
    private TextView tvToolbarTitle;  // 👈 Add field declaration with other TextViews

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private static final int NOTIF_PERMISSION = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        initializeViews();
        setupDrawer();
        setupToolbar();
        fetchCompanyNameForTitle();

        if (!setupCompanySession()) return;

        initializeFirebaseReferences();
        saveAdminFcmToken();
        ensureNotificationSetting();
        setupClickListeners();
        setupSearchView();
        requestNotificationPermission();
        setupRealTimeLeaveListener();
        fetchAllData();
    }
    private void setupRealTimeLeaveListener() {
        FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("leaves")
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                        checkLeaveRequests();
                    }
                    @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) { checkLeaveRequests(); }
                    @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) { checkLeaveRequests(); }
                    @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    private void fetchCompanyNameForTitle() {
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();
        if (email == null) return;

        String companyKey = email.replace(".", ",");

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
                            tvToolbarTitle.setText(companyName);  // ✅ ONLY "Sandhyaaaa"
                        } else {
                            tvToolbarTitle.setText("Admin");  // Fallback
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    private void initializeViews() {
        topAppBar = findViewById(R.id.topAppBar);
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvPresent = findViewById(R.id.tvPresent);
        tvAbsent = findViewById(R.id.tvAbsent);
        tvLate = findViewById(R.id.tvLate);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);  // 👈 Add this line

        // 👈 UPDATED - TextInputEditText instead of SearchView
        etSearch = findViewById(R.id.etSearch);

        rvEmployees = findViewById(R.id.rvEmployees);
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        rvEmployees.setHasFixedSize(true);

        employeeList = new ArrayList<>();
        fullEmployeeList = new ArrayList<>();
        adapter = new EmployeeAdapter(employeeList, this);
        rvEmployees.setAdapter(adapter);

        fabAddEmployee = findViewById(R.id.fabAddEmployee);
    }

    private void setupSearchView() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEmployees(s.toString().toLowerCase().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void filterEmployees(String query) {
        employeeList.clear();
        if (query.isEmpty()) {
            employeeList.addAll(fullEmployeeList);
        } else {
            for (EmployeeModel employee : fullEmployeeList) {
                if (employee.getEmployeeName().toLowerCase().contains(query) ||
                        employee.getEmployeeMobile().contains(query) ||
                        (employee.getEmployeeDepartment() != null &&
                                employee.getEmployeeDepartment().toLowerCase().contains(query))) {
                    employeeList.add(employee);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupDrawer() {

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // ✅ Get menu reference for badge
        navMenu = navigationView.getMenu();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                topAppBar,
                R.string.nav_open,
                R.string.nav_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();
            Intent intent = null;

            if (id == R.id.nav_dashboard) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
            else if (id == R.id.nav_employees) {
                intent = new Intent(this, EmployeeListActivity.class);
            }
            else if (id == R.id.nav_departments) {
                intent = new Intent(this, DepartmentActivity.class);
            }
            else if (id == R.id.nav_shifts) {
                intent = new Intent(this, ShiftActivity.class);
            }
            else if (id == R.id.nav_attendance) {
                intent = new Intent(this, AllAttendanceActivity.class);
            }
            else if (id == R.id.nav_leaves) {
                intent = new Intent(this, AdminLeaveListActivity.class);
            }

            else if (id == R.id.nav_reports) {
                intent = new Intent(this, ReportsActivity.class);
            }

            else if (id == R.id.nav_view_salary) {
                intent = new Intent(this, SalaryListActivity.class);
            }
            else if (id == R.id.nav_generate_salary) {
                intent = new Intent(this, GenerateSalaryActivity.class);
            }
            else if (id == R.id.nav_profile) {
                intent = new Intent(this, ProfileActivity.class);
            }
            else if (id == R.id.nav_settings) {
                intent = new Intent(this, SettingsActivity.class);
            }
            else if (id == R.id.nav_logout) {
                showLogoutConfirmation();
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            if (intent != null) startActivity(intent);

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                checkLeaveRequests();  // Update badge
            }
            // ... other methods empty
            @Override public void onDrawerClosed(View drawerView) {}
            @Override public void onDrawerSlide(View drawerView, float slideOffset) {}
            @Override public void onDrawerStateChanged(int newState) {}
        });
        navigationView.setCheckedItem(R.id.nav_dashboard);
        updateNavHeader();
    }
    private void checkLeaveRequests() {
        FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("leaves")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int pendingCount = 0;

                        for (DataSnapshot leave : snapshot.getChildren()) {
                            String status = leave.child("status").getValue(String.class);
                            if ("PENDING".equalsIgnoreCase(status)) {  // New requests
                                pendingCount++;
                            }
                        }

                        // ✅ Update badge
                        navLeavesItem = navMenu.findItem(R.id.nav_leaves);
                        if (pendingCount > 0) {
                            navLeavesItem.setTitle("Leave Requests (" + pendingCount + ")");
                        } else {
                            navLeavesItem.setTitle(getString(R.string.leave_requests));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateNavHeader() {

        try {
            View headerView = navigationView.getHeaderView(0);

            TextView tvCompanyName = headerView.findViewById(R.id.tvCompanyName);
            TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
            ShapeableImageView ivProfile = headerView.findViewById(R.id.ivProfile);

            PrefManager prefManager = new PrefManager(this);
            tvUserEmail.setText(prefManager.getUserEmail());

            String email = prefManager.getUserEmail();
            if (email == null) return;

            String companyKey = email.replace(".", ",");

            DatabaseReference companyInfoRef = FirebaseDatabase.getInstance()
                    .getReference("Companies")
                    .child(companyKey)
                    .child("companyInfo");

            companyInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (!snapshot.exists()) return;

                    String companyName = snapshot.child("companyName").getValue(String.class);
                    if (companyName != null && !companyName.isEmpty()) {
                        tvCompanyName.setText(companyName);
                    }

                    String logoUrl = snapshot.child("companyLogo").getValue(String.class);
                    if (logoUrl != null && !logoUrl.isEmpty()) {
                        Glide.with(AdminDashboardActivity.this)
                                .load(logoUrl)
                                .placeholder(R.drawable.ic_person)
                                .into(ivProfile);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            // ✅ CLICK ANYWHERE ON HEADER → OPEN PROFILE
            headerView.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(
                        AdminDashboardActivity.this,
                        ProfileActivity.class
                ));
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void setupToolbar() {

        // Force WHITE menu icon
        topAppBar.setNavigationIcon(R.drawable.ic_menu);

        topAppBar.setNavigationOnClickListener(v -> {
            if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

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

    private void initializeFirebaseReferences() {
        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies").child(companyKey);
        employeesRef = companyRef.child("employees");
        attendanceRef = companyRef.child("attendance");
    }

    private void setupClickListeners() {
        fabAddEmployee.setOnClickListener(v ->
                startActivity(new Intent(this, AddEmployeeActivity.class)));
        // ✅ Total Employees text click → open EmployeeListActivity

        MaterialCardView totalEmployeesCard = findViewById(R.id.cardTotalEmployees);
        if (totalEmployeesCard != null) {
            totalEmployeesCard.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this, EmployeeListActivity.class);
                startActivity(intent);
            });
        }
    }

    private void saveAdminFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null && !token.isEmpty()) {
                        FirebaseDatabase.getInstance()
                                .getReference("Companies")
                                .child(companyKey)
                                .child("companyInfo")
                                .child("adminFcmToken")
                                .setValue(token);
                    }
                });
    }

    private void ensureNotificationSetting() {
        FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("companyInfo")
                .child("notifyAttendance")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            snapshot.getRef().setValue(true);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIF_PERMISSION);
            }
        }
    }

    private void fetchAllData() {
        fetchEmployeeList();
        fetchDashboardData();
    }

    private void fetchEmployeeList() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        FirebaseDatabase.getInstance().getReference("Companies").child(companyKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        fullEmployeeList.clear();
                        ArrayList<EmployeeModel> presentList = new ArrayList<>();
                        ArrayList<EmployeeModel> absentList = new ArrayList<>();

                        DataSnapshot employeesSnap = snapshot.child("employees");
                        DataSnapshot todayAttSnap = snapshot.child("attendance").child(today);

                        if (employeesSnap.exists()) {
                            for (DataSnapshot empSnap : employeesSnap.getChildren()) {
                                DataSnapshot infoSnap = empSnap.child("info");
                                if (infoSnap.exists()) {
                                    EmployeeModel model = parseEmployeeSafely(infoSnap);
                                    if (model != null) {
                                        String phone = model.getEmployeeMobile();
                                        DataSnapshot attRecord = todayAttSnap.child(phone);

                                        if (attRecord.exists() && attRecord.hasChild("checkInTime")) {
                                            // Fetch status and lateStatus
                                            String status = safeToString(attRecord.child("status"));
                                            String lateStatus = safeToString(attRecord.child("lateStatus"));

                                            // Priority order for status determination:
                                            // 1. Half Day (highest priority)
                                            if ("Half Day".equalsIgnoreCase(status)) {
                                                model.setTodayStatus("Half Day");
                                            }
                                            // 2. Late (check lateStatus field)
                                            else if ("Late".equalsIgnoreCase(lateStatus)) {
                                                model.setTodayStatus("Late");
                                            }
                                            // 3. Present or Full Day
                                            else if ("Present".equalsIgnoreCase(status) ||
                                                    "Full Day".equalsIgnoreCase(status)) {
                                                model.setTodayStatus("Present");
                                            }
                                            // 4. Default to Present if has check-in
                                            else {
                                                model.setTodayStatus("Present");
                                            }

                                            // Fetch check-in and check-out times
                                            model.setCheckInTime(safeToString(attRecord.child("checkInTime")));
                                            model.setCheckOutTime(safeToString(attRecord.child("checkOutTime")));

                                            // Fetch other attendance data
                                            model.setTotalHours(safeToString(attRecord.child("totalHours")));
                                            model.setCheckInPhoto(safeToString(attRecord.child("checkInPhoto")));
                                            model.setCheckOutPhoto(safeToString(attRecord.child("checkOutPhoto")));

                                            presentList.add(model);
                                        } else {
                                            // No attendance record
                                            model.setTodayStatus("Absent");
                                            model.setCheckInTime(null);
                                            model.setCheckOutTime(null);
                                            model.setCheckInPhoto(null);
                                            model.setCheckOutPhoto(null);
                                            absentList.add(model);
                                        }
                                        fullEmployeeList.add(model);
                                    }
                                }
                            }
                        }

                        // Present employees first, then absent
                        employeeList.clear();
                        employeeList.addAll(presentList);
                        employeeList.addAll(absentList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminDashboardActivity.this,
                                "Failed to load employees", Toast.LENGTH_SHORT).show();
                    }
                });
    }

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
            return (model.getEmployeeName() != null && !model.getEmployeeName().trim().isEmpty()) ? model : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeToString(DataSnapshot dataSnap) {
        if (!dataSnap.exists()) return null;
        Object value = dataSnap.getValue();
        if (value == null) return null;
        return value.toString();
    }

    private void fetchDashboardData() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot empSnapshot) {

                int totalEmployees = (int) empSnapshot.getChildrenCount();

                attendanceRef.child(today).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot attSnapshot) {

                        int presentCount = 0;
                        int lateCount = 0;

                        for (DataSnapshot att : attSnapshot.getChildren()) {

                            // ✅ Any check-in = Present
                            if (att.hasChild("checkInTime")) {
                                presentCount++;

                                // ✅ Late is SUBSET of Present
                                String lateStatus = att.child("lateStatus").getValue(String.class);
                                if ("Late".equalsIgnoreCase(lateStatus)) {
                                    lateCount++;
                                }
                            }
                        }

                        int absentCount = Math.max(0, totalEmployees - presentCount);

                        updateDashboardUI(
                                totalEmployees,
                                presentCount,   // includes Late + Half Day
                                absentCount,
                                lateCount       // shown separately
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void updateDashboardUI(int total, int present, int absent, int late) {
        tvTotalEmployees.setText(String.valueOf(total));
        tvPresent.setText(String.valueOf(present));
        tvAbsent.setText(String.valueOf(absent));
        tvLate.setText(String.valueOf(late));
    }

    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        new PrefManager(this).logout();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAllData();
    }
}