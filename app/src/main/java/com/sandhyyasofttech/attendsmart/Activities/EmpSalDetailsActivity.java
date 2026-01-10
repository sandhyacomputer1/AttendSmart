package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmpSalDetailsActivity extends AppCompatActivity {

    private TextView tvMonth, tvNetSalary, tvGrossSalary, tvPerDaySalary, tvWorkingDays;
    private TextView tvPresentDays, tvAbsentDays, tvPaidLeaves, tvUnpaidLeaves;
    private TextView tvTotalDeduction, tvESI, tvPF, tvOtherDeduction, tvGeneratedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emp_sal_details);

        setupToolbar();

        // 🔥 GET DATA
        PrefManager pref = new PrefManager(this);
        String month = getIntent().getStringExtra("month");
        String mobile = pref.getEmployeeMobile();
        String companyRaw = pref.getCompanyKey();

        // 🔥 FIX COMMA PROBLEM
        String companyKey = companyRaw.replace(",", "");

        Log.d("SALARY", "Company: " + companyKey);
        Log.d("SALARY", "Mobile: " + mobile);
        Log.d("SALARY", "Month: " + month);

        tvMonth = findViewById(R.id.tvMonth);
        tvMonth.setText(formatMonth(month));

        bindViews();
        loadSalaryData(companyKey, month, mobile);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Salary Details");
        }
    }

    private void bindViews() {
        tvNetSalary = findViewById(R.id.tvNetSalary);
        tvGrossSalary = findViewById(R.id.tvGrossSalary);
        tvPerDaySalary = findViewById(R.id.tvPerDaySalary);
        tvWorkingDays = findViewById(R.id.tvWorkingDays);
        tvPresentDays = findViewById(R.id.tvPresentDays);
        tvAbsentDays = findViewById(R.id.tvAbsentDays);
        tvPaidLeaves = findViewById(R.id.tvPaidLeaves);
        tvUnpaidLeaves = findViewById(R.id.tvUnpaidLeaves);
        tvTotalDeduction = findViewById(R.id.tvTotalDeduction);
        tvESI = findViewById(R.id.tvESI);
        tvPF = findViewById(R.id.tvPF);
        tvOtherDeduction = findViewById(R.id.tvOtherDeduction);
        tvGeneratedAt = findViewById(R.id.tvGeneratedAt);
    }

    private void loadSalaryData(String companyKey, String month, String employeeMobile) {
        // 🔥 EXACT PATH FROM YOUR FIREBASE
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)  // sagarpatil26283233@gmail.com
                .child("salary")
                .child(month)       // 01-2026
                .child(employeeMobile); // 8605042155

        Log.d("SALARY", "🔍 Path: Companies/" + companyKey + "/salary/" + month + "/" + employeeMobile);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("SALARY", "📊 Data exists: " + snapshot.exists());

                if (!snapshot.exists()) {
                    Toast.makeText(EmpSalDetailsActivity.this, "No salary data found", Toast.LENGTH_LONG).show();
                    return;
                }

                // 🔥 YOUR EXACT DATA STRUCTURE
                DataSnapshot att = snapshot.child("attendanceSummary");
                DataSnapshot calc = snapshot.child("calculationResult");

                // 🔥 ATTENDANCE - YOUR EXACT FIELDS
                tvPresentDays.setText(String.valueOf(getIntSafe(att, "presentDays")));        // 1
                tvAbsentDays.setText(String.valueOf(getIntSafe(att, "absentDays")));          // 0
                tvPaidLeaves.setText(String.valueOf(getIntSafe(att, "paidLeavesUsed")));      // 0
                tvUnpaidLeaves.setText(String.valueOf(getIntSafe(att, "unpaidLeaves")));      // 2

                // 🔥 WORKING DAYS - FROM calculationResult
                tvWorkingDays.setText(String.valueOf(getIntSafe(calc, "payableDays")));       // 1

                // 🔥 SALARY AMOUNTS - YOUR EXACT FIELDS
                tvGrossSalary.setText("₹" + formatAmount(getDoubleSafe(calc, "grossSalary")));     // ₹400
                tvNetSalary.setText("₹" + formatAmount(getDoubleSafe(calc, "netSalary")));         // ₹400
                tvPerDaySalary.setText("₹" + formatAmount(getDoubleSafe(calc, "perDaySalary")) + "/day"); // ₹400/day
                tvTotalDeduction.setText("₹" + formatAmount(getDoubleSafe(calc, "totalDeduction"))); // ₹0

                // 🔥 DEDUCTIONS - YOUR EXACT FIELDS
                tvESI.setText("₹" + formatAmount(getDoubleSafe(calc, "esiAmount")));         // ₹0
                tvPF.setText("₹" + formatAmount(getDoubleSafe(calc, "pfAmount")));           // ₹0
                tvOtherDeduction.setText("₹" + formatAmount(getDoubleSafe(calc, "otherDeduction"))); // ₹0

                // 🔥 DATE
                Long timestamp = snapshot.child("generatedAt").getValue(Long.class);
                if (timestamp != null) {
                    tvGeneratedAt.setText("Generated: " + formatDate(timestamp));
                }

                Log.d("SALARY", "✅ ALL DATA LOADED SUCCESSFULLY!");
                Toast.makeText(EmpSalDetailsActivity.this, "Salary loaded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SALARY", "❌ Error: " + error.getMessage());
                Toast.makeText(EmpSalDetailsActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getIntSafe(DataSnapshot snapshot, String field) {
        try {
            Long value = snapshot.child(field).getValue(Long.class);
            return value != null ? value.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double getDoubleSafe(DataSnapshot snapshot, String field) {
        try {
            Number value = snapshot.child(field).getValue(Number.class);
            return value != null ? value.doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String formatMonth(String monthKey) {
        try {
            String[] parts = monthKey.split("-");
            if (parts.length == 2) {
                int m = Integer.parseInt(parts[0]);
                String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
                return months[m-1] + " " + parts[1];
            }
        } catch (Exception e) {}
        return monthKey;
    }

    private String formatAmount(double amount) {
        return new DecimalFormat("#,##0").format(amount);
    }

    private String formatDate(long timestamp) {
        try {
            return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(timestamp));
        } catch (Exception e) {
            return "Recent";
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
}
