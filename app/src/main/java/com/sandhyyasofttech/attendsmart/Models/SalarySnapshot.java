package com.sandhyyasofttech.attendsmart.Models;

import java.io.Serializable;

public class SalarySnapshot implements Serializable {  // ✅ implements Serializable
    // नवीन fields add करा
    public boolean manualAdjustments;
    public String adjustmentNotes;
    // Meta
    public String month;          // "2026-01"
    public String employeeMobile;
    public long generatedAt;

    // Attendance summary
    public MonthlyAttendanceSummary attendanceSummary;

    // Salary result
    public SalaryCalculationResult calculationResult;

    // Salary rules snapshot (VERY IMPORTANT)
    public SalaryConfig salaryConfigSnapshot;

    public SalarySnapshot() {
        // Firebase required
    }
}
