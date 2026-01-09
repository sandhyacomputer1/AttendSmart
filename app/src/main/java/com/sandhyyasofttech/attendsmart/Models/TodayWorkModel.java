package com.sandhyyasofttech.attendsmart.Models;

public class TodayWorkModel {

    private String employeeName;
    private String workSummary;
    private String tasks;
    private String issues;
    private long submittedAt;
    private String employeeMobile;

    public TodayWorkModel() {}

    public String getEmployeeName() {
        return employeeName;
    }

    public String getWorkSummary() {
        return workSummary;
    }

    public String getTasks() {
        return tasks;
    }

    public String getIssues() {
        return issues;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public String getEmployeeMobile() {
        return employeeMobile;
    }

    public void setEmployeeMobile(String employeeMobile) {
        this.employeeMobile = employeeMobile;
    }
}
