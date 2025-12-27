package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceReportActivity extends AppCompatActivity {

    // Calendar Views
    private RecyclerView rvCalendar;
    private TextView tvMonthYear;
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

    // Weekly holidays (Saturday = 7, Sunday = 1)
    private int[] weeklyHolidays = {Calendar.SATURDAY, Calendar.SUNDAY};

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
        calendarAdapter = new CalendarAdapter(currentMonth, allAttendanceDates, this::showDateDetails);
        rvCalendar.setAdapter(calendarAdapter);

        updateMonthDisplay();
    }

    private void setupMonthNavigation() {
        ivPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            calendarAdapter.updateMonth(currentMonth);
        });

        ivNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            calendarAdapter.updateMonth(currentMonth);
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
        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allAttendanceDates.clear();
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    allAttendanceDates.add(dateSnapshot.getKey());
                }
                calendarAdapter.updateAttendanceDates(allAttendanceDates);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AttendanceReportActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonth.getTime()));
    }

    private void showDateDetails(String date) {
        selectedDate = date;
        progressBar.setVisibility(View.VISIBLE);
        llDetailsContainer.removeAllViews();

        // Load employees first
        employeesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, EmployeeInfo> employees = new HashMap<>();
                for (DataSnapshot empSnapshot : snapshot.getChildren()) {
                    String mobile = empSnapshot.getKey();
                    DataSnapshot info = empSnapshot.child("info");
                    String name = info.child("employeeName").getValue(String.class);
                    String role = info.child("employeeRole").getValue(String.class);
                    if (name != null) employees.put(mobile, new EmployeeInfo(name, role));
                }
                loadDateAttendanceDetails(employees, selectedDate);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadDateAttendanceDetails(Map<String, EmployeeInfo> employees, String date) {
        attendanceRef.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int present = 0, absent = 0, late = 0, holidayCount = 0;
                List<EmployeeAttendance> attendanceList = new ArrayList<>();
                boolean isHoliday = isHoliday(date);

                // Add employees with attendance
                for (DataSnapshot empSnapshot : snapshot.getChildren()) {
                    String mobile = empSnapshot.getKey();
                    EmployeeInfo empInfo = employees.get(mobile);
                    if (empInfo != null) {
                        EmployeeAttendance attendance = parseAttendance(empSnapshot, empInfo);
                        attendanceList.add(attendance);
                        if (attendance.status.equals("Present")) present++;
                        else if (attendance.status.equals("Late")) late++;
                    }
                }

                // Add absent/holiday employees
                for (Map.Entry<String, EmployeeInfo> entry : employees.entrySet()) {
                    String mobile = entry.getKey();
                    if (!snapshot.hasChild(mobile)) {
                        String status = isHoliday ? "Holiday" : "Absent";
                        EmployeeAttendance emp = new EmployeeAttendance(
                                entry.getValue().name, entry.getValue().role, mobile, status,
                                null, null, null, null, null, null, null, null, null, null, null
                        );
                        attendanceList.add(emp);
                        if (status.equals("Absent")) absent++;
                        else holidayCount++;
                    }
                }

                // Sort: Present → Late → Holiday → Absent
                attendanceList.sort((a, b) -> {
                    if (a.status.equals("Present") && !b.status.equals("Present")) return -1;
                    if (!a.status.equals("Present") && b.status.equals("Present")) return 1;
                    if (a.status.equals("Late") && !b.status.equals("Late")) return -1;
                    if (a.status.equals("Holiday") && !b.status.equals("Holiday")) return -1;
                    return a.name.compareToIgnoreCase(b.name);
                });

                showSummary(employees.size(), present, absent, late, holidayCount, isHoliday);
                showEmployeeList(attendanceList);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private static boolean isHoliday(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            for (int holiday : FOCUSED_STATE_SET) {
                if (dayOfWeek == holiday) return true;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private void showSummary(int total, int present, int absent, int late, int holiday, boolean isHolidayDay) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View summaryView = inflater.inflate(R.layout.item_date_summary, llDetailsContainer, false);

        TextView tvDate = summaryView.findViewById(R.id.tvDate);
        TextView tvTotal = summaryView.findViewById(R.id.tvTotal);
        TextView tvPresent = summaryView.findViewById(R.id.tvPresent);
        TextView tvAbsent = summaryView.findViewById(R.id.tvAbsent);
        TextView tvLate = summaryView.findViewById(R.id.tvLate);
        TextView tvHoliday = summaryView.findViewById(R.id.tvHoliday);

        tvDate.setText("Date: " + selectedDate);
        tvTotal.setText("Total: " + total);
        tvPresent.setText("Present: " + present);
        tvPresent.setTextColor(Color.parseColor("#4CAF50"));
        tvAbsent.setText("Absent: " + absent);
        tvAbsent.setTextColor(Color.parseColor("#F44336"));
        tvLate.setText("Late: " + late);
        tvLate.setTextColor(Color.parseColor("#FF9800"));

        if (holiday > 0 || isHolidayDay) {
            tvHoliday.setVisibility(View.VISIBLE);
            tvHoliday.setText(holiday > 0 ? "Holiday: " + holiday : "📅 Weekly Holiday");
        }

        llDetailsContainer.addView(summaryView);
    }

    private void showEmployeeList(List<EmployeeAttendance> attendanceList) {
        // SearchView
        SearchView searchView = new SearchView(this);
        searchView.setQueryHint("Search employees...");
        searchView.setIconified(false);

        // RecyclerView
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

    // DATA CLASSES
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

    // CALENDAR ADAPTER
    public static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private Calendar monthCalendar;
        private List<String> attendanceDates;
        private DateClickListener listener;

        public interface DateClickListener {
            void onDateSelected(String date);
        }

        public CalendarAdapter(Calendar monthCalendar, List<String> attendanceDates, DateClickListener listener) {
            this.monthCalendar = (Calendar) monthCalendar.clone();
            this.attendanceDates = new ArrayList<>(attendanceDates);
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int day = getDayOfMonth(position);
            boolean isCurrentMonth = isCurrentMonthDay(position);
            boolean hasAttendance = hasAttendance(day);
            boolean isHolidayDay = isHolidayDay(day);

            holder.tvDay.setText(String.valueOf(Math.max(day, 1)));
            holder.tvDay.setVisibility(isCurrentMonth && day > 0 ? View.VISIBLE : View.INVISIBLE);

            if (isCurrentMonth && day > 0) {
                holder.itemView.setAlpha(1f);
                holder.tvDay.setTextColor(0xFF000000);

                if (hasAttendance) {
                    holder.ivDot.setImageResource(R.drawable.circle_green);
                    holder.ivDot.setVisibility(View.VISIBLE);
                } else if (isHolidayDay) {
                    holder.ivDot.setImageResource(R.drawable.circle_green);
                    holder.ivDot.setVisibility(View.VISIBLE);
                } else {
                    holder.ivDot.setVisibility(View.GONE);
                }
            } else {
                holder.itemView.setAlpha(0.4f);
                holder.tvDay.setTextColor(0xFF666666);
                holder.ivDot.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (isCurrentMonth && day > 0 && listener != null) {
                    String dateStr = getDateString(day);
                    listener.onDateSelected(dateStr);
                }
            });
        }

        @Override
        public int getItemCount() { return 42; }

        private int getDayOfMonth(int position) {
            monthCalendar.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOffset = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1;
            int offset = position - firstDayOffset;
            if (offset < 0) return offset;
            if (offset > monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                return offset - monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            return offset + 1;
        }

        private boolean isCurrentMonthDay(int position) {
            int day = getDayOfMonth(position);
            return day > 0 && day <= monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        }

        private boolean hasAttendance(int day) {
            return attendanceDates.contains(getDateString(day));
        }

        private boolean isHolidayDay(int day) {
            String dateStr = getDateString(day);
            return isHoliday(dateStr);
        }

        private String getDateString(int day) {
            Calendar cal = (Calendar) monthCalendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, day);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(cal.getTime());
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
            ImageView ivDot;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDay = itemView.findViewById(R.id.tvDayNumber);
                ivDot = itemView.findViewById(R.id.ivAttendanceDot);
            }
        }
    }

    // EMPLOYEE ADAPTER
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

            // Status colors
            int statusColor = item.status.equals("Present") ? Color.parseColor("#4CAF50") :
                    item.status.equals("Late") ? Color.parseColor("#FF9800") :
                            item.status.equals("Holiday") ? Color.parseColor("#FF9800") : Color.parseColor("#F44336");
            holder.tvStatus.setTextColor(statusColor);
            holder.tvStatus.setBackgroundColor(Color.parseColor(
                    item.status.equals("Present") ? "#E8F5E8" :
                            item.status.equals("Late") || item.status.equals("Holiday") ? "#FFF3E0" : "#FFEBEE"));

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
