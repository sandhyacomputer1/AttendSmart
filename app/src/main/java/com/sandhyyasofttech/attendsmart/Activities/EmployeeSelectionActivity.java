//package com.sandhyyasofttech.attendsmart.Activities;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.view.View;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.Toast;
//
//import com.sandhyyasofttech.attendsmart.Adapters.EmployeeSalaryAdapter;
//import com.sandhyyasofttech.attendsmart.Models.Employee;
//import com.sandhyyasofttech.attendsmart.R;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class EmployeeSelectionActivity extends AppCompatActivity {
//
//    private MaterialToolbar toolbar;
//    private EditText etSearchEmployee;
//    private RecyclerView rvEmployees;
//    private LinearLayout llEmptyState;
//
//    private EmployeeSalaryAdapter adapter;
//    private List<Employee> employeeList;
//    private List<Employee> filteredList;
//
//    private DatabaseReference employeesRef;
//    private String companyKey;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_employee_selection);
//
//        initViews();
//        setupToolbar();
//        setupRecyclerView();
//        setupSearch();
//        initFirebase();
//        loadEmployees();
//    }
//
//    private void initViews() {
//        toolbar = findViewById(R.id.toolbar);
//        etSearchEmployee = findViewById(R.id.etSearchEmployee);
//        rvEmployees = findViewById(R.id.rvEmployees);
//        llEmptyState = findViewById(R.id.llEmptyState);
//    }
//
//    private void setupToolbar() {
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
//        }
//        toolbar.setNavigationOnClickListener(v -> onBackPressed());
//    }
//
//    private void setupRecyclerView() {
//        employeeList = new ArrayList<>();
//        filteredList = new ArrayList<>();
//
//        adapter = new EmployeeSalaryAdapter(filteredList, employee -> {
//            // Navigate to SalaryConfigActivity
//            Intent intent = new Intent(EmployeeSelectionActivity.this, SalaryConfigActivity.class);
//            intent.putExtra("employeeMobile", employee.getMobile());
//            intent.putExtra("employeeName", employee.getName());
//            startActivity(intent);
//        });
//
//        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
//        rvEmployees.setAdapter(adapter);
//    }
//
//    private void setupSearch() {
//        etSearchEmployee.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                filterEmployees(s.toString());
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {}
//        });
//    }
//
//    private void filterEmployees(String query) {
//        filteredList.clear();
//
//        if (query.isEmpty()) {
//            filteredList.addAll(employeeList);
//        } else {
//            String lowerCaseQuery = query.toLowerCase();
//            for (Employee employee : employeeList) {
//                if (employee.getName().toLowerCase().contains(lowerCaseQuery) ||
//                    employee.getMobile().contains(lowerCaseQuery)) {
//                    filteredList.add(employee);
//                }
//            }
//        }
//
//        adapter.notifyDataSetChanged();
//        updateEmptyState();
//    }
//
//    private void initFirebase() {
//        PrefManager prefManager = new PrefManager(this);
//        String email = prefManager.getUserEmail();
//
//        if (email == null || email.isEmpty()) {
//            Toast.makeText(this, "Error: Company email not found", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        companyKey = email.replace(".", ",");
//        employeesRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("employees");
//    }
//
//    private void loadEmployees() {
//        employeesRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                employeeList.clear();
//
//                for (DataSnapshot empSnapshot : snapshot.getChildren()) {
//                    String mobile = empSnapshot.getKey();
//                    String name = empSnapshot.child("name").getValue(String.class);
//
//                    // Check if salary is configured
//                    boolean hasSalaryConfig = empSnapshot.child("salaryConfig").exists();
//                    String monthlySalary = "";
//
//                    if (hasSalaryConfig) {
//                        monthlySalary = empSnapshot.child("salaryConfig")
//                                .child("monthlySalary").getValue(String.class);
//                    }
//
//                    Employee employee = new Employee(
//                            mobile != null ? mobile : "",
//                            name != null ? name : "Unknown",
//                            hasSalaryConfig,
//                            monthlySalary != null ? monthlySalary : ""
//                    );
//
//                    employeeList.add(employee);
//                }
//
//                filteredList.clear();
//                filteredList.addAll(employeeList);
//                adapter.notifyDataSetChanged();
//                updateEmptyState();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(EmployeeSelectionActivity.this,
//                        "Failed to load employees: " + error.getMessage(),
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void updateEmptyState() {
//        if (filteredList.isEmpty()) {
//            rvEmployees.setVisibility(View.GONE);
//            llEmptyState.setVisibility(View.VISIBLE);
//        } else {
//            rvEmployees.setVisibility(View.VISIBLE);
//            llEmptyState.setVisibility(View.GONE);
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        // Refresh the list when returning from SalaryConfigActivity
//        if (employeesRef != null) {
//            loadEmployees();
//        }
//    }
//}


package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sandhyyasofttech.attendsmart.Adapters.EmployeeSalaryAdapter;
import com.sandhyyasofttech.attendsmart.Models.Employee;
import com.sandhyyasofttech.attendsmart.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.List;

public class EmployeeSelectionActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private EditText etSearchEmployee;
    private TabLayout tabLayout;
    private LinearLayout llTabAll, llTabConfigured, llTabNotConfigured;
    private TextView tvEmptyAll, tvEmptyConfigured, tvEmptyNotConfigured;
    private RecyclerView rvAll, rvConfigured, rvNotConfigured;

    private EmployeeSalaryAdapter adapterAll, adapterConfigured, adapterNotConfigured;

    private List<Employee> allEmployees = new ArrayList<>();
    private List<Employee> configuredEmployees = new ArrayList<>();
    private List<Employee> notConfiguredEmployees = new ArrayList<>();

    private DatabaseReference employeesRef;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_selection);

        initViews();
        setupToolbar();
        initFirebase();
        setupTabs();
        setupSearch();
        loadEmployees();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearchEmployee = findViewById(R.id.etSearchEmployee);

        // Hide original RecyclerView and empty state
        RecyclerView rvOriginal = findViewById(R.id.rvEmployees);
        rvOriginal.setVisibility(View.GONE);
        LinearLayout llOriginalEmpty = findViewById(R.id.llEmptyState);
        llOriginalEmpty.setVisibility(View.GONE);

        // Initialize tab containers
        LinearLayout mainContainer = findViewById(R.id.container);

        // Create TabLayout
        tabLayout = new TabLayout(this);
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tabParams.setMargins(0, 16, 0, 0);
        tabLayout.setLayoutParams(tabParams);

        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("All (0)"));
        tabLayout.addTab(tabLayout.newTab().setText("Configured (0)"));
        tabLayout.addTab(tabLayout.newTab().setText("Not Configured (0)"));

        // Create tab content containers
        llTabAll = new LinearLayout(this);
        llTabConfigured = new LinearLayout(this);
        llTabNotConfigured = new LinearLayout(this);

        setupTabContent(llTabAll, "all");
        setupTabContent(llTabConfigured, "configured");
        setupTabContent(llTabNotConfigured, "not_configured");

        // Initially show only first tab
        llTabAll.setVisibility(View.VISIBLE);
        llTabConfigured.setVisibility(View.GONE);
        llTabNotConfigured.setVisibility(View.GONE);

        // Add views to main container
        mainContainer.addView(tabLayout);
        mainContainer.addView(llTabAll);
        mainContainer.addView(llTabConfigured);
        mainContainer.addView(llTabNotConfigured);
    }

    private void setupTabContent(LinearLayout container, String tag) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
        );
        params.weight = 1;
        container.setLayoutParams(params);
        container.setOrientation(LinearLayout.VERTICAL);

        // Create RecyclerView
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        recyclerView.setTag(tag + "_recycler");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create empty state
        LinearLayout emptyState = new LinearLayout(this);
        emptyState.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        emptyState.setOrientation(LinearLayout.VERTICAL);
        emptyState.setGravity(View.TEXT_ALIGNMENT_CENTER);
        emptyState.setTag(tag + "_empty");

        TextView tvEmpty = new TextView(this);
        tvEmpty.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tvEmpty.setText("No employees found");
        tvEmpty.setTextSize(16);
        tvEmpty.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvEmpty.setTag(tag + "_empty_text");

        emptyState.addView(tvEmpty);
        emptyState.setVisibility(View.GONE);

        container.addView(recyclerView);
        container.addView(emptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        llTabAll.setVisibility(View.VISIBLE);
                        llTabConfigured.setVisibility(View.GONE);
                        llTabNotConfigured.setVisibility(View.GONE);
                        break;
                    case 1:
                        llTabAll.setVisibility(View.GONE);
                        llTabConfigured.setVisibility(View.VISIBLE);
                        llTabNotConfigured.setVisibility(View.GONE);
                        break;
                    case 2:
                        llTabAll.setVisibility(View.GONE);
                        llTabConfigured.setVisibility(View.GONE);
                        llTabNotConfigured.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Initialize adapters
        adapterAll = new EmployeeSalaryAdapter(allEmployees, this::onEmployeeClick);
        adapterConfigured = new EmployeeSalaryAdapter(configuredEmployees, this::onEmployeeClick);
        adapterNotConfigured = new EmployeeSalaryAdapter(notConfiguredEmployees, this::onEmployeeClick);

        // Get RecyclerViews
        rvAll = llTabAll.findViewWithTag("all_recycler");
        rvConfigured = llTabConfigured.findViewWithTag("configured_recycler");
        rvNotConfigured = llTabNotConfigured.findViewWithTag("not_configured_recycler");

        // Set adapters
        rvAll.setAdapter(adapterAll);
        rvConfigured.setAdapter(adapterConfigured);
        rvNotConfigured.setAdapter(adapterNotConfigured);

        // Get empty state TextViews
        tvEmptyAll = llTabAll.findViewWithTag("all_empty_text");
        tvEmptyConfigured = llTabConfigured.findViewWithTag("configured_empty_text");
        tvEmptyNotConfigured = llTabNotConfigured.findViewWithTag("not_configured_empty_text");
    }

    private void setupSearch() {
        etSearchEmployee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEmployees(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterEmployees(String query) {
        List<Employee> filteredAll = filterList(allEmployees, query);
        List<Employee> filteredConfigured = filterList(configuredEmployees, query);
        List<Employee> filteredNotConfigured = filterList(notConfiguredEmployees, query);

        adapterAll = new EmployeeSalaryAdapter(filteredAll, this::onEmployeeClick);
        adapterConfigured = new EmployeeSalaryAdapter(filteredConfigured, this::onEmployeeClick);
        adapterNotConfigured = new EmployeeSalaryAdapter(filteredNotConfigured, this::onEmployeeClick);

        rvAll.setAdapter(adapterAll);
        rvConfigured.setAdapter(adapterConfigured);
        rvNotConfigured.setAdapter(adapterNotConfigured);

        updateEmptyStates(filteredAll, filteredConfigured, filteredNotConfigured);
    }

    private List<Employee> filterList(List<Employee> source, String query) {
        if (query.isEmpty()) {
            return new ArrayList<>(source);
        }

        List<Employee> filtered = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (Employee employee : source) {
            if (employee.getName().toLowerCase().contains(lowerCaseQuery) ||
                    employee.getMobile().contains(lowerCaseQuery) ||
                    (employee.getDepartment() != null &&
                            employee.getDepartment().toLowerCase().contains(lowerCaseQuery))) {
                filtered.add(employee);
            }
        }

        return filtered;
    }

    private void updateEmptyStates(List<Employee> all, List<Employee> configured, List<Employee> notConfigured) {
        updateSingleEmptyState(rvAll, tvEmptyAll, all);
        updateSingleEmptyState(rvConfigured, tvEmptyConfigured, configured);
        updateSingleEmptyState(rvNotConfigured, tvEmptyNotConfigured, notConfigured);
    }

    private void updateSingleEmptyState(RecyclerView recyclerView, TextView emptyTextView, List<Employee> list) {
        if (list.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            ((ViewGroup) emptyTextView.getParent()).setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            ((ViewGroup) emptyTextView.getParent()).setVisibility(View.GONE);
        }
    }

    private void initFirebase() {
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();

        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Error: Company email not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        companyKey = email.replace(".", ",");
        employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees");
    }

    private void loadEmployees() {
        employeesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allEmployees.clear();
                configuredEmployees.clear();
                notConfiguredEmployees.clear();

                for (DataSnapshot empSnapshot : snapshot.getChildren()) {
                    String mobile = empSnapshot.getKey();

                    // Get employee info from "info" node
                    DataSnapshot infoSnapshot = empSnapshot.child("info");

                    String name = infoSnapshot.child("employeeName").getValue(String.class);
                    String department = infoSnapshot.child("employeeDepartment").getValue(String.class);

                    // Default values if not present
                    if (name == null || name.isEmpty()) {
                        name = "Unknown";
                    }

                    if (department == null || department.isEmpty()) {
                        department = "Not Assigned";
                    }

                    // Check if salary is configured - look for salaryConfig at root level
                    boolean hasSalaryConfig = empSnapshot.child("salaryConfig").exists();
                    String monthlySalary = "";

                    if (hasSalaryConfig) {
                        monthlySalary = empSnapshot.child("salaryConfig")
                                .child("monthlySalary").getValue(String.class);
                        if (monthlySalary == null) {
                            monthlySalary = "0";
                        }
                    }

                    Employee employee = new Employee(
                            mobile != null ? mobile : "",
                            name,
                            department,
                            hasSalaryConfig,
                            monthlySalary
                    );

                    allEmployees.add(employee);

                    // Add to appropriate list
                    if (hasSalaryConfig) {
                        configuredEmployees.add(employee);
                    } else {
                        notConfiguredEmployees.add(employee);
                    }
                }

                // Update adapters
                adapterAll = new EmployeeSalaryAdapter(allEmployees, EmployeeSelectionActivity.this::onEmployeeClick);
                adapterConfigured = new EmployeeSalaryAdapter(configuredEmployees, EmployeeSelectionActivity.this::onEmployeeClick);
                adapterNotConfigured = new EmployeeSalaryAdapter(notConfiguredEmployees, EmployeeSelectionActivity.this::onEmployeeClick);

                rvAll.setAdapter(adapterAll);
                rvConfigured.setAdapter(adapterConfigured);
                rvNotConfigured.setAdapter(adapterNotConfigured);

                updateTabCounts();
                updateEmptyStates(allEmployees, configuredEmployees, notConfiguredEmployees);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmployeeSelectionActivity.this,
                        "Failed to load employees: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTabCounts() {
        TabLayout.Tab tabAll = tabLayout.getTabAt(0);
        TabLayout.Tab tabConfigured = tabLayout.getTabAt(1);
        TabLayout.Tab tabNotConfigured = tabLayout.getTabAt(2);

        if (tabAll != null) tabAll.setText("All (" + allEmployees.size() + ")");
        if (tabConfigured != null) tabConfigured.setText("Configured (" + configuredEmployees.size() + ")");
        if (tabNotConfigured != null) tabNotConfigured.setText("Not Configured (" + notConfiguredEmployees.size() + ")");
    }

    private void onEmployeeClick(Employee employee) {
        Intent intent = new Intent(EmployeeSelectionActivity.this, SalaryConfigActivity.class);
        intent.putExtra("employeeMobile", employee.getMobile());
        intent.putExtra("employeeName", employee.getName());
        intent.putExtra("employeeDepartment", employee.getDepartment());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (employeesRef != null) {
            loadEmployees();
        }
    }
}