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
        // Header Background Paint - Blue color
        headerBgPaint = new Paint();
        headerBgPaint.setColor(Color.parseColor("#1976D2")); // Blue

        // Header Text Paint
        headerTextPaint = new Paint();
        headerTextPaint.setColor(Color.WHITE);
        headerTextPaint.setTextSize(10);
        headerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        headerTextPaint.setAntiAlias(true);

        // Title Paint
        titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#0D47A1")); // Dark Blue
        titlePaint.setTextSize(14);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setAntiAlias(true);

        // Regular Text Paint
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(9);
        textPaint.setAntiAlias(true);

        // Lighter Border Paint - Dark Gray instead of Black
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#666666")); // Dark Gray
        linePaint.setStrokeWidth(0.8f); // Reduced from 1.5f
        linePaint.setStyle(Paint.Style.STROKE);

        // Alternate Row Paint - Light Blue
        altRowPaint = new Paint();
        altRowPaint.setColor(Color.parseColor("#F0F8FF")); // Even lighter blue

        // Footer Paint
        footerPaint = new Paint();
        footerPaint.setColor(Color.GRAY);
        footerPaint.setTextSize(8);
        footerPaint.setAntiAlias(true);
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
        int availableWidth = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN;

        // Calculate proportional widths based on content needs
        colDateX = LEFT_MARGIN;
        colNameX = colDateX + 80;        // Date column: 80px
        colPhoneX = colNameX + 150;      // Name column: 150px
        colStatusX = colPhoneX + 100;    // Phone column: 100px
        colCheckInX = colStatusX + 80;   // Status column: 80px
        colCheckOutX = colCheckInX + 100; // Check-in: 100px
        colMarkedByX = colCheckOutX + 100; // Check-out: 100px
        // Total used: 80+150+100+80+100+100 = 610px, well within 842-60=782px

        // Validate boundaries
        if (colMarkedByX + 100 > PAGE_WIDTH - RIGHT_MARGIN) {
            // Auto-adjust if needed
            float scaleFactor = (float)(PAGE_WIDTH - RIGHT_MARGIN - LEFT_MARGIN) / (colMarkedByX + 100 - LEFT_MARGIN);
            colNameX = (int)(LEFT_MARGIN + 80 * scaleFactor);
            colPhoneX = (int)(colNameX + 150 * scaleFactor);
            colStatusX = (int)(colPhoneX + 100 * scaleFactor);
            colCheckInX = (int)(colStatusX + 80 * scaleFactor);
            colCheckOutX = (int)(colCheckInX + 100 * scaleFactor);
            colMarkedByX = (int)(colCheckOutX + 100 * scaleFactor);
        }
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
                        String weeklyHoliday = infoSnapshot.child("weeklyHoliday").getValue(String.class);

                        if (name != null && !name.trim().isEmpty()) {
                            EmployeeData emp = new EmployeeData();
                            emp.phone = phone;
                            emp.name = name.trim();
                            emp.email = email;
                            emp.department = department;
                            emp.weeklyHoliday = weeklyHoliday != null ? weeklyHoliday : "Sunday"; // Default
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

                // Draw table header - NO extra space added
                yPosition = drawDetailedTableHeader(canvas, yPosition);

                // REMOVED: yPosition += 10; // No extra space

                // Draw table rows with proper boundary checking
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
        // Draw blue background
        Paint headerBgPaint = new Paint();
        headerBgPaint.setColor(Color.parseColor("#1976D2"));
        canvas.drawRect(0, 0, PAGE_WIDTH, 70, headerBgPaint);

        // Company name
        String companyName = (companyInfo != null && companyInfo.companyName != null) ?
                companyInfo.companyName : "AttendSmart";
        Paint companyPaint = new Paint();
        companyPaint.setColor(Color.WHITE);
        companyPaint.setTextSize(22);
        companyPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        companyPaint.setAntiAlias(true);

        float companyWidth = companyPaint.measureText(companyName);
        float companyX = (PAGE_WIDTH - companyWidth) / 2;
        canvas.drawText(companyName, companyX, 35, companyPaint);

        // Report title
        Paint reportTitlePaint = new Paint();
        reportTitlePaint.setColor(Color.WHITE);
        reportTitlePaint.setTextSize(16);
        reportTitlePaint.setAntiAlias(true);
        String reportTitle = "DETAILED ATTENDANCE REPORT";
        float titleWidth = reportTitlePaint.measureText(reportTitle);
        float titleX = (PAGE_WIDTH - titleWidth) / 2;
        canvas.drawText(reportTitle, titleX, 55, reportTitlePaint);

        // Draw bottom border
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(2f);
        canvas.drawLine(0, 70, PAGE_WIDTH, 70, borderPaint);

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
        // Draw blue header background
        Paint headerBg = new Paint();
        headerBg.setColor(Color.parseColor("#1976D2"));
        canvas.drawRect(LEFT_MARGIN, yPosition, PAGE_WIDTH - RIGHT_MARGIN, yPosition + 24, headerBg);

        // Draw DARK GRAY border around header (lighter than black)
        Paint grayBorder = new Paint();
        grayBorder.setColor(Color.parseColor("#666666"));
        grayBorder.setStyle(Paint.Style.STROKE);
        grayBorder.setStrokeWidth(0.8f); // Lighter border
        canvas.drawRect(LEFT_MARGIN, yPosition, PAGE_WIDTH - RIGHT_MARGIN, yPosition + 24, grayBorder);

        int headerY = yPosition + 16; // Adjusted text position

        // Draw DARK GRAY vertical lines between columns (lighter)
        // IMPORTANT: Use exact column positions that match the table rows
        Paint verticalLine = new Paint();
        verticalLine.setColor(Color.parseColor("#666666"));
        verticalLine.setStrokeWidth(0.8f); // Lighter border

        // Make sure lines extend to exact header bottom
        float headerBottom = yPosition + 22;

        // Draw lines at EXACT column boundaries
        canvas.drawLine(colNameX, yPosition, colNameX, headerBottom, verticalLine);
        canvas.drawLine(colPhoneX, yPosition, colPhoneX, headerBottom, verticalLine);
        canvas.drawLine(colStatusX, yPosition, colStatusX, headerBottom, verticalLine);
        canvas.drawLine(colCheckInX, yPosition, colCheckInX, headerBottom, verticalLine);
        canvas.drawLine(colCheckOutX, yPosition, colCheckOutX, headerBottom, verticalLine);
        canvas.drawLine(colMarkedByX, yPosition, colMarkedByX, headerBottom, verticalLine);

        // White text for headers
        Paint headerText = new Paint();
        headerText.setColor(Color.WHITE);
        headerText.setTextSize(10);
        headerText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        headerText.setAntiAlias(true);

        canvas.drawText("Date", colDateX + 5, headerY, headerText);
        canvas.drawText("Employee Name", colNameX + 5, headerY, headerText);
        canvas.drawText("Phone", colPhoneX + 5, headerY, headerText);
        canvas.drawText("Status", colStatusX + 5, headerY, headerText);
        canvas.drawText("Check-In", colCheckInX + 5, headerY, headerText);
        canvas.drawText("Check-Out", colCheckOutX + 5, headerY, headerText);
        canvas.drawText("Marked By", colMarkedByX + 5, headerY, headerText);

        return yPosition + 26; // Reduced from 30 to minimize gap
    }

    private int drawDetailedTableRows(Canvas canvas, int yPosition, int startRecord) {
        int maxRowsPerPage = 22; // Increased by 2 rows since we have more space now
        int rowHeight = 20; // Slightly reduced row height
        int rowsDrawn = 0;

        // Reduced footer space to 40px (from 50px)
        int footerSpace = 40;

        // Lighter border paint
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#666666")); // Dark Gray
        borderPaint.setStrokeWidth(0.8f); // Lighter border

        for (int i = startRecord; i < attendanceRecords.size() && rowsDrawn < maxRowsPerPage; i++) {
            // Check if there's enough space for this row
            int currentY = yPosition + (rowsDrawn * rowHeight);

            // Reduced buffer to 45px (just 5px above footer)
            if (currentY + rowHeight > PAGE_HEIGHT - footerSpace - 5) {
                break;
            }

            AttendanceRecord record = attendanceRecords.get(i);
            int rowY = currentY;

            // Draw alternate row background (light blue)
            if (rowsDrawn % 2 == 0) {
                canvas.drawRect(LEFT_MARGIN, rowY, PAGE_WIDTH - RIGHT_MARGIN, rowY + rowHeight, altRowPaint);
            }

            // Draw horizontal border for row - Lighter
            canvas.drawLine(LEFT_MARGIN, rowY + rowHeight, PAGE_WIDTH - RIGHT_MARGIN, rowY + rowHeight, borderPaint);

            // Draw vertical lines - Lighter
            canvas.drawLine(colNameX, rowY, colNameX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colPhoneX, rowY, colPhoneX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colStatusX, rowY, colStatusX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colCheckInX, rowY, colCheckInX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colCheckOutX, rowY, colCheckOutX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colMarkedByX, rowY, colMarkedByX, rowY + rowHeight, borderPaint);

            int textY = rowY + 14; // Adjusted text position

            // Date
            String displayDate = formatDate(record.date);
            canvas.drawText(displayDate, colDateX + 5, textY, textPaint);

            // Employee Name
            String name = record.employeeName != null ? record.employeeName : "Unknown";
            if (name.length() > 25) {
                name = name.substring(0, 22) + "...";
            }
            canvas.drawText(name, colNameX + 5, textY, textPaint);

            // Phone
            canvas.drawText(record.phone != null ? record.phone : "-", colPhoneX + 5, textY, textPaint);

            // Status with color coding
            Paint statusPaint = new Paint(textPaint);
            statusPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            String status = record.status != null ? record.status : "-";
            if ("Present".equalsIgnoreCase(status)) {
                statusPaint.setColor(Color.parseColor("#2E7D32"));
            } else if ("Absent".equalsIgnoreCase(status)) {
                statusPaint.setColor(Color.parseColor("#C62828"));
            } else if (status.toLowerCase().contains("half")) {
                statusPaint.setColor(Color.parseColor("#1565C0"));
            }
            canvas.drawText(status, colStatusX + 5, textY, statusPaint);

            // Check-In Time
            canvas.drawText(record.checkInTime != null ? record.checkInTime : "-", colCheckInX + 5, textY, textPaint);

            // Check-Out Time
            canvas.drawText(record.checkOutTime != null ? record.checkOutTime : "-", colCheckOutX + 5, textY, textPaint);

            // Marked By
            String markedBy = record.markedBy != null ? record.markedBy : "-";
            if (markedBy.length() > 12) {
                markedBy = markedBy.substring(0, 10) + "..";
            }
            canvas.drawText(markedBy, colMarkedByX + 5, textY, textPaint);

            rowsDrawn++;
        }

        // Draw left and right borders for entire table - Lighter
        int tableBottomY = yPosition + (rowsDrawn * rowHeight);

        // Only draw vertical borders if we actually drew rows
        if (rowsDrawn > 0) {
            canvas.drawLine(LEFT_MARGIN, yPosition - 25, LEFT_MARGIN, tableBottomY, borderPaint);
            canvas.drawLine(PAGE_WIDTH - RIGHT_MARGIN, yPosition - 25, PAGE_WIDTH - RIGHT_MARGIN, tableBottomY, borderPaint);

            // Draw top border of table - Lighter
            canvas.drawLine(LEFT_MARGIN, yPosition - 25, PAGE_WIDTH - RIGHT_MARGIN, yPosition - 25, borderPaint);
        }

        return rowsDrawn;
    }

    private void createMonthlySummaryPdf() {
        try {
            PdfDocument pdfDocument = new PdfDocument();

            if (attendanceRecords.isEmpty()) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "No attendance records found", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // Calculate monthly summary data
            Map<String, EmployeeMonthlySummary> monthlySummary = calculateMonthlySummary();

            int pageNumber = 1;
            int startRecord = 0;

            // Use phone numbers as keys and sort by employee name
            List<String> employeePhones = new ArrayList<>(monthlySummary.keySet());
            Collections.sort(employeePhones, (p1, p2) -> {
                String name1 = monthlySummary.get(p1).employeeName;
                String name2 = monthlySummary.get(p2).employeeName;
                return name1.compareToIgnoreCase(name2);
            });

            while (startRecord < employeePhones.size()) {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                int yPosition = TOP_MARGIN;

                if (pageNumber == 1) {
                    yPosition = drawMonthlySummaryHeader(canvas, yPosition);
                    yPosition = drawMonthlyReportInfo(canvas, yPosition);
                    yPosition = drawMonthlySummaryStats(canvas, yPosition, monthlySummary);
                } else {
                    yPosition = drawMonthlyContinuationHeader(canvas, yPosition, pageNumber);
                }

                yPosition = drawMonthlyTableHeader(canvas, yPosition);
                int rowsDrawn = drawMonthlyTableRows(canvas, yPosition, startRecord, employeePhones, monthlySummary);
                startRecord += rowsDrawn;

                drawMonthlyFooter(canvas, pageNumber);

                pdfDocument.finishPage(page);
                pageNumber++;
            }

            savePdfDocument(pdfDocument, "Monthly_Summary_");

        } catch (Exception e) {
            Log.e("ReportsActivity", "Monthly PDF Error: " + e.getMessage(), e);
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to create PDF", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private int drawMonthlySummaryHeader(Canvas canvas, int yPosition) {
        // Draw blue background
        Paint headerBg = new Paint();
        headerBg.setColor(Color.parseColor("#1976D2"));
        canvas.drawRect(0, 0, PAGE_WIDTH, 70, headerBg);

        // Company name
        String companyName = (companyInfo != null && companyInfo.companyName != null) ?
                companyInfo.companyName : "AttendSmart";
        Paint companyPaint = new Paint();
        companyPaint.setColor(Color.WHITE);
        companyPaint.setTextSize(22);
        companyPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        companyPaint.setAntiAlias(true);

        float companyWidth = companyPaint.measureText(companyName);
        canvas.drawText(companyName, (PAGE_WIDTH - companyWidth) / 2, 35, companyPaint);

        // Report title
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(16);
        titlePaint.setAntiAlias(true);
        String reportTitle = "MONTHLY ATTENDANCE SUMMARY";
        float titleWidth = titlePaint.measureText(reportTitle);
        canvas.drawText(reportTitle, (PAGE_WIDTH - titleWidth) / 2, 55, titlePaint);

        // Draw bottom border
        Paint border = new Paint();
        border.setColor(Color.BLACK);
        border.setStrokeWidth(2f);
        canvas.drawLine(0, 70, PAGE_WIDTH, 70, border);

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
        int headerHeight = 24; // Reduced to match detailed report

        // Draw blue background
        Paint headerBg = new Paint();
        headerBg.setColor(Color.parseColor("#1976D2"));
        canvas.drawRect(LEFT_MARGIN, yPosition, PAGE_WIDTH - RIGHT_MARGIN, yPosition + headerHeight, headerBg);

        // Draw LIGHT GRAY border around header
        Paint grayBorder = new Paint();
        grayBorder.setColor(Color.parseColor("#666666")); // Dark Gray
        grayBorder.setStyle(Paint.Style.STROKE);
        grayBorder.setStrokeWidth(0.8f); // Lighter border
        canvas.drawRect(LEFT_MARGIN, yPosition, PAGE_WIDTH - RIGHT_MARGIN, yPosition + headerHeight, grayBorder);

        // Calculate column positions with proper spacing
        int colNameX = LEFT_MARGIN;
        int colPhoneX = colNameX + 180;
        int colDeptX = colPhoneX + 90;
        int colPresentX = colDeptX + 80;
        int colAbsentX = colPresentX + 70;
        int colLateX = colAbsentX + 65;
        int colHalfDayX = colLateX + 60;
        int colTotalX = colHalfDayX + 65;

        int headerY = yPosition + 16; // Adjusted for text positioning

        // Draw LIGHT GRAY vertical lines
        Paint vertLine = new Paint();
        vertLine.setColor(Color.parseColor("#666666")); // Dark Gray
        vertLine.setStrokeWidth(0.8f); // Lighter border

        canvas.drawLine(colPhoneX, yPosition, colPhoneX, yPosition + headerHeight, vertLine);
        canvas.drawLine(colDeptX, yPosition, colDeptX, yPosition + headerHeight, vertLine);
        canvas.drawLine(colPresentX, yPosition, colPresentX, yPosition + headerHeight, vertLine);
        canvas.drawLine(colAbsentX, yPosition, colAbsentX, yPosition + headerHeight, vertLine);
        canvas.drawLine(colLateX, yPosition, colLateX, yPosition + headerHeight, vertLine);
        canvas.drawLine(colHalfDayX, yPosition, colHalfDayX, yPosition + headerHeight, vertLine);
        canvas.drawLine(colTotalX, yPosition, colTotalX, yPosition + headerHeight, vertLine);

        // White text headers
        Paint headerText = new Paint();
        headerText.setColor(Color.WHITE);
        headerText.setTextSize(10);
        headerText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        headerText.setAntiAlias(true);

        canvas.drawText("Employee Name", colNameX + 5, headerY, headerText);
        canvas.drawText("Phone", colPhoneX + 5, headerY, headerText);
        canvas.drawText("Department", colDeptX + 5, headerY, headerText);
        canvas.drawText("Present", colPresentX + 5, headerY, headerText);
        canvas.drawText("Absent", colAbsentX + 5, headerY, headerText);
        canvas.drawText("Late", colLateX + 5, headerY, headerText);
        canvas.drawText("Half Day", colHalfDayX + 5, headerY, headerText);
        canvas.drawText("Total", colTotalX + 5, headerY, headerText);

        return yPosition + 28; // Same as detailed report
    }

    private int drawMonthlyTableRows(Canvas canvas, int yPosition, int startRecord,
                                     List<String> employeePhones, Map<String, EmployeeMonthlySummary> summaryMap) {
        int maxRowsPerPage = 22; // Increased to match detailed report
        int rowHeight = 20; // Reduced row height
        int rowsDrawn = 0;

        // Column positions (same as header)
        int colNameX = LEFT_MARGIN;
        int colPhoneX = colNameX + 180;
        int colDeptX = colPhoneX + 90;
        int colPresentX = colDeptX + 80;
        int colAbsentX = colPresentX + 70;
        int colLateX = colAbsentX + 65;
        int colHalfDayX = colLateX + 60;
        int colTotalX = colHalfDayX + 65;

        // Lighter border paint
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#666666")); // Dark Gray
        borderPaint.setStrokeWidth(0.8f); // Lighter border

        for (int i = startRecord; i < employeePhones.size() && rowsDrawn < maxRowsPerPage; i++) {
            if (yPosition + rowHeight > PAGE_HEIGHT - 45) { // Adjusted for footer
                break;
            }

            String phone = employeePhones.get(i);
            EmployeeMonthlySummary summary = summaryMap.get(phone);

            int rowY = yPosition + (rowsDrawn * rowHeight);

            // Draw alternate row background (light blue)
            if (rowsDrawn % 2 == 0) {
                canvas.drawRect(LEFT_MARGIN, rowY, PAGE_WIDTH - RIGHT_MARGIN, rowY + rowHeight, altRowPaint);
            }

            // Draw LIGHT GRAY horizontal border
            canvas.drawLine(LEFT_MARGIN, rowY + rowHeight, PAGE_WIDTH - RIGHT_MARGIN, rowY + rowHeight, borderPaint);

            // Draw LIGHT GRAY vertical lines
            canvas.drawLine(colPhoneX, rowY, colPhoneX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colDeptX, rowY, colDeptX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colPresentX, rowY, colPresentX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colAbsentX, rowY, colAbsentX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colLateX, rowY, colLateX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colHalfDayX, rowY, colHalfDayX, rowY + rowHeight, borderPaint);
            canvas.drawLine(colTotalX, rowY, colTotalX, rowY + rowHeight, borderPaint);

            int textY = rowY + 14; // Adjusted text position

            // Employee Name
            String name = summary.employeeName != null ? summary.employeeName : "Unknown";
            if (name.length() > 28) {
                name = name.substring(0, 25) + "...";
            }
            canvas.drawText(name, colNameX + 5, textY, textPaint);

            // Phone
            canvas.drawText(summary.phone != null ? summary.phone : "-", colPhoneX + 5, textY, textPaint);

            // Department
            String dept = summary.department != null ? summary.department : "-";
            if (dept.length() > 12) {
                dept = dept.substring(0, 10) + "..";
            }
            canvas.drawText(dept, colDeptX + 5, textY, textPaint);

            // Present Days (green)
            Paint presentPaint = new Paint(textPaint);
            presentPaint.setColor(Color.parseColor("#2E7D32"));
            presentPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.valueOf(summary.presentDays), colPresentX + 5, textY, presentPaint);

            // Absent Days (red)
            Paint absentPaint = new Paint(textPaint);
            absentPaint.setColor(Color.parseColor("#C62828"));
            absentPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.valueOf(summary.absentDays), colAbsentX + 5, textY, absentPaint);

            // Late Days (orange)
            Paint latePaint = new Paint(textPaint);
            latePaint.setColor(Color.parseColor("#EF6C00"));
            latePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.valueOf(summary.lateDays), colLateX + 5, textY, latePaint);

            // Half Days (blue)
            Paint halfDayPaint = new Paint(textPaint);
            halfDayPaint.setColor(Color.parseColor("#1565C0"));
            halfDayPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.valueOf(summary.halfDays), colHalfDayX + 5, textY, halfDayPaint);

            // Total Days
            Paint totalPaint = new Paint(textPaint);
            totalPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.valueOf(summary.totalWorkingDays), colTotalX + 5, textY, totalPaint);

            rowsDrawn++;
        }

        // Draw left and right borders with LIGHT GRAY
        int tableBottomY = yPosition + (rowsDrawn * rowHeight);
        canvas.drawLine(LEFT_MARGIN, yPosition - 28, LEFT_MARGIN, tableBottomY, borderPaint);
        canvas.drawLine(PAGE_WIDTH - RIGHT_MARGIN, yPosition - 28, PAGE_WIDTH - RIGHT_MARGIN, tableBottomY, borderPaint);

        // Draw bottom border of table
        canvas.drawLine(LEFT_MARGIN, tableBottomY, PAGE_WIDTH - RIGHT_MARGIN, tableBottomY, borderPaint);

        return rowsDrawn;
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






    private void fetchAttendanceData() {
        databaseRef.child("attendance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot attendanceSnapshot) {
                progressDialog.setMessage("Processing attendance data...");

                // Clear all data
                attendanceRecords.clear();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                // Map to track attendance by date and phone
                Map<String, Map<String, AttendanceRecord>> dateEmployeeMap = new HashMap<>();

                // 1. FIRST: Collect ALL existing attendance records from Firebase
                Calendar currentDate = (Calendar) fromDate.clone();
                while (!currentDate.after(toDate)) {
                    String dateKey = dateFormat.format(currentDate.getTime());

                    if (attendanceSnapshot.hasChild(dateKey)) {
                        DataSnapshot dateSnapshot = attendanceSnapshot.child(dateKey);

                        for (DataSnapshot empSnapshot : dateSnapshot.getChildren()) {
                            String phone = empSnapshot.getKey();

                            // Only process if employee exists
                            if (employeeDataMap.containsKey(phone)) {
                                AttendanceRecord record = createAttendanceRecord(empSnapshot, dateKey, phone);

                                // Store in map for easy lookup
                                if (!dateEmployeeMap.containsKey(dateKey)) {
                                    dateEmployeeMap.put(dateKey, new HashMap<>());
                                }
                                dateEmployeeMap.get(dateKey).put(phone, record);

                                attendanceRecords.add(record);
                            }
                        }
                    }
                    currentDate.add(Calendar.DAY_OF_MONTH, 1);
                }

                // 2. SECOND: For each employee, check each date in range
                currentDate = (Calendar) fromDate.clone();

                while (!currentDate.after(toDate)) {
                    String dateKey = dateFormat.format(currentDate.getTime());

                    for (Map.Entry<String, EmployeeData> empEntry : employeeDataMap.entrySet()) {
                        String phone = empEntry.getKey();
                        EmployeeData empData = empEntry.getValue();

                        // Check if we already have a record for this employee on this date
                        boolean hasRecord = dateEmployeeMap.containsKey(dateKey) &&
                                dateEmployeeMap.get(dateKey).containsKey(phone);

                        // If no record exists, check if we should create an absent record
                        if (!hasRecord) {
                            // Check if this is a working day for this employee
                            String weeklyHoliday = empData.weeklyHoliday != null ? empData.weeklyHoliday : "Sunday";
                            boolean isHoliday = isWeeklyHoliday(currentDate, weeklyHoliday);

                            // Only create absent record if it's NOT a holiday
                            if (!isHoliday) {
                                AttendanceRecord absentRecord = createAbsentRecord(dateKey, phone, empData.name);

                                // Add to collections
                                if (!dateEmployeeMap.containsKey(dateKey)) {
                                    dateEmployeeMap.put(dateKey, new HashMap<>());
                                }
                                dateEmployeeMap.get(dateKey).put(phone, absentRecord);
                                attendanceRecords.add(absentRecord);
                            }
                        }
                    }
                    currentDate.add(Calendar.DAY_OF_MONTH, 1);
                }

                // 3. THIRD: COUNT ALL RECORDS - USING SAME LOGIC AS AdminEmployeeAttendanceActivity
                CountWrapper counts = new CountWrapper();

                for (AttendanceRecord record : attendanceRecords) {
                    if (record.status != null) {
                        String status = record.status.trim().toLowerCase();
                        String lateStatus = record.lateStatus != null ? record.lateStatus.trim().toLowerCase() : "";

                        // Skip holiday records completely (like your other activity does)
                        if (status.contains("holiday") || status.contains("leave")) {
                            continue;
                        }

                        // Check if it has check-in time or is marked as present/half day
                        boolean hasCheckIn = record.checkInTime != null && !record.checkInTime.isEmpty();
                        boolean countedPresent = false;

                        // ✅ PRESENT (base condition) - SAME LOGIC
                        if (hasCheckIn || status.contains("present") || status.contains("full") || status.contains("half")) {
                            counts.presentCount++;
                            countedPresent = true;
                        }

                        // ✅ HALF DAY (independent) - SAME LOGIC
                        if (status.contains("half")) {
                            counts.halfDayCount++;
                        }

                        // ✅ LATE (independent) - SAME LOGIC
                        if ("late".equalsIgnoreCase(lateStatus) || status.contains("late")) {
                            counts.lateCount++;
                        }

                        // ✅ ABSENT - SAME LOGIC
                        // If no check-in & no present/half day status → ABSENT
                        if (!countedPresent && (status.equals("absent") ||
                                (record.checkInTime == null && !status.contains("half") && !status.contains("present")))) {
                            counts.absentCount++;
                        }
                    }
                }

                // 4. Calculate monthly summary
                Map<String, EmployeeMonthlySummary> monthlySummary = calculateMonthlySummary();
                MonthlyCountWrapper monthlyCounts = new MonthlyCountWrapper();
                monthlyCounts.totalEmployees = employeeDataMap.size();

                // Calculate totals
                int totalWorkingDays = 0;
                for (EmployeeMonthlySummary summary : monthlySummary.values()) {
                    monthlyCounts.totalPresentDays += summary.presentDays;
                    monthlyCounts.totalAbsentDays += summary.absentDays;
                    totalWorkingDays += summary.totalWorkingDays;
                }

                // For display, show average or total working days
                monthlyCounts.workingDays = monthlySummary.isEmpty() ? 0 : totalWorkingDays;

                // 5. Update UI
                runOnUiThread(() -> {
                    // Update detailed summary
                    tvPresentCount.setText(String.valueOf(counts.presentCount));
                    tvAbsentCount.setText(String.valueOf(counts.absentCount));
                    tvLateCount.setText(String.valueOf(counts.lateCount));
                    tvHalfDayCount.setText(String.valueOf(counts.halfDayCount));

                    // Update monthly summary
                    tvTotalEmployeesSummary.setText(String.valueOf(monthlyCounts.totalEmployees));
                    tvWorkingDays.setText(String.valueOf(monthlyCounts.workingDays));
                    tvTotalPresentDays.setText(String.valueOf(monthlyCounts.totalPresentDays));
                    tvTotalAbsentDays.setText(String.valueOf(monthlyCounts.totalAbsentDays));

                    // Debug logging
                    Log.d("FINAL_COUNTS",
                            "Employees: " + monthlyCounts.totalEmployees +
                                    "\nPresent: " + counts.presentCount +
                                    "\nAbsent: " + counts.absentCount +
                                    "\nLate: " + counts.lateCount +
                                    "\nHalfDay: " + counts.halfDayCount +
                                    "\nTotalRecords: " + attendanceRecords.size());

                    // Generate PDF
                    new Thread(() -> {
                        if (selectedReportType == 0) {
                            createDetailedPdf();
                        } else {
                            createMonthlySummaryPdf();
                        }
                    }).start();

                    progressDialog.dismiss();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ReportsActivity.this,
                            "Error loading attendance data",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private AttendanceRecord createAttendanceRecord(DataSnapshot empSnapshot, String dateKey, String phone) {
        AttendanceRecord record = new AttendanceRecord();
        record.date = dateKey;
        record.phone = phone;

        EmployeeData empData = employeeDataMap.get(phone);
        record.employeeName = empData != null ? empData.name : "Unknown";

        // Get basic fields
        record.checkInTime = getStringValue(empSnapshot, "checkInTime");
        record.checkOutTime = getStringValue(empSnapshot, "checkOutTime");
        record.markedBy = getStringValue(empSnapshot, "markedBy");
        record.checkInAddress = getStringValue(empSnapshot, "checkInAddress");
        record.lateStatus = getStringValue(empSnapshot, "lateStatus");

        // DETERMINE STATUS - THIS IS THE CRITICAL PART
        String status = getStringValue(empSnapshot, "finalStatus");
        if (status == null || status.isEmpty()) {
            status = getStringValue(empSnapshot, "status");
        }

        // If still no status, check if there's check-in time
        if ((status == null || status.isEmpty()) && record.checkInTime != null && !record.checkInTime.isEmpty()) {
            status = "Present";
        }

        // Normalize status
        if (status != null && !status.isEmpty()) {
            String statusLower = status.trim().toLowerCase();

            if (statusLower.equals("present") || statusLower.equals("late")) {
                record.status = "Present";
            } else if (statusLower.equals("absent")) {
                record.status = "Absent";
            } else if (statusLower.contains("half") || statusLower.equals("half day") || statusLower.equals("half-day")) {
                record.status = "Half Day";
            } else if (statusLower.equals("holiday") || statusLower.equals("leave")) {
                record.status = "Holiday";
            } else {
                record.status = status; // Keep original
            }
        } else {
            record.status = "Unknown";
        }
//
//        // Special case: If lateStatus is "Late", ensure status is "Present"
//        if ("Late".equalsIgnoreCase(record.lateStatus) && !"Absent".equalsIgnoreCase(record.status)) {
//            record.status = "Present";
//        }

        return record;
    }


    private AttendanceRecord createAbsentRecord(String dateKey, String phone, String name) {
        AttendanceRecord record = new AttendanceRecord();
        record.date = dateKey;
        record.phone = phone;
        record.employeeName = name != null ? name : "Unknown";
        record.status = "Absent";
        record.lateStatus = null;
        record.checkInTime = null;
        record.checkOutTime = null;
        record.markedBy = "System";
        record.checkInAddress = "Not Applicable";
        return record;
    }


    private Map<String, EmployeeMonthlySummary> calculateMonthlySummary() {
        Map<String, EmployeeMonthlySummary> summaryMap = new HashMap<>();

        if (employeeDataMap.isEmpty()) {
            return summaryMap;
        }

        // Group attendance records by employee
        Map<String, List<AttendanceRecord>> employeeRecords = new HashMap<>();
        for (AttendanceRecord record : attendanceRecords) {
            if (!employeeRecords.containsKey(record.phone)) {
                employeeRecords.put(record.phone, new ArrayList<>());
            }
            employeeRecords.get(record.phone).add(record);
        }

        // Process each employee
        for (Map.Entry<String, EmployeeData> empEntry : employeeDataMap.entrySet()) {
            String phone = empEntry.getKey();
            EmployeeData empData = empEntry.getValue();

            EmployeeMonthlySummary summary = new EmployeeMonthlySummary();
            summary.employeeName = empData.name;
            summary.phone = phone;
            summary.department = empData.department;

            // Calculate working days for this employee
            String weeklyHoliday = empData.weeklyHoliday != null ? empData.weeklyHoliday : "Sunday";
            summary.totalWorkingDays = calculateWorkingDaysForEmployee(fromDate, toDate, weeklyHoliday);

            // Initialize counters
            summary.presentDays = 0;
            summary.absentDays = 0;
            summary.lateDays = 0;
            summary.halfDays = 0;

            // Count from this employee's records - USING SAME LOGIC
            if (employeeRecords.containsKey(phone)) {
                for (AttendanceRecord record : employeeRecords.get(phone)) {
                    if (record.status != null) {
                        String status = record.status.trim().toLowerCase();
                        String lateStatus = record.lateStatus != null ? record.lateStatus.trim().toLowerCase() : "";

                        // Skip holiday/leave - SAME LOGIC
                        if (status.contains("holiday") || status.contains("leave")) {
                            continue;
                        }

                        // Check if it has check-in time or is marked as present/half day
                        boolean hasCheckIn = record.checkInTime != null && !record.checkInTime.isEmpty();
                        boolean countedPresent = false;

                        // ✅ PRESENT (base condition) - SAME LOGIC
                        if (hasCheckIn || status.contains("present") || status.contains("full") || status.contains("half")) {
                            summary.presentDays++;
                            countedPresent = true;
                        }

                        // ✅ HALF DAY (independent) - SAME LOGIC
                        if (status.contains("half")) {
                            summary.halfDays++;
                        }

                        // ✅ LATE (independent) - SAME LOGIC
                        if ("late".equalsIgnoreCase(lateStatus) || status.contains("late")) {
                            summary.lateDays++;
                        }

                        // ✅ ABSENT - SAME LOGIC
                        // If no check-in & no present/half day status → ABSENT
                        if (!countedPresent && (status.equals("absent") ||
                                (record.checkInTime == null && !status.contains("half") && !status.contains("present")))) {
                            summary.absentDays++;
                        }
                    }
                }
            } else {
                // If no records at all, employee is absent for all working days
                summary.absentDays = summary.totalWorkingDays;
            }

            summaryMap.put(phone, summary);

            // Debug log
            Log.d("EMP_SUMMARY", phone + " - " + summary.employeeName +
                    ": Present=" + summary.presentDays +
                    ", Absent=" + summary.absentDays +
                    ", Late=" + summary.lateDays +
                    ", HalfDay=" + summary.halfDays +
                    ", TotalWorking=" + summary.totalWorkingDays);
        }

        return summaryMap;
    }








    // Helper method to get string value from DataSnapshot
    private String getStringValue(DataSnapshot snapshot, String key) {
        if (snapshot.hasChild(key)) {
            Object value = snapshot.child(key).getValue();
            return value != null ? value.toString() : null;
        }
        return null;
    }

    // Helper method to check if a date is weekly holiday
    private boolean isWeeklyHoliday(Calendar date, String weeklyHoliday) {
        if (weeklyHoliday == null || weeklyHoliday.isEmpty()) {
            return false;
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        String dayOfWeek = dayFormat.format(date.getTime());

        return dayOfWeek.equalsIgnoreCase(weeklyHoliday.trim());
    }

    // Calculate working days based on employee's weekly holiday
    private int calculateWorkingDaysForEmployee(Calendar start, Calendar end, String weeklyHoliday) {
        int workingDays = 0;
        Calendar current = (Calendar) start.clone();

        while (!current.after(end)) {
            if (!isWeeklyHoliday(current, weeklyHoliday)) {
                workingDays++;
            }
            current.add(Calendar.DAY_OF_MONTH, 1);
        }

        return workingDays;
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

    private static class EmployeeData {
        String phone, name, email, department,weeklyHoliday;
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
        String weeklyHoliday; // Add this

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
