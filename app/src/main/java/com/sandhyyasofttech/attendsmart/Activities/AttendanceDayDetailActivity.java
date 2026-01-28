//package com.sandhyyasofttech.attendsmart.Activities;
//
//import android.graphics.Color;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.card.MaterialCardView;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.sandhyyasofttech.attendsmart.R;
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.Locale;
//import java.util.concurrent.TimeUnit;
//
//public class AttendanceDayDetailActivity extends AppCompatActivity {
//
//    private TextView tvDate, tvDayName, tvStatusLabel;
//    private TextView tvCheckInTime, tvCheckInLocation;
//    private TextView tvCheckOutTime, tvCheckOutLocation;
//    private TextView tvWorkingHours, tvAdditionalInfo;
//    private View viewStatusBar;
//    private MaterialCardView cardCheckIn, cardCheckOut, cardWorkingHours, cardAdditionalInfo;
//
//    private String date, employeeMobile, companyKey;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_attendance_day_detail);
//
//        date = getIntent().getStringExtra("date");
//        employeeMobile = getIntent().getStringExtra("employeeMobile");
//        companyKey = getIntent().getStringExtra("companyKey");
//
//        if (date == null || employeeMobile == null || companyKey == null) {
//            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        setupToolbar();
//        initViews();
//        loadAttendanceDetails();
//    }
//
//    private void setupToolbar() {
//        MaterialToolbar toolbar = findViewById(R.id.toolbar);
//        toolbar.setNavigationOnClickListener(v -> finish());
//    }
//
//    private void initViews() {
//        tvDate = findViewById(R.id.tvDate);
//        tvDayName = findViewById(R.id.tvDayName);
//        tvStatusLabel = findViewById(R.id.tvStatusLabel);
//        viewStatusBar = findViewById(R.id.viewStatusBar);
//
//        tvCheckInTime = findViewById(R.id.tvCheckInTime);
//        tvCheckInLocation = findViewById(R.id.tvCheckInLocation);
//        tvCheckOutTime = findViewById(R.id.tvCheckOutTime);
//        tvCheckOutLocation = findViewById(R.id.tvCheckOutLocation);
//        tvWorkingHours = findViewById(R.id.tvWorkingHours);
//        tvAdditionalInfo = findViewById(R.id.tvAdditionalInfo);
//
//        cardCheckIn = findViewById(R.id.cardCheckIn);
//        cardCheckOut = findViewById(R.id.cardCheckOut);
//        cardWorkingHours = findViewById(R.id.cardWorkingHours);
//        cardAdditionalInfo = findViewById(R.id.cardAdditionalInfo);
//
//        setDateInfo();
//    }
//
//    private void setDateInfo() {
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//            Date dateObj = sdf.parse(date);
//
//            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
//            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
//
//            tvDate.setText(displayFormat.format(dateObj));
//            tvDayName.setText(dayFormat.format(dateObj));
//        } catch (ParseException e) {
//            tvDate.setText(date);
//        }
//    }
//
//    private void loadAttendanceDetails() {
//        DatabaseReference ref = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("attendance")
//                .child(date)
//                .child(employeeMobile);
//
//        ref.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (!snapshot.exists()) {
//                    showAbsentStatus();
//                    return;
//                }
//
//                String status = snapshot.child("status").getValue(String.class);
//                String lateStatus = snapshot.child("lateStatus").getValue(String.class);
//                String checkInTime = snapshot.child("checkInTime").getValue(String.class);
//                String checkOutTime = snapshot.child("checkOutTime").getValue(String.class);
//                String checkInLoc = snapshot.child("checkInLocation").getValue(String.class);
//                String checkOutLoc = snapshot.child("checkOutLocation").getValue(String.class);
//                String lateMinutes = snapshot.child("lateMinutes").getValue(String.class);
//
//                displayAttendanceData(status, lateStatus, checkInTime, checkOutTime,
//                        checkInLoc, checkOutLoc, lateMinutes);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(AttendanceDayDetailActivity.this,
//                        "Failed to load details", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void showAbsentStatus() {
//        tvStatusLabel.setText("ABSENT");
//        tvStatusLabel.setTextColor(Color.parseColor("#F44336"));
//        viewStatusBar.setBackgroundColor(Color.parseColor("#F44336"));
//
//        cardCheckIn.setVisibility(View.GONE);
//        cardCheckOut.setVisibility(View.GONE);
//        cardWorkingHours.setVisibility(View.GONE);
//    }
//
//    private void displayAttendanceData(String status, String lateStatus, String checkInTime,
//                                       String checkOutTime, String checkInLoc, String checkOutLoc,
//                                       String lateMinutes) {
//        String finalStatus = "PRESENT";
//        int statusColor = Color.parseColor("#4CAF50");
//
//        if ("Half Day".equalsIgnoreCase(status)) {
//            finalStatus = "HALF DAY";
//            statusColor = Color.parseColor("#FF9800");
//        } else if ("Late".equalsIgnoreCase(lateStatus)) {
//            finalStatus = "LATE";
//            statusColor = Color.parseColor("#FFC107");
//
//            if (lateMinutes != null && !lateMinutes.isEmpty()) {
//                tvAdditionalInfo.setText("Late arrival: " + lateMinutes + " minutes");
//                cardAdditionalInfo.setVisibility(View.VISIBLE);
//            }
//        }
//
//        tvStatusLabel.setText(finalStatus);
//        tvStatusLabel.setTextColor(statusColor);
//        viewStatusBar.setBackgroundColor(statusColor);
//
//        if (checkInTime != null && !checkInTime.isEmpty()) {
//            tvCheckInTime.setText(checkInTime);
//            cardCheckIn.setVisibility(View.VISIBLE);
//        } else {
//            cardCheckIn.setVisibility(View.GONE);
//        }
//
//        if (checkInLoc != null && !checkInLoc.isEmpty()) {
//            tvCheckInLocation.setText(checkInLoc);
//        } else {
//            tvCheckInLocation.setText("Location not available");
//        }
//
//        if (checkOutTime != null && !checkOutTime.isEmpty()) {
//            tvCheckOutTime.setText(checkOutTime);
//            cardCheckOut.setVisibility(View.VISIBLE);
//
//            if (checkOutLoc != null && !checkOutLoc.isEmpty()) {
//                tvCheckOutLocation.setText(checkOutLoc);
//            } else {
//                tvCheckOutLocation.setText("Location not available");
//            }
//
//            calculateWorkingHours(checkInTime, checkOutTime);
//        } else {
//            cardCheckOut.setVisibility(View.GONE);
//            cardWorkingHours.setVisibility(View.GONE);
//        }
//    }
//
//    private void calculateWorkingHours(String checkInTime, String checkOutTime) {
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
//            Date checkIn = sdf.parse(checkInTime);
//            Date checkOut = sdf.parse(checkOutTime);
//
//            if (checkIn != null && checkOut != null) {
//                long diff = checkOut.getTime() - checkIn.getTime();
//                long hours = TimeUnit.MILLISECONDS.toHours(diff);
//                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
//
//                tvWorkingHours.setText(hours + "h " + minutes + "m");
//                cardWorkingHours.setVisibility(View.VISIBLE);
//            }
//        } catch (ParseException e) {
//            cardWorkingHours.setVisibility(View.GONE);
//        }
//    }
//}
package com.sandhyyasofttech.attendsmart.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AttendanceDayDetailActivity extends AppCompatActivity {

    // Views
    private TextView tvDate, tvDayName, tvShiftTime, tvStatusLabel;
    private TextView tvCheckInTime, tvCheckInLocation, tvCheckOutTime, tvCheckOutLocation;
    private TextView tvWorkingHours, tvAdditionalInfo;
    private ImageView ivCheckInPhoto, ivCheckOutPhoto;
    private View viewStatusBar;
    private MaterialCardView cardCheckIn, cardCheckOut, cardWorkingHours, cardAdditionalInfo;

    // Data
    private String date, employeeMobile, companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_day_detail);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }

        // Get intent data
        date = getIntent().getStringExtra("date");
        employeeMobile = getIntent().getStringExtra("employeeMobile");
        companyKey = new PrefManager(this).getCompanyKey();

        if (date == null || employeeMobile == null || companyKey == null) {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initViews();
        setDateInfo();
        loadEmployeeShift();  // Load shift first
        loadAttendanceDetails();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        tvDate = findViewById(R.id.tvDate);
        tvDayName = findViewById(R.id.tvDayName);
        tvShiftTime = findViewById(R.id.tvShiftTime);
        tvStatusLabel = findViewById(R.id.tvStatusLabel);
        viewStatusBar = findViewById(R.id.viewStatusBar);

        tvCheckInTime = findViewById(R.id.tvCheckInTime);
        tvCheckInLocation = findViewById(R.id.tvCheckInLocation);
        tvCheckOutTime = findViewById(R.id.tvCheckOutTime);
        tvCheckOutLocation = findViewById(R.id.tvCheckOutLocation);
        tvWorkingHours = findViewById(R.id.tvWorkingHours);
        tvAdditionalInfo = findViewById(R.id.tvAdditionalInfo);
        ivCheckInPhoto = findViewById(R.id.ivCheckInPhoto);
        ivCheckOutPhoto = findViewById(R.id.ivCheckOutPhoto);

        cardCheckIn = findViewById(R.id.cardCheckIn);
        cardCheckOut = findViewById(R.id.cardCheckOut);
        cardWorkingHours = findViewById(R.id.cardWorkingHours);
        cardAdditionalInfo = findViewById(R.id.cardAdditionalInfo);
    }

    private void setDateInfo() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date dateObj = sdf.parse(date);

            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

            tvDate.setText(displayFormat.format(dateObj));
            tvDayName.setText(dayFormat.format(dateObj));
        } catch (ParseException e) {
            tvDate.setText(date);
        }
    }

    private void loadEmployeeShift() {
        DatabaseReference employeeRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("info");

        employeeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String shiftStart = snapshot.child("shiftStart").getValue(String.class);
                    String shiftEnd = snapshot.child("shiftEnd").getValue(String.class);
                    if (shiftStart != null && shiftEnd != null) {
                        tvShiftTime.setText("Shift: " + shiftStart + " - " + shiftEnd);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAttendanceDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("attendance")
                .child(date)
                .child(employeeMobile);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    showAbsentStatus();
                    return;
                }

                // Load all attendance data exactly like admin code
                String status = snapshot.child("status").getValue(String.class);
                String lateStatus = snapshot.child("lateStatus").getValue(String.class);
                String checkInTime = snapshot.child("checkInTime").getValue(String.class);
                String checkOutTime = snapshot.child("checkOutTime").getValue(String.class);
                String checkInLoc = snapshot.child("checkInAddress").getValue(String.class);  // Matches admin
                String checkOutLoc = snapshot.child("checkOutAddress").getValue(String.class); // Matches admin
                String lateMinutes = snapshot.child("lateMinutes").getValue(String.class);
                String totalHours = snapshot.child("totalHours").getValue(String.class);

                // Load photos exactly like admin
                String checkInPhoto = snapshot.child("checkInPhoto").getValue(String.class);
                if (checkInPhoto != null && !checkInPhoto.isEmpty()) {
                    Glide.with(AttendanceDayDetailActivity.this)
                            .load(checkInPhoto)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(ivCheckInPhoto);
                    ivCheckInPhoto.setVisibility(View.VISIBLE);
                }

                String checkOutPhoto = snapshot.child("checkOutPhoto").getValue(String.class);
                if (checkOutPhoto != null && !checkOutPhoto.isEmpty()) {
                    Glide.with(AttendanceDayDetailActivity.this)
                            .load(checkOutPhoto)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(ivCheckOutPhoto);
                    ivCheckOutPhoto.setVisibility(View.VISIBLE);
                }

                displayAttendanceData(status, lateStatus, checkInTime, checkOutTime,
                        checkInLoc, checkOutLoc, lateMinutes, totalHours);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AttendanceDayDetailActivity.this,
                        "Failed to load details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAbsentStatus() {
        tvStatusLabel.setText("ABSENT");
        tvStatusLabel.setTextColor(Color.parseColor("#F44336"));
        viewStatusBar.setBackgroundColor(Color.parseColor("#F44336"));

        cardCheckIn.setVisibility(View.GONE);
        cardCheckOut.setVisibility(View.GONE);
        cardWorkingHours.setVisibility(View.GONE);
        tvShiftTime.setText("No shift assigned");
    }

    private void displayAttendanceData(String status, String lateStatus, String checkInTime,
                                       String checkOutTime, String checkInLoc, String checkOutLoc,
                                       String lateMinutes, String totalHours) {

        // Status display (same logic)
        String finalStatus = "PRESENT";
        int statusColor = Color.parseColor("#4CAF50");

        if ("Half Day".equalsIgnoreCase(status)) {
            finalStatus = "HALF DAY";
            statusColor = Color.parseColor("#FF9800");
        } else if ("Late".equalsIgnoreCase(lateStatus)) {
            finalStatus = "LATE";
            statusColor = Color.parseColor("#FFC107");
        }

        tvStatusLabel.setText(finalStatus);
        tvStatusLabel.setTextColor(statusColor);
        viewStatusBar.setBackgroundColor(statusColor);

        // Check-in data
        if (checkInTime != null && !checkInTime.isEmpty()) {
            tvCheckInTime.setText(checkInTime);
            tvCheckInLocation.setText(checkInLoc != null && !checkInLoc.isEmpty() ?
                    checkInLoc : "Location not available");
            cardCheckIn.setVisibility(View.VISIBLE);
        } else {
            cardCheckIn.setVisibility(View.GONE);
        }

        // Check-out data
        if (checkOutTime != null && !checkOutTime.isEmpty()) {
            tvCheckOutTime.setText(checkOutTime);
            tvCheckOutLocation.setText(checkOutLoc != null && !checkOutLoc.isEmpty() ?
                    checkOutLoc : "Location not available");
            cardCheckOut.setVisibility(View.VISIBLE);
        } else {
            cardCheckOut.setVisibility(View.GONE);
        }

        // Working hours
        if (totalHours != null && !totalHours.isEmpty()) {
            tvWorkingHours.setText(totalHours);
            cardWorkingHours.setVisibility(View.VISIBLE);
        } else {
            calculateWorkingHours(checkInTime, checkOutTime);
        }

        // Additional info (late, etc.)
        if (lateStatus != null && "Late".equalsIgnoreCase(lateStatus) && lateMinutes != null) {
            tvAdditionalInfo.setText("Late arrival: " + lateMinutes + " minutes");
            cardAdditionalInfo.setVisibility(View.VISIBLE);
        }
    }

    private void calculateWorkingHours(String checkInTime, String checkOutTime) {
        if (checkInTime == null || checkOutTime == null) {
            cardWorkingHours.setVisibility(View.GONE);
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date checkIn = sdf.parse(checkInTime);
            Date checkOut = sdf.parse(checkOutTime);

            if (checkIn != null && checkOut != null) {
                long diff = checkOut.getTime() - checkIn.getTime();
                if (diff < 0) diff += 24 * 60 * 60 * 1000; // Overnight shift

                long hours = TimeUnit.MILLISECONDS.toHours(diff);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

                tvWorkingHours.setText(hours + "h " + minutes + "m");
                cardWorkingHours.setVisibility(View.VISIBLE);
            }
        } catch (ParseException e) {
            cardWorkingHours.setVisibility(View.GONE);
        }
    }
}
