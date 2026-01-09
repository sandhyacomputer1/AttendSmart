package com.sandhyyasofttech.attendsmart.Registration;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.Activities.EmployeeDashboardActivity;
import com.sandhyyasofttech.attendsmart.Activities.NoInternetActivity;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

public class SplashActivity extends AppCompatActivity {

    // UI Elements for animations
    private RelativeLayout logoContainer;
    private ImageView appLogo;
    private View shine, circle1, circle2, circle3;
    private TextView appName, subtitle, loadingText;
    private LinearLayout bottomBranding;

    // Firebase and preferences
    DatabaseReference rootRef;
    PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        initFirebase();
        startAnimations();

        // Start login status check after animations (3.5 seconds total)
        new Handler().postDelayed(this::checkLoginStatus, 3500);
    }

    private void initViews() {
        logoContainer = findViewById(R.id.logoContainer);
        appLogo = findViewById(R.id.app_logo);
        shine = findViewById(R.id.shine);
        circle1 = findViewById(R.id.circle1);
        circle2 = findViewById(R.id.circle2);
        circle3 = findViewById(R.id.circle3);
        appName = findViewById(R.id.appName);
        subtitle = findViewById(R.id.subtitle);
        loadingText = findViewById(R.id.loadingText);
        bottomBranding = findViewById(R.id.bottomBranding);
    }

    private void initFirebase() {
        rootRef = FirebaseDatabase.getInstance().getReference("Companies");
        prefManager = new PrefManager(this);
    }

    private void startAnimations() {
        // Logo scale up animation
        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_scale_up);
        logoContainer.startAnimation(logoAnim);

        // App name fade in
        Animation fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);
        fadeInUp.setStartOffset(300);
        appName.startAnimation(fadeInUp);

        // Subtitle fade in
        Animation subtitleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);
        subtitleAnim.setStartOffset(500);
        subtitle.startAnimation(subtitleAnim);

        // Loading text fade in
        Animation loadingAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);
        loadingAnim.setStartOffset(700);
        loadingText.startAnimation(loadingAnim);

        // Bottom branding fade in
        Animation bottomAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);
        bottomAnim.setStartOffset(900);
        bottomBranding.startAnimation(bottomAnim);

        // Circles rotation
        Animation rotateAnim1 = AnimationUtils.loadAnimation(this, R.anim.rotate_infinite);
        circle1.startAnimation(rotateAnim1);

        Animation rotateAnim2 = AnimationUtils.loadAnimation(this, R.anim.rotate_infinite);
        rotateAnim2.setStartOffset(500);
        circle2.startAnimation(rotateAnim2);

        Animation rotateAnim3 = AnimationUtils.loadAnimation(this, R.anim.rotate_infinite);
        rotateAnim3.setStartOffset(1000);
        circle3.startAnimation(rotateAnim3);

        // Shine pulse effect
        Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse);
        shine.startAnimation(pulseAnim);
    }

    private void checkLoginStatus() {
        if (!isConnected()) {
            startActivity(new Intent(this, NoInternetActivity.class));
            finish();
            return;
        }

        String userType = prefManager.getUserType();
        String email = prefManager.getUserEmail();
        String companyKey = prefManager.getCompanyKey();

        if (userType == null || email == null || companyKey == null) {
            navigateToLogin();
            return;
        }

        if (userType.equals("ADMIN")) {
            String safeEmail = email.replace(".", ",");
            rootRef.child(safeEmail).child("companyInfo").child("status")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String status = snapshot.getValue(String.class);
                        if ("ACTIVE".equals(status)) {
                            startActivity(new Intent(this, AdminDashboardActivity.class));
                        } else {
                            prefManager.logout();
                            Toast.makeText(this, "Please contact your Admin ❌", Toast.LENGTH_LONG).show();
                            navigateToLogin();
                        }
                    })
                    .addOnFailureListener(e -> {
                        prefManager.logout();
                        Toast.makeText(this, "Check internet or login again ❌", Toast.LENGTH_LONG).show();
                        navigateToLogin();
                    });

        } else if (userType.equals("EMPLOYEE")) {
            rootRef.child(companyKey).child("employees").child(prefManager.getEmployeeMobile())
                    .child("info").child("employeeStatus")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String status = snapshot.getValue(String.class);
                        if ("ACTIVE".equals(status)) {
                            Intent intent = new Intent(this, EmployeeDashboardActivity.class);
                            intent.putExtra("companyKey", companyKey);
                            startActivity(intent);
                        } else {
                            prefManager.logout();
                            Toast.makeText(this, "Your account is disabled! Contact Admin ❌", Toast.LENGTH_LONG).show();
                            navigateToLogin();
                        }
                    })
                    .addOnFailureListener(e -> {
                        prefManager.logout();
                        Toast.makeText(this, "Data not loading! Login again ❌", Toast.LENGTH_LONG).show();
                        navigateToLogin();
                    });
        } else {
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }
}
