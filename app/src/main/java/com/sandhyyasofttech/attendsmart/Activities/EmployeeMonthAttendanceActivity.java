package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.CalendarDayAdapter;
import com.sandhyyasofttech.attendsmart.Models.AttendanceDayModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EmployeeMonthAttendanceActivity extends AppCompatActivity {

    private RecyclerView rvCalendar;
    private TextView tvMonthYear;
    private TextView tvPresentCount, tvAbsentCount, tvHalfDayCount, tvLateCount;

    private Calendar currentMonth;
    private String employeeMobile;
    private String companyKey;

    private final List<AttendanceDayModel> calendarDays = new ArrayList<>();
    private CalendarDayAdapter adapter;

    private int presentCount = 0, absentCount = 0, halfDayCount = 0, lateCount = 0;
    private int weeklyHolidayDay = -1; // Calendar.SUNDAY, etc.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_month_attendance);

        employeeMobile = getIntent().getStringExtra("employeeMobile");
        companyKey = new PrefManager(this).getCompanyKey();

        if (employeeMobile == null) {
            Toast.makeText(this, "Employee not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initViews();
        setupCalendar();
        setupListeners();
        loadMonthAttendance();
        fetchEmployeeWeeklyHoliday();

    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    private void fetchEmployeeWeeklyHoliday() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("info")
                .child("weeklyHoliday");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String holiday = snapshot.getValue(String.class);

                if (holiday != null) {
                    weeklyHolidayDay = getDayOfWeekFromString(holiday);
                }

                // ✅ load attendance only AFTER holiday is fetched
                loadMonthAttendance();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // fallback: no holiday
                loadMonthAttendance();
            }
        });
    }
    private int getDayOfWeekFromString(String day) {
        if (day == null) return -1;

        switch (day.toLowerCase()) {
            case "sunday":
                return Calendar.SUNDAY;
            case "monday":
                return Calendar.MONDAY;
            case "tuesday":
                return Calendar.TUESDAY;
            case "wednesday":
                return Calendar.WEDNESDAY;
            case "thursday":
                return Calendar.THURSDAY;
            case "friday":
                return Calendar.FRIDAY;
            case "saturday":
                return Calendar.SATURDAY;
            default:
                return -1;
        }
    }

    private void initViews() {
        rvCalendar = findViewById(R.id.rvCalendar);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvHalfDayCount = findViewById(R.id.tvHalfDayCount);
        tvLateCount = findViewById(R.id.tvLateCount);

        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));

        adapter = new CalendarDayAdapter(
                calendarDays,
                v -> {
                    AttendanceDayModel d = (AttendanceDayModel) v.getTag();
                    if (d == null || d.isEmpty) return;

                    Intent intent = new Intent(
                            EmployeeMonthAttendanceActivity.this,
                            AttendanceDayDetailActivity.class
                    );
                    intent.putExtra("date", d.date);
                    intent.putExtra("employeeMobile", employeeMobile);
                    intent.putExtra("companyKey", companyKey);
                    startActivity(intent);
                },
                null // ✅ third argument (stats listener not needed here)
        );

        rvCalendar.setAdapter(adapter);
    }

    private void setupListeners() {
        tvMonthYear.setOnClickListener(v -> showMonthYearPicker());
        findViewById(R.id.ivPrev).setOnClickListener(v -> changeMonth(-1));
        findViewById(R.id.ivNext).setOnClickListener(v -> changeMonth(1));
    }

    private void setupCalendar() {
        currentMonth = Calendar.getInstance();
        updateMonthText();
    }

    private void changeMonth(int delta) {
        currentMonth.add(Calendar.MONTH, delta);

        if (currentMonth.after(Calendar.getInstance())) {
            currentMonth.add(Calendar.MONTH, -delta);
            Toast.makeText(this, "Future attendance not available", Toast.LENGTH_SHORT).show();
            return;
        }

        updateMonthText();
        loadMonthAttendance();
    }

    private void updateMonthText() {
        tvMonthYear.setText(
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        .format(currentMonth.getTime())
        );
    }

    private void showMonthYearPicker() {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_month_year_picker, null);

        NumberPicker pickerMonth = view.findViewById(R.id.pickerMonth);
        NumberPicker pickerYear = view.findViewById(R.id.pickerYear);

        String[] months = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        pickerMonth.setMinValue(0);
        pickerMonth.setMaxValue(11);
        pickerMonth.setDisplayedValues(months);
        pickerMonth.setValue(currentMonth.get(Calendar.MONTH));

        pickerYear.setMinValue(2020);
        pickerYear.setMaxValue(Calendar.getInstance().get(Calendar.YEAR));
        pickerYear.setValue(currentMonth.get(Calendar.YEAR));

        new AlertDialog.Builder(this)
                .setTitle("Select Month & Year")
                .setView(view)
                .setPositiveButton("Apply", (d, w) -> {
                    currentMonth.set(Calendar.MONTH, pickerMonth.getValue());
                    currentMonth.set(Calendar.YEAR, pickerYear.getValue());
                    updateMonthText();
                    loadMonthAttendance();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadMonthAttendance() {
        calendarDays.clear();
        presentCount = 0;
        absentCount = 0;
        halfDayCount = 0;
        lateCount = 0;

        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = 0; i < firstDayOfWeek; i++) {
            calendarDays.add(new AttendanceDayModel("", "Empty", true));
        }

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            calendarDays.add(new AttendanceDayModel(
                    sdf.format(cal.getTime()),
                    "Absent",
                    false
            ));
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("attendance");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Calendar today = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayStr = dateFormat.format(today.getTime());

                for (AttendanceDayModel d : calendarDays) {
                    if (d.isEmpty) continue;

                    Calendar dayCal = Calendar.getInstance();
                    try {
                        dayCal.setTime(
                                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        .parse(d.date)
                        );
                    } catch (Exception e) {
                        continue;
                    }

                    // 🔮 Future date
                    if (d.date.compareTo(todayStr) > 0) {
                        d.status = "Future";
                        continue;
                    }

                    // 🟠 EMPLOYEE WEEKLY HOLIDAY
                    if (weeklyHolidayDay != -1 &&
                            dayCal.get(Calendar.DAY_OF_WEEK) == weeklyHolidayDay) {
                        d.status = "Holiday";
                        continue; // ❌ do NOT count
                    }

                    DataSnapshot s = snapshot.child(d.date).child(employeeMobile);

                    if (s.exists()) {
                        String status = s.child("status").getValue(String.class);
                        String late = s.child("lateStatus").getValue(String.class);
                        String checkIn = s.child("checkInTime").getValue(String.class);

                        boolean hasCheckIn = checkIn != null && !checkIn.isEmpty();

                        // 1️⃣ Half Day → Present + Half Day
                        if ("Half Day".equalsIgnoreCase(status)) {
                            d.status = "Half Day";
                            presentCount++;
                            halfDayCount++;
                        }
                        // 2️⃣ Late → Present + Late
                        else if ("Late".equalsIgnoreCase(late) && hasCheckIn) {
                            d.status = "Late";
                            presentCount++;
                            lateCount++;
                        }
                        // 3️⃣ Present
                        else if (hasCheckIn ||
                                "Present".equalsIgnoreCase(status) ||
                                "Full Day".equalsIgnoreCase(status)) {
                            d.status = "Present";
                            presentCount++;
                        }
                        // 4️⃣ Absent
                        else {
                            d.status = "Absent";
                            absentCount++;
                        }
                    } else {
                        d.status = "Absent";
                        absentCount++;
                    }
                }

                updateStatistics();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void updateStatistics() {
        tvPresentCount.setText(String.valueOf(presentCount));
        tvAbsentCount.setText(String.valueOf(absentCount));
        tvHalfDayCount.setText(String.valueOf(halfDayCount));
        tvLateCount.setText(String.valueOf(lateCount));
    }
}