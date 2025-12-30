//package com.sandhyyasofttech.attendsmart.Admin;
//
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.util.Patterns;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.Spinner;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.dialog.MaterialAlertDialogBuilder;
//import com.google.android.material.textfield.TextInputEditText;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//public class AddEmployeeActivity extends AppCompatActivity {
//
//    // UI Elements
//    private MaterialToolbar toolbar;
//    private TextInputEditText etEmpName, etEmpMobile, etEmpEmail, etEmpPassword;
//    private Spinner spinnerRole, spinnerHoliday, spinnerDepartment, spinnerShift;
//    private MaterialButton btnSaveEmployee;
//
//    // Firebase References
//    private DatabaseReference employeesRef, departmentsRef, shiftsRef;
//    private String companyKey;
//
//    // Selected Values
//    private String selectedRole = "Employee";
//    private String selectedHoliday = "Sunday";
//    private String selectedDepartment = "";
//    private String selectedShift = "";
//    private String selectedShiftKey = ""; // Store shift key for proper mapping
//
//    // Data Lists
//    private ArrayList<String> departmentsList = new ArrayList<>();
//    private ArrayList<String> shiftsList = new ArrayList<>();
//    private ArrayList<String> shiftKeysList = new ArrayList<>(); // Store shift keys
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_add_employee);
//
//        initializeViews();
//        setupToolbar();
//        setupCompanyReference();
//        setupSpinners();
//        loadDepartments();
//        loadShifts();
//        setupClickListeners();
//    }
//
//    /**
//     * Initialize all views
//     */
//    private void initializeViews() {
//        toolbar = findViewById(R.id.toolbar);
//
//        // Input fields
//        etEmpName = findViewById(R.id.etEmpName);
//        etEmpMobile = findViewById(R.id.etEmpMobile);
//        etEmpEmail = findViewById(R.id.etEmpEmail);
//        etEmpPassword = findViewById(R.id.etEmpPassword);
//
//        // Spinners
//        spinnerRole = findViewById(R.id.spinnerRole);
//        spinnerHoliday = findViewById(R.id.spinnerHoliday);
//        spinnerDepartment = findViewById(R.id.spinnerDepartment);
//        spinnerShift = findViewById(R.id.spinnerShift);
//
//        // Button
//        btnSaveEmployee = findViewById(R.id.btnSaveEmployee);
//    }
//
//    /**
//     * Setup toolbar with back navigation
//     */
//    private void setupToolbar() {
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
//            getSupportActionBar().setTitle("Add New Employee");
//        }
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            onBackPressed();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    /**
//     * Setup company Firebase reference
//     */
//    private void setupCompanyReference() {
//        PrefManager prefManager = new PrefManager(this);
//        String email = prefManager.getUserEmail();
//
//        if (email == null || email.isEmpty()) {
//            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        companyKey = email.replace(".", ",");
//
//        DatabaseReference companyRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey);
//
//        employeesRef = companyRef.child("employees");
//        departmentsRef = companyRef.child("departments");
//        shiftsRef = companyRef.child("shifts");
//    }
//
//    /**
//     * Setup all spinners with adapters and listeners
//     */
//    private void setupSpinners() {
//        // Role Spinner
//        setupRoleSpinner();
//
//        // Holiday Spinner
//        setupHolidaySpinner();
//
//        // Department Spinner Listener
//        spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                if (position > 0) { // Skip "Select Department"
//                    selectedDepartment = departmentsList.get(position);
//                } else {
//                    selectedDepartment = "";
//                }
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                selectedDepartment = "";
//            }
//        });
//
//        // Shift Spinner Listener
//        spinnerShift.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                if (position > 0) { // Skip "Select Shift"
//                    selectedShift = shiftsList.get(position);
//                    selectedShiftKey = shiftKeysList.get(position);
//                } else {
//                    selectedShift = "";
//                    selectedShiftKey = "";
//                }
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                selectedShift = "";
//                selectedShiftKey = "";
//            }
//        });
//    }
//
//    /**
//     * Setup Role Spinner
//     */
//    private void setupRoleSpinner() {
//        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(this,
//                R.array.employee_roles, android.R.layout.simple_spinner_item);
//        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerRole.setAdapter(roleAdapter);
//
//        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                selectedRole = parent.getItemAtPosition(position).toString();
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                selectedRole = "Employee";
//            }
//        });
//    }
//
//    /**
//     * Setup Holiday Spinner
//     */
//    private void setupHolidaySpinner() {
//        ArrayAdapter<CharSequence> holidayAdapter = ArrayAdapter.createFromResource(this,
//                R.array.weekly_holidays, android.R.layout.simple_spinner_item);
//        holidayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerHoliday.setAdapter(holidayAdapter);
//
//        spinnerHoliday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                selectedHoliday = parent.getItemAtPosition(position).toString();
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                selectedHoliday = "Sunday";
//            }
//        });
//    }
//
//    /**
//     * Load departments from Firebase
//     */
//    private void loadDepartments() {
//        departmentsRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                departmentsList.clear();
//                departmentsList.add("Select Department");
//
//                if (snapshot.exists()) {
//                    for (DataSnapshot ds : snapshot.getChildren()) {
//                        String deptName = ds.getKey();
//                        if (deptName != null) {
//                            departmentsList.add(deptName);
//                        }
//                    }
//                }
//
//                ArrayAdapter<String> adapter = new ArrayAdapter<>(
//                        AddEmployeeActivity.this,
//                        android.R.layout.simple_spinner_item,
//                        departmentsList);
//                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                spinnerDepartment.setAdapter(adapter);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(AddEmployeeActivity.this,
//                        "Failed to load departments: " + error.getMessage(),
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    /**
//     * Load shifts from Firebase
//     */
//    private void loadShifts() {
//        shiftsRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                shiftsList.clear();
//                shiftKeysList.clear();
//
//                shiftsList.add("Select Shift");
//                shiftKeysList.add("");
//
//                if (snapshot.exists()) {
//                    for (DataSnapshot ds : snapshot.getChildren()) {
//                        String shiftKey = ds.getKey();
//                        Object startObj = ds.child("startTime").getValue();
//                        Object endObj = ds.child("endTime").getValue();
//
//                        String startTime = startObj != null ? startObj.toString() : "N/A";
//                        String endTime = endObj != null ? endObj.toString() : "N/A";
//
//                        String displayText = shiftKey + " (" + startTime + " - " + endTime + ")";
//
//                        shiftsList.add(displayText);
//                        shiftKeysList.add(shiftKey);
//                    }
//                }
//
//                ArrayAdapter<String> adapter = new ArrayAdapter<>(
//                        AddEmployeeActivity.this,
//                        android.R.layout.simple_spinner_item,
//                        shiftsList);
//                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                spinnerShift.setAdapter(adapter);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(AddEmployeeActivity.this,
//                        "Failed to load shifts: " + error.getMessage(),
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    /**
//     * Setup click listeners
//     */
//    private void setupClickListeners() {
//        btnSaveEmployee.setOnClickListener(v -> {
//            if (validateInputs()) {
//                showSaveConfirmation();
//            }
//        });
//    }
//
//    /**
//     * Validate all input fields
//     */
//    private boolean validateInputs() {
//        String name = etEmpName.getText().toString().trim();
//        String mobile = etEmpMobile.getText().toString().trim();
//        String email = etEmpEmail.getText().toString().trim();
//        String password = etEmpPassword.getText().toString().trim();
//
//        // Name validation
//        if (TextUtils.isEmpty(name)) {
//            etEmpName.setError("Name is required");
//            etEmpName.requestFocus();
//            return false;
//        }
//
//        // Mobile validation
//        if (TextUtils.isEmpty(mobile)) {
//            etEmpMobile.setError("Mobile number is required");
//            etEmpMobile.requestFocus();
//            return false;
//        }
//        if (mobile.length() != 10) {
//            etEmpMobile.setError("Enter valid 10-digit mobile number");
//            etEmpMobile.requestFocus();
//            return false;
//        }
//
//        // Email validation
//        if (TextUtils.isEmpty(email)) {
//            etEmpEmail.setError("Email is required");
//            etEmpEmail.requestFocus();
//            return false;
//        }
//        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//            etEmpEmail.setError("Enter valid email address");
//            etEmpEmail.requestFocus();
//            return false;
//        }
//
//        // Password validation
//        if (TextUtils.isEmpty(password)) {
//            etEmpPassword.setError("Password is required");
//            etEmpPassword.requestFocus();
//            return false;
//        }
//        if (password.length() < 6) {
//            etEmpPassword.setError("Password must be at least 6 characters");
//            etEmpPassword.requestFocus();
//            return false;
//        }
//
//        // Department validation
//        if (TextUtils.isEmpty(selectedDepartment) || "Select Department".equals(selectedDepartment)) {
//            Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show();
//            spinnerDepartment.requestFocus();
//            return false;
//        }
//
//        // Shift validation
//        if (TextUtils.isEmpty(selectedShift) || "Select Shift".equals(selectedShift)) {
//            Toast.makeText(this, "Please select a shift", Toast.LENGTH_SHORT).show();
//            spinnerShift.requestFocus();
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * Show confirmation dialog before saving
//     */
//    private void showSaveConfirmation() {
//        String name = etEmpName.getText().toString().trim();
//
//        new MaterialAlertDialogBuilder(this)
//                .setTitle("Confirm Employee Details")
//                .setMessage("Are you sure you want to add " + name + " as a new employee?")
//                .setPositiveButton("Yes, Save", (dialog, which) -> saveEmployee())
//                .setNegativeButton("Cancel", null)
//                .show();
//    }
//
//    /**
//     * Save employee to Firebase
//     */
//    private void saveEmployee() {
//        // Show progress
//        btnSaveEmployee.setEnabled(false);
//        btnSaveEmployee.setText("Saving...");
//
//        String name = etEmpName.getText().toString().trim();
//        String mobile = etEmpMobile.getText().toString().trim();
//        String email = etEmpEmail.getText().toString().trim();
//        String password = etEmpPassword.getText().toString().trim();
//
//        // Create employee data
//        HashMap<String, Object> employeeInfo = new HashMap<>();
//        employeeInfo.put("employeeName", name);
//        employeeInfo.put("employeeMobile", mobile);
//        employeeInfo.put("employeeEmail", email);
//        employeeInfo.put("employeePassword", password);
//        employeeInfo.put("employeeDepartment", selectedDepartment);
//        employeeInfo.put("employeeShift", selectedShiftKey); // Store shift key
//        employeeInfo.put("employeeRole", selectedRole);
//        employeeInfo.put("weeklyHoliday", selectedHoliday);
//        employeeInfo.put("employeeStatus", "ACTIVE");
//        employeeInfo.put("createdAt", System.currentTimeMillis());
//
//        // Save to Firebase
//        employeesRef.child(mobile).child("info").setValue(employeeInfo)
//                .addOnCompleteListener(task -> {
//                    btnSaveEmployee.setEnabled(true);
//                    btnSaveEmployee.setText("Save Employee");
//
//                    if (task.isSuccessful()) {
//                        showSuccessDialog(name);
//                    } else {
//                        String errorMsg = task.getException() != null ?
//                                task.getException().getMessage() : "Unknown error";
//                        Toast.makeText(this, "Failed to add employee: " + errorMsg,
//                                Toast.LENGTH_LONG).show();
//                    }
//                });
//    }
//
//    /**
//     * Show success dialog and finish activity
//     */
//    private void showSuccessDialog(String employeeName) {
//        new MaterialAlertDialogBuilder(this)
//                .setTitle("Success!")
//                .setMessage(employeeName + " has been added successfully.")
//                .setPositiveButton("OK", (dialog, which) -> finish())
//                .setCancelable(false)
//                .show();
//    }
//
//    @Override
//    public void onBackPressed() {
//        if (isFormFilled()) {
//            new MaterialAlertDialogBuilder(this)
//                    .setTitle("Discard Changes?")
//                    .setMessage("You have unsaved changes. Are you sure you want to go back?")
//                    .setPositiveButton("Discard", (dialog, which) -> super.onBackPressed())
//                    .setNegativeButton("Cancel", null)
//                    .show();
//        } else {
//            super.onBackPressed();
//        }
//    }
//
//    /**
//     * Check if any form field is filled
//     */
//    private boolean isFormFilled() {
//        return !TextUtils.isEmpty(etEmpName.getText()) ||
//                !TextUtils.isEmpty(etEmpMobile.getText()) ||
//                !TextUtils.isEmpty(etEmpEmail.getText()) ||
//                !TextUtils.isEmpty(etEmpPassword.getText());
//    }
//}




