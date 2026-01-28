package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.WorkReportPagerAdapter;
import com.sandhyyasofttech.attendsmart.Models.WorkSummary;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeAllWorksActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MaterialToolbar toolbar;
    private WorkReportPagerAdapter pagerAdapter;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView tvEmptyMessage, tvPageIndicator;
    private MaterialButton btnDatePicker, btnPrevDate, btnNextDate;
    private FloatingActionButton fabAddWork;

    private String companyKey, employeeMobile;
    private List<WorkSummary> allWorksList = new ArrayList<>();
    private String todayDate;
    private int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_all_works);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        initViews();
        loadSession();
        loadAllWorks();
    }

    private void initViews() {
        // Initialize all views
        viewPager = findViewById(R.id.viewPager);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        btnPrevDate = findViewById(R.id.btnPrevDate);
        btnNextDate = findViewById(R.id.btnNextDate);
        fabAddWork = findViewById(R.id.fabAddWork);

        // Null checks for safety
        if (viewPager == null) {
            Toast.makeText(this, "Error: ViewPager not found in layout", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Toolbar setup
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Work Reports");
        }
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // ViewPager setup
        pagerAdapter = new WorkReportPagerAdapter(this, new WorkReportPagerAdapter.OnWorkActionListener() {
            @Override
            public void onEditClick(WorkSummary work) {
                openEditWork(work);
            }

            @Override
            public void onDeleteSuccess() {
                loadAllWorks();
            }
        });
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);

        // ViewPager page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                updateNavigationButtons();
                updatePageIndicator();
            }
        });

        // Navigation buttons
        // PREV = Older dates (swipe left behavior)
        if (btnPrevDate != null) {
            btnPrevDate.setOnClickListener(v -> navigateToPreviousDate());
        }
        // NEXT = Newer dates (swipe right behavior)
        if (btnNextDate != null) {
            btnNextDate.setOnClickListener(v -> navigateToNextDate());
        }
        if (btnDatePicker != null) {
            btnDatePicker.setOnClickListener(v -> showDatePicker());
        }

        // FAB - Add Today's Work
        if (fabAddWork != null) {
            fabAddWork.setOnClickListener(v -> {
                Intent intent = new Intent(this, EmployeeTodayWorkActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadSession() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        employeeMobile = pref.getEmployeeMobile();

        if (TextUtils.isEmpty(companyKey) || TextUtils.isEmpty(employeeMobile)) {
            Toast.makeText(this, "⚠️ Please login again", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void loadAllWorks() {
        showLoading(true);

        DatabaseReference worksRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork");

        worksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allWorksList.clear();

                if (!snapshot.exists()) {
                    showLoading(false);
                    showEmptyState(true, "No work records found.\nStart by adding today's work!");
                    return;
                }

                // Get ALL available dates data
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey();

                    try {
                        Date workDate = sdf.parse(date);
                        if (workDate != null) {
                            DataSnapshot empSnapshot = dateSnapshot.child(employeeMobile);
                            if (empSnapshot.exists()) {
                                WorkSummary work = new WorkSummary();
                                work.workDate = date;
                                work.employeeName = empSnapshot.child("employeeName").getValue(String.class);
                                work.completedWork = empSnapshot.child("completedWork").getValue(String.class);
                                work.ongoingWork = empSnapshot.child("ongoingWork").getValue(String.class);
                                work.tomorrowWork = empSnapshot.child("tomorrowWork").getValue(String.class);

                                Long timestamp = empSnapshot.child("submittedAt").getValue(Long.class);
                                work.submittedAt = (timestamp != null) ? timestamp : 0L;

                                allWorksList.add(work);
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                // Sort by date (OLDEST FIRST for natural left-to-right navigation)
                // Index 0 = oldest, Index max = newest (today)
                Collections.sort(allWorksList, (w1, w2) -> w1.workDate.compareTo(w2.workDate));

                showLoading(false);

                if (allWorksList.isEmpty()) {
                    showEmptyState(true, "No work records found.\nStart by adding today's work!");
                } else {
                    showEmptyState(false, null);
                    pagerAdapter.updateWorks(allWorksList);

                    // Auto navigate to today's report if exists
                    navigateToTodayReport();
                    updateNavigationButtons();
                    updatePageIndicator();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(EmployeeAllWorksActivity.this,
                        "❌ Failed to load: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToTodayReport() {
        if (viewPager == null) return;

        for (int i = 0; i < allWorksList.size(); i++) {
            if (allWorksList.get(i).workDate.equals(todayDate)) {
                viewPager.setCurrentItem(i, false);
                currentPosition = i;
                break;
            }
        }
    }

    // PREV button = go to OLDER date (decrease index, swipe left)
    private void navigateToPreviousDate() {
        if (viewPager != null && currentPosition > 0) {
            viewPager.setCurrentItem(currentPosition - 1, true);
        }
    }

    // NEXT button = go to NEWER date (increase index, swipe right)
    private void navigateToNextDate() {
        if (viewPager != null && currentPosition < allWorksList.size() - 1) {
            viewPager.setCurrentItem(currentPosition + 1, true);
        }
    }

    private void updateNavigationButtons() {
        // PREV button enabled if we can go to older dates (index > 0)
        if (btnPrevDate != null) {
            btnPrevDate.setEnabled(currentPosition > 0);
            btnPrevDate.setAlpha(btnPrevDate.isEnabled() ? 1.0f : 0.5f);
        }

        // NEXT button enabled if we can go to newer dates (index < max)
        if (btnNextDate != null) {
            btnNextDate.setEnabled(currentPosition < allWorksList.size() - 1);
            btnNextDate.setAlpha(btnNextDate.isEnabled() ? 1.0f : 0.5f);
        }
    }

    private void updatePageIndicator() {
        if (tvPageIndicator == null) return;

        if (!allWorksList.isEmpty() && currentPosition < allWorksList.size()) {
            WorkSummary currentWork = allWorksList.get(currentPosition);
            String dateStr = formatDate(currentWork.workDate);

            String indicator = String.format(Locale.getDefault(),
                    "%s (%d/%d)", dateStr, currentPosition + 1, allWorksList.size());

            tvPageIndicator.setText(indicator);

            // Show TODAY badge
            if (currentWork.workDate.equals(todayDate)) {
                tvPageIndicator.setText("📌 TODAY - " + indicator);
            }
        }
    }

    private void showDatePicker() {
        if (allWorksList.isEmpty()) {
            Toast.makeText(this, "No work records available", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    String selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(calendar.getTime());

                    // Find and navigate to selected date
                    for (int i = 0; i < allWorksList.size(); i++) {
                        if (allWorksList.get(i).workDate.equals(selectedDate)) {
                            viewPager.setCurrentItem(i, true);
                            return;
                        }
                    }

                    Toast.makeText(this, "No work report found for " + formatDate(selectedDate),
                            Toast.LENGTH_SHORT).show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Set min date to earliest work date in database (now at index 0)
        if (!allWorksList.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date earliestDate = sdf.parse(allWorksList.get(0).workDate);
                if (earliestDate != null) {
                    datePickerDialog.getDatePicker().setMinDate(earliestDate.getTime());
                }
            } catch (Exception e) {
                // If parsing fails, no min date restriction
            }
        }

        datePickerDialog.show();
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

    private void openEditWork(WorkSummary work) {
        if (work.workDate == null) {
            Toast.makeText(this, "Invalid work date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!work.workDate.equals(todayDate)) {
            Toast.makeText(this, "✏️ You can only edit today's work", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, EmployeeTodayWorkActivity.class);
        intent.putExtra("editMode", true);
        intent.putExtra("workData", work);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (viewPager != null) {
            viewPager.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        View navLayout = findViewById(R.id.navigationLayout);
        if (navLayout != null) {
            navLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showEmptyState(boolean show, String message) {
        if (emptyState != null) {
            emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (viewPager != null) {
            viewPager.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        View navLayout = findViewById(R.id.navigationLayout);
        if (navLayout != null) {
            navLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }

        if (show && message != null && tvEmptyMessage != null) {
            tvEmptyMessage.setText(message);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllWorks();
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