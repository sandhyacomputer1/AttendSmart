package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceReportActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SearchView searchView;
    private TextView tvDate, tvTotalEmployees, tvPresentCount, tvAbsentCount, tvLateCount;
    private ProgressBar progressBar;
    private PrefManager prefManager;
    private String companyKey;
    private DatabaseReference employeesRef, attendanceRef;
    private List<EmployeeAttendance> attendanceList;
    private EmployeeAdapter adapter;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_report);

        initViews();
        setupFirebase();
        selectedDate = getTodayDate();
        tvDate.setText("Date: " + selectedDate);
        loadAttendanceData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        searchView = findViewById(R.id.searchView);
        tvDate = findViewById(R.id.tvDate);
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        progressBar = findViewById(R.id.progressBar);
        prefManager = new PrefManager(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        attendanceList = new ArrayList<>();
        adapter = new EmployeeAdapter(attendanceList, this);
        recyclerView.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }

    private void setupFirebase() {
        companyKey = prefManager.getCompanyKey();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        employeesRef = db.getReference("Companies").child(companyKey).child("employees");
        attendanceRef = db.getReference("Companies").child(companyKey).child("attendance");
    }

    private void loadAttendanceData() {
        progressBar.setVisibility(View.VISIBLE);
        attendanceList.clear();

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
                    employees.put(mobile, new EmployeeInfo(name, role));
                }

                // Load attendance for selected date
                loadAttendanceForDate(employees, selectedDate);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AttendanceReportActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAttendanceForDate(Map<String, EmployeeInfo> employees, String date) {
        attendanceRef.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int present = 0, absent = 0, late = 0;  // ✅ Declare here
                attendanceList.clear();

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

                // Add absent employees ✅ FIXED: Use absent counter
                for (Map.Entry<String, EmployeeInfo> entry : employees.entrySet()) {
                    String mobile = entry.getKey();
                    boolean hasAttendance = snapshot.hasChild(mobile);
                    if (!hasAttendance) {
                        EmployeeAttendance absentEmp = new EmployeeAttendance(
                                entry.getValue().name,
                                entry.getValue().role,
                                mobile,
                                "Absent",
                                null, null, null, null,  // ✅ 8 nulls
                                null, null, null, null   // ✅ 4 more nulls = 15 total
                        );
                        attendanceList.add(absentEmp);
                        absent++;  // ✅ Now correct variable
                    }
                }

                // Sort: Present → Late → Absent
                attendanceList.sort((a, b) -> {
                    if (a.status.equals("Present") && !b.status.equals("Present")) return -1;
                    if (!a.status.equals("Present") && b.status.equals("Present")) return 1;
                    if (a.status.equals("Late") && !b.status.equals("Late")) return -1;
                    return a.name.compareToIgnoreCase(b.name);
                });

                adapter.notifyDataSetChanged();
                updateSummary(employees.size(), present, absent, late);  // ✅ All 4 params
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
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

        return new EmployeeAttendance(
                empInfo.name, empInfo.role, mobile, status,
                checkInTime, checkOutTime, totalHours,
                checkInLat, checkInLng, checkInAddr,
                checkOutLat, checkOutLng
        );
    }

    private void updateSummary(int total, int present, int absent, int late) {
        tvTotalEmployees.setText("Total: " + total);
        tvPresentCount.setText("Present: " + present);
        tvPresentCount.setTextColor(Color.parseColor("#4CAF50"));
        tvAbsentCount.setText("Absent: " + absent);
        tvAbsentCount.setTextColor(Color.parseColor("#F44336"));
        tvLateCount.setText("Late: " + late);
        tvLateCount.setTextColor(Color.parseColor("#FF9800"));
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    // Data Classes
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
                                  Double checkOutLat, Double checkOutLng) {  // ✅ 15 params
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

    // RecyclerView Adapter
    public static class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {
        private List<EmployeeAttendance> originalList, filteredList;
        private AttendanceReportActivity context;

        public EmployeeAdapter(List<EmployeeAttendance> list, AttendanceReportActivity context) {
            this.originalList = list;
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
                    item.status.equals("Late") ? Color.parseColor("#FF9800") : Color.parseColor("#F44336");
            holder.tvStatus.setTextColor(statusColor);
            holder.tvStatus.setBackgroundColor(Color.parseColor(item.status.equals("Present") ? "#E8F5E8" :
                    item.status.equals("Late") ? "#FFF3E0" : "#FFEBEE"));

            holder.tvCheckIn.setText(item.checkInTime != null ? item.checkInTime : "-");
            holder.tvCheckOut.setText(item.checkOutTime != null ? item.checkOutTime : "-");
            holder.tvHours.setText(item.totalHours != null ? item.totalHours + "h" : "-");

            // GPS Location
            if (item.checkInLat != null && item.checkInLng != null) {
                holder.tvLocation.setText(item.checkInAddr != null ?
                        item.checkInAddr.substring(0, 30) + "..." : String.format("%.4f, %.4f", item.checkInLat, item.checkInLng));
                holder.tvLocation.setOnClickListener(v -> openGoogleMaps(item.checkInLat, item.checkInLng));
            }

            // Photos
            if (item.checkInPhoto != null) {
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
            if (query.isEmpty()) {
                filteredList.addAll(originalList);
            } else {
                for (EmployeeAttendance item : originalList) {
                    if (item.name.toLowerCase().contains(query.toLowerCase()) ||
                            item.mobile.contains(query)) {
                        filteredList.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        private void openGoogleMaps(Double lat, Double lng) {
            String uri = String.format("geo:%.6f,%.6f?q=%.6f,%.6f", lat, lng, lat, lng);
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
