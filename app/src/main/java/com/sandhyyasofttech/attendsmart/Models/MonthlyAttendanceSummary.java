package com.sandhyyasofttech.attendsmart.Models;
import java.io.Serializable;

public class MonthlyAttendanceSummary implements Serializable {  // ✅ implements Serializable
    public int presentDays = 0;
    public int halfDays = 0;
    public int absentDays = 0;
    public int lateCount = 0;
    public int paidLeavesUsed = 0;
    public int unpaidLeaves = 0;
}
