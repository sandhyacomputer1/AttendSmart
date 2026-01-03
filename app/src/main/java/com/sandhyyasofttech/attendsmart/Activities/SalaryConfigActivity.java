package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SalaryConfigActivity extends AppCompatActivity {

    // UI
    private MaterialToolbar toolbar;
    private EditText etMonthlySalary, etWorkingDays, etPerDaySalary;
    private EditText etPaidLeaves, etEffectiveFrom;
    private Spinner spLateRule;
    private Switch switchDeduction;
    private EditText etPfPercent, etEsiPercent, etOtherDeduction, etDeductionNote;
    private Button btnSave;

    // Firebase
    private DatabaseReference salaryRef;
    private String companyKey, employeeMobile;

    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_config);

        initViews();
        setupToolbar();
        setupLateRuleSpinner();
        setupDeductionToggle();
        setupEffectiveMonthPicker();
        initFirebase();
        fetchSalaryConfig();

        btnSave.setOnClickListener(v -> saveSalaryConfig());
    }

    // ---------------- INIT ----------------

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

        btnSave = findViewById(R.id.btnSaveSalary);

        etPerDaySalary.setEnabled(false); // auto later
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setTitle("Salary Settings");
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupLateRuleSpinner() {
        String[] rules = {
                "No deduction",
                "3 Late = 0.5 Day",
                "5 Late = 1 Day"
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
        switchDeduction.setOnCheckedChangeListener((b, checked) -> toggleDeductionFields(checked));
    }

    private void toggleDeductionFields(boolean show) {
        int v = show ? View.VISIBLE : View.GONE;
        etPfPercent.setVisibility(v);
        etEsiPercent.setVisibility(v);
        etOtherDeduction.setVisibility(v);
        etDeductionNote.setVisibility(v);
    }

    private void setupEffectiveMonthPicker() {
        etEffectiveFrom.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        String value = String.format(
                                Locale.getDefault(),
                                "%02d-%d",
                                month + 1,
                                year
                        );
                        etEffectiveFrom.setText(value);
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private void initFirebase() {
        employeeMobile = getIntent().getStringExtra("employeeMobile");

        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();
        companyKey = email.replace(".", ",");

        salaryRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("salaryConfig");
    }

    // ---------------- FETCH ----------------

    private void fetchSalaryConfig() {
        salaryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isEditMode = true;
                    btnSave.setText("Update Salary");

                    etMonthlySalary.setText(snapshot.child("monthlySalary").getValue(String.class));
                    etWorkingDays.setText(snapshot.child("workingDays").getValue(String.class));
                    etPaidLeaves.setText(snapshot.child("paidLeaves").getValue(String.class));
                    etEffectiveFrom.setText(snapshot.child("effectiveFrom").getValue(String.class));

                    String lateRule = snapshot.child("lateRule").getValue(String.class);
                    if (lateRule != null) {
                        int pos = ((ArrayAdapter) spLateRule.getAdapter()).getPosition(lateRule);
                        spLateRule.setSelection(pos);
                    }

                    Boolean deductionEnabled = snapshot.child("deductionEnabled").getValue(Boolean.class);
                    if (deductionEnabled != null && deductionEnabled) {
                        switchDeduction.setChecked(true);
                        etPfPercent.setText(snapshot.child("pfPercent").getValue(String.class));
                        etEsiPercent.setText(snapshot.child("esiPercent").getValue(String.class));
                        etOtherDeduction.setText(snapshot.child("otherDeduction").getValue(String.class));
                        etDeductionNote.setText(snapshot.child("deductionNote").getValue(String.class));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SalaryConfigActivity.this,
                        "Failed to load salary data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- SAVE / UPDATE ----------------

    private void saveSalaryConfig() {

        if (etMonthlySalary.getText().toString().trim().isEmpty()) {
            etMonthlySalary.setError("Required");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("monthlySalary", etMonthlySalary.getText().toString().trim());
        data.put("workingDays", etWorkingDays.getText().toString().trim());
        data.put("paidLeaves", etPaidLeaves.getText().toString().trim());
        data.put("lateRule", spLateRule.getSelectedItem().toString());
        data.put("effectiveFrom", etEffectiveFrom.getText().toString().trim());

        boolean deductionEnabled = switchDeduction.isChecked();
        data.put("deductionEnabled", deductionEnabled);

        if (deductionEnabled) {
            data.put("pfPercent", etPfPercent.getText().toString().trim());
            data.put("esiPercent", etEsiPercent.getText().toString().trim());
            data.put("otherDeduction", etOtherDeduction.getText().toString().trim());
            data.put("deductionNote", etDeductionNote.getText().toString().trim());
        }

        data.put("updatedAt", System.currentTimeMillis());

        salaryRef.updateChildren(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            isEditMode ? "Salary updated successfully" : "Salary saved successfully",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
