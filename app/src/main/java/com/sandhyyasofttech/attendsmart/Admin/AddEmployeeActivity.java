package com.sandhyyasofttech.attendsmart.Admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.HashMap;

public class AddEmployeeActivity extends AppCompatActivity {

    EditText etEmpName, etEmpMobile, etEmpEmail, etEmpRole;
    Button btnSaveEmployee;

    DatabaseReference employeesRef;
    String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);

        etEmpName = findViewById(R.id.etEmpName);
        etEmpMobile = findViewById(R.id.etEmpMobile);
        etEmpEmail = findViewById(R.id.etEmpEmail);
        etEmpRole = findViewById(R.id.etEmpRole);
        btnSaveEmployee = findViewById(R.id.btnSaveEmployee);

        PrefManager prefManager = new PrefManager(this);
        companyKey = prefManager.getUserEmail().replace(".", ",");

        // ✅ FIXED PATH (EMPLOYEES UNDER COMPANY)
        employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees");

        btnSaveEmployee.setOnClickListener(v -> saveEmployee());
    }

    private void saveEmployee() {

        String name = etEmpName.getText().toString().trim();
        String mobile = etEmpMobile.getText().toString().trim();
        String email = etEmpEmail.getText().toString().trim();
        String role = etEmpRole.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etEmpName.setError("Enter name");
            return;
        }

        if (TextUtils.isEmpty(mobile)) {
            etEmpMobile.setError("Enter mobile");
            return;
        }

        HashMap<String, Object> info = new HashMap<>();
        info.put("employeeName", name);
        info.put("employeeMobile", mobile);
        info.put("employeeEmail", email);
        info.put("employeeRole", role);
        info.put("employeeStatus", "ACTIVE");
        info.put("createdAt", System.currentTimeMillis());

        employeesRef
                .child(mobile)
                .child("info")
                .setValue(info)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Employee Added Successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Failed to add employee",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
