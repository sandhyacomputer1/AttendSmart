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
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
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
    private boolean isEditMode = false;
    private String editingEmployeeId = "";
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
        checkEditMode();

    }


    private void checkEditMode() {
        EmployeeModel existingEmployee = (EmployeeModel) getIntent().getSerializableExtra("employee");
        isEditMode = getIntent().getStringExtra("mode") != null &&
                "EDIT".equals(getIntent().getStringExtra("mode"));

        if (isEditMode && existingEmployee != null) {
            populateEditData(existingEmployee);
            editingEmployeeId = existingEmployee.getEmployeeId();
            toolbar.setTitle("Edit Employee");
            btnSaveEmployee.setText("Update Employee");
            etEmpPassword.setHint("Leave blank to keep current password");
        }
    }
    private void populateEditData(EmployeeModel employee) {
        etEmpName.setText(employee.getEmployeeName());
        etEmpMobile.setText(employee.getEmployeeMobile());
        etEmpEmail.setText(employee.getEmployeeEmail());
        etSalary.setText(employee.getSalary() != null ? employee.getSalary() : "");
        etAddress.setText(employee.getAddress() != null ? employee.getAddress() : "");
        etEmergencyContact.setText(employee.getEmergencyContact() != null ? employee.getEmergencyContact() : "");

        // Set spinners to correct positions
        setSpinnerSelection(spinnerRole, employee.getEmployeeRole(), "Employee");
        setSpinnerSelection(spinnerHoliday, employee.getWeeklyHoliday(), "Sunday");

        // Parse and set joining date
        if (employee.getJoinDate() != null && !employee.getJoinDate().isEmpty()) {
            try {
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                joiningDate = employee.getJoinDate();
                etJoiningDate.setText(displayFormat.format(parseFormat.parse(joiningDate)));
            } catch (Exception e) {
                Log.e(TAG, "Date parse error: " + e.getMessage());
            }
        }

        // Load departments and shifts first, then set selections
        loadDepartmentsForEdit(employee.getEmployeeDepartment());
        loadShiftsForEdit(employee.getEmployeeShift());
    }
    private void setSpinnerSelection(Spinner spinner, String value, String defaultValue) {
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                    spinner.setSelection(i);
                    return;
                }
            }
        }
        selectedRole = defaultValue; // Fallback
    }

    private void loadDepartmentsForEdit(final String selectedDept) {
        departmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                        AddEmployeeActivity.this, android.R.layout.simple_spinner_item, departmentsList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDepartment.setAdapter(adapter);

                // Set selected department after adapter is set
                setSpinnerSelection(spinnerDepartment, selectedDept, "");
                selectedDepartment = selectedDept;
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadShiftsForEdit(final String selectedShiftKeyParam) {
        shiftsRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                        AddEmployeeActivity.this, android.R.layout.simple_spinner_item, shiftsList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerShift.setAdapter(adapter);

                // ✅ FIXED: Set selection properly
                for (int i = 0; i < shiftKeysList.size(); i++) {
                    if (shiftKeysList.get(i).equals(selectedShiftKeyParam)) {
                        spinnerShift.setSelection(i);
                        selectedShiftKey = selectedShiftKeyParam;  // ✅ FIXED variable name
                        selectedShift = shiftsList.get(i);
                        break;
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        if (TextUtils.isEmpty(name) || name.length() < 3) {
            etEmpName.setError("Valid name required (min 3 chars)");
            etEmpName.requestFocus();
            return false;
        }

        // Mobile validation
        if (TextUtils.isEmpty(mobile) || mobile.length() != 10 || !mobile.matches("[6-9][0-9]{9}")) {
            etEmpMobile.setError("Valid 10-digit Indian mobile required");
            etEmpMobile.requestFocus();
            return false;
        }

        // Email validation
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmpEmail.setError("Valid email required");
            etEmpEmail.requestFocus();
            return false;
        }

        // Password validation (skip for edit mode if empty)
        if (!isEditMode) {
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                etEmpPassword.setError("Password required (min 6 chars)");
                etEmpPassword.requestFocus();
                return false;
            }
        } else if (!TextUtils.isEmpty(password) && password.length() < 6) {
            etEmpPassword.setError("Password must be min 6 chars");
            return false;
        }

        // Required selections
        if (TextUtils.isEmpty(joiningDate)) {
            etJoiningDate.setError("Joining date required");
            return false;
        }
        if (TextUtils.isEmpty(selectedDepartment) || "Select Department".equals(selectedDepartment)) {
            Toast.makeText(this, "Please select department", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(selectedShiftKey) || "Select Shift".equals(selectedShift)) {
            Toast.makeText(this, "Please select shift", Toast.LENGTH_SHORT).show();
            return false;
        }

        // ✅ FIXED: Async mobile check with proper callback
        checkMobileExists(mobile);
        return false;

    }

    private void checkMobileExists(String mobile) {
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean mobileExists = false;

                // Check all employees
                for (DataSnapshot empSnap : snapshot.getChildren()) {
                    DataSnapshot infoSnap = empSnap.child("info");
                    if (infoSnap.exists()) {
                        String existingMobile = "";
                        Object mobileObj = infoSnap.child("employeeMobile").getValue();
                        if (mobileObj != null) {
                            existingMobile = mobileObj.toString();
                        }

                        if (mobile.equals(existingMobile)) {
                            // Allow editing same employee
                            if (isEditMode && editingEmployeeId.equals(empSnap.getKey())) {
                                saveEmployee();
                                return;
                            }
                            mobileExists = true;
                            break;
                        }
                    }
                }

                if (mobileExists) {
                    etEmpMobile.setError("Mobile already registered");
                    Toast.makeText(AddEmployeeActivity.this, "Mobile already exists", Toast.LENGTH_SHORT).show();
                } else {
                    showSaveConfirmation();
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

        String title = isEditMode ? "Update Employee?" : "Add Employee?";
        String message = "Name: " + name + "\n" +
                "Mobile: " + mobile + "\n" +
                "Department: " + department + "\n" +
                "Joining Date: " + etJoiningDate.getText().toString() + "\n\n" +
                "Continue?";

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> saveEmployee())
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();  // ✅ FIXED: Added .show()
    }

    private void saveEmployee() {
        btnSaveEmployee.setEnabled(false);
        btnSaveEmployee.setText(isEditMode ? "Updating..." : "Saving...");

        String name = etEmpName.getText().toString().trim();
        String mobile = etEmpMobile.getText().toString().trim();
        String email = etEmpEmail.getText().toString().trim();
        String password = etEmpPassword.getText().toString().trim();
        String salary = etSalary.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String emergencyContact = etEmergencyContact.getText().toString().trim();

        HashMap<String, Object> employeeInfo = new HashMap<>();
        employeeInfo.put("employeeName", name);
        employeeInfo.put("employeeMobile", mobile);
        employeeInfo.put("employeeEmail", email);
        employeeInfo.put("employeeDepartment", selectedDepartment);
        employeeInfo.put("employeeShift", selectedShiftKey);  // ✅ FIXED: selectedShiftKey
        employeeInfo.put("employeeRole", selectedRole);
        employeeInfo.put("weeklyHoliday", selectedHoliday);
        employeeInfo.put("joinDate", joiningDate);
        employeeInfo.put("employeeStatus", "ACTIVE");

        // Password handling
        if (!isEditMode || !TextUtils.isEmpty(password)) {
            employeeInfo.put("employeePassword", password);
        }

        // Optional fields
        if (!TextUtils.isEmpty(salary)) employeeInfo.put("salary", salary);
        if (!TextUtils.isEmpty(address)) employeeInfo.put("address", address);
        if (!TextUtils.isEmpty(emergencyContact)) employeeInfo.put("emergencyContact", emergencyContact);

        if (!isEditMode) {
            employeeInfo.put("createdAt", System.currentTimeMillis());
        }

        DatabaseReference targetRef = isEditMode ?
                employeesRef.child(editingEmployeeId).child("info") :
                employeesRef.child(mobile).child("info");

        targetRef.setValue(employeeInfo)
                .addOnCompleteListener(task -> {
                    btnSaveEmployee.setEnabled(true);
                    btnSaveEmployee.setText(isEditMode ? "Update Employee" : "Save Employee");

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "✅ " + (isEditMode ? "Updated" : "Added") + " successfully!",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "❌ Failed: " + task.getException().getMessage(),
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