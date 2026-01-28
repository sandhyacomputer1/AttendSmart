package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.Models.MonthlyAttendanceSummary;
import com.sandhyyasofttech.attendsmart.Models.SalaryCalculationResult;
import com.sandhyyasofttech.attendsmart.Models.SalaryConfig;
import com.sandhyyasofttech.attendsmart.Models.SalaryPreviewData;
import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.NumberFormat;
import java.util.Locale;

public class SalaryPreviewActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvMonth, tvEmployee;
    private TextView tvPresentDays, tvHalfDays, tvLeaves, tvAbsentDays;
    private TextView tvGrossSalary, tvPfAmount, tvEsiAmount, tvOtherDeduction, tvNetSalary;

    // ✅ Professional - Total Deduction add करा
    private TextView tvTotalDeduction;

    // Editable fields
    private EditText etManualGross, etManualPf, etManualEsi, etManualOther, etNotes;
    // ✅ Professional - Net Salary editable
    private EditText etManualNetSalary;

    private Button btnSave, btnCancel;
    private MaterialCardView cardAdjustments;

    private SalaryPreviewData previewData;
    private PrefManager pref;
    private String companyKey;

    // ✅ Flag to prevent circular calculations
    private boolean isCalculating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_preview);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        initViews();
        setupToolbar();

        previewData = (SalaryPreviewData) getIntent().getSerializableExtra("previewData");
        if (previewData == null) {
            Toast.makeText(this, "Data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();

        displayData();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // Toolbar setup
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Salary Preview & Edit");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tvMonth = findViewById(R.id.tvMonth);
        tvEmployee = findViewById(R.id.tvEmployee);
        tvPresentDays = findViewById(R.id.tvPresentDays);
        tvHalfDays = findViewById(R.id.tvHalfDays);
        tvLeaves = findViewById(R.id.tvLeaves);
        tvAbsentDays = findViewById(R.id.tvAbsentDays);

        // Salary display fields
        tvGrossSalary = findViewById(R.id.tvBasicSalary);
        tvPfAmount = findViewById(R.id.tvPfAmount);
        tvEsiAmount = findViewById(R.id.tvEsiAmount);
        tvOtherDeduction = findViewById(R.id.tvOtherDeductions);
        tvTotalDeduction = findViewById(R.id.tvTotalDeduction); // ✅ New
        tvNetSalary = findViewById(R.id.tvNetSalary);

        // Editable fields
        etManualGross = findViewById(R.id.etManualBasic);
        etManualPf = findViewById(R.id.etManualPf);
        etManualEsi = findViewById(R.id.etManualEsi);
        etManualOther = findViewById(R.id.etManualOther);
        etManualNetSalary = findViewById(R.id.etManualNetSalary); // ✅ New
        etNotes = findViewById(R.id.etNotes);

        cardAdjustments = findViewById(R.id.cardAdjustments);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Edit Salary");
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void displayData() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Header
        tvMonth.setText(previewData.month);
        tvEmployee.setText(previewData.employeeMobile);

        // Attendance Summary
        MonthlyAttendanceSummary summary = previewData.attendanceSummary;
        tvPresentDays.setText(String.valueOf(summary.presentDays));
//        tvHalfDays.setText(String.valueOf(summary.halfDays));
        tvLeaves.setText(summary.paidLeavesUsed + " paid, " + summary.unpaidLeaves + " unpaid");
        tvAbsentDays.setText(String.valueOf(summary.absentDays));
        if (tvHalfDays != null) {
            tvHalfDays.setText(String.valueOf(summary.halfDays));
        } else {
            Log.e("SalaryPreview", "tvHalfDays is null!");
        }
        // Salary Calculation (Original)
        SalaryCalculationResult result = previewData.calculationResult;

        // Display values
        tvGrossSalary.setText(formatter.format(result.grossSalary));
        tvPfAmount.setText(formatter.format(result.pfAmount));
        tvEsiAmount.setText(formatter.format(result.esiAmount));
        tvOtherDeduction.setText(formatter.format(result.otherDeduction));
        tvTotalDeduction.setText(formatter.format(result.totalDeduction));
        tvNetSalary.setText(formatter.format(result.netSalary));

        // Editable fields मध्ये original values सेट करा
        etManualGross.setText(String.valueOf(result.grossSalary));
        etManualPf.setText(String.valueOf(result.pfAmount));
        etManualEsi.setText(String.valueOf(result.esiAmount));
        etManualOther.setText(String.valueOf(result.otherDeduction));
        etManualNetSalary.setText(String.valueOf(result.netSalary));
        etNotes.setText(previewData.notes != null ? previewData.notes : "");
    }

    private void setupListeners() {
        // ✅ Professional: Real-time calculation साठी TextWatchers
        TextWatcher calculationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isCalculating) {
                    calculateFromGross();
                }
            }
        };

        // ✅ सर्व input fields ला listeners add करा
        etManualGross.addTextChangedListener(calculationWatcher);
        etManualPf.addTextChangedListener(calculationWatcher);
        etManualEsi.addTextChangedListener(calculationWatcher);
        etManualOther.addTextChangedListener(calculationWatcher);

        // ✅ Net Salary चा separate listener (जर user directly net salary edit करेल)
        etManualNetSalary.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isCalculating) {
                    calculateFromNet();
                }
            }
        });

        // Save button
        btnSave.setOnClickListener(v -> saveSalaryToFirebase());

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());
    }

    // ✅ Method 1: Gross पासून calculate करा (PF/ESI/Other बदलल्यावर)
    private void calculateFromGross() {
        isCalculating = true;

        try {
            double gross = getDoubleFromEditText(etManualGross);
            double pf = getDoubleFromEditText(etManualPf);
            double esi = getDoubleFromEditText(etManualEsi);
            double other = getDoubleFromEditText(etManualOther);

            double totalDeduction = pf + esi + other;
            double net = gross - totalDeduction;

            // ✅ Update net salary field
            etManualNetSalary.setText(String.format("%.2f", net));

            // ✅ Update display
            updateDisplay(gross, pf, esi, other, totalDeduction, net);

            // ✅ Update preview data
            updatePreviewData(gross, pf, esi, other, net);

        } catch (Exception e) {
            // Invalid input
        } finally {
            isCalculating = false;
        }
    }

    // ✅ Method 2: Net पासून calculate करा (जर user directly net edit करेल)
    private void calculateFromNet() {
        isCalculating = true;

        try {
            double net = getDoubleFromEditText(etManualNetSalary);
            double pf = getDoubleFromEditText(etManualPf);
            double esi = getDoubleFromEditText(etManualEsi);
            double other = getDoubleFromEditText(etManualOther);

            double totalDeduction = pf + esi + other;
            double gross = net + totalDeduction;

            // ✅ Update gross salary field
            etManualGross.setText(String.format("%.2f", gross));

            // ✅ Update display
            updateDisplay(gross, pf, esi, other, totalDeduction, net);

            // ✅ Update preview data
            updatePreviewData(gross, pf, esi, other, net);

        } catch (Exception e) {
            // Invalid input
        } finally {
            isCalculating = false;
        }
    }

    private double getDoubleFromEditText(EditText editText) {
        try {
            String text = editText.getText().toString().trim();
            if (text.isEmpty()) return 0.0;
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void updateDisplay(double gross, double pf, double esi, double other,
                               double totalDeduction, double net) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        tvGrossSalary.setText(formatter.format(gross));
        tvPfAmount.setText(formatter.format(pf));
        tvEsiAmount.setText(formatter.format(esi));
        tvOtherDeduction.setText(formatter.format(other));
        tvTotalDeduction.setText(formatter.format(totalDeduction));
        tvNetSalary.setText(formatter.format(net));
    }

    private void updatePreviewData(double gross, double pf, double esi, double other, double net) {
        previewData.manualGrossSalary = gross;
        previewData.manualPfAmount = pf;
        previewData.manualEsiAmount = esi;
        previewData.manualOtherDeduction = other;
        previewData.manualNetSalary = net;
        previewData.notes = etNotes.getText().toString();
    }

    private void saveSalaryToFirebase() {
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // SalarySnapshot तयार करा
        SalarySnapshot snapshot = new SalarySnapshot();
        snapshot.month = previewData.month;
        snapshot.employeeMobile = previewData.employeeMobile;
        snapshot.generatedAt = System.currentTimeMillis();
        snapshot.attendanceSummary = previewData.attendanceSummary;
        snapshot.salaryConfigSnapshot = previewData.salaryConfig;

        // Manual adjustments असल्यास calculation result update करा
        SalaryCalculationResult finalResult = previewData.calculationResult;
        finalResult.grossSalary = previewData.manualGrossSalary;
        finalResult.pfAmount = previewData.manualPfAmount;
        finalResult.esiAmount = previewData.manualEsiAmount;
        finalResult.otherDeduction = previewData.manualOtherDeduction;
        finalResult.totalDeduction = finalResult.pfAmount + finalResult.esiAmount + finalResult.otherDeduction;
        finalResult.netSalary = previewData.manualNetSalary;

        snapshot.calculationResult = finalResult;
        snapshot.manualAdjustments = true;
        snapshot.adjustmentNotes = previewData.notes;

        // Firebase मध्ये save करा
        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey);

        companyRef.child("salary")
                .child(previewData.month)
                .child(previewData.employeeMobile)
                .setValue(snapshot)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Salary saved successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Salary");
                });
    }
}