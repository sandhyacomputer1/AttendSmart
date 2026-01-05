package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AllAttendanceActivity extends AppCompatActivity {

    private RecyclerView rvAttendance;
    private TextView tvDate;
    private DatabaseReference attendanceRef;
    private String companyKey;

    // Later you can connect adapter
    // private AttendanceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_attendance);

        initViews();
        setupFirebase();
        loadTodayAttendance();
    }

    private void initViews() {
        rvAttendance = findViewById(R.id.rvAttendance);
        tvDate = findViewById(R.id.tvDate);

        rvAttendance.setLayoutManager(new LinearLayoutManager(this));
        rvAttendance.setHasFixedSize(true);

        tvDate.setText(getTodayDate());
    }

    private void setupFirebase() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();

        attendanceRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("attendance");
    }

    private void loadTodayAttendance() {
        String today = getTodayDate();

        attendanceRef.child(today)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        // 🔥 FOR NOW JUST LOG / COUNT
                        int total = (int) snapshot.getChildrenCount();
                        tvDate.setText("Attendance : " + today + " (" + total + ")");

                        // Later → attach adapter here
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
    }
}
