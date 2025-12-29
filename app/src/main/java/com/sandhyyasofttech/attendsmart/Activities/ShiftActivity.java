package com.sandhyyasofttech.attendsmart.Activities;

import android.app.TimePickerDialog;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.ShiftAdapter;
import com.sandhyyasofttech.attendsmart.Models.ShiftModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class ShiftActivity extends AppCompatActivity {

    private TextInputEditText etShiftName, etStartTime, etEndTime;
    private MaterialButton btnAddShift;
    private RecyclerView rvShifts;

    private ShiftAdapter shiftAdapter;
    private ArrayList<ShiftModel> shiftList;

    private DatabaseReference shiftsRef;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift);

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

        shiftList = new ArrayList<>();
        shiftAdapter = new ShiftAdapter(shiftList);

        rvShifts.setLayoutManager(new LinearLayoutManager(this));
        rvShifts.setAdapter(shiftAdapter);
        rvShifts.setHasFixedSize(true);
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

        shiftsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("shifts");
    }

    /* ---------------- Listeners ---------------- */
    private void setupListeners() {
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));
        btnAddShift.setOnClickListener(v -> addShift());
    }

    /* ---------------- Time Picker ---------------- */
    private void showTimePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        new TimePickerDialog(this,
                (view, h, m) -> target.setText(formatTime(h, m)),
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
            etStartTime.setError("Select start time");
            return;
        }
        if (TextUtils.isEmpty(end)) {
            etEndTime.setError("Select end time");
            return;
        }

        btnAddShift.setEnabled(false);

        shiftsRef.child(name)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(ShiftActivity.this,
                                    "Shift already exists", Toast.LENGTH_SHORT).show();
                            btnAddShift.setEnabled(true);
                            return;
                        }

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("startTime", start);
                        map.put("endTime", end);
                        map.put("createdAt", System.currentTimeMillis());

                        shiftsRef.child(name).setValue(map)
                                .addOnCompleteListener(task -> {
                                    btnAddShift.setEnabled(true);
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ShiftActivity.this,
                                                "✅ Shift added", Toast.LENGTH_SHORT).show();
                                        clearFields();
                                    } else {
                                        Toast.makeText(ShiftActivity.this,
                                                "Failed to add shift", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnAddShift.setEnabled(true);
                    }
                });
    }

    private void clearFields() {
        etShiftName.setText("");
        etStartTime.setText("");
        etEndTime.setText("");
    }

    /* ---------------- Load Shifts ---------------- */
    private void loadShifts() {
        shiftsRef.orderByKey()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        shiftList.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String name = ds.getKey();
                            String start = ds.child("startTime").getValue(String.class);
                            String end = ds.child("endTime").getValue(String.class);

                            shiftList.add(new ShiftModel(
                                    name,
                                    start != null ? start : "N/A",
                                    end != null ? end : "N/A"
                            ));
                        }
                        shiftAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
