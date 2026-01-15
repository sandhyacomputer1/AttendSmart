package com.sandhyyasofttech.attendsmart.Activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SalaryDetailActivity extends AppCompatActivity {

    private TextView tvEmployeeName, tvEmployeeMobile, tvMonth;
    private EditText etPresent, etHalf, etAbsent, etLate;
    private EditText etPerDay, etGross, etNet, etDeductions;
    private Button btnPdf, btnSave, btnEdit;
    private ImageButton btnBack;

    private String companyKey, month, employeeMobile;
    private SalarySnapshot cachedSnapshot;
    private String employeeName = "";
    private boolean isEditMode = false;

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

        initializeViews();
        setupListeners();

        month = getIntent().getStringExtra("month");
        employeeMobile = getIntent().getStringExtra("employeeMobile");

        if (month == null || employeeMobile == null) {
            Toast.makeText(this, "Invalid salary data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PrefManager pref = new PrefManager(this);
        companyKey = pref.getUserEmail().replace(".", ",");

        fetchEmployeeName();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);

        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvEmployeeMobile = findViewById(R.id.tvEmployeeMobile);
        tvMonth = findViewById(R.id.tvMonth);

        etPresent = findViewById(R.id.etPresentDays);
        etHalf = findViewById(R.id.etHalfDays);
        etAbsent = findViewById(R.id.etAbsentDays);
        etLate = findViewById(R.id.etLateDays);

        etPerDay = findViewById(R.id.etPerDaySalary);
        etGross = findViewById(R.id.etGrossSalary);
        etNet = findViewById(R.id.etNetSalary);
        etDeductions = findViewById(R.id.etTotalDeductions);

        btnPdf = findViewById(R.id.btnGeneratePdf);
        btnSave = findViewById(R.id.btnSave);
        btnEdit = findViewById(R.id.btnEdit);

        setEditableMode(false);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> toggleEditMode());

        btnSave.setOnClickListener(v -> saveChanges());

        btnPdf.setOnClickListener(v -> {
            if (cachedSnapshot != null) {
                generateAndOpenPdf(cachedSnapshot);
            } else {
                Toast.makeText(this, "Salary not loaded yet", Toast.LENGTH_SHORT).show();
            }
        });

        // Auto-calculate when fields change
        TextWatcher calculationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditMode) {
                    recalculateSalary();
                }
            }
        };

        etPresent.addTextChangedListener(calculationWatcher);
        etHalf.addTextChangedListener(calculationWatcher);
        etAbsent.addTextChangedListener(calculationWatcher);
        etPerDay.addTextChangedListener(calculationWatcher);
        etDeductions.addTextChangedListener(calculationWatcher);
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        setEditableMode(isEditMode);

        if (isEditMode) {
            btnEdit.setText("Cancel");
            btnEdit.setBackgroundColor(Color.parseColor("#FF5722"));
            btnSave.setVisibility(View.VISIBLE);
        } else {
            btnEdit.setText("Edit");
            btnEdit.setBackgroundColor(Color.parseColor("#2196F3"));
            btnSave.setVisibility(View.GONE);
            // Reload original data
            if (cachedSnapshot != null) {
                bindData(cachedSnapshot);
            }
        }
    }

    private void setEditableMode(boolean editable) {
        etPresent.setEnabled(editable);
        etHalf.setEnabled(editable);
        etAbsent.setEnabled(editable);
        etLate.setEnabled(editable);
        etPerDay.setEnabled(editable);
        etDeductions.setEnabled(editable);

        // Net and Gross are always calculated
        etGross.setEnabled(false);
        etNet.setEnabled(false);
    }

    private void recalculateSalary() {
        try {
            int present = parseIntValue(etPresent.getText().toString());
            double halfDays = parseIntValue(etHalf.getText().toString()) * 0.5;
            double perDay = parseDoubleValue(etPerDay.getText().toString());
            double deductions = parseDoubleValue(etDeductions.getText().toString());

            double workingDays = present + halfDays;
            double gross = workingDays * perDay;
            double net = gross - deductions;

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            etGross.setText(formatter.format(gross));
            etNet.setText(formatter.format(net));

        } catch (Exception e) {
            // Silent calculation error
        }
    }

    private void saveChanges() {
        if (cachedSnapshot == null) return;

        try {
            // Update attendance summary
            cachedSnapshot.attendanceSummary.presentDays = parseIntValue(etPresent.getText().toString());
            cachedSnapshot.attendanceSummary.halfDays = parseIntValue(etHalf.getText().toString());
            cachedSnapshot.attendanceSummary.absentDays = parseIntValue(etAbsent.getText().toString());
            cachedSnapshot.attendanceSummary.lateCount = parseIntValue(etLate.getText().toString());

            // Update salary calculation
            double perDay = parseDoubleValue(etPerDay.getText().toString());
            double gross = parseDoubleValue(etGross.getText().toString());
            double net = parseDoubleValue(etNet.getText().toString());
            double deductions = parseDoubleValue(etDeductions.getText().toString());

            cachedSnapshot.calculationResult.perDaySalary = perDay;
            cachedSnapshot.calculationResult.grossSalary = gross;
            cachedSnapshot.calculationResult.netSalary = net;
            cachedSnapshot.calculationResult.totalDeduction = deductions;

            // Save to Firebase
            DatabaseReference salaryRef = FirebaseDatabase.getInstance()
                    .getReference("Companies")
                    .child(companyKey)
                    .child("salary")
                    .child(month)
                    .child(employeeMobile);

            salaryRef.setValue(cachedSnapshot)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Salary updated successfully", Toast.LENGTH_SHORT).show();
                        toggleEditMode();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Invalid data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

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
                    tvEmployeeName.setText(employeeName != null ? employeeName : "N/A");
                } else {
                    tvEmployeeName.setText("Employee not found");
                }
                fetchSalaryDetails();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SalaryDetailActivity.this,
                        "Error loading employee: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                fetchSalaryDetails();
            }
        });
    }

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
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindData(SalarySnapshot s) {
        tvEmployeeMobile.setText(s.employeeMobile);
        tvMonth.setText(s.month);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        if (s.attendanceSummary != null) {
            etPresent.setText(String.valueOf(s.attendanceSummary.presentDays));
            etHalf.setText(String.valueOf(s.attendanceSummary.halfDays));
            etAbsent.setText(String.valueOf(s.attendanceSummary.absentDays));
            etLate.setText(String.valueOf(s.attendanceSummary.lateCount));
        }

        if (s.calculationResult != null) {
            double perDay = parseSalaryValue(s.calculationResult.perDaySalary);
            double gross = parseSalaryValue(s.calculationResult.grossSalary);
            double net = parseSalaryValue(s.calculationResult.netSalary);
            double deductions = parseSalaryValue(s.calculationResult.totalDeduction);

            if (deductions == 0 && gross > 0 && net > 0) {
                deductions = gross - net;
            }

            etPerDay.setText(formatter.format(perDay));
            etGross.setText(formatter.format(gross));
            etNet.setText(formatter.format(net));
            etDeductions.setText(formatter.format(deductions));
        }
    }

    private double parseSalaryValue(Object value) {
        if (value == null) return 0.0;
        try {
            if (value instanceof String) {
                String str = ((String) value).replaceAll("[₹$,]", "").trim();
                return str.isEmpty() ? 0.0 : Double.parseDouble(str);
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Exception e) {
            // Return 0 on error
        }
        return 0.0;
    }

    private int parseIntValue(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleValue(String value) {
        try {
            return Double.parseDouble(value.replaceAll("[₹$,]", "").trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void generateAndOpenPdf(SalarySnapshot s) {
        try {
            PdfDocument pdf = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            canvas.drawColor(Color.WHITE);
            drawPdfContent(canvas, s);
            pdf.finishPage(page);

            String fileName = "SalarySlip_" + s.employeeMobile + "_" +
                    s.month.replace("/", "_") + "_" + System.currentTimeMillis() + ".pdf";

            File appDocumentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (appDocumentsDir == null) {
                appDocumentsDir = getFilesDir();
            }

            File salaryDir = new File(appDocumentsDir, "SalarySlips");
            if (!salaryDir.exists()) {
                salaryDir.mkdirs();
            }

            File pdfFile = new File(salaryDir, fileName);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdf.writeTo(fos);
            fos.close();
            pdf.close();

            Toast.makeText(this, "Salary slip generated!", Toast.LENGTH_SHORT).show();
            openPdfWithOptions(pdfFile);

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void drawPdfContent(Canvas canvas, SalarySnapshot s) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int leftMargin = 40;
        int rightMargin = 40;
        int contentWidth = 595 - leftMargin - rightMargin;
        int y = 60;
        int centerX = leftMargin + contentWidth / 2;

        // Header
        paint.setColor(HEADER_COLOR);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(leftMargin, y - 20, leftMargin + contentWidth, y + 100, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(28);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ATTEND SMART", centerX, y + 30, paint);

        paint.setTextSize(16);
        canvas.drawText("Employee Salary Slip", centerX, y + 60, paint);

        y += 120;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);

        // Employee Details
        paint.setColor(TEXT_DARK);
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("EMPLOYEE DETAILS", leftMargin, y, paint);
        y += 30;

        paint.setTextSize(12);
        canvas.drawText("Name:", leftMargin + 20, y, paint);
        paint.setFakeBoldText(false);
        paint.setColor(TEXT_LIGHT);
        canvas.drawText(employeeName, leftMargin + 100, y, paint);
        y += 25;

        paint.setFakeBoldText(true);
        paint.setColor(TEXT_DARK);
        canvas.drawText("Mobile:", leftMargin + 20, y, paint);
        paint.setFakeBoldText(false);
        paint.setColor(TEXT_LIGHT);
        canvas.drawText(s.employeeMobile, leftMargin + 100, y, paint);
        y += 25;

        paint.setFakeBoldText(true);
        paint.setColor(TEXT_DARK);
        canvas.drawText("Month:", leftMargin + 20, y, paint);
        paint.setFakeBoldText(false);
        paint.setColor(TEXT_LIGHT);
        canvas.drawText(s.month, leftMargin + 100, y, paint);

        y += 50;

        // Attendance Summary
        paint.setFakeBoldText(true);
        paint.setTextSize(14);
        paint.setColor(TEXT_DARK);
        canvas.drawText("ATTENDANCE SUMMARY", leftMargin, y, paint);
        y += 30;

        if (s.attendanceSummary != null) {
            drawAttendanceRow(canvas, leftMargin, y, "Present Days:", String.valueOf(s.attendanceSummary.presentDays), SUCCESS_COLOR);
            y += 25;
            drawAttendanceRow(canvas, leftMargin, y, "Half Days:", String.valueOf(s.attendanceSummary.halfDays), Color.parseColor("#F39C12"));
            y += 25;
            drawAttendanceRow(canvas, leftMargin, y, "Absent Days:", String.valueOf(s.attendanceSummary.absentDays), ACCENT_COLOR);
            y += 25;
            drawAttendanceRow(canvas, leftMargin, y, "Late Days:", String.valueOf(s.attendanceSummary.lateCount), Color.parseColor("#8E44AD"));
        }

        y += 50;

        // Salary Details
        paint.setFakeBoldText(true);
        paint.setTextSize(14);
        paint.setColor(TEXT_DARK);
        canvas.drawText("SALARY BREAKDOWN", leftMargin, y, paint);
        y += 30;

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        double perDay = parseSalaryValue(s.calculationResult.perDaySalary);
        double gross = parseSalaryValue(s.calculationResult.grossSalary);
        double net = parseSalaryValue(s.calculationResult.netSalary);
        double deductions = parseSalaryValue(s.calculationResult.totalDeduction);

        drawSalaryRow(canvas, leftMargin, y, "Per Day Salary:", formatter.format(perDay));
        y += 25;
        drawSalaryRow(canvas, leftMargin, y, "Gross Salary:", formatter.format(gross));
        y += 25;
        drawSalaryRow(canvas, leftMargin, y, "Total Deductions:", formatter.format(deductions));
        y += 25;

        paint.setFakeBoldText(true);
        paint.setTextSize(16);
        paint.setColor(SUCCESS_COLOR);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Net Salary:", leftMargin + 20, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(formatter.format(net), leftMargin + contentWidth - 20, y, paint);

        y += 50;

        // Footer
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(TEXT_LIGHT);
        paint.setTextSize(9);
        paint.setFakeBoldText(false);
        canvas.drawText("This is a system generated salary slip", centerX, y, paint);
        y += 15;
        canvas.drawText("Generated on: " + new SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(new Date()), centerX, y, paint);
    }

    private void drawAttendanceRow(Canvas canvas, float left, float y, String label, String value, int color) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        paint.setColor(TEXT_DARK);
        canvas.drawText(label, left + 20, y, paint);

        paint.setColor(color);
        canvas.drawText(value, left + 200, y, paint);
    }

    private void drawSalaryRow(Canvas canvas, float left, float y, String label, String value) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        paint.setColor(TEXT_DARK);
        canvas.drawText(label, left + 20, y, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(value, left + 515, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void openPdfWithOptions(File pdfFile) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
        pdfIntent.setDataAndType(uri, "application/pdf");
        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(pdfIntent);
        } catch (ActivityNotFoundException e) {
            showPdfOptionsDialog(pdfFile, uri);
        }
    }

    private void showPdfOptionsDialog(File pdfFile, Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle("No PDF Viewer Found")
                .setMessage("Would you like to share the PDF?")
                .setPositiveButton("Share", (d, w) -> sharePdfFile(pdfFile, uri))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sharePdfFile(File pdfFile, Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Salary Slip - " + month);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Salary Slip"));
    }
}