package com.sandhyyasofttech.attendsmart.Activities;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.ShiftModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ShiftActivity extends AppCompatActivity {

    private TextInputEditText etShiftName, etStartTime, etEndTime;
    private MaterialButton btnAddShift;
    private RecyclerView rvShifts;
    private View llStartTime, llEndTime;
    private TextView tvCount;
    private View llEmptyState;
    private NestedScrollView nestedScrollView;

    private ShiftAdapter shiftAdapter;
    private List<ShiftModel> shiftList;

    private DatabaseReference shiftsRef, employeesRef;
    private String companyKey;
    private boolean isEditing = false;
    private String currentEditShiftName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }

        setupToolbar();
        initViews();
        setupFirebase();
        setupListeners();
        loadShifts();
    }

    /* ---------------- Toolbar ---------------- */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Shifts");
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

    /* ---------------- Init Views ---------------- */
    private void initViews() {
        etShiftName = findViewById(R.id.etShiftName);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        btnAddShift = findViewById(R.id.btnAddShift);
        rvShifts = findViewById(R.id.rvShifts);
        tvCount = findViewById(R.id.tvCount);
        llEmptyState = findViewById(R.id.llEmptyState);
        nestedScrollView = findViewById(R.id.nestedScrollView);

        // Get the LinearLayouts for time pickers
        llStartTime = findViewById(R.id.llStartTime);
        llEndTime = findViewById(R.id.llEndTime);

        shiftList = new ArrayList<>();
        shiftAdapter = new ShiftAdapter(shiftList);

        if (rvShifts != null) {
            rvShifts.setLayoutManager(new LinearLayoutManager(this));
            rvShifts.setAdapter(shiftAdapter);
            rvShifts.setHasFixedSize(true);
        }
    }

    /* ---------------- Firebase ---------------- */
    private void setupFirebase() {
        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();

        if (email == null) {
            Toast.makeText(this, "Session expired. Login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        companyKey = email.replace(".", ",");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        shiftsRef = database.getReference("Companies")
                .child(companyKey)
                .child("shifts");

        employeesRef = database.getReference("Companies")
                .child(companyKey)
                .child("employees");
    }

    /* ---------------- Listeners ---------------- */
    private void setupListeners() {
        // Set click listeners on the LinearLayouts for time pickers
        if (llStartTime != null) {
            llStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        }

        if (llEndTime != null) {
            llEndTime.setOnClickListener(v -> showTimePicker(etEndTime));
        }

        if (btnAddShift != null) {
            btnAddShift.setOnClickListener(v -> {
                if (isEditing) {
                    updateShift();
                } else {
                    addShift();
                }
            });
        }
    }

    /* ---------------- Time Picker ---------------- */
    private void showTimePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        new TimePickerDialog(this,
                (view, h, m) -> {
                    if (target != null) {
                        target.setText(formatTime(h, m));
                    }
                },
                hour, minute, false).show();
    }

    private String formatTime(int hour, int minute) {
        String amPm = hour < 12 ? "AM" : "PM";
        int h = hour % 12;
        if (h == 0) h = 12;
        return String.format(Locale.getDefault(),
                "%02d:%02d %s", h, minute, amPm);
    }

    /* ---------------- Add Shift ---------------- */
    private void addShift() {
        String name = etShiftName.getText() != null
                ? etShiftName.getText().toString().trim() : "";
        String start = etStartTime.getText() != null
                ? etStartTime.getText().toString().trim() : "";
        String end = etEndTime.getText() != null
                ? etEndTime.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etShiftName.setError("Enter shift name");
            return;
        }
        if (TextUtils.isEmpty(start)) {
            Toast.makeText(this, "Select start time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(end)) {
            Toast.makeText(this, "Select end time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnAddShift != null) {
            btnAddShift.setEnabled(false);
        }

        shiftsRef.child(name)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(ShiftActivity.this,
                                    "Shift already exists", Toast.LENGTH_SHORT).show();
                            if (btnAddShift != null) {
                                btnAddShift.setEnabled(true);
                            }
                            return;
                        }

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("startTime", start);
                        map.put("endTime", end);
                        map.put("createdAt", System.currentTimeMillis());
                        map.put("totalEmployees", 0);

                        shiftsRef.child(name).setValue(map)
                                .addOnCompleteListener(task -> {
                                    if (btnAddShift != null) {
                                        btnAddShift.setEnabled(true);
                                    }
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ShiftActivity.this,
                                                "✅ Shift added successfully", Toast.LENGTH_SHORT).show();
                                        clearFields();
                                    } else {
                                        Toast.makeText(ShiftActivity.this,
                                                "Failed to add shift", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (btnAddShift != null) {
                            btnAddShift.setEnabled(true);
                        }
                    }
                });
    }

    /* ---------------- Edit Shift ---------------- */
    private void startEditShift(String shiftName, String startTime, String endTime) {
        isEditing = true;
        currentEditShiftName = shiftName;

        if (etShiftName != null) {
            etShiftName.setText(shiftName);
        }

        if (etStartTime != null) {
            etStartTime.setText(startTime);
        }

        if (etEndTime != null) {
            etEndTime.setText(endTime);
        }

        if (btnAddShift != null) {
            btnAddShift.setText("UPDATE SHIFT");
            btnAddShift.setIconResource(R.drawable.ic_edit);
        }

        if (etShiftName != null) {
            etShiftName.requestFocus();
        }

        // Scroll to top
        if (nestedScrollView != null) {
            nestedScrollView.post(() -> nestedScrollView.smoothScrollTo(0, 0));
        }
    }

    private void updateShift() {
        String name = etShiftName.getText() != null
                ? etShiftName.getText().toString().trim() : "";
        String start = etStartTime.getText() != null
                ? etStartTime.getText().toString().trim() : "";
        String end = etEndTime.getText() != null
                ? etEndTime.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            if (etShiftName != null) {
                etShiftName.setError("Enter shift name");
            }
            return;
        }
        if (TextUtils.isEmpty(start)) {
            Toast.makeText(this, "Select start time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(end)) {
            Toast.makeText(this, "Select end time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnAddShift != null) {
            btnAddShift.setEnabled(false);
        }

        if (!name.equals(currentEditShiftName)) {
            // Check if new name already exists
            shiftsRef.child(name).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(ShiftActivity.this,
                                "Shift name already exists", Toast.LENGTH_SHORT).show();
                        if (btnAddShift != null) {
                            btnAddShift.setEnabled(true);
                        }
                    } else {
                        performShiftUpdate(name, start, end);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (btnAddShift != null) {
                        btnAddShift.setEnabled(true);
                    }
                }
            });
        } else {
            performShiftUpdate(name, start, end);
        }
    }

    private void performShiftUpdate(String name, String start, String end) {
        if (!name.equals(currentEditShiftName)) {
            // Move data from old to new name
            shiftsRef.child(currentEditShiftName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Object shiftData = snapshot.getValue();
                        if (shiftData != null) {
                            // Add with new name
                            shiftsRef.child(name).setValue(shiftData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Delete old entry
                                        shiftsRef.child(currentEditShiftName).removeValue()
                                                .addOnSuccessListener(aVoid1 -> {
                                                    // Update employees with new shift name
                                                    updateEmployeesShiftName(currentEditShiftName, name);

                                                    HashMap<String, Object> updateData = new HashMap<>();
                                                    updateData.put("startTime", start);
                                                    updateData.put("endTime", end);
                                                    updateData.put("updatedAt", System.currentTimeMillis());

                                                    shiftsRef.child(name).updateChildren(updateData)
                                                            .addOnSuccessListener(aVoid2 -> {
                                                                finishEditing();
                                                                Toast.makeText(ShiftActivity.this,
                                                                        "✅ Shift updated successfully", Toast.LENGTH_SHORT).show();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                if (btnAddShift != null) {
                                                                    btnAddShift.setEnabled(true);
                                                                }
                                                                Toast.makeText(ShiftActivity.this,
                                                                        "Failed to update shift", Toast.LENGTH_SHORT).show();
                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                    if (btnAddShift != null) {
                                                        btnAddShift.setEnabled(true);
                                                    }
                                                    Toast.makeText(ShiftActivity.this,
                                                            "Failed to delete old shift", Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        if (btnAddShift != null) {
                                            btnAddShift.setEnabled(true);
                                        }
                                        Toast.makeText(ShiftActivity.this,
                                                "Failed to create new shift", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        if (btnAddShift != null) {
                            btnAddShift.setEnabled(true);
                        }
                        Toast.makeText(ShiftActivity.this, "Shift not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (btnAddShift != null) {
                        btnAddShift.setEnabled(true);
                    }
                }
            });
        } else {
            // Just update the time
            HashMap<String, Object> updateData = new HashMap<>();
            updateData.put("startTime", start);
            updateData.put("endTime", end);
            updateData.put("updatedAt", System.currentTimeMillis());

            shiftsRef.child(name).updateChildren(updateData)
                    .addOnSuccessListener(aVoid -> {
                        finishEditing();
                        Toast.makeText(ShiftActivity.this,
                                "✅ Shift updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        if (btnAddShift != null) {
                            btnAddShift.setEnabled(true);
                        }
                        Toast.makeText(ShiftActivity.this,
                                "Failed to update shift: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateEmployeesShiftName(String oldShiftName, String newShiftName) {
        employeesRef.orderByChild("info/employeeShift")
                .equalTo(oldShiftName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot empSnapshot : snapshot.getChildren()) {
                            String empKey = empSnapshot.getKey();
                            if (empKey != null) {
                                employeesRef.child(empKey).child("info")
                                        .child("employeeShift").setValue(newShiftName);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Silently fail - shift update should still complete
                        Log.e("ShiftActivity", "Error updating employee shifts: " + error.getMessage());
                    }
                });
    }

    private void finishEditing() {
        isEditing = false;
        currentEditShiftName = "";

        if (btnAddShift != null) {
            btnAddShift.setText("ADD SHIFT");
            btnAddShift.setIconResource(R.drawable.ic_add);
            btnAddShift.setEnabled(true);
        }

        clearFields();
    }

    /* ---------------- Delete Shift ---------------- */
    private void deleteShift(String shiftName) {
        // Check if any employees are assigned to this shift
        employeesRef.orderByChild("info/employeeShift")
                .equalTo(shiftName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            // Employees are assigned to this shift
                            new AlertDialog.Builder(ShiftActivity.this)
                                    .setTitle("Cannot Delete Shift")
                                    .setMessage(snapshot.getChildrenCount() + " employee(s) are assigned to this shift.\n" +
                                            "Please reassign them to another shift before deleting.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else {
                            // No employees assigned, safe to delete
                            confirmDeleteShift(shiftName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ShiftActivity.this,
                                "Error checking employees", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmDeleteShift(String shiftName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Shift")
                .setMessage("Are you sure you want to delete '" + shiftName + "' shift?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    shiftsRef.child(shiftName).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ShiftActivity.this,
                                        "Shift deleted successfully", Toast.LENGTH_SHORT).show();

                                // Reset edit mode if deleting the shift being edited
                                if (shiftName.equals(currentEditShiftName)) {
                                    finishEditing();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ShiftActivity.this,
                                        "Failed to delete shift: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* ---------------- Load Shifts ---------------- */
    private void loadShifts() {
        shiftsRef.orderByKey()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        shiftList.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String shiftName = ds.getKey();
                            String start = ds.child("startTime").getValue(String.class);
                            String end = ds.child("endTime").getValue(String.class);
                            Long createdAt = ds.child("createdAt").getValue(Long.class);
                            Long employeeCount = ds.child("totalEmployees").getValue(Long.class);

                            // Create ShiftModel with proper constructor
                            ShiftModel shift = new ShiftModel();
                            shift.setShiftName(shiftName != null ? shiftName : "Unknown");
                            shift.setStartTime(start != null ? start : "Not set");
                            shift.setEndTime(end != null ? end : "Not set");
                            shift.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());
                            shift.setEmployeeCount(employeeCount != null ? employeeCount.intValue() : 0);

                            shiftList.add(shift);
                        }

                        updateUI();
                        shiftAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ShiftActivity.this,
                                "Failed to load shifts: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI() {
        try {
            int count = shiftList.size();

            // Update count text
            if (tvCount != null) {
                tvCount.setText(count + " Shift" + (count != 1 ? "s" : ""));
            }

            // Show/hide empty state and recycler view
            if (count == 0) {
                if (llEmptyState != null) {
                    llEmptyState.setVisibility(View.VISIBLE);
                }
                if (rvShifts != null) {
                    rvShifts.setVisibility(View.GONE);
                }
            } else {
                if (llEmptyState != null) {
                    llEmptyState.setVisibility(View.GONE);
                }
                if (rvShifts != null) {
                    rvShifts.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            Log.e("ShiftActivity", "Error in updateUI: " + e.getMessage());
        }
    }

    private void clearFields() {
        if (etShiftName != null) {
            etShiftName.setText("");
            etShiftName.clearFocus();
        }

        if (etStartTime != null) {
            etStartTime.setText("");
        }

        if (etEndTime != null) {
            etEndTime.setText("");
        }
    }

    /* ---------------- Shift Adapter Class ---------------- */
    private class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.ViewHolder> {

        private List<ShiftModel> shifts;

        ShiftAdapter(List<ShiftModel> shifts) {
            this.shifts = shifts;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_shift, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ShiftModel shift = shifts.get(position);

            // Set shift details
            holder.tvShiftName.setText(shift.getShiftName());
            holder.tvShiftTime.setText(shift.getStartTime() + " - " + shift.getEndTime());

            // Calculate duration
            String duration = calculateDuration(shift.getStartTime(), shift.getEndTime());
            holder.tvShiftDuration.setText(duration);

            // Show employee count if available
            if (shift.getEmployeeCount() > 0) {
                holder.tvEmployeesCount.setText(shift.getEmployeeCount() + " employee(s)");
                holder.tvEmployeesCount.setVisibility(View.VISIBLE);
            } else {
                holder.tvEmployeesCount.setVisibility(View.GONE);
            }

            // Set click listeners for edit button
            holder.btnEdit.setOnClickListener(v -> {
                startEditShift(shift.getShiftName(), shift.getStartTime(), shift.getEndTime());
            });

            // Set click listeners for delete button
            holder.btnDelete.setOnClickListener(v -> {
                deleteShift(shift.getShiftName());
            });

            // Whole item click
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(ShiftActivity.this,
                        shift.getShiftName() + " shift selected",
                        Toast.LENGTH_SHORT).show();
            });
        }

        // Calculate duration between start and end time
        private String calculateDuration(String startTime, String endTime) {
            try {
                // Parse time strings like "9:00 AM"
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
                java.util.Date start = sdf.parse(startTime);
                java.util.Date end = sdf.parse(endTime);

                if (start != null && end != null) {
                    long diff = end.getTime() - start.getTime();
                    if (diff < 0) {
                        diff += 24 * 60 * 60 * 1000; // Add 24 hours for next day
                    }

                    long hours = diff / (60 * 60 * 1000);
                    long minutes = (diff % (60 * 60 * 1000)) / (60 * 1000);

                    if (minutes > 0) {
                        return hours + "h " + minutes + "m";
                    } else {
                        return hours + " hours";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Duration N/A";
        }

        @Override
        public int getItemCount() {
            return shifts.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvShiftName, tvShiftTime, tvShiftDuration, tvEmployeesCount;
            ImageButton btnEdit, btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvShiftName = itemView.findViewById(R.id.tvShiftName);
                tvShiftTime = itemView.findViewById(R.id.tvShiftTime);
                tvShiftDuration = itemView.findViewById(R.id.tvShiftDuration);
                tvEmployeesCount = itemView.findViewById(R.id.tvEmployeesCount);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}