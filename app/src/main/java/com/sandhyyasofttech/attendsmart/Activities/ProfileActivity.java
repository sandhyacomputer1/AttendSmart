package com.sandhyyasofttech.attendsmart.Activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 100;

    // UI
    private ShapeableImageView ivLogo;
    private TextInputEditText etCompanyName, etRegNumber, etIndustry;
    private TextInputEditText etEmail, etPhone, etWebsite;
    private TextInputEditText etCompanyAddress, etCity, etState, etPincode, etCountry;
    private TextInputEditText etPassword;
    private MaterialButton btnSave;

    // Firebase
    private DatabaseReference companyInfoRef;
    private StorageReference logoStorageRef;
    private String companyKey;

    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        initViews();
        setupToolbar();
        setupFirebase();
        loadCompanyData();
        setupListeners();
    }

    private void initViews() {

        ivLogo = findViewById(R.id.ivLogo);

        etCompanyName = findViewById(R.id.etCompanyName);
        etRegNumber = findViewById(R.id.etRegNumber);
        etIndustry = findViewById(R.id.etIndustry);

        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etWebsite = findViewById(R.id.etWebsite);

        etCompanyAddress = findViewById(R.id.etCompanyAddress);
        etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState);
        etPincode = findViewById(R.id.etPincode);
        etCountry = findViewById(R.id.etCountry);

        etPassword = findViewById(R.id.etPassword);

        btnSave = findViewById(R.id.btnSave);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFirebase() {

        PrefManager pref = new PrefManager(this);
        String email = pref.getUserEmail();

        if (email == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        companyKey = email.replace(".", ",");

        companyInfoRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("companyInfo");

        logoStorageRef = FirebaseStorage.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("company_logo.jpg");
    }

    private void setupListeners() {

        ivLogo.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, PICK_IMAGE);
        });

        btnSave.setOnClickListener(v -> saveProfile());
    }

    // ✅ FETCH & DISPLAY ALL DATA
    private void loadCompanyData() {

        companyInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {

                if (!s.exists()) return;

                etCompanyName.setText(s.child("companyName").getValue(String.class));
                etRegNumber.setText(s.child("registrationNumber").getValue(String.class));
                etIndustry.setText(s.child("industry").getValue(String.class));

                etEmail.setText(s.child("companyEmail").getValue(String.class));
                etPhone.setText(s.child("companyPhone").getValue(String.class));
                etWebsite.setText(s.child("website").getValue(String.class));

                etCompanyAddress.setText(s.child("companyAddress").getValue(String.class));
                etCity.setText(s.child("city").getValue(String.class));
                etState.setText(s.child("state").getValue(String.class));
                etPincode.setText(s.child("pincode").getValue(String.class));
                etCountry.setText(s.child("country").getValue(String.class));

                etPassword.setText(s.child("password").getValue(String.class));

                String logoUrl = s.child("companyLogo").getValue(String.class);
                if (logoUrl != null && !logoUrl.isEmpty()) {
                    Glide.with(ProfileActivity.this)
                            .load(logoUrl)
                            .placeholder(R.drawable.salarylogo)
                            .into(ivLogo);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this,
                        "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ SAVE ALL DATA
    private void saveProfile() {

        Map<String, Object> map = new HashMap<>();

        map.put("companyName", get(etCompanyName));
        map.put("registrationNumber", get(etRegNumber));
        map.put("industry", get(etIndustry));

        map.put("companyEmail", get(etEmail));
        map.put("companyPhone", get(etPhone));
        map.put("website", get(etWebsite));

        map.put("companyAddress", get(etCompanyAddress));
        map.put("city", get(etCity));
        map.put("state", get(etState));
        map.put("pincode", get(etPincode));
        map.put("country", get(etCountry));

        if (!TextUtils.isEmpty(get(etPassword))) {
            map.put("password", get(etPassword));
        }

        companyInfoRef.updateChildren(map)
                .addOnSuccessListener(unused -> {
                    if (selectedImageUri != null) uploadLogo();
                    Toast.makeText(this,
                            "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Update failed", Toast.LENGTH_SHORT).show()
                );
    }

    private String get(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    // ✅ LOGO UPLOAD + SAVE URL
    private void uploadLogo() {

        logoStorageRef.putFile(selectedImageUri)
                .addOnSuccessListener(task ->
                        logoStorageRef.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        companyInfoRef.child("companyLogo")
                                                .setValue(uri.toString())
                                )
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Logo upload failed", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            ivLogo.setImageURI(selectedImageUri);
        }
    }
}
