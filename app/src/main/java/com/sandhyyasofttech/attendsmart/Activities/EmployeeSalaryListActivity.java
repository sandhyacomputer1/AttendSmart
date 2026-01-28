package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class EmployeeSalaryListActivity extends AppCompatActivity {

    private ListView listView;
    private TextView tvEmptyState;
    private ArrayList<String> monthList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_salary_list);  // ✅ LIST LAYOUT

        initToolbar();
        initViews();
        loadSalaryMonths();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Salary");

            // Set title text color
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

            // For back arrow color - create a white drawable
            Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back);
            if (upArrow != null) {
                upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
        }
    }

    private void initViews() {
        PrefManager pref = new PrefManager(this);
        if (pref.getCompanyKey() == null || pref.getEmployeeMobile() == null) {
            Toast.makeText(this, "⚠️ Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        listView = findViewById(R.id.listSalaryMonths);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, monthList);
        listView.setAdapter(adapter);

        // ✅ FIXED CLICK LISTENER - Only pass MONTH
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String month = monthList.get(position);
            Intent intent = new Intent(this, EmpSalDetailsActivity.class);
            intent.putExtra("month", month);  // Only month needed
            startActivity(intent);
        });
    }

    private void loadSalaryMonths() {
        PrefManager pref = new PrefManager(this);
        String companyKey = pref.getCompanyKey();

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("salary");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                monthList.clear();

                for (DataSnapshot monthSnap : snapshot.getChildren()) {
                    String employeeMobile = pref.getEmployeeMobile();
                    if (monthSnap.hasChild(employeeMobile)) {
                        monthList.add(monthSnap.getKey());  // "01-2026"
                    }
                }

                // Sort newest first
                Collections.sort(monthList, (a, b) -> b.compareTo(a));

                adapter.notifyDataSetChanged();

                if (monthList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmployeeSalaryListActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
            }
        });
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
