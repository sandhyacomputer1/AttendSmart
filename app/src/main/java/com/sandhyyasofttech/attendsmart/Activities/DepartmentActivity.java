package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.HashMap;

public class DepartmentActivity extends AppCompatActivity {

    private EditText etNewDepartment;
    private ImageButton btnAddDepartment;
    private ListView lvDepartments;

    private ArrayAdapter<String> adapter;
    private ArrayList<String> departmentList;

    private DatabaseReference departmentsRef;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department);

        etNewDepartment = findViewById(R.id.etNewDepartment);
        btnAddDepartment = findViewById(R.id.btnAddDepartment);
        lvDepartments = findViewById(R.id.lvDepartments);

        departmentList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, departmentList);
        lvDepartments.setAdapter(adapter);

        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();
        if (email == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        companyKey = email.replace(".", ",");

        departmentsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("departments");

        btnAddDepartment.setOnClickListener(v -> addDepartment());
        loadDepartments();
    }

    private void addDepartment() {
        String name = etNewDepartment.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etNewDepartment.setError("Enter department name");
            return;
        }

        String key = name; // use name as node key
        HashMap<String, Object> map = new HashMap<>();
        map.put("createdAt", System.currentTimeMillis());

        departmentsRef.child(key).setValue(map)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Department added", Toast.LENGTH_SHORT).show();
                        etNewDepartment.setText("");
                    } else {
                        Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadDepartments() {
        departmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                departmentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    departmentList.add(ds.getKey());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
