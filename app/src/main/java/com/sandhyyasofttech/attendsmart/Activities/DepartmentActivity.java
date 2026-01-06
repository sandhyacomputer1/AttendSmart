package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.Adapters.DepartmentAdapter;
import com.sandhyyasofttech.attendsmart.Models.DepartmentModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.HashMap;

public class DepartmentActivity extends AppCompatActivity {

    private static final String TAG = "DepartmentActivity";

    private TextInputEditText etNewDepartment;
    private MaterialButton btnAddDepartment;
    private RecyclerView rvDepartments;

    private DepartmentAdapter adapter;
    private ArrayList<DepartmentModel> list;

    private DatabaseReference departmentsRef;
    private String companyKey;

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Departments");
        }
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

        adapter = new DepartmentAdapter(list, new DepartmentAdapter.OnDepartmentActionListener() {
            @Override
            public void onEditDepartment(String oldName, String newName, int position) {
                updateDepartmentInFirebase(oldName, newName, position);
            }

            @Override
            public void onDeleteDepartment(String departmentName, int position) {
                permanentDeleteDepartment(departmentName);
            }
        });

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

        companyKey = email.replace(".", ",");
        departmentsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("departments");

        Log.d(TAG, "Firebase path: " + departmentsRef.toString());
    }

    private void addDepartment() {
        String name = etNewDepartment.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etNewDepartment.setError("Enter department name");
            etNewDepartment.requestFocus();
            return;
        }

        btnAddDepartment.setEnabled(false);

        departmentsRef.child(name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isDeleted = snapshot.child("isDeleted").getValue(Boolean.class);
                    if (isDeleted != null && isDeleted) {
                        restoreDepartment(name);
                    } else {
                        Toast.makeText(DepartmentActivity.this, "Department already exists", Toast.LENGTH_SHORT).show();
                        btnAddDepartment.setEnabled(true);
                    }
                } else {
                    createNewDepartment(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking department: " + error.getMessage());
                Toast.makeText(DepartmentActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                btnAddDepartment.setEnabled(true);
            }
        });
    }

    private void createNewDepartment(String name) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("createdAt", System.currentTimeMillis());
        map.put("isDeleted", false);

        departmentsRef.child(name).setValue(map)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Department created: " + name);
                    Toast.makeText(this, "Department added successfully", Toast.LENGTH_SHORT).show();
                    etNewDepartment.setText("");
                    btnAddDepartment.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create department: " + e.getMessage());
                    Toast.makeText(this, "Failed to add department: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnAddDepartment.setEnabled(true);
                });
    }

    private void restoreDepartment(String name) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("isDeleted", false);
        map.put("restoredAt", System.currentTimeMillis());

        departmentsRef.child(name).updateChildren(map)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Department restored: " + name);
                    Toast.makeText(this, "Department restored successfully", Toast.LENGTH_SHORT).show();
                    etNewDepartment.setText("");
                    btnAddDepartment.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to restore department: " + e.getMessage());
                    Toast.makeText(this, "Failed to restore department: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnAddDepartment.setEnabled(true);
                });
    }

    private void updateDepartmentInFirebase(String oldName, String newName, int position) {
        Log.d(TAG, "Updating department: " + oldName + " -> " + newName);

        departmentsRef.child(newName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isDeleted = snapshot.child("isDeleted").getValue(Boolean.class);
                    if (isDeleted == null || !isDeleted) {
                        Toast.makeText(DepartmentActivity.this, "Department name already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                departmentsRef.child(oldName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot oldSnapshot) {
                        if (oldSnapshot.exists()) {
                            HashMap<String, Object> data = new HashMap<>();
                            data.put("name", newName);

                            Long createdAt = oldSnapshot.child("createdAt").getValue(Long.class);
                            data.put("createdAt", createdAt != null ? createdAt : System.currentTimeMillis());
                            data.put("updatedAt", System.currentTimeMillis());
                            data.put("isDeleted", false);

                            departmentsRef.child(newName).setValue(data)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "New department entry created: " + newName);

                                        departmentsRef.child(oldName).removeValue()
                                                .addOnSuccessListener(aVoid2 -> {
                                                    Log.d(TAG, "Old department permanently deleted: " + oldName);
                                                    list.get(position).name = newName;
                                                    adapter.notifyItemChanged(position);
                                                    Toast.makeText(DepartmentActivity.this,
                                                            "Department updated successfully", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Failed to delete old department: " + e.getMessage());
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to create new entry: " + e.getMessage());
                                        Toast.makeText(DepartmentActivity.this,
                                                "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: " + error.getMessage());
                        Toast.makeText(DepartmentActivity.this,
                                "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(DepartmentActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void permanentDeleteDepartment(String departmentName) {
        Log.d(TAG, "Permanently deleting department: " + departmentName);

        departmentsRef.child(departmentName).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Department permanently deleted from Firebase: " + departmentName);
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            Snackbar.make(rvDepartments, "Department permanently deleted", Snackbar.LENGTH_LONG).show(), 300);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to permanently delete department: " + e.getMessage());
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDepartments() {
        Log.d(TAG, "Loading departments...");

        departmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Boolean isDeleted = ds.child("isDeleted").getValue(Boolean.class);
                    if (isDeleted == null || !isDeleted) {
                        String departmentName = ds.getKey();
                        list.add(new DepartmentModel(departmentName));
                        Log.d(TAG, "Loaded department: " + departmentName);
                    }
                }
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Total active departments: " + list.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load departments: " + error.getMessage());
                Toast.makeText(DepartmentActivity.this,
                        "Failed to load departments: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
