package com.sandhyyasofttech.attendsmart.Registration;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;


public class SplashActivity extends AppCompatActivity {

    DatabaseReference rootRef;
    PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        rootRef = FirebaseDatabase.getInstance().getReference("ServiceCenter");
        prefManager = new PrefManager(this);

        new Handler().postDelayed(this::checkLoginStatus, 1500);
    }

    private void checkLoginStatus() {

        if (!isConnected()) {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String email = prefManager.getUserEmail();

        if (email == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {

            String safeEmail = email.replace(".", ",");

            rootRef.child(safeEmail)
                    .child("ownerInfo")
                    .child("status")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        Boolean status = snapshot.getValue(Boolean.class);

                        if (status != null && status) {
                            startActivity(new Intent(this, AdminDashboardActivity.class));
                            finish();
                        } else {
                            prefManager.logout();
                            Toast.makeText(this, "Account disabled! Please Login.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        }

                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Login expired, Please Login again", Toast.LENGTH_SHORT).show();
                        prefManager.logout();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
        }
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
