package com.sandhyyasofttech.attendsmart.payroll;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.sandhyyasofttech.attendsmart.Models.SalarySnapshot;

import java.io.File;
import java.io.FileOutputStream;

public class SalaryPdfGenerator {

    public static File generateSalaryPdf(
            Context context,
            SalarySnapshot snapshot
    ) throws Exception {

        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint();

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = 40;

        // ===== TITLE =====
        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        canvas.drawText("Salary Slip", 220, y, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(false);

        y += 40;
        canvas.drawText("Month: " + snapshot.month, 40, y, paint);

        y += 20;
        canvas.drawText("Employee Mobile: " + snapshot.employeeMobile, 40, y, paint);

        y += 30;
        canvas.drawLine(40, y, 550, y, paint);

        // ===== ATTENDANCE =====
        y += 25;
        paint.setFakeBoldText(true);
        canvas.drawText("Attendance Summary", 40, y, paint);

        paint.setFakeBoldText(false);
        y += 20;
        canvas.drawText("Present Days: " + snapshot.attendanceSummary.presentDays, 40, y, paint);
        y += 18;
        canvas.drawText("Half Days: " + snapshot.attendanceSummary.halfDays, 40, y, paint);
        y += 18;
        canvas.drawText("Paid Leaves: " + snapshot.attendanceSummary.paidLeavesUsed, 40, y, paint);
        y += 18;
        canvas.drawText("Unpaid Leaves: " + snapshot.attendanceSummary.unpaidLeaves, 40, y, paint);

        // ===== SALARY =====
        y += 30;
        paint.setFakeBoldText(true);
        canvas.drawText("Salary Details", 40, y, paint);

        paint.setFakeBoldText(false);
        y += 20;
        canvas.drawText("Per Day Salary: ₹" +
                snapshot.calculationResult.perDaySalary, 40, y, paint);

        y += 18;
        canvas.drawText("Gross Salary: ₹" +
                snapshot.calculationResult.grossSalary, 40, y, paint);

        y += 18;
        canvas.drawText("PF Deduction: ₹" +
                snapshot.calculationResult.pfAmount, 40, y, paint);

        y += 18;
        canvas.drawText("ESI Deduction: ₹" +
                snapshot.calculationResult.esiAmount, 40, y, paint);

        y += 18;
        canvas.drawText("Other Deduction: ₹" +
                snapshot.calculationResult.otherDeduction, 40, y, paint);

        y += 18;
        canvas.drawText("Total Deduction: ₹" +
                snapshot.calculationResult.totalDeduction, 40, y, paint);

        y += 25;
        paint.setFakeBoldText(true);
        canvas.drawText("Net Salary: ₹" +
                snapshot.calculationResult.netSalary, 40, y, paint);

        pdf.finishPage(page);

        // ===== SAVE FILE =====
        File file = new File(
                context.getExternalFilesDir(null),
                "Salary_" + snapshot.employeeMobile + "_" + snapshot.month + ".pdf"
        );

        FileOutputStream fos = new FileOutputStream(file);
        pdf.writeTo(fos);

        fos.close();
        pdf.close();

        return file;
    }
}
