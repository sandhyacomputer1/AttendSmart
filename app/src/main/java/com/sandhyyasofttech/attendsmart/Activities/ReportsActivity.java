package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

public class ReportsActivity extends AppCompatActivity {

    private TextView tvTotalEmployees, tvPresent, tvAbsent;
    private DatabaseReference employeesRef;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        initViews();
        setupFirebase();
        loadSummary();
    }

    private void initViews() {
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvPresent = findViewById(R.id.tvPresent);
        tvAbsent = findViewById(R.id.tvAbsent);
    }

    private void setupFirebase() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();

        employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees");
    }

    private void loadSummary() {

        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                int total = (int) snapshot.getChildrenCount();

                tvTotalEmployees.setText(String.valueOf(total));
                tvPresent.setText("—");
                tvAbsent.setText("—");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
