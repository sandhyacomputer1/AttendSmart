//package com.sandhyyasofttech.attendsmart.Settings;
//
//import android.content.Intent;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.text.InputType;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.bottomsheet.BottomSheetDialog;
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.card.MaterialCardView;
//import com.google.android.material.switchmaterial.SwitchMaterial;
//import com.google.android.material.textfield.TextInputEditText;
//import com.google.android.material.textfield.TextInputLayout;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.sandhyyasofttech.attendsmart.Activities.ExportDataActivity;
//import com.sandhyyasofttech.attendsmart.Activities.ProfileActivity;
//import com.sandhyyasofttech.attendsmart.Activities.WebViewActivity;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//import java.io.File;
//
//public class SettingsActivity extends AppCompatActivity {
//
//    private MaterialToolbar topAppBar;
//    private SwitchMaterial switchAttendance, switchLeaveNotifications,
//            switchAutoBackup, switchBiometric;
//    private LinearLayout cardCompanyProfile, cardChangePassword, cardExportData,
//            cardClearCache, cardPrivacyPolicy, cardTerms;
//    private TextView tvAppVersion;
//    private MaterialButton btnLogout;
//
//    private DatabaseReference companyRef;
//    private String companyKey;
//    private PrefManager pref;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_settings2);
//
//        initializeViews();
//        setupToolbar();
//        setupCompanySession();
//        loadSettings();
//        setupListeners();
//    }
//
//    private void initializeViews() {
//        topAppBar = findViewById(R.id.topAppBar);
//        switchAttendance = findViewById(R.id.switchAttendance);
//        switchLeaveNotifications = findViewById(R.id.switchLeaveNotifications);
//        switchAutoBackup = findViewById(R.id.switchAutoBackup);
//        switchBiometric = findViewById(R.id.switchBiometric);
//
//        cardCompanyProfile = findViewById(R.id.cardCompanyProfile);
//        cardChangePassword = findViewById(R.id.cardChangePassword);
//        cardExportData = findViewById(R.id.cardExportData);
//        cardClearCache = findViewById(R.id.cardClearCache);
//        cardPrivacyPolicy = findViewById(R.id.cardPrivacyPolicy);
//        cardTerms = findViewById(R.id.cardTerms);
//
//        tvAppVersion = findViewById(R.id.tvAppVersion);
//        btnLogout = findViewById(R.id.btnLogout);
//
//        // Set app version
//        try {
//            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//            tvAppVersion.setText("Version " + pInfo.versionName);
//        } catch (PackageManager.NameNotFoundException e) {
//            tvAppVersion.setText("Version 1.0.0");
//        }
//    }
//
//    private void setupToolbar() {
//        topAppBar.setNavigationOnClickListener(v -> finish());
//    }
//
//    private void setupCompanySession() {
//        pref = new PrefManager(this);
//        companyKey = pref.getUserEmail().replace(".", ",");
//        companyRef = FirebaseDatabase.getInstance()
//                .getReference("Companies").child(companyKey).child("companyInfo");
//    }
//
//    private void loadSettings() {
//        // Load Attendance Notification Setting
//        companyRef.child("notifyAttendance").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                boolean enabled = snapshot.getValue(Boolean.class) != null
//                        ? snapshot.getValue(Boolean.class) : true;
//                switchAttendance.setChecked(enabled);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {}
//        });
//
//        // Load Leave Notification Setting
//        companyRef.child("notifyLeave").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                boolean enabled = snapshot.getValue(Boolean.class) != null
//                        ? snapshot.getValue(Boolean.class) : true;
//                switchLeaveNotifications.setChecked(enabled);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {}
//        });
//
//        // Load Auto Backup Setting
//        companyRef.child("autoBackup").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                boolean enabled = snapshot.getValue(Boolean.class) != null
//                        ? snapshot.getValue(Boolean.class) : false;
//                switchAutoBackup.setChecked(enabled);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {}
//        });
//
//        // Load Biometric Setting (from SharedPreferences)
//        switchBiometric.setChecked(pref.isBiometricEnabled());
//    }
//
//    private void setupListeners() {
//        // Attendance Notification Switch
//        switchAttendance.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (buttonView.isPressed()) {
//                companyRef.child("notifyAttendance").setValue(isChecked)
//                        .addOnSuccessListener(aVoid ->
//                                Toast.makeText(SettingsActivity.this,
//                                        isChecked ? "Attendance Notifications ON" : "Attendance Notifications OFF",
//                                        Toast.LENGTH_SHORT).show());
//            }
//        });
//
//        // Leave Notification Switch
//        switchLeaveNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (buttonView.isPressed()) {
//                companyRef.child("notifyLeave").setValue(isChecked)
//                        .addOnSuccessListener(aVoid ->
//                                Toast.makeText(SettingsActivity.this,
//                                        isChecked ? "Leave Notifications ON" : "Leave Notifications OFF",
//                                        Toast.LENGTH_SHORT).show());
//            }
//        });
//
//        // Auto Backup Switch
//        switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (buttonView.isPressed()) {
//                companyRef.child("autoBackup").setValue(isChecked)
//                        .addOnSuccessListener(aVoid ->
//                                Toast.makeText(SettingsActivity.this,
//                                        isChecked ? "Auto Backup Enabled" : "Auto Backup Disabled",
//                                        Toast.LENGTH_SHORT).show());
//            }
//        });
//
//        // Biometric Switch
//        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (buttonView.isPressed()) {
//                pref.setBiometricEnabled(isChecked);
//                Toast.makeText(SettingsActivity.this,
//                        isChecked ? "Biometric Login Enabled" : "Biometric Login Disabled",
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // Company Profile Click
//        cardCompanyProfile.setOnClickListener(v -> openCompanyProfile());
//
//        // Change Password Click
//        cardChangePassword.setOnClickListener(v -> showChangePasswordBottomSheet());
//
//        // Export Data Click
//        cardExportData.setOnClickListener(v -> openExportData());
//
//        // Clear Cache Click
//        cardClearCache.setOnClickListener(v -> showClearCacheDialog());
//
//        // Privacy Policy Click
//        cardPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());
//
//        // Terms & Conditions Click
//        cardTerms.setOnClickListener(v -> openTermsAndConditions());
//
//        // Logout Click
//        btnLogout.setOnClickListener(v -> showLogoutDialog());
//    }
//
//    private void openCompanyProfile() {
//        Intent intent = new Intent(this, ProfileActivity.class);
//        startActivity(intent);
//    }
//
//    private void showChangePasswordBottomSheet() {
//        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
//        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_change_password, null);
//        bottomSheetDialog.setContentView(view);
//
//        TextInputLayout tilCurrentPassword = view.findViewById(R.id.tilCurrentPassword);
//        TextInputLayout tilNewPassword = view.findViewById(R.id.tilNewPassword);
//        TextInputLayout tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);
//
//        TextInputEditText etCurrentPassword = view.findViewById(R.id.etCurrentPassword);
//        TextInputEditText etNewPassword = view.findViewById(R.id.etNewPassword);
//        TextInputEditText etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
//
//        MaterialButton btnChange = view.findViewById(R.id.btnChangePassword);
//        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
//
//        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
//
//        btnChange.setOnClickListener(v -> {
//            String currentPass = etCurrentPassword.getText().toString().trim();
//            String newPass = etNewPassword.getText().toString().trim();
//            String confirmPass = etConfirmPassword.getText().toString().trim();
//
//            // Validation
//            if (currentPass.isEmpty()) {
//                tilCurrentPassword.setError("Enter current password");
//                return;
//            } else {
//                tilCurrentPassword.setError(null);
//            }
//
//            if (newPass.isEmpty()) {
//                tilNewPassword.setError("Enter new password");
//                return;
//            } else {
//                tilNewPassword.setError(null);
//            }
//
//            if (confirmPass.isEmpty()) {
//                tilConfirmPassword.setError("Confirm your password");
//                return;
//            } else {
//                tilConfirmPassword.setError(null);
//            }
//
//            if (!newPass.equals(confirmPass)) {
//                tilConfirmPassword.setError("Passwords don't match");
//                return;
//            }
//
//            if (newPass.length() < 6) {
//                tilNewPassword.setError("Password must be at least 6 characters");
//                return;
//            }
//
//            // Verify and change password
//            changePassword(currentPass, newPass, bottomSheetDialog);
//        });
//
//        bottomSheetDialog.show();
//    }
//
//    private void changePassword(String currentPass, String newPass, BottomSheetDialog dialog) {
//        companyRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                String storedPassword = snapshot.getValue(String.class);
//
//                if (storedPassword != null && storedPassword.equals(currentPass)) {
//                    // Update password in Firebase
//                    companyRef.child("password").setValue(newPass)
//                            .addOnSuccessListener(aVoid -> {
//                                Toast.makeText(SettingsActivity.this,
//                                        "Password changed successfully", Toast.LENGTH_SHORT).show();
//                                dialog.dismiss();
//                            })
//                            .addOnFailureListener(e -> {
//                                Toast.makeText(SettingsActivity.this,
//                                        "Failed to change password", Toast.LENGTH_SHORT).show();
//                            });
//                } else {
//                    Toast.makeText(SettingsActivity.this,
//                            "Current password is incorrect", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(SettingsActivity.this,
//                        "Error verifying password", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void openExportData() {
//        Intent intent = new Intent(this, ExportDataActivity.class);
//        startActivity(intent);
//    }
//
//    private void showClearCacheDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Clear Cache")
//                .setMessage("This will clear temporary files and free up storage space. Continue?")
//                .setPositiveButton("Clear", (dialog, which) -> {
//                    try {
//                        File cacheDir = getCacheDir();
//                        if (cacheDir != null && cacheDir.isDirectory()) {
//                            deleteDir(cacheDir);
//                        }
//                        Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
//                    } catch (Exception e) {
//                        Toast.makeText(this, "Failed to clear cache", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .setNegativeButton("Cancel", null)
//                .show();
//    }
//
//    private boolean deleteDir(File dir) {
//        if (dir != null && dir.isDirectory()) {
//            String[] children = dir.list();
//            if (children != null) {
//                for (String child : children) {
//                    boolean success = deleteDir(new File(dir, child));
//                    if (!success) {
//                        return false;
//                    }
//                }
//            }
//            return dir.delete();
//        } else if (dir != null && dir.isFile()) {
//            return dir.delete();
//        } else {
//            return false;
//        }
//    }
//
//    private void openPrivacyPolicy() {
//        Intent intent = new Intent(this, WebViewActivity.class);
//        intent.putExtra("title", "Privacy Policy");
//        intent.putExtra("url", "https://yourcompany.com/privacy-policy");
//        startActivity(intent);
//    }
//
//    private void openTermsAndConditions() {
//        Intent intent = new Intent(this, WebViewActivity.class);
//        intent.putExtra("title", "Terms & Conditions");
//        intent.putExtra("url", "https://yourcompany.com/terms");
//        startActivity(intent);
//    }
//
//    private void showLogoutDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Logout")
//                .setMessage("Are you sure you want to logout?")
//                .setPositiveButton("Yes", (dialog, which) -> {
//                    pref.logout();
//                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    finish();
//                })
//                .setNegativeButton("Cancel", null)
//                .show();
//    }
//}




