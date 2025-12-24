package com.sandhyyasofttech.attendsmart.Registration;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeAdapter;
import com.sandhyyasofttech.attendsmart.Admin.AddEmployeeActivity;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalEmployees, tvPresent, tvAbsent, tvLate;

    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private ArrayList<EmployeeModel> employeeList;

    private FloatingActionButton fabAddEmployee;

    private DatabaseReference employeesRef;
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
        companyKey = prefManager.getUserEmail().replace(".", ",");

        // Firebase reference
        employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees");

        // Fetch data
        fetchEmployeeList();
        fetchDashboardData();

        // FAB click → Add Employee
        fabAddEmployee.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminDashboardActivity.this,
                        AddEmployeeActivity.class
                ))
        );
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
                        EmployeeModel model =
                                infoSnap.getValue(EmployeeModel.class);

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
}
