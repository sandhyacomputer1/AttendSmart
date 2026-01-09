package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeWorkAdapter;
import com.sandhyyasofttech.attendsmart.Models.WorkSummary;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmployeeMyWorksActivity extends AppCompatActivity {

    private RecyclerView rvMyWorks;
    private MaterialToolbar toolbar;
    private EmployeeWorkAdapter adapter;
    private String companyKey, employeeMobile, todayDate;
    private PrefManager pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_my_works);

        initViews();
        loadSession();
        loadTodayWork();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvMyWorks = findViewById(R.id.rvMyWorks);
        pref = new PrefManager(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle("My Today's Work");

        rvMyWorks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeWorkAdapter(this);
        rvMyWorks.setAdapter(adapter);
        findViewById(R.id.btnAddWork).setOnClickListener(v -> {
            startActivity(new Intent(this, EmployeeTodayWorkActivity.class));
        });
    }

    private void loadSession() {
        companyKey = pref.getCompanyKey();
        employeeMobile = pref.getEmployeeMobile();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (TextUtils.isEmpty(companyKey) || TextUtils.isEmpty(employeeMobile)) {
            finish();
            return;
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void loadTodayWork() {
        DatabaseReference workRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork")
                .child(todayDate)
                .child(employeeMobile);

        workRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    WorkSummary work = snapshot.getValue(WorkSummary.class);
                    if (work != null) {
                        adapter.updateWork(work);
                        rvMyWorks.setVisibility(View.VISIBLE);
                        findViewById(R.id.emptyState).setVisibility(View.GONE);
                    }
                } else {
                    rvMyWorks.setVisibility(View.GONE);
                    findViewById(R.id.emptyState).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

}
