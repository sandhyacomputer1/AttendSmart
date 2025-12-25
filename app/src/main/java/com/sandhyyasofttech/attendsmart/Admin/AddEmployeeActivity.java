package com.sandhyyasofttech.attendsmart.Admin;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class AddEmployeeActivity extends AppCompatActivity {

    // UI Elements
    TextInputEditText etEmpName, etEmpMobile, etEmpEmail, etEmpPassword, etEmpDepartment, etShiftTime;
    Spinner spinnerRole, spinnerHoliday;
    MaterialButton btnSaveEmployee;

    DatabaseReference employeesRef;
    String companyKey;
    String selectedRole = "Employee";
    String selectedHoliday = "Sunday";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);

        initViews();
        setupCompanyReference();
        setupSpinners();
        setupClickListeners();
    }

    private void initViews() {
        etEmpName = findViewById(R.id.etEmpName);
        etEmpMobile = findViewById(R.id.etEmpMobile);
        etEmpEmail = findViewById(R.id.etEmpEmail);
        etEmpPassword = findViewById(R.id.etEmpPassword);
        etEmpDepartment = findViewById(R.id.etEmpDepartment);
        etShiftTime = findViewById(R.id.etShiftTime);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerHoliday = findViewById(R.id.spinnerHoliday);
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
        employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees");
    }

    private void setupSpinners() {
        // Role Spinner - FIXED
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

        // Holiday Spinner - FIXED
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
    }

    private void setupClickListeners() {
        etShiftTime.setOnClickListener(v -> showTimePicker());
        btnSaveEmployee.setOnClickListener(v -> saveEmployee());
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minuteOfDay) -> {
            String time = String.format("%02d:%02d", hourOfDay, minuteOfDay);
            etShiftTime.setText(time);
        }, hour, minute, false);
        timePicker.show();
    }

    private void saveEmployee() {
        String name = etEmpName.getText().toString().trim();
        String mobile = etEmpMobile.getText().toString().trim();
        String email = etEmpEmail.getText().toString().trim();
        String password = etEmpPassword.getText().toString().trim();
        String department = etEmpDepartment.getText().toString().trim();
        String shiftTime = etShiftTime.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) { etEmpName.setError("Enter name"); etEmpName.requestFocus(); return; }
        if (TextUtils.isEmpty(mobile)) { etEmpMobile.setError("Enter mobile"); etEmpMobile.requestFocus(); return; }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etEmpPassword.setError("Password must be 6+ characters"); etEmpPassword.requestFocus(); return;
        }
        if (TextUtils.isEmpty(department)) { etEmpDepartment.setError("Enter department"); etEmpDepartment.requestFocus(); return; }
        if (TextUtils.isEmpty(shiftTime)) { etShiftTime.setError("Select shift time"); etShiftTime.requestFocus(); return; }

        HashMap<String, Object> info = new HashMap<>();
        info.put("employeeName", name);
        info.put("employeeMobile", mobile);
        info.put("employeeEmail", email);
        info.put("employeePassword", password); // Store hashed in production
        info.put("employeeDepartment", department);
        info.put("employeeRole", selectedRole);
        info.put("shiftTime", shiftTime);
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
