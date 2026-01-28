package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.MyLeavesAdapter;
import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class MyLeavesActivity extends AppCompatActivity {

    private RecyclerView rv;
    private MaterialCardView cardEmpty, layoutFilters;
    private ChipGroup chipGroupFilter, chipGroupSort;
    private LinearLayout layoutStats;
    private TextView tvPendingCount, tvApprovedCount, tvRejectedCount, tvResetFilters;

    private ArrayList<LeaveModel> fullList = new ArrayList<>();
    private ArrayList<LeaveModel> filteredList = new ArrayList<>();
    private MyLeavesAdapter adapter;

    private String currentStatusFilter = "ALL";
    private String currentSortOrder = "NEWEST";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_my_leaves);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        initViews();
        setupToolbar();
        setupFilters();
        setupRecyclerView();
        loadLeaves();
    }

    private void initViews() {
        rv = findViewById(R.id.rvLeaves);
        cardEmpty = findViewById(R.id.cardEmpty);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        chipGroupSort = findViewById(R.id.chipGroupSort);
        layoutFilters = findViewById(R.id.layoutFilters);
        layoutStats = findViewById(R.id.layoutStats);

        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);
        tvRejectedCount = findViewById(R.id.tvRejectedCount);
        tvResetFilters = findViewById(R.id.tvResetFilters);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFilters() {
        // Status Filter
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                Chip chip = findViewById(checkedId);
                if (chip != null) {
                    currentStatusFilter = chip.getText().toString().toUpperCase();
                    applyFilters();

                    // Show reset button if not "All"
                    tvResetFilters.setVisibility(
                            currentStatusFilter.equals("ALL") ? View.GONE : View.VISIBLE
                    );
                }
            }
        });

        // Sort Order
        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                Chip chip = findViewById(checkedId);
                if (chip != null) {
                    currentSortOrder = chip.getTag().toString();
                    applyFilters();
                }
            }
        });

        // Reset Filters
        tvResetFilters.setOnClickListener(v -> {
            ((Chip) findViewById(R.id.chipAll)).setChecked(true);
            ((Chip) findViewById(R.id.chipNewest)).setChecked(true);
        });
    }

    private void setupRecyclerView() {
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyLeavesAdapter(filteredList);
        rv.setAdapter(adapter);

        // Add item decoration for spacing
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = 8;
            }
        });
    }

    private void loadLeaves() {
        PrefManager pref = new PrefManager(this);
        String companyKey = pref.getCompanyKey();
        String mobile = pref.getEmployeeMobile();

        // Validate company key and mobile
        if (companyKey == null || companyKey.isEmpty() || mobile == null || mobile.isEmpty()) {
            showEmptyState();
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("leaves");

        Query q = ref.orderByChild("employeeMobile").equalTo(mobile);

        q.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                fullList.clear();

                for (DataSnapshot s : snap.getChildren()) {
                    LeaveModel m = s.getValue(LeaveModel.class);
                    if (m != null) {
                        m.leaveId = s.getKey();
                        fullList.add(m);
                    }
                }

                updateStatistics();
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                showEmptyState();
                Toast.makeText(MyLeavesActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatistics() {
        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;

        for (LeaveModel leave : fullList) {
            if (leave.status != null) {
                switch (leave.status.toUpperCase()) {
                    case "PENDING":
                        pendingCount++;
                        break;
                    case "APPROVED":
                        approvedCount++;
                        break;
                    case "REJECTED":
                        rejectedCount++;
                        break;
                }
            }
        }

        tvPendingCount.setText(String.valueOf(pendingCount));
        tvApprovedCount.setText(String.valueOf(approvedCount));
        tvRejectedCount.setText(String.valueOf(rejectedCount));

        // Show stats only if there are leaves
        layoutStats.setVisibility(fullList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void applyFilters() {
        filteredList.clear();

        // Apply status filter
        for (LeaveModel leave : fullList) {
            if (currentStatusFilter.equals("ALL") ||
                    (leave.status != null && leave.status.equalsIgnoreCase(currentStatusFilter))) {
                filteredList.add(leave);
            }
        }

        // Apply sorting
        Collections.sort(filteredList, getComparator());

        // Update UI
        if (filteredList.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }

        adapter.notifyDataSetChanged();

        // Show filters only if there are leaves
        layoutFilters.setVisibility(fullList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private Comparator<LeaveModel> getComparator() {
        switch (currentSortOrder) {
            case "NEWEST":
                return (l1, l2) -> Long.compare(l2.appliedAt, l1.appliedAt);
            case "OLDEST":
                return (l1, l2) -> Long.compare(l1.appliedAt, l2.appliedAt);
            case "START_DATE":
                return (l1, l2) -> compareDates(l1.fromDate, l2.fromDate);
            default:
                return (l1, l2) -> Long.compare(l2.appliedAt, l1.appliedAt);
        }
    }

    private int compareDates(String date1, String date2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d1 = date1 != null ? sdf.parse(date1) : new Date(0);
            Date d2 = date2 != null ? sdf.parse(date2) : new Date(0);

            if (d1 != null && d2 != null) {
                return d1.compareTo(d2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void showEmptyState() {
        cardEmpty.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);
        layoutFilters.setVisibility(View.GONE);
        layoutStats.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        cardEmpty.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        layoutFilters.setVisibility(View.VISIBLE);
        layoutStats.setVisibility(View.VISIBLE);
    }
}