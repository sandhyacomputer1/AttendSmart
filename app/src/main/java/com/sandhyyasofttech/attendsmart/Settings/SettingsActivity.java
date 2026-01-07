//package com.sandhyyasofttech.attendsmart.Settings;
//
//import android.os.Bundle;
//import android.widget.CompoundButton;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.switchmaterial.SwitchMaterial;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//public class SettingsActivity extends AppCompatActivity {
//
//    private SwitchMaterial switchAttendance;
//    private DatabaseReference notifyRef;
//    private String companyKey;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_settings2);
//
//        // Toolbar
//        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
//        toolbar.setNavigationOnClickListener(v -> finish());
//
//        // Switch
//        switchAttendance = findViewById(R.id.switchAttendance);
//
//        // Company key
//        PrefManager prefManager = new PrefManager(this);
//        String email = prefManager.getUserEmail();
//        companyKey = email.replace(".", ",");
//
//        notifyRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("companyInfo")
//                .child("notifyAttendance");
//
//        loadNotificationSetting();
//        setupSwitchListener();
//    }
//
//    /**
//     * Load current notification setting
//     */
//    private void loadNotificationSetting() {
//        notifyRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                boolean enabled = true; // default ON
//                if (snapshot.exists()) {
//                    Boolean val = snapshot.getValue(Boolean.class);
//                    if (val != null) enabled = val;
//                }
//                switchAttendance.setChecked(enabled);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//            }
//        });
//    }
//
//    /**
//     * Update value when switch changes
//     */
//    private void setupSwitchListener() {
//        switchAttendance.setOnCheckedChangeListener(
//                (CompoundButton buttonView, boolean isChecked) -> {
//                    notifyRef.setValue(isChecked);
//                    Toast.makeText(
//                            this,
//                            isChecked ? "Notifications Enabled" : "Notifications Disabled",
//                            Toast.LENGTH_SHORT
//                    ).show();
//                }
//        );
//    }
//}
package com.sandhyyasofttech.attendsmart.Settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

public class SettingsActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private SwitchMaterial switchAttendance;
    private DatabaseReference companyRef;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        setupToolbar();
        setupCompanySession();
        loadNotificationSetting();
        setupSwitchListener();
        setupLogoutClick();
    }

    private void initializeViews() {
        topAppBar = findViewById(R.id.topAppBar);
        switchAttendance = findViewById(R.id.switchAttendance);
    }

    private void setupToolbar() {
        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCompanySession() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getUserEmail().replace(".", ",");
        companyRef = FirebaseDatabase.getInstance()
                .getReference("Companies").child(companyKey).child("companyInfo");
    }

    private void loadNotificationSetting() {
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
    }

    private void setupSwitchListener() {
        switchAttendance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            companyRef.child("notifyAttendance").setValue(isChecked)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(SettingsActivity.this,
                                    isChecked ? "Notifications ON" : "Notifications OFF",
                                    Toast.LENGTH_SHORT).show());
        });
    }

    private void setupLogoutClick() {
        findViewById(R.id.tvLogout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        new PrefManager(this).logout();
                        startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                        finishAffinity();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
