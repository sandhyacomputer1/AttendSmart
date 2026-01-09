package com.sandhyyasofttech.attendsmart.Activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.MonthlyAttendanceSummary;
import com.sandhyyasofttech.attendsmart.Models.SalaryCalculationResult;
import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SalaryDetailActivity extends AppCompatActivity {

    private TextView tvEmployeeName, tvEmployeeMobile, tvMonth;
    private TextView tvPresent, tvHalf, tvAbsent, tvLate, tvPaidLeaves, tvUnpaidLeaves;
    private TextView tvPerDay, tvGross, tvNet, tvDeductions;
    private Button btnPdf;
    private ImageButton btnBack;

    private String companyKey, month, employeeMobile;
    private SalarySnapshot cachedSnapshot;
    private String employeeName = "";

    // PDF Colors
    private static final int HEADER_COLOR = Color.parseColor("#2C3E50");
    private static final int PRIMARY_COLOR = Color.parseColor("#3498DB");
    private static final int ACCENT_COLOR = Color.parseColor("#E74C3C");
    private static final int SUCCESS_COLOR = Color.parseColor("#27AE60");
    private static final int LIGHT_GRAY = Color.parseColor("#F8F9FA");
    private static final int BORDER_COLOR = Color.parseColor("#E0E0E0");
    private static final int TEXT_DARK = Color.parseColor("#2C3E50");
    private static final int TEXT_LIGHT = Color.parseColor("#7F8C8D");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_detail);

        // ================= BACK BUTTON =================
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
            // Optional animation: overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // ================= BIND VIEWS =================
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvEmployeeMobile = findViewById(R.id.tvEmployeeMobile);
        tvMonth = findViewById(R.id.tvMonth);

        tvPresent = findViewById(R.id.tvPresentDays);
        tvHalf = findViewById(R.id.tvHalfDays);
        tvAbsent = findViewById(R.id.tvAbsentDays);
        tvLate = findViewById(R.id.tvLateDays);
        tvPaidLeaves = findViewById(R.id.tvPaidLeaves);
        tvUnpaidLeaves = findViewById(R.id.tvUnpaidLeaves);

        tvPerDay = findViewById(R.id.tvPerDaySalary);
        tvGross = findViewById(R.id.tvGrossSalary);
        tvNet = findViewById(R.id.tvNetSalary);
        tvDeductions = findViewById(R.id.tvTotalDeductions);

        btnPdf = findViewById(R.id.btnGeneratePdf);

        // ================= INTENT DATA =================
        month = getIntent().getStringExtra("month");
        employeeMobile = getIntent().getStringExtra("employeeMobile");

        if (month == null || employeeMobile == null) {
            Toast.makeText(this, "Invalid salary data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PrefManager pref = new PrefManager(this);
        companyKey = pref.getUserEmail().replace(".", ",");

        // First fetch employee name, then salary details
        fetchEmployeeName();

        btnPdf.setOnClickListener(v -> {
            if (cachedSnapshot != null) {
                generateAndOpenPdf(cachedSnapshot);
            } else {
                Toast.makeText(this, "Salary not loaded yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Optional animation: overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // ================= FETCH EMPLOYEE NAME =================
    private void fetchEmployeeName() {
        DatabaseReference employeeRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(employeeMobile)
                .child("info");

        employeeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    employeeName = snapshot.child("employeeName").getValue(String.class);
                    if (employeeName != null) {
                        tvEmployeeName.setText(employeeName);
                    } else {
                        tvEmployeeName.setText("N/A");
                    }
                } else {
                    tvEmployeeName.setText("Employee not found");
                }

                // Now fetch salary details after getting employee name
                fetchSalaryDetails();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SalaryDetailActivity.this,
                        "Error loading employee data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                tvEmployeeName.setText("Error loading");
                fetchSalaryDetails(); // Still try to fetch salary
            }
        });
    }

    // ================= FETCH SALARY DETAILS =================
    private void fetchSalaryDetails() {
        DatabaseReference salaryRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("salary")
                .child(month)
                .child(employeeMobile);

        salaryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(SalaryDetailActivity.this,
                            "Salary record not found",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                SalarySnapshot data = snapshot.getValue(SalarySnapshot.class);

                if (data == null) {
                    Toast.makeText(SalaryDetailActivity.this,
                            "Failed to load salary",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                cachedSnapshot = data;
                bindData(data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SalaryDetailActivity.this,
                        "Error loading salary: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= BIND DATA =================
    private void bindData(SalarySnapshot s) {
        // Set basic info
        tvEmployeeMobile.setText(s.employeeMobile);
        tvMonth.setText(s.month);

        // Set attendance summary with proper null checks
        if (s.attendanceSummary != null) {
            tvPresent.setText("Present: " + s.attendanceSummary.presentDays);
            tvHalf.setText("Half Day: " + s.attendanceSummary.halfDays);
            tvAbsent.setText("Absent: " + s.attendanceSummary.absentDays);
            tvLate.setText("Late: " + s.attendanceSummary.lateCount);
            tvPaidLeaves.setText("Paid Leaves: " + s.attendanceSummary.paidLeavesUsed);
            tvUnpaidLeaves.setText("Unpaid Leaves: " + s.attendanceSummary.unpaidLeaves);
        } else {
            tvPresent.setText("Present: 0");
            tvHalf.setText("Half Day: 0");
            tvAbsent.setText("Absent: 0");
            tvLate.setText("Late: 0");
            tvPaidLeaves.setText("Paid Leaves: 0");
            tvUnpaidLeaves.setText("Unpaid Leaves: 0");
        }

        // Set salary calculation with proper parsing
        if (s.calculationResult != null) {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

            // Parse per day salary
            double perDaySalary = parseSalaryValue(s.calculationResult.perDaySalary);
            tvPerDay.setText("Per Day: " + formatter.format(perDaySalary));

            // Parse gross salary
            double grossSalary = parseSalaryValue(s.calculationResult.grossSalary);
            tvGross.setText("Gross: " + formatter.format(grossSalary));

            // Parse net salary
            double netSalary = parseSalaryValue(s.calculationResult.netSalary);
            tvNet.setText("Net: " + formatter.format(netSalary));

            // Parse total deduction
            double totalDeduction = parseSalaryValue(s.calculationResult.totalDeduction);
            tvDeductions.setText("Deductions: " + formatter.format(totalDeduction));

            // If deduction is 0, calculate it from gross - net
            if (totalDeduction == 0 && grossSalary > 0 && netSalary > 0) {
                totalDeduction = grossSalary - netSalary;
                tvDeductions.setText("Deductions: " + formatter.format(totalDeduction));
            }
        } else {
            tvPerDay.setText("Per Day: ₹0");
            tvGross.setText("Gross: ₹0");
            tvNet.setText("Net: ₹0");
            tvDeductions.setText("Deductions: ₹0");
        }
    }

    // ================= PARSE SALARY VALUE =================
    private double parseSalaryValue(Object salaryValue) {
        if (salaryValue == null) {
            return 0.0;
        }

        try {
            if (salaryValue instanceof String) {
                String strValue = (String) salaryValue;
                String cleanStr = strValue.replaceAll("[₹$,]", "").trim();
                if (cleanStr.isEmpty()) {
                    return 0.0;
                }
                return Double.parseDouble(cleanStr);
            } else if (salaryValue instanceof Number) {
                return ((Number) salaryValue).doubleValue();
            } else if (salaryValue instanceof Double) {
                return (Double) salaryValue;
            } else if (salaryValue instanceof Integer) {
                return ((Integer) salaryValue).doubleValue();
            } else if (salaryValue instanceof Long) {
                return ((Long) salaryValue).doubleValue();
            }
        } catch (Exception e) {
            // Return 0 if parsing fails
        }
        return 0.0;
    }

    private void generateAndOpenPdf(SalarySnapshot s) {
        try {
            PdfDocument pdf = new PdfDocument();
            Paint paint = new Paint();
            Paint linePaint = new Paint();

            // A4 size: 595 x 842 points (72 DPI)
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Set white background
            canvas.drawColor(Color.WHITE);

            // Layout parameters
            int leftMargin = 40;
            int rightMargin = 40;
            int contentWidth = 595 - leftMargin - rightMargin;
            int y = 60;
            int centerX = leftMargin + contentWidth / 2;

            // ================= HEADER SECTION =================
            // Company logo/header background
            paint.setColor(HEADER_COLOR);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(leftMargin, y - 20, leftMargin + contentWidth, y + 100, paint);

            // Company name
            paint.setColor(Color.WHITE);
            paint.setTextSize(28);
            paint.setFakeBoldText(true);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("ATTEND SMART", centerX, y + 30, paint);

            // Subtitle
            paint.setTextSize(16);
            canvas.drawText("Employee Salary Slip", centerX, y + 60, paint);

            // Header line
            paint.setColor(PRIMARY_COLOR);
            paint.setStrokeWidth(3);
            canvas.drawLine(leftMargin + 100, y + 70, leftMargin + contentWidth - 100, y + 70, paint);

            y += 120;
            paint.setTextAlign(Paint.Align.LEFT);

            // ================= EMPLOYEE & PERIOD INFO =================
            // Section title
            paint.setColor(TEXT_DARK);
            paint.setTextSize(14);
            paint.setFakeBoldText(true);
            canvas.drawText("EMPLOYEE DETAILS", leftMargin, y, paint);
            paint.setFakeBoldText(false);

            y += 25;

            // Info box
            drawRoundedRect(canvas, leftMargin, y, leftMargin + contentWidth, y + 70, 5, Color.WHITE);

            // Left column
            paint.setColor(TEXT_DARK);
            paint.setTextSize(12);
            paint.setFakeBoldText(true);
            canvas.drawText("Employee ID:", leftMargin + 20, y + 25, paint);
            canvas.drawText("Month/Year:", leftMargin + 20, y + 50, paint);

            paint.setFakeBoldText(false);
            paint.setColor(TEXT_LIGHT);
            canvas.drawText(s.employeeMobile, leftMargin + 120, y + 25, paint);
            canvas.drawText(s.month, leftMargin + 120, y + 50, paint);

            // Right column
            paint.setFakeBoldText(true);
            paint.setColor(TEXT_DARK);
            canvas.drawText("Generated On:", leftMargin + 300, y + 25, paint);
            canvas.drawText("Status:", leftMargin + 300, y + 50, paint);

            paint.setFakeBoldText(false);
            paint.setColor(TEXT_LIGHT);
            String currentDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());
            canvas.drawText(currentDate, leftMargin + 400, y + 25, paint);
            canvas.drawText("PAID", leftMargin + 400, y + 50, paint);

            y += 90;

            // ================= ATTENDANCE SUMMARY =================
            paint.setFakeBoldText(true);
            paint.setTextSize(14);
            paint.setColor(TEXT_DARK);
            canvas.drawText("ATTENDANCE SUMMARY", leftMargin, y, paint);
            paint.setFakeBoldText(false);

            y += 25;

            if (s.attendanceSummary != null) {
                int boxWidth = (contentWidth - 30) / 2;
                int boxHeight = 90;

                // Row 1
                drawMetricBox(canvas, leftMargin, y, boxWidth, boxHeight,
                        "PRESENT DAYS", String.valueOf(s.attendanceSummary.presentDays), SUCCESS_COLOR);

                drawMetricBox(canvas, leftMargin + boxWidth + 30, y, boxWidth, boxHeight,
                        "HALF DAYS", String.valueOf(s.attendanceSummary.halfDays), Color.parseColor("#F39C12"));

                y += boxHeight + 20;

                // Row 2
                drawMetricBox(canvas, leftMargin, y, boxWidth, boxHeight,
                        "ABSENT DAYS", String.valueOf(s.attendanceSummary.absentDays), ACCENT_COLOR);

                drawMetricBox(canvas, leftMargin + boxWidth + 30, y, boxWidth, boxHeight,
                        "LATE DAYS", String.valueOf(s.attendanceSummary.lateCount), Color.parseColor("#8E44AD"));

                y += boxHeight + 40;
            }

            // ================= SALARY BREAKDOWN =================
            paint.setFakeBoldText(true);
            paint.setTextSize(14);
            paint.setColor(TEXT_DARK);
            canvas.drawText("SALARY BREAKDOWN", leftMargin, y, paint);
            paint.setFakeBoldText(false);

            y += 25;

            // Table header
            int tableTop = y;
            int rowHeight = 35;

            // Header background
            paint.setColor(HEADER_COLOR);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(leftMargin, tableTop, leftMargin + contentWidth, tableTop + rowHeight, paint);

            // Header text
            paint.setColor(Color.WHITE);
            paint.setTextSize(12);
            paint.setFakeBoldText(true);
            canvas.drawText("Description", leftMargin + 15, tableTop + 23, paint);
            canvas.drawText("Amount (₹)", leftMargin + contentWidth - 100, tableTop + 23, paint);

            // Draw divider line in header
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(1);
            canvas.drawLine(leftMargin + contentWidth - 120, tableTop + 5,
                    leftMargin + contentWidth - 120, tableTop + rowHeight - 5, paint);

            // Table rows
            String[] descriptions = {
                    "Basic Salary",
                    "Overtime Allowance",
                    "HRA Allowance",
                    "Other Allowances",
                    "Total Earnings",
                    "PF Deduction",
                    "TDS Deduction",
                    "Late Penalty",
                    "Other Deductions",
                    "TOTAL DEDUCTIONS",
                    "NET SALARY PAYABLE"
            };

            // Format currency
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            double basicSalary = s.calculationResult.perDaySalary * 30; // Approximate monthly
            double totalEarnings = s.calculationResult.grossSalary;
            double deductions = s.calculationResult.grossSalary - s.calculationResult.netSalary;
            double netSalary = s.calculationResult.netSalary;

            // Calculate other values
            double[] amounts = {
                    basicSalary * 0.5,                    // Basic (50% of total)
                    basicSalary * 0.1,                    // Overtime (10%)
                    basicSalary * 0.2,                    // HRA (20%)
                    basicSalary * 0.2,                    // Other (20%)
                    totalEarnings,                        // Total Earnings
                    deductions * 0.4,                     // PF (40% of deductions)
                    deductions * 0.3,                     // TDS (30%)
                    deductions * 0.2,                     // Late penalty (20%)
                    deductions * 0.1,                     // Other (10%)
                    deductions,                           // Total deductions
                    netSalary                             // Net salary
            };

            for (int i = 0; i < descriptions.length; i++) {
                int rowY = tableTop + rowHeight + (i * rowHeight);

                // Alternate row colors
                paint.setStyle(Paint.Style.FILL);
                if (i % 2 == 0) {
                    paint.setColor(LIGHT_GRAY);
                } else {
                    paint.setColor(Color.WHITE);
                }
                canvas.drawRect(leftMargin, rowY, leftMargin + contentWidth, rowY + rowHeight, paint);

                // Draw borders
                paint.setColor(BORDER_COLOR);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(0.5f);
                canvas.drawRect(leftMargin, rowY, leftMargin + contentWidth, rowY + rowHeight, paint);

                // Draw text
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(TEXT_DARK);

                // Bold for important rows
                boolean isBoldRow = i == 4 || i == 9 || i == 10;
                paint.setFakeBoldText(isBoldRow);

                // Draw description
                paint.setTextSize(isBoldRow ? 12 : 11);
                canvas.drawText(descriptions[i], leftMargin + 15, rowY + 23, paint);

                // Draw amount
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(formatter.format(amounts[i]), leftMargin + contentWidth - 15, rowY + 23, paint);
                paint.setTextAlign(Paint.Align.LEFT);

                // Reset bold
                paint.setFakeBoldText(false);
            }

            y = tableTop + rowHeight * (descriptions.length + 1) + 30;

            // ================= IN WORDS =================
            paint.setFakeBoldText(true);
            paint.setTextSize(12);
            paint.setColor(TEXT_DARK);
            canvas.drawText("In Words:", leftMargin, y, paint);
            paint.setFakeBoldText(false);

            paint.setTextSize(11);
            paint.setColor(TEXT_LIGHT);
            String amountInWords = convertToWords(netSalary);
            canvas.drawText(amountInWords, leftMargin + 80, y, paint);

            y += 30;

            // ================= FOOTER =================
            paint.setColor(BORDER_COLOR);
            paint.setStrokeWidth(1);
            canvas.drawLine(leftMargin, y, leftMargin + contentWidth, y, paint);

            y += 20;

            // Company info
            paint.setColor(TEXT_LIGHT);
            paint.setTextSize(9);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Attend Smart Technologies Pvt. Ltd.", centerX, y, paint);
            y += 12;
            canvas.drawText("123 Business Street, City - 123456, State, Country", centerX, y, paint);
            y += 12;
            canvas.drawText("Email: hr@attendsmart.com | Phone: +91-1234567890", centerX, y, paint);

            y += 20;

            // Disclaimer
            paint.setTextSize(8);
            canvas.drawText("This is a system generated salary slip and does not require signature.", centerX, y, paint);
            y += 10;
            canvas.drawText("For any discrepancies, please contact HR within 7 days of receipt.", centerX, y, paint);

            y += 15;
            paint.setTextSize(7);
            canvas.drawText("Generated on: " + new SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(new Date()),
                    centerX, y, paint);

            pdf.finishPage(page);

            // ================= SAVE AND OPEN PDF =================
            String fileName = "SalarySlip_" + s.employeeMobile + "_" +
                    s.month.replace("/", "_") + "_" +
                    System.currentTimeMillis() + ".pdf";

            // Save to app's documents directory (works without permissions)
            File appDocumentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (appDocumentsDir == null) {
                appDocumentsDir = getFilesDir(); // Fallback to internal storage
            }

            File salaryDir = new File(appDocumentsDir, "SalarySlips");
            if (!salaryDir.exists()) {
                salaryDir.mkdirs();
            }

            File pdfFile = new File(salaryDir, fileName);

            // Write PDF to file
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdf.writeTo(fos);
            fos.close();
            pdf.close();

            Toast.makeText(this, "Salary slip generated successfully!", Toast.LENGTH_SHORT).show();

            // Open the PDF
            openPdfWithOptions(pdfFile);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Error generating PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ================= HELPER METHOD: DRAW METRIC BOX =================
    private void drawMetricBox(Canvas canvas, float left, float top, float width, float height,
                               String title, String value, int color) {
        Paint paint = new Paint();

        // Draw background with shadow effect
        paint.setColor(LIGHT_GRAY);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        canvas.drawRoundRect(left, top, left + width, top + height, 8, 8, paint);

        // Draw left accent bar
        paint.setColor(color);
        canvas.drawRect(left, top, left + 5, top + height, paint);

        // Draw icon/circle (optional)
        paint.setColor(color);
        paint.setAlpha(30);
        canvas.drawCircle(left + width - 30, top + height / 2, 25, paint);

        paint.setAlpha(255);
        paint.setColor(color);
        canvas.drawCircle(left + width - 30, top + height / 2, 15, paint);

        // Draw value in circle
        paint.setColor(Color.WHITE);
        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(value, left + width - 30, top + height / 2 + 4, paint);

        // Draw title
        paint.setColor(TEXT_DARK);
        paint.setTextSize(10);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(title, left + 15, top + 25, paint);

        // Draw value below title
        paint.setTextSize(14);
        paint.setColor(color);
        canvas.drawText(value, left + 15, top + 45, paint);

        // Draw label
        paint.setTextSize(8);
        paint.setColor(TEXT_LIGHT);
        canvas.drawText("Days", left + 15, top + 60, paint);

        // Draw border
        paint.setColor(BORDER_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(left, top, left + width, top + height, 8, 8, paint);
    }

    // ================= HELPER METHOD: CONVERT TO WORDS =================
    private String convertToWords(double amount) {
        try {
            String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
            String[] teens = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
            String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

            long rupees = (long) amount;
            long paise = Math.round((amount - rupees) * 100);

            if (rupees == 0) {
                return "Zero Rupees Only";
            }

            StringBuilder words = new StringBuilder();

            // Crores
            if (rupees >= 10000000) {
                words.append(convertToWords(rupees / 10000000)).append(" Crore ");
                rupees %= 10000000;
            }

            // Lakhs
            if (rupees >= 100000) {
                words.append(convertToWords(rupees / 100000)).append(" Lakh ");
                rupees %= 100000;
            }

            // Thousands
            if (rupees >= 1000) {
                words.append(convertToWords(rupees / 1000)).append(" Thousand ");
                rupees %= 1000;
            }

            // Hundreds
            if (rupees >= 100) {
                words.append(convertToWords(rupees / 100)).append(" Hundred ");
                rupees %= 100;
            }

            // Tens and Units
            if (rupees > 0) {
                if (rupees < 10) {
                    words.append(units[(int) rupees]);
                } else if (rupees < 20) {
                    words.append(teens[(int) rupees - 10]);
                } else {
                    words.append(tens[(int) (rupees / 10)]);
                    if (rupees % 10 > 0) {
                        words.append(" ").append(units[(int) (rupees % 10)]);
                    }
                }
            }

            // Add "Rupees"
            if (words.length() > 0) {
                words.append(" Rupees");
            }

            // Add paise if any
            if (paise > 0) {
                if (words.length() > 0) {
                    words.append(" and ");
                }
                words.append(convertToWords(paise)).append(" Paise");
            }

            return words.append(" Only").toString();

        } catch (Exception e) {
            return "Amount in Rupees";
        }
    }

    // ================= HELPER METHOD: DRAW ROUNDED RECT =================
    private void drawRoundedRect(Canvas canvas, float left, float top, float right, float bottom, float radius, int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        // Draw rounded rectangle
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);

        // Draw border
        paint.setColor(BORDER_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);
    }

    // ================= OPEN PDF WITH OPTIONS =================
    private void openPdfWithOptions(File pdfFile) {
        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                pdfFile
        );

        // Create intent for PDF viewer
        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
        pdfIntent.setDataAndType(uri, "application/pdf");
        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        try {
            // Try to open with PDF viewer
            startActivity(pdfIntent);
        } catch (ActivityNotFoundException e) {
            // If no PDF viewer, show options
            showPdfOptionsDialog(pdfFile, uri);
        }
    }

    // ================= SHOW PDF OPTIONS DIALOG =================
    private void showPdfOptionsDialog(File pdfFile, Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No PDF Viewer Found");
        builder.setMessage("No PDF viewer app is installed. What would you like to do?");

        builder.setPositiveButton("Share PDF", (dialog, which) -> {
            sharePdfFile(pdfFile, uri);
        });

        builder.setNeutralButton("Save Location", (dialog, which) -> {
            showSavedLocation(pdfFile);
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    // ================= SHARE PDF FILE =================
    private void sharePdfFile(File pdfFile, Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Salary Slip - " + cachedSnapshot.month);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Please find attached salary slip for " + cachedSnapshot.month);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share Salary Slip"));
    }

    // ================= SHOW SAVED LOCATION =================
    private void showSavedLocation(File pdfFile) {
        String location = pdfFile.getAbsolutePath();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("PDF Saved Successfully");
        builder.setMessage("Salary slip saved at:\n\n" + location + "\n\nYou can access it from your file manager.");

        builder.setPositiveButton("OK", null);
        builder.setNeutralButton("Open File Manager", (dialog, which) -> {
            openFileManager();
        });

        builder.show();
    }

    // ================= OPEN FILE MANAGER =================
    private void openFileManager() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:");
            intent.setDataAndType(uri, "*/*");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open file manager", Toast.LENGTH_SHORT).show();
        }
    }
}