package com.sandhyyasofttech.attendsmart.Settings;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Activities.ExportDataActivity;
import com.sandhyyasofttech.attendsmart.Activities.ProfileActivity;
import com.sandhyyasofttech.attendsmart.Activities.WebViewActivity;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private SwitchMaterial switchAttendance, switchLeaveNotifications,
            switchAutoBackup, switchBiometric;
    private LinearLayout cardCompanyProfile, cardChangePassword, cardExportData,
            cardClearCache, cardPrivacyPolicy, cardTerms;
    private TextView tvAppVersion;
    private MaterialButton btnLogout;

    private DatabaseReference companyRef;
    private String companyKey;
    private PrefManager pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings2);

        initializeViews();
        setupToolbar();
        setupCompanySession();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        topAppBar = findViewById(R.id.topAppBar);
        switchAttendance = findViewById(R.id.switchAttendance);
        switchLeaveNotifications = findViewById(R.id.switchLeaveNotifications);
        switchAutoBackup = findViewById(R.id.switchAutoBackup);
        switchBiometric = findViewById(R.id.switchBiometric);

        cardCompanyProfile = findViewById(R.id.cardCompanyProfile);
        cardChangePassword = findViewById(R.id.cardChangePassword);
        cardExportData = findViewById(R.id.cardExportData);
        cardClearCache = findViewById(R.id.cardClearCache);
        cardPrivacyPolicy = findViewById(R.id.cardPrivacyPolicy);
        cardTerms = findViewById(R.id.cardTerms);

        tvAppVersion = findViewById(R.id.tvAppVersion);
        btnLogout = findViewById(R.id.btnLogout);

        // Set app version
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvAppVersion.setText("Version " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvAppVersion.setText("Version 1.0.0");
        }
    }

    private void setupToolbar() {
        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCompanySession() {
        pref = new PrefManager(this);
        companyKey = pref.getUserEmail().replace(".", ",");
        companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies").child(companyKey).child("companyInfo");
    }

    private void loadSettings() {
        // Load Attendance Notification Setting
        companyRef.child("notifyAttendance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean enabled = snapshot.getValue(Boolean.class) != null
                        ? snapshot.getValue(Boolean.class) : true;
                switchAttendance.setChecked(enabled);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Load Leave Notification Setting
        companyRef.child("notifyLeave").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean enabled = snapshot.getValue(Boolean.class) != null
                        ? snapshot.getValue(Boolean.class) : true;
                switchLeaveNotifications.setChecked(enabled);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Load Auto Backup Setting
        companyRef.child("autoBackup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean enabled = snapshot.getValue(Boolean.class) != null
                        ? snapshot.getValue(Boolean.class) : false;
                switchAutoBackup.setChecked(enabled);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Load Biometric Setting (from SharedPreferences)
        switchBiometric.setChecked(pref.isBiometricEnabled());
    }

    private void setupListeners() {
        // Attendance Notification Switch
        switchAttendance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                companyRef.child("notifyAttendance").setValue(isChecked)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(SettingsActivity.this,
                                        isChecked ? "Attendance Notifications ON" : "Attendance Notifications OFF",
                                        Toast.LENGTH_SHORT).show());
            }
        });

        // Leave Notification Switch
        switchLeaveNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                companyRef.child("notifyLeave").setValue(isChecked)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(SettingsActivity.this,
                                        isChecked ? "Leave Notifications ON" : "Leave Notifications OFF",
                                        Toast.LENGTH_SHORT).show());
            }
        });

        // Auto Backup Switch
        switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                companyRef.child("autoBackup").setValue(isChecked)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(SettingsActivity.this,
                                        isChecked ? "Auto Backup Enabled" : "Auto Backup Disabled",
                                        Toast.LENGTH_SHORT).show());
            }
        });

        // Biometric Switch
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                pref.setBiometricEnabled(isChecked);
                Toast.makeText(SettingsActivity.this,
                        isChecked ? "Biometric Login Enabled" : "Biometric Login Disabled",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Company Profile Click
        cardCompanyProfile.setOnClickListener(v -> openCompanyProfile());

        // Change Password Click
        cardChangePassword.setOnClickListener(v -> showChangePasswordBottomSheet());

        // Export Data Click
        cardExportData.setOnClickListener(v -> openExportData());

        // Clear Cache Click
        cardClearCache.setOnClickListener(v -> showClearCacheDialog());

        // Privacy Policy Click
        cardPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());

        // Terms & Conditions Click
        cardTerms.setOnClickListener(v -> openTermsAndConditions());

        // Logout Click
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void openCompanyProfile() {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }

    private void showChangePasswordBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_change_password, null);
        bottomSheetDialog.setContentView(view);

        TextInputLayout tilCurrentPassword = view.findViewById(R.id.tilCurrentPassword);
        TextInputLayout tilNewPassword = view.findViewById(R.id.tilNewPassword);
        TextInputLayout tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);

        TextInputEditText etCurrentPassword = view.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = view.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = view.findViewById(R.id.etConfirmPassword);

        TextView tvCurrentPasswordHint = view.findViewById(R.id.tvCurrentPasswordHint);
        ProgressBar progressBarPassword = view.findViewById(R.id.progressBarPassword);

        MaterialButton btnChange = view.findViewById(R.id.btnChangePassword);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        // Fetch and display current password from Firebase
        progressBarPassword.setVisibility(View.VISIBLE);
        tvCurrentPasswordHint.setVisibility(View.GONE);

        companyRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBarPassword.setVisibility(View.GONE);
                String currentPassword = snapshot.getValue(String.class);

                if (currentPassword != null) {
                    tvCurrentPasswordHint.setVisibility(View.VISIBLE);
                    tvCurrentPasswordHint.setText("Current Password: " + currentPassword);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBarPassword.setVisibility(View.GONE);
                Toast.makeText(SettingsActivity.this,
                        "Error loading password", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnChange.setOnClickListener(v -> {
            String currentPass = etCurrentPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            // Validation
            if (currentPass.isEmpty()) {
                tilCurrentPassword.setError("Enter current password");
                return;
            } else {
                tilCurrentPassword.setError(null);
            }

            if (newPass.isEmpty()) {
                tilNewPassword.setError("Enter new password");
                return;
            } else {
                tilNewPassword.setError(null);
            }

            if (confirmPass.isEmpty()) {
                tilConfirmPassword.setError("Confirm your password");
                return;
            } else {
                tilConfirmPassword.setError(null);
            }

            if (!newPass.equals(confirmPass)) {
                tilConfirmPassword.setError("Passwords don't match");
                return;
            }

            if (newPass.length() < 6) {
                tilNewPassword.setError("Password must be at least 6 characters");
                return;
            }

            // Verify and change password
            changePassword(currentPass, newPass, bottomSheetDialog);
        });

        bottomSheetDialog.show();
    }

    private void changePassword(String currentPass, String newPass, BottomSheetDialog dialog) {
        companyRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String storedPassword = snapshot.getValue(String.class);

                if (storedPassword != null && storedPassword.equals(currentPass)) {
                    // Update password in Firebase
                    companyRef.child("password").setValue(newPass)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(SettingsActivity.this,
                                        "Password changed successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(SettingsActivity.this,
                                        "Failed to change password", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(SettingsActivity.this,
                            "Current password is incorrect", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SettingsActivity.this,
                        "Error verifying password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openExportData() {
        Intent intent = new Intent(this, ExportDataActivity.class);
        startActivity(intent);
    }

    private void showClearCacheDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Cache")
                .setMessage("This will clear temporary files and free up storage space. Continue?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    try {
                        File cacheDir = getCacheDir();
                        if (cacheDir != null && cacheDir.isDirectory()) {
                            deleteDir(cacheDir);
                        }
                        Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to clear cache", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private void openPrivacyPolicy() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("title", "Privacy Policy");
        intent.putExtra("url", "https://yourcompany.com/privacy-policy");
        startActivity(intent);
    }

    private void openTermsAndConditions() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("title", "Terms & Conditions");
        intent.putExtra("url", "https://yourcompany.com/terms");
        startActivity(intent);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    pref.logout();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}