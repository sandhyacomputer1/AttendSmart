package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.EmployeeWorkAdapter; // ← Existing Adapter use कर
import com.sandhyyasofttech.attendsmart.Activities.EmployeeTodayWorkActivity;
import com.sandhyyasofttech.attendsmart.Models.WorkSummary;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeAllWorksActivity extends AppCompatActivity {
    private RecyclerView rvAllWorks;
    private MaterialToolbar toolbar;
    private EmployeeWorkAdapter adapter; // ← Existing Adapter
    private String companyKey, employeeMobile;
    private List<WorkSummary> allWorks = new ArrayList<>();
    private int currentPage = 0;
    private MaterialButton btnPrev, btnNext;
    private PrefManager pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_all_works);

        initViews();
        loadSession();
        loadAllWorks();
    }

    private void initViews() {
        rvAllWorks = findViewById(R.id.rvAllWorks);
        toolbar = findViewById(R.id.toolbar);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        pref = new PrefManager(this);

        // Toolbar setup
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle("All My Works");

        // RecyclerView setup
        rvAllWorks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeWorkAdapter(this);
        rvAllWorks.setAdapter(adapter);

        // Navigation buttons
        btnPrev.setOnClickListener(v -> prevPage());
        btnNext.setOnClickListener(v -> nextPage());
    }

    private void loadSession() {
        companyKey = pref.getCompanyKey();
        employeeMobile = pref.getEmployeeMobile();

        if (TextUtils.isEmpty(companyKey) || TextUtils.isEmpty(employeeMobile)) {
            Toast.makeText(this, "⚠️ Please login again", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    private void loadAllWorks() {
        DatabaseReference worksRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork");

        worksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allWorks.clear();

                // Last 30 days only
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -30);
                Date thirtyDaysAgo = cal.getTime();

                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey();
                    try {
                        Date workDate = sdf.parse(date);
                        if (workDate != null && workDate.after(thirtyDaysAgo)) {
                            for (DataSnapshot mobileSnapshot : dateSnapshot.getChildren()) {
                                if (mobileSnapshot.getKey().equals(employeeMobile)) {
                                    WorkSummary work = mobileSnapshot.getValue(WorkSummary.class);
                                    if (work != null) {
                                        work.setWorkDate(date); // Add date to model
                                        allWorks.add(work);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {}
                }

                Collections.reverse(allWorks); // Recent first
                adapter.updateWorks(allWorks); // Show all works
                updateNavigation();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmployeeAllWorksActivity.this, "Failed to load works", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNavigation() {
        int itemsPerPage = 5;
        int totalPages = (int) Math.ceil(allWorks.size() * 1.0 / itemsPerPage);

        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < totalPages - 1);

        toolbar.setSubtitle("Page " + (currentPage + 1) + "/" + totalPages + " (" + allWorks.size() + ")");
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            showCurrentPage();
        }
    }

    private void nextPage() {
        int itemsPerPage = 5;
        int totalPages = (int) Math.ceil(allWorks.size() * 1.0 / itemsPerPage);
        if (currentPage < totalPages - 1) {
            currentPage++;
            showCurrentPage();
        }
    }

    private void showCurrentPage() {
        int itemsPerPage = 5;
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allWorks.size());

        List<WorkSummary> pageWorks = allWorks.subList(start, end);
        adapter.updateWorks(new ArrayList<>(pageWorks));
        updateNavigation();
        rvAllWorks.scrollToPosition(0);
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
