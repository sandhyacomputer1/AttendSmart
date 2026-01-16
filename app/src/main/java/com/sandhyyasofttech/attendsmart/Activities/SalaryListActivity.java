package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.SalaryListAdapter;
import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SalaryListActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText etMonth;
    private RecyclerView rvSalary;
    private LinearLayout layoutEmpty;
    private TextView tvEmpty, tvTotalRecords, tvTotalPaid, tvTotalPending;
    private ChipGroup chipGroupQuickFilter;
    private Chip chipCurrentMonth, chipLastMonth, chipAllRecords;

    private SalaryListAdapter adapter;
    private final ArrayList<SalarySnapshot> salaryList = new ArrayList<>();
    private final ArrayList<SalarySnapshot> allSalaryRecords = new ArrayList<>();

    private DatabaseReference companyRef;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_list);

        initializeViews();
        setupToolbar();
        setupChipListeners();
        setupMonthPicker();

        PrefManager pref = new PrefManager(this);
        companyKey = pref.getUserEmail().replace(".", ",");

        companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("salary");

        rvSalary.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SalaryListAdapter(salaryList, this::openSalaryDetail, this, companyKey);
        rvSalary.setAdapter(adapter);
        // Load all salary data first for "All Records" chip
        loadAllSalaryData();

        // Set current month as default
        loadCurrentMonthSalary();
        etMonth.setText(getCurrentMonth());
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etMonth = findViewById(R.id.etMonthFilter);
        rvSalary = findViewById(R.id.rvSalaryList);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvTotalRecords = findViewById(R.id.tvTotalRecords);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);
        tvTotalPending = findViewById(R.id.tvTotalPending);

        chipGroupQuickFilter = findViewById(R.id.chipGroupQuickFilter);
        chipCurrentMonth = findViewById(R.id.chipCurrentMonth);
        chipLastMonth = findViewById(R.id.chipLastMonth);
        chipAllRecords = findViewById(R.id.chipAllRecords);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupChipListeners() {
        chipCurrentMonth.setOnClickListener(v -> {
            chipGroupQuickFilter.check(R.id.chipCurrentMonth);
            loadCurrentMonthSalary();
            etMonth.setText(getCurrentMonth());
        });

        chipLastMonth.setOnClickListener(v -> {
            chipGroupQuickFilter.check(R.id.chipLastMonth);
            loadLastMonthSalary();
            etMonth.setText(getLastMonth());
        });

        chipAllRecords.setOnClickListener(v -> {
            chipGroupQuickFilter.check(R.id.chipAllRecords);
            showAllSalaryRecords();
            etMonth.setText("All Records");
        });

        // Set default selection
        chipCurrentMonth.setChecked(true);
    }

    private void setupMonthPicker() {
        etMonth.setOnClickListener(v -> {
            // Clear chip selection when manually picking month
            chipGroupQuickFilter.clearCheck();

            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        String selected = String.format(Locale.getDefault(),
                                "%02d-%d", month + 1, year);
                        etMonth.setText(selected);
                        loadSalaryForMonth(selected);
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private String getCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    private String getLastMonth() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    // ================= LOAD ALL SALARY DATA =================
    private void loadAllSalaryData() {
        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allSalaryRecords.clear();

                if (!snapshot.exists()) {
                    return;
                }

                for (DataSnapshot monthSnapshot : snapshot.getChildren()) {
                    String month = monthSnapshot.getKey();

                    for (DataSnapshot empSnapshot : monthSnapshot.getChildren()) {
                        SalarySnapshot salary = empSnapshot.getValue(SalarySnapshot.class);
                        if (salary != null) {
                            salary.month = month;
                            allSalaryRecords.add(salary);
                        }
                    }
                }

                // Update summary for all records
                updateSummary(allSalaryRecords);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    // ================= LOAD CURRENT MONTH =================
    private void loadCurrentMonthSalary() {
        String currentMonth = getCurrentMonth();
        loadSalaryForMonth(currentMonth);
    }

    // ================= LOAD LAST MONTH =================
    private void loadLastMonthSalary() {
        String lastMonth = getLastMonth();
        loadSalaryForMonth(lastMonth);
    }

    private void showAllSalaryRecords() {
        salaryList.clear();
        salaryList.addAll(allSalaryRecords);

        if (salaryList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("No salary records found");
            rvSalary.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvSalary.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
        updateSummary(salaryList);
    }

    // Update the loadSalaryForMonth method:
    private void loadSalaryForMonth(String month) {
        salaryList.clear();
        layoutEmpty.setVisibility(View.GONE);
        rvSalary.setVisibility(View.VISIBLE);

        // Create new adapter with employee details fetching
        adapter = new SalaryListAdapter(salaryList, this::openSalaryDetail, this, companyKey);
        rvSalary.setAdapter(adapter);

        companyRef.child(month)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        salaryList.clear();

                        if (!snapshot.exists()) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                            rvSalary.setVisibility(View.GONE);
                            tvEmpty.setText("No salary records found for " + month);
                            updateSummary(new ArrayList<>());
                            return;
                        }

                        for (DataSnapshot s : snapshot.getChildren()) {
                            SalarySnapshot snap = s.getValue(SalarySnapshot.class);
                            if (snap != null) {
                                snap.month = month;
                                salaryList.add(snap);
                            }
                        }

                        if (salaryList.isEmpty()) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                            rvSalary.setVisibility(View.GONE);
                            tvEmpty.setText("No salary records found for " + month);
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                            rvSalary.setVisibility(View.VISIBLE);
                        }

                        adapter.notifyDataSetChanged();
                        updateSummary(salaryList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvSalary.setVisibility(View.GONE);
                        tvEmpty.setText("Error loading salary data");
                        updateSummary(new ArrayList<>());
                    }
                });
    }

    // ================= UPDATE SUMMARY =================
    private void updateSummary(List<SalarySnapshot> records) {
        if (records == null || records.isEmpty()) {
            tvTotalRecords.setText("0");
            tvTotalPaid.setText("₹0");
            tvTotalPending.setText("₹0");
            return;
        }

        int totalRecords = records.size();
        double totalNetSalary = 0;
        double totalGrossSalary = 0;
        double totalDeductions = 0;

        for (SalarySnapshot salary : records) {
            if (salary.calculationResult != null) {
                // Parse net salary
                Object netSalaryObj = salary.calculationResult.netSalary;
                if (netSalaryObj != null) {
                    totalNetSalary += parseSalaryValue(netSalaryObj);
                }

                // Parse gross salary
                Object grossSalaryObj = salary.calculationResult.grossSalary;
                if (grossSalaryObj != null) {
                    totalGrossSalary += parseSalaryValue(grossSalaryObj);
                }

                // Parse total deduction
                Object deductionObj = salary.calculationResult.totalDeduction;
                if (deductionObj != null) {
                    totalDeductions += parseSalaryValue(deductionObj);
                }
            }
        }

        // Update UI
        tvTotalRecords.setText(String.valueOf(totalRecords));
        tvTotalPaid.setText(formatCurrency(totalNetSalary));
        tvTotalPending.setText(formatCurrency(totalDeductions));
    }

    private double parseSalaryValue(Object salaryValue) {
        if (salaryValue == null) {
            return 0.0;
        }

        try {
            if (salaryValue instanceof String) {
                String strValue = (String) salaryValue;
                String cleanStr = strValue.replaceAll("[₹$,]", "").trim();
                if (cleanStr.isEmpty()) {
                    return 0.0;
                }
                return Double.parseDouble(cleanStr);
            } else if (salaryValue instanceof Number) {
                return ((Number) salaryValue).doubleValue();
            } else if (salaryValue instanceof Double) {
                return (Double) salaryValue;
            } else if (salaryValue instanceof Integer) {
                return ((Integer) salaryValue).doubleValue();
            } else if (salaryValue instanceof Long) {
                return ((Long) salaryValue).doubleValue();
            }
        } catch (Exception e) {
            // Return 0 if parsing fails
        }
        return 0.0;
    }

    private String formatCurrency(double amount) {
        if (amount == 0) {
            return "₹0";
        }

        // Format with commas for thousands
        if (amount % 1 == 0) {
            // Whole number
            return String.format(Locale.getDefault(), "₹%,.0f", amount);
        } else {
            // Decimal number
            return String.format(Locale.getDefault(), "₹%,.2f", amount);
        }
    }

    // ================= OPEN DETAIL =================
    private void openSalaryDetail(SalarySnapshot s) {
        Intent i = new Intent(this, SalaryDetailActivity.class);
        i.putExtra("month", s.month);
        i.putExtra("employeeMobile", s.employeeMobile);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}