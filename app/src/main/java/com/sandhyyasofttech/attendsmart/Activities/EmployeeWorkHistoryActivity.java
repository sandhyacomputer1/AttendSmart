package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.WorkHistoryAdapter;
import com.sandhyyasofttech.attendsmart.Models.WorkHistoryModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeWorkHistoryActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView rvHistory;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvEmployeeName, tvStats;

    private String companyKey, employeeMobile, employeeName;
    private List<WorkHistoryModel> workHistoryList;
    private WorkHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_work_history);

        getIntentData();
        initViews();
        loadWorkHistory();
    }

    private void getIntentData() {
        employeeMobile = getIntent().getStringExtra("employeeMobile");
        employeeName = getIntent().getStringExtra("employeeName");

        if (TextUtils.isEmpty(employeeMobile)) {
            Toast.makeText(this, "Invalid employee data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Work History");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvStats = findViewById(R.id.tvStats);
        rvHistory = findViewById(R.id.rvHistory);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        tvEmployeeName.setText(" " + employeeName + "'s Work History");

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        workHistoryList = new ArrayList<>();
        adapter = new WorkHistoryAdapter(workHistoryList);
        rvHistory.setAdapter(adapter);
    }

    private void loadWorkHistory() {
        showLoading(true);

        DatabaseReference workRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork");

        workRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                workHistoryList.clear();

                if (!snapshot.exists()) {
                    showLoading(false);
                    showEmptyState();
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                // Iterate through all dates
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey();

                    try {
                        Date workDate = sdf.parse(date);
                        if (workDate != null) {
                            // Check if this employee has work for this date
                            if (dateSnapshot.hasChild(employeeMobile)) {
                                DataSnapshot empWorkSnap = dateSnapshot.child(employeeMobile);

                                WorkHistoryModel work = new WorkHistoryModel();
                                work.setWorkDate(date);
                                work.setCompletedWork(empWorkSnap.child("completedWork").getValue(String.class));
                                work.setOngoingWork(empWorkSnap.child("ongoingWork").getValue(String.class));
                                work.setTomorrowWork(empWorkSnap.child("tomorrowWork").getValue(String.class));

                                Long timestamp = empWorkSnap.child("submittedAt").getValue(Long.class);
                                work.setSubmittedAt(timestamp != null ? timestamp : 0L);

                                workHistoryList.add(work);
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                // Sort by date (most recent first)
                Collections.sort(workHistoryList, (w1, w2) -> w2.getWorkDate().compareTo(w1.getWorkDate()));

                showLoading(false);

                if (workHistoryList.isEmpty()) {
                    showEmptyState();
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvHistory.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                    updateStats();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(EmployeeWorkHistoryActivity.this,
                        "❌ Failed to load: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        int totalReports = workHistoryList.size();
        
        // Calculate date range
        if (!workHistoryList.isEmpty()) {
            String latestDate = workHistoryList.get(0).getWorkDate();
            String oldestDate = workHistoryList.get(workHistoryList.size() - 1).getWorkDate();
            
            String stats = String.format(Locale.getDefault(),
                    "Total Reports: %d | From: %s to %s",
                    totalReports, 
                    formatDate(oldestDate), 
                    formatDate(latestDate));
            
            tvStats.setText(stats);
            tvStats.setVisibility(View.VISIBLE);
        }
    }

    private String formatDate(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date d = inputFormat.parse(date);
            return outputFormat.format(d);
        } catch (Exception e) {
            return date;
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvHistory.setVisibility(show ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("No work history found for this employee");
        rvHistory.setVisibility(View.GONE);
        tvStats.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}