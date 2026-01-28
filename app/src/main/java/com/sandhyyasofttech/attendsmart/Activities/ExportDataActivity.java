package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExportDataActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 200;
    private static final String TAG = "ExportDataActivity";

    private MaterialToolbar toolbar;
    private RadioGroup radioGroupType;
    private MaterialRadioButton rbAttendance, rbLeaves, rbEmployees;
    private TextView tvDateRange, tvSelectedRange;
    private MaterialButton btnSelectDate, btnExport;
    private ProgressBar progressBar;

    private DatabaseReference databaseRef;
    private String companyKey;
    private PrefManager pref;

    private String startDate = "";
    private String endDate = "";
    private String exportType = "attendance";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_data);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        Log.d(TAG, "ExportDataActivity created");
        initializeViews();
        setupToolbar();
        setupFirebase();
        setupListeners();
        checkAndRequestPermissions();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        radioGroupType = findViewById(R.id.radioGroupType);
        rbAttendance = findViewById(R.id.rbAttendance);
        rbLeaves = findViewById(R.id.rbLeaves);
        rbEmployees = findViewById(R.id.rbEmployees);
        tvDateRange = findViewById(R.id.tvDateRange);
        tvSelectedRange = findViewById(R.id.tvSelectedRange);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnExport = findViewById(R.id.btnExport);
        progressBar = findViewById(R.id.progressBar);
    }

    private void showExportSuccessDialog(final String filePath) {
        runOnUiThread(() -> {
            try {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ExportDataActivity.this);

                View dialogView = LayoutInflater.from(ExportDataActivity.this).inflate(R.layout.bottom_sheet_export_success, null);
                bottomSheetDialog.setContentView(dialogView);

                TextView tvFilePath = dialogView.findViewById(R.id.tvFilePath);
                TextView tvFileName = dialogView.findViewById(R.id.tvFileName);
                MaterialButton btnCopyPath = dialogView.findViewById(R.id.btnCopyPath);
                MaterialButton btnShare = dialogView.findViewById(R.id.btnShare);
                MaterialButton btnViewExcel = dialogView.findViewById(R.id.btnViewExcel); // Add this button in your XML
                MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

                String fileName = new File(filePath).getName();
                tvFileName.setText(fileName);
                tvFilePath.setText(filePath);

                btnCopyPath.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("File Path", filePath);
                    clipboard.setPrimaryClip(clip);
                    showSnackbar("Path copied to clipboard");
                });

                btnShare.setOnClickListener(v -> shareFile(filePath));

                // Add View Excel button functionality
                btnViewExcel.setOnClickListener(v -> {
                    openExcelFile(filePath);
                    bottomSheetDialog.dismiss(); // Close dialog after opening
                });

                btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

                bottomSheetDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing success dialog", e);
                showSnackbar("Error showing dialog: " + e.getMessage());
            }
        });
    }

    private void openExcelFile(String filePath) {
        Log.d(TAG, "Opening Excel file: " + filePath);
        File file = new File(filePath);

        if (!file.exists()) {
            showSnackbar("File not found: " + filePath);
            return;
        }

        try {
            Uri fileUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".provider",
                        file
                );
            } else {
                fileUri = Uri.fromFile(file);
            }

            // Create intent to view Excel file
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Check if there's an app to handle Excel files
            PackageManager packageManager = getPackageManager();
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent);
                showSnackbar("Opening Excel file...");
            } else {
                // If no Excel viewer app, try with generic view intent
                intent.setDataAndType(fileUri, "*/*");
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent);
                } else {
                    showSnackbar("No app found to open Excel files. Please install Microsoft Excel, Google Sheets, or a file viewer app.");

                    // Optionally, open Play Store to suggest Excel viewers
                    try {
                        Intent playStoreIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.microsoft.office.excel"));
                        startActivity(playStoreIntent);
                    } catch (Exception e) {
                        // If Play Store not available, open browser
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.microsoft.office.excel"));
                        startActivity(browserIntent);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Excel file", e);
            showSnackbar("Error opening file: " + e.getMessage());
        }
    }
    private void shareFile(String filePath) {
        Log.d(TAG, "Sharing file: " + filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            showSnackbar("File not found: " + filePath);
            return;
        }

        try {
            Uri fileUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".provider",
                        file
                );
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "AttendSmart Export - " + file.getName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Please find the attached Excel export file from AttendSmart.");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Excel File"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing file", e);
            showSnackbar("Error sharing file: " + e.getMessage());
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Export Data");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFirebase() {
        pref = new PrefManager(this);
        companyKey = pref.getUserEmail().replace(".", ",");
        databaseRef = FirebaseDatabase.getInstance().getReference("Companies").child(companyKey);
        Log.d(TAG, "Firebase setup complete. Company key: " + companyKey);
    }

    private void setupListeners() {
        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAttendance) {
                exportType = "attendance";
                tvDateRange.setVisibility(View.VISIBLE);
                btnSelectDate.setVisibility(View.VISIBLE);
                Log.d(TAG, "Export type set to: attendance");
            } else if (checkedId == R.id.rbLeaves) {
                exportType = "leaves";
                tvDateRange.setVisibility(View.VISIBLE);
                btnSelectDate.setVisibility(View.VISIBLE);
                Log.d(TAG, "Export type set to: leaves");
            } else if (checkedId == R.id.rbEmployees) {
                exportType = "employees";
                tvDateRange.setVisibility(View.GONE);
                btnSelectDate.setVisibility(View.GONE);
                tvSelectedRange.setText("All employees will be exported");
                Log.d(TAG, "Export type set to: employees");
            }
        });

        btnSelectDate.setOnClickListener(v -> showDateRangePicker());
        btnExport.setOnClickListener(v -> {
            Log.d(TAG, "Export button clicked. Type: " + exportType);
            if (hasStoragePermission()) {
                exportData();
            } else {
                Log.d(TAG, "Storage permission not granted");
                checkAndRequestPermissions();
            }
        });
    }

    private void showDateRangePicker() {
        Log.d(TAG, "Showing date range picker");
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Date Range")
                        .build();

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            startDate = sdf.format(new Date(selection.first));
            endDate = sdf.format(new Date(selection.second));

            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String displayStart = displayFormat.format(new Date(selection.first));
            String displayEnd = displayFormat.format(new Date(selection.second));

            tvSelectedRange.setText(displayStart + " - " + displayEnd);
            Log.d(TAG, "Date range selected: " + startDate + " to " + endDate);
        });

        dateRangePicker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Storage permission check: " + hasPermission);
            return hasPermission;
        }
        return true;
    }

    private void checkAndRequestPermissions() {
        Log.d(TAG, "Checking and requesting permissions");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasStoragePermission()) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "Permission result received. Request code: " + requestCode);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage permission granted");
                showSnackbar("Storage permission granted");
                exportData();
            } else {
                Log.d(TAG, "Storage permission denied");
                showSnackbar("Storage permission required for exports");
            }
        }
    }

    private void exportData() {
        Log.d(TAG, "Starting export. Type: " + exportType);

        if (exportType.equals("attendance") || exportType.equals("leaves")) {
            if (startDate.isEmpty() || endDate.isEmpty()) {
                Log.d(TAG, "Date range not selected for type: " + exportType);
                showSnackbar("Please select date range");
                return;
            }
        }

        progressBar.setVisibility(View.VISIBLE);
        btnExport.setEnabled(false);
        Log.d(TAG, "Progress bar shown, export button disabled");

        switch (exportType) {
            case "attendance":
                Log.d(TAG, "Exporting attendance data");
                exportAttendanceData();
                break;
            case "leaves":
                Log.d(TAG, "Exporting leaves data");
                exportLeavesData();
                break;
            case "employees":
                Log.d(TAG, "Exporting employees data");
                exportEmployeesData();
                break;
        }
    }

    private void exportAttendanceData() {
        Log.d(TAG, "Fetching attendance data from Firebase");
        databaseRef.child("attendance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot attendanceSnapshot) {
                Log.d(TAG, "Attendance data received. Total dates: " + attendanceSnapshot.getChildrenCount());
                List<AttendanceRecord> records = new ArrayList<>();

                // First get all employee names for mapping mobile numbers to names
                Map<String, String> employeeNames = new HashMap<>();
                databaseRef.child("employees").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot employeesSnapshot) {
                        for (DataSnapshot employee : employeesSnapshot.getChildren()) {
                            String mobile = employee.getKey();
                            DataSnapshot info = employee.child("info");
                            if (info.exists()) {
                                String name = info.child("employeeName").getValue(String.class);
                                if (name != null) {
                                    employeeNames.put(mobile, name);
                                    Log.d(TAG, "Mapped " + mobile + " -> " + name);
                                }
                            }
                        }
                        Log.d(TAG, "Loaded " + employeeNames.size() + " employee names");

                        // Now process attendance data with names
                        for (DataSnapshot dateSnapshot : attendanceSnapshot.getChildren()) {
                            String date = dateSnapshot.getKey();
                            Log.d(TAG, "Processing date: " + date);

                            // Check if this date node has any children (employee data)
                            if (!dateSnapshot.hasChildren()) {
                                Log.d(TAG, "No employee data for date: " + date);
                                continue;
                            }

                            // Check if date is in range
                            if (isDateInRange(date, startDate, endDate)) {
                                Log.d(TAG, "Date " + date + " is in range " + startDate + " to " + endDate);

                                // Iterate through all employees for this date
                                for (DataSnapshot employeeSnapshot : dateSnapshot.getChildren()) {
                                    String employeeMobile = employeeSnapshot.getKey();
                                    Log.d(TAG, "Processing employee attendance: " + employeeMobile + " for date: " + date);

                                    // Get the attendance data for this employee on this date
                                    if (employeeSnapshot.exists()) {
                                        AttendanceRecord record = new AttendanceRecord();

                                        // Get employee name from our map
                                        String employeeName = employeeNames.get(employeeMobile);
                                        record.employeeName = employeeName != null ? employeeName : "Unknown";
                                        record.employeeMobile = employeeMobile;
                                        record.date = date;

                                        // Get attendance details - handle null values
                                        record.checkInTime = employeeSnapshot.child("checkInTime").getValue(String.class);
                                        record.checkOutTime = employeeSnapshot.child("checkOutTime").getValue(String.class);
                                        record.status = employeeSnapshot.child("status").getValue(String.class);
                                        record.finalStatus = employeeSnapshot.child("finalStatus").getValue(String.class);
                                        record.totalHours = employeeSnapshot.child("totalHours").getValue(String.class);
                                        record.lateStatus = employeeSnapshot.child("lateStatus").getValue(String.class);
                                        record.markedBy = employeeSnapshot.child("markedBy").getValue(String.class);

                                        // Debug log all values
                                        Log.d(TAG, "Employee: " + record.employeeName +
                                                " | Date: " + record.date +
                                                " | CheckIn: " + record.checkInTime +
                                                " | CheckOut: " + record.checkOutTime +
                                                " | Status: " + record.status);

                                        // Handle totalHours from totalMinutes if needed
                                        if (record.totalHours == null || record.totalHours.isEmpty()) {
                                            Integer totalMinutes = employeeSnapshot.child("totalMinutes").getValue(Integer.class);
                                            if (totalMinutes != null && totalMinutes > 0) {
                                                int hours = totalMinutes / 60;
                                                int minutes = totalMinutes % 60;
                                                record.totalHours = hours + "h " + minutes + "m";
                                            } else {
                                                record.totalHours = "0h 0m";
                                            }
                                        }

                                        // Set default values for null fields
                                        if (record.status == null) record.status = "";
                                        if (record.finalStatus == null) record.finalStatus = "";
                                        if (record.lateStatus == null) record.lateStatus = "";
                                        if (record.markedBy == null) record.markedBy = "";
                                        if (record.checkInTime == null) record.checkInTime = "";
                                        if (record.checkOutTime == null) record.checkOutTime = "";

                                        records.add(record);
                                        Log.d(TAG, "✓ Added attendance record for " + record.employeeName + " on " + record.date);
                                    } else {
                                        Log.w(TAG, "No attendance data found for employee " + employeeMobile + " on " + date);
                                    }
                                }
                            } else {
                                Log.d(TAG, "Date " + date + " is NOT in range " + startDate + " to " + endDate);
                            }
                        }

                        Log.d(TAG, "Total filtered attendance records: " + records.size());

                        if (records.isEmpty()) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                btnExport.setEnabled(true);
                                showSnackbar("No attendance records found for the selected date range");
                            });
                        } else {
                            createAttendanceExcel(records);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching employee names: " + error.getMessage());
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnExport.setEnabled(true);
                            showSnackbar("Error fetching employee data: " + error.getMessage());
                        });
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching attendance data: " + error.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnExport.setEnabled(true);
                    showSnackbar("Error: " + error.getMessage());
                });
            }
        });
    }    private void exportLeavesData() {
        Log.d(TAG, "Fetching leaves data from Firebase");
        databaseRef.child("leaves").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Leaves data received. Count: " + snapshot.getChildrenCount());
                List<LeaveRecord> records = new ArrayList<>();

                for (DataSnapshot leaveSnapshot : snapshot.getChildren()) {
                    String fromDate = leaveSnapshot.child("fromDate").getValue(String.class);
                    String toDate = leaveSnapshot.child("toDate").getValue(String.class);

                    // Check if fromDate OR toDate is in range
                    if (isDateInRange(fromDate, startDate, endDate) ||
                            (toDate != null && isDateInRange(toDate, startDate, endDate))) {

                        LeaveRecord record = new LeaveRecord();
                        record.employeeName = leaveSnapshot.child("employeeName").getValue(String.class);
                        record.employeeMobile = leaveSnapshot.child("employeeMobile").getValue(String.class);
                        record.leaveType = leaveSnapshot.child("leaveType").getValue(String.class);
                        record.fromDate = fromDate;
                        record.toDate = toDate;
                        record.reason = leaveSnapshot.child("reason").getValue(String.class);
                        record.status = leaveSnapshot.child("status").getValue(String.class);
                        record.appliedAt = leaveSnapshot.child("appliedAt").getValue(Long.class);
                        record.approvedAt = leaveSnapshot.child("approvedAt").getValue(Long.class);
                        record.isPaid = leaveSnapshot.child("isPaid").getValue(Boolean.class);
                        record.halfDayType = leaveSnapshot.child("halfDayType").getValue(String.class);

                        records.add(record);
                        Log.d(TAG, "Added leave record: " + record.employeeName + " - " + record.fromDate + " to " + record.toDate);
                    }
                }

                Log.d(TAG, "Filtered leaves records: " + records.size());
                createLeavesExcel(records);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching leaves data: " + error.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnExport.setEnabled(true);
                    showSnackbar("Error: " + error.getMessage());
                });
            }
        });
    }

    private void exportEmployeesData() {
        Log.d(TAG, "Fetching employees data from Firebase");
        databaseRef.child("employees").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Employees data received. Count: " + snapshot.getChildrenCount());
                List<EmployeeRecord> records = new ArrayList<>();

                for (DataSnapshot empSnapshot : snapshot.getChildren()) {
                    String mobile = empSnapshot.getKey();
                    DataSnapshot infoSnapshot = empSnapshot.child("info");

                    EmployeeRecord record = new EmployeeRecord();
                    record.mobile = mobile;
                    record.name = infoSnapshot.child("employeeName").getValue(String.class);
                    record.email = infoSnapshot.child("employeeEmail").getValue(String.class);
                    record.department = infoSnapshot.child("employeeDepartment").getValue(String.class);
                    record.designation = infoSnapshot.child("employeeRole").getValue(String.class);
                    record.joiningDate = infoSnapshot.child("joinDate").getValue(String.class);
                    record.status = infoSnapshot.child("employeeStatus").getValue(String.class);
                    record.shift = infoSnapshot.child("employeeShift").getValue(String.class);
                    record.weeklyHoliday = infoSnapshot.child("weeklyHoliday").getValue(String.class);
                    record.address = infoSnapshot.child("address").getValue(String.class);
                    record.emergencyContact = infoSnapshot.child("emergencyContact").getValue(String.class);

                    // Get salary information if exists
                    DataSnapshot salaryConfigSnapshot = empSnapshot.child("salaryConfig");
                    if (salaryConfigSnapshot.exists()) {
                        record.monthlySalary = salaryConfigSnapshot.child("monthlySalary").getValue(String.class);
                        record.workingDays = salaryConfigSnapshot.child("workingDays").getValue(String.class);
                        record.paidLeaves = salaryConfigSnapshot.child("paidLeaves").getValue(String.class);
                    }

                    records.add(record);
                    Log.d(TAG, "Added employee record: " + record.name + " - " + record.mobile);
                }

                Log.d(TAG, "Employees records: " + records.size());
                createEmployeesExcel(records);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching employees data: " + error.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnExport.setEnabled(true);
                    showSnackbar("Error: " + error.getMessage());
                });
            }
        });
    }

    private void createAttendanceExcel(List<AttendanceRecord> records) {
        Log.d(TAG, "Creating attendance Excel with " + records.size() + " records");
        new Thread(() -> {
            try {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Attendance");

                // Header - More detailed based on your data structure
                Row headerRow = sheet.createRow(0);
                String[] headers = {
                        "Employee Name", "Employee Mobile", "Date",
                        "Check In Time", "Check Out Time", "Status",
                        "Final Status", "Total Hours", "Late Status",
                        "Marked By"
                };

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }

                // Data
                int rowNum = 1;
                for (AttendanceRecord record : records) {
                    Row row = sheet.createRow(rowNum++);

                    int col = 0;
                    row.createCell(col++).setCellValue(record.employeeName != null ? record.employeeName : "");
                    row.createCell(col++).setCellValue(record.employeeMobile != null ? record.employeeMobile : "");
                    row.createCell(col++).setCellValue(record.date != null ? record.date : "");
                    row.createCell(col++).setCellValue(record.checkInTime != null ? record.checkInTime : "");
                    row.createCell(col++).setCellValue(record.checkOutTime != null ? record.checkOutTime : "");
                    row.createCell(col++).setCellValue(record.status != null ? record.status : "");
                    row.createCell(col++).setCellValue(record.finalStatus != null ? record.finalStatus : "");
                    row.createCell(col++).setCellValue(record.totalHours != null ? record.totalHours : "");
                    row.createCell(col++).setCellValue(record.lateStatus != null ? record.lateStatus : "");
                    row.createCell(col++).setCellValue(record.markedBy != null ? record.markedBy : "");
                }

                // Set column widths
                sheet.setColumnWidth(0, 25 * 256);  // Employee Name
                sheet.setColumnWidth(1, 20 * 256);  // Employee Mobile
                sheet.setColumnWidth(2, 15 * 256);  // Date
                sheet.setColumnWidth(3, 15 * 256);  // Check In Time
                sheet.setColumnWidth(4, 15 * 256);  // Check Out Time
                sheet.setColumnWidth(5, 15 * 256);  // Status
                sheet.setColumnWidth(6, 15 * 256);  // Final Status
                sheet.setColumnWidth(7, 15 * 256);  // Total Hours
                sheet.setColumnWidth(8, 15 * 256);  // Late Status
                sheet.setColumnWidth(9, 15 * 256);  // Marked By

                Log.d(TAG, "Attendance Excel created successfully");
                saveExcelFile(workbook, "Attendance_Report");

            } catch (Exception e) {
                Log.e(TAG, "Error creating attendance Excel", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnExport.setEnabled(true);
                    showSnackbar("Error creating file: " + e.getMessage());
                });
            }
        }).start();
    }

    private void createLeavesExcel(List<LeaveRecord> records) {
        Log.d(TAG, "Creating leaves Excel with " + records.size() + " records");
        new Thread(() -> {
            try {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Leaves");

                // Header
                Row headerRow = sheet.createRow(0);
                String[] headers = {
                        "Employee Name", "Mobile", "Leave Type",
                        "From Date", "To Date", "Half Day Type",
                        "Reason", "Status", "Applied At",
                        "Approved At", "Is Paid"
                };

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }

                // Data
                int rowNum = 1;
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());

                for (LeaveRecord record : records) {
                    Row row = sheet.createRow(rowNum++);

                    int col = 0;
                    row.createCell(col++).setCellValue(record.employeeName != null ? record.employeeName : "");
                    row.createCell(col++).setCellValue(record.employeeMobile != null ? record.employeeMobile : "");
                    row.createCell(col++).setCellValue(record.leaveType != null ? record.leaveType : "");
                    row.createCell(col++).setCellValue(record.fromDate != null ? record.fromDate : "");
                    row.createCell(col++).setCellValue(record.toDate != null ? record.toDate : "");
                    row.createCell(col++).setCellValue(record.halfDayType != null ? record.halfDayType : "");
                    row.createCell(col++).setCellValue(record.reason != null ? record.reason : "");
                    row.createCell(col++).setCellValue(record.status != null ? record.status : "");

                    // Format timestamps
                    if (record.appliedAt != null && record.appliedAt > 0) {
                        row.createCell(col++).setCellValue(dateFormat.format(new Date(record.appliedAt)));
                    } else {
                        row.createCell(col++).setCellValue("");
                    }

                    if (record.approvedAt != null && record.approvedAt > 0) {
                        row.createCell(col++).setCellValue(dateFormat.format(new Date(record.approvedAt)));
                    } else {
                        row.createCell(col++).setCellValue("");
                    }

                    row.createCell(col++).setCellValue(record.isPaid != null ? (record.isPaid ? "Yes" : "No") : "No");
                }

                // Set column widths
                sheet.setColumnWidth(0, 25 * 256);  // Employee Name
                sheet.setColumnWidth(1, 20 * 256);  // Mobile
                sheet.setColumnWidth(2, 15 * 256);  // Leave Type
                sheet.setColumnWidth(3, 15 * 256);  // From Date
                sheet.setColumnWidth(4, 15 * 256);  // To Date
                sheet.setColumnWidth(5, 15 * 256);  // Half Day Type
                sheet.setColumnWidth(6, 30 * 256);  // Reason
                sheet.setColumnWidth(7, 15 * 256);  // Status
                sheet.setColumnWidth(8, 20 * 256);  // Applied At
                sheet.setColumnWidth(9, 20 * 256);  // Approved At
                sheet.setColumnWidth(10, 10 * 256); // Is Paid

                Log.d(TAG, "Leaves Excel created successfully");
                saveExcelFile(workbook, "Leaves_Report");

            } catch (Exception e) {
                Log.e(TAG, "Error creating leaves Excel", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnExport.setEnabled(true);
                    showSnackbar("Error creating file: " + e.getMessage());
                });
            }
        }).start();
    }

    private void createEmployeesExcel(List<EmployeeRecord> records) {
        Log.d(TAG, "Creating employees Excel with " + records.size() + " records");
        new Thread(() -> {
            try {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Employees");

                // Header
                Row headerRow = sheet.createRow(0);
                String[] headers = {
                        "Mobile", "Name", "Email",
                        "Department", "Designation", "Joining Date",
                        "Status", "Shift", "Weekly Holiday",
                        "Address", "Emergency Contact",
                        "Monthly Salary", "Working Days", "Paid Leaves"
                };

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }

                // Data
                int rowNum = 1;
                for (EmployeeRecord record : records) {
                    Row row = sheet.createRow(rowNum++);

                    int col = 0;
                    row.createCell(col++).setCellValue(record.mobile != null ? record.mobile : "");
                    row.createCell(col++).setCellValue(record.name != null ? record.name : "");
                    row.createCell(col++).setCellValue(record.email != null ? record.email : "");
                    row.createCell(col++).setCellValue(record.department != null ? record.department : "");
                    row.createCell(col++).setCellValue(record.designation != null ? record.designation : "");
                    row.createCell(col++).setCellValue(record.joiningDate != null ? record.joiningDate : "");
                    row.createCell(col++).setCellValue(record.status != null ? record.status : "");
                    row.createCell(col++).setCellValue(record.shift != null ? record.shift : "");
                    row.createCell(col++).setCellValue(record.weeklyHoliday != null ? record.weeklyHoliday : "");
                    row.createCell(col++).setCellValue(record.address != null ? record.address : "");
                    row.createCell(col++).setCellValue(record.emergencyContact != null ? record.emergencyContact : "");
                    row.createCell(col++).setCellValue(record.monthlySalary != null ? record.monthlySalary : "");
                    row.createCell(col++).setCellValue(record.workingDays != null ? record.workingDays : "");
                    row.createCell(col++).setCellValue(record.paidLeaves != null ? record.paidLeaves : "");
                }

                // Set column widths
                sheet.setColumnWidth(0, 15 * 256);  // Mobile
                sheet.setColumnWidth(1, 25 * 256);  // Name
                sheet.setColumnWidth(2, 30 * 256);  // Email
                sheet.setColumnWidth(3, 20 * 256);  // Department
                sheet.setColumnWidth(4, 20 * 256);  // Designation
                sheet.setColumnWidth(5, 15 * 256);  // Joining Date
                sheet.setColumnWidth(6, 15 * 256);  // Status
                sheet.setColumnWidth(7, 15 * 256);  // Shift
                sheet.setColumnWidth(8, 15 * 256);  // Weekly Holiday
                sheet.setColumnWidth(9, 30 * 256);  // Address
                sheet.setColumnWidth(10, 20 * 256); // Emergency Contact
                sheet.setColumnWidth(11, 15 * 256); // Monthly Salary
                sheet.setColumnWidth(12, 15 * 256); // Working Days
                sheet.setColumnWidth(13, 15 * 256); // Paid Leaves

                Log.d(TAG, "Employees Excel created successfully");
                saveExcelFile(workbook, "Employees_List");

            } catch (Exception e) {
                Log.e(TAG, "Error creating employees Excel", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnExport.setEnabled(true);
                    showSnackbar("Error creating file: " + e.getMessage());
                });
            }
        }).start();
    }

    private void saveExcelFile(Workbook workbook, String fileName) {
        Log.d(TAG, "Saving Excel file: " + fileName);
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fullFileName = fileName + "_" + timestamp + ".xlsx";

            // Always save to Downloads folder for easier sharing
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AttendSmart");

            Log.d(TAG, "Using Downloads directory: " + directory.getAbsolutePath());
            Log.d(TAG, "Directory exists: " + directory.exists());
            Log.d(TAG, "Directory can write: " + directory.canWrite());

            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                Log.d(TAG, "Directory created: " + created);
                if (!created) {
                    throw new Exception("Failed to create directory: " + directory.getAbsolutePath());
                }
            }

            File file = new File(directory, fullFileName);
            Log.d(TAG, "Creating file at: " + file.getAbsolutePath());

            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.flush();
            fos.close();
            workbook.close();

            Log.d(TAG, "File saved successfully. Size: " + file.length() + " bytes");
            Log.d(TAG, "File exists: " + file.exists());
            Log.d(TAG, "File can read: " + file.canRead());

            final String finalPath = file.getAbsolutePath();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnExport.setEnabled(true);
                showExportSuccessDialog(finalPath);
                showSnackbar("✅ File saved successfully!\nName: " + file.getName() + "\nLocation: " + directory.getAbsolutePath());
            });

        } catch (Exception e) {
            Log.e(TAG, "Error saving Excel file", e);
            e.printStackTrace();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnExport.setEnabled(true);
                showSnackbar("❌ Export failed: " + e.getMessage());
            });
        }
    }

    private boolean isDateInRange(String checkDate, String startDate, String endDate) {
        try {
            if (checkDate == null || checkDate.isEmpty() || startDate == null || endDate == null) {
                return false;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date check = sdf.parse(checkDate);
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            return check != null && start != null && end != null &&
                    (check.equals(start) || check.equals(end) ||
                            (check.after(start) && check.before(end)));
        } catch (Exception e) {
            Log.e(TAG, "Error checking date range for: " + checkDate, e);
            return false;
        }
    }

    private void showSnackbar(String message) {
        runOnUiThread(() -> {
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // Data classes
    private static class AttendanceRecord {
        String employeeName, employeeMobile, date, checkInTime, checkOutTime,
                status, finalStatus, totalHours, lateStatus, markedBy;
    }

    private static class LeaveRecord {
        String employeeName, employeeMobile, leaveType, fromDate, toDate,
                reason, status, halfDayType;
        Long appliedAt, approvedAt;
        Boolean isPaid;
    }

    private static class EmployeeRecord {
        String mobile, name, email, department, designation, joiningDate,
                status, shift, weeklyHoliday, address, emergencyContact,
                monthlySalary, workingDays, paidLeaves;
    }
}