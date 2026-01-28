package com.sandhyyasofttech.attendsmart.Models;

import java.io.Serializable;

public class SalaryPreviewData implements Serializable {  // ✅ हे तुम्ही करायला विसरलात
    public String month;
    public String employeeMobile;
    public MonthlyAttendanceSummary attendanceSummary;
    public SalaryConfig salaryConfig;
    public SalaryCalculationResult calculationResult;

    // Manual adjustments केलेले fields
    public double manualGrossSalary;  // basicSalary च्या ऐवजी grossSalary
    public double manualPfAmount;
    public double manualEsiAmount;
    public double manualOtherDeduction; // otherDeductions च्या ऐवजी otherDeduction
    public double manualNetSalary;
    public String notes;

    public SalaryPreviewData() {}
}