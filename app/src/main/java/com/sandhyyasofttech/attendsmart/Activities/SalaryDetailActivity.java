package com.sandhyyasofttech.attendsmart.Activities;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.io.File;
import java.io.FileOutputStream;

public class SalaryDetailActivity extends AppCompatActivity {

    private TextView tvEmployeeMobile, tvMonth;
    private TextView tvPresent, tvHalf, tvAbsent, tvLate;
    private TextView tvPerDay, tvGross, tvNet;
    private Button btnPdf;

    private String companyKey, month, employeeMobile;
    private SalarySnapshot cachedSnapshot; // 🔒 for PDF use

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_detail);

        // ================= BIND VIEWS =================
        tvEmployeeMobile = findViewById(R.id.tvEmployeeMobile);
        tvMonth = findViewById(R.id.tvMonth);

        tvPresent = findViewById(R.id.tvPresentDays);
        tvHalf = findViewById(R.id.tvHalfDays);
        tvAbsent = findViewById(R.id.tvAbsentDays);
        tvLate = findViewById(R.id.tvLateDays);

        tvPerDay = findViewById(R.id.tvPerDaySalary);
        tvGross = findViewById(R.id.tvGrossSalary);
        tvNet = findViewById(R.id.tvNetSalary);

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
        companyKey = pref.getCompanyKey();

        fetchSalaryDetails();

        btnPdf.setOnClickListener(v -> {
            if (cachedSnapshot != null) {
                generateAndOpenPdf(cachedSnapshot);
            } else {
                Toast.makeText(this, "Salary not loaded yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= FETCH SALARY =================
    private void fetchSalaryDetails() {

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("salary")
                .child(month)
                .child(employeeMobile);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
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
                        error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= BIND DATA =================
    private void bindData(SalarySnapshot s) {

        tvEmployeeMobile.setText(s.employeeMobile);
        tvMonth.setText(s.month);

        if (s.attendanceSummary != null) {
            tvPresent.setText("Present : " + s.attendanceSummary.presentDays);
            tvHalf.setText("Half Day : " + s.attendanceSummary.halfDays);
            tvAbsent.setText("Absent : " + s.attendanceSummary.absentDays);
            tvLate.setText("Late : " + s.attendanceSummary.lateCount);
        }

        if (s.calculationResult != null) {
            tvPerDay.setText("Per Day Salary : ₹ " + s.calculationResult.perDaySalary);
            tvGross.setText("Gross Salary : ₹ " + s.calculationResult.grossSalary);
            tvNet.setText("Net Salary : ₹ " + s.calculationResult.netSalary);
        }
    }

    // ================= PDF GENERATION =================
    private void generateAndOpenPdf(SalarySnapshot s) {

        try {
            PdfDocument pdf = new PdfDocument();
            Paint paint = new Paint();

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            int x = 40;
            int y = 60;

            paint.setTextSize(18);
            paint.setFakeBoldText(true);
            canvas.drawText("SALARY SLIP", x + 180, y, paint);

            paint.setTextSize(12);
            paint.setFakeBoldText(false);

            y += 40;
            canvas.drawText("Employee Mobile : " + s.employeeMobile, x, y, paint);
            y += 20;
            canvas.drawText("Month : " + s.month, x, y, paint);

            y += 30;
            paint.setFakeBoldText(true);
            canvas.drawText("ATTENDANCE SUMMARY", x, y, paint);
            paint.setFakeBoldText(false);

            y += 20;
            canvas.drawText("Present Days : " + s.attendanceSummary.presentDays, x, y, paint);
            y += 20;
            canvas.drawText("Half Days : " + s.attendanceSummary.halfDays, x, y, paint);
            y += 20;
            canvas.drawText("Absent Days : " + s.attendanceSummary.absentDays, x, y, paint);
            y += 20;
            canvas.drawText("Late Count : " + s.attendanceSummary.lateCount, x, y, paint);

            y += 30;
            paint.setFakeBoldText(true);
            canvas.drawText("SALARY DETAILS", x, y, paint);
            paint.setFakeBoldText(false);

            y += 20;
            canvas.drawText("Per Day Salary : ₹ " + s.calculationResult.perDaySalary, x, y, paint);
            y += 20;
            canvas.drawText("Gross Salary : ₹ " + s.calculationResult.grossSalary, x, y, paint);
            y += 20;
            canvas.drawText("Net Salary : ₹ " + s.calculationResult.netSalary, x, y, paint);

            pdf.finishPage(page);

            File pdfFile = new File(
                    getExternalFilesDir("Pictures"),
                    "SalarySlip_" + s.employeeMobile + "_" + s.month + ".pdf"
            );

            pdf.writeTo(new FileOutputStream(pdfFile));
            pdf.close();

            openPdf(pdfFile);

        } catch (Exception e) {
            Toast.makeText(this,
                    "PDF Error : " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ================= OPEN PDF =================
    private void openPdf(File file) {

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent);
    }
}
