
package com.sandhyyasofttech.attendsmart.Activities;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminEmployeeAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "AdminAttendance";

    private MaterialToolbar topAppBar;
    private RecyclerView rvCalendar;
    private TextView tvMonthYear, tvEmployeeName, tvEmployeeRole, tvEmployeeMobile;
    private TextView tvPresentCount, tvLateCount, tvHalfDayCount, tvAbsentCount;
    private ImageView ivPrevMonth, ivNextMonth;
    private ProgressBar progressBar;

    private PrefManager prefManager;
    private String companyKey;
    private DatabaseReference attendanceRef, employeesRef;
    private Calendar currentMonth;
    private List<String> allAttendanceDates;
    private CalendarAdapter calendarAdapter;

    private String employeeMobile, employeeName, employeeRole;
    private String joiningDate;

    private int weeklyHolidayDay = -1; // ✅ ADD THIS



    private int monthlyPresentDays = 0;
    private int monthlyLateDays = 0;
    private int monthlyHalfDayDays = 0;
    private int monthlyAbsentDays = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_employee_attendance);

        getEmployeeDataFromIntent();
        initViews();
        setupFirebase();
        setupToolbar();
        setupFirebase();
        setupToolbar();
        loadEmployeeInfo();
        fetchEmployeeWeeklyHoliday(); // 👈 this sets weeklyHolidayDay

    }

    private void getEmployeeDataFromIntent() {
        employeeMobile = getIntent().getStringExtra("employeeMobile");
        employeeName = getIntent().getStringExtra("employeeName");
        employeeRole = getIntent().getStringExtra("employeeRole");

        if (employeeMobile == null || employeeMobile.isEmpty()) {
            Toast.makeText(this, "Employee data missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchEmployeeWeeklyHoliday() {
        employeesRef
                .child(employeeMobile)
                .child("info")
                .child("weeklyHoliday")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String holiday = snapshot.getValue(String.class);
                        weeklyHolidayDay = getDayOfWeekFromString(holiday);

                        setupCalendar();
                        loadAllAttendanceDates();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        setupCalendar();
                        loadAllAttendanceDates();
                    }
                });
    }
    private int getDayOfWeekFromString(String day) {
        if (day == null) return -1;

        switch (day.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return -1;
        }
    }


    private void initViews() {
        topAppBar = findViewById(R.id.topAppBar);
        rvCalendar = findViewById(R.id.rvCalendar);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvEmployeeRole = findViewById(R.id.tvEmployeeRole);
        tvEmployeeMobile = findViewById(R.id.tvEmployeeMobile);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        tvHalfDayCount = findViewById(R.id.tvHalfDayCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        ivPrevMonth = findViewById(R.id.ivPrevMonth);
        ivNextMonth = findViewById(R.id.ivNextMonth);
        progressBar = findViewById(R.id.progressBar);
        prefManager = new PrefManager(this);

        setupMonthNavigation();
    }

    private void setupToolbar() {
        setSupportActionBar(topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Employee Attendance");
        }
        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCalendar() {
        currentMonth = Calendar.getInstance();
        allAttendanceDates = new ArrayList<>();

        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        calendarAdapter = new CalendarAdapter(
                currentMonth,
                allAttendanceDates,
                weeklyHolidayDay,
                this::showDateDetails,
                companyKey,
                employeeMobile
        );

        rvCalendar.setAdapter(calendarAdapter);

        updateMonthDisplay();
    }

    private void setupMonthNavigation() {
        ivPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            calendarAdapter.updateMonth(currentMonth);
            calculateMonthlyStats();
        });

        ivNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            calendarAdapter.updateMonth(currentMonth);
            calculateMonthlyStats();
        });
    }

    private void setupFirebase() {
        companyKey = prefManager.getCompanyKey();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        attendanceRef = db.getReference("Companies").child(companyKey).child("attendance");
        employeesRef = db.getReference("Companies").child(companyKey).child("employees");
    }

    private void loadEmployeeInfo() {
        tvEmployeeName.setText(employeeName != null ? employeeName : "N/A");
        tvEmployeeRole.setText(employeeRole != null ? employeeRole : "N/A");
        tvEmployeeMobile.setText(employeeMobile);

        employeesRef.child(employeeMobile).child("info").child("joiningDate")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        joiningDate = snapshot.getValue(String.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadAllAttendanceDates() {
        progressBar.setVisibility(View.VISIBLE);
        allAttendanceDates.clear();

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                    if (dateSnap.hasChild(employeeMobile)) {
                        allAttendanceDates.add(dateSnap.getKey());
                    }
                }
                calendarAdapter.updateAttendanceDates(allAttendanceDates);
                calendarAdapter.updateFirebaseData(companyKey, employeeMobile);
                calculateMonthlyStats();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminEmployeeAttendanceActivity.this,
                        "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateMonthlyStats() {
        progressBar.setVisibility(View.VISIBLE);
        resetMonthlyStats();

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

            if (dateStr.compareTo(today) > 0) continue;
            if (joiningDate != null && dateStr.compareTo(joiningDate) < 0) continue;
            if (isHoliday(dateStr)) continue;

            validDates.add(dateStr);
            checks[0]++;
        }

        if (checks[0] == 0) {
            finishCalculation(0, 0, 0, 0);
            return;
        }

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
                    if (checks[0] == 0) finishCalculation(present[0], late[0], halfDay[0], absent[0]);
                }
            });
        }
    }

    private void processAttendance(DataSnapshot snapshot, String dateStr,
                                   final int[] present, final int[] late,
                                   final int[] halfDay, final int[] absent, final int[] checks) {

        if (snapshot.exists()) {
            String status = snapshot.child("status").getValue(String.class);
            String lateStatus = snapshot.child("lateStatus").getValue(String.class);
            String checkInTime = snapshot.child("checkInTime").getValue(String.class);
            String checkOutTime = snapshot.child("checkOutTime").getValue(String.class);

            boolean hasCheckIn = checkInTime != null && !checkInTime.isEmpty();
            boolean hasCheckOut = checkOutTime != null && !checkOutTime.isEmpty();

            // Priority order for counting:
            // 1. Half Day status (counts as both present and half day)
            if (status != null && status.equalsIgnoreCase("Half Day")) {
                present[0]++;
                halfDay[0]++;
            }
            // 2. Late status (counts as both present and late)
            else if (lateStatus != null && lateStatus.equalsIgnoreCase("Late") && hasCheckIn) {
                present[0]++;
                late[0]++;
            }
            // 3. Present or Full Day status
            else if (status != null && (status.equalsIgnoreCase("Present") ||
                    status.equalsIgnoreCase("Full Day"))) {
                present[0]++;
            }
            // 4. Has check-in (at least marked attendance)
            else if (hasCheckIn) {
                present[0]++;
            }
            // 5. Marked absent explicitly
            else if (status != null && status.equalsIgnoreCase("Absent")) {
                absent[0]++;
            }
            // 6. No data means absent
            else {
                absent[0]++;
            }
        } else {
            // No attendance record = absent
            absent[0]++;
        }

        checks[0]--;
        if (checks[0] == 0) {
            finishCalculation(present[0], late[0], halfDay[0], absent[0]);
        }
    }

    private void finishCalculation(int present, int late, int halfDay, int absent) {
        monthlyPresentDays = present;
        monthlyLateDays = late;
        monthlyHalfDayDays = halfDay;
        monthlyAbsentDays = absent;

        progressBar.setVisibility(View.GONE);
        runOnUiThread(this::updateStatsDisplay);
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
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonth.getTime()));
    }

    private void showDateDetails(String date) {
        Intent intent = new Intent(this, AdminDayAttendanceDetailActivity.class);
        intent.putExtra("companyKey", companyKey);
        intent.putExtra("employeeMobile", employeeMobile);
        intent.putExtra("employeeName", employeeName);
        intent.putExtra("employeeRole", employeeRole);
        intent.putExtra("date", date);
        startActivity(intent);
    }

    private boolean isHoliday(String dateStr) {
        if (weeklyHolidayDay == -1) return false;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            return cal.get(Calendar.DAY_OF_WEEK) == weeklyHolidayDay;
        } catch (Exception e) {
            return false;
        }
    }

    // Calendar Adapter
    public static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private Calendar monthCalendar;
        private List<String> attendanceDates;
        private DateClickListener listener;
        private String today;
        private String companyKey;
        private int weeklyHolidayDay;

        private String employeeMobile;

        public interface DateClickListener {
            void onDateSelected(String date);
        }

        public CalendarAdapter(Calendar monthCalendar,
                               List<String> attendanceDates,
                               int weeklyHolidayDay,
                               DateClickListener listener,
                               String companyKey,
                               String employeeMobile) {

            this.monthCalendar = (Calendar) monthCalendar.clone();
            this.attendanceDates = new ArrayList<>(attendanceDates);
            this.weeklyHolidayDay = weeklyHolidayDay;
            this.listener = listener;
            this.companyKey = companyKey;
            this.employeeMobile = employeeMobile;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            this.today = sdf.format(new Date());
        }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // First row: Day headers
            if (position < 7) {
                String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
                holder.tvDay.setText(daysOfWeek[position]);
                holder.tvDay.setTextColor(Color.parseColor("#757575"));
                holder.tvDay.setTextSize(14);
                holder.tvDay.setVisibility(View.VISIBLE);
                holder.containerDay.setBackground(null);
                holder.itemView.setClickable(false);
                holder.itemView.setAlpha(1f);
                return;
            }

            int actualPosition = position - 7;
            int day = getDayOfMonth(actualPosition);
            boolean isCurrentMonth = isCurrentMonthDay(actualPosition);

            if (isCurrentMonth && day > 0) {
                String dateStr = getDateString(day);
                boolean hasAttendance = attendanceDates.contains(dateStr);
                boolean isHolidayDay = isHolidayDay(day);
                boolean isTodayDay = dateStr.equals(today);
                boolean isPastDateDay = isPastDate(dateStr);

                holder.tvDay.setText(String.valueOf(day));
                holder.tvDay.setVisibility(View.VISIBLE);
                holder.tvDay.setTextSize(16);
                holder.itemView.setAlpha(1f);

                // Check if today first to give it priority
                if (isTodayDay && hasAttendance && companyKey != null && employeeMobile != null) {
                    // Today with attendance - fetch real status but mark as today
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_purple);
                    holder.tvDay.setTextColor(Color.WHITE);
                    checkRealStatus(holder, dateStr);
                } else if (isTodayDay && !hasAttendance) {
                    // Today without attendance
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_purple);
                    holder.tvDay.setTextColor(Color.WHITE);
                } else if (hasAttendance && companyKey != null && employeeMobile != null) {
                    // Has attendance (not today) - fetch real status
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                    holder.tvDay.setTextColor(Color.WHITE);
                    checkRealStatus(holder, dateStr);
                } else if (isHolidayDay) {
                    // Weekly holiday
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_orange);
                    holder.tvDay.setTextColor(Color.WHITE);
                } else if (isPastDateDay) {
                    // Past date with no attendance = Absent
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
                    holder.tvDay.setTextColor(Color.WHITE);
                } else {
                    // Future date
                    holder.containerDay.setBackground(null);
                    holder.tvDay.setTextColor(Color.parseColor("#212121"));
                }

                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDateSelected(dateStr);
                    }
                });
            } else {
                // Empty cell (not current month)
                holder.tvDay.setText("");
                holder.tvDay.setVisibility(View.INVISIBLE);
                holder.itemView.setAlpha(0.3f);
                holder.containerDay.setBackground(null);
                holder.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return 49; }

        private void checkRealStatus(ViewHolder holder, String dateStr) {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("Companies").child(companyKey).child("attendance")
                    .child(dateStr).child(employeeMobile);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String status = snapshot.child("status").getValue(String.class);
                        String lateStatus = snapshot.child("lateStatus").getValue(String.class);
                        String checkInTime = snapshot.child("checkInTime").getValue(String.class);

                        holder.tvDay.setTextColor(Color.WHITE);

                        // Priority order for background colors:
                        // 1. Half Day = Blue
                        if (status != null && status.equalsIgnoreCase("Half Day")) {
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_blue);
                        }
                        // 2. Late = Yellow/Orange
                        else if (lateStatus != null && lateStatus.equalsIgnoreCase("Late")) {
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_yellow);
                        }
                        // 3. Present or Full Day = Green
                        else if (status != null && (status.equalsIgnoreCase("Present") ||
                                status.equalsIgnoreCase("Full Day"))) {
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                        }
                        // 4. Has check-in = Green
                        else if (checkInTime != null && !checkInTime.isEmpty()) {
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                        }
                        // 5. Absent = Red
                        else if (status != null && status.equalsIgnoreCase("Absent")) {
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
                        }
                        // 6. Default = Green (has attendance record)
                        else {
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // On error, keep default green
                }
            });
        }

        private int getDayOfMonth(int position) {
            monthCalendar.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOffset = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1;
            int offset = position - firstDayOffset;

            if (offset < 0) {
                Calendar prevMonth = (Calendar) monthCalendar.clone();
                prevMonth.add(Calendar.MONTH, -1);
                return prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH) + offset + 1;
            }
            if (offset >= monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                return offset - monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1;
            }
            return offset + 1;
        }

        private boolean isCurrentMonthDay(int position) {
            monthCalendar.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOffset = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1;
            int offset = position - firstDayOffset;
            return offset >= 0 && offset < monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        }

        private boolean isHolidayDay(int day) {
            if (weeklyHolidayDay == -1) return false;

            Calendar cal = (Calendar) monthCalendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, day);
            return cal.get(Calendar.DAY_OF_WEEK) == weeklyHolidayDay;
        }

        private String getDateString(int day) {
            Calendar cal = (Calendar) monthCalendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, day);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(cal.getTime());
        }

        private boolean isPastDate(String dateStr) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdf.parse(dateStr);
                Date todayDate = sdf.parse(today);
                return date != null && todayDate != null && date.before(todayDate);
            } catch (Exception e) {
                return false;
            }
        }

        public void updateAttendanceDates(List<String> dates) {
            this.attendanceDates.clear();
            this.attendanceDates.addAll(dates);
            notifyDataSetChanged();
        }

        public void updateMonth(Calendar newMonth) {
            this.monthCalendar = (Calendar) newMonth.clone();
            notifyDataSetChanged();
        }

        public void updateFirebaseData(String companyKey, String employeeMobile) {
            this.companyKey = companyKey;
            this.employeeMobile = employeeMobile;
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDay;
            FrameLayout containerDay;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDay = itemView.findViewById(R.id.tvDayNumber);
                containerDay = itemView.findViewById(R.id.containerDay);
            }
        }
    }
}