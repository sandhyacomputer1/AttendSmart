//package com.sandhyyasofttech.attendsmart.Activities;
//
//import android.content.Intent;
//import android.graphics.Color;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.Window;
//import android.view.WindowManager;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
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
//public class AttendanceReportActivity extends AppCompatActivity {
//
//    private static final String TAG = "AttendanceReport";
//
//    private RecyclerView rvCalendar;
//    private TextView tvMonthYear, tvPresentCount, tvLateCount, tvHalfDayCount, tvAbsentCount;
//    private ImageView ivPrevMonth, ivNextMonth;
//    private LinearLayout llDetailsContainer;
//    private ProgressBar progressBar;
//
//    private PrefManager prefManager;
//    private String companyKey;
//    private DatabaseReference employeesRef, attendanceRef;
//    private String selectedDate;
//    private Calendar currentMonth;
//    private List<String> allAttendanceDates;
//    private CalendarAdapter calendarAdapter;
//
//    // ✅ Employee-specific weekly holiday (fetched from Firebase)
//    private String employeeWeeklyHoliday = ""; // e.g., "Monday", "Sunday", etc.
//    private int[] weeklyHolidays = {}; // Will be calculated from employeeWeeklyHoliday
//
//    // ✅ 4-FIELD STATS
//    private int monthlyPresentDays = 0;
//    private int monthlyLateDays = 0;
//    private int monthlyHalfDayDays = 0;
//    private int monthlyAbsentDays = 0;
//
//    private boolean isCalculatingStats = false;
//    private String currentEmployeeMobile = null;
//    private String currentJoiningDate = null; // Store joining date
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_attendance_report);
//
//        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
//        }
//
//        // Set status bar color
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Window window = getWindow();
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(getResources().getColor(R.color.blue_800));
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                window.getDecorView().setSystemUiVisibility(
//                        window.getDecorView().getSystemUiVisibility() &
//                                ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//            }
//        }
//
//        toolbar.setNavigationOnClickListener(v -> onBackPressed());
//
//        initViews();
//        setupFirebase();
//        setupCalendar();
//        loadEmployeeDataAndAttendance(); // ✅ Changed method name
//    }
//
//
//    private void initViews() {
//        rvCalendar = findViewById(R.id.rvCalendar);
//        tvMonthYear = findViewById(R.id.tvMonthYear);
//        tvPresentCount = findViewById(R.id.tvPresentCount);
//        tvLateCount = findViewById(R.id.tvLateCount);
//        tvHalfDayCount = findViewById(R.id.tvHalfDayCount);
//        tvAbsentCount = findViewById(R.id.tvAbsentCount);
//        ivPrevMonth = findViewById(R.id.ivPrevMonth);
//        ivNextMonth = findViewById(R.id.ivNextMonth);
//        llDetailsContainer = findViewById(R.id.llDetailsContainer);
//        progressBar = findViewById(R.id.progressBar);
//        prefManager = new PrefManager(this);
//
//        setupMonthNavigation();
//    }
//
//    private void setupCalendar() {
//        currentMonth = Calendar.getInstance();
//        allAttendanceDates = new ArrayList<>();
//
//        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
//        calendarAdapter = new CalendarAdapter(currentMonth, allAttendanceDates, weeklyHolidays, this::showDateDetails);
//        rvCalendar.setAdapter(calendarAdapter);
//
//        updateMonthDisplay();
//    }
//
//    private void setupMonthNavigation() {
//        ivPrevMonth.setOnClickListener(v -> {
//            currentMonth.add(Calendar.MONTH, -1);
//            updateMonthDisplay();
//            calendarAdapter.updateMonth(currentMonth);
//            rvCalendar.postDelayed(this::calculateMonthlyStats, 100);
//        });
//
//        ivNextMonth.setOnClickListener(v -> {
//            currentMonth.add(Calendar.MONTH, 1);
//            updateMonthDisplay();
//            calendarAdapter.updateMonth(currentMonth);
//            rvCalendar.postDelayed(this::calculateMonthlyStats, 100);
//        });
//    }
//
//    private void setupFirebase() {
//        companyKey = prefManager.getCompanyKey();
//        FirebaseDatabase db = FirebaseDatabase.getInstance();
//        employeesRef = db.getReference("Companies").child(companyKey).child("employees");
//        attendanceRef = db.getReference("Companies").child(companyKey).child("attendance");
//    }
//
//    /**
//     * ✅ NEW METHOD: Load employee data first (holiday + joining date), then attendance
//     */
//    private void loadEmployeeDataAndAttendance() {
//        progressBar.setVisibility(View.VISIBLE);
//        allAttendanceDates.clear();
//
//        String employeeEmail = prefManager.getEmployeeEmail();
//        if (employeeEmail == null) {
//            progressBar.setVisibility(View.GONE);
//            return;
//        }
//
//        employeesRef.orderByChild("info/employeeEmail").equalTo(employeeEmail)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot empSnap) {
//                        if (!empSnap.exists()) {
//                            progressBar.setVisibility(View.GONE);
//                            return;
//                        }
//
//                        DataSnapshot employeeData = empSnap.getChildren().iterator().next();
//                        currentEmployeeMobile = employeeData.getKey();
//
//                        // ✅ Fetch weekly holiday from Firebase
//                        DataSnapshot infoNode = employeeData.child("info");
//                        employeeWeeklyHoliday = infoNode.child("weeklyHoliday").getValue(String.class);
//                        currentJoiningDate = infoNode.child("joinDate").getValue(String.class);
//
//                        // ✅ Convert day name to Calendar constant
//                        weeklyHolidays = convertDayNameToCalendarArray(employeeWeeklyHoliday);
//
//                        Log.d(TAG, "Employee Mobile: " + currentEmployeeMobile);
//                        Log.d(TAG, "Weekly Holiday: " + employeeWeeklyHoliday);
//                        Log.d(TAG, "Joining Date: " + currentJoiningDate);
//                        Log.d(TAG, "Holiday Array: " + (weeklyHolidays.length > 0 ? weeklyHolidays[0] : "None"));
//
//                        // ✅ Update calendar adapter with new holidays
//                        calendarAdapter.updateWeeklyHolidays(weeklyHolidays);
//
//                        // ✅ Now load attendance dates
//                        loadAllAttendanceDates();
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        progressBar.setVisibility(View.GONE);
//                        Log.e(TAG, "Error loading employee data: " + error.getMessage());
//                    }
//                });
//    }
//
//    /**
//     * ✅ Convert day name (e.g., "Monday") to Calendar constant (e.g., Calendar.MONDAY)
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
//     * ✅ Load attendance dates for the employee
//     */
//    private void loadAllAttendanceDates() {
//        if (currentEmployeeMobile == null) {
//            progressBar.setVisibility(View.GONE);
//            return;
//        }
//
//        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot dateSnap : snapshot.getChildren()) {
//                    if (dateSnap.hasChild(currentEmployeeMobile)) {
//                        allAttendanceDates.add(dateSnap.getKey());
//                    }
//                }
//                calendarAdapter.updateAttendanceDates(allAttendanceDates);
//                calculateMonthlyStats();
//                progressBar.setVisibility(View.GONE);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                progressBar.setVisibility(View.GONE);
//                Log.e(TAG, "Error loading attendance dates: " + error.getMessage());
//            }
//        });
//    }
//
//    /**
//     * ✅ Calculate monthly stats from joining date
//     */
//    private void calculateMonthlyStats() {
//        if (isCalculatingStats) {
//            Log.d(TAG, "Already calculating...");
//            return;
//        }
//
//        if (currentEmployeeMobile == null) {
//            Log.w(TAG, "Employee mobile not loaded yet");
//            return;
//        }
//
//        isCalculatingStats = true;
//        progressBar.setVisibility(View.VISIBLE);
//        resetMonthlyStats();
//
//        Log.d(TAG, "Calculating for: " +
//                new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.getTime()));
//
//        calendarAdapter.updateFirebaseData(companyKey, currentEmployeeMobile);
//        calculateMonthAttendance(currentEmployeeMobile, currentJoiningDate);
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
//                continue;
//            }
//
//            // ✅ Skip dates before joining
//            if (joiningDate != null && dateStr.compareTo(joiningDate) < 0) {
//                Log.d(TAG, "Skipping before joining: " + dateStr + " (Join: " + joiningDate + ")");
//                continue;
//            }
//
//            // ✅ Skip weekly holidays
//            if (isHoliday(dateStr)) {
//                Log.d(TAG, "Skipping holiday: " + dateStr + " (" + employeeWeeklyHoliday + ")");
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
//        if (snapshot.exists()) {
//
//            String status = snapshot.child("status").getValue(String.class);
//            String lateStatus = snapshot.child("lateStatus").getValue(String.class);
//            String checkInTime = snapshot.child("checkInTime").getValue(String.class);
//
//            boolean hasCheckIn = checkInTime != null && !checkInTime.isEmpty();
//            String statusSafe = status != null ? status.toLowerCase() : "";
//
//            boolean countedPresent = false;
//
//            // ✅ PRESENT (base condition)
//            if (hasCheckIn || statusSafe.contains("present") || statusSafe.contains("full") || statusSafe.contains("half")) {
//                present[0]++;
//                countedPresent = true;
//            }
//
//            // ✅ HALF DAY (independent)
//            if (statusSafe.contains("half")) {
//                halfDay[0]++;
//            }
//
//            // ✅ LATE (independent)
//            if ("late".equalsIgnoreCase(lateStatus) || statusSafe.contains("late")) {
//                late[0]++;
//            }
//
//            // ❌ If no check-in & no status → ABSENT
//            if (!countedPresent) {
//                absent[0]++;
//            }
//
//        } else {
//            // ❌ No record at all
//            absent[0]++;
//        }
//
//        checks[0]--;
//        if (checks[0] == 0) {
//            finishCalculation(present[0], late[0], halfDay[0], absent[0]);
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
//        progressBar.setVisibility(View.GONE);
//        runOnUiThread(this::updateStatsDisplay);
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
//        runOnUiThread(() -> {
//            tvPresentCount.setText(String.valueOf(monthlyPresentDays));
//            tvLateCount.setText(String.valueOf(monthlyLateDays));
//            tvHalfDayCount.setText(String.valueOf(monthlyHalfDayDays));
//            tvAbsentCount.setText(String.valueOf(monthlyAbsentDays));
//
//            Log.d(TAG, "=== MONTHLY STATS ===");
//            Log.d(TAG, "Present: " + monthlyPresentDays + " | Late: " + monthlyLateDays);
//            Log.d(TAG, "Half Day: " + monthlyHalfDayDays + " | Absent: " + monthlyAbsentDays);
//        });
//    }
//
//    private void updateMonthDisplay() {
//        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
//        tvMonthYear.setText(sdf.format(currentMonth.getTime()));
//    }
//
//    private void showDateDetails(String date) {
//        Intent intent = new Intent(this, AttendanceDayDetailsActivity.class);
//        intent.putExtra("date", date);
//        startActivity(intent);
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
//
//    // ✅ EMPLOYEE ATTENDANCE CLASS
//    public static class EmployeeAttendance {
//        String name, role, mobile, status;
//        String checkInTime, checkOutTime, totalHours;
//        Double checkInLat, checkInLng, checkOutLat, checkOutLng;
//        String checkInAddr, checkOutAddr, checkInPhoto, checkOutPhoto;
//
//        public EmployeeAttendance(String name, String role, String mobile, String status,
//                                  String checkInTime, String checkOutTime, String totalHours,
//                                  Double checkInLat, Double checkInLng, String checkInAddr,
//                                  Double checkOutLat, Double checkOutLng, String checkOutAddr,
//                                  String checkInPhoto, String checkOutPhoto) {
//            this.name = name;
//            this.role = role;
//            this.mobile = mobile;
//            this.status = status;
//            this.checkInTime = checkInTime;
//            this.checkOutTime = checkOutTime;
//            this.totalHours = totalHours;
//            this.checkInLat = checkInLat;
//            this.checkInLng = checkInLng;
//            this.checkInAddr = checkInAddr;
//            this.checkOutLat = checkOutLat;
//            this.checkOutLng = checkOutLng;
//            this.checkOutAddr = checkOutAddr;
//            this.checkInPhoto = checkInPhoto;
//            this.checkOutPhoto = checkOutPhoto;
//        }
//    }
//
//    // ✅ CALENDAR ADAPTER (with holiday update support)
//    public static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
//        private Calendar monthCalendar;
//        private List<String> attendanceDates;
//        private int[] weeklyHolidays;
//        private DateClickListener listener;
//        private String today;
//        private String companyKey;
//        private String employeeMobile;
//
//        public interface DateClickListener {
//            void onDateSelected(String date);
//        }
//
//        public CalendarAdapter(Calendar monthCalendar, List<String> attendanceDates,
//                               int[] weeklyHolidays, DateClickListener listener) {
//            this(monthCalendar, attendanceDates, weeklyHolidays, listener, null, null);
//        }
//
//        public CalendarAdapter(Calendar monthCalendar, List<String> attendanceDates,
//                               int[] weeklyHolidays, DateClickListener listener,
//                               String companyKey, String employeeMobile) {
//            this.monthCalendar = (Calendar) monthCalendar.clone();
//            this.attendanceDates = new ArrayList<>(attendanceDates);
//            this.weeklyHolidays = weeklyHolidays != null ? weeklyHolidays : new int[]{};
//            this.listener = listener;
//            this.companyKey = companyKey;
//            this.employeeMobile = employeeMobile;
//
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//            this.today = sdf.format(new Date());
//        }
//
//        @NonNull
//        @Override
//        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.item_calendar_day, parent, false);
//            return new ViewHolder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//            if (position < 7) {
//                String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
//                holder.tvDay.setText(daysOfWeek[position]);
//                holder.tvDay.setTextColor(Color.parseColor("#757575"));
//                holder.tvDay.setTextSize(14);
//                holder.tvDay.setVisibility(View.VISIBLE);
//                holder.containerDay.setBackground(null);
//                holder.itemView.setClickable(false);
//                holder.itemView.setAlpha(1f);
//                holder.itemView.setTag(null);
//                return;
//            }
//
//            int actualPosition = position - 7;
//            int day = getDayOfMonth(actualPosition);
//            boolean isCurrentMonth = isCurrentMonthDay(actualPosition);
//
//            if (isCurrentMonth && day > 0) {
//                String dateStr = getDateString(day);
//                boolean hasAttendance = attendanceDates.contains(dateStr);
//                boolean isHolidayDay = isHolidayDay(day);
//                boolean isTodayDay = dateStr.equals(today);
//                boolean isPastDateDay = isPastDate(dateStr);
//
//                holder.tvDay.setText(String.valueOf(day));
//                holder.tvDay.setVisibility(View.VISIBLE);
//                holder.tvDay.setTextSize(16);
//                holder.itemView.setAlpha(1f);
//
//                // ✅ TODAY GETS PRIORITY - Purple Color
//                if (isTodayDay && hasAttendance && companyKey != null && employeeMobile != null) {
//                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_purple);
//                    holder.tvDay.setTextColor(Color.WHITE);
//                    holder.itemView.setTag("🟣 Today");
//                    checkRealStatus(holder, dateStr);
//                } else if (isTodayDay && !hasAttendance) {
//                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_purple);
//                    holder.tvDay.setTextColor(Color.WHITE);
//                    holder.itemView.setTag("🟣 Today");
//                } else if (hasAttendance && companyKey != null && employeeMobile != null) {
//                    holder.containerDay.setBackgroundResource(android.R.color.darker_gray);
//                    holder.tvDay.setTextColor(Color.WHITE);
//                    holder.itemView.setTag("⏳ Loading...");
//                    checkRealStatus(holder, dateStr);
//                } else if (hasAttendance) {
//                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
//                    holder.tvDay.setTextColor(Color.WHITE);
//                    holder.itemView.setTag("🟢 Present");
//                } else if (isHolidayDay) {
//                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_orange);
//                    holder.tvDay.setTextColor(Color.WHITE);
//                    holder.itemView.setTag("🟠 Holiday");
//                } else if (isPastDateDay && !isHolidayDay) {
//                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
//                    holder.tvDay.setTextColor(Color.WHITE);
//                    holder.itemView.setTag("🔴 Absent");
//                } else {
//                    holder.containerDay.setBackground(null);
//                    holder.tvDay.setTextColor(Color.parseColor("#212121"));
//                    holder.itemView.setTag("⚪ Future");
//                }
//
//                holder.itemView.setOnClickListener(v -> {
//                    String status = (String) holder.itemView.getTag();
//                    Toast.makeText(v.getContext(), status + " (" + dateStr + ")", Toast.LENGTH_SHORT).show();
//                    if (listener != null) {
//                        listener.onDateSelected(dateStr);
//                    }
//                });
//            } else {
//                holder.tvDay.setText("");
//                holder.tvDay.setVisibility(View.INVISIBLE);
//                holder.itemView.setAlpha(0.3f);
//                holder.containerDay.setBackground(null);
//                holder.itemView.setOnClickListener(null);
//                holder.itemView.setTag(null);
//            }
//        }
//
//        @Override
//        public int getItemCount() {
//            return 49;
//        }
//
//        private void checkRealStatus(ViewHolder holder, String dateStr) {
//            DatabaseReference ref = FirebaseDatabase.getInstance()
//                    .getReference("Companies")
//                    .child(companyKey)
//                    .child("attendance")
//                    .child(dateStr)
//                    .child(employeeMobile);
//
//            ref.addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot snapshot) {
//                    holder.tvDay.setTextColor(Color.WHITE);
//
//                    if (!snapshot.exists()) {
//                        holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
//                        holder.itemView.setTag("🔴 Absent");
//                        return;
//                    }
//
//                    String status = snapshot.child("status").getValue(String.class);
//                    String lateStatus = snapshot.child("lateStatus").getValue(String.class);
//                    String statusSafe = status != null ? status.toLowerCase() : "";
//
//                    // ✅ 1. HALF DAY (TOP PRIORITY)
//                    if (statusSafe.contains("half")) {
//                        holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_blue);
//                        holder.itemView.setTag("🔵 Half Day");
//                    }
//                    // ✅ 2. LATE
//                    else if ("late".equalsIgnoreCase(lateStatus) || statusSafe.contains("late")) {
//                        holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_yellow);
//                        holder.itemView.setTag("🟡 Late");
//                    }
//                    // ✅ 3. PRESENT / FULL DAY
//                    else if (statusSafe.contains("present") || statusSafe.contains("full")) {
//                        holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
//                        holder.itemView.setTag("🟢 Present");
//                    }
//                    // ✅ 4. CHECK-IN EXISTS → PRESENT
//                    else {
//                        String checkInTime = snapshot.child("checkInTime").getValue(String.class);
//                        if (checkInTime != null && !checkInTime.isEmpty()) {
//                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
//                            holder.itemView.setTag("🟢 Present");
//                        } else {
//                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
//                            holder.itemView.setTag("🔴 Absent");
//                        }
//                    }
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError error) {
//                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
//                    holder.itemView.setTag("🔴 Error");
//                    holder.tvDay.setTextColor(Color.WHITE);
//                }
//            });
//        }
//
//        private int getDayOfMonth(int position) {
//            monthCalendar.set(Calendar.DAY_OF_MONTH, 1);
//            int firstDayOffset = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1;
//            int offset = position - firstDayOffset;
//
//            if (offset < 0) {
//                Calendar prevMonth = (Calendar) monthCalendar.clone();
//                prevMonth.add(Calendar.MONTH, -1);
//                return prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH) + offset + 1;
//            }
//            if (offset >= monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
//                return offset - monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1;
//            }
//            return offset + 1;
//        }
//
//        private boolean isCurrentMonthDay(int position) {
//            monthCalendar.set(Calendar.DAY_OF_MONTH, 1);
//            int firstDayOffset = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1;
//            int offset = position - firstDayOffset;
//            return offset >= 0 && offset < monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
//        }
//
//        private boolean isHolidayDay(int day) {
//            Calendar cal = (Calendar) monthCalendar.clone();
//            cal.set(Calendar.DAY_OF_MONTH, day);
//            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
//            for (int holiday : weeklyHolidays) {
//                if (dayOfWeek == holiday) return true;
//            }
//            return false;
//        }
//
//        private String getDateString(int day) {
//            Calendar cal = (Calendar) monthCalendar.clone();
//            cal.set(Calendar.DAY_OF_MONTH, day);
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//            return sdf.format(cal.getTime());
//        }
//
//        private boolean isPastDate(String dateStr) {
//            try {
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//                Date date = sdf.parse(dateStr);
//                Date todayDate = sdf.parse(today);
//                return date != null && todayDate != null && date.before(todayDate);
//            } catch (Exception e) {
//                return false;
//            }
//        }
//
//        public void updateAttendanceDates(List<String> dates) {
//            this.attendanceDates.clear();
//            this.attendanceDates.addAll(dates);
//            notifyDataSetChanged();
//        }
//
//        public void updateMonth(Calendar newMonth) {
//            this.monthCalendar = (Calendar) newMonth.clone();
//            notifyDataSetChanged();
//        }
//
//        public void updateFirebaseData(String companyKey, String employeeMobile) {
//            this.companyKey = companyKey;
//            this.employeeMobile = employeeMobile;
//            notifyDataSetChanged();
//        }
//
//        /**
//         * ✅ NEW METHOD: Update weekly holidays dynamically
//         */
//        public void updateWeeklyHolidays(int[] holidays) {
//            this.weeklyHolidays = holidays != null ? holidays : new int[]{};
//            notifyDataSetChanged();
//        }
//
//        static class ViewHolder extends RecyclerView.ViewHolder {
//            TextView tvDay;
//            FrameLayout containerDay;
//
//            ViewHolder(@NonNull View itemView) {
//                super(itemView);
//                tvDay = itemView.findViewById(R.id.tvDayNumber);
//                containerDay = itemView.findViewById(R.id.containerDay);
//            }
//        }
//    }
//}
//
////





package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
import java.util.List;
import java.util.Locale;

public class AttendanceReportActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceReport";

    private RecyclerView rvCalendar;
    private TextView tvMonthYear, tvPresentCount, tvLateCount, tvHalfDayCount, tvAbsentCount;
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

    // ✅ Employee-specific weekly holiday (fetched from Firebase)
    private String employeeWeeklyHoliday = ""; // e.g., "Monday", "Sunday", etc.
    private int[] weeklyHolidays = {}; // Will be calculated from employeeWeeklyHoliday

    // ✅ 4-FIELD STATS
    private int monthlyPresentDays = 0;
    private int monthlyLateDays = 0;
    private int monthlyHalfDayDays = 0;
    private int monthlyAbsentDays = 0;

    private boolean isCalculatingStats = false;
    private String currentEmployeeMobile = null;
    private String currentJoiningDate = null; // Store joining date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_report);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.blue_800));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(
                        window.getDecorView().getSystemUiVisibility() &
                                ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        initViews();
        setupFirebase();
        setupCalendar();
        loadEmployeeDataAndAttendance(); // ✅ Changed method name
    }


    private void initViews() {
        rvCalendar = findViewById(R.id.rvCalendar);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        tvHalfDayCount = findViewById(R.id.tvHalfDayCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
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
            rvCalendar.postDelayed(this::calculateMonthlyStats, 100);
        });

        ivNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            calendarAdapter.updateMonth(currentMonth);
            rvCalendar.postDelayed(this::calculateMonthlyStats, 100);
        });
    }

    private void setupFirebase() {
        companyKey = prefManager.getCompanyKey();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        employeesRef = db.getReference("Companies").child(companyKey).child("employees");
        attendanceRef = db.getReference("Companies").child(companyKey).child("attendance");
    }

    /**
     * ✅ NEW METHOD: Load employee data first (holiday + joining date), then attendance
     */
    private void loadEmployeeDataAndAttendance() {
        progressBar.setVisibility(View.VISIBLE);
        allAttendanceDates.clear();

        String employeeEmail = prefManager.getEmployeeEmail();
        if (employeeEmail == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        employeesRef.orderByChild("info/employeeEmail").equalTo(employeeEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot empSnap) {
                        if (!empSnap.exists()) {
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        DataSnapshot employeeData = empSnap.getChildren().iterator().next();
                        currentEmployeeMobile = employeeData.getKey();

                        // ✅ Fetch weekly holiday from Firebase
                        DataSnapshot infoNode = employeeData.child("info");
                        employeeWeeklyHoliday = infoNode.child("weeklyHoliday").getValue(String.class);
                        currentJoiningDate = infoNode.child("joinDate").getValue(String.class);

                        // ✅ Convert day name to Calendar constant
                        weeklyHolidays = convertDayNameToCalendarArray(employeeWeeklyHoliday);

                        Log.d(TAG, "Employee Mobile: " + currentEmployeeMobile);
                        Log.d(TAG, "Weekly Holiday: " + employeeWeeklyHoliday);
                        Log.d(TAG, "Joining Date: " + currentJoiningDate);
                        Log.d(TAG, "Holiday Array: " + (weeklyHolidays.length > 0 ? weeklyHolidays[0] : "None"));

                        // ✅ Update calendar adapter with new holidays
                        calendarAdapter.updateWeeklyHolidays(weeklyHolidays);

                        // ✅ Now load attendance dates
                        loadAllAttendanceDates();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error loading employee data: " + error.getMessage());
                    }
                });
    }

    /**
     * ✅ Convert day name (e.g., "Monday") to Calendar constant (e.g., Calendar.MONDAY)
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
     * ✅ Load attendance dates for the employee
     */
    private void loadAllAttendanceDates() {
        if (currentEmployeeMobile == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                    if (dateSnap.hasChild(currentEmployeeMobile)) {
                        allAttendanceDates.add(dateSnap.getKey());
                    }
                }
                calendarAdapter.updateAttendanceDates(allAttendanceDates);
                calculateMonthlyStats();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading attendance dates: " + error.getMessage());
            }
        });
    }

    /**
     * ✅ Calculate monthly stats from joining date
     */
    private void calculateMonthlyStats() {
        if (isCalculatingStats) {
            Log.d(TAG, "Already calculating...");
            return;
        }

        if (currentEmployeeMobile == null) {
            Log.w(TAG, "Employee mobile not loaded yet");
            return;
        }

        isCalculatingStats = true;
        progressBar.setVisibility(View.VISIBLE);
        resetMonthlyStats();

        Log.d(TAG, "Calculating for: " +
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.getTime()));

        calendarAdapter.updateFirebaseData(companyKey, currentEmployeeMobile);
        calculateMonthAttendance(currentEmployeeMobile, currentJoiningDate);
    }

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
                continue;
            }

            // ✅ Skip dates before joining
            if (joiningDate != null && dateStr.compareTo(joiningDate) < 0) {
                Log.d(TAG, "Skipping before joining: " + dateStr + " (Join: " + joiningDate + ")");
                continue;
            }

            // ✅ Skip weekly holidays
            if (isHoliday(dateStr)) {
                Log.d(TAG, "Skipping holiday: " + dateStr + " (" + employeeWeeklyHoliday + ")");
                continue;
            }

            validDates.add(dateStr);
            checks[0]++;
        }

        Log.d(TAG, "Total valid working dates: " + checks[0]);

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
                    if (checks[0] == 0)
                        finishCalculation(present[0], late[0], halfDay[0], absent[0]);
                }
            });
        }
    }


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
                // Don't increment any counters for holidays
                checks[0]--;
                if (checks[0] == 0) {
                    finishCalculation(present[0], late[0], halfDay[0], absent[0]);
                }
                return; // Exit early for holidays
            }

            boolean countedPresent = false;

            // ✅ PRESENT (base condition)
            if (hasCheckIn || statusSafe.contains("present") || statusSafe.contains("full") || statusSafe.contains("half")) {
                present[0]++;
                countedPresent = true;
            }

            // ✅ HALF DAY (independent)
            if (statusSafe.contains("half")) {
                halfDay[0]++;
            }

            // ✅ LATE (independent)
            if ("late".equalsIgnoreCase(lateStatus) || statusSafe.contains("late")) {
                late[0]++;
            }

            // ❌ If no check-in & no status → ABSENT
            if (!countedPresent) {
                absent[0]++;
            }

        } else {
            // ❌ No record at all
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

        isCalculatingStats = false;
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
        runOnUiThread(() -> {
            tvPresentCount.setText(String.valueOf(monthlyPresentDays));
            tvLateCount.setText(String.valueOf(monthlyLateDays));
            tvHalfDayCount.setText(String.valueOf(monthlyHalfDayDays));
            tvAbsentCount.setText(String.valueOf(monthlyAbsentDays));

            Log.d(TAG, "=== MONTHLY STATS ===");
            Log.d(TAG, "Present: " + monthlyPresentDays + " | Late: " + monthlyLateDays);
            Log.d(TAG, "Half Day: " + monthlyHalfDayDays + " | Absent: " + monthlyAbsentDays);
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

    // ✅ EMPLOYEE ATTENDANCE CLASS
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

    // ✅ CALENDAR ADAPTER (with holiday update support)
    public static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private Calendar monthCalendar;
        private List<String> attendanceDates;
        private int[] weeklyHolidays;
        private DateClickListener listener;
        private String today;
        private String companyKey;
        private String employeeMobile;

        public interface DateClickListener {
            void onDateSelected(String date);
        }

        public CalendarAdapter(Calendar monthCalendar, List<String> attendanceDates,
                               int[] weeklyHolidays, DateClickListener listener) {
            this(monthCalendar, attendanceDates, weeklyHolidays, listener, null, null);
        }

        public CalendarAdapter(Calendar monthCalendar, List<String> attendanceDates,
                               int[] weeklyHolidays, DateClickListener listener,
                               String companyKey, String employeeMobile) {
            this.monthCalendar = (Calendar) monthCalendar.clone();
            this.attendanceDates = new ArrayList<>(attendanceDates);
            this.weeklyHolidays = weeklyHolidays != null ? weeklyHolidays : new int[]{};
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
            if (position < 7) {
                String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
                holder.tvDay.setText(daysOfWeek[position]);
                holder.tvDay.setTextColor(Color.parseColor("#757575"));
                holder.tvDay.setTextSize(14);
                holder.tvDay.setVisibility(View.VISIBLE);
                holder.containerDay.setBackground(null);
                holder.itemView.setClickable(false);
                holder.itemView.setAlpha(1f);
                holder.itemView.setTag(null);
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

                // ✅ TODAY GETS PRIORITY - Purple Color
                if (isTodayDay && hasAttendance && companyKey != null && employeeMobile != null) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_purple);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("🟣 Today");
                    checkRealStatus(holder, dateStr);
                } else if (isTodayDay && !hasAttendance) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_purple);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("🟣 Today");
                } else if (hasAttendance && companyKey != null && employeeMobile != null) {
                    holder.containerDay.setBackgroundResource(android.R.color.darker_gray);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("⏳ Loading...");
                    checkRealStatus(holder, dateStr);
                } else if (hasAttendance) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("🟢 Present");
                } else if (isHolidayDay) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_orange);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("🟠 Weekly Holiday");
                } else if (isPastDateDay && !isHolidayDay) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("🔴 Absent");
                } else {
                    holder.containerDay.setBackground(null);
                    holder.tvDay.setTextColor(Color.parseColor("#212121"));
                    holder.itemView.setTag("⚪ Future");
                }

                holder.itemView.setOnClickListener(v -> {
                    String status = (String) holder.itemView.getTag();
                    Toast.makeText(v.getContext(), status + " (" + dateStr + ")", Toast.LENGTH_SHORT).show();
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
                holder.itemView.setTag(null);
            }
        }
        @Override
        public int getItemCount() {
            return 49;
        }

        private void checkRealStatus(ViewHolder holder, String dateStr) {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("Companies")
                    .child(companyKey)
                    .child("attendance")
                    .child(dateStr)
                    .child(employeeMobile);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    holder.tvDay.setTextColor(Color.WHITE);

                    if (!snapshot.exists()) {
                        holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
                        holder.itemView.setTag("🔴 Absent");
                        return;
                    }

                    String status = snapshot.child("status").getValue(String.class);
                    String lateStatus = snapshot.child("lateStatus").getValue(String.class);
                    String finalStatus = snapshot.child("finalStatus").getValue(String.class); // ADD THIS
                    String statusSafe = status != null ? status.toLowerCase() : "";
                    String finalStatusSafe = finalStatus != null ? finalStatus.toLowerCase() : "";

                    // ✅ 1. HOLIDAY (HIGHEST PRIORITY) - PURPLE
                    if (statusSafe.contains("holiday") || finalStatusSafe.contains("holiday")) {
                        holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_purplee);
                        holder.itemView.setTag("🟣 Holiday");
                    }
                    // ✅ 2. HALF DAY
                    else if (statusSafe.contains("half")) {
                        holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_blue);
                        holder.itemView.setTag("🔵 Half Day");
                    }
                    // ✅ 3. LATE
                    else if ("late".equalsIgnoreCase(lateStatus) || statusSafe.contains("late")) {
                        holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_yellow);
                        holder.itemView.setTag("🟡 Late");
                    }
                    // ✅ 4. PRESENT / FULL DAY
                    else if (statusSafe.contains("present") || statusSafe.contains("full")) {
                        holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                        holder.itemView.setTag("🟢 Present");
                    }
                    // ✅ 5. CHECK-IN EXISTS → PRESENT
                    else {
                        String checkInTime = snapshot.child("checkInTime").getValue(String.class);
                        if (checkInTime != null && !checkInTime.isEmpty()) {
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                            holder.itemView.setTag("🟢 Present");
                        } else {
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
                            holder.itemView.setTag("🔴 Absent");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
                    holder.itemView.setTag("🔴 Error");
                    holder.tvDay.setTextColor(Color.WHITE);
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

        /**
         * ✅ NEW METHOD: Update weekly holidays dynamically
         */
        public void updateWeeklyHolidays(int[] holidays) {
            this.weeklyHolidays = holidays != null ? holidays : new int[]{};
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

