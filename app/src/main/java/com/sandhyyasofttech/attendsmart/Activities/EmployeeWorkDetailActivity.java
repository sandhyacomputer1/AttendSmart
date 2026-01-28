package com.sandhyyasofttech.attendsmart.Activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmployeeWorkDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvEmployeeName, tvDate, tvStatus;
    private CardView cardSubmitted, cardNotSubmitted;
    private LinearLayout layoutWorkDetails;
    private TextView tvCompletedWork, tvOngoingWork, tvTomorrowWork, tvSubmittedTime;
    private MaterialButton btnAssignTask, btnViewAllWork;
    private ProgressBar progressBar;

    private String companyKey, employeeMobile, employeeName, selectedDate;
    private boolean hasSubmitted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_work_detail);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        getIntentData();
        initViews();
        loadWorkDetails();
    }

    private void getIntentData() {
        employeeMobile = getIntent().getStringExtra("employeeMobile");
        employeeName = getIntent().getStringExtra("employeeName");
        selectedDate = getIntent().getStringExtra("selectedDate");
        hasSubmitted = getIntent().getBooleanExtra("hasSubmitted", false);

        if (TextUtils.isEmpty(employeeMobile) || TextUtils.isEmpty(selectedDate)) {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show();
            finish();
        }

        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Work Details");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvDate = findViewById(R.id.tvDate);
        tvStatus = findViewById(R.id.tvStatus);
        cardSubmitted = findViewById(R.id.cardSubmitted);
        cardNotSubmitted = findViewById(R.id.cardNotSubmitted);
        layoutWorkDetails = findViewById(R.id.layoutWorkDetails);
        tvCompletedWork = findViewById(R.id.tvCompletedWork);
        tvOngoingWork = findViewById(R.id.tvOngoingWork);
        tvTomorrowWork = findViewById(R.id.tvTomorrowWork);
        tvSubmittedTime = findViewById(R.id.tvSubmittedTime);
        btnAssignTask = findViewById(R.id.btnAssignTask);
        btnViewAllWork = findViewById(R.id.btnViewAllWork);
        progressBar = findViewById(R.id.progressBar);

        tvEmployeeName.setText(employeeName);

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(selectedDate);
            tvDate.setText("Date: " + outputFormat.format(date));
        } catch (Exception e) {
            tvDate.setText("Date: " + selectedDate);
        }

        btnAssignTask.setOnClickListener(v -> showAssignTaskDialog());
        btnViewAllWork.setOnClickListener(v -> {
            Intent intent = new Intent(
                    EmployeeWorkDetailActivity.this,
                    EmployeeWorkHistoryActivity.class
            );
            intent.putExtra("employeeMobile", employeeMobile);
            intent.putExtra("employeeName", employeeName);
            startActivity(intent);
        });

    }

    private void loadWorkDetails() {
        showLoading(true);

        DatabaseReference workRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork")
                .child(selectedDate)
                .child(employeeMobile);

        workRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);

                if (snapshot.exists()) {
                    // Work submitted
                    cardSubmitted.setVisibility(View.VISIBLE);
                    cardNotSubmitted.setVisibility(View.GONE);
                    layoutWorkDetails.setVisibility(View.VISIBLE);
                    tvStatus.setText("✅ Work Submitted");
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                    String completed = snapshot.child("completedWork").getValue(String.class);
                    String ongoing = snapshot.child("ongoingWork").getValue(String.class);
                    String tomorrow = snapshot.child("tomorrowWork").getValue(String.class);
                    Long timestamp = snapshot.child("submittedAt").getValue(Long.class);

                    tvCompletedWork.setText(TextUtils.isEmpty(completed) ? "No work completed" : completed);
                    tvOngoingWork.setText(TextUtils.isEmpty(ongoing) ? "No ongoing work" : ongoing);
                    tvTomorrowWork.setText(TextUtils.isEmpty(tomorrow) ? "No work planned" : tomorrow);

                    if (timestamp != null && timestamp > 0) {
                        String time = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                .format(new Date(timestamp));
                        tvSubmittedTime.setText("Submitted at: " + time);
                    }
                } else {
                    // Work not submitted
                    cardSubmitted.setVisibility(View.GONE);
                    cardNotSubmitted.setVisibility(View.VISIBLE);
                    layoutWorkDetails.setVisibility(View.GONE);
                    tvStatus.setText("⏳ Work Not Submitted");
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(EmployeeWorkDetailActivity.this,
                        "❌ Failed to load: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAssignTaskDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_assign_task);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        EditText etTaskTitle = dialog.findViewById(R.id.etTaskTitle);
        EditText etTaskDescription = dialog.findViewById(R.id.etTaskDescription);
        EditText etTaskPriority = dialog.findViewById(R.id.etTaskPriority);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnAssign = dialog.findViewById(R.id.btnAssign);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBar);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAssign.setOnClickListener(v -> {
            String title = etTaskTitle.getText().toString().trim();
            String description = etTaskDescription.getText().toString().trim();
            String priority = etTaskPriority.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                etTaskTitle.setError("Task title required");
                return;
            }

            if (TextUtils.isEmpty(description)) {
                etTaskDescription.setError("Task description required");
                return;
            }

            // Assign task
            progressBar.setVisibility(View.VISIBLE);
            btnAssign.setEnabled(false);

            String taskId = FirebaseDatabase.getInstance().getReference().push().getKey();
            DatabaseReference taskRef = FirebaseDatabase.getInstance()
                    .getReference("Companies")
                    .child(companyKey)
                    .child("tasks")
                    .child(employeeMobile)
                    .child(taskId);

            Map<String, Object> taskData = new HashMap<>();
            taskData.put("taskId", taskId);
            taskData.put("taskTitle", title);
            taskData.put("taskDescription", description);
            taskData.put("taskPriority", TextUtils.isEmpty(priority) ? "Medium" : priority);
            taskData.put("assignedTo", employeeMobile);
            taskData.put("assignedToName", employeeName);
            taskData.put("assignedBy", new PrefManager(this).getEmployeeMobile());
            taskData.put("assignedByName", new PrefManager(this).getEmployeeName());
            taskData.put("assignedDate", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
            taskData.put("assignedAt", System.currentTimeMillis());
            taskData.put("status", "Pending");
            taskData.put("completedAt", 0L);

            taskRef.setValue(taskData).addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                btnAssign.setEnabled(true);

                if (task.isSuccessful()) {
                    Toast.makeText(this, "✅ Task assigned successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "❌ Failed to assign task", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}