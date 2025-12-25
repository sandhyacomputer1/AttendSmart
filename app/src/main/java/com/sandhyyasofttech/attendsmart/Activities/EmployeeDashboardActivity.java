package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

public class EmployeeDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvCompany, tvRole, tvShift;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);

        companyKey = getIntent().getStringExtra("companyKey");
        if (companyKey == null) {
            PrefManager prefManager = new PrefManager(this);
            companyKey = prefManager.getCompanyKey();
        }

        initViews();
        loadEmployeeData();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvCompany = findViewById(R.id.tvCompany);
        tvRole = findViewById(R.id.tvRole);
        tvShift = findViewById(R.id.tvShift);
    }

    private void loadEmployeeData() {
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getEmail();

        DatabaseReference employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies").child(companyKey).child("employees");

        employeesRef.orderByChild("info/employeeEmail").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot employeeSnapshot : snapshot.getChildren()) {
                            DataSnapshot info = employeeSnapshot.child("info");
                            String name = info.child("employeeName").getValue(String.class);
                            String role = info.child("employeeRole").getValue(String.class);
                            String shift = info.child("shiftTime").getValue(String.class);
                            String department = info.child("employeeDepartment").getValue(String.class);

                            tvWelcome.setText("Welcome, " + name + "!");
                            tvCompany.setText("Company: " + companyKey.replace(",", "."));
                            tvRole.setText("Role: " + role + " (" + department + ")");
                            tvShift.setText("Shift: " + shift);
                            return;
                        }
                        Toast.makeText(EmployeeDashboardActivity.this, "Employee data not found", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(EmployeeDashboardActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
