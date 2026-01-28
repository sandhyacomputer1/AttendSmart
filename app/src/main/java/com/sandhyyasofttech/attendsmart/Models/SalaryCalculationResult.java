package com.sandhyyasofttech.attendsmart.Models;
import java.io.Serializable;

public class SalaryCalculationResult implements Serializable {  // ✅ implements Serializable

    public double perDaySalary;
    public double payableDays;

    public double grossSalary;

    public double pfAmount;
    public double esiAmount;
    public double otherDeduction;
    public double totalDeduction;

    public double netSalary;
}
