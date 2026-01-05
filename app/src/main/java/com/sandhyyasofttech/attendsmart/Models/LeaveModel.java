package com.sandhyyasofttech.attendsmart.Models;

public class LeaveModel {

    public String leaveId;
    public String employeeName;
    public String employeeMobile;
    public String fromDate;
    public String toDate;
    public String leaveType;
    public String halfDayType;
    public String reason;
    public String status;
    public String adminReason;
    public long appliedAt;
    public Boolean isPaid;
    public String approvedBy;
    public Long approvedAt;
    public Double totalDays;
    public Double paidDays;
    public Double unpaidDays;

    public LeaveModel() {}

    private double calculateTotalDays(LeaveModel m) {
        // TODO: use LocalDate for API 26+
        // for now assume admin already knows days
        return m.leaveType.equals("HALF_DAY") ? 0.5 : 1.0;
    }

}
