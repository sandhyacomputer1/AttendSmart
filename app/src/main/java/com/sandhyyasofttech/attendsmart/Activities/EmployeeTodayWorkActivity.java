package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmployeeTodayWorkActivity extends AppCompatActivity {

    private static final int REQUEST_COMPLETED = 101;
    private static final int REQUEST_ONGOING = 102;
    private static final int REQUEST_TOMORROW = 103;

    private CardView cardCompleted, cardOngoing, cardTomorrow;
    private TextView tvCompletedPreview, tvOngoingPreview, tvTomorrowPreview;
    private TextView tvCompletedStatus, tvOngoingStatus, tvTomorrowStatus;
    private MaterialButton layoutSubmit;
    private ImageView btnHistory;


    private String companyKey, employeeMobile, employeeName;
    private String todayDate;

    private String completedWork = "";
    private String ongoingWork = "";
    private String tomorrowWork = "";

    private MaterialToolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_today_work);

        // Toolbar Setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Daily Work Report");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        initViews();
        loadSession();
        loadExistingData();
        setupClickListeners();
    }

    private void initViews() {
        cardCompleted = findViewById(R.id.cardCompleted);
        cardOngoing = findViewById(R.id.cardOngoing);
        cardTomorrow = findViewById(R.id.cardTomorrow);

        tvCompletedPreview = findViewById(R.id.tvCompletedPreview);
        tvOngoingPreview = findViewById(R.id.tvOngoingPreview);
        tvTomorrowPreview = findViewById(R.id.tvTomorrowPreview);

        tvCompletedStatus = findViewById(R.id.tvCompletedStatus);
        tvOngoingStatus = findViewById(R.id.tvOngoingStatus);
        tvTomorrowStatus = findViewById(R.id.tvTomorrowStatus);

        layoutSubmit = findViewById(R.id.layoutSubmit); // MaterialButton
        btnHistory = findViewById(R.id.btnHistory); // MaterialButton



        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Today's Work Report");
        }

        // Back Arrow Click Handler
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // History Button Setup
        btnHistory = findViewById(R.id.btnHistory);
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(EmployeeTodayWorkActivity.this, EmployeeAllWorksActivity.class);
            startActivity(intent);
        });

        // Submit Button
        layoutSubmit = findViewById(R.id.layoutSubmit);
        layoutSubmit.setOnClickListener(v -> {
            // Your submit logic here
        });

        // ... rest of your existing view initialization code ...
    }

    // Handle back arrow press in options menu (alternative method)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void loadSession() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        employeeMobile = pref.getEmployeeMobile();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (TextUtils.isEmpty(companyKey) || TextUtils.isEmpty(employeeMobile)) {
            Toast.makeText(this, "⚠️ Please login again", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

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
                if (TextUtils.isEmpty(employeeName)) {
                    employeeName = "Employee";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                employeeName = "Employee";
            }
        });
    }

    private void loadExistingData() {
        DatabaseReference workRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork")
                .child(todayDate)
                .child(employeeMobile);

        workRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    completedWork = snapshot.child("completedWork").getValue(String.class);
                    ongoingWork = snapshot.child("ongoingWork").getValue(String.class);
                    tomorrowWork = snapshot.child("tomorrowWork").getValue(String.class);

                    if (completedWork == null) completedWork = "";
                    if (ongoingWork == null) ongoingWork = "";
                    if (tomorrowWork == null) tomorrowWork = "";

                    updateUIAfterLoad();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore
            }
        });
    }

    private void updateUIAfterLoad() {
        updatePreview(tvCompletedPreview, tvCompletedStatus, completedWork);
        updatePreview(tvOngoingPreview, tvOngoingStatus, ongoingWork);
        updatePreview(tvTomorrowPreview, tvTomorrowStatus, tomorrowWork);
    }

    private void setupClickListeners() {
        cardCompleted.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkDetailActivity.class);
            intent.putExtra("type", "completed");
            intent.putExtra("title", "Completed Work");
            intent.putExtra("hint", "What did you complete today?");
            intent.putExtra("existingData", completedWork);
            startActivityForResult(intent, REQUEST_COMPLETED);
        });

        cardOngoing.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkDetailActivity.class);
            intent.putExtra("type", "ongoing");
            intent.putExtra("title", "Ongoing Work");
            intent.putExtra("hint", "What are you currently working on?");
            intent.putExtra("existingData", ongoingWork);
            startActivityForResult(intent, REQUEST_ONGOING);
        });

        cardTomorrow.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkDetailActivity.class);
            intent.putExtra("type", "tomorrow");
            intent.putExtra("title", "Tomorrow's Plan");
            intent.putExtra("hint", "What do you plan to work on tomorrow?");
            intent.putExtra("existingData", tomorrowWork);
            startActivityForResult(intent, REQUEST_TOMORROW);
        });

        layoutSubmit.setOnClickListener(v -> submitDailyReport());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String workData = data.getStringExtra("workData");
            if (workData == null) workData = "";

            switch (requestCode) {
                case REQUEST_COMPLETED:
                    completedWork = workData;
                    updatePreview(tvCompletedPreview, tvCompletedStatus, completedWork);
                    break;
                case REQUEST_ONGOING:
                    ongoingWork = workData;
                    updatePreview(tvOngoingPreview, tvOngoingStatus, ongoingWork);
                    break;
                case REQUEST_TOMORROW:
                    tomorrowWork = workData;
                    updatePreview(tvTomorrowPreview, tvTomorrowStatus, tomorrowWork);
                    break;
            }
        }
    }

    private void updatePreview(TextView previewView, TextView statusView, String data) {
        if (TextUtils.isEmpty(data)) {
            previewView.setText("Tap to add details");
            previewView.setTextColor(0xFF9E9E9E);
            statusView.setText("Not Added");
            statusView.setVisibility(View.VISIBLE);
        } else {
            previewView.setText(data);
            previewView.setTextColor(0xFF212121);
            statusView.setVisibility(View.GONE);
        }
    }

    private void submitDailyReport() {
        if (TextUtils.isEmpty(completedWork) && TextUtils.isEmpty(ongoingWork) && TextUtils.isEmpty(tomorrowWork)) {
            Toast.makeText(this, "⚠️ Please add at least one section", Toast.LENGTH_SHORT).show();
            return;
        }

        layoutSubmit.setEnabled(false);

        DatabaseReference workRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork")
                .child(todayDate)
                .child(employeeMobile);

        java.util.HashMap<String, Object> reportData = new java.util.HashMap<>();
        reportData.put("employeeName", employeeName);
        reportData.put("completedWork", completedWork);
        reportData.put("ongoingWork", ongoingWork);
        reportData.put("tomorrowWork", tomorrowWork);
        reportData.put("submittedAt", System.currentTimeMillis());
        reportData.put("date", todayDate);

        workRef.setValue(reportData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "✅ Daily report submitted successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    layoutSubmit.setEnabled(true);
                });
    }
}