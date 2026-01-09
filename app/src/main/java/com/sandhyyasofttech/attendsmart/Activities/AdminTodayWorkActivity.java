package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.TodayWorkAdapter;
import com.sandhyyasofttech.attendsmart.Models.TodayWorkModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AdminTodayWorkActivity extends AppCompatActivity {

    private RecyclerView rvWork;
    private TextView tvDate, tvEmpty, tvLoading;
    private ProgressBar progressBar;

    private ArrayList<TodayWorkModel> workList;
    private TodayWorkAdapter adapter;

    private String companyKey, todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_today_work);

        initViews();
        loadSession();
        loadTodayWork();
    }

    private void initViews() {
        tvDate = findViewById(R.id.tvDate);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvLoading = findViewById(R.id.tvLoading);
        progressBar = findViewById(R.id.progressBar);
        rvWork = findViewById(R.id.rvTodayWork);

        rvWork.setLayoutManager(new LinearLayoutManager(this));
        workList = new ArrayList<>();
        adapter = new TodayWorkAdapter(workList);
        rvWork.setAdapter(adapter);
    }

    private void loadSession() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();

        if (companyKey == null || companyKey.isEmpty()) {
            Toast.makeText(this, "⚠️ Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText("📋 Today's Work Summary (" + todayDate + ")");
    }

    private void loadTodayWork() {
        showLoading(true);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork")
                .child(todayDate);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);

                workList.clear();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    showEmptyState();
                    return;
                }

                int count = 0;
                for (DataSnapshot empSnap : snapshot.getChildren()) {
                    TodayWorkModel model = empSnap.getValue(TodayWorkModel.class);
                    if (model != null) {
                        model.setEmployeeMobile(empSnap.getKey());
                        workList.add(model);
                        count++;
                    }
                }

                if (workList.isEmpty()) {
                    showEmptyState();
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvWork.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                    tvDate.setText("📋 Today's Work Summary (" + todayDate + ") - " + count + " submissions");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(AdminTodayWorkActivity.this,
                        "❌ Failed to load: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        tvLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        rvWork.setVisibility(show ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        tvEmpty.setVisibility(View.VISIBLE);
        rvWork.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        tvLoading.setVisibility(View.GONE);
    }
}
