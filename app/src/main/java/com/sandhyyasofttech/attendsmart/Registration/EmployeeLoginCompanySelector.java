package com.sandhyyasofttech.attendsmart.Registration;

import android.content.Intent;
import android.os.Bundle;
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
    private String employeeEmail;
    private ArrayList<CompanyItem> companies = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_login_company_selector);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        employeeEmail = getIntent().getStringExtra("employeeEmail");
        ArrayList<String> companyKeys = getIntent().getStringArrayListExtra("companies");

        rvCompanies = findViewById(R.id.rvCompanies);
        rvCompanies.setLayoutManager(new LinearLayoutManager(this));

        loadCompanyNames(companyKeys);
    }

    private void loadCompanyNames(ArrayList<String> companyKeys) {
        DatabaseReference companiesRef = FirebaseDatabase.getInstance().getReference("Companies");

        for (String companyKey : companyKeys) {
            companiesRef.child(companyKey).child("companyInfo")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String companyName = snapshot.child("companyName").getValue(String.class);
                                if (companyName == null) companyName = companyKey.replace(",", ".");

                                companies.add(new CompanyItem(companyName, companyKey));

                                // ✅ All companies loaded → Set adapter
                                if (companies.size() == companyKeys.size()) {
                                    rvCompanies.setAdapter(new CompanyAdapter(companies, company -> {
                                        loginToCompany(company.companyKey);
                                    }));
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void loginToCompany(String companyKey) {
        PrefManager prefManager = new PrefManager(this);
        prefManager.saveUserEmail(employeeEmail);
        prefManager.saveUserType("EMPLOYEE");
        prefManager.saveCompanyKey(companyKey);

        Toast.makeText(this, "Logged in to " + companyKey.replace(",", "."), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, EmployeeDashboardActivity.class);
        intent.putExtra("companyKey", companyKey);
        startActivity(intent);
        finish();
    }
}
