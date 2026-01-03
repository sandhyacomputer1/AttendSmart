package com.sandhyyasofttech.attendsmart.payroll;

import com.sandhyyasofttech.attendsmart.Models.MonthlyAttendanceSummary;
import com.sandhyyasofttech.attendsmart.Models.SalaryCalculationResult;
import com.sandhyyasofttech.attendsmart.Models.SalaryConfig;

public class SalaryCalculator {

    public static SalaryCalculationResult calculateSalary(
            MonthlyAttendanceSummary summary,
            SalaryConfig config
    ) {

        SalaryCalculationResult result = new SalaryCalculationResult();

        // 🔐 SAFETY CHECK (NO NaN CRASH)
        if (config.workingDays <= 0 || config.monthlySalary <= 0) {
            return result;
        }

        double perDay = config.monthlySalary / config.workingDays;
        result.perDaySalary = perDay;

        double payableDays =
                summary.presentDays +
                        (summary.halfDays * 0.5) +
                        summary.paidLeavesUsed;

        result.payableDays = payableDays;
        result.grossSalary = payableDays * perDay;

        double pf = 0, esi = 0, other = 0;

        if (config.deductionEnabled) {
            pf = result.grossSalary * (config.pfPercent / 100.0);
            esi = result.grossSalary * (config.esiPercent / 100.0);
            other = config.otherDeduction;
        }

        result.pfAmount = pf;
        result.esiAmount = esi;
        result.otherDeduction = other;
        result.totalDeduction = pf + esi + other;
        result.netSalary = result.grossSalary - result.totalDeduction;

        return result;
    }
}