package com.sandhyyasofttech.attendsmart.Admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class AddEmployeeActivity extends AppCompatActivity {

    private static final String TAG = "AddEmployee";

    // UI Elements
    private MaterialToolbar toolbar;
    private TextInputEditText etEmpName, etEmpMobile, etEmpEmail, etEmpPassword;
    private TextInputEditText etJoiningDate, etSalary, etAddress, etEmergencyContact;
    private Spinner spinnerRole, spinnerHoliday, spinnerDepartment, spinnerShift;
    private MaterialButton btnSaveEmployee;

    // Firebase References
    private DatabaseReference employeesRef, departmentsRef, shiftsRef;
    private String companyKey;

    // Selected Values
    private String selectedRole = "Employee";
    private String selectedHoliday = "Sunday";
    private String selectedDepartment = "";
    private String selectedShift = "";
    private String selectedShiftKey = "";
    private String joiningDate = "";
    private Calendar joiningCalendar;

    // Data Lists
    private ArrayList<String> departmentsList = new ArrayList<>();
    private ArrayList<String> shiftsList = new ArrayList<>();
    private ArrayList<String> shiftKeysList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);

        initializeViews();
        setupToolbar();
        setupCompanyReference();
        setupSpinners();
        setupDatePicker();
        loadDepartments();
        loadShifts();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);

        // Basic Info
        etEmpName = findViewById(R.id.etEmpName);
        etEmpMobile = findViewById(R.id.etEmpMobile);
        etEmpEmail = findViewById(R.id.etEmpEmail);
        etEmpPassword = findViewById(R.id.etEmpPassword);

        // Additional Info
        etJoiningDate = findViewById(R.id.etJoiningDate);
        etSalary = findViewById(R.id.etSalary);
        etAddress = findViewById(R.id.etAddress);
        etEmergencyContact = findViewById(R.id.etEmergencyContact);

        // Spinners
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerHoliday = findViewById(R.id.spinnerHoliday);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerShift = findViewById(R.id.spinnerShift);

        // Button
        btnSaveEmployee = findViewById(R.id.btnSaveEmployee);

        joiningCalendar = Calendar.getInstance();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Add New Employee");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupCompanyReference() {
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();

        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        companyKey = email.replace(".", ",");
        Log.d(TAG, "Company Key: " + companyKey);

        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey);

        employeesRef = companyRef.child("employees");
        departmentsRef = companyRef.child("departments");
        shiftsRef = companyRef.child("shifts");
    }

    private void setupDatePicker() {
        etJoiningDate.setFocusable(false);
        etJoiningDate.setClickable(true);

        etJoiningDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        joiningCalendar.set(Calendar.YEAR, year);
                        joiningCalendar.set(Calendar.MONTH, month);
                        joiningCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        joiningDate = sdf.format(joiningCalendar.getTime());

                        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        etJoiningDate.setText(displayFormat.format(joiningCalendar.getTime()));
                    },
                    joiningCalendar.get(Calendar.YEAR),
                    joiningCalendar.get(Calendar.MONTH),
                    joiningCalendar.get(Calendar.DAY_OF_MONTH)
            );

            // Set max date to today
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void setupSpinners() {
        setupRoleSpinner();
        setupHolidaySpinner();

        spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedDepartment = departmentsList.get(position);
                } else {
                    selectedDepartment = "";
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDepartment = "";
            }
        });

        spinnerShift.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedShift = shiftsList.get(position);
                    selectedShiftKey = shiftKeysList.get(position);
                } else {
                    selectedShift = "";
                    selectedShiftKey = "";
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedShift = "";
                selectedShiftKey = "";
            }
        });
    }

    private void setupRoleSpinner() {
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
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRole = "Employee";
            }
        });
    }

    private void setupHolidaySpinner() {
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
            public void onNothingSelected(AdapterView<?> parent) {
                selectedHoliday = "Sunday";
            }
        });
    }

    private void loadDepartments() {
        departmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                departmentsList.clear();
                departmentsList.add("Select Department");

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String deptName = ds.getKey();
                        if (deptName != null) {
                            departmentsList.add(deptName);
                        }
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        AddEmployeeActivity.this,
                        android.R.layout.simple_spinner_item,
                        departmentsList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDepartment.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddEmployeeActivity.this,
                        "Failed to load departments: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadShifts() {
        shiftsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                shiftsList.clear();
                shiftKeysList.clear();

                shiftsList.add("Select Shift");
                shiftKeysList.add("");

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String shiftKey = ds.getKey();
                        Object startObj = ds.child("startTime").getValue();
                        Object endObj = ds.child("endTime").getValue();

                        String startTime = startObj != null ? startObj.toString() : "N/A";
                        String endTime = endObj != null ? endObj.toString() : "N/A";

                        String displayText = shiftKey + " (" + startTime + " - " + endTime + ")";

                        shiftsList.add(displayText);
                        shiftKeysList.add(shiftKey);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        AddEmployeeActivity.this,
                        android.R.layout.simple_spinner_item,
                        shiftsList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerShift.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddEmployeeActivity.this,
                        "Failed to load shifts: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        btnSaveEmployee.setOnClickListener(v -> {
            if (validateInputs()) {
                showSaveConfirmation();
            }
        });
    }

    private boolean validateInputs() {
        String name = etEmpName.getText().toString().trim();
        String mobile = etEmpMobile.getText().toString().trim();
        String email = etEmpEmail.getText().toString().trim();
        String password = etEmpPassword.getText().toString().trim();

        // Name validation
        if (TextUtils.isEmpty(name)) {
            etEmpName.setError("Name is required");
            etEmpName.requestFocus();
            return false;
        }
        if (name.length() < 3) {
            etEmpName.setError("Name must be at least 3 characters");
            etEmpName.requestFocus();
            return false;
        }

        // Mobile validation
        if (TextUtils.isEmpty(mobile)) {
            etEmpMobile.setError("Mobile number is required");
            etEmpMobile.requestFocus();
            return false;
        }
        if (mobile.length() != 10) {
            etEmpMobile.setError("Enter valid 10-digit mobile number");
            etEmpMobile.requestFocus();
            return false;
        }
        if (!mobile.matches("[6-9][0-9]{9}")) {
            etEmpMobile.setError("Enter valid Indian mobile number");
            etEmpMobile.requestFocus();
            return false;
        }

        // Email validation
        if (TextUtils.isEmpty(email)) {
            etEmpEmail.setError("Email is required");
            etEmpEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmpEmail.setError("Enter valid email address");
            etEmpEmail.requestFocus();
            return false;
        }

        // Password validation
        if (TextUtils.isEmpty(password)) {
            etEmpPassword.setError("Password is required");
            etEmpPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            etEmpPassword.setError("Password must be at least 6 characters");
            etEmpPassword.requestFocus();
            return false;
        }

        // Joining Date validation
        if (TextUtils.isEmpty(joiningDate)) {
            etJoiningDate.setError("Joining date is required");
            Toast.makeText(this, "Please select joining date", Toast.LENGTH_SHORT).show();
            etJoiningDate.requestFocus();
            return false;
        }

        // Department validation
        if (TextUtils.isEmpty(selectedDepartment) || "Select Department".equals(selectedDepartment)) {
            Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show();
            spinnerDepartment.requestFocus();
            return false;
        }

        // Shift validation
        if (TextUtils.isEmpty(selectedShift) || "Select Shift".equals(selectedShift)) {
            Toast.makeText(this, "Please select a shift", Toast.LENGTH_SHORT).show();
            spinnerShift.requestFocus();
            return false;
        }

        // Check if mobile already exists
        checkMobileExists(mobile);

        return true;
    }

    private void checkMobileExists(String mobile) {
        employeesRef.child(mobile).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    etEmpMobile.setError("This mobile number is already registered");
                    Toast.makeText(AddEmployeeActivity.this,
                            "Employee with this mobile already exists",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking mobile: " + error.getMessage());
            }
        });
    }

    private void showSaveConfirmation() {
        String name = etEmpName.getText().toString().trim();
        String mobile = etEmpMobile.getText().toString().trim();
        String department = selectedDepartment;

        String message = "Name: " + name + "\n" +
                "Mobile: " + mobile + "\n" +
                "Department: " + department + "\n" +
                "Joining Date: " + etJoiningDate.getText().toString() + "\n\n" +
                "Add this employee?";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Employee Details")
                .setMessage(message)
                .setPositiveButton("Yes, Save", (dialog, which) -> saveEmployee())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveEmployee() {
        btnSaveEmployee.setEnabled(false);
        btnSaveEmployee.setText("Saving...");

        String name = etEmpName.getText().toString().trim();
        String mobile = etEmpMobile.getText().toString().trim();
        String email = etEmpEmail.getText().toString().trim();
        String password = etEmpPassword.getText().toString().trim();
        String salary = etSalary.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String emergencyContact = etEmergencyContact.getText().toString().trim();

        // Create employee data with proper structure
        HashMap<String, Object> employeeInfo = new HashMap<>();
        employeeInfo.put("employeeName", name);
        employeeInfo.put("name", name); // For easier queries
        employeeInfo.put("employeeMobile", mobile);
        employeeInfo.put("employeeEmail", email);
        employeeInfo.put("employeePassword", password);
        employeeInfo.put("employeeDepartment", selectedDepartment);
        employeeInfo.put("employeeShift", selectedShiftKey);
        employeeInfo.put("employeeRole", selectedRole);
        employeeInfo.put("weeklyHoliday", selectedHoliday);
        employeeInfo.put("joiningDate", joiningDate); // Store in yyyy-MM-dd format
        employeeInfo.put("employeeStatus", "ACTIVE");
        employeeInfo.put("createdAt", System.currentTimeMillis());

        // Optional fields
        if (!TextUtils.isEmpty(salary)) {
            employeeInfo.put("salary", salary);
        }
        if (!TextUtils.isEmpty(address)) {
            employeeInfo.put("address", address);
        }
        if (!TextUtils.isEmpty(emergencyContact)) {
            employeeInfo.put("emergencyContact", emergencyContact);
        }

        Log.d(TAG, "Saving employee with mobile: " + mobile);
        Log.d(TAG, "Joining date: " + joiningDate);

        // Save to Firebase
        employeesRef.child(mobile).child("info").setValue(employeeInfo)
                .addOnCompleteListener(task -> {
                    btnSaveEmployee.setEnabled(true);
                    btnSaveEmployee.setText("Save Employee");

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Employee saved successfully");
                        showSuccessDialog(name);
                    } else {
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Failed to save employee: " + errorMsg);
                        Toast.makeText(this, "Failed to add employee: " + errorMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showSuccessDialog(String employeeName) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Success!")
                .setMessage(employeeName + " has been added successfully.\n\n" +
                        "Login Credentials:\n" +
                        "Email: " + etEmpEmail.getText().toString() + "\n" +
                        "Password: " + etEmpPassword.getText().toString())
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (isFormFilled()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Discard Changes?")
                    .setMessage("You have unsaved changes. Are you sure you want to go back?")
                    .setPositiveButton("Discard", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean isFormFilled() {
        return !TextUtils.isEmpty(etEmpName.getText()) ||
                !TextUtils.isEmpty(etEmpMobile.getText()) ||
                !TextUtils.isEmpty(etEmpEmail.getText()) ||
                !TextUtils.isEmpty(etEmpPassword.getText());
    }
}