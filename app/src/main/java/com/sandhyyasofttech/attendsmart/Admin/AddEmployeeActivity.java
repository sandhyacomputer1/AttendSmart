package com.sandhyyasofttech.attendsmart.Admin;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class AddEmployeeActivity extends AppCompatActivity {

    // UI Elements
    TextInputEditText etEmpName, etEmpMobile, etEmpEmail, etEmpPassword;
    Spinner spinnerRole, spinnerHoliday, spinnerDepartment, spinnerShift;  // ✅ Added Shift Spinner
    MaterialButton btnSaveEmployee;

    DatabaseReference employeesRef, departmentsRef, shiftsRef;  // ✅ Added shiftsRef
    String companyKey;
    String selectedRole = "Employee";
    String selectedHoliday = "Sunday";
    String selectedDepartment = "";
    String selectedShift = "";  // ✅ Selected shift name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);

        initViews();
        setupCompanyReference();
        setupSpinners();
        loadDepartments();
        loadShifts();  // ✅ Load shifts from Firebase
        setupClickListeners();
    }

    private void initViews() {
        etEmpName = findViewById(R.id.etEmpName);
        etEmpMobile = findViewById(R.id.etEmpMobile);
        etEmpEmail = findViewById(R.id.etEmpEmail);
        etEmpPassword = findViewById(R.id.etEmpPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerHoliday = findViewById(R.id.spinnerHoliday);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerShift = findViewById(R.id.spinnerShift);  // ✅ Shift spinner
        btnSaveEmployee = findViewById(R.id.btnSaveEmployee);
    }

    private void setupCompanyReference() {
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getEmail();
        if (email == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        companyKey = email.replace(".", ",");

        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey);
        employeesRef = companyRef.child("employees");
        departmentsRef = companyRef.child("departments");
        shiftsRef = companyRef.child("shifts");  // ✅ Shifts reference
    }

    private void setupSpinners() {
        // Role Spinner
        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(this,
                R.array.employee_roles, android.R.layout.simple_spinner_item);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Holiday Spinner
        ArrayAdapter<CharSequence> holidayAdapter = ArrayAdapter.createFromResource(this,
                R.array.weekly_holidays, android.R.layout.simple_spinner_item);
        holidayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHoliday.setAdapter(holidayAdapter);
        spinnerHoliday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedHoliday = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Department Spinner
        spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDepartment = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ✅ Shift Spinner
        spinnerShift.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedShift = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ✅ FETCH DEPARTMENTS FROM FIREBASE
    private void loadDepartments() {
        departmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> departments = new ArrayList<>();
                departments.add("Select Department");

                for (DataSnapshot ds : snapshot.getChildren()) {
                    departments.add(ds.getKey());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        AddEmployeeActivity.this,
                        android.R.layout.simple_spinner_item,
                        departments);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDepartment.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
           Toast.makeText(AddEmployeeActivity.this, "Failed to load departments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadShifts() {
        shiftsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> shifts = new ArrayList<>();
                shifts.add("Select Shift");

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String shiftName = ds.getKey();
                    Object startObj = ds.child("startTime").getValue(Object.class);
                    Object endObj = ds.child("endTime").getValue(Object.class);
                    String startTime = startObj != null ? startObj.toString() : "N/A";
                    String endTime = endObj != null ? endObj.toString() : "N/A";
                    shifts.add(shiftName + " (" + startTime + " - " + endTime + ")");
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        AddEmployeeActivity.this,
                        android.R.layout.simple_spinner_item,
                        shifts);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerShift.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            Toast.makeText(AddEmployeeActivity.this, "Failed to load shifts", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setupClickListeners() {
        btnSaveEmployee.setOnClickListener(v -> saveEmployee());
    }

    private void saveEmployee() {
        String name = etEmpName.getText().toString().trim();
        String mobile = etEmpMobile.getText().toString().trim();
        String email = etEmpEmail.getText().toString().trim();  // ✅ Now used
        String password = etEmpPassword.getText().toString().trim();

        // ✅ Updated Validation
        if (TextUtils.isEmpty(name)) { etEmpName.setError("Enter name"); etEmpName.requestFocus(); return; }
        if (TextUtils.isEmpty(mobile)) { etEmpMobile.setError("Enter mobile"); etEmpMobile.requestFocus(); return; }
            if (TextUtils.isEmpty(email)) {
            etEmpEmail.setError("Email is required");
            etEmpEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etEmpPassword.setError("Password must be 6+ characters"); etEmpPassword.requestFocus(); return;
        }
        if ("Select Department".equals(selectedDepartment)) {
            Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show();
            spinnerDepartment.requestFocus();
            return;
        }
        if ("Select Shift".equals(selectedShift)) {
            Toast.makeText(this, "Please select a shift", Toast.LENGTH_SHORT).show();
            spinnerShift.requestFocus();
            return;
        }

        HashMap<String, Object> info = new HashMap<>();
        info.put("employeeName", name);
        info.put("employeeMobile", mobile);
        info.put("employeeEmail", email);
        info.put("employeePassword", password);
        info.put("employeeDepartment", selectedDepartment);
        info.put("employeeShift", selectedShift);  // ✅ Save full shift name + timing
        info.put("employeeRole", selectedRole);
        info.put("weeklyHoliday", selectedHoliday);
        info.put("employeeStatus", "ACTIVE");
        info.put("createdAt", System.currentTimeMillis());

        employeesRef.child(mobile).child("info").setValue(info)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "✅ Employee Added Successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "❌ Failed to add employee", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
