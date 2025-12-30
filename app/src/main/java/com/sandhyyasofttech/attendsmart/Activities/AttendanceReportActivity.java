package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceReportActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceReport";

    private RecyclerView rvCalendar;
    private TextView tvMonthYear, tvPresentDaysCount, tvLateDaysCount, tvAbsentDaysCount;
    private ImageView ivPrevMonth, ivNextMonth;
    private LinearLayout llDetailsContainer;
    private ProgressBar progressBar;

    private PrefManager prefManager;
    private String companyKey;
    private DatabaseReference employeesRef, attendanceRef;
    private String selectedDate;
    private Calendar currentMonth;
    private List<String> allAttendanceDates;
    private CalendarAdapter calendarAdapter;

    private int[] weeklyHolidays = {Calendar.SUNDAY};

    // Monthly statistics
    private int monthlyPresentDays = 0;
    private int monthlyLateDays = 0;
    private int monthlyAbsentDays = 0;

    private boolean isCalculatingStats = false;
    private String currentEmployeeMobile = null;

    // Store employee joining date
    private String employeeJoiningDate = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_report);

        initViews();
        setupFirebase();
        setupCalendar();
        loadAllAttendanceDates();
    }

    private void initViews() {
        rvCalendar = findViewById(R.id.rvCalendar);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvPresentDaysCount = findViewById(R.id.tvPresentDaysCount);
        tvLateDaysCount = findViewById(R.id.tvLateDaysCount);
        tvAbsentDaysCount = findViewById(R.id.tvAbsentDaysCount);
        ivPrevMonth = findViewById(R.id.ivPrevMonth);
        ivNextMonth = findViewById(R.id.ivNextMonth);
        llDetailsContainer = findViewById(R.id.llDetailsContainer);
        progressBar = findViewById(R.id.progressBar);
        prefManager = new PrefManager(this);

        setupMonthNavigation();
    }

    private void setupCalendar() {
        currentMonth = Calendar.getInstance();
        allAttendanceDates = new ArrayList<>();

        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        calendarAdapter = new CalendarAdapter(currentMonth, allAttendanceDates, weeklyHolidays, this::showDateDetails);
        rvCalendar.setAdapter(calendarAdapter);

        updateMonthDisplay();
    }

    private void setupMonthNavigation() {
        ivPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            calendarAdapter.updateMonth(currentMonth);
            // Small delay to prevent overlapping calculations
            rvCalendar.postDelayed(this::calculateMonthlyStats, 100);
        });

        ivNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            calendarAdapter.updateMonth(currentMonth);
            // Small delay to prevent overlapping calculations
            rvCalendar.postDelayed(this::calculateMonthlyStats, 100);
        });
    }
    private void setupFirebase() {
        companyKey = prefManager.getCompanyKey();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        employeesRef = db.getReference("Companies").child(companyKey).child("employees");
        attendanceRef = db.getReference("Companies").child(companyKey).child("attendance");
    }

    private void loadAllAttendanceDates() {
        progressBar.setVisibility(View.VISIBLE);
        allAttendanceDates.clear();

        String employeeEmail = prefManager.getEmployeeEmail();
        if (employeeEmail == null) return;

        employeesRef.orderByChild("info/employeeEmail").equalTo(employeeEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot empSnap) {
                        if (!empSnap.exists()) return;

                        String mobile = empSnap.getChildren().iterator().next().getKey();

                        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                                    if (dateSnap.hasChild(mobile)) {
                                        allAttendanceDates.add(dateSnap.getKey()); // ✅ ONLY THIS EMPLOYEE
                                    }
                                }
                                calendarAdapter.updateAttendanceDates(allAttendanceDates);
                                calculateMonthlyStats();
                                progressBar.setVisibility(View.GONE);
                            }

                            @Override public void onCancelled(@NonNull DatabaseError error) {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }


    // Replace calculateMonthlyStats() completely
    private void calculateMonthlyStats() {
        if (isCalculatingStats) {
            Log.d(TAG, "Already calculating...");
            return;
        }

        String employeeEmail = prefManager.getEmployeeEmail();
        if (employeeEmail == null) return;

        isCalculatingStats = true;
        progressBar.setVisibility(View.VISIBLE);
        resetMonthlyStats();

        Log.d(TAG, "Calculating for: " +
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.getTime()));

        // Get employee mobile from employees node
        employeesRef.orderByChild("info/employeeEmail").equalTo(employeeEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            DataSnapshot empData = snapshot.getChildren().iterator().next();
                            currentEmployeeMobile = empData.getKey(); // ✅ 8605042157
                            String joiningDate = empData.child("info/joiningDate").getValue(String.class);

                            Log.d(TAG, "Employee mobile: " + currentEmployeeMobile);
                            calculateMonthAttendance(currentEmployeeMobile, joiningDate);
                        } else {
                            finishCalculation(0, 0, 0);
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        finishCalculation(0, 0, 0);
                    }
                });
    }


    private void calculateMonthAttendance(String employeeMobile, String joiningDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar monthCal = (Calendar) currentMonth.clone();
        monthCal.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String today = sdf.format(new Date());

        final int[] present = {0}, late = {0}, absent = {0}, checks = {0};
        List<String> validDates = new ArrayList<>();

        // Get valid dates for THIS MONTH
        for (int day = 1; day <= daysInMonth; day++) {
            monthCal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = sdf.format(monthCal.getTime());

            // Skip future, pre-joining, holidays
            if (dateStr.compareTo(today) > 0) continue;
            if (joiningDate != null && dateStr.compareTo(joiningDate) < 0) continue;
            if (isHoliday(dateStr)) continue;

            validDates.add(dateStr);
            checks[0]++;
        }

        Log.d(TAG, "Valid dates: " + validDates.size());

        if (checks[0] == 0) {
            finishCalculation(0, 0, 0);
            return;
        }

        // Check attendance for each valid date
        for (String dateStr : validDates) {
            DatabaseReference dateRef = attendanceRef.child(dateStr).child(employeeMobile);
            dateRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    processAttendance(snapshot, dateStr, present, late, absent, checks);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    checks[0]--;
                    if (checks[0] == 0) finishCalculation(present[0], late[0], absent[0]);
                }
            });
        }
    }

    // 4. Process single day attendance (YOUR ORIGINAL LOGIC)
    private void processAttendance(DataSnapshot snapshot, String dateStr,
                                   final int[] present, final int[] late, final int[] absent, final int[] checks) {
        if (snapshot.exists()) {
            String status = snapshot.child("status").getValue(String.class);
            Log.d(TAG, dateStr + " Status: " + status);

            if (status != null) {
                if (status.equalsIgnoreCase("Late")) {
                    late[0]++;
                    present[0]++;
                } else if (status.equalsIgnoreCase("Present") || status.equalsIgnoreCase("On Time")) {
                    present[0]++;
                }
            } else {
                String checkInTime = snapshot.child("checkInTime").getValue(String.class);
                if (checkInTime != null && !checkInTime.isEmpty()) {
                    present[0]++;
                } else {
                    absent[0]++;
                }
            }
        } else {
            absent[0]++;
            Log.d(TAG, dateStr + " - Absent (no record)");
        }

        checks[0]--;
        if (checks[0] == 0) {
            Log.d(TAG, "MONTH COMPLETE - P:" + present[0] + " L:" + late[0] + " A:" + absent[0]);
            finishCalculation(present[0], late[0], absent[0]);
        }
    }

    // 5. Safe UI update
    private void finishCalculation(int present, int late, int absent) {
        monthlyPresentDays = present;
        monthlyLateDays = late;
        monthlyAbsentDays = absent;
        isCalculatingStats = false;
        progressBar.setVisibility(View.GONE);
        runOnUiThread(this::updateStatsDisplay);
    }

    // 6. Reset method
    private void resetMonthlyStats() {
        monthlyPresentDays = 0;
        monthlyLateDays = 0;
        monthlyAbsentDays = 0;
    }

    // Add this new method to reset stats

    private void calculateEmployeeMonthStats(String employeeMobile) {
        resetMonthlyStats(); // Reset first

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String today = sdf.format(new Date());

        Log.d(TAG, "Days in month: " + daysInMonth);
        Log.d(TAG, "Today: " + today);

        // ✅ FIX: Use arrays for effectively final counters
        final int[] remainingChecks = {0};
        final List<String> validDates = new ArrayList<>();

        // First pass: Find ALL valid working days for this month ONLY
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = sdf.format(cal.getTime());

            // Skip future dates
            if (dateStr.compareTo(today) > 0) continue;

            // Skip dates before joining
            if (employeeJoiningDate != null && !employeeJoiningDate.isEmpty()) {
                if (dateStr.compareTo(employeeJoiningDate) < 0) continue;
            }

            // Skip holidays
            if (isHoliday(dateStr)) continue;

            validDates.add(dateStr);
            remainingChecks[0]++;
        }

        Log.d(TAG, "Valid working days for this month: " + validDates.size());

        if (remainingChecks[0] == 0) {
            updateStatsDisplay();
            return;
        }

        // Second pass: Check attendance for ONLY valid dates in this month
        for (String dateStr : validDates) {
            checkDayAttendanceForMonth(employeeMobile, dateStr, remainingChecks);
        }
    }

    private void checkDayAttendanceForMonth(String employeeMobile, String dateStr, final int[] remainingChecks) {
        attendanceRef.child(dateStr).child(employeeMobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // YOUR ORIGINAL LOGIC - PERFECT!
                            String status = snapshot.child("status").getValue(String.class);
                            Log.d(TAG, "Date: " + dateStr + " - Status: " + status);

                            if (status != null) {
                                if (status.equalsIgnoreCase("Late")) {
                                    monthlyLateDays++;
                                    monthlyPresentDays++; // Late is still present ✅
                                } else if (status.equalsIgnoreCase("Present") ||
                                        status.equalsIgnoreCase("On Time")) {
                                    monthlyPresentDays++;
                                }
                            } else {
                                // Fallback: checkInTime exists = present
                                String checkInTime = snapshot.child("checkInTime").getValue(String.class);
                                if (checkInTime != null && !checkInTime.isEmpty()) {
                                    monthlyPresentDays++;
                                    Log.d(TAG, "Date: " + dateStr + " - Present (has check-in)");
                                } else {
                                    monthlyAbsentDays++;
                                    Log.d(TAG, "Date: " + dateStr + " - Absent");
                                }
                            }
                        } else {
                            // No record = Absent ✅
                            monthlyAbsentDays++;
                            Log.d(TAG, "Date: " + dateStr + " - Absent (no record)");
                        }

                        remainingChecks[0]--;
                        if (remainingChecks[0] == 0) {
                            Log.d(TAG, "=== MONTH CALCULATION COMPLETE ===");
                            Log.d(TAG, "Present: " + monthlyPresentDays + ", Late: " + monthlyLateDays + ", Absent: " + monthlyAbsentDays);
                            updateStatsDisplay();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking " + dateStr + ": " + error.getMessage());
                        remainingChecks[0]--;
                        if (remainingChecks[0] == 0) {
                            updateStatsDisplay();
                        }
                    }
                });
    }
    private void checkDayAttendance(String employeeMobile, String dateStr, int[] remainingChecks) {
        attendanceRef.child(dateStr).child(employeeMobile)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Check for status field
                            String status = snapshot.child("status").getValue(String.class);

                            Log.d(TAG, "Date: " + dateStr + " - Status: " + status);

                            if (status != null) {
                                if (status.equalsIgnoreCase("Late")) {
                                    monthlyLateDays++;
                                    monthlyPresentDays++; // Late is still present
                                } else if (status.equalsIgnoreCase("Present") ||
                                        status.equalsIgnoreCase("On Time")) {
                                    monthlyPresentDays++;
                                }
                            } else {
                                // If no status but has checkInTime, consider as present
                                String checkInTime = snapshot.child("checkInTime").getValue(String.class);
                                if (checkInTime != null && !checkInTime.isEmpty()) {
                                    monthlyPresentDays++;
                                    Log.d(TAG, "Date: " + dateStr + " - Present (has check-in)");
                                } else {
                                    monthlyAbsentDays++;
                                    Log.d(TAG, "Date: " + dateStr + " - Absent");
                                }
                            }
                        } else {
                            // No attendance record = Absent
                            monthlyAbsentDays++;
                            Log.d(TAG, "Date: " + dateStr + " - Absent (no record)");
                        }

                        remainingChecks[0]--;
                        if (remainingChecks[0] == 0) {
                            // All checks complete, update UI
                            updateStatsDisplay();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking attendance for " + dateStr + ": " + error.getMessage());
                        remainingChecks[0]--;
                        if (remainingChecks[0] == 0) {
                            updateStatsDisplay();
                        }
                    }
                });
    }

    private void updateStatsDisplay() {
        runOnUiThread(() -> {
            tvPresentDaysCount.setText(String.valueOf(monthlyPresentDays));
            tvLateDaysCount.setText(String.valueOf(monthlyLateDays));
            tvAbsentDaysCount.setText(String.valueOf(monthlyAbsentDays));

            Log.d(TAG, "=== Final Stats ===");
            Log.d(TAG, "Present Days: " + monthlyPresentDays);
            Log.d(TAG, "Late Days: " + monthlyLateDays);
            Log.d(TAG, "Absent Days: " + monthlyAbsentDays);
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonth.getTime()));
    }

    private void showDateDetails(String date) {
        Intent intent = new Intent(this, AttendanceDayDetailsActivity.class);
        intent.putExtra("date", date);
        startActivity(intent);
    }

    private void loadDateAttendanceDetails(Map<String, EmployeeInfo> employees, String date) {
        attendanceRef.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int present = 0, absent = 0, late = 0;
                List<EmployeeAttendance> attendanceList = new ArrayList<>();
                boolean isHoliday = isHoliday(date);

                // Process employees who have attendance records
                for (DataSnapshot empSnapshot : snapshot.getChildren()) {
                    String mobile = empSnapshot.getKey();
                    EmployeeInfo empInfo = employees.get(mobile);
                    if (empInfo != null) {
                        EmployeeAttendance attendance = parseAttendance(empSnapshot, empInfo);
                        attendanceList.add(attendance);

                        if (attendance.status.equalsIgnoreCase("Present") ||
                                attendance.status.equalsIgnoreCase("On Time")) {
                            present++;
                        } else if (attendance.status.equalsIgnoreCase("Late")) {
                            late++;
                            present++; // Late is still present
                        }
                    }
                }

                // Add absent employees (those without attendance on non-holiday days)
                if (!isHoliday) {
                    for (Map.Entry<String, EmployeeInfo> entry : employees.entrySet()) {
                        String mobile = entry.getKey();
                        if (!snapshot.hasChild(mobile)) {
                            EmployeeAttendance emp = new EmployeeAttendance(
                                    entry.getValue().name, entry.getValue().role, mobile, "Absent",
                                    null, null, null, null, null, null, null, null, null, null, null
                            );
                            attendanceList.add(emp);
                            absent++;
                        }
                    }
                }

                // Sort: Present -> Late -> Absent -> then by name
                attendanceList.sort((a, b) -> {
                    if (a.status.equals("Present") && !b.status.equals("Present")) return -1;
                    if (!a.status.equals("Present") && b.status.equals("Present")) return 1;
                    if (a.status.equals("Late") && !b.status.equals("Late")) return -1;
                    if (!a.status.equals("Late") && b.status.equals("Late")) return 1;
                    return a.name.compareToIgnoreCase(b.name);
                });

                showSummary(employees.size(), present, absent, late, isHoliday);
                showEmployeeList(attendanceList);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private boolean isHoliday(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            for (int holiday : weeklyHolidays) {
                if (dayOfWeek == holiday) return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showSummary(int total, int present, int absent, int late, boolean isHolidayDay) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View summaryView = inflater.inflate(R.layout.item_date_summary, llDetailsContainer, false);

        TextView tvDate = summaryView.findViewById(R.id.tvDate);
        TextView tvTotal = summaryView.findViewById(R.id.tvTotal);
        TextView tvPresent = summaryView.findViewById(R.id.tvPresent);
        TextView tvAbsent = summaryView.findViewById(R.id.tvAbsent);
        TextView tvLate = summaryView.findViewById(R.id.tvLate);
        TextView tvHoliday = summaryView.findViewById(R.id.tvHoliday);

        tvDate.setText("Date: " + selectedDate + (isHolidayDay ? " (Holiday)" : ""));
        tvTotal.setText("Total: " + total);
        tvPresent.setText("Present: " + present);
        tvPresent.setTextColor(Color.parseColor("#4CAF50"));
        tvAbsent.setText("Absent: " + absent);
        tvAbsent.setTextColor(Color.parseColor("#F44336"));
        tvLate.setText("Late: " + late);
        tvLate.setTextColor(Color.parseColor("#FF9800"));

        tvHoliday.setVisibility(View.GONE);

        llDetailsContainer.addView(summaryView);
    }

    private void showEmployeeList(List<EmployeeAttendance> attendanceList) {
        SearchView searchView = new SearchView(this);
        searchView.setQueryHint("Search employees...");
        searchView.setIconified(false);

        RecyclerView rvEmployees = new RecyclerView(this);
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        EmployeeAdapter adapter = new EmployeeAdapter(attendanceList, this);
        rvEmployees.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, 16, 0, 8);
        searchView.setLayoutParams(searchParams);

        LinearLayout.LayoutParams rvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        rvEmployees.setLayoutParams(rvParams);

        llDetailsContainer.addView(searchView);
        llDetailsContainer.addView(rvEmployees);
    }

    // Inner classes remain the same...
    public static class EmployeeInfo {
        String name, role;
        public EmployeeInfo(String name, String role) {
            this.name = name;
            this.role = role;
        }
    }

    public static class EmployeeAttendance {
        String name, role, mobile, status;
        String checkInTime, checkOutTime, totalHours;
        Double checkInLat, checkInLng, checkOutLat, checkOutLng;
        String checkInAddr, checkOutAddr, checkInPhoto, checkOutPhoto;

        public EmployeeAttendance(String name, String role, String mobile, String status,
                                  String checkInTime, String checkOutTime, String totalHours,
                                  Double checkInLat, Double checkInLng, String checkInAddr,
                                  Double checkOutLat, Double checkOutLng, String checkOutAddr,
                                  String checkInPhoto, String checkOutPhoto) {
            this.name = name;
            this.role = role;
            this.mobile = mobile;
            this.status = status;
            this.checkInTime = checkInTime;
            this.checkOutTime = checkOutTime;
            this.totalHours = totalHours;
            this.checkInLat = checkInLat;
            this.checkInLng = checkInLng;
            this.checkInAddr = checkInAddr;
            this.checkOutLat = checkOutLat;
            this.checkOutLng = checkOutLng;
            this.checkOutAddr = checkOutAddr;
            this.checkInPhoto = checkInPhoto;
            this.checkOutPhoto = checkOutPhoto;
        }
    }

    // CalendarAdapter and EmployeeAdapter remain the same as in your original code
    // ... (keeping them unchanged to avoid making response too long)




    public static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private Calendar monthCalendar;
        private List<String> attendanceDates;
        private int[] weeklyHolidays;
        private DateClickListener listener;
        private String today;

        public interface DateClickListener {
            void onDateSelected(String date);
        }

        public CalendarAdapter(Calendar monthCalendar, List<String> attendanceDates,
                               int[] weeklyHolidays, DateClickListener listener) {
            this.monthCalendar = (Calendar) monthCalendar.clone();
            this.attendanceDates = new ArrayList<>(attendanceDates);
            this.weeklyHolidays = weeklyHolidays;
            this.listener = listener;

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
                boolean isToday = dateStr.equals(today);
                boolean isAbsentDay = !hasAttendance && !isHolidayDay && isPastDate(dateStr);

                holder.tvDay.setText(String.valueOf(day));
                holder.tvDay.setVisibility(View.VISIBLE);
                holder.tvDay.setTextSize(16);
                holder.itemView.setAlpha(1f);

                if (hasAttendance) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                    holder.tvDay.setTextColor(Color.WHITE);
                } else if (isHolidayDay) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_orange);
                    holder.tvDay.setTextColor(Color.WHITE);
                } else if (isAbsentDay) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
                    holder.tvDay.setTextColor(Color.WHITE);
                } else if (isToday) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_blue);
                    holder.tvDay.setTextColor(Color.WHITE);
                } else {
                    holder.containerDay.setBackground(null);
                    holder.tvDay.setTextColor(Color.parseColor("#212121"));
                }

                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDateSelected(dateStr);
                    }
                });
            } else {
                holder.tvDay.setText("");
                holder.tvDay.setVisibility(View.INVISIBLE);
                holder.itemView.setAlpha(0.3f);
                holder.containerDay.setBackground(null);
                holder.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return 49; }

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
            Calendar cal = (Calendar) monthCalendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, day);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            for (int holiday : weeklyHolidays) {
                if (dayOfWeek == holiday) return true;
            }
            return false;
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
                e.printStackTrace();
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

    public static class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {
        private List<EmployeeAttendance> originalList, filteredList;
        private AttendanceReportActivity context;

        public EmployeeAdapter(List<EmployeeAttendance> list, AttendanceReportActivity context) {
            this.originalList = new ArrayList<>(list);
            this.filteredList = new ArrayList<>(list);
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attendance_employee, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EmployeeAttendance item = filteredList.get(position);

            holder.tvName.setText(item.name);
            holder.tvRole.setText(item.role);
            holder.tvStatus.setText(item.status);

            int statusColor = item.status.equals("Present") ? Color.parseColor("#4CAF50") :
                    item.status.equals("Late") ? Color.parseColor("#FF9800") :
                            Color.parseColor("#F44336");
            holder.tvStatus.setTextColor(statusColor);

            holder.tvCheckIn.setText(item.checkInTime != null ? item.checkInTime : "-");
            holder.tvCheckOut.setText(item.checkOutTime != null ? item.checkOutTime : "-");
            holder.tvHours.setText(item.totalHours != null ? item.totalHours + "h" : "-");

            if (item.checkInLat != null && item.checkInLng != null) {
                String locationText = item.checkInAddr != null && !item.checkInAddr.isEmpty() ?
                        item.checkInAddr.substring(0, Math.min(30, item.checkInAddr.length())) + "..." :
                        String.format(Locale.getDefault(), "%.4f, %.4f", item.checkInLat, item.checkInLng);
                holder.tvLocation.setText(locationText);
                holder.tvLocation.setOnClickListener(v -> openGoogleMaps(item.checkInLat, item.checkInLng));
            } else {
                holder.tvLocation.setText("-");
            }

            if (item.checkInPhoto != null && !item.checkInPhoto.isEmpty()) {
                holder.ivCheckInPhoto.setVisibility(View.VISIBLE);
                holder.ivCheckInPhoto.setOnClickListener(v -> openPhoto(item.checkInPhoto));
            } else {
                holder.ivCheckInPhoto.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return filteredList.size(); }

        public void filter(String query) {
            filteredList.clear();
            if (query.trim().isEmpty()) {
                filteredList.addAll(originalList);
            } else {
                String lowerQuery = query.toLowerCase();
                for (EmployeeAttendance item : originalList) {
                    if (item != null && (
                            (item.name != null && item.name.toLowerCase().contains(lowerQuery)) ||
                                    (item.mobile != null && item.mobile.contains(lowerQuery))
                    )) {
                        filteredList.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        private void openGoogleMaps(Double lat, Double lng) {
            String uri = String.format(Locale.getDefault(), "geo:%.6f,%.6f?q=%.6f,%.6f", lat, lng, lat, lng);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            context.startActivity(intent);
        }

        private void openPhoto(String photoUrl) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(photoUrl));
            context.startActivity(intent);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvRole, tvStatus, tvCheckIn, tvCheckOut, tvHours, tvLocation;
            ImageView ivCheckInPhoto;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvRole = itemView.findViewById(R.id.tvRole);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvCheckIn = itemView.findViewById(R.id.tvCheckIn);
                tvCheckOut = itemView.findViewById(R.id.tvCheckOut);
                tvHours = itemView.findViewById(R.id.tvHours);
                tvLocation = itemView.findViewById(R.id.tvLocation);
                ivCheckInPhoto = itemView.findViewById(R.id.ivCheckInPhoto);
            }
        }
    }

    private EmployeeAttendance parseAttendance(DataSnapshot snapshot, EmployeeInfo empInfo) {
        String mobile = snapshot.getKey();
        String status = snapshot.child("status").getValue(String.class);
        String checkInTime = snapshot.child("checkInTime").getValue(String.class);
        String checkOutTime = snapshot.child("checkOutTime").getValue(String.class);
        String totalHours = snapshot.child("totalHours").getValue(String.class);
        Double checkInLat = snapshot.child("checkInLat").getValue(Double.class);
        Double checkInLng = snapshot.child("checkInLng").getValue(Double.class);
        String checkInAddr = snapshot.child("checkInAddress").getValue(String.class);
        Double checkOutLat = snapshot.child("checkOutLat").getValue(Double.class);
        Double checkOutLng = snapshot.child("checkOutLng").getValue(Double.class);
        String checkOutAddr = snapshot.child("checkOutAddress").getValue(String.class);
        String checkInPhoto = snapshot.child("checkInPhoto").getValue(String.class);
        String checkOutPhoto = snapshot.child("checkOutPhoto").getValue(String.class);

        return new EmployeeAttendance(empInfo.name, empInfo.role, mobile, status != null ? status : "Absent",
                checkInTime, checkOutTime, totalHours, checkInLat, checkInLng, checkInAddr,
                checkOutLat, checkOutLng, checkOutAddr, checkInPhoto, checkOutPhoto);
    }
}

