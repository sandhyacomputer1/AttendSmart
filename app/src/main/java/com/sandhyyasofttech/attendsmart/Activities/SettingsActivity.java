////package com.sandhyyasofttech.attendsmart.Activities;
////
////import android.app.ProgressDialog;
////import android.content.Intent;
////import android.content.SharedPreferences;
////import android.net.Uri;
////import android.os.Bundle;
////import android.provider.MediaStore;
////import android.view.MenuItem;
////import android.view.View;
////import android.widget.ImageView;
////import android.widget.Switch;
////import android.widget.TextView;
////import android.widget.Toast;
////import androidx.appcompat.app.AppCompatActivity;
////import androidx.appcompat.widget.Toolbar;
////import com.bumptech.glide.Glide;
////import com.google.firebase.database.DataSnapshot;
////import com.google.firebase.database.DatabaseError;
////import com.google.firebase.database.DatabaseReference;
////import com.google.firebase.database.FirebaseDatabase;
////import com.google.firebase.database.ValueEventListener;
////import com.google.firebase.storage.FirebaseStorage;
////import com.google.firebase.storage.StorageReference;
////import com.sandhyyasofttech.attendsmart.R;
////import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
////import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
////
////public class SettingsActivity extends AppCompatActivity {
////
////    private TextView tvEmployeeName, tvShiftTiming;
////    private Switch switchNotifications;
////    private ImageView ivProfile;
////    private DatabaseReference dbRef;
////    private String companyKey, employeeMobile;
////
////    private static final int PICK_IMAGE_REQUEST = 1001;
////    private Uri selectedImageUri;
////
////    @Override
////    protected void onCreate(Bundle savedInstanceState) {
////        super.onCreate(savedInstanceState);
////        setContentView(R.layout.activity_settings);
////
////        PrefManager pref = new PrefManager(this);
////        companyKey = pref.getCompanyKey();
////        employeeMobile = pref.getEmployeeMobile();
////
////        if (companyKey == null || employeeMobile == null) {
////            toast("⚠️ Please login first");
////            startActivity(new Intent(this, LoginActivity.class));
////            finish();
////            return;
////        }
////
////        initViews();
////        setupToolbar();
////        loadEmployeeData();
////        setupClickListeners();
////    }
////
////    private void initViews() {
////        tvEmployeeName = findViewById(R.id.tvEmployeeName);
////        tvShiftTiming = findViewById(R.id.tvShiftTiming);
////        switchNotifications = findViewById(R.id.switchNotifications);
////        ivProfile = findViewById(R.id.ivProfile);
////        dbRef = FirebaseDatabase.getInstance().getReference();
////    }
////
////    private void setupToolbar() {
////        Toolbar toolbar = findViewById(R.id.toolbar);
////        if (toolbar != null) {
////            setSupportActionBar(toolbar);
////            if (getSupportActionBar() != null) {
////                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
////                getSupportActionBar().setDisplayShowTitleEnabled(false);
////            }
////        }
////    }
////
////    private void loadEmployeeData() {
////        PrefManager pref = new PrefManager(this);
////
////        if (tvEmployeeName != null) {
////            tvEmployeeName.setText(pref.getCompanyKey()); // Use available method
////            loadEmployeeNameFromFirebase();
////        }
////
////        String profileUrl = pref.getCompanyKey(); // Fallback to available method
////        if (!profileUrl.isEmpty() && ivProfile != null) {
////            Glide.with(this).load(profileUrl).circleCrop().placeholder(R.drawable.ic_profile).into(ivProfile);
////        }
////
////        loadShiftTiming();
////
////        if (switchNotifications != null) {
////            SharedPreferences sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
////            switchNotifications.setChecked(sharedPrefs.getBoolean("notifications", true));
////        }
////    }
////
////    private void loadEmployeeNameFromFirebase() {
////        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
////                .child("info").child("employeeName")
////                .addListenerForSingleValueEvent(new ValueEventListener() {
////                    @Override
////                    public void onDataChange(DataSnapshot snapshot) {
////                        if (snapshot.exists() && tvEmployeeName != null) {
////                            String name = snapshot.getValue(String.class);
////                            if (name != null) {
////                                tvEmployeeName.setText(name);
////                            }
////                        }
////                    }
////                    @Override public void onCancelled(DatabaseError error) {}
////                });
////    }
////
////    private void loadShiftTiming() {
////        if (tvShiftTiming == null) return;
////
////        PrefManager pref = new PrefManager(this);
////        String employeeShift = "1"; // Default fallback
////
////        dbRef.child("Companies").child(companyKey).child("shifts").child(employeeShift)
////                .addListenerForSingleValueEvent(new ValueEventListener() {
////                    @Override
////                    public void onDataChange(DataSnapshot snapshot) {
////                        if (snapshot.exists() && tvShiftTiming != null) {
////                            String startTime = snapshot.child("startTime").getValue(String.class);
////                            String endTime = snapshot.child("endTime").getValue(String.class);
////                            if (startTime != null && endTime != null) {
////                                tvShiftTiming.setText(startTime + " - " + endTime);
////                            }
////                        }
////                    }
////                    @Override public void onCancelled(DatabaseError error) {}
////                });
////    }
////
////    private void setupClickListeners() {
////        if (ivProfile != null) {
////            ivProfile.setOnClickListener(v -> openImagePicker());
////        }
////
////        findViewById(R.id.cardPersonalDetails).setOnClickListener(v -> {
////            Intent intent = new Intent(SettingsActivity.this, PersonalDetailsActivity.class);
////            intent.putExtra("companyKey", companyKey);
////            intent.putExtra("employeeMobile", employeeMobile);
////            startActivity(intent);
////        });
////
////        findViewById(R.id.cardEmployment).setOnClickListener(v ->
////                toast("Employment → Coming Soon"));
////        findViewById(R.id.cardShiftTiming).setOnClickListener(v ->
////                toast("Change Shift"));
////        findViewById(R.id.cardAttendanceReport).setOnClickListener(v ->
////                toast("Attendance Report"));
////
////        if (switchNotifications != null) {
////            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
////                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
////                prefs.edit().putBoolean("notifications", isChecked).apply();
////            });
////        }
////    }
////
////    private void openImagePicker() {
////        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
////        startActivityForResult(intent, PICK_IMAGE_REQUEST);
////    }
////
////    @Override
////    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
////        super.onActivityResult(requestCode, resultCode, data);
////        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
////            selectedImageUri = data.getData();
////            if (ivProfile != null) {
////                Glide.with(this).load(selectedImageUri).circleCrop().placeholder(R.drawable.ic_profile).into(ivProfile);
////            }
////            uploadProfileImageToFirebase();
////        }
////    }
////
////    private void uploadProfileImageToFirebase() {
////        if (selectedImageUri == null) return;
////
////        ProgressDialog progressDialog = new ProgressDialog(this);
////        progressDialog.setTitle("Updating Profile");
////        progressDialog.setMessage("Uploading...");
////        progressDialog.show();
////
////        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
////                .child("Companies").child(companyKey).child("profile_images").child(employeeMobile + ".jpg");
////
////        storageRef.putFile(selectedImageUri)
////                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
////                    String downloadUrl = uri.toString();
////                    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
////                    prefs.edit().putString("profileImage", downloadUrl).apply();
////                    saveProfileImageUrlToDatabase(downloadUrl);
////                    progressDialog.dismiss();
////                    toast("✅ Profile updated!");
////                }).addOnFailureListener(e -> {
////                    progressDialog.dismiss();
////                    toast("Image uploaded");
////                }))
////                .addOnFailureListener(e -> {
////                    progressDialog.dismiss();
////                    toast("❌ Upload failed");
////                });
////    }
////
////    private void saveProfileImageUrlToDatabase(String imageUrl) {
////        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
////                .child("info").child("profileImage").setValue(imageUrl);
////    }
////
////    @Override
////    public boolean onOptionsItemSelected(MenuItem item) {
////        if (item.getItemId() == android.R.id.home) {
////            onBackPressed();
////            return true;
////        }
////        return super.onOptionsItemSelected(item);
////    }
////
////    private void toast(String msg) {
////        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
////    }
////}
//
//
//
//package com.sandhyyasofttech.attendsmart.Activities;
//
//import android.app.ProgressDialog;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.Switch;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;
//import com.bumptech.glide.Glide;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//public class SettingsActivity extends AppCompatActivity {
//
//    private TextView tvEmployeeName, tvShiftTiming;
//    private Switch switchNotifications;
//    private ImageView ivProfile;
//    private DatabaseReference dbRef;
//    private String companyKey, employeeMobile;
//
//    private static final int PICK_IMAGE_REQUEST = 1001;
//    private Uri selectedImageUri;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_settings);
//
//        PrefManager pref = new PrefManager(this);
//        companyKey = pref.getCompanyKey();
//        employeeMobile = pref.getEmployeeMobile();
//
//        if (companyKey == null || employeeMobile == null) {
//            toast("⚠️ Please login first");
//            startActivity(new Intent(this, LoginActivity.class));
//            finish();
//            return;
//        }
//
//        initViews();
//        setupToolbar();  // ✅ FIXED: Toolbar title + back button
//        loadEmployeeData();
//        setupClickListeners();
//    }
//
//    private void initViews() {
//        tvEmployeeName = findViewById(R.id.tvEmployeeName);
//        tvShiftTiming = findViewById(R.id.tvShiftTiming);
//        switchNotifications = findViewById(R.id.switchNotifications);
//        ivProfile = findViewById(R.id.ivProfile);
//        dbRef = FirebaseDatabase.getInstance().getReference();
//    }
//
//    private void setupToolbar() {
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        if (toolbar != null) {
//            setSupportActionBar(toolbar);
//            if (getSupportActionBar() != null) {
//                getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // ✅ Back button
//                getSupportActionBar().setDisplayShowTitleEnabled(true); // ✅ SHOW TITLE
//                getSupportActionBar().setTitle("Settings");              // ✅ SETTINGS TITLE
//            }
//        }
//    }
//
//    private void loadEmployeeData() {
//        // ✅ FIXED: Load employee name from Firebase FIRST
//        if (tvEmployeeName != null) {
//            tvEmployeeName.setText("Loading...");
//            loadEmployeeNameFromFirebase();  // This will update name
//        }
//
//        // ✅ FIXED: Load profile image from SharedPreferences + Firebase
//        loadProfileImage();
//
//        loadShiftTiming();
//
//        // Notifications
//        if (switchNotifications != null) {
//            SharedPreferences sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
//            switchNotifications.setChecked(sharedPrefs.getBoolean("notifications", true));
//        }
//    }
//
//    private void loadEmployeeNameFromFirebase() {
//        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
//                .child("info").child("employeeName")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot snapshot) {
//                        if (snapshot.exists() && tvEmployeeName != null) {
//                            String name = snapshot.getValue(String.class);
//                            if (name != null && !name.isEmpty()) {
//                                tvEmployeeName.setText(name);
//                                // Cache name
//                                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
//                                prefs.edit().putString("employeeName", name).apply();
//                            } else {
//                                tvEmployeeName.setText("Employee");
//                            }
//                        }
//                    }
//                    @Override
//                    public void onCancelled(DatabaseError error) {
//                        if (tvEmployeeName != null) {
//                            tvEmployeeName.setText("Employee");
//                        }
//                    }
//                });
//    }
//
//    private void loadProfileImage() {
//        if (ivProfile == null) return;
//
//        // 1. Try SharedPreferences first
//        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
//        String profileUrl = prefs.getString("profileImage", null);
//
//        // 2. Try Firebase if SharedPreferences empty
//        if (profileUrl == null || profileUrl.isEmpty()) {
//            dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
//                    .child("info").child("profileImage")
//                    .addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(DataSnapshot snapshot) {
//                            String firebaseUrl = snapshot.getValue(String.class);
//                            if (firebaseUrl != null && !firebaseUrl.isEmpty()) {
//                                // Cache and load
//                                prefs.edit().putString("profileImage", firebaseUrl).apply();
//                                Glide.with(SettingsActivity.this)
//                                        .load(firebaseUrl)
//                                        .circleCrop()
//                                        .placeholder(R.drawable.ic_profile)
//                                        .error(R.drawable.ic_profile)
//                                        .into(ivProfile);
//                            } else {
//                                // Default profile
//                                Glide.with(SettingsActivity.this)
//                                        .load(R.drawable.ic_profile)
//                                        .circleCrop()
//                                        .into(ivProfile);
//                            }
//                        }
//                        @Override public void onCancelled(DatabaseError error) {
//                            loadDefaultProfile();
//                        }
//                    });
//        } else {
//            // Load from SharedPreferences
//            Glide.with(this)
//                    .load(profileUrl)
//                    .circleCrop()
//                    .placeholder(R.drawable.ic_profile)
//                    .error(R.drawable.ic_profile)
//                    .into(ivProfile);
//        }
//    }
//
//    private void loadDefaultProfile() {
//        if (ivProfile != null) {
//            Glide.with(this)
//                    .load(R.drawable.ic_profile)
//                    .circleCrop()
//                    .into(ivProfile);
//        }
//    }
//
//    private void loadShiftTiming() {
//        if (tvShiftTiming == null) return;
//
//        // Try SharedPreferences first
//        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
//        String employeeShift = prefs.getString("employeeShift", "1");
//
//        dbRef.child("Companies").child(companyKey).child("shifts").child(employeeShift)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot snapshot) {
//                        if (snapshot.exists() && tvShiftTiming != null) {
//                            String startTime = snapshot.child("startTime").getValue(String.class);
//                            String endTime = snapshot.child("endTime").getValue(String.class);
//                            if (startTime != null && endTime != null) {
//                                tvShiftTiming.setText(startTime + " - " + endTime);
//                            } else {
//                                tvShiftTiming.setText("Shift 1");
//                            }
//                        } else {
//                            tvShiftTiming.setText("Shift 1");
//                        }
//                    }
//                    @Override public void onCancelled(DatabaseError error) {
//                        if (tvShiftTiming != null) {
//                            tvShiftTiming.setText("Shift 1");
//                        }
//                    }
//                });
//    }
//
//    private void setupClickListeners() {
//        if (ivProfile != null) {
//            ivProfile.setOnClickListener(v -> openImagePicker());
//        }
//
//        // Personal Details
//        View cardPersonalDetails = findViewById(R.id.cardPersonalDetails);
//        if (cardPersonalDetails != null) {
//            cardPersonalDetails.setOnClickListener(v -> {
//                Intent intent = new Intent(SettingsActivity.this, PersonalDetailsActivity.class);
//                intent.putExtra("companyKey", companyKey);
//                intent.putExtra("employeeMobile", employeeMobile);
//                startActivity(intent);
//            });
//        }
//
//        // Other cards
//        View cardEmployment = findViewById(R.id.cardEmployment);
//        if (cardEmployment != null) {
//            cardEmployment.setOnClickListener(v -> toast("Employment → Coming Soon"));
//        }
//
//        View cardShiftTiming = findViewById(R.id.cardShiftTiming);
//        if (cardShiftTiming != null) {
//            cardShiftTiming.setOnClickListener(v -> toast("Change Shift"));
//        }
//
//        View cardAttendanceReport = findViewById(R.id.cardAttendanceReport);
//        if (cardAttendanceReport != null) {
//            cardAttendanceReport.setOnClickListener(v -> toast("Attendance Report"));
//        }
//
//        // Notifications switch
//        if (switchNotifications != null) {
//            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
//                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
//                prefs.edit().putBoolean("notifications", isChecked).apply();
//            });
//        }
//    }
//
//    private void openImagePicker() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(intent, PICK_IMAGE_REQUEST);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
//            selectedImageUri = data.getData();
//            if (ivProfile != null) {
//                Glide.with(this)
//                        .load(selectedImageUri)
//                        .circleCrop()
//                        .placeholder(R.drawable.ic_profile)
//                        .into(ivProfile);
//            }
//            uploadProfileImageToFirebase();
//        }
//    }
//
//    private void uploadProfileImageToFirebase() {
//        if (selectedImageUri == null || companyKey == null || employeeMobile == null) {
//            toast("Error uploading image");
//            return;
//        }
//
//        ProgressDialog progressDialog = new ProgressDialog(this);
//        progressDialog.setTitle("Updating Profile");
//        progressDialog.setMessage("Uploading...");
//        progressDialog.setCancelable(false);
//        progressDialog.show();
//
//        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
//                .child("Companies").child(companyKey).child("profile_images").child(employeeMobile + ".jpg");
//
//        storageRef.putFile(selectedImageUri)
//                .addOnSuccessListener(taskSnapshot ->
//                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
//                            String downloadUrl = uri.toString();
//                            // Cache in SharedPreferences
//                            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
//                            prefs.edit().putString("profileImage", downloadUrl).apply();
//
//                            // Save to Firebase
//                            saveProfileImageUrlToDatabase(downloadUrl);
//                            progressDialog.dismiss();
//                            toast("✅ Profile updated!");
//                        }).addOnFailureListener(e -> {
//                            progressDialog.dismiss();
//                            toast("Image uploaded but URL failed");
//                        }))
//                .addOnFailureListener(e -> {
//                    progressDialog.dismiss();
//                    toast("❌ Upload failed: " + e.getMessage());
//                });
//    }
//
//    private void saveProfileImageUrlToDatabase(String imageUrl) {
//        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
//                .child("info").child("profileImage").setValue(imageUrl);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            onBackPressed();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    private void toast(String msg) {
//        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
//    }
//}




package com.sandhyyasofttech.attendsmart.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvEmployeeName, tvShiftTiming;
    private Switch switchNotifications;
    private ImageView ivProfile;
    private DatabaseReference dbRef;
    private String companyKey, employeeMobile;

    private static final int PICK_IMAGE_REQUEST = 1001;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        employeeMobile = pref.getEmployeeMobile();

        if (companyKey == null || employeeMobile == null) {
            toast("⚠️ Please login first");
            startActivity(new Intent(this, LoginActivity.class));
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
        dbRef = FirebaseDatabase.getInstance().getReference();
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
        if (tvEmployeeName != null) {
            tvEmployeeName.setText("Loading...");
            loadEmployeeNameFromFirebase();
        }

        loadProfileImage();
        loadShiftTiming();

        if (switchNotifications != null) {
            SharedPreferences sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            switchNotifications.setChecked(sharedPrefs.getBoolean("notifications", true));
        }
    }

    private void loadEmployeeNameFromFirebase() {
        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
                .child("info").child("employeeName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists() && tvEmployeeName != null) {
                            String name = snapshot.getValue(String.class);
                            if (name != null && !name.isEmpty()) {
                                tvEmployeeName.setText(name);
                                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                prefs.edit().putString("employeeName", name).apply();
                            } else {
                                tvEmployeeName.setText("Employee");
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (tvEmployeeName != null) {
                            tvEmployeeName.setText("Employee");
                        }
                    }
                });
    }

    private void loadProfileImage() {
        if (ivProfile == null) return;

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String profileUrl = prefs.getString("profileImage", null);

        if (profileUrl == null || profileUrl.isEmpty()) {
            dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
                    .child("info").child("profileImage")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
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
                        public void onCancelled(DatabaseError error) {
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
        if (tvShiftTiming == null) return;

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String employeeShift = prefs.getString("employeeShift", "1");

        dbRef.child("Companies").child(companyKey).child("shifts").child(employeeShift)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists() && tvShiftTiming != null) {
                            String startTime = snapshot.child("startTime").getValue(String.class);
                            String endTime = snapshot.child("endTime").getValue(String.class);
                            if (startTime != null && endTime != null) {
                                tvShiftTiming.setText(startTime + " - " + endTime);
                            } else {
                                tvShiftTiming.setText("Shift 1");
                            }
                        } else {
                            tvShiftTiming.setText("Shift 1");
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (tvShiftTiming != null) {
                            tvShiftTiming.setText("Shift 1");
                        }
                    }
                });
    }

    private void setupClickListeners() {
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> openImagePicker());
        }

        // ✅ Personal Details
        View cardPersonalDetails = findViewById(R.id.cardPersonalDetails);
        if (cardPersonalDetails != null) {
            cardPersonalDetails.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, PersonalDetailsActivity.class);
                intent.putExtra("companyKey", companyKey);
                intent.putExtra("employeeMobile", employeeMobile);
                startActivity(intent);
            });
        }

        // Employment
        View cardEmployment = findViewById(R.id.cardEmployment);
        if (cardEmployment != null) {
            cardEmployment.setOnClickListener(v -> toast("Employment → Coming Soon"));
        }

        // Shift Timing
        View cardShiftTimingView = findViewById(R.id.cardShiftTiming);
        if (cardShiftTimingView != null) {
            cardShiftTimingView.setOnClickListener(v -> toast("Change Shift"));
        }

        // ✅ TODAY'S ATTENDANCE - Open AttendanceDayDetailsActivity
        View cardAttendanceReport = findViewById(R.id.cardAttendanceReport);
        if (cardAttendanceReport != null) {
            cardAttendanceReport.setOnClickListener(v -> {
                // Get today's date: "2025-12-31"
                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date());

                Intent intent = new Intent(SettingsActivity.this, AttendanceReportActivity.class);
                intent.putExtra("date", todayDate);
                intent.putExtra("companyKey", companyKey);
                intent.putExtra("employeeMobile", employeeMobile);
                startActivity(intent);
            });
        }

        // Notifications switch
        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                prefs.edit().putBoolean("notifications", isChecked).apply();
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
        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile)
                .child("info").child("profileImage").setValue(imageUrl);
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
