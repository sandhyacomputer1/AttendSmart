package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.SplashActivity;

public class NoInternetActivity extends AppCompatActivity {

    private MaterialButton btnRetry, btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        // Initialize views
        btnRetry = findViewById(R.id.btn_retry);
        btnExit = findViewById(R.id.btn_exit);

        // Retry button click
        btnRetry.setOnClickListener(v -> {
            if (isConnected()) {
                Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
                // Go back to splash to check login status
                startActivity(new Intent(this, SplashActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Still no internet connection", Toast.LENGTH_SHORT).show();
            }
        });

        // Exit button click
        btnExit.setOnClickListener(v -> {
            finishAffinity(); // Close all activities and exit app
        });
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        // Prevent going back, force user to retry or exit
        Toast.makeText(this, "Please connect to internet or exit", Toast.LENGTH_SHORT).show();
    }
}