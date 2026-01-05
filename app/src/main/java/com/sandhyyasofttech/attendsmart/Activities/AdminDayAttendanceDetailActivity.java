//package com.sandhyyasofttech.attendsmart.Activities;
//
//import android.app.AlertDialog;
//import android.app.TimePickerDialog;
//import android.os.Bundle;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.*;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.bumptech.glide.Glide;
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.textfield.TextInputEditText;
//import com.google.firebase.database.*;
//import com.sandhyyasofttech.attendsmart.R;
//
//import java.text.SimpleDateFormat;
//import java.util.*;
//
//public class AdminDayAttendanceDetailActivity extends AppCompatActivity {
//
//    private MaterialToolbar toolbar;
//    private TextView tvDate, tvTotalHours;
//    private TextView tvCheckInAddress, tvCheckOutAddress;
//    private TextInputEditText etCheckIn, etCheckOut;
//    private AutoCompleteTextView spinnerStatus, spinnerLateStatus;
//    private ImageView ivCheckInPhoto, ivCheckOutPhoto;
//    private MaterialButton btnSave, btnDelete;
//
//    private String companyKey, employeeMobile, date;
//    private DatabaseReference attendanceRef;
//
//    // Store original data for comparison
//    private String originalCheckIn = "";
//    private String originalCheckOut = "";
//    private String originalStatus = "";
//    private String originalLateStatus = "";
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
//        initViews();
//        setupToolbar();
//        setupFirebase();
//        setupSpinners();
//        loadAttendance();
//    }
//
//    private void initViews() {
//        toolbar = findViewById(R.id.toolbar);
//
//        tvDate = findViewById(R.id.tvDate);
//        tvTotalHours = findViewById(R.id.tvTotalHours);
//        tvCheckInAddress = findViewById(R.id.tvCheckInAddress);
//        tvCheckOutAddress = findViewById(R.id.tvCheckOutAddress);
//
//        etCheckIn = findViewById(R.id.etCheckIn);
//        etCheckOut = findViewById(R.id.etCheckOut);
//
//        spinnerStatus = findViewById(R.id.spinnerStatus);
//        spinnerLateStatus = findViewById(R.id.spinnerLateStatus);
//
//        ivCheckInPhoto = findViewById(R.id.ivCheckInPhoto);
//        ivCheckOutPhoto = findViewById(R.id.ivCheckOutPhoto);
//
//        btnSave = findViewById(R.id.btnSave);
//        btnDelete = findViewById(R.id.btnDelete);
//
//        // Format and display date
//        formatAndDisplayDate();
//
//        // Set click listeners
//        etCheckIn.setOnClickListener(v -> openTimePicker(etCheckIn));
//        etCheckOut.setOnClickListener(v -> openTimePicker(etCheckOut));
//
//        btnSave.setOnClickListener(v -> saveAttendance());
//        btnDelete.setOnClickListener(v -> showDeletePopup());
//    }
//
//    private void formatAndDisplayDate() {
//        try {
//            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
//            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
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
//    @Override
//    public void onBackPressed() {
//        if (hasUnsavedChanges()) {
//            showUnsavedChangesDialog();
//        } else {
//            super.onBackPressed();
//        }
//    }
//
//    private boolean hasUnsavedChanges() {
//        String currentCheckIn = etCheckIn.getText() != null ? etCheckIn.getText().toString().trim() : "";
//        String currentCheckOut = etCheckOut.getText() != null ? etCheckOut.getText().toString().trim() : "";
//        String currentStatus = spinnerStatus.getText().toString();
//        String currentLateStatus = spinnerLateStatus.getText().toString();
//
//        return !currentCheckIn.equals(originalCheckIn) ||
//                !currentCheckOut.equals(originalCheckOut) ||
//                !currentStatus.equals(originalStatus) ||
//                !currentLateStatus.equals(originalLateStatus);
//    }
//
//    private void showUnsavedChangesDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Unsaved Changes")
//                .setMessage("You have unsaved changes. Do you want to save before leaving?")
//                .setPositiveButton("Save", (d, w) -> saveAttendance())
//                .setNegativeButton("Discard", (d, w) -> finish())
//                .setNeutralButton("Cancel", null)
//                .show();
//    }
//
//    private void setupFirebase() {
//        attendanceRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("attendance")
//                .child(date)
//                .child(employeeMobile);
//    }
//
//    private void setupSpinners() {
//        String[] statusOptions = {"Present", "Half Day", "Absent", "Full Day"};
//        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
//                this,
//                android.R.layout.simple_dropdown_item_1line,
//                statusOptions
//        );
//        spinnerStatus.setAdapter(statusAdapter);
//        spinnerStatus.setFocusable(false);
//        spinnerStatus.setCursorVisible(false);
//        spinnerStatus.setOnClickListener(v -> spinnerStatus.showDropDown());
//
//        String[] lateOptions = {"On Time", "Late"};
//        ArrayAdapter<String> lateAdapter = new ArrayAdapter<>(
//                this,
//                android.R.layout.simple_dropdown_item_1line,
//                lateOptions
//        );
//        spinnerLateStatus.setAdapter(lateAdapter);
//        spinnerLateStatus.setFocusable(false);
//        spinnerLateStatus.setCursorVisible(false);
//        spinnerLateStatus.setOnClickListener(v -> spinnerLateStatus.showDropDown());
//    }
//
//    private void loadAttendance() {
//        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot s) {
//                if (!s.exists()) {
//                    spinnerStatus.setText("Absent", false);
//                    spinnerLateStatus.setText("On Time", false);
//                    tvTotalHours.setText("0h 0m");
//                    tvCheckInAddress.setText("No check-in recorded");
//                    tvCheckOutAddress.setText("No check-out recorded");
//
//                    // Store as original values
//                    originalStatus = "Absent";
//                    originalLateStatus = "On Time";
//                    return;
//                }
//
//                // Load check-in time
//                String checkIn = s.child("checkInTime").getValue(String.class);
//                if (checkIn != null && !checkIn.isEmpty()) {
//                    etCheckIn.setText(checkIn);
//                    originalCheckIn = checkIn;
//                }
//
//                // Load check-out time
//                String checkOut = s.child("checkOutTime").getValue(String.class);
//                if (checkOut != null && !checkOut.isEmpty()) {
//                    etCheckOut.setText(checkOut);
//                    originalCheckOut = checkOut;
//                }
//
//                // Load status
//                String status = s.child("status").getValue(String.class);
//                if (status != null) {
//                    spinnerStatus.setText(status, false);
//                    originalStatus = status;
//                } else {
//                    spinnerStatus.setText("Present", false);
//                    originalStatus = "Present";
//                }
//
//                // Load late status
//                String lateStatus = s.child("lateStatus").getValue(String.class);
//                if (lateStatus != null) {
//                    spinnerLateStatus.setText(lateStatus, false);
//                    originalLateStatus = lateStatus;
//                } else {
//                    spinnerLateStatus.setText("On Time", false);
//                    originalLateStatus = "On Time";
//                }
//
//                // Load total hours
//                String totalHours = s.child("totalHours").getValue(String.class);
//                if (totalHours != null && !totalHours.isEmpty()) {
//                    tvTotalHours.setText(totalHours);
//                } else {
//                    tvTotalHours.setText("0h 0m");
//                }
//
//                // Load addresses
//                String checkInAddress = s.child("checkInAddress").getValue(String.class);
//                if (checkInAddress != null && !checkInAddress.isEmpty()) {
//                    tvCheckInAddress.setText(checkInAddress);
//                } else {
//                    tvCheckInAddress.setText("Location not available");
//                }
//
//                String checkOutAddress = s.child("checkOutAddress").getValue(String.class);
//                if (checkOutAddress != null && !checkOutAddress.isEmpty()) {
//                    tvCheckOutAddress.setText(checkOutAddress);
//                } else {
//                    tvCheckOutAddress.setText("Location not available");
//                }
//
//                // Load photos
//                String checkInPhoto = s.child("checkInPhoto").getValue(String.class);
//                if (checkInPhoto != null && !checkInPhoto.isEmpty()) {
//                    Glide.with(AdminDayAttendanceDetailActivity.this)
//                            .load(checkInPhoto)
//                            .placeholder(R.drawable.ic_image_placeholder)
//                            .error(R.drawable.ic_image_placeholder)
//                            .into(ivCheckInPhoto);
//                }
//
//                String checkOutPhoto = s.child("checkOutPhoto").getValue(String.class);
//                if (checkOutPhoto != null && !checkOutPhoto.isEmpty()) {
//                    Glide.with(AdminDayAttendanceDetailActivity.this)
//                            .load(checkOutPhoto)
//                            .placeholder(R.drawable.ic_image_placeholder)
//                            .error(R.drawable.ic_image_placeholder)
//                            .into(ivCheckOutPhoto);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError e) {
//                Toast.makeText(AdminDayAttendanceDetailActivity.this,
//                        "Failed to load attendance data", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void openTimePicker(TextInputEditText target) {
//        Calendar cal = Calendar.getInstance();
//
//        // If the field already has a time, parse it and set as initial time
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
//            // Auto-calculate total hours when both times are set
//            calculateTotalHours();
//
//        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
//    }
//
//    private void calculateTotalHours() {
//        String in = etCheckIn.getText() != null ? etCheckIn.getText().toString().trim() : "";
//        String out = etCheckOut.getText() != null ? etCheckOut.getText().toString().trim() : "";
//
//        if (in.isEmpty() || out.isEmpty()) {
//            return;
//        }
//
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//            Date inTime = sdf.parse(in);
//            Date outTime = sdf.parse(out);
//
//            if (inTime != null && outTime != null) {
//                long diff = outTime.getTime() - inTime.getTime();
//                if (diff < 0) diff += 24 * 60 * 60 * 1000; // Handle overnight shifts
//
//                long totalMinutes = diff / 60000;
//                long hours = totalMinutes / 60;
//                long minutes = totalMinutes % 60;
//
//                tvTotalHours.setText(String.format(Locale.ENGLISH, "%dh %dm", hours, minutes));
//            }
//        } catch (Exception ignored) {}
//    }
//
//    private void saveAttendance() {
//        String in = etCheckIn.getText() != null ? etCheckIn.getText().toString().trim() : "";
//        String out = etCheckOut.getText() != null ? etCheckOut.getText().toString().trim() : "";
//        String status = spinnerStatus.getText().toString();
//        String lateStatus = spinnerLateStatus.getText().toString();
//
//        if (in.isEmpty()) {
//            Toast.makeText(this, "Check-in time is required", Toast.LENGTH_SHORT).show();
//            etCheckIn.requestFocus();
//            return;
//        }
//
//        if (status.isEmpty()) {
//            Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show();
//            spinnerStatus.requestFocus();
//            return;
//        }
//
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//            Date inTime = sdf.parse(in);
//            Date outTime = out.isEmpty() ? null : sdf.parse(out);
//
//            long totalMinutes = 0;
//            if (inTime != null && outTime != null) {
//                long diff = outTime.getTime() - inTime.getTime();
//                if (diff < 0) diff += 24 * 60 * 60 * 1000; // Handle overnight shifts
//                totalMinutes = diff / 60000;
//            }
//
//            Map<String, Object> map = new HashMap<>();
//            map.put("checkInTime", in);
//            map.put("checkOutTime", out);
//            map.put("status", status);
//            map.put("lateStatus", lateStatus);
//            map.put("totalMinutes", totalMinutes);
//            map.put("totalHours", (totalMinutes / 60) + "h " + (totalMinutes % 60) + "m");
//            map.put("markedBy", "Admin");
//            map.put("lastModified", System.currentTimeMillis());
//
//            // Disable save button to prevent multiple clicks
//            btnSave.setEnabled(false);
//
//            attendanceRef.updateChildren(map)
//                    .addOnSuccessListener(a -> {
//                        Toast.makeText(this, "Attendance updated successfully", Toast.LENGTH_SHORT).show();
//
//                        // Update original values after successful save
//                        originalCheckIn = in;
//                        originalCheckOut = out;
//                        originalStatus = status;
//                        originalLateStatus = lateStatus;
//
//                        finish();
//                    })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(this, "Failed to update attendance", Toast.LENGTH_SHORT).show();
//                        btnSave.setEnabled(true);
//                    });
//
//        } catch (Exception e) {
//            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
//            btnSave.setEnabled(true);
//        }
//    }
//
//    private void showDeletePopup() {
//        new AlertDialog.Builder(this)
//                .setTitle("Delete Attendance")
//                .setMessage("Are you sure you want to delete this attendance record? This action cannot be undone.")
//                .setPositiveButton("Delete", (d, w) -> deleteAttendance())
//                .setNegativeButton("Cancel", null)
//                .setCancelable(true)
//                .show();
//    }
//
//    private void deleteAttendance() {
//        // Disable delete button to prevent multiple clicks
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
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.R;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdminDayAttendanceDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvDate, tvTotalHours;
    private TextView tvCheckInAddress, tvCheckOutAddress;
    private TextInputEditText etCheckIn, etCheckOut;
    private AutoCompleteTextView spinnerStatus, spinnerLateStatus;
    private ImageView ivCheckInPhoto, ivCheckOutPhoto;
    private MaterialButton btnSave, btnDelete;

    private String companyKey, employeeMobile, date;
    private DatabaseReference attendanceRef;

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

        initViews();
        setupToolbar();
        setupFirebase();
        setupSpinners();
        loadAttendance();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        tvDate = findViewById(R.id.tvDate);
        tvTotalHours = findViewById(R.id.tvTotalHours);
        tvCheckInAddress = findViewById(R.id.tvCheckInAddress);
        tvCheckOutAddress = findViewById(R.id.tvCheckOutAddress);

        etCheckIn = findViewById(R.id.etCheckIn);
        etCheckOut = findViewById(R.id.etCheckOut);

        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerLateStatus = findViewById(R.id.spinnerLateStatus);

        ivCheckInPhoto = findViewById(R.id.ivCheckInPhoto);
        ivCheckOutPhoto = findViewById(R.id.ivCheckOutPhoto);

        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        // Format and display date
        formatAndDisplayDate();

        // Set click listeners
        etCheckIn.setOnClickListener(v -> openTimePicker(etCheckIn));
        etCheckOut.setOnClickListener(v -> openTimePicker(etCheckOut));

        btnSave.setOnClickListener(v -> saveAttendance());
        btnDelete.setOnClickListener(v -> showDeletePopup());
    }

    private void formatAndDisplayDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
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
        String currentStatus = spinnerStatus.getText().toString();
        String currentLateStatus = spinnerLateStatus.getText().toString();

        return !currentCheckIn.equals(originalCheckIn) ||
                !currentCheckOut.equals(originalCheckOut) ||
                !currentStatus.equals(originalStatus) ||
                !currentLateStatus.equals(originalLateStatus);
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
        attendanceRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("attendance")
                .child(date)
                .child(employeeMobile);
    }

    private void setupSpinners() {
        String[] statusOptions = {"Present", "Half Day", "Absent", "Full Day"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                statusOptions
        );
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setFocusable(false);
        spinnerStatus.setCursorVisible(false);
        spinnerStatus.setOnClickListener(v -> spinnerStatus.showDropDown());

        String[] lateOptions = {"On Time", "Late"};
        ArrayAdapter<String> lateAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                lateOptions
        );
        spinnerLateStatus.setAdapter(lateAdapter);
        spinnerLateStatus.setFocusable(false);
        spinnerLateStatus.setCursorVisible(false);
        spinnerLateStatus.setOnClickListener(v -> spinnerLateStatus.showDropDown());
    }

    private void loadAttendance() {
        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (!s.exists()) {
                    spinnerStatus.setText("Absent", false);
                    spinnerLateStatus.setText("On Time", false);
                    tvTotalHours.setText("0h 0m");
                    tvCheckInAddress.setText("No check-in recorded");
                    tvCheckOutAddress.setText("No check-out recorded");

                    originalStatus = "Absent";
                    originalLateStatus = "On Time";
                    return;
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
                    spinnerStatus.setText(status, false);
                    originalStatus = status;
                } else {
                    spinnerStatus.setText("Present", false);
                    originalStatus = "Present";
                }

                // Load late status
                String lateStatus = s.child("lateStatus").getValue(String.class);
                if (lateStatus != null) {
                    spinnerLateStatus.setText(lateStatus, false);
                    originalLateStatus = lateStatus;
                } else {
                    spinnerLateStatus.setText("On Time", false);
                    originalLateStatus = "On Time";
                }

                // Calculate and display times
                calculateTotalHours(checkIn, checkOut);

                // Load addresses
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
                    Glide.with(AdminDayAttendanceDetailActivity.this)
                            .load(checkInPhoto)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(ivCheckInPhoto);
                }

                String checkOutPhoto = s.child("checkOutPhoto").getValue(String.class);
                if (checkOutPhoto != null && !checkOutPhoto.isEmpty()) {
                    Glide.with(AdminDayAttendanceDetailActivity.this)
                            .load(checkOutPhoto)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(ivCheckOutPhoto);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
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
            String in = etCheckIn.getText() != null ? etCheckIn.getText().toString().trim() : "";
            String out = etCheckOut.getText() != null ? etCheckOut.getText().toString().trim() : "";
            calculateTotalHours(in, out);

        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private void calculateTotalHours(String checkIn, String checkOut) {
        if (checkIn == null || checkIn.isEmpty() || checkOut == null || checkOut.isEmpty()) {
            tvTotalHours.setText("0h 0m");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date inTime = sdf.parse(checkIn);
            Date outTime = sdf.parse(checkOut);

            if (inTime != null && outTime != null) {
                long diff = outTime.getTime() - inTime.getTime();
                if (diff < 0) diff += 24 * 60 * 60 * 1000; // Handle overnight shifts

                long totalMinutes = diff / 60000;
                long hours = totalMinutes / 60;
                long minutes = totalMinutes % 60;

                tvTotalHours.setText(String.format(Locale.ENGLISH, "%dh %dm", hours, minutes));
            }
        } catch (Exception e) {
            tvTotalHours.setText("0h 0m");
        }
    }

    private void saveAttendance() {
        String in = etCheckIn.getText() != null ? etCheckIn.getText().toString().trim() : "";
        String out = etCheckOut.getText() != null ? etCheckOut.getText().toString().trim() : "";
        String status = spinnerStatus.getText().toString();
        String lateStatus = spinnerLateStatus.getText().toString();

        if (in.isEmpty()) {
            Toast.makeText(this, "Check-in time is required", Toast.LENGTH_SHORT).show();
            etCheckIn.requestFocus();
            return;
        }

        if (status.isEmpty()) {
            Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show();
            spinnerStatus.requestFocus();
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date inTime = sdf.parse(in);
            Date outTime = out.isEmpty() ? null : sdf.parse(out);

            long totalMinutes = 0;
            if (inTime != null && outTime != null) {
                long diff = outTime.getTime() - inTime.getTime();
                if (diff < 0) diff += 24 * 60 * 60 * 1000; // Handle overnight shifts
                totalMinutes = diff / 60000;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("checkInTime", in);
            map.put("checkOutTime", out);
            map.put("status", status);
            map.put("lateStatus", lateStatus);
            map.put("totalMinutes", totalMinutes);
            map.put("totalHours", (totalMinutes / 60) + "h " + (totalMinutes % 60) + "m");
            map.put("markedBy", "Admin");
            map.put("lastModified", System.currentTimeMillis());

            // Disable save button to prevent multiple clicks
            btnSave.setEnabled(false);

            attendanceRef.updateChildren(map)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Attendance updated successfully", Toast.LENGTH_SHORT).show();

                        // Update original values after successful save
                        originalCheckIn = in;
                        originalCheckOut = out;
                        originalStatus = status;
                        originalLateStatus = lateStatus;

                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update attendance", Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
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
}