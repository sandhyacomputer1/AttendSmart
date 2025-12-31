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

    private int[] weeklyHolidays = {Calendar.SUNDAY};

    // ✅ 4-FIELD STATS
    private int monthlyPresentDays = 0;
    private int monthlyLateDays = 0;
    private int monthlyHalfDayDays = 0;
    private int monthlyAbsentDays = 0;

    private boolean isCalculatingStats = false;
    private String currentEmployeeMobile = null;

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
                                        allAttendanceDates.add(dateSnap.getKey());
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

        employeesRef.orderByChild("info/employeeEmail").equalTo(employeeEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            DataSnapshot empData = snapshot.getChildren().iterator().next();
                            currentEmployeeMobile = empData.getKey();
                            calendarAdapter.updateFirebaseData(companyKey, currentEmployeeMobile);
                            String joiningDate = empData.child("info/joiningDate").getValue(String.class);

                            Log.d(TAG, "Employee mobile: " + currentEmployeeMobile);
                            calculateMonthAttendance(currentEmployeeMobile, joiningDate);
                        } else {
                            finishCalculation(0, 0, 0, 0);
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        finishCalculation(0, 0, 0, 0);
                    }
                });
    }

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
            Long totalMinutesRaw = snapshot.child("totalMinutes").getValue(Long.class);
            String checkInTime = snapshot.child("checkInTime").getValue(String.class);

            long totalMinutes = (totalMinutesRaw != null) ? totalMinutesRaw : 0;
            boolean hasCheckIn = checkInTime != null && !checkInTime.isEmpty();

            Log.d(TAG, dateStr + " | Status: " + status + " | Late: " + lateStatus + " | Mins: " + totalMinutes);

            // ✅ HALF DAY (3hr+): Present:1 | Half Day:1
            if (status != null && status.equalsIgnoreCase("Half Day") && totalMinutes >= 180) {
                present[0]++;
                halfDay[0]++;
            }
            // ✅ LATE + FULL DAY (8hr+): Present:1 | Late:1
            else if (lateStatus != null && lateStatus.equalsIgnoreCase("Late") && totalMinutes >= 480) {
                present[0]++;
                late[0]++;
            }
            // ✅ LATE + Any check-in: Present:1 | Late:1
            else if (lateStatus != null && lateStatus.equalsIgnoreCase("Late") && hasCheckIn) {
                present[0]++;
                late[0]++;
            }
            // ✅ Normal Present/Full Day: Present:1
            else if (status != null && (status.equalsIgnoreCase("Present") || status.equalsIgnoreCase("Full Day"))
                    || (hasCheckIn && totalMinutes >= 480)) {
                present[0]++;
            }
            // ✅ Any other check-in = Present
            else if (hasCheckIn) {
                present[0]++;
            }

        } else {
            // ✅ NO CHECK-IN: Absent:1
            absent[0]++;
            Log.d(TAG, dateStr + " → Absent (no record)");
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

    // ✅ INNER CLASSES (UNCHANGED)
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
            this.weeklyHolidays = weeklyHolidays;
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

                // ✅ INTEGRATED checkRealStatus WITH LOADING STATE
                if (hasAttendance && companyKey != null && employeeMobile != null) {
                    // Show loading state first
                    holder.containerDay.setBackgroundResource(android.R.color.darker_gray);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("⏳ Loading...");

                    checkRealStatus(holder, dateStr); // Async Firebase call
                } else if (hasAttendance) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("🟢 Present");
                } else if (isHolidayDay) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_orange);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("🟠 Holiday");
                } else if (isPastDateDay && !isHolidayDay) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_red);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("🔴 Absent");
                } else if (isTodayDay) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_blue);
                    holder.tvDay.setTextColor(Color.WHITE);
                    holder.itemView.setTag("🔵 Today");
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

        // ✅ INTEGRATED checkRealStatus METHOD
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
                        Long totalMinutesRaw = snapshot.child("totalMinutes").getValue(Long.class);
                        long totalMinutes = (totalMinutesRaw != null) ? totalMinutesRaw : 0;

                        holder.tvDay.setTextColor(Color.WHITE);

                        // ✅ MATCHES STATS HEADER COLORS + LOGIC
                        if (status != null && status.equalsIgnoreCase("Half Day") && totalMinutes >= 180) {
                            // HALF DAY = BLUE (like stats header)
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_blue);
                            holder.itemView.setTag("🔵 Half Day");
                        }
                        else if (lateStatus != null && lateStatus.equalsIgnoreCase("Late")) {
                            // LATE = YELLOW (like stats header)
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_yellow);
                            holder.itemView.setTag("🟡 Late");
                        }
                        else if (status != null && (status.equalsIgnoreCase("Present") ||
                                status.equalsIgnoreCase("Full Day"))) {
                            // PRESENT = GREEN (like stats header)
                            holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                            holder.itemView.setTag("🟢 Present");
                        }
                        else {
                            String checkInTime = snapshot.child("checkInTime").getValue(String.class);
                            if (checkInTime != null && !checkInTime.isEmpty()) {
                                // ANY CHECK-IN = GREEN
                                holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                                holder.itemView.setTag("🟢 Present");
                            } else {
                                holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_yellow);
                                holder.itemView.setTag("🟡 Unknown");
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.containerDay.setBackgroundResource(R.drawable.calendar_bg_green);
                    holder.itemView.setTag("🟢 Error");
                    holder.tvDay.setTextColor(Color.WHITE);
                }
            });
        }

        // ✅ ALL HELPER METHODS (unchanged)
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
}
