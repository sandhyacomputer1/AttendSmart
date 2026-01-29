package com.sandhyyasofttech.attendsmart.Registration;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Activities.EmployeeDashboardActivity;
import com.sandhyyasofttech.attendsmart.Adapters.CompanyAdapter;
import com.sandhyyasofttech.attendsmart.Models.CompanyItem;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;

public class EmployeeLoginCompanySelector extends AppCompatActivity {

    private RecyclerView rvCompanies;
    private TextView tvEmployeeEmail, tvCompanyCount;
    private LinearLayout llEmptyMessage;
    private ProgressBar progressBar;
    private String employeeEmail;
    private ArrayList<CompanyItem> companies = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_login_company_selector);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }

        employeeEmail = getIntent().getStringExtra("employeeEmail");
        ArrayList<String> companyKeys = getIntent().getStringArrayListExtra("companies");

        initViews();

        if (employeeEmail != null) {
            tvEmployeeEmail.setText(employeeEmail);
        } else {
            tvEmployeeEmail.setText("Employee Email");
        }

        loadCompanyNames(companyKeys);
    }

    private void initViews() {
        rvCompanies = findViewById(R.id.rvCompanies);
        tvEmployeeEmail = findViewById(R.id.tvEmployeeEmail);
        tvCompanyCount = findViewById(R.id.tvCompanyCount);
        llEmptyMessage = findViewById(R.id.llEmptyMessage); // Changed to LinearLayout
        progressBar = findViewById(R.id.progressBar);

        rvCompanies.setLayoutManager(new LinearLayoutManager(this));

        // Hide empty message initially
        llEmptyMessage.setVisibility(View.GONE);
        rvCompanies.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void loadCompanyNames(ArrayList<String> companyKeys) {
        if (companyKeys == null || companyKeys.isEmpty()) {
            showNoCompaniesMessage();
            return;
        }

        DatabaseReference companiesRef = FirebaseDatabase.getInstance().getReference("Companies");

        for (String companyKey : companyKeys) {
            companiesRef.child(companyKey).child("companyInfo")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String companyName = snapshot.child("companyName").getValue(String.class);
                                String companyEmail = snapshot.child("companyEmail").getValue(String.class);
                                String companyPhone = snapshot.child("companyPhone").getValue(String.class);

                                if (companyName == null) companyName = "Company " + companyKey.replace(",", ".");
                                if (companyEmail == null) companyEmail = "No email available";
                                if (companyPhone == null) companyPhone = "No phone available";

                                companies.add(new CompanyItem(companyName, companyKey, companyEmail, companyPhone));
                            } else {
                                // If no company info, use basic info
                                companies.add(new CompanyItem(
                                        "Company " + companyKey.replace(",", "."),
                                        companyKey,
                                        "Info not available",
                                        "Contact admin"
                                ));
                            }

                            // Update count
                            tvCompanyCount.setText("Available Companies: " + companies.size());

                            // All companies loaded → Set adapter
                            if (companies.size() == companyKeys.size()) {
                                setupCompaniesList();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // If there's an error loading one company, continue with others
                            if (companies.size() == companyKeys.size()) {
                                setupCompaniesList();
                            }
                        }
                    });
        }
    }

    private void setupCompaniesList() {
        progressBar.setVisibility(View.GONE);

        if (companies.isEmpty()) {
            showNoCompaniesMessage();
            return;
        }

        rvCompanies.setVisibility(View.VISIBLE);
        llEmptyMessage.setVisibility(View.GONE);

        // Sort companies alphabetically
        companies.sort((c1, c2) -> c1.companyName.compareToIgnoreCase(c2.companyName));

        CompanyAdapter adapter = new CompanyAdapter(companies, company -> {
            loginToCompany(company.companyKey, company.companyName);
        });

        rvCompanies.setAdapter(adapter);

        // You can add item decoration if needed
        // rvCompanies.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void showNoCompaniesMessage() {
        progressBar.setVisibility(View.GONE);
        rvCompanies.setVisibility(View.GONE);
        llEmptyMessage.setVisibility(View.VISIBLE);
        tvCompanyCount.setText("No Companies Available");
    }

    private void loginToCompany(String companyKey, String companyName) {
        PrefManager prefManager = new PrefManager(this);
        prefManager.saveUserEmail(employeeEmail);
        prefManager.saveUserType("EMPLOYEE");
        prefManager.saveCompanyKey(companyKey);

        // Show loading
        progressBar.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Logging in to " + companyName, Toast.LENGTH_SHORT).show();

        // Small delay for better UX
        rvCompanies.postDelayed(() -> {
            Intent intent = new Intent(this, EmployeeDashboardActivity.class);
            intent.putExtra("companyKey", companyKey);
            intent.putExtra("companyName", companyName);
            intent.putExtra("employeeEmail", employeeEmail);
            startActivity(intent);
            finish();
        }, 500);
    }
}