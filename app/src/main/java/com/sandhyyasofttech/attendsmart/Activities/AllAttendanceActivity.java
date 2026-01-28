package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeAttendanceListAdapter;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Locale;

public class AllAttendanceActivity extends AppCompatActivity {

    private RecyclerView rvAttendance;
    private DatabaseReference employeesRef;
    private String companyKey;

    private ArrayList<EmployeeModel> originalEmployeeList = new ArrayList<>();
    private ArrayList<EmployeeModel> filteredEmployeeList = new ArrayList<>();
    private EmployeeAttendanceListAdapter adapter;
    private SearchView searchView;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_attendance);

        setupToolbar();
        initViews();
        setupFirebase();
        setupSearchView();
        loadEmployees();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("All Attendance");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white, getTheme()));
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvAttendance = findViewById(R.id.rvAttendance);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvAttendance.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EmployeeAttendanceListAdapter(filteredEmployeeList, this::openEmployeeAttendance);
        rvAttendance.setAdapter(adapter);
    }

    private void setupFirebase() {
        companyKey = new PrefManager(this).getCompanyKey();
        employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees");
    }

    private void setupSearchView() {
        searchView = findViewById(R.id.searchView);

        // Customize search view appearance
        int searchPlateId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(Color.TRANSPARENT);
        }

        // Set hint text color
        int searchTextId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_src_text", null, null);
        TextView searchText = searchView.findViewById(searchTextId);
        if (searchText != null) {
            searchText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            searchText.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        // Set search query listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterEmployees(newText);
                return true;
            }
        });

        // Clear search when closed
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                filterEmployees("");
                return false;
            }
        });
    }

    private void loadEmployees() {
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                originalEmployeeList.clear();
                filteredEmployeeList.clear();

                for (DataSnapshot empSnap : snapshot.getChildren()) {
                    DataSnapshot info = empSnap.child("info");
                    if (!info.exists()) continue;

                    EmployeeModel e = new EmployeeModel();
                    e.setEmployeeMobile(empSnap.getKey());
                    e.setEmployeeName(info.child("employeeName").getValue(String.class));
                    e.setEmployeeDepartment(info.child("employeeDepartment").getValue(String.class));

                    originalEmployeeList.add(e);
                }

                // Initially show all employees
                filteredEmployeeList.addAll(originalEmployeeList);
                adapter.notifyDataSetChanged();
                checkEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void filterEmployees(String query) {
        filteredEmployeeList.clear();

        if (query == null || query.trim().isEmpty()) {
            // Show all employees if query is empty
            filteredEmployeeList.addAll(originalEmployeeList);
        } else {
            String searchQuery = query.toLowerCase(Locale.getDefault()).trim();

            for (EmployeeModel employee : originalEmployeeList) {
                // Search by name OR mobile number
                String employeeName = employee.getEmployeeName() != null ?
                        employee.getEmployeeName().toLowerCase(Locale.getDefault()) : "";
                String employeeMobile = employee.getEmployeeMobile() != null ?
                        employee.getEmployeeMobile() : "";

                if (employeeName.contains(searchQuery) ||
                        employeeMobile.contains(searchQuery)) {
                    filteredEmployeeList.add(employee);
                }
            }
        }

        adapter.notifyDataSetChanged();
        checkEmptyState();
    }

    private void checkEmptyState() {
        if (filteredEmployeeList.isEmpty()) {
            if (searchView != null && searchView.getQuery() != null &&
                    !searchView.getQuery().toString().trim().isEmpty()) {
                // Show "no results found" message
                tvEmptyState.setText("No employees found for \"" + searchView.getQuery() + "\"");
                tvEmptyState.setVisibility(View.VISIBLE);
                rvAttendance.setVisibility(View.GONE);
            } else {
                // Show "no employees" message
                tvEmptyState.setText("No employees found");
                tvEmptyState.setVisibility(View.VISIBLE);
                rvAttendance.setVisibility(View.GONE);
            }
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvAttendance.setVisibility(View.VISIBLE);
        }
    }

    private void openEmployeeAttendance(EmployeeModel employee) {
        Intent intent = new Intent(this, EmployeeMonthAttendanceActivity.class);
        intent.putExtra("employeeMobile", employee.getEmployeeMobile());
        intent.putExtra("employeeName", employee.getEmployeeName());
        startActivity(intent);
    }
}