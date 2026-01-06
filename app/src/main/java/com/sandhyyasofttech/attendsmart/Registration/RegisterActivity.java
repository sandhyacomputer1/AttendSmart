package com.sandhyyasofttech.attendsmart.Registration;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.R;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private EditText etCompanyName, etName, etPhone, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvBackToLogin;

    private FirebaseAuth mAuth;
    private DatabaseReference companiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        companiesRef = FirebaseDatabase.getInstance().getReference("Companies");

        etCompanyName = findViewById(R.id.etCompanyName);  // ✅ NEW
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnRegister.setOnClickListener(v -> registerUser());

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {

        String companyName = etCompanyName.getText().toString().trim();  // ✅ NEW - was etName
        String name = etName.getText().toString().trim();  // ✅ Renamed from companyName
        String companyPhone = etPhone.getText().toString().trim();
        String companyEmail = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // ✅ NEW validation for company name
        if (TextUtils.isEmpty(companyName)) {
            etCompanyName.setError("Enter company name");
            return;
        }

        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter full name");
            return;
        }

        if (TextUtils.isEmpty(companyPhone)) {
            etPhone.setError("Enter phone");
            return;
        }

        if (TextUtils.isEmpty(companyEmail)) {
            etEmail.setError("Enter email");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Minimum 6 characters required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(companyEmail, password)
                .addOnCompleteListener(this, task -> {

                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);

                    if (task.isSuccessful()) {

                        String companyKey = companyEmail.replace(".", ",");

                        HashMap<String, Object> companyInfo = new HashMap<>();
                        companyInfo.put("companyName", companyName);  // ✅ NEW field
                        companyInfo.put("name", name);  // ✅ Owner/admin name
                        companyInfo.put("companyEmail", companyEmail);
                        companyInfo.put("companyPhone", companyPhone);
                        companyInfo.put("password", password);
                        companyInfo.put("status", "ACTIVE");

                        companiesRef
                                .child(companyKey)
                                .child("companyInfo")
                                .setValue(companyInfo)
                                .addOnCompleteListener(done -> {

                                    if (done.isSuccessful()) {
                                        Toast.makeText(
                                                RegisterActivity.this,
                                                "Registration Successful. Please Login.",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        finish();
                                    } else {
                                        Toast.makeText(
                                                RegisterActivity.this,
                                                "Failed to save company data",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                });

                    } else {
                        Toast.makeText(
                                RegisterActivity.this,
                                "Registration Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
