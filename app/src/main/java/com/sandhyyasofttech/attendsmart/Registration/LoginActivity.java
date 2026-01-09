package com.sandhyyasofttech.attendsmart.Registration;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Activities.EmployeeDashboardActivity;
import com.sandhyyasofttech.attendsmart.Registration.AdminDashboardActivity;
import com.sandhyyasofttech.attendsmart.Registration.EmployeeLoginCompanySelector;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import com.sandhyyasofttech.attendsmart.R;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make status bar transparent with gradient color
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_login);

        rootRef = FirebaseDatabase.getInstance().getReference();
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }

        showLoading(true);
        checkAdminLogin(email, password);
    }

    private void checkAdminLogin(String email, String password) {
        String companyKey = email.replace(".", ",");

        rootRef.child("Companies").child(companyKey).child("companyInfo")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Object passwordObj = snapshot.child("password").getValue(Object.class);
                            Object statusObj = snapshot.child("status").getValue(Object.class);

                            String storedPassword = passwordObj != null ? passwordObj.toString() : null;
                            String status = statusObj != null ? statusObj.toString() : null;

                            if (storedPassword != null && status != null &&
                                    password.equals(storedPassword) && "ACTIVE".equals(status)) {
                                PrefManager prefManager = new PrefManager(LoginActivity.this);
                                prefManager.saveUserEmail(email);  // ✅ Admin email
                                prefManager.saveUserType("ADMIN");
                                prefManager.saveCompanyKey(companyKey);

                                showLoading(false);
                                startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                                finish();
                                return;
                            }

                        }
                        checkEmployeeLogin(email, password);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                    }
                });
    }

    private void checkEmployeeLogin(String employeeEmail, String password) {
        rootRef.child("Companies").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> companies = new ArrayList<>();

                for (DataSnapshot companySnapshot : snapshot.getChildren()) {
                    String companyKey = companySnapshot.getKey();
                    for (DataSnapshot employeeSnapshot : companySnapshot.child("employees").getChildren()) {
                        DataSnapshot info = employeeSnapshot.child("info");
                        if (info.exists()) {
                            Object emailObj = info.child("employeeEmail").getValue(Object.class);
                            Object passwordObj = info.child("employeePassword").getValue(Object.class);
                            Object statusObj = info.child("employeeStatus").getValue(Object.class);

                            String email = emailObj != null ? emailObj.toString() : null;
                            String storedPassword = passwordObj != null ? passwordObj.toString() : null;
                            String status = statusObj != null ? statusObj.toString() : null;

                            if (email != null && storedPassword != null && status != null &&
                                    employeeEmail.equals(email) &&
                                    password.equals(storedPassword) &&
                                    "ACTIVE".equals(status)) {
                                companies.add(companyKey);
                                String employeeMobile = employeeSnapshot.getKey();  // ✅ Get mobile key
                                PrefManager prefManager = new PrefManager(LoginActivity.this);
                                prefManager.setEmployeeEmail(employeeEmail);  // ✅ Employee email
                                prefManager.setEmployeeMobile(employeeMobile);  // ✅ Mobile for attendance
                                prefManager.saveUserEmail(employeeEmail);  // ✅ Admin compatibility
                                prefManager.saveUserType("EMPLOYEE");
                                prefManager.saveCompanyKey(companyKey);

                                showLoading(false);
                                Intent intent = new Intent(LoginActivity.this, EmployeeDashboardActivity.class);
                                intent.putExtra("companyKey", companyKey);
                                startActivity(intent);
                                finish();
                                return;  // ✅ Exit immediately
                            }
                        }
                    }
                }

                if (companies.size() == 0) {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "Invalid credentials ❌", Toast.LENGTH_SHORT).show();
                } else if (companies.size() > 1) {
                    showCompanySelector(employeeEmail, companies);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCompanySelector(String email, ArrayList<String> companies) {
        showLoading(false);
        Intent intent = new Intent(this, EmployeeLoginCompanySelector.class);
        intent.putExtra("employeeEmail", email);
        intent.putStringArrayListExtra("companies", companies);
        startActivity(intent);
    }

    private void goToAdminDashboard() {
        showLoading(false);
        startActivity(new Intent(this, AdminDashboardActivity.class));
        finish();
    }

    private void goToEmployeeDashboard(String companyKey) {
        showLoading(false);
        Intent intent = new Intent(this, EmployeeDashboardActivity.class);
        intent.putExtra("companyKey", companyKey);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Logging in..." : "Login");
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    private void resetPassword() {
        Toast.makeText(this, "Contact your admin", Toast.LENGTH_SHORT).show();
    }
}