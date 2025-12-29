package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.Adapters.DepartmentAdapter;
import com.sandhyyasofttech.attendsmart.Models.DepartmentModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.HashMap;

public class DepartmentActivity extends AppCompatActivity {

    private TextInputEditText etNewDepartment;
    private MaterialButton btnAddDepartment;
    private RecyclerView rvDepartments;

    private DepartmentAdapter adapter;
    private ArrayList<DepartmentModel> list;

    private DatabaseReference departmentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department);

        setupToolbar();
        initViews();
        setupFirebase();
        loadDepartments();

        btnAddDepartment.setOnClickListener(v -> addDepartment());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        etNewDepartment = findViewById(R.id.etNewDepartment);
        btnAddDepartment = findViewById(R.id.btnAddDepartment);
        rvDepartments = findViewById(R.id.rvDepartments);

        list = new ArrayList<>();
        adapter = new DepartmentAdapter(list);
        rvDepartments.setLayoutManager(new LinearLayoutManager(this));
        rvDepartments.setAdapter(adapter);
    }

    private void setupFirebase() {
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();

        if (email == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String companyKey = email.replace(".", ",");
        departmentsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("departments");
    }

    private void addDepartment() {
        String name = etNewDepartment.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etNewDepartment.setError("Enter department name");
            return;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("createdAt", System.currentTimeMillis());

        departmentsRef.child(name).setValue(map)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Department added", Toast.LENGTH_SHORT).show();
                    etNewDepartment.setText("");
                });
    }

    private void loadDepartments() {
        departmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    list.add(new DepartmentModel(ds.getKey()));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
