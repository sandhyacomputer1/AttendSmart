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
import androidx.appcompat.app.AlertDialog;
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
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.AttendanceReminderHelper;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    // Add these 2 lines with other card variables
    private View cardAddTodaysWork;
    private View cardViewWork;

    private Switch switchNotifications;
    private TextView tvShiftTiming;
    private TextView tvEmployeeName;

    private DatabaseReference dbRef;
    private String companyKey, employeeMobile;
    private PrefManager pref;

    private ImageView ivProfile;
    private View cardPersonalDetails;
    private View cardAttendanceReport;
    private View cardApplyLeave;
    private View cardEmployment;
    private View cardNotifications;
    private View cardMyLeaves;
    private View cardMySalary;
    private View cardLogout;

    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private Uri selectedImageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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
        cardAttendanceReport = findViewById(R.id.cardAttendanceReport);
        cardApplyLeave = findViewById(R.id.cardApplyLeave);
        cardMyLeaves = findViewById(R.id.cardMyLeaves);
        cardMySalary = findViewById(R.id.cardMySalary);
        cardNotifications = findViewById(R.id.cardNotifications);
        cardLogout = findViewById(R.id.cardLogout);

        cardAddTodaysWork = findViewById(R.id.cardAddTodaysWork);
        cardViewWork = findViewById(R.id.cardViewWork);
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
        if (dbRef == null) {
            dbRef = FirebaseDatabase.getInstance().getReference();
        }

        if (tvEmployeeName != null) {
            tvEmployeeName.setText("Loading...");
            loadEmployeeNameFromFirebase();
        }

        loadProfileImage();
        loadShiftTiming();

        if (switchNotifications != null) {
            boolean notificationsEnabled = pref.getNotificationsEnabled();
            switchNotifications.setChecked(notificationsEnabled);
        }
    }

    private void loadEmployeeNameFromFirebase() {
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
                            tvShiftTiming.setText(start + " - " + end); // Shows both times

                            if (pref.getNotificationsEnabled()) {
                                // ✅ Pass context and activity reference
                                getShiftTimes(shiftId);
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

    private void getShiftTimes(String shiftId) {
        if (dbRef == null) return;

        dbRef.child("Companies")
                .child(companyKey)
                .child("shifts")
                .child(shiftId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        String startTime = s.child("startTime").getValue(String.class);
                        String endTime = s.child("endTime").getValue(String.class);

                        if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                            // ✅ CORRECT - Pass 'this' context
                            AttendanceReminderHelper.scheduleCheckinReminder(SettingsActivity.this, startTime);
                            AttendanceReminderHelper.scheduleCheckoutReminder(SettingsActivity.this, endTime);
                            pref.setNotificationsEnabled(true);
                            toast("✅ Check-in & Check-out reminders enabled");
                        } else {
                            toast("Invalid shift times");
                            if (switchNotifications != null) {
                                switchNotifications.setChecked(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toast("Failed to get shift times");
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
            cardEmployment.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, CurrentEmploymentActivity.class);
                intent.putExtra("companyKey", companyKey);
                intent.putExtra("employeeMobile", employeeMobile);
                startActivity(intent);
            });
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

        if (cardApplyLeave != null) {
            cardApplyLeave.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, ApplyLeaveActivity.class);
                intent.putExtra("companyKey", companyKey);
                intent.putExtra("employeeMobile", employeeMobile);
                startActivity(intent);
            });
        }

        if (cardMySalary != null) {
            cardMySalary.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, EmployeeSalaryListActivity.class);
                startActivity(intent);
            });
        }

        if (cardMyLeaves != null) {
            cardMyLeaves.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, MyLeavesActivity.class);
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
        // ✅ NEW WORK SECTION - Add BEFORE switchNotifications
        if (cardAddTodaysWork != null) {
            cardAddTodaysWork.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, EmployeeTodayWorkActivity.class);
                intent.putExtra("companyKey", companyKey);
                intent.putExtra("employeeMobile", employeeMobile);
                startActivity(intent);
            });
        }

        if (cardViewWork != null) {
            cardViewWork.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, EmployeeAllWorksActivity.class);
                intent.putExtra("companyKey", companyKey);
                intent.putExtra("employeeMobile", employeeMobile);
                startActivity(intent);
            });
        }


        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .setCancelable(true)
                .show();
    }

    private void performLogout() {
        // Clear all preferences
        pref.logout();

        // Clear profile image cache
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Cancel any scheduled notifications
        AttendanceReminderHelper.cancel(this);

        toast("✅ Logged out successfully");

        // Navigate to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
                            getShiftTimes(shiftId); // Updated: gets both start AND end time
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
        // ✅ Pass context
        AttendanceReminderHelper.cancelAllReminders(this);
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