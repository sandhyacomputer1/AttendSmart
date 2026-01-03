package com.sandhyyasofttech.attendsmart.Models;

public class SalaryConfig {

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
