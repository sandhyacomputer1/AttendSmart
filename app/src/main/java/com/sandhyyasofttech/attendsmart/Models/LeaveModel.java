package com.sandhyyasofttech.attendsmart.Models;

public class LeaveModel {

    // Leave identification
    public String leaveId;

    // Employee details
    public String employeeName;
    public String employeeMobile;

    // Leave dates
    public String fromDate;        // Format: yyyy-MM-dd
    public String toDate;          // Format: yyyy-MM-dd

    // Leave type
    public String leaveType;       // e.g., "CASUAL_LEAVE", "SICK_LEAVE", "FULL_DAY", "HALF_DAY"
    public String halfDayType;     // e.g., "FIRST_HALF", "SECOND_HALF" (only for half-day leaves)

    // Leave reason
    public String reason;          // Employee's reason for taking leave

    // Status
    public String status;          // "PENDING", "APPROVED", "REJECTED", "CANCELLED"

    // Admin details
    public String adminReason;     // Rejection/approval reason
    public String approvedBy;      // Admin who processed the leave
    public Long approvedAt;        // Timestamp when admin processed

    // Timestamps
    public long appliedAt;         // Unix timestamp when leave was applied

    // Payment and days calculation
    public Boolean isPaid;         // true = paid leave, false = unpaid leave
    public Double totalDays;       // Total number of days
    public Double paidDays;        // Number of paid days
    public Double unpaidDays;      // Number of unpaid days

    // Default constructor (required for Firebase)
    public LeaveModel() {}

    // Helper method to calculate total days
    private double calculateTotalDays(LeaveModel m) {
        // TODO: use LocalDate for API 26+
        // for now assume admin already knows days
        return m.leaveType.equals("HALF_DAY") ? 0.5 : 1.0;
    }

    // Helper method to get applied date as String (for adapter compatibility)
    public String getAppliedDate() {
        if (appliedAt > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(appliedAt));
        }
        return null;
    }

}