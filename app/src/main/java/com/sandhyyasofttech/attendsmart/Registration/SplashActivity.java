//package com.sandhyyasofttech.attendsmart.Registration;
//
//import android.content.Intent;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.os.Bundle;
//import android.os.Handler;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.sandhyyasofttech.attendsmart.Activities.EmployeeDashboardActivity;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//
//public class SplashActivity extends AppCompatActivity {
//
//    DatabaseReference rootRef;
//    PrefManager prefManager;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_splash);
//
//        rootRef = FirebaseDatabase.getInstance().getReference("Companies");
//
//        prefManager = new PrefManager(this);
//
//        new Handler().postDelayed(this::checkLoginStatus, 1500);
//    }
//
//    private void checkLoginStatus() {
//
//        if (!isConnected()) {
//            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
//            startActivity(new Intent(this, LoginActivity.class));
//            finish();
//            return;
//        }
//
//        String userType = prefManager.getUserType();
//        String email = prefManager.getUserEmail();
//        String companyKey = prefManager.getCompanyKey();
//
//        // Not logged in
//        if (userType == null || email == null || companyKey == null) {
//            startActivity(new Intent(this, LoginActivity.class));
//            finish();
//            return;
//        }
//
//        if (userType.equals("ADMIN")) {
//            // ✅ Admin: check company status
//            String safeEmail = email.replace(".", ",");
//
//            rootRef.child(safeEmail)
//                    .child("companyInfo")
//                    .child("status")
//                    .get()
//                    .addOnSuccessListener(snapshot -> {
//                        String status = snapshot.getValue(String.class);
//
//                        if ("ACTIVE".equals(status)) {
//                            startActivity(new Intent(this, AdminDashboardActivity.class));
//                            finish();
//                        } else {
//                            prefManager.logout();
//                            Toast.makeText(this, "Account disabled! Please Login.", Toast.LENGTH_SHORT).show();
//                            startActivity(new Intent(this, LoginActivity.class));
//                            finish();
//                        }
//
//                    })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(this, "Login expired, Please Login again", Toast.LENGTH_SHORT).show();
//                        prefManager.logout();
//                        startActivity(new Intent(this, LoginActivity.class));
//                        finish();
//                    });
//
//        } else if (userType.equals("EMPLOYEE")) {
//            // ✅ Employee: go directly to dashboard, companyKey already saved
//            Intent intent = new Intent(this, EmployeeDashboardActivity.class);
//            intent.putExtra("companyKey", companyKey);
//            startActivity(intent);
//            finish();
//        } else {
//            // Unknown type → force login
//            prefManager.logout();
//            startActivity(new Intent(this, LoginActivity.class));
//            finish();
//        }
//    }
//
//    private boolean isConnected() {
//        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
//        if (cm != null) {
//            NetworkInfo info = cm.getActiveNetworkInfo();
//            return info != null && info.isConnected();
//        }
//        return false;
//    }
//}



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
import com.sandhyyasofttech.attendsmart.Activities.EmployeeDashboardActivity;
import com.sandhyyasofttech.attendsmart.Activities.NoInternetActivity;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;


public class SplashActivity extends AppCompatActivity {

    DatabaseReference rootRef;
    PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        rootRef = FirebaseDatabase.getInstance().getReference("Companies");

        prefManager = new PrefManager(this);

        new Handler().postDelayed(this::checkLoginStatus, 2000);
    }

    private void checkLoginStatus() {

        if (!isConnected()) {
            // Navigate to No Internet Activity instead of showing toast
            startActivity(new Intent(this, NoInternetActivity.class));
            finish();
            return;
        }

        String userType = prefManager.getUserType();
        String email = prefManager.getUserEmail();
        String companyKey = prefManager.getCompanyKey();

        // Not logged in
        if (userType == null || email == null || companyKey == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (userType.equals("ADMIN")) {
            // ✅ Admin: check company status
            String safeEmail = email.replace(".", ",");

            rootRef.child(safeEmail)
                    .child("companyInfo")
                    .child("status")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String status = snapshot.getValue(String.class);

                        if ("ACTIVE".equals(status)) {
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

        } else if (userType.equals("EMPLOYEE")) {
            // ✅ Employee: go directly to dashboard, companyKey already saved
            Intent intent = new Intent(this, EmployeeDashboardActivity.class);
            intent.putExtra("companyKey", companyKey);
            startActivity(intent);
            finish();
        } else {
            // Unknown type → force login
            prefManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
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