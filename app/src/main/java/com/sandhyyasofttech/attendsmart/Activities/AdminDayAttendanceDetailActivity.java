//package com.sandhyyasofttech.attendsmart.Activities;
//
//import android.app.AlertDialog;
//import android.app.TimePickerDialog;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.bumptech.glide.Glide;
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.card.MaterialCardView;
//import com.google.android.material.textfield.TextInputEditText;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.sandhyyasofttech.attendsmart.R;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Set;
//
//public class AdminDayAttendanceDetailActivity extends AppCompatActivity {
//
//    private static final String TAG = "AdminAttendanceDetail";
//
//    private MaterialToolbar toolbar;
//    private TextView tvDate, tvTotalHours, tvShiftTiming;
//    private LinearLayout sessionsContainer;
//    private MaterialButton btnSave, btnDelete, btnAddSession;
//
//    // Status buttons
//    private MaterialButton btnStatusPresent, btnStatusHalfDay, btnStatusAbsent;
//    private MaterialButton btnLateOnTime, btnLateLate;
//
//    private String companyKey, employeeMobile, date;
//    private DatabaseReference attendanceRef, employeeRef, companyRef;
//
//    private String employeeShift = "";
//    private String shiftStartTime = "";
//    private String shiftEndTime = "";
//
//    // Selected status
//    private String selectedStatus = "";
//    private String selectedLateStatus = "";
//
//    // Store all sessions
//    private List<SessionData> sessions = new ArrayList<>();
//
//    // Helper class to store session data
//    private static class SessionData {
//        String checkInTime = "";
//        String checkOutTime = "";
//        String checkInPhoto = "";
//        String checkOutPhoto = "";
//        String checkInAddress = "";
//        String checkOutAddress = "";
//        double checkInLat, checkInLng;
//        double checkOutLat, checkOutLng;
//        boolean checkInGPS, checkOutGPS;
//        View sessionView; // Reference to the UI view
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_admin_day_attendance_detail);
//
//        companyKey = getIntent().getStringExtra("companyKey");
//        employeeMobile = getIntent().getStringExtra("employeeMobile");
//        date = getIntent().getStringExtra("date");
//
//        Log.d(TAG, "CompanyKey: " + companyKey);
//        Log.d(TAG, "EmployeeMobile: " + employeeMobile);
//        Log.d(TAG, "Date: " + date);
//
//        initViews();
//        setupToolbar();
//        setupFirebase();
//        loadEmployeeData();
//    }
//
//    private void initViews() {
//        toolbar = findViewById(R.id.toolbar);
//        tvDate = findViewById(R.id.tvDate);
//        tvTotalHours = findViewById(R.id.tvTotalHours);
//        tvShiftTiming = findViewById(R.id.tvShiftTiming);
//        sessionsContainer = findViewById(R.id.sessionsContainer);
//
//        // Status buttons
//        btnStatusPresent = findViewById(R.id.btnStatusPresent);
//        btnStatusHalfDay = findViewById(R.id.btnStatusHalfDay);
//        btnStatusAbsent = findViewById(R.id.btnStatusAbsent);
//        btnLateOnTime = findViewById(R.id.btnLateOnTime);
//        btnLateLate = findViewById(R.id.btnLateLate);
//
//        btnSave = findViewById(R.id.btnSave);
//        btnDelete = findViewById(R.id.btnDelete);
//        btnAddSession = findViewById(R.id.btnAddSession);
//
//        formatAndDisplayDate();
//        setupStatusButtons();
//
//        btnAddSession.setOnClickListener(v -> addNewSession());
//        btnSave.setOnClickListener(v -> saveAttendance());
//        btnDelete.setOnClickListener(v -> showDeletePopup());
//    }
//
//    private void setupStatusButtons() {
//        btnStatusPresent.setOnClickListener(v -> selectStatus("Present"));
//        btnStatusHalfDay.setOnClickListener(v -> selectStatus("Half Day"));
//        btnStatusAbsent.setOnClickListener(v -> selectStatus("Absent"));
//
//        btnLateOnTime.setOnClickListener(v -> selectLateStatus("On Time"));
//        btnLateLate.setOnClickListener(v -> selectLateStatus("Late"));
//    }
//
//    private void selectStatus(String status) {
//        selectedStatus = status;
//
//        resetStatusButton(btnStatusPresent);
//        resetStatusButton(btnStatusHalfDay);
//        resetStatusButton(btnStatusAbsent);
//
//        MaterialButton selectedBtn = null;
//        int color = 0;
//
//        switch (status) {
//            case "Present":
//                selectedBtn = btnStatusPresent;
//                color = 0xFF43A047;
//                break;
//            case "Half Day":
//                selectedBtn = btnStatusHalfDay;
//                color = 0xFFFF9800;
//                break;
//            case "Absent":
//                selectedBtn = btnStatusAbsent;
//                color = 0xFFE53935;
//                break;
//        }
//
//        if (selectedBtn != null) {
//            selectedBtn.setBackgroundColor(color);
//            selectedBtn.setTextColor(0xFFFFFFFF);
//            selectedBtn.setStrokeWidth(0);
//        }
//    }
//
//    private void selectLateStatus(String lateStatus) {
//        selectedLateStatus = lateStatus;
//
//        resetStatusButton(btnLateOnTime);
//        resetStatusButton(btnLateLate);
//
//        MaterialButton selectedBtn = null;
//        int color = 0;
//
//        if ("On Time".equals(lateStatus)) {
//            selectedBtn = btnLateOnTime;
//            color = 0xFF43A047;
//        } else if ("Late".equals(lateStatus)) {
//            selectedBtn = btnLateLate;
//            color = 0xFFE53935;
//        }
//
//        if (selectedBtn != null) {
//            selectedBtn.setBackgroundColor(color);
//            selectedBtn.setTextColor(0xFFFFFFFF);
//            selectedBtn.setStrokeWidth(0);
//        }
//    }
//
//    private void resetStatusButton(MaterialButton button) {
//        button.setBackgroundColor(0xFFFFFFFF);
//        button.setTextColor(0xFF757575);
//        button.setStrokeColorResource(android.R.color.darker_gray);
//        button.setStrokeWidth(4);
//    }
//
//    private void formatAndDisplayDate() {
//        try {
//            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
//            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH);
//            Date dateObj = inputFormat.parse(date);
//            if (dateObj != null) {
//                tvDate.setText(outputFormat.format(dateObj));
//            } else {
//                tvDate.setText(date);
//            }
//        } catch (Exception e) {
//            tvDate.setText(date);
//        }
//    }
//
//    private void setupToolbar() {
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setTitle("Attendance Details");
//        }
//        toolbar.setNavigationOnClickListener(v -> onBackPressed());
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            onBackPressed();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    private void setupFirebase() {
//        companyRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey);
//
//        attendanceRef = companyRef
//                .child("attendance")
//                .child(date)
//                .child(employeeMobile);
//
//        employeeRef = companyRef
//                .child("employees")
//                .child(employeeMobile)
//                .child("info");
//    }
//
//    private void loadEmployeeData() {
//        Log.d(TAG, "Loading employee data...");
//
//        employeeRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    employeeShift = snapshot.child("employeeShift").getValue(String.class);
//                    Log.d(TAG, "Employee shift: " + employeeShift);
//
//                    if (employeeShift != null && !employeeShift.isEmpty()) {
//                        loadShiftDetails(employeeShift);
//                    } else {
//                        tvShiftTiming.setText("No shift assigned");
//                        loadAttendance();
//                    }
//                } else {
//                    tvShiftTiming.setText("Employee not found");
//                    loadAttendance();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e(TAG, "Error loading employee: " + error.getMessage());
//                loadAttendance();
//            }
//        });
//    }
//
//    private void loadShiftDetails(String shiftName) {
//        DatabaseReference shiftRef = companyRef.child("shifts").child(shiftName);
//
//        shiftRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    shiftStartTime = snapshot.child("startTime").getValue(String.class);
//                    shiftEndTime = snapshot.child("endTime").getValue(String.class);
//
//                    if (shiftStartTime != null && shiftEndTime != null) {
//                        tvShiftTiming.setText(shiftStartTime + " - " + shiftEndTime);
//                    } else {
//                        tvShiftTiming.setText(shiftName + " Shift");
//                    }
//                } else {
//                    tvShiftTiming.setText("Shift not found");
//                }
//                loadAttendance();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e(TAG, "Error loading shift: " + error.getMessage());
//                loadAttendance();
//            }
//        });
//    }
//
//    private void loadAttendance() {
//        Log.d(TAG, "Loading attendance...");
//
//        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot s) {
//                sessions.clear();
//                sessionsContainer.removeAllViews();
//
//                if (!s.exists()) {
//                    selectStatus("Absent");
//                    selectLateStatus("On Time");
//                    tvTotalHours.setText("0h 0m");
//                    addNewSession();
//                    return;
//                }
//
//                // Load status and late status
//                selectStatus(s.child("status").getValue(String.class) != null ?
//                        s.child("status").getValue(String.class) : "Present");
//                selectLateStatus(s.child("lateStatus").getValue(String.class) != null ?
//                        s.child("lateStatus").getValue(String.class) : "On Time");
//
//                // ✅ STEP 1: Check checkInOutPairs
//                DataSnapshot pairsSnapshot = s.child("checkInOutPairs");
//                boolean hasPairs = pairsSnapshot.exists() && pairsSnapshot.getChildrenCount() > 0;
//
//                if (hasPairs) {
//                    // ✅ Load from pairs structure
//                    List<SessionData> allPairData = new ArrayList<>();
//
//                    for (DataSnapshot pairSnap : pairsSnapshot.getChildren()) {
//                        SessionData pairData = new SessionData();
//                        pairData.checkInTime = pairSnap.child("checkInTime").getValue(String.class);
//                        pairData.checkOutTime = pairSnap.child("checkOutTime").getValue(String.class);
//                        pairData.checkInPhoto = pairSnap.child("checkInPhoto").getValue(String.class);
//                        pairData.checkOutPhoto = pairSnap.child("checkOutPhoto").getValue(String.class);
//                        pairData.checkInAddress = pairSnap.child("checkInAddress").getValue(String.class);
//                        pairData.checkOutAddress = pairSnap.child("checkOutAddress").getValue(String.class);
//
//                        // Load GPS data
//                        Double lat = pairSnap.child("checkInLat").getValue(Double.class);
//                        pairData.checkInLat = lat != null ? lat : 0.0;
//                        Double lng = pairSnap.child("checkInLng").getValue(Double.class);
//                        pairData.checkInLng = lng != null ? lng : 0.0;
//
//                        lat = pairSnap.child("checkOutLat").getValue(Double.class);
//                        pairData.checkOutLat = lat != null ? lat : 0.0;
//                        lng = pairSnap.child("checkOutLng").getValue(Double.class);
//                        pairData.checkOutLng = lng != null ? lng : 0.0;
//
//                        Boolean gps = pairSnap.child("checkInGPS").getValue(Boolean.class);
//                        pairData.checkInGPS = gps != null ? gps : false;
//                        gps = pairSnap.child("checkOutGPS").getValue(Boolean.class);
//                        pairData.checkOutGPS = gps != null ? gps : false;
//
//                        allPairData.add(pairData);
//                    }
//
//                    // ✅ STEP 2: Match and merge duplicate check-ins
//                    List<SessionData> mergedPairs = new ArrayList<>();
//
//                    for (SessionData pairData : allPairData) {
//                        if (pairData.checkInTime == null || pairData.checkInTime.isEmpty()) {
//                            continue;
//                        }
//
//                        // Check if this check-in already exists
//                        boolean alreadyMerged = false;
//                        for (SessionData merged : mergedPairs) {
//                            if (pairData.checkInTime.equals(merged.checkInTime)) {
//                                alreadyMerged = true;
//                                // If current pair has check-out but merged doesn't, update it
//                                if ((merged.checkOutTime == null || merged.checkOutTime.isEmpty()) &&
//                                        pairData.checkOutTime != null && !pairData.checkOutTime.isEmpty()) {
//                                    merged.checkOutTime = pairData.checkOutTime;
//                                    merged.checkOutPhoto = pairData.checkOutPhoto;
//                                    merged.checkOutAddress = pairData.checkOutAddress;
//                                    merged.checkOutLat = pairData.checkOutLat;
//                                    merged.checkOutLng = pairData.checkOutLng;
//                                    merged.checkOutGPS = pairData.checkOutGPS;
//                                }
//                                break;
//                            }
//                        }
//
//                        if (!alreadyMerged) {
//                            mergedPairs.add(pairData);
//                        }
//                    }
//
//                    // ✅ STEP 3: Add ALL pairs (complete AND incomplete)
//                    for (SessionData pairData : mergedPairs) {
//                        if (pairData.checkInTime != null && !pairData.checkInTime.isEmpty()) {
//                            sessions.add(pairData);
//                            addSessionView(pairData);
//                        }
//                    }
//                }
//
//                // ✅ FALLBACK: Check main fields
//                if (sessions.isEmpty()) {
//                    String checkInTime = s.child("checkInTime").getValue(String.class);
//                    String checkOutTime = s.child("checkOutTime").getValue(String.class);
//
//                    if (checkInTime != null && !checkInTime.isEmpty()) {
//                        SessionData session = new SessionData();
//                        session.checkInTime = checkInTime;
//                        session.checkOutTime = checkOutTime;
//                        session.checkInPhoto = s.child("checkInPhoto").getValue(String.class);
//                        session.checkOutPhoto = s.child("checkOutPhoto").getValue(String.class);
//                        session.checkInAddress = s.child("checkInAddress").getValue(String.class);
//                        session.checkOutAddress = s.child("checkOutAddress").getValue(String.class);
//
//                        // Load GPS data
//                        Double lat = s.child("checkInLat").getValue(Double.class);
//                        session.checkInLat = lat != null ? lat : 0.0;
//                        Double lng = s.child("checkInLng").getValue(Double.class);
//                        session.checkInLng = lng != null ? lng : 0.0;
//
//                        lat = s.child("checkOutLat").getValue(Double.class);
//                        session.checkOutLat = lat != null ? lat : 0.0;
//                        lng = s.child("checkOutLng").getValue(Double.class);
//                        session.checkOutLng = lng != null ? lng : 0.0;
//
//                        Boolean gps = s.child("checkInGPS").getValue(Boolean.class);
//                        session.checkInGPS = gps != null ? gps : false;
//                        gps = s.child("checkOutGPS").getValue(Boolean.class);
//                        session.checkOutGPS = gps != null ? gps : false;
//
//                        sessions.add(session);
//                        addSessionView(session);
//                    }
//                }
//
//                // If no sessions, add empty one
//                if (sessions.isEmpty()) {
//                    addNewSession();
//                }
//
//                calculateTotalHours();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError e) {
//                Log.e(TAG, "Error loading attendance: " + e.getMessage());
//            }
//        });
//    }
//
//    private void addNewSession() {
//        SessionData session = new SessionData();
//        sessions.add(session);
//        addSessionView(session);
//        calculateTotalHours();
//    }
//
//    private void addSessionView(SessionData session) {
//        View sessionView = LayoutInflater.from(this).inflate(R.layout.item_admin_session, sessionsContainer, false);
//        session.sessionView = sessionView;
//
//        TextView tvSessionNumber = sessionView.findViewById(R.id.tvSessionNumber);
//        TextInputEditText etCheckIn = sessionView.findViewById(R.id.etCheckIn);
//        TextInputEditText etCheckOut = sessionView.findViewById(R.id.etCheckOut);
//        TextView tvCheckInAddress = sessionView.findViewById(R.id.tvCheckInAddress);
//        TextView tvCheckOutAddress = sessionView.findViewById(R.id.tvCheckOutAddress);
//        ImageView ivCheckInPhoto = sessionView.findViewById(R.id.ivCheckInPhoto);
//        ImageView ivCheckOutPhoto = sessionView.findViewById(R.id.ivCheckOutPhoto);
//        MaterialCardView cardCheckInPhoto = sessionView.findViewById(R.id.cardCheckInPhoto);
//        MaterialCardView cardCheckOutPhoto = sessionView.findViewById(R.id.cardCheckOutPhoto);
//        MaterialButton btnRemove = sessionView.findViewById(R.id.btnRemoveSession);
//
//        // ✅ ADD THIS: Session duration TextView
//        TextView tvSessionDuration = sessionView.findViewById(R.id.tvSessionDuration);
//
//        int sessionNumber = sessions.indexOf(session) + 1;
//        tvSessionNumber.setText("Session " + sessionNumber);
//
//        // Set times
//        etCheckIn.setText(session.checkInTime);
//        etCheckOut.setText(session.checkOutTime);
//
//        // ✅ Calculate and display session duration
//        String durationText = calculateSessionDuration(session);
//        tvSessionDuration.setText(durationText);
//
//        // Set addresses
//        if (session.checkInAddress != null && !session.checkInAddress.isEmpty()) {
//            tvCheckInAddress.setText(session.checkInAddress);
//        } else {
//            tvCheckInAddress.setText("No location");
//        }
//
//        if (session.checkOutAddress != null && !session.checkOutAddress.isEmpty()) {
//            tvCheckOutAddress.setText(session.checkOutAddress);
//        } else {
//            tvCheckOutAddress.setText("No location");
//        }
//
//        // Load check-in photo
//        if (session.checkInPhoto != null && !session.checkInPhoto.isEmpty()) {
//            Glide.with(this).load(session.checkInPhoto)
//                    .placeholder(R.drawable.ic_image_placeholder)
//                    .into(ivCheckInPhoto);
//        } else {
//            ivCheckInPhoto.setImageResource(R.drawable.ic_image_placeholder);
//        }
//
//        // Load check-out photo (if exists)
//        if (session.checkOutPhoto != null && !session.checkOutPhoto.isEmpty()) {
//            Glide.with(this).load(session.checkOutPhoto)
//                    .placeholder(R.drawable.ic_image_placeholder)
//                    .into(ivCheckOutPhoto);
//        } else {
//            ivCheckOutPhoto.setImageResource(R.drawable.ic_image_placeholder);
//        }
//
//        // Time pickers - update to recalculate duration when time changes
//        etCheckIn.setOnClickListener(v -> openTimePicker(etCheckIn, session, true, tvSessionDuration));
//        etCheckOut.setOnClickListener(v -> openTimePicker(etCheckOut, session, false, tvSessionDuration));
//
//        // Photo click listeners
//        cardCheckInPhoto.setOnClickListener(v -> {
//            if (session.checkInPhoto != null && !session.checkInPhoto.isEmpty()) {
//                openImageInGallery(session.checkInPhoto);
//            } else {
//                Toast.makeText(AdminDayAttendanceDetailActivity.this, "No check-in photo available", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        cardCheckOutPhoto.setOnClickListener(v -> {
//            if (session.checkOutPhoto != null && !session.checkOutPhoto.isEmpty()) {
//                openImageInGallery(session.checkOutPhoto);
//            } else {
//                Toast.makeText(AdminDayAttendanceDetailActivity.this, "No check-out photo available", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // Remove button
//        btnRemove.setOnClickListener(v -> {
//            if (sessions.size() > 1) {
//                sessions.remove(session);
//                sessionsContainer.removeView(sessionView);
//                updateSessionNumbers();
//                calculateTotalHours();
//            } else {
//                Toast.makeText(this, "At least one session is required", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        sessionsContainer.addView(sessionView);
//    }
//
//    private void openTimePicker(TextInputEditText target, SessionData session, boolean isCheckIn, TextView tvDuration) {
//        Calendar cal = Calendar.getInstance();
//
//        String currentTime = target.getText() != null ? target.getText().toString() : "";
//        if (!currentTime.isEmpty()) {
//            try {
//                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//                Date date = sdf.parse(currentTime);
//                if (date != null) {
//                    cal.setTime(date);
//                }
//            } catch (Exception ignored) {}
//        }
//
//        new TimePickerDialog(this, (v, h, m) -> {
//            String amPm = h >= 12 ? "PM" : "AM";
//            int hour12 = h % 12;
//            if (hour12 == 0) hour12 = 12;
//
//            String formattedTime = String.format(Locale.ENGLISH, "%d:%02d %s", hour12, m, amPm);
//            target.setText(formattedTime);
//
//            if (isCheckIn) {
//                session.checkInTime = formattedTime;
//            } else {
//                session.checkOutTime = formattedTime;
//            }
//
//            // ✅ Update session duration
//            String durationText = calculateSessionDuration(session);
//            tvDuration.setText(durationText);
//
//            // Update total hours
//            calculateTotalHours();
//
//        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
//    }
//    private String calculateSessionDuration(SessionData session) {
//        if (session.checkInTime == null || session.checkInTime.isEmpty() ||
//                session.checkOutTime == null || session.checkOutTime.isEmpty()) {
//            return "0h 0m";
//        }
//
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//            Date inTime = sdf.parse(session.checkInTime);
//            Date outTime = sdf.parse(session.checkOutTime);
//
//            if (inTime != null && outTime != null) {
//                long diff = (outTime.getTime() - inTime.getTime()) / 60000; // in minutes
//                if (diff < 0) diff += 24 * 60; // Handle overnight
//
//                long hours = diff / 60;
//                long minutes = diff % 60;
//                return String.format(Locale.ENGLISH, "%dh %dm", hours, minutes);
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Session duration calculation error: " + e.getMessage());
//        }
//
//        return "0h 0m";
//    }
//    private void updateSessionNumbers() {
//        for (int i = 0; i < sessions.size(); i++) {
//            SessionData session = sessions.get(i);
//            if (session.sessionView != null) {
//                TextView tvSessionNumber = session.sessionView.findViewById(R.id.tvSessionNumber);
//                tvSessionNumber.setText("Session " + (i + 1));
//            }
//        }
//    }
//
//    private void openImageInGallery(String imageUrl) {
//        try {
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setDataAndType(Uri.parse(imageUrl), "image/*");
//            startActivity(intent);
//        } catch (Exception e) {
//            Toast.makeText(this, "Cannot open image", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void calculateTotalHours() {
//        long totalMinutes = 0;
//        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//
//        for (SessionData session : sessions) {
//            if (session.checkInTime != null && !session.checkInTime.isEmpty() &&
//                    session.checkOutTime != null && !session.checkOutTime.isEmpty()) {
//                try {
//                    Date inTime = sdf.parse(session.checkInTime);
//                    Date outTime = sdf.parse(session.checkOutTime);
//
//                    if (inTime != null && outTime != null) {
//                        long diff = (outTime.getTime() - inTime.getTime()) / 60000;
//                        if (diff < 0) diff += 24 * 60;
//                        totalMinutes += diff;
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, "Time calculation error: " + e.getMessage());
//                }
//            }
//        }
//
//        long hours = totalMinutes / 60;
//        long minutes = totalMinutes % 60;
//        tvTotalHours.setText(String.format(Locale.ENGLISH, "%dh %dm", hours, minutes));
//    }
//    private void saveAttendance() {
//        if (selectedStatus.isEmpty()) {
//            Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        try {
//            // Calculate total minutes from complete pairs only
//            long totalMinutes = 0;
//            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//
//            for (SessionData session : sessions) {
//                if (session.checkInTime != null && !session.checkInTime.isEmpty() &&
//                        session.checkOutTime != null && !session.checkOutTime.isEmpty()) {
//                    Date inTime = sdf.parse(session.checkInTime);
//                    Date outTime = sdf.parse(session.checkOutTime);
//
//                    if (inTime != null && outTime != null) {
//                        long diff = (outTime.getTime() - inTime.getTime()) / 60000;
//                        if (diff < 0) diff += 24 * 60;
//                        totalMinutes += diff;
//                    }
//                }
//            }
//
//            // Prepare main attendance data
//            Map<String, Object> map = new HashMap<>();
//
//            // Set first and last times from sessions
//            if (!sessions.isEmpty()) {
//                SessionData firstSession = sessions.get(0);
//                SessionData lastSession = sessions.get(sessions.size() - 1);
//
//                map.put("checkInTime", firstSession.checkInTime);
//                map.put("checkInPhoto", firstSession.checkInPhoto != null ? firstSession.checkInPhoto : "");
//                map.put("checkInAddress", firstSession.checkInAddress != null ? firstSession.checkInAddress : "");
//                map.put("checkInLat", firstSession.checkInLat);
//                map.put("checkInLng", firstSession.checkInLng);
//
//                map.put("checkOutTime", lastSession.checkOutTime);
//                map.put("checkOutPhoto", lastSession.checkOutPhoto != null ? lastSession.checkOutPhoto : "");
//                map.put("checkOutAddress", lastSession.checkOutAddress != null ? lastSession.checkOutAddress : "");
//                map.put("checkOutLat", lastSession.checkOutLat);
//                map.put("checkOutLng", lastSession.checkOutLng);
//            }
//
//            map.put("status", selectedStatus);
//            map.put("finalStatus", selectedStatus);
//            map.put("lateStatus", selectedLateStatus);
//            map.put("totalMinutes", totalMinutes);
//            map.put("totalHours", String.format("%.1f", totalMinutes / 60.0));
//            map.put("markedBy", "Admin");
//            map.put("lastModified", System.currentTimeMillis());
//
//            btnSave.setEnabled(false);
//
//            // Save main data first
//            attendanceRef.updateChildren(map).addOnSuccessListener(a -> {
//                // Now save all pairs (including check-in only sessions)
//                savePairs();
//            }).addOnFailureListener(e -> {
//                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
//                btnSave.setEnabled(true);
//            });
//
//        } catch (Exception e) {
//            Log.e(TAG, "Save error", e);
//            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void savePairs() {
//        DatabaseReference pairsRef = attendanceRef.child("checkInOutPairs");
//
//        // Clear existing pairs
//        pairsRef.removeValue().addOnSuccessListener(aVoid -> {
//            // Save all sessions
//            int pairNumber = 0;
//
//            for (SessionData session : sessions) {
//                // ✅ Save ALL sessions - both complete and incomplete
//                // Only requirement: must have check-in time
//                if (session.checkInTime != null && !session.checkInTime.isEmpty()) {
//                    pairNumber++;
//
//                    Map<String, Object> pairData = new HashMap<>();
//                    pairData.put("checkInTime", session.checkInTime);
//                    pairData.put("checkOutTime", session.checkOutTime != null ? session.checkOutTime : "");
//                    pairData.put("checkInPhoto", session.checkInPhoto != null ? session.checkInPhoto : "");
//                    pairData.put("checkOutPhoto", session.checkOutPhoto != null ? session.checkOutPhoto : "");
//                    pairData.put("checkInAddress", session.checkInAddress != null ? session.checkInAddress : "");
//                    pairData.put("checkOutAddress", session.checkOutAddress != null ? session.checkOutAddress : "");
//                    pairData.put("checkInLat", session.checkInLat);
//                    pairData.put("checkInLng", session.checkInLng);
//                    pairData.put("checkOutLat", session.checkOutLat);
//                    pairData.put("checkOutLng", session.checkOutLng);
//                    pairData.put("checkInGPS", session.checkInGPS);
//                    pairData.put("checkOutGPS", session.checkOutGPS);
//
//                    // Calculate duration if both times exist (complete pair)
//                    if (session.checkInTime != null && !session.checkInTime.isEmpty() &&
//                            session.checkOutTime != null && !session.checkOutTime.isEmpty()) {
//                        long duration = calculateDuration(session.checkInTime, session.checkOutTime);
//                        pairData.put("durationMinutes", duration);
//                    } else {
//                        // Incomplete session (check-in only)
//                        pairData.put("durationMinutes", 0);
//                    }
//
//                    pairsRef.child("pair_" + pairNumber).setValue(pairData);
//
//                    Log.d(TAG, "✅ Saved pair_" + pairNumber + ": " +
//                            session.checkInTime + " → " + session.checkOutTime);
//                }
//            }
//
//            Log.d(TAG, "========================================");
//            Log.d(TAG, "Total pairs saved: " + pairNumber);
//            Log.d(TAG, "========================================");
//
//            Toast.makeText(this, "Attendance saved successfully", Toast.LENGTH_SHORT).show();
//            finish();
//
//        }).addOnFailureListener(e -> {
//            Toast.makeText(this, "Failed to save pairs", Toast.LENGTH_SHORT).show();
//            btnSave.setEnabled(true);
//        });
//    }
//
//    private long calculateDuration(String checkIn, String checkOut) {
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//            Date inTime = sdf.parse(checkIn);
//            Date outTime = sdf.parse(checkOut);
//
//            if (inTime != null && outTime != null) {
//                long diff = (outTime.getTime() - inTime.getTime()) / 60000;
//                if (diff < 0) diff += 24 * 60;
//                return diff;
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Duration calculation error: " + e.getMessage());
//        }
//        return 0;
//    }
//
//    private void showDeletePopup() {
//        new AlertDialog.Builder(this)
//                .setTitle("Delete Attendance")
//                .setMessage("Are you sure you want to delete this attendance record? This will delete all sessions.")
//                .setPositiveButton("Delete", (d, w) -> deleteAttendance())
//                .setNegativeButton("Cancel", null)
//                .show();
//    }
//
//    private void deleteAttendance() {
//        btnDelete.setEnabled(false);
//
//        attendanceRef.removeValue()
//                .addOnSuccessListener(a -> {
//                    Toast.makeText(this, "Attendance deleted successfully", Toast.LENGTH_SHORT).show();
//                    finish();
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "Failed to delete attendance", Toast.LENGTH_SHORT).show();
//                    btnDelete.setEnabled(true);
//                });
//    }
//}







