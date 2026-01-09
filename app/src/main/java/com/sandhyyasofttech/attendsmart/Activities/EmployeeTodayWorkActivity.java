package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.WorkSummary;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class EmployeeTodayWorkActivity extends AppCompatActivity {

    private TextInputEditText etWorkSummary, etTasks, etIssues;
    private MaterialButton btnSubmit;

    private String companyKey, employeeMobile, employeeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_today_work);

        // 🔥 ADD TOOLBAR FIRST
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();

        // 🔥 EDIT MODE CHECK BEFORE loadSession()
        boolean editMode = getIntent().getBooleanExtra("editMode", false);
        WorkSummary existing = getIntent().getParcelableExtra("workData");

        loadSession();

        if (editMode && existing != null) {
            etWorkSummary.setText(existing.workSummary);
            etTasks.setText(existing.tasks);
            etIssues.setText(existing.issues);
            btnSubmit.setText("Update Work");
        }

        btnSubmit.setOnClickListener(v -> submitTodayWork());
    }


    private void initViews() {
        etWorkSummary = findViewById(R.id.etWorkSummary);
        etTasks = findViewById(R.id.etTasks);
        etIssues = findViewById(R.id.etIssues);
        btnSubmit = findViewById(R.id.btnSubmitWork);
    }

    private void loadSession() {
        PrefManager pref = new PrefManager(this);

        companyKey = pref.getCompanyKey();
        employeeMobile = pref.getEmployeeMobile();

        if (TextUtils.isEmpty(companyKey) || TextUtils.isEmpty(employeeMobile)) {
            Toast.makeText(this, "⚠️ Please login again", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class)); // Replace with your login activity
            finish();
            return;
        }

        // Load employee name for title
        loadEmployeeName();
    }

    private void loadEmployeeName() {
        DatabaseReference empRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("info")
                .child("employeeName");

        empRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                employeeName = snapshot.getValue(String.class);
                if (!TextUtils.isEmpty(employeeName)) {
                    setTitle("Work Summary - " + employeeName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore
            }
        });
    }

    private void submitTodayWork() {
        String workSummary = etWorkSummary.getText().toString().trim();
        String tasks = etTasks.getText().toString().trim();
        String issues = etIssues.getText().toString().trim();

        if (TextUtils.isEmpty(workSummary)) {
            etWorkSummary.setError("Work summary required");
            etWorkSummary.requestFocus();
            return;
        }

        // Show loading
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference empRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("info")
                .child("employeeName");

        empRef.get().addOnSuccessListener(snapshot -> {
            String empName = snapshot.getValue(String.class);
            if (empName == null) empName = "Employee";

            DatabaseReference workRef = FirebaseDatabase.getInstance()
                    .getReference("Companies")
                    .child(companyKey)
                    .child("dailyWork")
                    .child(todayDate)
                    .child(employeeMobile);

            HashMap<String, Object> data = new HashMap<>();
            data.put("employeeName", empName);
            data.put("workSummary", workSummary);
            data.put("tasks", tasks);
            data.put("issues", issues);
            data.put("submittedAt", System.currentTimeMillis());

            workRef.setValue(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(EmployeeTodayWorkActivity.this,
                                "✅ Today's work submitted successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EmployeeTodayWorkActivity.this,
                                "❌ Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit Today's Work");
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(EmployeeTodayWorkActivity.this,
                    "❌ Failed to load profile", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit Today's Work");
        });
    }
}
