//package com.sandhyyasofttech.attendsmart.Activities;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.NumberPicker;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.sandhyyasofttech.attendsmart.Adapters.CalendarDayAdapter;
//import com.sandhyyasofttech.attendsmart.Models.AttendanceDayModel;
//import com.sandhyyasofttech.attendsmart.R;
//import com.sandhyyasofttech.attendsmart.Utils.PrefManager;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//
//public class EmployeeMonthAttendanceActivity extends AppCompatActivity {
//
//    private static final String TAG = "EmpMonthAttendance";
//
//    private RecyclerView rvCalendar;
//    private TextView tvMonthYear;
//    private TextView tvPresentCount, tvAbsentCount, tvHalfDayCount, tvLateCount;
//    private ProgressBar progressBar;
//
//    private Calendar currentMonth;
//    private String employeeMobile;
//    private String companyKey;
//    private String currentJoiningDate = null;
//
//    private final List<AttendanceDayModel> calendarDays = new ArrayList<>();
//    private CalendarDayAdapter adapter;
//
//    private int monthlyPresentDays = 0;
//    private int monthlyLateDays = 0;
//    private int monthlyHalfDayDays = 0;
//    private int monthlyAbsentDays = 0;
//
//    private int[] weeklyHolidays = {};
//    private String employeeWeeklyHoliday = "";
//
//    private boolean isCalculatingStats = false;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_employee_month_attendance);
//
//        employeeMobile = getIntent().getStringExtra("employeeMobile");
//        companyKey = new PrefManager(this).getCompanyKey();
//
//        if (employeeMobile == null) {
//            Toast.makeText(this, "Employee not found", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        setupToolbar();
//        initViews();
//        setupCalendar();
//        setupListeners();
//        loadEmployeeDataAndAttendance();
//    }
//
//    private void setupToolbar() {
//        MaterialToolbar toolbar = findViewById(R.id.toolbar);
//        toolbar.setNavigationOnClickListener(v -> finish());
//    }
//
//    private void initViews() {
//        rvCalendar = findViewById(R.id.rvCalendar);
//        tvMonthYear = findViewById(R.id.tvMonthYear);
//        tvPresentCount = findViewById(R.id.tvPresentCount);
//        tvAbsentCount = findViewById(R.id.tvAbsentCount);
//        tvHalfDayCount = findViewById(R.id.tvHalfDayCount);
//        tvLateCount = findViewById(R.id.tvLateCount);
//        progressBar = findViewById(R.id.progressBar); // May be null if not in layout
//
//        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
//
//        adapter = new CalendarDayAdapter(
//                calendarDays,
//                v -> {
//                    AttendanceDayModel d = (AttendanceDayModel) v.getTag();
//                    if (d == null || d.isEmpty) return;
//
//                    Intent intent = new Intent(
//                            EmployeeMonthAttendanceActivity.this,
//                            AttendanceDayDetailActivity.class
//                    );
//                    intent.putExtra("date", d.date);
//                    intent.putExtra("employeeMobile", employeeMobile);
//                    intent.putExtra("companyKey", companyKey);
//                    startActivity(intent);
//                },
//                companyKey,
//                employeeMobile
//        );
//
//        rvCalendar.setAdapter(adapter);
//    }
//
//    private void setupListeners() {
//        tvMonthYear.setOnClickListener(v -> showMonthYearPicker());
//        findViewById(R.id.ivPrev).setOnClickListener(v -> changeMonth(-1));
//        findViewById(R.id.ivNext).setOnClickListener(v -> changeMonth(1));
//    }
//
//    private void setupCalendar() {
//        currentMonth = Calendar.getInstance();
//        updateMonthText();
//    }
//
//    private void changeMonth(int delta) {
//        currentMonth.add(Calendar.MONTH, delta);
//
//        if (currentMonth.after(Calendar.getInstance())) {
//            currentMonth.add(Calendar.MONTH, -delta);
//            Toast.makeText(this, "Future attendance not available", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        updateMonthText();
//        buildCalendarGrid();
//        calculateMonthlyStats();
//    }
//
//    private void updateMonthText() {
//        tvMonthYear.setText(
//                new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
//                        .format(currentMonth.getTime())
//        );
//    }
//
//    private void showMonthYearPicker() {
//        View view = LayoutInflater.from(this)
//                .inflate(R.layout.dialog_month_year_picker, null);
//
//        NumberPicker pickerMonth = view.findViewById(R.id.pickerMonth);
//        NumberPicker pickerYear = view.findViewById(R.id.pickerYear);
//
//        String[] months = {
//                "January", "February", "March", "April", "May", "June",
//                "July", "August", "September", "October", "November", "December"
//        };
//
//        pickerMonth.setMinValue(0);
//        pickerMonth.setMaxValue(11);
//        pickerMonth.setDisplayedValues(months);
//        pickerMonth.setValue(currentMonth.get(Calendar.MONTH));
//
//        pickerYear.setMinValue(2020);
//        pickerYear.setMaxValue(Calendar.getInstance().get(Calendar.YEAR));
//        pickerYear.setValue(currentMonth.get(Calendar.YEAR));
//
//        new AlertDialog.Builder(this)
//                .setTitle("Select Month & Year")
//                .setView(view)
//                .setPositiveButton("Apply", (d, w) -> {
//                    currentMonth.set(Calendar.MONTH, pickerMonth.getValue());
//                    currentMonth.set(Calendar.YEAR, pickerYear.getValue());
//                    updateMonthText();
//                    buildCalendarGrid();
//                    calculateMonthlyStats();
//                })
//                .setNegativeButton("Cancel", null)
//                .show();
//    }
//
//    /**
//     * ✅ Load employee data first (holiday + joining date), then build calendar
//     */
//    private void loadEmployeeDataAndAttendance() {
//        if (progressBar != null) {
//            progressBar.setVisibility(View.VISIBLE);
//        }
//
//        DatabaseReference employeesRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("employees")
//                .child(employeeMobile)
//                .child("info");
//
//        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot infoNode) {
//                // ✅ Fetch weekly holiday
//                employeeWeeklyHoliday = infoNode.child("weeklyHoliday").getValue(String.class);
//                currentJoiningDate = infoNode.child("joinDate").getValue(String.class);
//
//                // ✅ Convert day name to Calendar constant
//                weeklyHolidays = convertDayNameToCalendarArray(employeeWeeklyHoliday);
//
//                Log.d(TAG, "Employee Mobile: " + employeeMobile);
//                Log.d(TAG, "Weekly Holiday: " + employeeWeeklyHoliday);
//                Log.d(TAG, "Joining Date: " + currentJoiningDate);
//
//                // ✅ Update adapter with holidays
//                adapter.updateWeeklyHolidays(weeklyHolidays);
//
//                // ✅ Build calendar and calculate stats
//                buildCalendarGrid();
//                calculateMonthlyStats();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                if (progressBar != null) {
//                    progressBar.setVisibility(View.GONE);
//                }
//                Log.e(TAG, "Error loading employee data: " + error.getMessage());
//            }
//        });
//    }
//
//    /**
//     * ✅ Convert day name to Calendar constant
//     */
//    private int[] convertDayNameToCalendarArray(String dayName) {
//        if (dayName == null || dayName.trim().isEmpty()) {
//            return new int[]{Calendar.SUNDAY}; // Default fallback
//        }
//
//        dayName = dayName.trim();
//
//        switch (dayName) {
//            case "Sunday":
//                return new int[]{Calendar.SUNDAY};
//            case "Monday":
//                return new int[]{Calendar.MONDAY};
//            case "Tuesday":
//                return new int[]{Calendar.TUESDAY};
//            case "Wednesday":
//                return new int[]{Calendar.WEDNESDAY};
//            case "Thursday":
//                return new int[]{Calendar.THURSDAY};
//            case "Friday":
//                return new int[]{Calendar.FRIDAY};
//            case "Saturday":
//                return new int[]{Calendar.SATURDAY};
//            default:
//                Log.w(TAG, "Unknown day name: " + dayName + ", defaulting to Sunday");
//                return new int[]{Calendar.SUNDAY};
//        }
//    }
//
//    /**
//     * ✅ Build calendar grid with proper empty cells
//     */
//    private void buildCalendarGrid() {
//        calendarDays.clear();
//
//        Calendar cal = (Calendar) currentMonth.clone();
//        cal.set(Calendar.DAY_OF_MONTH, 1);
//
//        // Add empty cells for days before month starts
//        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
//        for (int i = 0; i < firstDayOfWeek; i++) {
//            calendarDays.add(new AttendanceDayModel("", "Empty", true));
//        }
//
//        // Add all days in the month
//        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//
//        for (int day = 1; day <= daysInMonth; day++) {
//            cal.set(Calendar.DAY_OF_MONTH, day);
//            String dateStr = sdf.format(cal.getTime());
//
//            // Initially set all as "Loading"
//            calendarDays.add(new AttendanceDayModel(dateStr, "Loading", false));
//        }
//
//        adapter.notifyDataSetChanged();
//    }
//
//    /**
//     * ✅ Calculate monthly stats considering joining date and holidays
//     */
//    private void calculateMonthlyStats() {
//        if (isCalculatingStats) {
//            Log.d(TAG, "Already calculating...");
//            return;
//        }
//
//        if (employeeMobile == null) {
//            Log.w(TAG, "Employee mobile not loaded yet");
//            return;
//        }
//
//        isCalculatingStats = true;
//        if (progressBar != null) {
//            progressBar.setVisibility(View.VISIBLE);
//        }
//        resetMonthlyStats();
//
//        Log.d(TAG, "Calculating for: " +
//                new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.getTime()));
//
//        calculateMonthAttendance(employeeMobile, currentJoiningDate);
//    }
//
//    /**
//     * ✅ Calculate attendance considering joining date and weekly holiday
//     */
//    private void calculateMonthAttendance(String employeeMobile, String joiningDate) {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        Calendar monthCal = (Calendar) currentMonth.clone();
//        monthCal.set(Calendar.DAY_OF_MONTH, 1);
//        int daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
//        String today = sdf.format(new Date());
//
//        final int[] present = {0}, late = {0}, halfDay = {0}, absent = {0}, checks = {0};
//        List<String> validDates = new ArrayList<>();
//
//        for (int day = 1; day <= daysInMonth; day++) {
//            monthCal.set(Calendar.DAY_OF_MONTH, day);
//            String dateStr = sdf.format(monthCal.getTime());
//
//            // ✅ Skip future dates
//            if (dateStr.compareTo(today) > 0) {
//                Log.d(TAG, "Skipping future date: " + dateStr);
//                updateCalendarDayStatus(dateStr, "Future");
//                continue;
//            }
//
//            // ✅ Skip dates before joining
//            if (joiningDate != null && dateStr.compareTo(joiningDate) < 0) {
//                Log.d(TAG, "Skipping before joining: " + dateStr + " (Join: " + joiningDate + ")");
//                updateCalendarDayStatus(dateStr, "Before Joining");
//                continue;
//            }
//
//            // ✅ Skip weekly holidays
//            if (isHoliday(dateStr)) {
//                Log.d(TAG, "Skipping holiday: " + dateStr + " (" + employeeWeeklyHoliday + ")");
//                updateCalendarDayStatus(dateStr, "Holiday");
//                continue;
//            }
//
//            validDates.add(dateStr);
//            checks[0]++;
//        }
//
//        Log.d(TAG, "Total valid working dates: " + checks[0]);
//
//        if (checks[0] == 0) {
//            finishCalculation(0, 0, 0, 0);
//            return;
//        }
//
//        DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
//                .getReference("Companies")
//                .child(companyKey)
//                .child("attendance");
//
//        for (String dateStr : validDates) {
//            DatabaseReference dateRef = attendanceRef.child(dateStr).child(employeeMobile);
//            dateRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot snapshot) {
//                    processAttendance(snapshot, dateStr, present, late, halfDay, absent, checks);
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError error) {
//                    checks[0]--;
//                    if (checks[0] == 0)
//                        finishCalculation(present[0], late[0], halfDay[0], absent[0]);
//                }
//            });
//        }
//    }
//
//    /**
//     * ✅ Process individual attendance record
//     */
//    private void processAttendance(DataSnapshot snapshot, String dateStr,
//                                   final int[] present, final int[] late,
//                                   final int[] halfDay, final int[] absent, final int[] checks) {
//
//        String finalStatus;
//
//        if (snapshot.exists()) {
//            String status = snapshot.child("status").getValue(String.class);
//            String lateStatus = snapshot.child("lateStatus").getValue(String.class);
//            String checkInTime = snapshot.child("checkInTime").getValue(String.class);
//
//            boolean hasCheckIn = checkInTime != null && !checkInTime.isEmpty();
//            String statusSafe = status != null ? status.toLowerCase() : "";
//
//            boolean countedPresent = false;
//
//            // ✅ 1. HALF DAY (highest priority after presence check)
//            if (statusSafe.contains("half")) {
//                present[0]++;
//                halfDay[0]++;
//                countedPresent = true;
//                finalStatus = "Half Day";
//            }
//            // ✅ 2. LATE
//            else if ("late".equalsIgnoreCase(lateStatus) || statusSafe.contains("late")) {
//                present[0]++;
//                late[0]++;
//                countedPresent = true;
//                finalStatus = "Late";
//            }
//            // ✅ 3. PRESENT (Full Day or has check-in)
//            else if (hasCheckIn || statusSafe.contains("present") || statusSafe.contains("full")) {
//                present[0]++;
//                countedPresent = true;
//                finalStatus = "Present";
//            }
//            // ❌ No valid check-in or status
//            else {
//                absent[0]++;
//                finalStatus = "Absent";
//            }
//
//        } else {
//            // ❌ No record at all
//            absent[0]++;
//            finalStatus = "Absent";
//        }
//
//        updateCalendarDayStatus(dateStr, finalStatus);
//
//        checks[0]--;
//        if (checks[0] == 0) {
//            finishCalculation(present[0], late[0], halfDay[0], absent[0]);
//        }
//    }
//
//    /**
//     * ✅ Update specific day in calendar
//     */
//    private void updateCalendarDayStatus(String dateStr, String status) {
//        for (AttendanceDayModel day : calendarDays) {
//            if (!day.isEmpty && day.date.equals(dateStr)) {
//                day.status = status;
//                break;
//            }
//        }
//    }
//
//    private void finishCalculation(int present, int late, int halfDay, int absent) {
//        monthlyPresentDays = present;
//        monthlyLateDays = late;
//        monthlyHalfDayDays = halfDay;
//        monthlyAbsentDays = absent;
//
//        isCalculatingStats = false;
//        if (progressBar != null) {
//            progressBar.setVisibility(View.GONE);
//        }
//        runOnUiThread(() -> {
//            updateStatsDisplay();
//            adapter.notifyDataSetChanged();
//        });
//    }
//
//    private void resetMonthlyStats() {
//        monthlyPresentDays = 0;
//        monthlyLateDays = 0;
//        monthlyHalfDayDays = 0;
//        monthlyAbsentDays = 0;
//    }
//
//    private void updateStatsDisplay() {
//        tvPresentCount.setText(String.valueOf(monthlyPresentDays));
//        tvLateCount.setText(String.valueOf(monthlyLateDays));
//        tvHalfDayCount.setText(String.valueOf(monthlyHalfDayDays));
//        tvAbsentCount.setText(String.valueOf(monthlyAbsentDays));
//
//        Log.d(TAG, "=== MONTHLY STATS ===");
//        Log.d(TAG, "Present: " + monthlyPresentDays + " | Late: " + monthlyLateDays);
//        Log.d(TAG, "Half Day: " + monthlyHalfDayDays + " | Absent: " + monthlyAbsentDays);
//    }
//
//    /**
//     * ✅ Check if date is a holiday based on employee's weekly holiday
//     */
//    private boolean isHoliday(String dateStr) {
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//            Date date = sdf.parse(dateStr);
//            if (date == null) return false;
//
//            Calendar cal = Calendar.getInstance();
//            cal.setTime(date);
//            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
//
//            for (int holiday : weeklyHolidays) {
//                if (dayOfWeek == holiday) {
//                    return true;
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error checking holiday: " + e.getMessage());
//        }
//        return false;
//    }
//}




package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeMonthAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "EmpMonthAttendance";

    private RecyclerView rvCalendar;
    private TextView tvMonthYear;
    private TextView tvPresentCount, tvAbsentCount, tvHalfDayCount, tvLateCount;
    private ProgressBar progressBar;

    private Calendar currentMonth;
    private String employeeMobile;
    private String companyKey;
    private String currentJoiningDate = null;

    private final List<AttendanceDayModel> calendarDays = new ArrayList<>();
    private CalendarDayAdapter adapter;

    private int monthlyPresentDays = 0;
    private int monthlyLateDays = 0;
    private int monthlyHalfDayDays = 0;
    private int monthlyAbsentDays = 0;

    private int[] weeklyHolidays = {};
    private String employeeWeeklyHoliday = "";

    private boolean isCalculatingStats = false;

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
        loadEmployeeDataAndAttendance();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvCalendar = findViewById(R.id.rvCalendar);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvHalfDayCount = findViewById(R.id.tvHalfDayCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        progressBar = findViewById(R.id.progressBar); // May be null if not in layout

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
                companyKey,
                employeeMobile
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
        buildCalendarGrid();
        calculateMonthlyStats();
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
                    buildCalendarGrid();
                    calculateMonthlyStats();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * ✅ Load employee data first (holiday + joining date), then build calendar
     */
    private void loadEmployeeDataAndAttendance() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        DatabaseReference employeesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("info");

        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot infoNode) {
                // ✅ Fetch weekly holiday
                employeeWeeklyHoliday = infoNode.child("weeklyHoliday").getValue(String.class);
                currentJoiningDate = infoNode.child("joinDate").getValue(String.class);

                // ✅ Convert day name to Calendar constant
                weeklyHolidays = convertDayNameToCalendarArray(employeeWeeklyHoliday);

                Log.d(TAG, "Employee Mobile: " + employeeMobile);
                Log.d(TAG, "Weekly Holiday: " + employeeWeeklyHoliday);
                Log.d(TAG, "Joining Date: " + currentJoiningDate);

                // ✅ Update adapter with holidays
                adapter.updateWeeklyHolidays(weeklyHolidays);

                // ✅ Build calendar and calculate stats
                buildCalendarGrid();
                calculateMonthlyStats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Log.e(TAG, "Error loading employee data: " + error.getMessage());
            }
        });
    }

    /**
     * ✅ Convert day name to Calendar constant
     */
    private int[] convertDayNameToCalendarArray(String dayName) {
        if (dayName == null || dayName.trim().isEmpty()) {
            return new int[]{Calendar.SUNDAY}; // Default fallback
        }

        dayName = dayName.trim();

        switch (dayName) {
            case "Sunday":
                return new int[]{Calendar.SUNDAY};
            case "Monday":
                return new int[]{Calendar.MONDAY};
            case "Tuesday":
                return new int[]{Calendar.TUESDAY};
            case "Wednesday":
                return new int[]{Calendar.WEDNESDAY};
            case "Thursday":
                return new int[]{Calendar.THURSDAY};
            case "Friday":
                return new int[]{Calendar.FRIDAY};
            case "Saturday":
                return new int[]{Calendar.SATURDAY};
            default:
                Log.w(TAG, "Unknown day name: " + dayName + ", defaulting to Sunday");
                return new int[]{Calendar.SUNDAY};
        }
    }

    /**
     * ✅ Build calendar grid with proper empty cells
     */
    private void buildCalendarGrid() {
        calendarDays.clear();

        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // Add empty cells for days before month starts
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = 0; i < firstDayOfWeek; i++) {
            calendarDays.add(new AttendanceDayModel("", "Empty", true));
        }

        // Add all days in the month
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = sdf.format(cal.getTime());

            // Initially set all as "Loading"
            calendarDays.add(new AttendanceDayModel(dateStr, "Loading", false));
        }

        adapter.notifyDataSetChanged();
    }

    /**
     * ✅ Calculate monthly stats considering joining date and holidays
     */
    private void calculateMonthlyStats() {
        if (isCalculatingStats) {
            Log.d(TAG, "Already calculating...");
            return;
        }

        if (employeeMobile == null) {
            Log.w(TAG, "Employee mobile not loaded yet");
            return;
        }

        isCalculatingStats = true;
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        resetMonthlyStats();

        Log.d(TAG, "Calculating for: " +
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.getTime()));

        calculateMonthAttendance(employeeMobile, currentJoiningDate);
    }

    /**
     * ✅ Calculate attendance considering joining date and weekly holiday
     */
    /**
     * ✅ Calculate attendance considering joining date and weekly holiday
     */
    private void calculateMonthAttendance(String employeeMobile, String joiningDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar monthCal = (Calendar) currentMonth.clone();
        monthCal.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String today = sdf.format(new Date());

        final int[] present = {0}, late = {0}, halfDay = {0}, absent = {0}, checks = {0};
        List<String> validDates = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            monthCal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = sdf.format(monthCal.getTime());

            // ✅ Skip future dates
            if (dateStr.compareTo(today) > 0) {
                Log.d(TAG, "Skipping future date: " + dateStr);
                updateCalendarDayStatus(dateStr, "Future");
                continue;
            }

            // ✅ Skip dates before joining
            if (joiningDate != null && dateStr.compareTo(joiningDate) < 0) {
                Log.d(TAG, "Skipping before joining: " + dateStr + " (Join: " + joiningDate + ")");
                updateCalendarDayStatus(dateStr, "Before Joining");
                continue;
            }

            // ✅ Mark weekly holidays
            if (isHoliday(dateStr)) {
                Log.d(TAG, "Marking as weekly holiday: " + dateStr + " (" + employeeWeeklyHoliday + ")");
                updateCalendarDayStatus(dateStr, "Weekly Holiday");
                continue; // Don't check attendance for holidays
            }

            validDates.add(dateStr);
            checks[0]++;
        }

        Log.d(TAG, "Total valid working dates: " + checks[0]);

        if (checks[0] == 0) {
            finishCalculation(0, 0, 0, 0);
            return;
        }

        DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("attendance");

        for (String dateStr : validDates) {
            DatabaseReference dateRef = attendanceRef.child(dateStr).child(employeeMobile);
            dateRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    processAttendance(snapshot, dateStr, present, late, halfDay, absent, checks);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    checks[0]--;
                    if (checks[0] == 0)
                        finishCalculation(present[0], late[0], halfDay[0], absent[0]);
                }
            });
        }
    }
    /**
     * ✅ Process individual attendance record
     */
    /**
     * ✅ Process individual attendance record
     */
    private void processAttendance(DataSnapshot snapshot, String dateStr,
                                   final int[] present, final int[] late,
                                   final int[] halfDay, final int[] absent, final int[] checks) {

        String finalStatus;

        if (snapshot.exists()) {
            String status = snapshot.child("status").getValue(String.class);
            String lateStatus = snapshot.child("lateStatus").getValue(String.class);
            String checkInTime = snapshot.child("checkInTime").getValue(String.class);
            String finalStatusFromDB = snapshot.child("finalStatus").getValue(String.class); // ADD THIS

            boolean hasCheckIn = checkInTime != null && !checkInTime.isEmpty();
            String statusSafe = status != null ? status.toLowerCase() : "";
            String finalStatusSafe = finalStatusFromDB != null ? finalStatusFromDB.toLowerCase() : "";

            // ✅ CHECK FOR HOLIDAY FIRST (highest priority)
            if (statusSafe.contains("holiday") || finalStatusSafe.contains("holiday")) {
                finalStatus = "Holiday";
                // Don't increment any counters for holidays
                updateCalendarDayStatus(dateStr, finalStatus);
                checks[0]--;
                if (checks[0] == 0) {
                    finishCalculation(present[0], late[0], halfDay[0], absent[0]);
                }
                return; // Exit early
            }

            boolean countedPresent = false;

            // ✅ 1. HALF DAY (highest priority after holiday)
            if (statusSafe.contains("half")) {
                present[0]++;
                halfDay[0]++;
                countedPresent = true;
                finalStatus = "Half Day";
            }
            // ✅ 2. LATE
            else if ("late".equalsIgnoreCase(lateStatus) || statusSafe.contains("late")) {
                present[0]++;
                late[0]++;
                countedPresent = true;
                finalStatus = "Late";
            }
            // ✅ 3. PRESENT (Full Day or has check-in)
            else if (hasCheckIn || statusSafe.contains("present") || statusSafe.contains("full")) {
                present[0]++;
                countedPresent = true;
                finalStatus = "Present";
            }
            // ❌ No valid check-in or status
            else {
                absent[0]++;
                finalStatus = "Absent";
            }

        } else {
            // ❌ No record at all
            absent[0]++;
            finalStatus = "Absent";
        }

        updateCalendarDayStatus(dateStr, finalStatus);

        checks[0]--;
        if (checks[0] == 0) {
            finishCalculation(present[0], late[0], halfDay[0], absent[0]);
        }
    }

    /**
     * ✅ Update specific day in calendar
     */
    private void updateCalendarDayStatus(String dateStr, String status) {
        for (AttendanceDayModel day : calendarDays) {
            if (!day.isEmpty && day.date.equals(dateStr)) {
                day.status = status;
                break;
            }
        }
    }

    private void finishCalculation(int present, int late, int halfDay, int absent) {
        monthlyPresentDays = present;
        monthlyLateDays = late;
        monthlyHalfDayDays = halfDay;
        monthlyAbsentDays = absent;

        isCalculatingStats = false;
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        runOnUiThread(() -> {
            updateStatsDisplay();
            adapter.notifyDataSetChanged();
        });
    }

    private void resetMonthlyStats() {
        monthlyPresentDays = 0;
        monthlyLateDays = 0;
        monthlyHalfDayDays = 0;
        monthlyAbsentDays = 0;
    }

    private void updateStatsDisplay() {
        tvPresentCount.setText(String.valueOf(monthlyPresentDays));
        tvLateCount.setText(String.valueOf(monthlyLateDays));
        tvHalfDayCount.setText(String.valueOf(monthlyHalfDayDays));
        tvAbsentCount.setText(String.valueOf(monthlyAbsentDays));

        Log.d(TAG, "=== MONTHLY STATS ===");
        Log.d(TAG, "Present: " + monthlyPresentDays + " | Late: " + monthlyLateDays);
        Log.d(TAG, "Half Day: " + monthlyHalfDayDays + " | Absent: " + monthlyAbsentDays);
    }

    /**
     * ✅ Check if date is a holiday based on employee's weekly holiday
     */
    private boolean isHoliday(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date == null) return false;

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            for (int holiday : weeklyHolidays) {
                if (dayOfWeek == holiday) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking holiday: " + e.getMessage());
        }
        return false;
    }
}