package com.sandhyyasofttech.attendsmart.Activities;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminDayAttendanceDetailActivity extends AppCompatActivity {

    private static final String TAG = "AdminAttendanceDetail";

    private MaterialToolbar toolbar;
    private TextView tvDate, tvTotalHours, tvShiftTiming;
    private LinearLayout sessionsContainer;
    private MaterialButton btnSave, btnDelete, btnAddSession;

    // Status buttons
    private MaterialButton btnStatusPresent, btnStatusHalfDay, btnStatusAbsent, btnStatusHoliday;
    private MaterialButton btnLateOnTime, btnLateLate;

    private String companyKey, employeeMobile, date;
    private DatabaseReference attendanceRef, employeeRef, companyRef;

    private String employeeShift = "";
    private String shiftStartTime = "";
    private String shiftEndTime = "";

    // Selected status
    private String selectedStatus = "";
    private String selectedLateStatus = "";

    // Store all sessions
    private List<SessionData> sessions = new ArrayList<>();

    // Helper class to store session data
    private static class SessionData {
        String checkInTime = "";
        String checkOutTime = "";
        String checkInPhoto = "";
        String checkOutPhoto = "";
        String checkInAddress = "";
        String checkOutAddress = "";
        double checkInLat, checkInLng;
        double checkOutLat, checkOutLng;
        boolean checkInGPS, checkOutGPS;
        View sessionView; // Reference to the UI view
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_day_attendance_detail);

        companyKey = getIntent().getStringExtra("companyKey");
        employeeMobile = getIntent().getStringExtra("employeeMobile");
        date = getIntent().getStringExtra("date");

        Log.d(TAG, "CompanyKey: " + companyKey);
        Log.d(TAG, "EmployeeMobile: " + employeeMobile);
        Log.d(TAG, "Date: " + date);

        initViews();
        setupToolbar();
        setupFirebase();
        loadEmployeeData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvDate = findViewById(R.id.tvDate);
        tvTotalHours = findViewById(R.id.tvTotalHours);
        tvShiftTiming = findViewById(R.id.tvShiftTiming);
        sessionsContainer = findViewById(R.id.sessionsContainer);

        // Status buttons
        btnStatusPresent = findViewById(R.id.btnStatusPresent);
        btnStatusHalfDay = findViewById(R.id.btnStatusHalfDay);
        btnStatusAbsent = findViewById(R.id.btnStatusAbsent);
        btnStatusHoliday = findViewById(R.id.btnStatusHoliday); // NEW

        btnLateOnTime = findViewById(R.id.btnLateOnTime);
        btnLateLate = findViewById(R.id.btnLateLate);

        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnAddSession = findViewById(R.id.btnAddSession);

        formatAndDisplayDate();
        setupStatusButtons();

        btnAddSession.setOnClickListener(v -> addNewSession());
        btnSave.setOnClickListener(v -> saveAttendance());
        btnDelete.setOnClickListener(v -> showDeletePopup());
    }

    private void setupStatusButtons() {
        btnStatusPresent.setOnClickListener(v -> selectStatus("Present"));
        btnStatusHalfDay.setOnClickListener(v -> selectStatus("Half Day"));
        btnStatusAbsent.setOnClickListener(v -> selectStatus("Absent"));
        btnStatusHoliday.setOnClickListener(v -> selectStatus("Holiday")); // NEW

        btnLateOnTime.setOnClickListener(v -> selectLateStatus("On Time"));
        btnLateLate.setOnClickListener(v -> selectLateStatus("Late"));
    }

    private void selectStatus(String status) {
        selectedStatus = status;

        // Reset all buttons
        resetStatusButton(btnStatusPresent);
        resetStatusButton(btnStatusHalfDay);
        resetStatusButton(btnStatusAbsent);
        resetStatusButton(btnStatusHoliday); // NEW

        MaterialButton selectedBtn = null;
        int color = 0;

        switch (status) {
            case "Present":
                selectedBtn = btnStatusPresent;
                color = 0xFF43A047; // Green
                break;
            case "Half Day":
                selectedBtn = btnStatusHalfDay;
                color = 0xFFFF9800; // Orange
                break;
            case "Absent":
                selectedBtn = btnStatusAbsent;
                color = 0xFFE53935; // Red
                break;
            case "Holiday":
                selectedBtn = btnStatusHoliday;
                color = 0xFF2196F3; // Blue - NEW
                break;
        }

        if (selectedBtn != null) {
            selectedBtn.setBackgroundColor(color);
            selectedBtn.setTextColor(0xFFFFFFFF);
            selectedBtn.setStrokeWidth(0);
        }
    }
    private void selectLateStatus(String lateStatus) {
        selectedLateStatus = lateStatus;

        resetStatusButton(btnLateOnTime);
        resetStatusButton(btnLateLate);

        MaterialButton selectedBtn = null;
        int color = 0;

        if ("On Time".equals(lateStatus)) {
            selectedBtn = btnLateOnTime;
            color = 0xFF43A047;
        } else if ("Late".equals(lateStatus)) {
            selectedBtn = btnLateLate;
            color = 0xFFE53935;
        }

        if (selectedBtn != null) {
            selectedBtn.setBackgroundColor(color);
            selectedBtn.setTextColor(0xFFFFFFFF);
            selectedBtn.setStrokeWidth(0);
        }
    }

    private void resetStatusButton(MaterialButton button) {
        button.setBackgroundColor(0xFFFFFFFF);
        button.setTextColor(0xFF757575);
        button.setStrokeColorResource(android.R.color.darker_gray);
        button.setStrokeWidth(4);
    }

    private void formatAndDisplayDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH);
            Date dateObj = inputFormat.parse(date);
            if (dateObj != null) {
                tvDate.setText(outputFormat.format(dateObj));
            } else {
                tvDate.setText(date);
            }
        } catch (Exception e) {
            tvDate.setText(date);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Attendance Details");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupFirebase() {
        companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey);

        attendanceRef = companyRef
                .child("attendance")
                .child(date)
                .child(employeeMobile);

        employeeRef = companyRef
                .child("employees")
                .child(employeeMobile)
                .child("info");
    }

    private void loadEmployeeData() {
        Log.d(TAG, "Loading employee data...");

        employeeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    employeeShift = snapshot.child("employeeShift").getValue(String.class);
                    Log.d(TAG, "Employee shift: " + employeeShift);

                    if (employeeShift != null && !employeeShift.isEmpty()) {
                        loadShiftDetails(employeeShift);
                    } else {
                        tvShiftTiming.setText("No shift assigned");
                        loadAttendance();
                    }
                } else {
                    tvShiftTiming.setText("Employee not found");
                    loadAttendance();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading employee: " + error.getMessage());
                loadAttendance();
            }
        });
    }

    private void loadShiftDetails(String shiftName) {
        DatabaseReference shiftRef = companyRef.child("shifts").child(shiftName);

        shiftRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    shiftStartTime = snapshot.child("startTime").getValue(String.class);
                    shiftEndTime = snapshot.child("endTime").getValue(String.class);

                    if (shiftStartTime != null && shiftEndTime != null) {
                        tvShiftTiming.setText(shiftStartTime + " - " + shiftEndTime);
                    } else {
                        tvShiftTiming.setText(shiftName + " Shift");
                    }
                } else {
                    tvShiftTiming.setText("Shift not found");
                }
                loadAttendance();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading shift: " + error.getMessage());
                loadAttendance();
            }
        });
    }

    private void loadAttendance() {
        Log.d(TAG, "Loading attendance...");

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                sessions.clear();
                sessionsContainer.removeAllViews();

                if (!s.exists()) {
                    selectStatus("Absent");
                    selectLateStatus("On Time");
                    tvTotalHours.setText("0h 0m");
                    addNewSession();
                    return;
                }

                // Load status and late status
                selectStatus(s.child("status").getValue(String.class) != null ?
                        s.child("status").getValue(String.class) : "Present");
                selectLateStatus(s.child("lateStatus").getValue(String.class) != null ?
                        s.child("lateStatus").getValue(String.class) : "On Time");

                // ✅ STEP 1: Check checkInOutPairs
                DataSnapshot pairsSnapshot = s.child("checkInOutPairs");
                boolean hasPairs = pairsSnapshot.exists() && pairsSnapshot.getChildrenCount() > 0;

                if (hasPairs) {
                    // ✅ Load from pairs structure
                    List<SessionData> allPairData = new ArrayList<>();

                    for (DataSnapshot pairSnap : pairsSnapshot.getChildren()) {
                        SessionData pairData = new SessionData();
                        pairData.checkInTime = pairSnap.child("checkInTime").getValue(String.class);
                        pairData.checkOutTime = pairSnap.child("checkOutTime").getValue(String.class);
                        pairData.checkInPhoto = pairSnap.child("checkInPhoto").getValue(String.class);
                        pairData.checkOutPhoto = pairSnap.child("checkOutPhoto").getValue(String.class);
                        pairData.checkInAddress = pairSnap.child("checkInAddress").getValue(String.class);
                        pairData.checkOutAddress = pairSnap.child("checkOutAddress").getValue(String.class);

                        // Load GPS data
                        Double lat = pairSnap.child("checkInLat").getValue(Double.class);
                        pairData.checkInLat = lat != null ? lat : 0.0;
                        Double lng = pairSnap.child("checkInLng").getValue(Double.class);
                        pairData.checkInLng = lng != null ? lng : 0.0;

                        lat = pairSnap.child("checkOutLat").getValue(Double.class);
                        pairData.checkOutLat = lat != null ? lat : 0.0;
                        lng = pairSnap.child("checkOutLng").getValue(Double.class);
                        pairData.checkOutLng = lng != null ? lng : 0.0;

                        Boolean gps = pairSnap.child("checkInGPS").getValue(Boolean.class);
                        pairData.checkInGPS = gps != null ? gps : false;
                        gps = pairSnap.child("checkOutGPS").getValue(Boolean.class);
                        pairData.checkOutGPS = gps != null ? gps : false;

                        allPairData.add(pairData);
                    }

                    // ✅ STEP 2: Match and merge duplicate check-ins
                    List<SessionData> mergedPairs = new ArrayList<>();

                    for (SessionData pairData : allPairData) {
                        if (pairData.checkInTime == null || pairData.checkInTime.isEmpty()) {
                            continue;
                        }

                        // Check if this check-in already exists
                        boolean alreadyMerged = false;
                        for (SessionData merged : mergedPairs) {
                            if (pairData.checkInTime.equals(merged.checkInTime)) {
                                alreadyMerged = true;
                                // If current pair has check-out but merged doesn't, update it
                                if ((merged.checkOutTime == null || merged.checkOutTime.isEmpty()) &&
                                        pairData.checkOutTime != null && !pairData.checkOutTime.isEmpty()) {
                                    merged.checkOutTime = pairData.checkOutTime;
                                    merged.checkOutPhoto = pairData.checkOutPhoto;
                                    merged.checkOutAddress = pairData.checkOutAddress;
                                    merged.checkOutLat = pairData.checkOutLat;
                                    merged.checkOutLng = pairData.checkOutLng;
                                    merged.checkOutGPS = pairData.checkOutGPS;
                                }
                                break;
                            }
                        }

                        if (!alreadyMerged) {
                            mergedPairs.add(pairData);
                        }
                    }

                    // ✅ STEP 3: Add ALL pairs (complete AND incomplete)
                    for (SessionData pairData : mergedPairs) {
                        if (pairData.checkInTime != null && !pairData.checkInTime.isEmpty()) {
                            sessions.add(pairData);
                            addSessionView(pairData);
                        }
                    }
                }

                // ✅ FALLBACK: Check main fields
                if (sessions.isEmpty()) {
                    String checkInTime = s.child("checkInTime").getValue(String.class);
                    String checkOutTime = s.child("checkOutTime").getValue(String.class);

                    if (checkInTime != null && !checkInTime.isEmpty()) {
                        SessionData session = new SessionData();
                        session.checkInTime = checkInTime;
                        session.checkOutTime = checkOutTime;
                        session.checkInPhoto = s.child("checkInPhoto").getValue(String.class);
                        session.checkOutPhoto = s.child("checkOutPhoto").getValue(String.class);
                        session.checkInAddress = s.child("checkInAddress").getValue(String.class);
                        session.checkOutAddress = s.child("checkOutAddress").getValue(String.class);

                        // Load GPS data
                        Double lat = s.child("checkInLat").getValue(Double.class);
                        session.checkInLat = lat != null ? lat : 0.0;
                        Double lng = s.child("checkInLng").getValue(Double.class);
                        session.checkInLng = lng != null ? lng : 0.0;

                        lat = s.child("checkOutLat").getValue(Double.class);
                        session.checkOutLat = lat != null ? lat : 0.0;
                        lng = s.child("checkOutLng").getValue(Double.class);
                        session.checkOutLng = lng != null ? lng : 0.0;

                        Boolean gps = s.child("checkInGPS").getValue(Boolean.class);
                        session.checkInGPS = gps != null ? gps : false;
                        gps = s.child("checkOutGPS").getValue(Boolean.class);
                        session.checkOutGPS = gps != null ? gps : false;

                        sessions.add(session);
                        addSessionView(session);
                    }
                }

                // If no sessions, add empty one
                if (sessions.isEmpty()) {
                    addNewSession();
                }

                calculateTotalHours();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.e(TAG, "Error loading attendance: " + e.getMessage());
            }
        });
    }

    private void addNewSession() {
        SessionData session = new SessionData();
        sessions.add(session);
        addSessionView(session);
        calculateTotalHours();
    }

    private void addSessionView(SessionData session) {
        View sessionView = LayoutInflater.from(this).inflate(R.layout.item_admin_session, sessionsContainer, false);
        session.sessionView = sessionView;

        TextView tvSessionNumber = sessionView.findViewById(R.id.tvSessionNumber);
        TextInputEditText etCheckIn = sessionView.findViewById(R.id.etCheckIn);
        TextInputEditText etCheckOut = sessionView.findViewById(R.id.etCheckOut);
        TextView tvCheckInAddress = sessionView.findViewById(R.id.tvCheckInAddress);
        TextView tvCheckOutAddress = sessionView.findViewById(R.id.tvCheckOutAddress);
        ImageView ivCheckInPhoto = sessionView.findViewById(R.id.ivCheckInPhoto);
        ImageView ivCheckOutPhoto = sessionView.findViewById(R.id.ivCheckOutPhoto);
        MaterialCardView cardCheckInPhoto = sessionView.findViewById(R.id.cardCheckInPhoto);
        MaterialCardView cardCheckOutPhoto = sessionView.findViewById(R.id.cardCheckOutPhoto);
        MaterialButton btnRemove = sessionView.findViewById(R.id.btnRemoveSession);

        // ✅ ADD THIS: Session duration TextView
        TextView tvSessionDuration = sessionView.findViewById(R.id.tvSessionDuration);

        int sessionNumber = sessions.indexOf(session) + 1;
        tvSessionNumber.setText("Session " + sessionNumber);

        // Set times
        etCheckIn.setText(session.checkInTime);
        etCheckOut.setText(session.checkOutTime);

        // ✅ Calculate and display session duration
        String durationText = calculateSessionDuration(session);
        tvSessionDuration.setText(durationText);

        // Set addresses
        if (session.checkInAddress != null && !session.checkInAddress.isEmpty()) {
            tvCheckInAddress.setText(session.checkInAddress);
        } else {
            tvCheckInAddress.setText("No location");
        }

        if (session.checkOutAddress != null && !session.checkOutAddress.isEmpty()) {
            tvCheckOutAddress.setText(session.checkOutAddress);
        } else {
            tvCheckOutAddress.setText("No location");
        }

        // Load check-in photo
        if (session.checkInPhoto != null && !session.checkInPhoto.isEmpty()) {
            Glide.with(this).load(session.checkInPhoto)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(ivCheckInPhoto);
        } else {
            ivCheckInPhoto.setImageResource(R.drawable.ic_image_placeholder);
        }

        // Load check-out photo (if exists)
        if (session.checkOutPhoto != null && !session.checkOutPhoto.isEmpty()) {
            Glide.with(this).load(session.checkOutPhoto)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(ivCheckOutPhoto);
        } else {
            ivCheckOutPhoto.setImageResource(R.drawable.ic_image_placeholder);
        }

        // Time pickers - update to recalculate duration when time changes
        etCheckIn.setOnClickListener(v -> openTimePicker(etCheckIn, session, true, tvSessionDuration));
        etCheckOut.setOnClickListener(v -> openTimePicker(etCheckOut, session, false, tvSessionDuration));

        // Photo click listeners
        cardCheckInPhoto.setOnClickListener(v -> {
            if (session.checkInPhoto != null && !session.checkInPhoto.isEmpty()) {
                openImageInGallery(session.checkInPhoto);
            } else {
                Toast.makeText(AdminDayAttendanceDetailActivity.this, "No check-in photo available", Toast.LENGTH_SHORT).show();
            }
        });

        cardCheckOutPhoto.setOnClickListener(v -> {
            if (session.checkOutPhoto != null && !session.checkOutPhoto.isEmpty()) {
                openImageInGallery(session.checkOutPhoto);
            } else {
                Toast.makeText(AdminDayAttendanceDetailActivity.this, "No check-out photo available", Toast.LENGTH_SHORT).show();
            }
        });

        // Remove button
        btnRemove.setOnClickListener(v -> {
            if (sessions.size() > 1) {
                sessions.remove(session);
                sessionsContainer.removeView(sessionView);
                updateSessionNumbers();
                calculateTotalHours();
            } else {
                Toast.makeText(this, "At least one session is required", Toast.LENGTH_SHORT).show();
            }
        });

        sessionsContainer.addView(sessionView);
    }

    private void openTimePicker(TextInputEditText target, SessionData session, boolean isCheckIn, TextView tvDuration) {
        Calendar cal = Calendar.getInstance();

        String currentTime = target.getText() != null ? target.getText().toString() : "";
        if (!currentTime.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
                Date date = sdf.parse(currentTime);
                if (date != null) {
                    cal.setTime(date);
                }
            } catch (Exception ignored) {}
        }

        new TimePickerDialog(this, (v, h, m) -> {
            String amPm = h >= 12 ? "PM" : "AM";
            int hour12 = h % 12;
            if (hour12 == 0) hour12 = 12;

            String formattedTime = String.format(Locale.ENGLISH, "%d:%02d %s", hour12, m, amPm);
            target.setText(formattedTime);

            if (isCheckIn) {
                session.checkInTime = formattedTime;
            } else {
                session.checkOutTime = formattedTime;
            }

            // ✅ Update session duration
            String durationText = calculateSessionDuration(session);
            tvDuration.setText(durationText);

            // Update total hours
            calculateTotalHours();

        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }
    private String calculateSessionDuration(SessionData session) {
        if (session.checkInTime == null || session.checkInTime.isEmpty() ||
                session.checkOutTime == null || session.checkOutTime.isEmpty()) {
            return "0h 0m";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date inTime = sdf.parse(session.checkInTime);
            Date outTime = sdf.parse(session.checkOutTime);

            if (inTime != null && outTime != null) {
                long diff = (outTime.getTime() - inTime.getTime()) / 60000; // in minutes
                if (diff < 0) diff += 24 * 60; // Handle overnight

                long hours = diff / 60;
                long minutes = diff % 60;
                return String.format(Locale.ENGLISH, "%dh %dm", hours, minutes);
            }
        } catch (Exception e) {
            Log.e(TAG, "Session duration calculation error: " + e.getMessage());
        }

        return "0h 0m";
    }
    private void updateSessionNumbers() {
        for (int i = 0; i < sessions.size(); i++) {
            SessionData session = sessions.get(i);
            if (session.sessionView != null) {
                TextView tvSessionNumber = session.sessionView.findViewById(R.id.tvSessionNumber);
                tvSessionNumber.setText("Session " + (i + 1));
            }
        }
    }

    private void openImageInGallery(String imageUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(imageUrl), "image/*");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open image", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateTotalHours() {
        long totalMinutes = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);

        for (SessionData session : sessions) {
            if (session.checkInTime != null && !session.checkInTime.isEmpty() &&
                    session.checkOutTime != null && !session.checkOutTime.isEmpty()) {
                try {
                    Date inTime = sdf.parse(session.checkInTime);
                    Date outTime = sdf.parse(session.checkOutTime);

                    if (inTime != null && outTime != null) {
                        long diff = (outTime.getTime() - inTime.getTime()) / 60000;
                        if (diff < 0) diff += 24 * 60;
                        totalMinutes += diff;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Time calculation error: " + e.getMessage());
                }
            }
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        tvTotalHours.setText(String.format(Locale.ENGLISH, "%dh %dm", hours, minutes));
    }
    private void saveAttendance() {
        if (selectedStatus.isEmpty()) {
            Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Calculate total minutes from complete pairs only
            long totalMinutes = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);

            for (SessionData session : sessions) {
                if (session.checkInTime != null && !session.checkInTime.isEmpty() &&
                        session.checkOutTime != null && !session.checkOutTime.isEmpty()) {
                    Date inTime = sdf.parse(session.checkInTime);
                    Date outTime = sdf.parse(session.checkOutTime);

                    if (inTime != null && outTime != null) {
                        long diff = (outTime.getTime() - inTime.getTime()) / 60000;
                        if (diff < 0) diff += 24 * 60;
                        totalMinutes += diff;
                    }
                }
            }

            // Prepare main attendance data
            Map<String, Object> map = new HashMap<>();

            // Set first and last times from sessions
            if (!sessions.isEmpty()) {
                SessionData firstSession = sessions.get(0);
                SessionData lastSession = sessions.get(sessions.size() - 1);

                map.put("checkInTime", firstSession.checkInTime);
                map.put("checkInPhoto", firstSession.checkInPhoto != null ? firstSession.checkInPhoto : "");
                map.put("checkInAddress", firstSession.checkInAddress != null ? firstSession.checkInAddress : "");
                map.put("checkInLat", firstSession.checkInLat);
                map.put("checkInLng", firstSession.checkInLng);

                map.put("checkOutTime", lastSession.checkOutTime);
                map.put("checkOutPhoto", lastSession.checkOutPhoto != null ? lastSession.checkOutPhoto : "");
                map.put("checkOutAddress", lastSession.checkOutAddress != null ? lastSession.checkOutAddress : "");
                map.put("checkOutLat", lastSession.checkOutLat);
                map.put("checkOutLng", lastSession.checkOutLng);
            }

            map.put("status", selectedStatus);
            map.put("finalStatus", selectedStatus);
            map.put("lateStatus", selectedLateStatus);
            map.put("totalMinutes", totalMinutes);
            map.put("totalHours", String.format("%.1f", totalMinutes / 60.0));
            map.put("markedBy", "Admin");
            map.put("lastModified", System.currentTimeMillis());

            btnSave.setEnabled(false);

            // Save main data first
            attendanceRef.updateChildren(map).addOnSuccessListener(a -> {
                // Now save all pairs (including check-in only sessions)
                savePairs();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
            });

        } catch (Exception e) {
            Log.e(TAG, "Save error", e);
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePairs() {
        DatabaseReference pairsRef = attendanceRef.child("checkInOutPairs");

        // Clear existing pairs
        pairsRef.removeValue().addOnSuccessListener(aVoid -> {
            // Save all sessions
            int pairNumber = 0;

            for (SessionData session : sessions) {
                // ✅ Save ALL sessions - both complete and incomplete
                // Only requirement: must have check-in time
                if (session.checkInTime != null && !session.checkInTime.isEmpty()) {
                    pairNumber++;

                    Map<String, Object> pairData = new HashMap<>();
                    pairData.put("checkInTime", session.checkInTime);
                    pairData.put("checkOutTime", session.checkOutTime != null ? session.checkOutTime : "");
                    pairData.put("checkInPhoto", session.checkInPhoto != null ? session.checkInPhoto : "");
                    pairData.put("checkOutPhoto", session.checkOutPhoto != null ? session.checkOutPhoto : "");
                    pairData.put("checkInAddress", session.checkInAddress != null ? session.checkInAddress : "");
                    pairData.put("checkOutAddress", session.checkOutAddress != null ? session.checkOutAddress : "");
                    pairData.put("checkInLat", session.checkInLat);
                    pairData.put("checkInLng", session.checkInLng);
                    pairData.put("checkOutLat", session.checkOutLat);
                    pairData.put("checkOutLng", session.checkOutLng);
                    pairData.put("checkInGPS", session.checkInGPS);
                    pairData.put("checkOutGPS", session.checkOutGPS);

                    // Calculate duration if both times exist (complete pair)
                    if (session.checkInTime != null && !session.checkInTime.isEmpty() &&
                            session.checkOutTime != null && !session.checkOutTime.isEmpty()) {
                        long duration = calculateDuration(session.checkInTime, session.checkOutTime);
                        pairData.put("durationMinutes", duration);
                    } else {
                        // Incomplete session (check-in only)
                        pairData.put("durationMinutes", 0);
                    }

                    pairsRef.child("pair_" + pairNumber).setValue(pairData);

                    Log.d(TAG, "✅ Saved pair_" + pairNumber + ": " +
                            session.checkInTime + " → " + session.checkOutTime);
                }
            }

            Log.d(TAG, "========================================");
            Log.d(TAG, "Total pairs saved: " + pairNumber);
            Log.d(TAG, "========================================");

            Toast.makeText(this, "Attendance saved successfully", Toast.LENGTH_SHORT).show();
            finish();

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to save pairs", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
        });
    }

    private long calculateDuration(String checkIn, String checkOut) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date inTime = sdf.parse(checkIn);
            Date outTime = sdf.parse(checkOut);

            if (inTime != null && outTime != null) {
                long diff = (outTime.getTime() - inTime.getTime()) / 60000;
                if (diff < 0) diff += 24 * 60;
                return diff;
            }
        } catch (Exception e) {
            Log.e(TAG, "Duration calculation error: " + e.getMessage());
        }
        return 0;
    }

    private void showDeletePopup() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Attendance")
                .setMessage("Are you sure you want to delete this attendance record? This will delete all sessions.")
                .setPositiveButton("Delete", (d, w) -> deleteAttendance())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAttendance() {
        btnDelete.setEnabled(false);

        attendanceRef.removeValue()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Attendance deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete attendance", Toast.LENGTH_SHORT).show();
                    btnDelete.setEnabled(true);
                });
    }
}