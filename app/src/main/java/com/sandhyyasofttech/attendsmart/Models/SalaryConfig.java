package com.sandhyyasofttech.attendsmart.Models;
import java.io.Serializable;

public class SalaryConfig implements Serializable {  // ✅ implements Serializable

    // 🔢 NUMBERS ONLY (Firebase Number)
    public double monthlySalary;
    public int workingDays;
    public int paidLeaves;

    public double pfPercent;
    public double esiPercent;
    public double otherDeduction;

    // 🔘 Boolean / String
    public boolean deductionEnabled;
    public String lateRule;
    public String effectiveFrom;
    public String deductionNote;

    // 🔑 REQUIRED for Firebase
    public SalaryConfig() {
    }
}
