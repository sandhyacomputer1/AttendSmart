package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import java.util.Locale;

public class EmployeeDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvCompany, tvRole, tvShift, tvTodayStatus, tvCurrentTime;
    private MaterialButton btnCheckIn, btnCheckOut;
    private String companyKey, employeeMobile, employeeShift, shiftStart, shiftEnd;
    private DatabaseReference employeesRef, attendanceRef;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private String todayStatus = "Absent";
    private String pendingAction = ""; // Track checkIn vs checkOut
    private static final int CAMERA_REQUEST_CODE_CHECKIN = 101;
    private static final int CAMERA_REQUEST_CODE_CHECKOUT = 102;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);

        initViews();
        setupFirebase();
        loadEmployeeData();  // Load FIRST
        updateCurrentTime();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvCompany = findViewById(R.id.tvCompany);
        tvRole = findViewById(R.id.tvRole);
        tvShift = findViewById(R.id.tvShift);
        tvTodayStatus = findViewById(R.id.tvTodayStatus);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnCheckOut = findViewById(R.id.btnCheckOut);

        btnCheckIn.setOnClickListener(v -> checkInWithCamera());
        btnCheckOut.setOnClickListener(v -> checkOutWithCamera());
    }

    private void setupFirebase() {
        companyKey = getIntent().getStringExtra("companyKey");
        if (companyKey == null) {
            PrefManager prefManager = new PrefManager(this);
            companyKey = prefManager.getCompanyKey();
        }
        if (companyKey == null) {
            Toast.makeText(this, "Company not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies").child(companyKey).child("employees");
        attendanceRef = FirebaseDatabase.getInstance()
                .getReference("Companies").child(companyKey).child("attendance");
    }

    private void loadEmployeeData() {
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getEmployeeEmail();
        if (email == null) {
            Toast.makeText(this, "Employee session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        employeesRef.orderByChild("info/employeeEmail").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot employeeSnapshot : snapshot.getChildren()) {
                            employeeMobile = employeeSnapshot.getKey();  // ✅ Mobile key for attendance
                            DataSnapshot info = employeeSnapshot.child("info");
                            String name = info.child("employeeName").getValue(String.class);
                            String role = info.child("employeeRole").getValue(String.class);
                            employeeShift = info.child("employeeShift").getValue(String.class);
                            String department = info.child("employeeDepartment").getValue(String.class);

                            tvWelcome.setText("Welcome, " + (name != null ? name : "Employee") + "!");
                            tvCompany.setText("Company: " + companyKey.replace(",", "."));
                            tvRole.setText("Role: " + (role != null ? role : "Employee") + " (" + (department != null ? department : "N/A") + ")");
                            tvShift.setText("Shift: " + (employeeShift != null ? employeeShift : "Not set"));
                            parseShiftTime(employeeShift);

                            // ✅ Now safe to load status
                            loadTodayStatus();
                            return;
                        }
                        Toast.makeText(EmployeeDashboardActivity.this, "Employee data not found", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(EmployeeDashboardActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void parseShiftTime(String shiftText) {
        if (shiftText != null && shiftText.contains("(")) {
            try {
                String timeRange = shiftText.substring(shiftText.indexOf("(") + 1, shiftText.indexOf(")"));
                String[] times = timeRange.split(" - ");
                if (times.length == 2) {
                    shiftStart = times[0].trim();
                    shiftEnd = times[1].trim();
                }
            } catch (Exception e) {
                shiftStart = "9:00 AM";
                shiftEnd = "6:00 PM";
            }
        }
    }

    private void loadTodayStatus() {
        if (employeeMobile == null) {
            tvTodayStatus.setText("Today: Loading...");
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        attendanceRef.child(today).child(employeeMobile).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.hasChild("checkOutTime")) {
                        todayStatus = "Completed";
                        btnCheckIn.setEnabled(false);
                        btnCheckOut.setEnabled(false);
                    } else if (snapshot.hasChild("checkInTime")) {
                        todayStatus = "Check In Done";
                        btnCheckIn.setEnabled(false);
                        btnCheckOut.setEnabled(true);
                    } else {
                        todayStatus = "Absent";
                        btnCheckIn.setEnabled(true);
                        btnCheckOut.setEnabled(false);
                    }
                } else {
                    todayStatus = "Absent";
                    btnCheckIn.setEnabled(true);
                    btnCheckOut.setEnabled(false);
                }
                updateStatusUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateStatusUI() {
        tvTodayStatus.setText("Today: " + todayStatus);
        int color = ContextCompat.getColor(this,
                todayStatus.equals("Completed") ? R.color.green :
                        todayStatus.equals("Check In Done") ? R.color.orange : R.color.red);
        tvTodayStatus.setTextColor(color);
    }

    private void checkInWithCamera() {
        if (isWithinShiftTime("start")) {
            if (checkCameraPermission()) {
                openCamera("checkIn");
            } else {
                requestCameraPermission("checkIn"); // Pass action type
            }
        } else {
            Toast.makeText(this, "Outside shift time! Shift starts at " + (shiftStart != null ? shiftStart : "9:00 AM"), Toast.LENGTH_LONG).show();
        }
    }

    private void checkOutWithCamera() {
        if (isWithinShiftTime("end")) {
            if (checkCameraPermission()) {
                openCamera("checkOut");
            } else {
                requestCameraPermission("checkOut"); // Pass action type
            }
        } else {
            Toast.makeText(this, "Outside shift time! Shift ends at " + (shiftEnd != null ? shiftEnd : "6:00 PM"), Toast.LENGTH_LONG).show();
        }
    }


    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    private void openCamera(String action) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            int requestCode = action.equals("checkIn") ? CAMERA_REQUEST_CODE_CHECKIN : CAMERA_REQUEST_CODE_CHECKOUT;
            startActivityForResult(cameraIntent, requestCode);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == CAMERA_PERMISSION_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                openCamera("checkIn");
//            } else {
//                Toast.makeText(this, "Camera permission required for attendance", Toast.LENGTH_SHORT).show();
//            }
//        }
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == CAMERA_PERMISSION_CODE) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera(pendingAction); // Now opens correct camera action
        } else {
            Toast.makeText(this, "Camera permission required for attendance", Toast.LENGTH_SHORT).show();
        }
    }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // ✅ FIXED: Use correct request codes
        if ((requestCode == CAMERA_REQUEST_CODE_CHECKIN || requestCode == CAMERA_REQUEST_CODE_CHECKOUT)
                && resultCode == RESULT_OK && data != null) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            recordAttendance(); // Single call only
        }
    }

    private void recordAttendance() {
        if (employeeMobile == null) {
            Toast.makeText(this, "Employee data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());

        DatabaseReference attendanceNode = attendanceRef.child(today).child(employeeMobile);

        // ✅ FIXED: Direct child checks - No DataSnapshot casting
        attendanceNode.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasCheckIn = snapshot.hasChild("checkInTime");
                boolean hasCheckOut = snapshot.hasChild("checkOutTime");

                if (!hasCheckIn) {
                    // ✅ Check In - First time only
                    attendanceNode.child("checkInTime").setValue(currentTime);
                    attendanceNode.child("checkInPhoto").setValue("captured");
                    todayStatus = "Check In Done";
                    Toast.makeText(EmployeeDashboardActivity.this, "✅ Check In Recorded: " + currentTime, Toast.LENGTH_LONG).show();
                } else if (hasCheckIn && !hasCheckOut) {
                    // ✅ Check Out - After check-in only
                    attendanceNode.child("checkOutTime").setValue(currentTime);
                    attendanceNode.child("checkOutPhoto").setValue("captured");
                    todayStatus = "Completed";
                    Toast.makeText(EmployeeDashboardActivity.this, "✅ Check Out Recorded: " + currentTime, Toast.LENGTH_LONG).show();
                } else {
                    // ✅ Already completed
                    Toast.makeText(EmployeeDashboardActivity.this, "Attendance already completed for today", Toast.LENGTH_SHORT).show();
                }

                updateStatusUI();
                loadTodayStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmployeeDashboardActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isWithinShiftTime(String type) {
        return true; // Simplified for now
    }

    private void updateCurrentTime() {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String currentTimeStr = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(new Date());
                tvCurrentTime.setText(currentTimeStr);
                handler.postDelayed(this, 60000); // Update every minute
            }
        };
        handler.post(runnable);
    }
    private void requestCameraPermission(String action) {
        pendingAction = action;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }


}
