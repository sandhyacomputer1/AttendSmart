package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EmployeeDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvCompany, tvRole, tvShift, tvTodayStatus, tvCurrentTime;
    private MaterialButton btnCheckIn, btnCheckOut;
    private String companyKey, employeeMobile, employeeShift, shiftStart, shiftEnd;
    private DatabaseReference employeesRef, attendanceRef, shiftsRef;
    private StorageReference storageRef;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE_CHECKIN = 101;
    private static final int CAMERA_REQUEST_CODE_CHECKOUT = 102;

    private String todayStatus = "Absent";
    private String pendingAction = "";
    private Bitmap currentPhotoBitmap;
    private String shiftKey;
    private boolean isCheckInDone = false; // ✅ Track check-in status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);

        initViews();
        setupFirebase();
        loadEmployeeData();
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

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        employeesRef = database.getReference("Companies").child(companyKey).child("employees");
        attendanceRef = database.getReference("Companies").child(companyKey).child("attendance");
        shiftsRef = database.getReference("Companies").child(companyKey).child("shifts");
        storageRef = FirebaseStorage.getInstance().getReference()
                .child("Companies").child(companyKey).child("attendance_photos");
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
                            employeeMobile = employeeSnapshot.getKey();
                            DataSnapshot info = employeeSnapshot.child("info");
                            String name = info.child("employeeName").getValue(String.class);
                            String role = info.child("employeeRole").getValue(String.class);
                            employeeShift = info.child("employeeShift").getValue(String.class);
                            String department = info.child("employeeDepartment").getValue(String.class);
                            shiftKey = info.child("shiftKey").getValue(String.class);

                            tvWelcome.setText("Welcome, " + (name != null ? name : "Employee") + "!");
                            tvCompany.setText("Company: " + companyKey.replace(",", "."));
                            tvRole.setText("Role: " + (role != null ? role : "Employee") + " (" + (department != null ? department : "N/A") + ")");
                            tvShift.setText("Shift: " + (employeeShift != null ? employeeShift : "Not set"));

                            // ✅ Load shift details or default
                            if (shiftKey != null && !shiftKey.isEmpty()) {
                                loadShiftDetails(shiftKey);
                            } else {
                                shiftStart = "09:00 AM";
                                shiftEnd = "06:00 PM";
                                loadTodayStatus();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(EmployeeDashboardActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadShiftDetails(String shiftKey) {
        shiftsRef.child(shiftKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    shiftStart = snapshot.child("startTime").getValue(String.class);
                    shiftEnd = snapshot.child("endTime").getValue(String.class);
                    if (shiftStart == null) shiftStart = "09:00 AM";
                    if (shiftEnd == null) shiftEnd = "06:00 PM";
                } else {
                    shiftStart = "09:00 AM";
                    shiftEnd = "06:00 PM";
                }
                tvShift.setText("Shift: " + shiftStart + " - " + shiftEnd);
                loadTodayStatus(); // ✅ Now load status
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                shiftStart = "09:00 AM";
                shiftEnd = "06:00 PM";
                loadTodayStatus();
            }
        });
    }

    private void loadTodayStatus() {
        if (employeeMobile == null) {
            tvTodayStatus.setText("Today: Loading...");
            return;
        }

        String today = getTodayDate();
        attendanceRef.child(today).child(employeeMobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        boolean hasCheckIn = snapshot.hasChild("checkInTime");
                        boolean hasCheckOut = snapshot.hasChild("checkOutTime");

                        // flags
                        isCheckInDone = hasCheckIn;

                        if (!hasCheckIn && !hasCheckOut) {
                            todayStatus = "Absent";
                        } else if (hasCheckIn && !hasCheckOut) {
                            // फक्त check-in झालेले
                            String checkInTime = snapshot.child("checkInTime").getValue(String.class);
                            long minutesLate = getTimeDifferenceMinutes(shiftStart, checkInTime);
                            todayStatus = minutesLate > 15 ? "Late" : "Present (In)";
                        } else if (hasCheckIn && hasCheckOut) {
                            todayStatus = "Present";
                        }

                        updateStatusUI();
                        updateButtons(hasCheckIn, hasCheckOut);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void calculateTodayStatus(DataSnapshot snapshot) {
        boolean hasCheckIn = snapshot.hasChild("checkInTime");
        boolean hasCheckOut = snapshot.hasChild("checkOutTime");

        if (hasCheckOut) {
            todayStatus = "Present";
            isCheckInDone = true;
        } else if (hasCheckIn) {
            isCheckInDone = true;
            String checkInTime = snapshot.child("checkInTime").getValue(String.class);
            long minutesLate = getTimeDifferenceMinutes(checkInTime, shiftStart);
            todayStatus = minutesLate > 15 ? "Late" : "Present (Checked In)";
        } else {
            todayStatus = "Absent";
            isCheckInDone = false;
        }
    }

    private void updateStatusUI() {
        tvTodayStatus.setText("Today: " + todayStatus);
        int color = ContextCompat.getColor(this,
                todayStatus.contains("Present") ? R.color.green :
                        todayStatus.equals("Late") ? R.color.orange :
                                R.color.red);
        tvTodayStatus.setTextColor(color);
    }

    private void updateButtons(boolean hasCheckIn, boolean hasCheckOut) {
        // Check-in फक्त तेव्हाच possible आहे जेव्हा अजून check-in झालेले नाही
        btnCheckIn.setEnabled(!hasCheckIn);

        // Check-out फक्त तेव्हाच possible जेव्हा check-in झाले आहे आणि check-out झालेला नाही
        btnCheckOut.setEnabled(hasCheckIn && !hasCheckOut);
    }


    private void checkInWithCamera() {
        if (isWithinShiftTime("start", 60)) { // ✅ 60 min grace period
            if (checkCameraPermission()) {
                pendingAction = "checkIn";
                openCamera();
            } else {
                requestCameraPermission("checkIn");
            }
        } else {
            Toast.makeText(this, "⏰ Check-in time: " + shiftStart + " (60 min grace)", Toast.LENGTH_LONG).show();
        }
    }

    private void checkOutWithCamera() {
        if (isWithinShiftTime("end", 120)) { // ✅ 2 hours before/after allowed
            if (checkCameraPermission()) {
                pendingAction = "checkOut";
                openCamera();
            } else {
                requestCameraPermission("checkOut");
            }
        } else {
            Toast.makeText(this, "⏰ Check-out time: " + shiftEnd + " (±2 hrs)", Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission(String action) {
        pendingAction = action;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, pendingAction.equals("checkIn") ?
                    CAMERA_REQUEST_CODE_CHECKIN : CAMERA_REQUEST_CODE_CHECKOUT);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "📸 Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == CAMERA_REQUEST_CODE_CHECKIN || requestCode == CAMERA_REQUEST_CODE_CHECKOUT)
                && resultCode == RESULT_OK && data != null) {
            currentPhotoBitmap = (Bitmap) data.getExtras().get("data");
            if (currentPhotoBitmap != null) {
                uploadPhotoAndRecordAttendance();
            }
        }
    }

    private void uploadPhotoAndRecordAttendance() {
        if (currentPhotoBitmap == null || employeeMobile == null) {
            Toast.makeText(this, "Photo capture failed", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Show uploading progress
        Toast.makeText(this, "📤 Uploading photo...", Toast.LENGTH_SHORT).show();

        String today = getTodayDate();
        String timestamp = System.currentTimeMillis() + "";
        String photoPath = String.format("attendance/%s/%s_%s_%s.jpg",
                today, employeeMobile, pendingAction, timestamp);

        StorageReference photoRef = storageRef.child(photoPath);

        // ✅ Compress bitmap
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currentPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] data = baos.toByteArray();

        // ✅ Upload to Firebase Storage
        photoRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String photoUrl = uri.toString();
                        Toast.makeText(this, "✅ Photo uploaded successfully", Toast.LENGTH_SHORT).show();
                        recordAttendance(photoUrl);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "❌ URL failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        recordAttendance("photo_uploaded"); // Continue without URL
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    recordAttendance("upload_failed");
                });
    }

    private void recordAttendance(String photoUrl) {
        String today = getTodayDate();
        String currentTime = getCurrentTime();
        DatabaseReference attendanceNode = attendanceRef.child(today).child(employeeMobile);

        attendanceNode.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (pendingAction.equals("checkIn") && !snapshot.hasChild("checkInTime")) {
                    // ✅ CHECK-IN LOGIC
                    long minutesLate = getTimeDifferenceMinutes(shiftStart, currentTime);
                    String status = minutesLate > 15 ? "Late" : "Present";

                    attendanceNode.child("checkInTime").setValue(currentTime);
                    attendanceNode.child("checkInPhoto").setValue(photoUrl);
                    attendanceNode.child("status").setValue(status);
                    attendanceNode.child("shiftStart").setValue(shiftStart);
                    attendanceNode.child("shiftEnd").setValue(shiftEnd);
                    attendanceNode.child("employeeMobile").setValue(employeeMobile);

                    isCheckInDone = true;
                    todayStatus = status;
                    Toast.makeText(EmployeeDashboardActivity.this,
                            "✅ CHECK-IN: " + currentTime + (minutesLate > 15 ? " (Late " + minutesLate + "m)" : ""),
                            Toast.LENGTH_LONG).show();

                } else if (pendingAction.equals("checkOut") && snapshot.hasChild("checkInTime") && !snapshot.hasChild("checkOutTime")) {
                    // ✅ CHECK-OUT LOGIC - FIXED!
                    String checkInTime = snapshot.child("checkInTime").getValue(String.class);
                    long totalMinutes = getTimeDifferenceMinutes(checkInTime, currentTime);
                    double totalHours = totalMinutes / 60.0;

                    attendanceNode.child("checkOutTime").setValue(currentTime);
                    attendanceNode.child("checkOutPhoto").setValue(photoUrl);
                    attendanceNode.child("totalHours").setValue(String.format("%.2f", totalHours));
                    attendanceNode.child("status").setValue("Present");
                    attendanceNode.child("totalMinutes").setValue(totalMinutes);

                    isCheckInDone = true;
                    todayStatus = "Present";
                    Toast.makeText(EmployeeDashboardActivity.this,
                            "✅ CHECK-OUT: " + currentTime + " (Total: " + String.format("%.2f hrs)", totalHours),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(EmployeeDashboardActivity.this, "⚠️ Invalid action for current status", Toast.LENGTH_SHORT).show();
                }

                updateStatusUI();
// recordAttendance() मध्ये data change नंतर
                boolean hasCheckIn = true; // कारण check-in case मध्ये नक्की true असेल
                boolean hasCheckOut = pendingAction.equals("checkOut");
                updateButtons(hasCheckIn, hasCheckOut);
                loadTodayStatus(); // Refresh
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmployeeDashboardActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isWithinShiftTime(String type, int graceMinutes) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
            Calendar now = Calendar.getInstance();
            Calendar shiftCal = Calendar.getInstance();

            Date shiftTime = sdf.parse(type.equals("start") ? shiftStart : shiftEnd);
            shiftCal.setTime(shiftTime);
            shiftCal.set(Calendar.YEAR, now.get(Calendar.YEAR));
            shiftCal.set(Calendar.MONTH, now.get(Calendar.MONTH));
            shiftCal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            shiftCal.add(Calendar.MINUTE, graceMinutes);
            return now.before(shiftCal) || now.equals(shiftCal);
        } catch (Exception e) {
            return true; // Allow if parsing fails
        }
    }

    private long getTimeDifferenceMinutes(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date d1 = sdf.parse(startTime);
            Date d2 = sdf.parse(endTime);
            return (d2.getTime() - d1.getTime()) / (1000 * 60);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date());
    }

    private void updateCurrentTime() {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String currentTimeStr = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(new Date());
                tvCurrentTime.setText(currentTimeStr);
                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(runnable);
    }
}
