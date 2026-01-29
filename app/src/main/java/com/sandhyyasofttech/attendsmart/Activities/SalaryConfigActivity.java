package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SalaryConfigActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private EditText etMonthlySalary, etWorkingDays, etPerDaySalary;
    private EditText etPaidLeaves, etEffectiveFrom;
    private Spinner spLateRule;
    private SwitchMaterial switchDeduction;
    private EditText etPfPercent, etEsiPercent, etOtherDeduction, etDeductionNote;
    private TextInputLayout tilPf, tilEsi, tilOtherDeduction, tilDeductionNote;
    private Button btnSave, btnDelete;
    // Bank Details
    private EditText etBankName, etAccountHolder, etAccountNumber, etIfscCode, etBranchName;


    // Firebase
    private DatabaseReference salaryRef;
    private String companyKey, employeeMobile, employeeName;

    // State
    private boolean isEditMode = false;
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_config);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        initViews();
        setupToolbar();
        setupLateRuleSpinner();
        setupDeductionToggle();
        setupEffectiveMonthPicker();
        setupAutoCalculation();
        initFirebase();
        fetchSalaryConfig();

        btnSave.setOnClickListener(v -> saveSalaryConfig());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        etMonthlySalary = findViewById(R.id.etMonthlySalary);
        etWorkingDays = findViewById(R.id.etWorkingDays);
        etPerDaySalary = findViewById(R.id.etPerDaySalary);
        etPaidLeaves = findViewById(R.id.etPaidLeaves);
        etEffectiveFrom = findViewById(R.id.etEffectiveFrom);

        spLateRule = findViewById(R.id.spLateRule);

        switchDeduction = findViewById(R.id.switchDeduction);
        etPfPercent = findViewById(R.id.etPfPercent);
        etEsiPercent = findViewById(R.id.etEsiPercent);
        etOtherDeduction = findViewById(R.id.etOtherDeduction);
        etDeductionNote = findViewById(R.id.etDeductionNote);

        tilPf = findViewById(R.id.tilPf);
        tilEsi = findViewById(R.id.tilEsi);
        tilOtherDeduction = findViewById(R.id.tilOtherDeduction);
        tilDeductionNote = findViewById(R.id.tilDeductionNote);

        btnSave = findViewById(R.id.btnSaveSalary);
        btnDelete = findViewById(R.id.btnDeleteSalary);

        etBankName = findViewById(R.id.etBankName);
        etAccountHolder = findViewById(R.id.etAccountHolder);
        etAccountNumber = findViewById(R.id.etAccountNumber);
        etIfscCode = findViewById(R.id.etIfscCode);
        etBranchName = findViewById(R.id.etBranchName);

    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Update title if employee name is provided
        if (employeeName != null && !employeeName.isEmpty()) {
            toolbar.setTitle("Salary Config - " + employeeName);
        }
    }

    private void setupLateRuleSpinner() {
        String[] rules = {
                "No deduction for late coming",
                "3 Late marks = 0.5 Day deduction",
                "5 Late marks = 1 Day deduction"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                rules
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLateRule.setAdapter(adapter);
    }

    private void setupDeductionToggle() {
        toggleDeductionFields(false);
        switchDeduction.setOnCheckedChangeListener((buttonView, isChecked) ->
                toggleDeductionFields(isChecked)
        );
    }

    private void toggleDeductionFields(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        tilPf.setVisibility(visibility);
        tilEsi.setVisibility(visibility);
        tilOtherDeduction.setVisibility(visibility);
        tilDeductionNote.setVisibility(visibility);
    }

    private void setupEffectiveMonthPicker() {
        etEffectiveFrom.setOnClickListener(v -> showMonthYearPicker());
    }

    private void showMonthYearPicker() {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d-%d", month + 1, year);
                    etEffectiveFrom.setText(selectedDate);
                },
                currentYear,
                currentMonth,
                1
        );

        datePickerDialog.show();
    }

    private void setupAutoCalculation() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculatePerDaySalary();
            }
        };

        etMonthlySalary.addTextChangedListener(textWatcher);
        etWorkingDays.addTextChangedListener(textWatcher);
    }

    private void calculatePerDaySalary() {
        String salaryStr = etMonthlySalary.getText().toString().trim();
        String workingDaysStr = etWorkingDays.getText().toString().trim();

        if (!salaryStr.isEmpty() && !workingDaysStr.isEmpty()) {
            try {
                double salary = Double.parseDouble(salaryStr);
                int workingDays = Integer.parseInt(workingDaysStr);

                if (workingDays > 0) {
                    double perDay = salary / workingDays;
                    etPerDaySalary.setText(decimalFormat.format(perDay));
                } else {
                    etPerDaySalary.setText("");
                }
            } catch (NumberFormatException e) {
                etPerDaySalary.setText("");
            }
        } else {
            etPerDaySalary.setText("");
        }
    }

    private void initFirebase() {
        employeeMobile = getIntent().getStringExtra("employeeMobile");
        employeeName = getIntent().getStringExtra("employeeName");

        if (employeeMobile == null || employeeMobile.isEmpty()) {
            Toast.makeText(this, "Error: Employee mobile not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();

        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Error: Company email not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        companyKey = email.replace(".", ",");

        salaryRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("salaryConfig");
    }

    private void fetchSalaryConfig() {
        salaryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isEditMode = true;
                    btnSave.setText("Update Salary Configuration");
                    btnDelete.setVisibility(View.VISIBLE);
                    populateFields(snapshot);
                } else {
                    isEditMode = false;
                    btnSave.setText("Save Salary Configuration");
                    btnDelete.setVisibility(View.GONE);
                    setDefaultValues();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SalaryConfigActivity.this,
                        "Failed to load salary data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFields(DataSnapshot snapshot) {
        etMonthlySalary.setText(getStringValue(snapshot, "monthlySalary"));
        etWorkingDays.setText(getStringValue(snapshot, "workingDays"));
        etPaidLeaves.setText(getStringValue(snapshot, "paidLeaves"));
        etEffectiveFrom.setText(getStringValue(snapshot, "effectiveFrom"));
        etBankName.setText(getStringValue(snapshot, "bankName"));
        etAccountHolder.setText(getStringValue(snapshot, "accountHolder"));
        etAccountNumber.setText(getStringValue(snapshot, "accountNumber"));
        etIfscCode.setText(getStringValue(snapshot, "ifscCode"));
        etBranchName.setText(getStringValue(snapshot, "branchName"));

        String lateRule = getStringValue(snapshot, "lateRule");
        if (!lateRule.isEmpty()) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spLateRule.getAdapter();
            int position = adapter.getPosition(lateRule);
            if (position >= 0) {
                spLateRule.setSelection(position);
            }
        }

        Boolean deductionEnabled = snapshot.child("deductionEnabled").getValue(Boolean.class);
        if (deductionEnabled != null && deductionEnabled) {
            switchDeduction.setChecked(true);
            etPfPercent.setText(getStringValue(snapshot, "pfPercent"));
            etEsiPercent.setText(getStringValue(snapshot, "esiPercent"));
            etOtherDeduction.setText(getStringValue(snapshot, "otherDeduction"));
            etDeductionNote.setText(getStringValue(snapshot, "deductionNote"));
        }

        calculatePerDaySalary();
    }

    private String getStringValue(DataSnapshot snapshot, String key) {
        String value = snapshot.child(key).getValue(String.class);
        return value != null ? value : "";
    }

    private void setDefaultValues() {
        Calendar calendar = Calendar.getInstance();
        String currentMonth = String.format(Locale.getDefault(),
                "%02d-%d",
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR));
        etEffectiveFrom.setText(currentMonth);
        etWorkingDays.setText("26");
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String salaryStr = etMonthlySalary.getText().toString().trim();
        if (salaryStr.isEmpty()) {
            etMonthlySalary.setError("Monthly salary is required");
            isValid = false;
        } else {
            try {
                double salary = Double.parseDouble(salaryStr);
                if (salary <= 0) {
                    etMonthlySalary.setError("Salary must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etMonthlySalary.setError("Invalid salary amount");
                isValid = false;
            }
        }

        String workingDaysStr = etWorkingDays.getText().toString().trim();
        if (workingDaysStr.isEmpty()) {
            etWorkingDays.setError("Working days is required");
            isValid = false;
        } else {
            try {
                int days = Integer.parseInt(workingDaysStr);
                if (days <= 0 || days > 31) {
                    etWorkingDays.setError("Working days must be between 1 and 31");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etWorkingDays.setError("Invalid working days");
                isValid = false;
            }
        }

        if (etEffectiveFrom.getText().toString().trim().isEmpty()) {
            etEffectiveFrom.setError("Effective date is required");
            isValid = false;
        }

        if (switchDeduction.isChecked()) {
            String pfStr = etPfPercent.getText().toString().trim();
            if (!pfStr.isEmpty()) {
                try {
                    double pf = Double.parseDouble(pfStr);
                    if (pf < 0 || pf > 100) {
                        etPfPercent.setError("PF must be between 0 and 100");
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    etPfPercent.setError("Invalid percentage");
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    private void saveSalaryConfig() {
        if (!validateInputs()) {
            Toast.makeText(this, "Please fix the errors", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        Map<String, Object> data = new HashMap<>();
        data.put("monthlySalary", etMonthlySalary.getText().toString().trim());
        data.put("workingDays", etWorkingDays.getText().toString().trim());
        data.put("perDaySalary", etPerDaySalary.getText().toString().trim());
        data.put("paidLeaves", etPaidLeaves.getText().toString().trim());
        data.put("lateRule", spLateRule.getSelectedItem().toString());
        data.put("effectiveFrom", etEffectiveFrom.getText().toString().trim());
        data.put("bankName", etBankName.getText().toString().trim());
        data.put("accountHolder", etAccountHolder.getText().toString().trim());
        data.put("accountNumber", etAccountNumber.getText().toString().trim());
        data.put("ifscCode", etIfscCode.getText().toString().trim());
        data.put("branchName", etBranchName.getText().toString().trim());

        boolean deductionEnabled = switchDeduction.isChecked();
        data.put("deductionEnabled", deductionEnabled);

        if (deductionEnabled) {
            data.put("pfPercent", etPfPercent.getText().toString().trim());
            data.put("esiPercent", etEsiPercent.getText().toString().trim());
            data.put("otherDeduction", etOtherDeduction.getText().toString().trim());
            data.put("deductionNote", etDeductionNote.getText().toString().trim());
        } else {
            data.put("pfPercent", "");
            data.put("esiPercent", "");
            data.put("otherDeduction", "");
            data.put("deductionNote", "");
        }

        data.put("updatedAt", System.currentTimeMillis());
        data.put("updatedBy", companyKey);

        salaryRef.updateChildren(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            isEditMode ? "Salary updated successfully" : "Salary saved successfully",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText(isEditMode ? "Update Salary Configuration" : "Save Salary Configuration");
                    Toast.makeText(this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Salary Configuration")
                .setMessage("Are you sure you want to delete this salary configuration?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSalaryConfig())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSalaryConfig() {
        salaryRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Salary configuration deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}