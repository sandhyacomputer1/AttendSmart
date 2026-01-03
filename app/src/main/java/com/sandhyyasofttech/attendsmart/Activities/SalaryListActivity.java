package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.SalaryListAdapter;
import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class SalaryListActivity extends AppCompatActivity {

    private EditText etMonth;
    private RecyclerView rvSalary;
    private TextView tvEmpty;

    private SalaryListAdapter adapter;
    private final ArrayList<SalarySnapshot> salaryList = new ArrayList<>();

    private DatabaseReference companyRef;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_list);

        etMonth = findViewById(R.id.etMonthFilter);
        rvSalary = findViewById(R.id.rvSalaryList);
        tvEmpty = findViewById(R.id.tvEmpty);

        PrefManager pref = new PrefManager(this);
        companyKey = pref.getUserEmail().replace(".", ",");

        companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("salary");

        rvSalary.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SalaryListAdapter(salaryList, this::openSalaryDetail);
        rvSalary.setAdapter(adapter);

        setupMonthPicker();
    }

    // ================= MONTH PICKER =================
    private void setupMonthPicker() {
        etMonth.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        String selected =
                                String.format(Locale.getDefault(),
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

    // ================= LOAD SALARY =================
    private void loadSalaryForMonth(String month) {
        salaryList.clear();
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(TextView.GONE);

        companyRef.child(month)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            tvEmpty.setVisibility(TextView.VISIBLE);
                            return;
                        }

                        for (DataSnapshot s : snapshot.getChildren()) {
                            SalarySnapshot snap =
                                    s.getValue(SalarySnapshot.class);
                            if (snap != null) {
                                salaryList.add(snap);
                            }
                        }

                        if (salaryList.isEmpty()) {
                            tvEmpty.setVisibility(TextView.VISIBLE);
                        } else {
                            tvEmpty.setVisibility(TextView.GONE);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvEmpty.setVisibility(TextView.VISIBLE);
                    }
                });
    }

    // ================= OPEN DETAIL =================
    private void openSalaryDetail(SalarySnapshot s) {
        Intent i = new Intent(this, SalaryDetailActivity.class);
        i.putExtra("month", s.month);
        i.putExtra("employeeMobile", s.employeeMobile);
        startActivity(i);
    }
}
