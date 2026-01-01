//package com.sandhyyasofttech.attendsmart.Activities;
//
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.widget.Switch;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Utils.AttendanceReminderHelper;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//import android.content.Intent;
//import android.view.View;
//import android.widget.ImageView;
//
//public class SettingsActivity extends AppCompatActivity {
//
//    private Switch switchNotifications;
//    private TextView tvShiftTiming;
//
//    private DatabaseReference dbRef;
//    private String companyKey, employeeMobile;
//    private PrefManager pref;
//
//    private ImageView ivProfile;
//    private View cardPersonalDetails;
//    private View cardAttendanceReport;
//    private View cardEmployment;
//    private View cardShiftTiming;
//    private View cardNotifications;
//
//    private static final int NOTIFICATION_PERMISSION_CODE = 101;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_settings);
//
//        switchNotifications = findViewById(R.id.switchNotifications);
//        tvShiftTiming = findViewById(R.id.tvShiftTiming);
//
//        ivProfile = findViewById(R.id.ivProfile);
//        cardPersonalDetails = findViewById(R.id.cardPersonalDetails);
//        cardEmployment = findViewById(R.id.cardEmployment);
//        cardShiftTiming = findViewById(R.id.cardShiftTiming);
//        cardAttendanceReport = findViewById(R.id.cardAttendanceReport);
//        cardNotifications = findViewById(R.id.cardNotifications);
//
//        pref = new PrefManager(this);
//        companyKey = pref.getCompanyKey();
//        employeeMobile = pref.getEmployeeMobile();
//
//        dbRef = FirebaseDatabase.getInstance().getReference();
//
//        // Setup click listeners
//        setupClickListeners();
//
//        // Load shift timing
//        loadShiftTiming();
//
//        // Load notification preference
//        boolean notificationsEnabled = pref.getNotificationsEnabled();
//        switchNotifications.setChecked(notificationsEnabled);
//
//        // Set up switch listener
//        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                if (checkNotificationPermission()) {
//                    enableNotifications();
//                } else {
//                    requestNotificationPermission();
//                    // Will be enabled after permission is granted
//                    switchNotifications.setChecked(false);
//                }
//            } else {
//                disableNotifications();
//            }
//        });
//    }
//
//    private void setupClickListeners() {
//        // Profile image click
//        ivProfile.setOnClickListener(v -> {
//            startActivity(new Intent(SettingsActivity.this, PersonalDetailsActivity.class));
//        });
//
//        // Personal Details card click
//        cardPersonalDetails.setOnClickListener(v -> {
//            startActivity(new Intent(SettingsActivity.this, PersonalDetailsActivity.class));
//        });
//
//        // Attendance Report card click
//        cardAttendanceReport.setOnClickListener(v -> {
//            startActivity(new Intent(SettingsActivity.this, AttendanceReportActivity.class));
//        });
//    }
//
//    private void loadShiftTiming() {
//        dbRef.child("Companies")
//                .child(companyKey)
//                .child("employees")
//                .child(employeeMobile)
//                .child("info")
//                .child("employeeShift")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        String shiftId = snapshot.getValue(String.class);
//
//                        if (shiftId != null && !shiftId.isEmpty()) {
//                            loadShiftDetails(shiftId);
//                        } else {
//                            tvShiftTiming.setText("No shift assigned");
//                            switchNotifications.setEnabled(false);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        toast("Failed to load shift information");
//                    }
//                });
//    }
//
//    private void loadShiftDetails(String shiftId) {
//        dbRef.child("Companies")
//                .child(companyKey)
//                .child("shifts")
//                .child(shiftId)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot s) {
//                        String start = s.child("startTime").getValue(String.class);
//                        String end = s.child("endTime").getValue(String.class);
//
//                        if (start != null && end != null) {
//                            tvShiftTiming.setText(start + " - " + end);
//
//                            // If notifications are enabled, reschedule with current shift time
//                            if (pref.getNotificationsEnabled()) {
//                                scheduleReminder(start);
//                            }
//                        } else {
//                            tvShiftTiming.setText("Invalid shift data");
//                            switchNotifications.setEnabled(false);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        toast("Failed to load shift details");
//                    }
//                });
//    }
//
//    private void enableNotifications() {
//        dbRef.child("Companies")
//                .child(companyKey)
//                .child("employees")
//                .child(employeeMobile)
//                .child("info")
//                .child("employeeShift")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        String shiftId = snapshot.getValue(String.class);
//
//                        if (shiftId != null && !shiftId.isEmpty()) {
//                            getShiftStartTime(shiftId);
//                        } else {
//                            toast("No shift assigned");
//                            switchNotifications.setChecked(false);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        toast("Failed to enable notifications");
//                        switchNotifications.setChecked(false);
//                    }
//                });
//    }
//
//    private void getShiftStartTime(String shiftId) {
//        dbRef.child("Companies")
//                .child(companyKey)
//                .child("shifts")
//                .child(shiftId)
//                .child("startTime")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot s) {
//                        String startTime = s.getValue(String.class);
//
//                        if (startTime != null && !startTime.isEmpty()) {
//                            scheduleReminder(startTime);
//                            pref.setNotificationsEnabled(true);
//                            toast("Reminder enabled");
//                        } else {
//                            toast("Invalid shift time");
//                            switchNotifications.setChecked(false);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        toast("Failed to get shift time");
//                        switchNotifications.setChecked(false);
//                    }
//                });
//    }
//
//    private void scheduleReminder(String startTime) {
//        AttendanceReminderHelper.schedule(this, startTime);
//    }
//
//    private void disableNotifications() {
//        AttendanceReminderHelper.cancel(this);
//        pref.setNotificationsEnabled(false);
//        toast("Reminder disabled");
//    }
//
//    private boolean checkNotificationPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            return ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.POST_NOTIFICATIONS
//            ) == PackageManager.PERMISSION_GRANTED;
//        }
//        return true; // Permission not required for Android < 13
//    }
//
//    private void requestNotificationPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            ActivityCompat.requestPermissions(
//                    this,
//                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
//                    NOTIFICATION_PERMISSION_CODE
//            );
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                switchNotifications.setChecked(true);
//                enableNotifications();
//            } else {
//                toast("Notification permission denied");
//                switchNotifications.setChecked(false);
//            }
//        }
//    }
//
//    private void toast(String msg) {
//        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
//    }
//}
package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.AttendanceReminderHelper;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchNotifications;
    private TextView tvShiftTiming;
    private TextView tvEmployeeName;

    private DatabaseReference dbRef;
    private String companyKey, employeeMobile;
    private PrefManager pref;

    private ImageView ivProfile;
    private View cardPersonalDetails;
    private View cardAttendanceReport;
    private View cardEmployment;
    private View cardShiftTimingView;
    private View cardNotifications;

    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 🔥 FIX: Initialize dbRef FIRST
        dbRef = FirebaseDatabase.getInstance().getReference();

        pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        employeeMobile = pref.getEmployeeMobile();

        if (companyKey == null || employeeMobile == null) {
            toast("⚠️ Please login first");
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadEmployeeData();
        setupClickListeners();
    }

    private void initViews() {
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvShiftTiming = findViewById(R.id.tvShiftTiming);
        switchNotifications = findViewById(R.id.switchNotifications);
        ivProfile = findViewById(R.id.ivProfile);
        cardPersonalDetails = findViewById(R.id.cardPersonalDetails);
        cardEmployment = findViewById(R.id.cardEmployment);
        cardShiftTimingView = findViewById(R.id.cardShiftTiming);
        cardAttendanceReport = findViewById(R.id.cardAttendanceReport);
        cardNotifications = findViewById(R.id.cardNotifications);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                getSupportActionBar().setTitle("Settings");
            }
        }
    }

    private void loadEmployeeData() {
        // 🔥 SAFE: Check dbRef before using
        if (dbRef == null) {
            dbRef = FirebaseDatabase.getInstance().getReference();
        }

        if (tvEmployeeName != null) {
            tvEmployeeName.setText("Loading...");
            loadEmployeeNameFromFirebase();
        }

        loadProfileImage();
        loadShiftTiming();

        // Load notification preference
        if (switchNotifications != null) {
            boolean notificationsEnabled = pref.getNotificationsEnabled();
            switchNotifications.setChecked(notificationsEnabled);
        }
    }

    private void loadEmployeeNameFromFirebase() {
        // 🔥 DOUBLE SAFE CHECK
        if (dbRef == null || companyKey == null || employeeMobile == null) {
            if (tvEmployeeName != null) tvEmployeeName.setText("Employee");
            return;
        }

        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
                .child("info").child("employeeName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && tvEmployeeName != null) {
                            String name = snapshot.getValue(String.class);
                            if (name != null && !name.isEmpty()) {
                                tvEmployeeName.setText(name);
                            } else {
                                tvEmployeeName.setText("Employee");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (tvEmployeeName != null) {
                            tvEmployeeName.setText("Employee");
                        }
                    }
                });
    }

    private void loadProfileImage() {
        if (ivProfile == null || dbRef == null) return;

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String profileUrl = prefs.getString("profileImage", null);

        if (profileUrl == null || profileUrl.isEmpty()) {
            dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
                    .child("info").child("profileImage")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String firebaseUrl = snapshot.getValue(String.class);
                            if (firebaseUrl != null && !firebaseUrl.isEmpty()) {
                                prefs.edit().putString("profileImage", firebaseUrl).apply();
                                Glide.with(SettingsActivity.this)
                                        .load(firebaseUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_profile)
                                        .error(R.drawable.ic_profile)
                                        .into(ivProfile);
                            } else {
                                loadDefaultProfile();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadDefaultProfile();
                        }
                    });
        } else {
            Glide.with(this)
                    .load(profileUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(ivProfile);
        }
    }

    private void loadDefaultProfile() {
        if (ivProfile != null) {
            Glide.with(this)
                    .load(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivProfile);
        }
    }

    private void loadShiftTiming() {
        if (dbRef == null || tvShiftTiming == null) return;

        dbRef.child("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("info")
                .child("employeeShift")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String shiftId = snapshot.getValue(String.class);

                        if (shiftId != null && !shiftId.isEmpty()) {
                            loadShiftDetails(shiftId);
                        } else {
                            tvShiftTiming.setText("No shift assigned");
                            if (switchNotifications != null) {
                                switchNotifications.setEnabled(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toast("Failed to load shift information");
                    }
                });
    }

    private void loadShiftDetails(String shiftId) {
        if (dbRef == null) return;

        dbRef.child("Companies")
                .child(companyKey)
                .child("shifts")
                .child(shiftId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        String start = s.child("startTime").getValue(String.class);
                        String end = s.child("endTime").getValue(String.class);

                        if (start != null && end != null && tvShiftTiming != null) {
                            tvShiftTiming.setText(start + " - " + end);

                            if (pref.getNotificationsEnabled()) {
                                scheduleReminder(start);
                            }
                        } else {
                            if (tvShiftTiming != null) {
                                tvShiftTiming.setText("Invalid shift data");
                            }
                            if (switchNotifications != null) {
                                switchNotifications.setEnabled(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toast("Failed to load shift details");
                    }
                });
    }

    private void setupClickListeners() {
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> openImagePicker());
        }

        if (cardPersonalDetails != null) {
            cardPersonalDetails.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, PersonalDetailsActivity.class);
                intent.putExtra("companyKey", companyKey);
                intent.putExtra("employeeMobile", employeeMobile);
                startActivity(intent);
            });
        }

        if (cardEmployment != null) {
            cardEmployment.setOnClickListener(v -> toast("Employment → Coming Soon"));
        }

        if (cardShiftTimingView != null) {
            cardShiftTimingView.setOnClickListener(v -> toast("Change Shift"));
        }

        if (cardAttendanceReport != null) {
            cardAttendanceReport.setOnClickListener(v -> {
                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date());
                Intent intent = new Intent(SettingsActivity.this, AttendanceReportActivity.class);
                intent.putExtra("date", todayDate);
                intent.putExtra("companyKey", companyKey);
                intent.putExtra("employeeMobile", employeeMobile);
                startActivity(intent);
            });
        }

        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (checkNotificationPermission()) {
                        enableNotifications();
                    } else {
                        requestNotificationPermission();
                        switchNotifications.setChecked(false);
                    }
                } else {
                    disableNotifications();
                }
            });
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (ivProfile != null) {
                Glide.with(this)
                        .load(selectedImageUri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .into(ivProfile);
            }
            uploadProfileImageToFirebase();
        }
    }

    private void uploadProfileImageToFirebase() {
        if (selectedImageUri == null || companyKey == null || employeeMobile == null) {
            toast("Error uploading image");
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Updating Profile");
        progressDialog.setMessage("Uploading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("Companies").child(companyKey).child("profile_images").child(employeeMobile + ".jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                            prefs.edit().putString("profileImage", downloadUrl).apply();
                            saveProfileImageUrlToDatabase(downloadUrl);
                            progressDialog.dismiss();
                            toast("✅ Profile updated!");
                        }).addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            toast("Image uploaded but URL failed");
                        }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    toast("❌ Upload failed: " + e.getMessage());
                });
    }

    private void saveProfileImageUrlToDatabase(String imageUrl) {
        if (dbRef != null) {
            dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
                    .child("info").child("profileImage").setValue(imageUrl);
        }
    }

    private void enableNotifications() {
        if (dbRef == null) {
            toast("Database not available");
            return;
        }

        dbRef.child("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("info")
                .child("employeeShift")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String shiftId = snapshot.getValue(String.class);

                        if (shiftId != null && !shiftId.isEmpty()) {
                            getShiftStartTime(shiftId);
                        } else {
                            toast("No shift assigned");
                            if (switchNotifications != null) {
                                switchNotifications.setChecked(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toast("Failed to enable notifications");
                        if (switchNotifications != null) {
                            switchNotifications.setChecked(false);
                        }
                    }
                });
    }

    private void getShiftStartTime(String shiftId) {
        if (dbRef == null) return;

        dbRef.child("Companies")
                .child(companyKey)
                .child("shifts")
                .child(shiftId)
                .child("startTime")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        String startTime = s.getValue(String.class);

                        if (startTime != null && !startTime.isEmpty()) {
                            scheduleReminder(startTime);
                            pref.setNotificationsEnabled(true);
                            toast("Reminder enabled");
                        } else {
                            toast("Invalid shift time");
                            if (switchNotifications != null) {
                                switchNotifications.setChecked(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toast("Failed to get shift time");
                        if (switchNotifications != null) {
                            switchNotifications.setChecked(false);
                        }
                    }
                });
    }

    private void scheduleReminder(String startTime) {
        AttendanceReminderHelper.schedule(this, startTime);
    }

    private void disableNotifications() {
        AttendanceReminderHelper.cancel(this);
        pref.setNotificationsEnabled(false);
        toast("Reminder disabled");
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (switchNotifications != null) {
                    switchNotifications.setChecked(true);
                }
                enableNotifications();
            } else {
                toast("Notification permission denied");
                if (switchNotifications != null) {
                    switchNotifications.setChecked(false);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
