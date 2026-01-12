package com.sandhyyasofttech.attendsmart.Activities;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.R;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdminDayAttendanceDetailActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceDetail";

    private MaterialToolbar toolbar;
    private TextView tvDate, tvTotalHours, tvShiftTiming;
    private TextView tvCheckInAddress, tvCheckOutAddress;
    private TextInputEditText etCheckIn, etCheckOut;
    private ImageView ivCheckInPhoto, ivCheckOutPhoto;
    private MaterialButton btnSave, btnDelete;

    // Status buttons
    private MaterialButton btnStatusPresent, btnStatusHalfDay, btnStatusAbsent;
    private MaterialButton btnLateOnTime, btnLateLate;

    // Photo cards
    private MaterialCardView cardCheckInPhoto, cardCheckOutPhoto;

    private String companyKey, employeeMobile, date;
    private DatabaseReference attendanceRef, employeeRef, companyRef;

    private String employeeShift = "";
    private String shiftStartTime = "";
    private String shiftEndTime = "";

    // Selected status
    private String selectedStatus = "";
    private String selectedLateStatus = "";

    // Photo URLs
    private String checkInPhotoUrl = "";
    private String checkOutPhotoUrl = "";

    // Store original data for comparison
    private String originalCheckIn = "";
    private String originalCheckOut = "";
    private String originalStatus = "";
    private String originalLateStatus = "";

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
        loadEmployeeData(); // Load employee data first
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        tvDate = findViewById(R.id.tvDate);
        tvTotalHours = findViewById(R.id.tvTotalHours);
        tvShiftTiming = findViewById(R.id.tvShiftTiming);
        tvCheckInAddress = findViewById(R.id.tvCheckInAddress);
        tvCheckOutAddress = findViewById(R.id.tvCheckOutAddress);

        etCheckIn = findViewById(R.id.etCheckIn);
        etCheckOut = findViewById(R.id.etCheckOut);

        // Status buttons
        btnStatusPresent = findViewById(R.id.btnStatusPresent);
        btnStatusHalfDay = findViewById(R.id.btnStatusHalfDay);
        btnStatusAbsent = findViewById(R.id.btnStatusAbsent);

        // Late status buttons
        btnLateOnTime = findViewById(R.id.btnLateOnTime);
        btnLateLate = findViewById(R.id.btnLateLate);

        // Photo cards
        cardCheckInPhoto = findViewById(R.id.cardCheckInPhoto);
        cardCheckOutPhoto = findViewById(R.id.cardCheckOutPhoto);

        ivCheckInPhoto = findViewById(R.id.ivCheckInPhoto);
        ivCheckOutPhoto = findViewById(R.id.ivCheckOutPhoto);

        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        // Format and display date
        formatAndDisplayDate();

        // Setup status button listeners
        setupStatusButtons();

        // Set click listeners
        etCheckIn.setOnClickListener(v -> openTimePicker(etCheckIn));
        etCheckOut.setOnClickListener(v -> openTimePicker(etCheckOut));

        // Image click listeners for gallery view
        cardCheckInPhoto.setOnClickListener(v -> {
            if (checkInPhotoUrl != null && !checkInPhotoUrl.isEmpty()) {
                openImageInGallery(checkInPhotoUrl);
            }
        });

        cardCheckOutPhoto.setOnClickListener(v -> {
            if (checkOutPhotoUrl != null && !checkOutPhotoUrl.isEmpty()) {
                openImageInGallery(checkOutPhotoUrl);
            }
        });

        btnSave.setOnClickListener(v -> saveAttendance());
        btnDelete.setOnClickListener(v -> showDeletePopup());
    }

    private void setupStatusButtons() {
        // Attendance status buttons
        btnStatusPresent.setOnClickListener(v -> selectStatus("Present"));
        btnStatusHalfDay.setOnClickListener(v -> selectStatus("Half Day"));
        btnStatusAbsent.setOnClickListener(v -> selectStatus("Absent"));

        // Punctuality status buttons
        btnLateOnTime.setOnClickListener(v -> selectLateStatus("On Time"));
        btnLateLate.setOnClickListener(v -> selectLateStatus("Late"));
    }

    private void selectStatus(String status) {
        selectedStatus = status;

        // Reset all buttons
        resetStatusButton(btnStatusPresent);
        resetStatusButton(btnStatusHalfDay);
        resetStatusButton(btnStatusAbsent);

        // Highlight selected button
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
        }

        if (selectedBtn != null) {
            selectedBtn.setBackgroundColor(color);
            selectedBtn.setTextColor(0xFFFFFFFF);
            selectedBtn.setStrokeWidth(0);
        }
    }

    private void selectLateStatus(String lateStatus) {
        selectedLateStatus = lateStatus;

        // Reset all buttons
        resetStatusButton(btnLateOnTime);
        resetStatusButton(btnLateLate);

        // Highlight selected button
        MaterialButton selectedBtn = null;
        int color = 0;

        if ("On Time".equals(lateStatus)) {
            selectedBtn = btnLateOnTime;
            color = 0xFF43A047; // Green
        } else if ("Late".equals(lateStatus)) {
            selectedBtn = btnLateLate;
            color = 0xFFE53935; // Red
        }

        if (selectedBtn != null) {
            selectedBtn.setBackgroundColor(color);
            selectedBtn.setTextColor(0xFFFFFFFF);
            selectedBtn.setStrokeWidth(0);
        }
    }

    private void resetStatusButton(MaterialButton button) {
        button.setBackgroundColor(0xFFFFFFFF); // White
        button.setTextColor(0xFF757575); // Gray
        button.setStrokeColorResource(android.R.color.darker_gray);
        button.setStrokeWidth(4); // 1.5dp approximation
    }

    private void openImageInGallery(String imageUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(imageUrl), "image/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            // If no app can handle it, try browser
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
                startActivity(browserIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "Cannot open image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void formatAndDisplayDate() {
        try {
            // Your date is in format: yyyy-MM-dd (2026-01-06)
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH);
            Date dateObj = inputFormat.parse(date);
            if (dateObj != null) {
                tvDate.setText(outputFormat.format(dateObj));
            } else {
                tvDate.setText(date);
            }
        } catch (Exception e) {
            // Fallback: try dd-MM-yyyy format
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
                SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH);
                Date dateObj = inputFormat.parse(date);
                if (dateObj != null) {
                    tvDate.setText(outputFormat.format(dateObj));
                } else {
                    tvDate.setText(date);
                }
            } catch (Exception ex) {
                tvDate.setText(date);
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Attendance Details");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        String currentCheckIn = etCheckIn.getText() != null ? etCheckIn.getText().toString().trim() : "";
        String currentCheckOut = etCheckOut.getText() != null ? etCheckOut.getText().toString().trim() : "";

        return !currentCheckIn.equals(originalCheckIn) ||
                !currentCheckOut.equals(originalCheckOut) ||
                !selectedStatus.equals(originalStatus) ||
                !selectedLateStatus.equals(originalLateStatus);
    }

    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save before leaving?")
                .setPositiveButton("Save", (d, w) -> saveAttendance())
                .setNegativeButton("Discard", (d, w) -> finish())
                .setNeutralButton("Cancel", null)
                .show();
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

        Log.d(TAG, "Company Path: " + companyRef.toString());
        Log.d(TAG, "Attendance Path: " + attendanceRef.toString());
        Log.d(TAG, "Employee Path: " + employeeRef.toString());
    }

    // Load employee data and shift - FIXED TO MATCH YOUR FIREBASE STRUCTURE
    private void loadEmployeeData() {
        Log.d(TAG, "Loading employee data...");

        employeeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Employee snapshot exists: " + snapshot.exists());

                if (snapshot.exists()) {
                    // Log all employee data
                    Log.d(TAG, "=== Employee Data ===");
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Log.d(TAG, child.getKey() + " = " + child.getValue());
                    }

                    // Get employee shift - FIXED FIELD NAME
                    employeeShift = snapshot.child("employeeShift").getValue(String.class);
                    Log.d(TAG, "Employee shift: " + employeeShift);

                    if (employeeShift != null && !employeeShift.isEmpty()) {
                        loadShiftDetails(employeeShift);
                    } else {
                        Log.w(TAG, "No shift assigned to employee");
                        tvShiftTiming.setText("No shift assigned");
                        loadAttendance();
                    }
                } else {
                    Log.e(TAG, "Employee not found at path: " + employeeRef.toString());
                    tvShiftTiming.setText("Employee not found");
                    loadAttendance();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading employee: " + error.getMessage());
                Toast.makeText(AdminDayAttendanceDetailActivity.this,
                        "Failed to load employee data", Toast.LENGTH_SHORT).show();
                loadAttendance();
            }
        });
    }

    // Load shift timing details
    private void loadShiftDetails(String shiftName) {
        Log.d(TAG, "Loading shift details for: " + shiftName);

        DatabaseReference shiftRef = companyRef.child("shifts").child(shiftName);
        Log.d(TAG, "Shift Path: " + shiftRef.toString());

        shiftRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Shift snapshot exists: " + snapshot.exists());

                if (snapshot.exists()) {
                    // Log all shift data
                    Log.d(TAG, "=== Shift Data ===");
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Log.d(TAG, child.getKey() + " = " + child.getValue());
                    }

                    shiftStartTime = snapshot.child("startTime").getValue(String.class);
                    shiftEndTime = snapshot.child("endTime").getValue(String.class);

                    Log.d(TAG, "Start time: " + shiftStartTime);
                    Log.d(TAG, "End time: " + shiftEndTime);

                    // Display shift timing
                    if (shiftStartTime != null && shiftEndTime != null) {
                        tvShiftTiming.setText(shiftName + " Shift: " + shiftStartTime + " - " + shiftEndTime);
                    } else {
                        tvShiftTiming.setText(shiftName + " (Times not available)");
                    }
                } else {
                    Log.e(TAG, "Shift not found at path: " + shiftRef.toString());
                    tvShiftTiming.setText("Shift '" + shiftName + "' not found");
                }
                // Load attendance after shift details
                loadAttendance();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading shift: " + error.getMessage());
                Toast.makeText(AdminDayAttendanceDetailActivity.this,
                        "Failed to load shift details", Toast.LENGTH_SHORT).show();
                loadAttendance();
            }
        });
    }

    private void loadAttendance() {
        Log.d(TAG, "Loading attendance...");

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                Log.d(TAG, "Attendance snapshot exists: " + s.exists());

                if (!s.exists()) {
                    selectStatus("Absent");
                    selectLateStatus("On Time");
                    tvTotalHours.setText("0h 0m");
                    tvCheckInAddress.setText("No check-in recorded");
                    tvCheckOutAddress.setText("No check-out recorded");

                    originalStatus = "Absent";
                    originalLateStatus = "On Time";
                    return;
                }

                // Log all attendance data
                Log.d(TAG, "=== Attendance Data ===");
                for (DataSnapshot child : s.getChildren()) {
                    Log.d(TAG, child.getKey() + " = " + child.getValue());
                }

                // Load check-in time
                String checkIn = s.child("checkInTime").getValue(String.class);
                if (checkIn != null && !checkIn.isEmpty()) {
                    etCheckIn.setText(checkIn);
                    originalCheckIn = checkIn;
                }

                // Load check-out time
                String checkOut = s.child("checkOutTime").getValue(String.class);
                if (checkOut != null && !checkOut.isEmpty()) {
                    etCheckOut.setText(checkOut);
                    originalCheckOut = checkOut;
                }

                // Load status
                String status = s.child("status").getValue(String.class);
                if (status != null) {
                    selectStatus(status);
                    originalStatus = status;
                } else {
                    selectStatus("Present");
                    originalStatus = "Present";
                }

                // Load late status
                String lateStatus = s.child("lateStatus").getValue(String.class);
                if (lateStatus != null) {
                    selectLateStatus(lateStatus);
                    originalLateStatus = lateStatus;
                } else {
                    selectLateStatus("On Time");
                    originalLateStatus = "On Time";
                }

                // Calculate and display times
                if (checkIn != null && !checkIn.isEmpty() && checkOut != null && !checkOut.isEmpty()) {
                    calculateTotalHours();
                }

                // Load addresses - FIXED FIELD NAMES
                String checkInAddress = s.child("checkInAddress").getValue(String.class);
                if (checkInAddress != null && !checkInAddress.isEmpty()) {
                    tvCheckInAddress.setText(checkInAddress);
                } else {
                    tvCheckInAddress.setText("Location not available");
                }

                String checkOutAddress = s.child("checkOutAddress").getValue(String.class);
                if (checkOutAddress != null && !checkOutAddress.isEmpty()) {
                    tvCheckOutAddress.setText(checkOutAddress);
                } else {
                    tvCheckOutAddress.setText("Location not available");
                }

                // Load photos
                String checkInPhoto = s.child("checkInPhoto").getValue(String.class);
                if (checkInPhoto != null && !checkInPhoto.isEmpty()) {
                    checkInPhotoUrl = checkInPhoto; // Store URL
                    Glide.with(AdminDayAttendanceDetailActivity.this)
                            .load(checkInPhoto)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(ivCheckInPhoto);
                }

                String checkOutPhoto = s.child("checkOutPhoto").getValue(String.class);
                if (checkOutPhoto != null && !checkOutPhoto.isEmpty()) {
                    checkOutPhotoUrl = checkOutPhoto; // Store URL
                    Glide.with(AdminDayAttendanceDetailActivity.this)
                            .load(checkOutPhoto)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(ivCheckOutPhoto);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.e(TAG, "Error loading attendance: " + e.getMessage());
                Toast.makeText(AdminDayAttendanceDetailActivity.this,
                        "Failed to load attendance data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openTimePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();

        // If the field already has a time, parse it and set as initial time
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

            // Auto-calculate total hours when both times are set
            calculateTotalHours();

        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private void calculateTotalHours() {
        String in = etCheckIn.getText() != null ? etCheckIn.getText().toString().trim() : "";

        if (in.isEmpty()) {
            tvTotalHours.setText("—");
            return;
        }

        try {
            // Parse "11:00 AM" format correctly
            SimpleDateFormat sdfParse = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date inTime = sdfParse.parse(in);

            if (inTime == null) {
                tvTotalHours.setText("—");
                return;
            }

            Calendar calIn = Calendar.getInstance();
            calIn.setTime(inTime);

            Calendar calNow = Calendar.getInstance();

            // Calculate difference in minutes
            long diffMinutes = 0;

            if (!etCheckOut.getText().toString().trim().isEmpty()) {
                // Both check-in and check-out
                String out = etCheckOut.getText().toString().trim();
                Date outTime = sdfParse.parse(out);

                if (outTime != null) {
                    Calendar calOut = Calendar.getInstance();
                    calOut.setTime(outTime);

                    // Same day assumption (admin edit)
                    diffMinutes = (calOut.getTimeInMillis() - calIn.getTimeInMillis()) / 60000;

                    if (diffMinutes < 0) {
                        // Overnight shift
                        diffMinutes += 24 * 60;
                    }
                }
            } else {
                // Only check-in: live duration to now (today)
                calIn.set(Calendar.YEAR, calNow.get(Calendar.YEAR));
                calIn.set(Calendar.MONTH, calNow.get(Calendar.MONTH));
                calIn.set(Calendar.DAY_OF_MONTH, calNow.get(Calendar.DAY_OF_MONTH));

                diffMinutes = (calNow.getTimeInMillis() - calIn.getTimeInMillis()) / 60000;

                if (diffMinutes < 0) {
                    // If check-in time is in future, show 0
                    diffMinutes = 0;
                }
            }

            // Format display
            long hours = Math.abs(diffMinutes) / 60;
            long minutes = Math.abs(diffMinutes) % 60;

            if (etCheckOut.getText().toString().trim().isEmpty()) {
                tvTotalHours.setText(String.format(Locale.ENGLISH, "%dh %dm (live)", hours, minutes));
            } else {
                tvTotalHours.setText(String.format(Locale.ENGLISH, "%dh %dm", hours, minutes));
            }

        } catch (Exception e) {
            Log.e(TAG, "Time calculation error: " + e.getMessage());
            tvTotalHours.setText("—");
        }
    }

    private void saveAttendance() {
        String in = etCheckIn.getText() != null ? etCheckIn.getText().toString().trim() : "";
        String out = etCheckOut.getText() != null ? etCheckOut.getText().toString().trim() : "";
        String status = selectedStatus;
        String lateStatus = selectedLateStatus;

        if (status.isEmpty()) {
            Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long totalMinutes = 0;
            String totalHoursDisplay = "0h 0m";

            SimpleDateFormat sdfParse = new SimpleDateFormat("h:mm a", Locale.ENGLISH);

            // 🔹 CASE 1: Admin entered check-in & check-out
            if (!in.isEmpty() && !out.isEmpty()) {

                Date inTime = sdfParse.parse(in);
                Date outTime = sdfParse.parse(out);

                if (inTime != null && outTime != null) {
                    long diff = (outTime.getTime() - inTime.getTime()) / 60000;
                    if (diff < 0) diff += 24 * 60;

                    totalMinutes = diff;
                }

            }
            // 🔹 CASE 2: Only check-in (live)
            else if (!in.isEmpty()) {

                Date inTime = sdfParse.parse(in);
                if (inTime != null) {
                    Calendar calIn = Calendar.getInstance();
                    calIn.setTime(inTime);

                    Calendar calNow = Calendar.getInstance();
                    totalMinutes = (calNow.getTimeInMillis() - calIn.getTimeInMillis()) / 60000;
                    if (totalMinutes < 0) totalMinutes = 0;
                }

            }
            // 🔹 CASE 3: Admin manual PRESENT (no times)
            else if ("Present".equals(status)) {

                totalMinutes = getShiftMinutes();

            }
            // 🔹 CASE 4: Admin manual HALF DAY
            else if ("Half Day".equals(status)) {

                totalMinutes = getShiftMinutes() / 2;

            }
            // 🔹 CASE 5: ABSENT
            else {
                totalMinutes = 0;
            }

            // Format hours
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            totalHoursDisplay = hours + "h " + minutes + "m";

            Map<String, Object> map = new HashMap<>();
            map.put("checkInTime", in.isEmpty() ? "" : in);
            map.put("checkOutTime", out.isEmpty() ? "" : out);
            map.put("status", status);
            map.put("finalStatus", status);
            map.put("lateStatus", lateStatus);
            map.put("totalMinutes", totalMinutes);
            map.put("totalHours", totalHoursDisplay);
            map.put("markedBy", "Admin");
            map.put("lastModified", System.currentTimeMillis());

            btnSave.setEnabled(false);

            attendanceRef.setValue(map)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Attendance saved successfully", Toast.LENGTH_SHORT).show();
                        originalCheckIn = in;
                        originalCheckOut = out;
                        originalStatus = status;
                        originalLateStatus = lateStatus;
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Save error", e);
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeletePopup() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Attendance")
                .setMessage("Are you sure you want to delete this attendance record? This action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteAttendance())
                .setNegativeButton("Cancel", null)
                .setCancelable(true)
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

    private long getShiftMinutes() {
        if (shiftStartTime == null || shiftEndTime == null) return 0;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);

            Date start = sdf.parse(shiftStartTime);
            Date end = sdf.parse(shiftEndTime);

            if (start == null || end == null) return 0;

            long diff = (end.getTime() - start.getTime()) / 60000;
            if (diff < 0) diff += 24 * 60; // overnight shift support

            return diff;

        } catch (Exception e) {
            return 0;
        }
    }
}