package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.sandhyyasofttech.attendsmart.Fragments.DashboardFragment;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Registration.LoginActivity;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
import com.sandhyyasofttech.attendsmart.Activities.AttendanceReportActivity;

public class EmployeeDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);

        initViews();
        setupNavigation();
        loadDefaultFragment(savedInstanceState);
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        prefManager = new PrefManager(this);
        setSupportActionBar(toolbar);
    }

    private void setupNavigation() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_dashboard);
    }

    private void loadDefaultFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment.newInstance(), false);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        boolean addToBackStack = true;

        // ✅ FIXED: Use if-else instead of switch
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) {
            fragment = DashboardFragment.newInstance();
            addToBackStack = false;
        } else if (id == R.id.nav_attendance) {
            launchAttendanceReport();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_leaves) {
            toast("Leaves coming soon!");
        } else if (id == R.id.nav_shifts) {
            toast("Shifts coming soon!");
        } else if (id == R.id.nav_profile) {
            toast("Profile coming soon!");
        } else if (id == R.id.nav_reports) {
            startAttendanceReport();
            return true;
        } else if (id == R.id.nav_logout) {
            showLogoutConfirmation();
            return true;
        }

        if (fragment != null) {
            loadFragment(fragment, addToBackStack);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .addToBackStack(addToBackStack ? "fragment" : null)
                .commit();
    }

    private void launchAttendanceReport() {
        PrefManager pref = new PrefManager(this);
        if (pref.getCompanyKey() != null) {
            Intent intent = new Intent(this, AttendanceReportActivity.class);
            intent.putExtra("companyKey", pref.getCompanyKey());
            intent.putExtra("employeeMobile", pref.getEmployeeMobile());
            intent.putExtra("employeeEmail", pref.getEmployeeEmail());
            startActivity(intent);
        } else {
            toast("Loading profile first...");
        }
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void startAttendanceReport() {
        launchAttendanceReport();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        PrefManager logoutPref = new PrefManager(this);
        logoutPref.logout();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void toast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}
