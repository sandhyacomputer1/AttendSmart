package com.sandhyyasofttech.attendsmart.Activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;  // ✅ MaterialButton
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ShiftActivity extends AppCompatActivity {

    private TextInputEditText etShiftName, etStartTime, etEndTime;
    private MaterialButton btnAddShift;  // ✅ FIXED: MaterialButton (NOT ImageButton)
    private ListView lvShifts;

    private ArrayAdapter<String> adapter;
    private ArrayList<String> shiftList;

    private DatabaseReference shiftsRef;
    private String companyKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift);

        etShiftName = findViewById(R.id.etShiftName);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        btnAddShift = findViewById(R.id.btnAddShift);  // ✅ LINE 48: MaterialButton
        lvShifts = findViewById(R.id.lvShifts);

        shiftList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, shiftList);
        lvShifts.setAdapter(adapter);

        PrefManager prefManager = new PrefManager(this);
        String email = prefManager.getUserEmail();
        if (email == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        companyKey = email.replace(".", ",");

        shiftsRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("shifts");

        setupClickListeners();
        loadShifts();
    }

    private void setupClickListeners() {
        btnAddShift.setOnClickListener(v -> addShift());  // ✅ Works with MaterialButton
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));
    }

    private void showTimePicker(TextInputEditText timeField) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfDay) -> {
                    // ✅ 12-HOUR FORMAT with AM/PM
                    String time = String.format("%02d:%02d %s",
                            hourOfDay % 12 == 0 ? 12 : hourOfDay % 12,
                            minuteOfDay,
                            hourOfDay < 12 ? "AM" : "PM");
                    timeField.setText(time);
                },
                hour, minute,
                false  // ✅ false = 12-hour format
        );
        timePicker.show();
    }


    private void addShift() {
        String name = etShiftName.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etShiftName.setError("Enter shift name");
            return;
        }
        if (TextUtils.isEmpty(startTime)) {
            etStartTime.setError("Select start time");
            return;
        }
        if (TextUtils.isEmpty(endTime)) {
            etEndTime.setError("Select end time");
            return;
        }

        shiftsRef.child(name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(ShiftActivity.this, "Shift already exists", Toast.LENGTH_SHORT).show();
                } else {
                    HashMap<String, Object> shiftData = new HashMap<>();
                    shiftData.put("startTime", startTime);
                    shiftData.put("endTime", endTime);
                    shiftData.put("createdAt", System.currentTimeMillis());

                    shiftsRef.child(name).setValue(shiftData)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ShiftActivity.this, "✅ Shift added", Toast.LENGTH_SHORT).show();
                                    etShiftName.setText("");
                                    etStartTime.setText("");
                                    etEndTime.setText("");
                                } else {
                                    Toast.makeText(ShiftActivity.this, "Failed to add shift", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadShifts() {
        shiftsRef.orderByKey().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                shiftList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String shiftName = ds.getKey();
                    Object startObj = ds.child("startTime").getValue(Object.class);
                    Object endObj = ds.child("endTime").getValue(Object.class);
                    String startTime = startObj != null ? startObj.toString() : "N/A";
                    String endTime = endObj != null ? endObj.toString() : "N/A";
                    shiftList.add(shiftName + " (" + startTime + " - " + endTime + ")");
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
