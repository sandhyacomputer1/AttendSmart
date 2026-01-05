package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeAttendanceListAdapter;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
public class AllAttendanceActivity extends AppCompatActivity {

    private RecyclerView rvAttendance;
    private DatabaseReference employeesRef;
    private String companyKey;

    private ArrayList<EmployeeModel> employeeList = new ArrayList<>();
    private EmployeeAttendanceListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_attendance);

        setupToolbar();
        initViews();
        setupFirebase();
        loadEmployees();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setTitle("All Attendance");  // Added toolbar heading
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvAttendance = findViewById(R.id.rvAttendance);
        rvAttendance.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EmployeeAttendanceListAdapter(employeeList, this::openEmployeeAttendance);
        rvAttendance.setAdapter(adapter);
    }

    private void setupFirebase() {
        companyKey = new PrefManager(this).getCompanyKey();
        employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees");
    }

    private void loadEmployees() {
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                employeeList.clear();

                for (DataSnapshot empSnap : snapshot.getChildren()) {
                    DataSnapshot info = empSnap.child("info");
                    if (!info.exists()) continue;

                    EmployeeModel e = new EmployeeModel();
                    e.setEmployeeMobile(empSnap.getKey());
                    e.setEmployeeName(info.child("employeeName").getValue(String.class));
                    e.setEmployeeDepartment(info.child("employeeDepartment").getValue(String.class));
                    employeeList.add(e);
                }
                adapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void openEmployeeAttendance(EmployeeModel employee) {
        Intent intent = new Intent(this, EmployeeMonthAttendanceActivity.class);
        intent.putExtra("employeeMobile", employee.getEmployeeMobile());
        intent.putExtra("employeeName", employee.getEmployeeName());
        startActivity(intent);
    }
}
