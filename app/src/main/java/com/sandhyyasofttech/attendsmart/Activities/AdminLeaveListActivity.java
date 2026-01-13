package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.AdminLeaveAdapter;
import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminLeaveListActivity extends AppCompatActivity {

    private RecyclerView rv;
    private LinearLayout tvEmpty;  // ✅ FIXED: Changed from TextView to LinearLayout
    private TextView tvStats;
    private ProgressBar progressBar;
    private TabLayout tabLayout;

    private final List<LeaveModel> allLeaves = new ArrayList<>();
    private final List<LeaveModel> displayList = new ArrayList<>();
    private DatabaseReference leavesRef;
    private AdminLeaveAdapter adapter;

    private String currentFilter = "ALL"; // ALL, PENDING, APPROVED, REJECTED

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_admin_leave_list);

        // 🔹 Toolbar setup
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Leaves");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white, getTheme()));
        }

        // 🔹 Make back arrow WHITE
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.getNavigationIcon().setTint(
                getResources().getColor(android.R.color.white)
        );
        toolbar.setNavigationOnClickListener(v -> finish());

        // 🔹 Initialize Views
        rv = findViewById(R.id.rvLeaves);
        tvEmpty = findViewById(R.id.tvEmpty);  // ✅ Now correctly typed as LinearLayout
        tvStats = findViewById(R.id.tvStats);
        progressBar = findViewById(R.id.progressBar);
        tabLayout = findViewById(R.id.tabLayout);

        rv.setLayoutManager(new LinearLayoutManager(this));

        // 🔹 Setup Tabs
        setupTabs();

        // 🔹 Firebase Reference
        String companyKey = new PrefManager(this).getCompanyKey();
        leavesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("leaves");

        // 🔹 Setup Adapter
        adapter = new AdminLeaveAdapter(this, displayList, leavesRef);
        rv.setAdapter(adapter);

        // 🔹 Load Data
        loadLeaves();
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Approved"));
        tabLayout.addTab(tabLayout.newTab().setText("Rejected"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "ALL";
                        break;
                    case 1:
                        currentFilter = "PENDING";
                        break;
                    case 2:
                        currentFilter = "APPROVED";
                        break;
                    case 3:
                        currentFilter = "REJECTED";
                        break;
                }
                filterLeaves();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadLeaves() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        leavesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                allLeaves.clear();

                for (DataSnapshot d : s.getChildren()) {
                    LeaveModel m = d.getValue(LeaveModel.class);
                    if (m != null) {
                        m.leaveId = d.getKey();
                        allLeaves.add(m);
                    }
                }

                // 🔹 Sort: Pending → Approved → Rejected
                Collections.sort(allLeaves, (a, b) -> {
                    if (a.status == null && b.status == null) return 0;
                    if (a.status == null) return 1;
                    if (b.status == null) return -1;

                    if (a.status.equals(b.status)) {
                        return 0;  // ✅ FIXED: Added missing return statement
                    }

                    if ("PENDING".equals(a.status)) return -1;
                    if ("PENDING".equals(b.status)) return 1;
                    if ("APPROVED".equals(a.status)) return -1;
                    return 1;
                });

                updateStats();
                filterLeaves();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void filterLeaves() {
        displayList.clear();

        if ("ALL".equals(currentFilter)) {
            displayList.addAll(allLeaves);
        } else {
            for (LeaveModel leave : allLeaves) {
                if (currentFilter.equals(leave.status)) {
                    displayList.add(leave);
                }
            }
        }

        adapter.notifyDataSetChanged();

        // Show/Hide Empty State
        if (displayList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
        }
    }

    private void updateStats() {
        int total = allLeaves.size();
        int pending = 0;
        int approved = 0;
        int rejected = 0;

        for (LeaveModel leave : allLeaves) {
            if (leave.status == null) continue;

            switch (leave.status) {
                case "PENDING":
                    pending++;
                    break;
                case "APPROVED":
                    approved++;
                    break;
                case "REJECTED":
                    rejected++;
                    break;
            }
        }

        String stats = "Total: " + total +
                " | Pending: " + pending +
                " | Approved: " + approved +
                " | Rejected: " + rejected;

        tvStats.setText(stats);

        // Update tab badges (optional - shows count)
        if (tabLayout.getTabCount() >= 4) {
            tabLayout.getTabAt(0).setText("All (" + total + ")");
            tabLayout.getTabAt(1).setText("Pending (" + pending + ")");
            tabLayout.getTabAt(2).setText("Approved (" + approved + ")");
            tabLayout.getTabAt(3).setText("Rejected (" + rejected + ")");
        }
    }
}
