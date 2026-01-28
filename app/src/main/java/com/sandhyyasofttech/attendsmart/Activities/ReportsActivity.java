package com.sandhyyasofttech.attendsmart.Activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.database.*;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportsActivity extends AppCompatActivity {

    private TextView tvTotalEmployees, tvPresentCount, tvAbsentCount, tvLateCount, tvHalfDayCount;
    private Button btnFromDate, btnToDate, btnGenerateReport, btnViewPdf, btnSharePdf;
    private DatabaseReference databaseRef;
    private String companyKey;
    private Calendar fromDate, toDate;
    private Map<String, EmployeeData> employeeDataMap = new HashMap<>();
    private CompanyInfo companyInfo;
    private List<AttendanceRecord> attendanceRecords = new ArrayList<>();
    private File pdfFile;
    private ProgressDialog progressDialog;

    // PDF Constants
    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final int LEFT_MARGIN = 30;
    private static final int RIGHT_MARGIN = 30;
    private static final int TOP_MARGIN = 40;
    private static final int BOTTOM_MARGIN = 50;

    // Report type selection
    private RadioGroup rgReportType;
    private RadioButton rbDetailedReport, rbMonthlySummary;
    private LinearLayout layoutDetailedSummary;
    private TextView tvSummaryTitle, tvMonthlyInfo;
    private int selectedReportType = 0; // 0 = Detailed, 1 = Monthly Summary

    // Column positions for detailed report
    private int colDateX, colNameX, colPhoneX, colStatusX, colCheckInX, colCheckOutX, colMarkedByX;

    // Paint objects
    private Paint headerBgPaint, headerTextPaint, titlePaint, textPaint, linePaint, altRowPaint, footerPaint;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MAX_REPORT_DAYS = 31;
    private LinearLayout layoutMonthlySummary;
    private TextView tvTotalEmployeesSummary, tvWorkingDays, tvTotalPresentDays, tvTotalAbsentDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        initViews();
        initPaintObjects();
        setupToolbar();
        setupFirebase();
        loadTotalEmployees();
        setupClickListeners();
        checkPermissions();
    }

    private void initPaintObjects() {
        // Header Background Paint - Using orange color #FF5722
        headerBgPaint = new Paint();
        headerBgPaint.setColor(Color.parseColor("#FF5722")); // Orange color

        // Header Text Paint
        headerTextPaint = new Paint();
        headerTextPaint.setColor(Color.WHITE);
        headerTextPaint.setTextSize(10);
        headerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Title Paint
        titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(14);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Regular Text Paint
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(9);

        // Line Paint
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E0E0E0"));
        linePaint.setStrokeWidth(1f);

        // Alternate Row Paint
        altRowPaint = new Paint();
        altRowPaint.setColor(Color.parseColor("#F8F8F8"));

        // Footer Paint
        footerPaint = new Paint();
        footerPaint.setColor(Color.GRAY);
        footerPaint.setTextSize(8);
    }

    private void initViews() {
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        tvHalfDayCount = findViewById(R.id.tvHalfDayCount);

        btnFromDate = findViewById(R.id.btnFromDate);
        btnToDate = findViewById(R.id.btnToDate);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnViewPdf = findViewById(R.id.btnViewPdf);
        btnSharePdf = findViewById(R.id.btnSharePdf);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);

        // Initialize dates to current month
        fromDate = Calendar.getInstance();
        fromDate.set(Calendar.DAY_OF_MONTH, 1);
        toDate = Calendar.getInstance();

        // Initialize report type views
        rgReportType = findViewById(R.id.rgReportType);
        rbDetailedReport = findViewById(R.id.rbDetailedReport);
        rbMonthlySummary = findViewById(R.id.rbMonthlySummary);
        layoutDetailedSummary = findViewById(R.id.layoutDetailedSummary);
        tvSummaryTitle = findViewById(R.id.tvSummaryTitle);
        tvMonthlyInfo = findViewById(R.id.tvMonthlyInfo);

        // Set up report type listener
        rgReportType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDetailedReport) {
                selectedReportType = 0;
                layoutDetailedSummary.setVisibility(View.VISIBLE);
                tvMonthlyInfo.setVisibility(View.GONE);
                tvSummaryTitle.setText("Summary");
            } else if (checkedId == R.id.rbMonthlySummary) {
                selectedReportType = 1;
                layoutDetailedSummary.setVisibility(View.GONE);
                tvMonthlyInfo.setVisibility(View.VISIBLE);
                tvSummaryTitle.setText("Monthly Summary");
            }
        });


        // Initialize monthly summary views
        layoutMonthlySummary = findViewById(R.id.layoutMonthlySummary);
        tvTotalEmployeesSummary = findViewById(R.id.tvTotalEmployeesSummary);
        tvWorkingDays = findViewById(R.id.tvWorkingDays);
        tvTotalPresentDays = findViewById(R.id.tvTotalPresentDays);
        tvTotalAbsentDays = findViewById(R.id.tvTotalAbsentDays);

        // Set up report type listener
        rgReportType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDetailedReport) {
                selectedReportType = 0;
                layoutDetailedSummary.setVisibility(View.VISIBLE);
                layoutMonthlySummary.setVisibility(View.GONE);
                tvSummaryTitle.setText("Daily Summary");
            } else if (checkedId == R.id.rbMonthlySummary) {
                selectedReportType = 1;
                layoutDetailedSummary.setVisibility(View.GONE);
                layoutMonthlySummary.setVisibility(View.VISIBLE);
                tvSummaryTitle.setText("Monthly Summary");
            }
        });

        updateDateButtons();

        // Calculate column positions for detailed report
        calculateColumnPositions();
    }

    private void calculateColumnPositions() {
        // Adjust column widths for 7 columns (without address)
        colDateX = LEFT_MARGIN;
        colNameX = colDateX + 90; // Wider for name
        colPhoneX = colNameX + 180; // Much wider for full name
        colStatusX = colPhoneX + 100;
        colCheckInX = colStatusX + 90;
        colCheckOutX = colCheckInX + 100;
        colMarkedByX = colCheckOutX + 100;
        // No address column
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 🔥 MAKE BACK ARROW WHITE
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }


    private void setupFirebase() {
        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        databaseRef = FirebaseDatabase.getInstance().getReference("Companies").child(companyKey);
        loadCompanyInfo();
    }

    private void loadCompanyInfo() {
        databaseRef.child("companyInfo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    companyInfo = new CompanyInfo();
                    companyInfo.companyName = snapshot.child("companyName").getValue(String.class);
                    companyInfo.companyEmail = snapshot.child("companyEmail").getValue(String.class);
                    companyInfo.companyPhone = snapshot.child("companyPhone").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ReportsActivity", "Error loading company info: " + error.getMessage());
            }
        });
    }

    private void loadTotalEmployees() {
        progressDialog.show();
        databaseRef.child("employees").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                employeeDataMap.clear();
                int total = 0;

                for (DataSnapshot empSnapshot : snapshot.getChildren()) {
                    String phone = empSnapshot.getKey();
                    DataSnapshot infoSnapshot = empSnapshot.child("info");

                    if (infoSnapshot.exists()) {
                        String name = infoSnapshot.child("employeeName").getValue(String.class);
                        String email = infoSnapshot.child("employeeEmail").getValue(String.class);
                        String department = infoSnapshot.child("employeeDepartment").getValue(String.class);

                        if (name != null && !name.trim().isEmpty()) {
                            EmployeeData emp = new EmployeeData();
                            emp.phone = phone;
                            emp.name = name.trim();
                            emp.email = email;
                            emp.department = department;
                            employeeDataMap.put(phone, emp);
                            total++;
                        }
                    }
                }

                tvTotalEmployees.setText("Total Employees: " + total);
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(ReportsActivity.this, "Error loading employees", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        btnFromDate.setOnClickListener(v -> showDatePicker(true));
        btnToDate.setOnClickListener(v -> showDatePicker(false));
        btnGenerateReport.setOnClickListener(v -> generateReport());
        btnViewPdf.setOnClickListener(v -> viewPdf());
        btnSharePdf.setOnClickListener(v -> sharePdf());
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = isFromDate ? fromDate : toDate;
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    if (isFromDate) {
                        fromDate.set(year, month, dayOfMonth);
                    } else {
                        toDate.set(year, month, dayOfMonth);
                    }
                    updateDateButtons();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateButtons() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        btnFromDate.setText(sdf.format(fromDate.getTime()));
        btnToDate.setText(sdf.format(toDate.getTime()));
    }

    private void generateReport() {
        // Validate date range
        if (fromDate.after(toDate)) {
            Toast.makeText(this, "From date cannot be after To date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (employeeDataMap.isEmpty()) {
            Toast.makeText(this, "No employees found", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Generating report...");
        progressDialog.show();
        attendanceRecords.clear();

        // Show warning for large date ranges
        long diffInMillis = toDate.getTimeInMillis() - fromDate.getTimeInMillis();
        long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

        if (diffInDays > 90) { // Warning for ranges > 3 months
            runOnUiThread(() -> {
                Toast.makeText(this, "Generating report for " + diffInDays + " days. This may take a moment...",
                        Toast.LENGTH_LONG).show();
            });
        }

        // Run in background thread
        new Thread(() -> {
            try {
                fetchAttendanceData();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ReportsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
//    private void fetchAttendanceData() {
//        databaseRef.child("attendance").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                // Create wrapper object to hold counts
//                final CountWrapper counts = new CountWrapper();
//
//                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//
//                // Iterate through each date in the range
//                Calendar currentDate = (Calendar) fromDate.clone();
//                while (!currentDate.after(toDate)) {
//                    String dateKey = dateFormat.format(currentDate.getTime());
//
//                    if (snapshot.hasChild(dateKey)) {
//                        DataSnapshot dateSnapshot = snapshot.child(dateKey);
//
//                        for (DataSnapshot empSnapshot : dateSnapshot.getChildren()) {
//                            String phone = empSnapshot.getKey();
//
//                            if (!employeeDataMap.containsKey(phone)) continue;
//
//                            AttendanceRecord record = new AttendanceRecord();
//                            record.date = dateKey;
//                            record.phone = phone;
//
//                            // Get employee name from map
//                            EmployeeData empData = employeeDataMap.get(phone);
//                            record.employeeName = empData != null ? empData.name : "Unknown";
//
//                            // Get status
//                            String finalStatus = empSnapshot.child("finalStatus").getValue(String.class);
//                            String adminStatus = empSnapshot.child("adminStatus").getValue(String.class);
//                            String status = empSnapshot.child("status").getValue(String.class);
//
//                            record.status = finalStatus != null ? finalStatus :
//                                    (adminStatus != null ? adminStatus : status);
//
//                            record.lateStatus = empSnapshot.child("lateStatus").getValue(String.class);
//                            record.markedBy = empSnapshot.child("markedBy").getValue(String.class);
//                            record.checkInTime = empSnapshot.child("checkInTime").getValue(String.class);
//                            record.checkOutTime = empSnapshot.child("checkOutTime").getValue(String.class);
//                            record.checkInAddress = empSnapshot.child("checkInAddress").getValue(String.class);
//
//                            attendanceRecords.add(record);
//
//                            // Count status types
//                            if (record.status != null) {
//                                if (record.status.equalsIgnoreCase("Present")) {
//                                    counts.presentCount++;
//                                    if ("Late".equalsIgnoreCase(record.lateStatus)) {
//                                        counts.lateCount++;
//                                    }
//                                } else if (record.status.equalsIgnoreCase("Absent")) {
//                                    counts.absentCount++;
//                                } else if (record.status.equalsIgnoreCase("Half Day") ||
//                                        record.status.equalsIgnoreCase("Half-Day")) {
//                                    counts.halfDayCount++;
//                                }
//                            }
//                        }
//                    }
//                    currentDate.add(Calendar.DAY_OF_MONTH, 1);
//                }
//
//                // Sort records
//                Collections.sort(attendanceRecords, (a, b) -> {
//                    int dateCompare = a.date.compareTo(b.date);
//                    if (dateCompare != 0) return dateCompare;
//                    return (a.employeeName != null ? a.employeeName : "").compareTo(b.employeeName != null ? b.employeeName : "");
//                });
//
//                // Update UI on main thread
//                runOnUiThread(() -> {
//                    tvPresentCount.setText(String.valueOf(counts.presentCount));
//                    tvAbsentCount.setText(String.valueOf(counts.absentCount));
//                    tvLateCount.setText(String.valueOf(counts.lateCount));
//                    tvHalfDayCount.setText(String.valueOf(counts.halfDayCount));
//
//                    // Generate PDF based on selected type
//                    new Thread(() -> {
//                        if (selectedReportType == 0) {
//                            createDetailedPdf();
//                        } else {
//                            createMonthlySummaryPdf();
//                        }
//                    }).start();
//                });
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                runOnUiThread(() -> {
//                    progressDialog.dismiss();
//                    Toast.makeText(ReportsActivity.this, "Error loading attendance data", Toast.LENGTH_SHORT).show();
//                });
//            }
//        });
//    }

    private void fetchAttendanceData() {
        databaseRef.child("attendance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Create wrapper object to hold counts
                final CountWrapper counts = new CountWrapper();
                final MonthlyCountWrapper monthlyCounts = new MonthlyCountWrapper();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                // Iterate through each date in the range
                Calendar currentDate = (Calendar) fromDate.clone();
                while (!currentDate.after(toDate)) {
                    String dateKey = dateFormat.format(currentDate.getTime());

                    if (snapshot.hasChild(dateKey)) {
                        DataSnapshot dateSnapshot = snapshot.child(dateKey);

                        for (DataSnapshot empSnapshot : dateSnapshot.getChildren()) {
                            String phone = empSnapshot.getKey();

                            if (!employeeDataMap.containsKey(phone)) continue;

                            AttendanceRecord record = new AttendanceRecord();
                            record.date = dateKey;
                            record.phone = phone;

                            // Get employee name from map
                            EmployeeData empData = employeeDataMap.get(phone);
                            record.employeeName = empData != null ? empData.name : "Unknown";

                            // Get status
                            String finalStatus = empSnapshot.child("finalStatus").getValue(String.class);
                            String adminStatus = empSnapshot.child("adminStatus").getValue(String.class);
                            String status = empSnapshot.child("status").getValue(String.class);

                            record.status = finalStatus != null ? finalStatus :
                                    (adminStatus != null ? adminStatus : status);

                            record.lateStatus = empSnapshot.child("lateStatus").getValue(String.class);
                            record.markedBy = empSnapshot.child("markedBy").getValue(String.class);
                            record.checkInTime = empSnapshot.child("checkInTime").getValue(String.class);
                            record.checkOutTime = empSnapshot.child("checkOutTime").getValue(String.class);
                            record.checkInAddress = empSnapshot.child("checkInAddress").getValue(String.class);

                            attendanceRecords.add(record);

                            // Count status types for detailed report
                            if (record.status != null) {
                                if (record.status.equalsIgnoreCase("Present")) {
                                    counts.presentCount++;
                                    if ("Late".equalsIgnoreCase(record.lateStatus)) {
                                        counts.lateCount++;
                                    }
                                } else if (record.status.equalsIgnoreCase("Absent")) {
                                    counts.absentCount++;
                                } else if (record.status.equalsIgnoreCase("Half Day") ||
                                        record.status.equalsIgnoreCase("Half-Day")) {
                                    counts.halfDayCount++;
                                }
                            }
                        }
                    }
                    currentDate.add(Calendar.DAY_OF_MONTH, 1);
                }

                // Sort records
                Collections.sort(attendanceRecords, (a, b) -> {
                    int dateCompare = a.date.compareTo(b.date);
                    if (dateCompare != 0) return dateCompare;
                    return (a.employeeName != null ? a.employeeName : "").compareTo(b.employeeName != null ? b.employeeName : "");
                });

                // Calculate monthly summary stats
                Map<String, EmployeeMonthlySummary> monthlySummary = calculateMonthlySummary();
                monthlyCounts.totalEmployees = employeeDataMap.size();
                monthlyCounts.workingDays = calculateWorkingDays(fromDate, toDate);

                for (EmployeeMonthlySummary summary : monthlySummary.values()) {
                    monthlyCounts.totalPresentDays += summary.presentDays;
                    monthlyCounts.totalAbsentDays += summary.absentDays;
                }

                // Update UI on main thread
                runOnUiThread(() -> {
                    // Update detailed report summary
                    tvPresentCount.setText(String.valueOf(counts.presentCount));
                    tvAbsentCount.setText(String.valueOf(counts.absentCount));
                    tvLateCount.setText(String.valueOf(counts.lateCount));
                    tvHalfDayCount.setText(String.valueOf(counts.halfDayCount));

                    // Update monthly report summary
                    tvTotalEmployeesSummary.setText(String.valueOf(monthlyCounts.totalEmployees));
                    tvWorkingDays.setText(String.valueOf(monthlyCounts.workingDays));
                    tvTotalPresentDays.setText(String.valueOf(monthlyCounts.totalPresentDays));
                    tvTotalAbsentDays.setText(String.valueOf(monthlyCounts.totalAbsentDays));

                    // Generate PDF based on selected type
                    new Thread(() -> {
                        if (selectedReportType == 0) {
                            createDetailedPdf();
                        } else {
                            createMonthlySummaryPdf();
                        }
                    }).start();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ReportsActivity.this, "Error loading attendance data", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ==============================
    // DETAILED PDF GENERATION
    // ==============================

    private void createDetailedPdf() {
        try {
            PdfDocument pdfDocument = new PdfDocument();

            if (attendanceRecords.isEmpty()) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ReportsActivity.this, "No attendance records found for selected period", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            int pageNumber = 1;
            int startRecord = 0;

            while (startRecord < attendanceRecords.size()) {
                // Create a new page
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                int yPosition = TOP_MARGIN;

                // Draw header for first page only
                if (pageNumber == 1) {
                    yPosition = drawDetailedHeader(canvas, yPosition);
                    yPosition = drawReportInfo(canvas, yPosition);
                    yPosition = drawSummarySection(canvas, yPosition);
                } else {
                    // Draw continuation header for other pages
                    yPosition = drawPageContinuationHeader(canvas, yPosition, pageNumber);
                }

                // Draw table header
                yPosition = drawDetailedTableHeader(canvas, yPosition);

                // Draw table rows (max 30 rows per page)
                int rowsDrawn = drawDetailedTableRows(canvas, yPosition, startRecord);
                startRecord += rowsDrawn;

                // Draw footer
                drawFooter(canvas, pageNumber, "Detailed Attendance Report");

                pdfDocument.finishPage(page);
                pageNumber++;
            }

            // Save the PDF document
            savePdfDocument(pdfDocument, "Detailed_Attendance_");

        } catch (Exception e) {
            Log.e("ReportsActivity", "Detailed PDF Generation Error: " + e.getMessage(), e);
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(ReportsActivity.this, "Failed to create detailed PDF", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private int drawDetailedHeader(Canvas canvas, int yPosition) {
        // Draw company header background with orange color
        canvas.drawRect(0, 0, PAGE_WIDTH, 70, headerBgPaint);

        // Company name
        String companyName = (companyInfo != null && companyInfo.companyName != null) ?
                companyInfo.companyName : "AttendSmart";
        Paint companyPaint = new Paint(headerTextPaint);
        companyPaint.setTextSize(22);
        companyPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        float companyWidth = companyPaint.measureText(companyName);
        float companyX = (PAGE_WIDTH - companyWidth) / 2;
        canvas.drawText(companyName, companyX, 35, companyPaint);

        // Report title
        Paint titlePaint = new Paint(headerTextPaint);
        titlePaint.setTextSize(16);
        String reportTitle = "DETAILED ATTENDANCE REPORT";
        float titleWidth = titlePaint.measureText(reportTitle);
        float titleX = (PAGE_WIDTH - titleWidth) / 2;
        canvas.drawText(reportTitle, titleX, 55, titlePaint);

        return 90;
    }

    private int drawPageContinuationHeader(Canvas canvas, int yPosition, int pageNumber) {
        canvas.drawLine(LEFT_MARGIN, yPosition - 10, PAGE_WIDTH - RIGHT_MARGIN, yPosition - 10, linePaint);

        Paint contPaint = new Paint(textPaint);
        contPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        contPaint.setTextSize(10);

        String continuationText = "Attendance Report - Continued (Page " + pageNumber + ")";
        canvas.drawText(continuationText, LEFT_MARGIN, yPosition, contPaint);

        return yPosition + 20;
    }

    private int drawReportInfo(Canvas canvas, int yPosition) {
        // Draw report period
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String periodText = "Report Period: " + displayFormat.format(fromDate.getTime()) +
                " to " + displayFormat.format(toDate.getTime());
        canvas.drawText(periodText, LEFT_MARGIN, yPosition, textPaint);

        // Draw generation date
        String genDate = "Generated: " + new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText(genDate, PAGE_WIDTH - 200, yPosition, textPaint);

        // Draw company details if available
        if (companyInfo != null) {
            yPosition += 15;
            if (companyInfo.companyEmail != null) {
                canvas.drawText("Email: " + companyInfo.companyEmail, LEFT_MARGIN, yPosition, textPaint);
            }
            if (companyInfo.companyPhone != null) {
                canvas.drawText("Phone: " + companyInfo.companyPhone, PAGE_WIDTH - 200, yPosition, textPaint);
            }
        }

        return yPosition + 25;
    }

    private int drawSummarySection(Canvas canvas, int yPosition) {
        // Draw summary title
        canvas.drawText("SUMMARY", LEFT_MARGIN, yPosition, titlePaint);
        yPosition += 5;

        // Draw separator line
        canvas.drawLine(LEFT_MARGIN, yPosition, PAGE_WIDTH - RIGHT_MARGIN, yPosition, linePaint);
        yPosition += 15;

        // Draw summary stats in a grid layout
        int startX = LEFT_MARGIN;
        int columnWidth = 180;

        // Row 1
        canvas.drawText("Total Employees: " + employeeDataMap.size(), startX, yPosition, textPaint);
        canvas.drawText("Present: " + tvPresentCount.getText(), startX + columnWidth, yPosition, textPaint);
        canvas.drawText("Absent: " + tvAbsentCount.getText(), startX + columnWidth * 2, yPosition, textPaint);
        yPosition += 15;

        // Row 2
        canvas.drawText("Late: " + tvLateCount.getText(), startX, yPosition, textPaint);
        canvas.drawText("Half Day: " + tvHalfDayCount.getText(), startX + columnWidth, yPosition, textPaint);
        canvas.drawText("Records: " + attendanceRecords.size(), startX + columnWidth * 2, yPosition, textPaint);

        return yPosition + 25;
    }

    private int drawDetailedTableHeader(Canvas canvas, int yPosition) {
        // Draw table header background with orange color
        canvas.drawRect(LEFT_MARGIN, yPosition, PAGE_WIDTH - RIGHT_MARGIN, yPosition + 25, headerBgPaint);

        // Draw column headers with vertical lines
        int headerY = yPosition + 17;

        // Date column
        canvas.drawText("Date", colDateX + 5, headerY, headerTextPaint);
        canvas.drawLine(colNameX, yPosition, colNameX, yPosition + 25, headerTextPaint);

        // Name column - Wider for full name
        canvas.drawText("Employee Name", colNameX + 5, headerY, headerTextPaint);
        canvas.drawLine(colPhoneX, yPosition, colPhoneX, yPosition + 25, headerTextPaint);

        // Phone column
        canvas.drawText("Phone", colPhoneX + 5, headerY, headerTextPaint);
        canvas.drawLine(colStatusX, yPosition, colStatusX, yPosition + 25, headerTextPaint);

        // Status column
        canvas.drawText("Status", colStatusX + 5, headerY, headerTextPaint);
        canvas.drawLine(colCheckInX, yPosition, colCheckInX, yPosition + 25, headerTextPaint);

        // Check-In column
        canvas.drawText("Check-In", colCheckInX + 5, headerY, headerTextPaint);
        canvas.drawLine(colCheckOutX, yPosition, colCheckOutX, yPosition + 25, headerTextPaint);

        // Check-Out column
        canvas.drawText("Check-Out", colCheckOutX + 5, headerY, headerTextPaint);
        canvas.drawLine(colMarkedByX, yPosition, colMarkedByX, yPosition + 25, headerTextPaint);

        // Marked By column (last column)
        canvas.drawText("Marked By", colMarkedByX + 5, headerY, headerTextPaint);

        return yPosition + 30;
    }

    private int drawDetailedTableRows(Canvas canvas, int yPosition, int startRecord) {
        int maxRowsPerPage = 30;
        int rowHeight = 20;
        int rowsDrawn = 0;

        for (int i = startRecord; i < attendanceRecords.size() && rowsDrawn < maxRowsPerPage; i++) {
            if (yPosition + rowHeight > PAGE_HEIGHT - BOTTOM_MARGIN) {
                break;
            }

            AttendanceRecord record = attendanceRecords.get(i);
            int rowY = yPosition + (rowsDrawn * rowHeight);

            // Draw alternate row background
            if (rowsDrawn % 2 == 0) {
                canvas.drawRect(LEFT_MARGIN, rowY, PAGE_WIDTH - RIGHT_MARGIN, rowY + rowHeight, altRowPaint);
            }

            // Draw vertical lines for all columns
            canvas.drawLine(colNameX, rowY, colNameX, rowY + rowHeight, linePaint);
            canvas.drawLine(colPhoneX, rowY, colPhoneX, rowY + rowHeight, linePaint);
            canvas.drawLine(colStatusX, rowY, colStatusX, rowY + rowHeight, linePaint);
            canvas.drawLine(colCheckInX, rowY, colCheckInX, rowY + rowHeight, linePaint);
            canvas.drawLine(colCheckOutX, rowY, colCheckOutX, rowY + rowHeight, linePaint);
            canvas.drawLine(colMarkedByX, rowY, colMarkedByX, rowY + rowHeight, linePaint);

            // Draw row content
            int textY = rowY + 14;

            // Date - formatted nicely
            String displayDate = formatDate(record.date);
            canvas.drawText(displayDate, colDateX + 5, textY, textPaint);

            // Employee Name - display full name
            String name = record.employeeName != null ? record.employeeName : "Unknown";
            if (name.length() > 30) {
                name = name.substring(0, 27) + "...";
            }
            canvas.drawText(name, colNameX + 5, textY, textPaint);

            // Phone
            String phone = record.phone != null ? record.phone : "-";
            canvas.drawText(phone, colPhoneX + 5, textY, textPaint);

            // Status with color coding
            Paint statusPaint = new Paint(textPaint);
            statusPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            if ("Present".equalsIgnoreCase(record.status)) {
                statusPaint.setColor(Color.parseColor("#388E3C")); // Dark green
            } else if ("Absent".equalsIgnoreCase(record.status)) {
                statusPaint.setColor(Color.parseColor("#D32F2F")); // Dark red
            } else if (record.status != null && (record.status.toLowerCase().contains("half") ||
                    record.status.equalsIgnoreCase("Half Day"))) {
                statusPaint.setColor(Color.parseColor("#1976D2")); // Blue
            } else if ("Late".equalsIgnoreCase(record.status)) {
                statusPaint.setColor(Color.parseColor("#FF5722")); // Orange (matching your company color)
            } else {
                statusPaint.setColor(Color.BLACK);
            }

            String status = record.status != null ? record.status : "-";
            canvas.drawText(status, colStatusX + 5, textY, statusPaint);

            // Check-In Time
            String checkIn = record.checkInTime != null ? record.checkInTime : "-";
            canvas.drawText(checkIn, colCheckInX + 5, textY, textPaint);

            // Check-Out Time
            String checkOut = record.checkOutTime != null ? record.checkOutTime : "-";
            canvas.drawText(checkOut, colCheckOutX + 5, textY, textPaint);

            // Marked By (last column)
            String markedBy = record.markedBy != null ? record.markedBy : "-";
            if (markedBy.length() > 15) {
                markedBy = markedBy.substring(0, 12) + "...";
            }
            canvas.drawText(markedBy, colMarkedByX + 5, textY, textPaint);

            rowsDrawn++;
        }

        // Draw horizontal line at bottom of table
        int tableBottomY = yPosition + (rowsDrawn * rowHeight);
        canvas.drawLine(LEFT_MARGIN, tableBottomY, PAGE_WIDTH - RIGHT_MARGIN, tableBottomY, linePaint);

        return rowsDrawn;
    }

    // ==============================
    // MONTHLY SUMMARY PDF GENERATION
    // ==============================

    // ==============================
// MONTHLY SUMMARY PDF GENERATION - FIXED VERSION
// ==============================

    private void createMonthlySummaryPdf() {
        try {
            PdfDocument pdfDocument = new PdfDocument();

            if (attendanceRecords.isEmpty()) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "No attendance records found for selected period", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // Calculate monthly summary data
            Map<String, EmployeeMonthlySummary> monthlySummary = calculateMonthlySummary();

            int pageNumber = 1;
            int startRecord = 0;
            List<String> employeeNames = new ArrayList<>(monthlySummary.keySet());
            Collections.sort(employeeNames);

            while (startRecord < employeeNames.size()) {
                // Create a new page
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                int yPosition = TOP_MARGIN;

                // Draw header for first page only
                if (pageNumber == 1) {
                    yPosition = drawMonthlySummaryHeader(canvas, yPosition);
                    yPosition = drawMonthlyReportInfo(canvas, yPosition);
                    yPosition = drawMonthlySummaryStats(canvas, yPosition, monthlySummary);
                } else {
                    // Draw continuation header for other pages
                    yPosition = drawMonthlyContinuationHeader(canvas, yPosition, pageNumber);
                }

                // Draw table header
                yPosition = drawMonthlyTableHeader(canvas, yPosition);

                // Draw table rows
                int rowsDrawn = drawMonthlyTableRows(canvas, yPosition, startRecord, employeeNames, monthlySummary);
                startRecord += rowsDrawn;

                // Draw footer
                drawMonthlyFooter(canvas, pageNumber);

                pdfDocument.finishPage(page);
                pageNumber++;
            }

            // Save the PDF document
            savePdfDocument(pdfDocument, "Monthly_Summary_");

        } catch (Exception e) {
            Log.e("ReportsActivity", "Monthly PDF Generation Error: " + e.getMessage(), e);
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to create monthly summary PDF", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private Map<String, EmployeeMonthlySummary> calculateMonthlySummary() {
        Map<String, EmployeeMonthlySummary> summaryMap = new HashMap<>();

        // Get month name from date range
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());

        // Check if date range spans multiple months
        Calendar tempCal = (Calendar) fromDate.clone();
        List<String> monthsInRange = new ArrayList<>();

        while (!tempCal.after(toDate)) {
            monthsInRange.add(monthFormat.format(tempCal.getTime()));
            tempCal.add(Calendar.MONTH, 1);
            tempCal.set(Calendar.DAY_OF_MONTH, 1);
        }

        String monthText;
        if (monthsInRange.size() == 1) {
            monthText = monthsInRange.get(0);
        } else {
            monthText = monthFormat.format(fromDate.getTime()) + " to " + monthFormat.format(toDate.getTime());
        }

        // Calculate total working days
        int totalWorkingDays = calculateWorkingDays(fromDate, toDate);

        for (Map.Entry<String, EmployeeData> entry : employeeDataMap.entrySet()) {
            EmployeeMonthlySummary summary = new EmployeeMonthlySummary();
            summary.employeeName = entry.getValue().name;
            summary.phone = entry.getValue().phone;
            summary.department = entry.getValue().department;
            summary.month = monthText;
            summary.totalWorkingDays = totalWorkingDays;

            // Initialize counters
            summary.presentDays = 0;
            summary.absentDays = 0;
            summary.lateDays = 0;
            summary.halfDays = 0;

            summaryMap.put(entry.getValue().name, summary);
        }

        // Count attendance for each employee
        for (AttendanceRecord record : attendanceRecords) {
            String employeeName = record.employeeName;
            if (summaryMap.containsKey(employeeName)) {
                EmployeeMonthlySummary summary = summaryMap.get(employeeName);

                if (record.status != null) {
                    if (record.status.equalsIgnoreCase("Present")) {
                        summary.presentDays++;
                        if ("Late".equalsIgnoreCase(record.lateStatus)) {
                            summary.lateDays++;
                        }
                    } else if (record.status.equalsIgnoreCase("Absent")) {
                        summary.absentDays++;
                    } else if (record.status.equalsIgnoreCase("Half Day") ||
                            record.status.equalsIgnoreCase("Half-Day")) {
                        summary.halfDays++;
                    }
                }
            }
        }

        return summaryMap;
    }
    private int calculateWorkingDays(Calendar start, Calendar end) {
        int workingDays = 0;
        Calendar current = (Calendar) start.clone();

        while (!current.after(end)) {
            int dayOfWeek = current.get(Calendar.DAY_OF_WEEK);
            // Count only weekdays (Monday to Friday)
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                workingDays++;
            }
            current.add(Calendar.DAY_OF_MONTH, 1);
        }

        return workingDays;
    }
    private int drawMonthlySummaryHeader(Canvas canvas, int yPosition) {
        // Draw company header background with orange color
        canvas.drawRect(0, 0, PAGE_WIDTH, 70, headerBgPaint);

        // Company name
        String companyName = (companyInfo != null && companyInfo.companyName != null) ?
                companyInfo.companyName : "AttendSmart";
        Paint companyPaint = new Paint(headerTextPaint);
        companyPaint.setTextSize(22);
        companyPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        float companyWidth = companyPaint.measureText(companyName);
        float companyX = (PAGE_WIDTH - companyWidth) / 2;
        canvas.drawText(companyName, companyX, 35, companyPaint);

        // Report title
        Paint titlePaint = new Paint(headerTextPaint);
        titlePaint.setTextSize(16);
        String reportTitle = "EMPLOYEE ATTENDANCE SUMMARY";
        float titleWidth = titlePaint.measureText(reportTitle);
        float titleX = (PAGE_WIDTH - titleWidth) / 2;
        canvas.drawText(reportTitle, titleX, 55, titlePaint);

        return 90;
    }

    private int drawMonthlyReportInfo(Canvas canvas, int yPosition) {
        // Draw report period
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String periodText = "Report Period: " + displayFormat.format(fromDate.getTime()) +
                " to " + displayFormat.format(toDate.getTime());
        canvas.drawText(periodText, LEFT_MARGIN, yPosition, textPaint);

        // Draw generation date
        String genDate = "Generated: " + new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText(genDate, PAGE_WIDTH - 200, yPosition, textPaint);

        // Draw company details if available
        if (companyInfo != null) {
            yPosition += 15;
            if (companyInfo.companyEmail != null) {
                canvas.drawText("Email: " + companyInfo.companyEmail, LEFT_MARGIN, yPosition, textPaint);
            }
            if (companyInfo.companyPhone != null) {
                canvas.drawText("Phone: " + companyInfo.companyPhone, PAGE_WIDTH - 200, yPosition, textPaint);
            }
        }

        return yPosition + 25;
    }

    private int drawMonthlySummaryStats(Canvas canvas, int yPosition, Map<String, EmployeeMonthlySummary> summaryMap) {
        // Draw summary title
        canvas.drawText("SUMMARY STATISTICS", LEFT_MARGIN, yPosition, titlePaint);
        yPosition += 5;

        // Draw separator line
        canvas.drawLine(LEFT_MARGIN, yPosition, PAGE_WIDTH - RIGHT_MARGIN, yPosition, linePaint);
        yPosition += 15;

        // Calculate total statistics
        int totalPresent = 0;
        int totalAbsent = 0;
        int totalLate = 0;
        int totalHalfDay = 0;

        for (EmployeeMonthlySummary summary : summaryMap.values()) {
            totalPresent += summary.presentDays;
            totalAbsent += summary.absentDays;
            totalLate += summary.lateDays;
            totalHalfDay += summary.halfDays;
        }

        // Draw summary stats in a grid layout
        int startX = LEFT_MARGIN;
        int columnWidth = 180;

        // Row 1
        canvas.drawText("Total Employees: " + summaryMap.size(), startX, yPosition, textPaint);
        canvas.drawText("Total Present Days: " + totalPresent, startX + columnWidth, yPosition, textPaint);
        canvas.drawText("Total Absent Days: " + totalAbsent, startX + columnWidth * 2, yPosition, textPaint);
        yPosition += 15;

        // Row 2
        canvas.drawText("Total Late Days: " + totalLate, startX, yPosition, textPaint);
        canvas.drawText("Total Half Days: " + totalHalfDay, startX + columnWidth, yPosition, textPaint);
        canvas.drawText("Working Days: " + (summaryMap.size() > 0 ? summaryMap.values().iterator().next().totalWorkingDays : "0"),
                startX + columnWidth * 2, yPosition, textPaint);

        return yPosition + 25;
    }

    private int drawMonthlyContinuationHeader(Canvas canvas, int yPosition, int pageNumber) {
        canvas.drawLine(LEFT_MARGIN, yPosition - 10, PAGE_WIDTH - RIGHT_MARGIN, yPosition - 10, linePaint);

        Paint contPaint = new Paint(textPaint);
        contPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        contPaint.setTextSize(10);

        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String continuationText = "Employee Summary " + displayFormat.format(fromDate.getTime()) +
                " to " + displayFormat.format(toDate.getTime()) +
                " - Page " + pageNumber;
        canvas.drawText(continuationText, LEFT_MARGIN, yPosition, contPaint);

        return yPosition + 20;
    }

    private int drawMonthlyTableHeader(Canvas canvas, int yPosition) {
        // Draw table header background with orange color
        int headerHeight = 25;
        canvas.drawRect(LEFT_MARGIN, yPosition, PAGE_WIDTH - RIGHT_MARGIN, yPosition + headerHeight, headerBgPaint);

        // Draw column headers
        int headerY = yPosition + 17;

        // Set column positions for monthly summary - MORE SPACE FOR NAME
        int colNameX = LEFT_MARGIN;
        int colPhoneX = colNameX + 220;  // Increased from 180 to 220
        int colDeptX = colPhoneX + 100;
        int colPresentX = colDeptX + 80;  // Reduced from 90 to 80
        int colAbsentX = colPresentX + 65; // Reduced from 70 to 65
        int colLateX = colAbsentX + 55;   // Reduced from 60 to 55
        int colHalfDayX = colLateX + 65;  // Reduced from 70 to 65
        int colTotalX = colHalfDayX + 70; // Reduced from 80 to 70

        // Draw vertical lines
        Paint whiteLinePaint = new Paint();
        whiteLinePaint.setColor(Color.WHITE);
        whiteLinePaint.setStrokeWidth(1f);

        canvas.drawLine(colPhoneX, yPosition, colPhoneX, yPosition + headerHeight, whiteLinePaint);
        canvas.drawLine(colDeptX, yPosition, colDeptX, yPosition + headerHeight, whiteLinePaint);
        canvas.drawLine(colPresentX, yPosition, colPresentX, yPosition + headerHeight, whiteLinePaint);
        canvas.drawLine(colAbsentX, yPosition, colAbsentX, yPosition + headerHeight, whiteLinePaint);
        canvas.drawLine(colLateX, yPosition, colLateX, yPosition + headerHeight, whiteLinePaint);
        canvas.drawLine(colHalfDayX, yPosition, colHalfDayX, yPosition + headerHeight, whiteLinePaint);
        canvas.drawLine(colTotalX, yPosition, colTotalX, yPosition + headerHeight, whiteLinePaint);

        // Draw header text
        canvas.drawText("Employee Name", colNameX + 5, headerY, headerTextPaint);
        canvas.drawText("Phone", colPhoneX + 5, headerY, headerTextPaint);
        canvas.drawText("Department", colDeptX + 5, headerY, headerTextPaint);
        canvas.drawText("Present", colPresentX + 5, headerY, headerTextPaint);
        canvas.drawText("Absent", colAbsentX + 5, headerY, headerTextPaint);
        canvas.drawText("Late", colLateX + 5, headerY, headerTextPaint);
        canvas.drawText("Half Day", colHalfDayX + 5, headerY, headerTextPaint);
        canvas.drawText("Total Days", colTotalX + 5, headerY, headerTextPaint);

        return yPosition + 30;
    }

    private int drawMonthlyTableRows(Canvas canvas, int yPosition, int startRecord,
                                     List<String> employeeNames, Map<String, EmployeeMonthlySummary> summaryMap) {
        int maxRowsPerPage = 35;
        int rowHeight = 18;
        int rowsDrawn = 0;

        // Define column positions (same as header)
        int colNameX = LEFT_MARGIN;
        int colPhoneX = colNameX + 220;
        int colDeptX = colPhoneX + 100;
        int colPresentX = colDeptX + 80;
        int colAbsentX = colPresentX + 65;
        int colLateX = colAbsentX + 55;
        int colHalfDayX = colLateX + 65;
        int colTotalX = colHalfDayX + 70;

        for (int i = startRecord; i < employeeNames.size() && rowsDrawn < maxRowsPerPage; i++) {
            if (yPosition + rowHeight > PAGE_HEIGHT - BOTTOM_MARGIN) {
                break;
            }

            String employeeName = employeeNames.get(i);
            EmployeeMonthlySummary summary = summaryMap.get(employeeName);

            int rowY = yPosition + (rowsDrawn * rowHeight);

            // Draw alternate row background
            if (rowsDrawn % 2 == 0) {
                canvas.drawRect(LEFT_MARGIN, rowY, PAGE_WIDTH - RIGHT_MARGIN, rowY + rowHeight, altRowPaint);
            }

            // Draw vertical lines
            canvas.drawLine(colPhoneX, rowY, colPhoneX, rowY + rowHeight, linePaint);
            canvas.drawLine(colDeptX, rowY, colDeptX, rowY + rowHeight, linePaint);
            canvas.drawLine(colPresentX, rowY, colPresentX, rowY + rowHeight, linePaint);
            canvas.drawLine(colAbsentX, rowY, colAbsentX, rowY + rowHeight, linePaint);
            canvas.drawLine(colLateX, rowY, colLateX, rowY + rowHeight, linePaint);
            canvas.drawLine(colHalfDayX, rowY, colHalfDayX, rowY + rowHeight, linePaint);
            canvas.drawLine(colTotalX, rowY, colTotalX, rowY + rowHeight, linePaint);

            // Draw row content
            int textY = rowY + 13;

            // Employee Name - BETTER DISPLAY FOR LONG NAMES
            String name = summary.employeeName != null ? summary.employeeName : "Unknown";

            // Create a paint to measure text width
            Paint namePaint = new Paint(textPaint);
            float maxNameWidth = 200; // Maximum width for name column

            // If name is too long, wrap it to multiple lines
            if (namePaint.measureText(name) > maxNameWidth) {
                // Split name into words
                String[] words = name.split(" ");
                StringBuilder line1 = new StringBuilder();
                StringBuilder line2 = new StringBuilder();

                boolean firstLineFull = false;
                for (String word : words) {
                    if (!firstLineFull) {
                        if (namePaint.measureText(line1.toString() + " " + word) < maxNameWidth) {
                            if (line1.length() > 0) line1.append(" ");
                            line1.append(word);
                        } else {
                            firstLineFull = true;
                            line2.append(word);
                        }
                    } else {
                        if (line2.length() > 0) line2.append(" ");
                        line2.append(word);
                    }
                }

                // Draw first line
                canvas.drawText(line1.toString(), colNameX + 5, textY, textPaint);

                // Draw second line if needed
                if (line2.length() > 0) {
                    // Truncate if still too long
                    String secondLine = line2.toString();
                    if (namePaint.measureText(secondLine) > maxNameWidth) {
                        secondLine = truncateText(secondLine, maxNameWidth - 20, namePaint) + "...";
                    }
                    canvas.drawText(secondLine, colNameX + 5, textY + 10, textPaint);
                }
            } else {
                // Name fits in one line
                canvas.drawText(name, colNameX + 5, textY, textPaint);
            }

            // Phone
            String phone = summary.phone != null ? summary.phone : "-";
            canvas.drawText(phone, colPhoneX + 5, textY, textPaint);

            // Department
            String dept = summary.department != null ? summary.department : "-";
            if (dept.length() > 12) {
                dept = dept.substring(0, 9) + "...";
            }
            canvas.drawText(dept, colDeptX + 5, textY, textPaint);

            // Present Days (green)
            Paint presentPaint = new Paint(textPaint);
            presentPaint.setColor(Color.parseColor("#4CAF50"));
            presentPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.valueOf(summary.presentDays), colPresentX + 5, textY, presentPaint);

            // Absent Days (red)
            Paint absentPaint = new Paint(textPaint);
            absentPaint.setColor(Color.parseColor("#F44336"));
            absentPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.valueOf(summary.absentDays), colAbsentX + 5, textY, absentPaint);

            // Late Days (orange)
            Paint latePaint = new Paint(textPaint);
            latePaint.setColor(Color.parseColor("#FF5722"));
            latePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.valueOf(summary.lateDays), colLateX + 5, textY, latePaint);

            // Half Days (blue)
            Paint halfDayPaint = new Paint(textPaint);
            halfDayPaint.setColor(Color.parseColor("#2196F3"));
            halfDayPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.valueOf(summary.halfDays), colHalfDayX + 5, textY, halfDayPaint);

            // Total Days (black)
            Paint totalPaint = new Paint(textPaint);
            totalPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.valueOf(summary.totalWorkingDays), colTotalX + 5, textY, totalPaint);

            // Increase row height if name has two lines
            if (namePaint.measureText(name) > maxNameWidth) {
                rowHeight = 25; // Increase row height for two lines
            } else {
                rowHeight = 18; // Normal row height
            }

            rowsDrawn++;
        }

        // Draw horizontal line at bottom of table
        int tableBottomY = yPosition + (rowsDrawn * 18); // Use standard height for line
        canvas.drawLine(LEFT_MARGIN, tableBottomY, PAGE_WIDTH - RIGHT_MARGIN, tableBottomY, linePaint);

        return rowsDrawn;
    }

    // Add this helper method for text truncation
    private String truncateText(String text, float maxWidth, Paint paint) {
        if (text == null) return "";

        if (paint.measureText(text) <= maxWidth) {
            return text;
        }

        // Truncate with ellipsis
        String result = text;
        while (result.length() > 3 && paint.measureText(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }

        return result + "...";
    }

    private void drawMonthlyFooter(Canvas canvas, int pageNumber) {
        int footerY = PAGE_HEIGHT - 25;

        // Draw separator line with orange color
        Paint orangeLinePaint = new Paint();
        orangeLinePaint.setColor(Color.parseColor("#FF5722"));
        orangeLinePaint.setStrokeWidth(1f);
        canvas.drawLine(LEFT_MARGIN, footerY, PAGE_WIDTH - RIGHT_MARGIN, footerY, orangeLinePaint);

        // Get date range text
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String dateRangeText = displayFormat.format(fromDate.getTime()) + " to " + displayFormat.format(toDate.getTime());

        // Left footer text
        String leftText = "Employee Summary - " + dateRangeText;
        canvas.drawText(leftText, LEFT_MARGIN, PAGE_HEIGHT - 10, footerPaint);

        // Center footer text
        String centerText = "Page " + pageNumber;
        float textWidth = footerPaint.measureText(centerText);
        canvas.drawText(centerText, (PAGE_WIDTH - textWidth) / 2, PAGE_HEIGHT - 10, footerPaint);

        // Right footer text
        String rightText = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());
        float rightTextWidth = footerPaint.measureText(rightText);
        canvas.drawText(rightText, PAGE_WIDTH - RIGHT_MARGIN - rightTextWidth, PAGE_HEIGHT - 10, footerPaint);
    }

    // ==============================
    // COMMON METHODS
    // ==============================

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void drawFooter(Canvas canvas, int pageNumber, String reportType) {
        int footerY = PAGE_HEIGHT - 25;

        // Draw separator line with orange color
        Paint orangeLinePaint = new Paint();
        orangeLinePaint.setColor(Color.parseColor("#FF5722"));
        orangeLinePaint.setStrokeWidth(1f);
        canvas.drawLine(LEFT_MARGIN, footerY, PAGE_WIDTH - RIGHT_MARGIN, footerY, orangeLinePaint);

        // Left footer text
        String leftText = "AttendSmart © " + Calendar.getInstance().get(Calendar.YEAR);
        canvas.drawText(leftText, LEFT_MARGIN, PAGE_HEIGHT - 10, footerPaint);

        // Center footer text
        String centerText = "Page " + pageNumber;
        float textWidth = footerPaint.measureText(centerText);
        canvas.drawText(centerText, (PAGE_WIDTH - textWidth) / 2, PAGE_HEIGHT - 10, footerPaint);

        // Right footer text
        String rightText = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());
        float rightTextWidth = footerPaint.measureText(rightText);
        canvas.drawText(rightText, PAGE_WIDTH - RIGHT_MARGIN - rightTextWidth, PAGE_HEIGHT - 10, footerPaint);
    }

    private void savePdfDocument(PdfDocument pdfDocument, String fileNamePrefix) {
        try {
            // Create directory if it doesn't exist
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AttendSmart");
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    throw new Exception("Failed to create directory");
                }
            }

            // Create file name with timestamp and prefix
            SimpleDateFormat fileFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = fileNamePrefix + fileFormat.format(new Date()) + ".pdf";
            pdfFile = new File(directory, fileName);

            // Write PDF to file
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, "PDF generated successfully!", Toast.LENGTH_SHORT).show();

                // Enable view and share buttons
                btnViewPdf.setEnabled(true);
                btnSharePdf.setEnabled(true);
            });

        } catch (Exception e) {
            Log.e("ReportsActivity", "PDF Save Error: " + e.getMessage(), e);
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    private void viewPdf() {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(this, "Please generate report first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            // Check if there's a PDF viewer app
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No PDF viewer app found. Please install a PDF viewer.", Toast.LENGTH_SHORT).show();
                // Open Play Store to download PDF viewer
                Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
                playStoreIntent.setData(Uri.parse("market://details?id=com.adobe.reader"));
                startActivity(playStoreIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdf() {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(this, "Please generate report first", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("SharePDF", "PDF Path: " + pdfFile.getAbsolutePath());
        Log.d("SharePDF", "PDF Exists: " + pdfFile.exists());
        Log.d("SharePDF", "PDF Size: " + pdfFile.length());

        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    pdfFile);

            Log.d("SharePDF", "URI: " + uri.toString());

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share PDF"));

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("SharePDF", "Error: ", e);
        }
    }
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission required to save PDF files", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        // Clear collections to free memory
        if (employeeDataMap != null) {
            employeeDataMap.clear();
        }
        if (attendanceRecords != null) {
            attendanceRecords.clear();
        }
    }

    // ==============================
    // DATA CLASSES
    // ==============================

    private static class EmployeeData {
        String phone, name, email, department;
    }

    private static class CompanyInfo {
        String companyName, companyEmail, companyPhone;
    }

    private static class AttendanceRecord {
        String date, phone, employeeName, status, lateStatus, markedBy, checkInTime, checkOutTime, checkInAddress;
    }

    private static class EmployeeMonthlySummary {
        String employeeName;
        String phone;
        String department;
        String month;
        int presentDays;
        int absentDays;
        int lateDays;
        int halfDays;
        int totalWorkingDays;
    }

    private static class CountWrapper {
        int presentCount = 0;
        int absentCount = 0;
        int lateCount = 0;
        int halfDayCount = 0;
    }

    private static class MonthlyCountWrapper {
        int totalEmployees = 0;
        int workingDays = 0;
        int totalPresentDays = 0;
        int totalAbsentDays = 0;
    }
}
