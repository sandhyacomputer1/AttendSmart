//package com.sandhyyasofttech.attendsmart.Registration;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//
//public class LoginActivity extends AppCompatActivity {
//
//    private EditText etEmail, etPassword;
//    private Button btnLogin;
//    private TextView tvRegister, tvForgotPassword;
//    private ProgressBar progressBar;
//
//    private FirebaseAuth mAuth;
//    private DatabaseReference rootRef;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_login);
//
//        mAuth = FirebaseAuth.getInstance();
//        rootRef = FirebaseDatabase.getInstance().getReference("ServiceCenter");
//
//        etEmail = findViewById(R.id.etEmail);
//        etPassword = findViewById(R.id.etPassword);
//        btnLogin = findViewById(R.id.btnLogin);
//        tvRegister = findViewById(R.id.tvRegister);
//        tvForgotPassword = findViewById(R.id.tvForgotPassword);
//        progressBar = findViewById(R.id.progressBar);
//
//        btnLogin.setOnClickListener(v -> loginUser());
//
//        tvRegister.setOnClickListener(v ->
//                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
//
//        tvForgotPassword.setOnClickListener(v -> resetPassword());
//    }
//
//    private void loginUser() {
//        String email = etEmail.getText().toString().trim();
//        String password = etPassword.getText().toString().trim();
//
//        if (TextUtils.isEmpty(email)) {
//            etEmail.setError("Enter email");
//            etEmail.requestFocus();
//            return;
//        }
//
//        if (TextUtils.isEmpty(password)) {
//            etPassword.setError("Enter password");
//            etPassword.requestFocus();
//            return;
//        }
//
//        progressBar.setVisibility(View.VISIBLE);
//        btnLogin.setEnabled(false);
//
//        // Firebase Auth check
//        mAuth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener(this, task -> {
//                    if (task.isSuccessful()) {
//
//                        // Safe key for firebase
//                        String safeEmailKey = email.replace(".", ",");
//
//                        rootRef.child(safeEmailKey)
//                                .child("ownerInfo")
//                                .addListenerForSingleValueEvent(new ValueEventListener() {
//                                    @Override
//                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//
//                                        progressBar.setVisibility(View.GONE);
//                                        btnLogin.setEnabled(true);
//
//                                        if (snapshot.exists()) {
//
//                                            boolean status = false;
//                                            if (snapshot.child("status").exists()) {
//                                                status = snapshot.child("status").getValue(Boolean.class);
//                                            }
//
//                                            if (status) {
//                                                Toast.makeText(LoginActivity.this,
//                                                        "Login Successful",
//                                                        Toast.LENGTH_SHORT).show();
//
//                                                PrefManager prefManager = new PrefManager(LoginActivity.this);
//                                                prefManager.saveUserEmail(email);
//
//                                                // UPDATE NEW PASSWORD TO DATABASE
//                                                String safeEmailKey = email.replace(".", ",");
//
//                                                FirebaseDatabase.getInstance()
//                                                        .getReference("ServiceCenter")
//                                                        .child(safeEmailKey)
//                                                        .child("ownerInfo")
//                                                        .child("password")
//                                                        .setValue(password);
//
//
//                                                startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
//                                                finish();
//
//                                            } else {
//                                                mAuth.signOut();
//                                                Toast.makeText(LoginActivity.this,
//                                                        "Account is disabled. Contact Admin",
//                                                        Toast.LENGTH_LONG).show();
//                                            }
//
//                                        } else {
//                                            mAuth.signOut();
//                                            Toast.makeText(LoginActivity.this,
//                                                    "Owner data not found ❌",
//                                                    Toast.LENGTH_SHORT).show();
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onCancelled(@NonNull DatabaseError error) {
//                                        progressBar.setVisibility(View.GONE);
//                                        btnLogin.setEnabled(true);
//                                        Toast.makeText(LoginActivity.this,
//                                                "Error: " + error.getMessage(),
//                                                Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//
//                    } else {
//                        progressBar.setVisibility(View.GONE);
//                        btnLogin.setEnabled(true);
//
//                        Toast.makeText(LoginActivity.this,
//                                "Login Failed: " + task.getException().getMessage(),
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
//
//    private void resetPassword() {
//        String email = etEmail.getText().toString().trim();
//
//        if (TextUtils.isEmpty(email)) {
//            etEmail.setError("Enter your email to reset password");
//            etEmail.requestFocus();
//            return;
//        }
//
//        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Toast.makeText(LoginActivity.this,
//                                "Reset link sent to your email.",
//                                Toast.LENGTH_LONG).show();
//                    } else {
//                        Toast.makeText(LoginActivity.this,
//                                "Failed: " + task.getException().getMessage(),
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
//}



package com.sandhyyasofttech.attendsmart.Registration;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference companiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        companiesRef = FirebaseDatabase.getInstance().getReference("Companies");

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

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

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // Firebase Auth login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        String companyKey = email.replace(".", ",");

                        companiesRef
                                .child(companyKey)
                                .child("companyInfo")
                                .addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        progressBar.setVisibility(View.GONE);
                                        btnLogin.setEnabled(true);

                                        if (snapshot.exists()) {

                                            String status = snapshot.child("status").getValue(String.class);

                                            if ("ACTIVE".equals(status)) {

                                                Toast.makeText(LoginActivity.this,
                                                        "Login Successful",
                                                        Toast.LENGTH_SHORT).show();

                                                PrefManager prefManager =
                                                        new PrefManager(LoginActivity.this);
                                                prefManager.saveUserEmail(email);

                                                // UPDATE PASSWORD (unchanged logic)
                                                companiesRef
                                                        .child(companyKey)
                                                        .child("companyInfo")
                                                        .child("password")
                                                        .setValue(password);

                                                startActivity(new Intent(
                                                        LoginActivity.this,
                                                        AdminDashboardActivity.class
                                                ));
                                                finish();

                                            } else {
                                                mAuth.signOut();
                                                Toast.makeText(LoginActivity.this,
                                                        "Account is disabled. Contact Admin",
                                                        Toast.LENGTH_LONG).show();
                                            }

                                        } else {
                                            mAuth.signOut();
                                            Toast.makeText(LoginActivity.this,
                                                    "Company data not found ❌",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        progressBar.setVisibility(View.GONE);
                                        btnLogin.setEnabled(true);
                                        Toast.makeText(LoginActivity.this,
                                                "Error: " + error.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });

                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this,
                                "Login Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetPassword() {

        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email to reset password");
            etEmail.requestFocus();
            return;
        }

        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Reset link sent to your email.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
