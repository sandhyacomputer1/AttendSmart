package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;

public class EmployeeSalaryListActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<String> monthList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private String companyKey;
    private String employeeMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_salary_list);

        listView = findViewById(R.id.listSalaryMonths);

        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();          // company of employee
        employeeMobile = pref.getEmployeeMobile();     // logged-in employee

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                monthList
        );

        listView.setAdapter(adapter);

        loadSalaryMonths();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String month = monthList.get(position);

            Intent intent = new Intent(
                    this,
                    SalaryDetailActivity.class
            );
            intent.putExtra("month", month);
            intent.putExtra("employeeMobile", employeeMobile);
            startActivity(intent);
        });
    }

    // ================= LOAD MONTHS =================
    private void loadSalaryMonths() {

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("salary");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                monthList.clear();

                for (DataSnapshot monthSnap : snapshot.getChildren()) {

                    if (monthSnap.child(employeeMobile).exists()) {
                        monthList.add(monthSnap.getKey()); // 12-2025
                    }
                }

                if (monthList.isEmpty()) {
                    Toast.makeText(
                            EmployeeSalaryListActivity.this,
                            "No salary records found",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        EmployeeSalaryListActivity.this,
                        error.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}
