package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeWorkStatusAdapter;
import com.sandhyyasofttech.attendsmart.Models.EmployeeWorkStatus;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AdminTodayWorkActivity extends AppCompatActivity {

    private RecyclerView rvEmployees;
    private TextView tvDate, tvEmpty, tvStats;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private MaterialButton btnDatePicker;
    private Toolbar toolbar;

    private ArrayList<EmployeeWorkStatus> allEmployeesList;
    private ArrayList<EmployeeWorkStatus> filteredList;
    private EmployeeWorkStatusAdapter adapter;

    private String companyKey, selectedDate;
    private int submittedCount = 0, notSubmittedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_today_work);

        initViews();
        loadSession();
        loadEmployeesWorkStatus();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Employee Work Reports");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvDate = findViewById(R.id.tvDate);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvStats = findViewById(R.id.tvStats);
        progressBar = findViewById(R.id.progressBar);
        rvEmployees = findViewById(R.id.rvEmployees);
        tabLayout = findViewById(R.id.tabLayout);
        btnDatePicker = findViewById(R.id.btnDatePicker);

        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        allEmployeesList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new EmployeeWorkStatusAdapter(filteredList, employee -> {
            // Open detail activity
            Intent intent = new Intent(AdminTodayWorkActivity.this, EmployeeWorkDetailActivity.class);
            intent.putExtra("employeeMobile", employee.getEmployeeMobile());
            intent.putExtra("employeeName", employee.getEmployeeName());
            intent.putExtra("selectedDate", selectedDate);
            intent.putExtra("hasSubmitted", employee.isHasSubmitted());
            startActivity(intent);
        });
        rvEmployees.setAdapter(adapter);

        setupTabs();
        btnDatePicker.setOnClickListener(v -> showDatePicker());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Submitted"));
        tabLayout.addTab(tabLayout.newTab().setText("Not Submitted"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterEmployees(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterEmployees(int tabPosition) {
        filteredList.clear();

        switch (tabPosition) {
            case 0: // All
                filteredList.addAll(allEmployeesList);
                break;
            case 1: // Submitted
                for (EmployeeWorkStatus emp : allEmployeesList) {
                    if (emp.isHasSubmitted()) {
                        filteredList.add(emp);
                    }
                }
                break;
            case 2: // Not Submitted
                for (EmployeeWorkStatus emp : allEmployeesList) {
                    if (!emp.isHasSubmitted()) {
                        filteredList.add(emp);
                    }
                }
                break;
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void loadSession() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();

        if (companyKey == null || companyKey.isEmpty()) {
            Toast.makeText(this, "⚠️ Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        updateDateDisplay();
    }

    private void updateDateDisplay() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(selectedDate);
            String formattedDate = outputFormat.format(date);

            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            if (selectedDate.equals(todayDate)) {
                tvDate.setText(" Today - " + formattedDate);
            } else {
                tvDate.setText(" " + formattedDate);
            }
            btnDatePicker.setText(formattedDate);
        } catch (Exception e) {
            tvDate.setText(" " + selectedDate);
        }
    }

    private void loadEmployeesWorkStatus() {
        showLoading(true);
        submittedCount = 0;
        notSubmittedCount = 0;

        DatabaseReference empRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees");

        DatabaseReference workRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork")
                .child(selectedDate);

        // 1️⃣ Load today's work FIRST (FAST)
        workRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot workSnapshot) {

                // Map<employeeMobile, workSnapshot>
                final java.util.Map<String, DataSnapshot> workMap = new java.util.HashMap<>();
                for (DataSnapshot snap : workSnapshot.getChildren()) {
                    workMap.put(snap.getKey(), snap);
                }

                // 2️⃣ Load employees (FULL INFO)
                empRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot empSnapshot) {

                        allEmployeesList.clear();

                        if (!empSnapshot.exists()) {
                            showLoading(false);
                            showEmptyState();
                            return;
                        }

                        for (DataSnapshot empSnap : empSnapshot.getChildren()) {

                            DataSnapshot infoSnap = empSnap.child("info");
                            if (!infoSnap.exists()) continue;

                            String status = infoSnap.child("employeeStatus").getValue(String.class);
                            if (!"ACTIVE".equals(status)) continue;

                            // ✅ SAME AS OLD WORKING CODE
                            String mobile = infoSnap.child("employeeMobile").getValue(String.class);
                            String name = infoSnap.child("employeeName").getValue(String.class);
                            String email = infoSnap.child("employeeEmail").getValue(String.class);
                            String department = infoSnap.child("employeeDepartment").getValue(String.class);
                            String role = infoSnap.child("employeeRole").getValue(String.class);
                            String profileImage = infoSnap.child("profileImage").getValue(String.class);

                            EmployeeWorkStatus emp = new EmployeeWorkStatus();
                            emp.setEmployeeMobile(mobile);
                            emp.setEmployeeName(name);
                            emp.setEmployeeEmail(email);
                            emp.setEmployeeDepartment(department);
                            emp.setEmployeeRole(role);
                            emp.setProfileImage(profileImage);
                            emp.setHasSubmitted(false);

                            // 🔥 FAST submission check
                            if (workMap.containsKey(mobile)) {
                                DataSnapshot work = workMap.get(mobile);
                                emp.setHasSubmitted(true);
                                emp.setCompletedWork(work.child("completedWork").getValue(String.class));
                                emp.setOngoingWork(work.child("ongoingWork").getValue(String.class));
                                emp.setTomorrowWork(work.child("tomorrowWork").getValue(String.class));

                                Long time = work.child("submittedAt").getValue(Long.class);
                                emp.setSubmittedAt(time != null ? time : 0L);

                                submittedCount++;
                            } else {
                                notSubmittedCount++;
                            }

                            allEmployeesList.add(emp);
                        }

                        showLoading(false);
                        updateStats();
                        filterEmployees(tabLayout.getSelectedTabPosition());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Toast.makeText(AdminTodayWorkActivity.this,
                                "❌ Employee load failed: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(AdminTodayWorkActivity.this,
                        "❌ Work load failed: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkWorkSubmissions() {
        DatabaseReference workRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork")
                .child(selectedDate);

        workRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Mark employees who have submitted
                for (EmployeeWorkStatus emp : allEmployeesList) {
                    if (snapshot.hasChild(emp.getEmployeeMobile())) {
                        DataSnapshot workSnap = snapshot.child(emp.getEmployeeMobile());
                        emp.setHasSubmitted(true);

                        // Get submission details
                        emp.setCompletedWork(workSnap.child("completedWork").getValue(String.class));
                        emp.setOngoingWork(workSnap.child("ongoingWork").getValue(String.class));
                        emp.setTomorrowWork(workSnap.child("tomorrowWork").getValue(String.class));

                        Long timestamp = workSnap.child("submittedAt").getValue(Long.class);
                        emp.setSubmittedAt(timestamp != null ? timestamp : 0L);

                        submittedCount++;
                    } else {
                        notSubmittedCount++;
                    }
                }

                showLoading(false);
                updateStats();
                filterEmployees(tabLayout.getSelectedTabPosition());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(AdminTodayWorkActivity.this,
                        "❌ Failed to load work data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        int total = allEmployeesList.size();
        String stats = String.format(Locale.getDefault(),
                "Total: %d |  Submitted: %d |  Pending: %d",
                total, submittedCount, notSubmittedCount);
        tvStats.setText(stats);

        // Update tab badges
        TabLayout.Tab allTab = tabLayout.getTabAt(0);
        TabLayout.Tab submittedTab = tabLayout.getTabAt(1);
        TabLayout.Tab notSubmittedTab = tabLayout.getTabAt(2);

        if (allTab != null) allTab.setText("All (" + total + ")");
        if (submittedTab != null) submittedTab.setText("Submitted (" + submittedCount + ")");
        if (notSubmittedTab != null) notSubmittedTab.setText("Pending (" + notSubmittedCount + ")");
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(calendar.getTime());
                    updateDateDisplay();
                    loadEmployeesWorkStatus();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvEmployees.setVisibility(show ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("No active employees found");
        rvEmployees.setVisibility(View.GONE);
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            int selectedTab = tabLayout.getSelectedTabPosition();
            if (selectedTab == 1) {
                tvEmpty.setText("No employees have submitted work yet");
            } else if (selectedTab == 2) {
                tvEmpty.setText("All employees have submitted their work! 🎉");
            } else {
                tvEmpty.setText("No employees found");
            }
            rvEmployees.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvEmployees.setVisibility(View.VISIBLE);
        }
    }

}