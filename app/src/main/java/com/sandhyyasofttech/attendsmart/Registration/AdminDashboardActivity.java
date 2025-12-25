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
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeAdapter;
import com.sandhyyasofttech.attendsmart.Admin.AddEmployeeActivity;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalEmployees, tvPresent, tvAbsent, tvLate, tvDepartmentCount;
    private MaterialButton btnManageDepartments;

    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private ArrayList<EmployeeModel> employeeList;

    private FloatingActionButton fabAddEmployee;

    private DatabaseReference employeesRef, departmentsRef;
    private String companyKey;

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
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvTotalEmployees.setText("0");
                    tvPresent.setText("0");
                    tvAbsent.setText("0");
                    tvLate.setText("0");
                    return;
                }

                int totalEmployees = (int) snapshot.getChildrenCount();
                // Temporary logic (attendance not implemented yet)
                int present = totalEmployees;
                int absent = 0;
                int late = 0;

                tvTotalEmployees.setText(String.valueOf(totalEmployees));
                tvPresent.setText(String.valueOf(present));
                tvAbsent.setText(String.valueOf(absent));
                tvLate.setText(String.valueOf(late));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Failed to load dashboard data",
                        Toast.LENGTH_SHORT).show();
            }
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
