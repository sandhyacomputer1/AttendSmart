package com.sandhyyasofttech.attendsmart.Models;

import java.io.Serializable;

public class EmployeeWorkStatus implements Serializable {
    private String employeeMobile;
    private String employeeName;
    private String employeeEmail;
    private String employeeDepartment;
    private String employeeRole;
    private String profileImage;
    private boolean hasSubmitted;
    private String completedWork;
    private String ongoingWork;
    private String tomorrowWork;
    private long submittedAt;

    public EmployeeWorkStatus() {
    }

    // Getters and Setters
    public String getEmployeeMobile() {
        return employeeMobile;
    }

    public void setEmployeeMobile(String employeeMobile) {
        this.employeeMobile = employeeMobile;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    public String getEmployeeDepartment() {
        return employeeDepartment;
    }

    public void setEmployeeDepartment(String employeeDepartment) {
        this.employeeDepartment = employeeDepartment;
    }

    public String getEmployeeRole() {
        return employeeRole;
    }

    public void setEmployeeRole(String employeeRole) {
        this.employeeRole = employeeRole;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public boolean isHasSubmitted() {
        return hasSubmitted;
    }

    public void setHasSubmitted(boolean hasSubmitted) {
        this.hasSubmitted = hasSubmitted;
    }

    public String getCompletedWork() {
        return completedWork;
    }

    public void setCompletedWork(String completedWork) {
        this.completedWork = completedWork;
    }

    public String getOngoingWork() {
        return ongoingWork;
    }

    public void setOngoingWork(String ongoingWork) {
        this.ongoingWork = ongoingWork;
    }

    public String getTomorrowWork() {
        return tomorrowWork;
    }

    public void setTomorrowWork(String tomorrowWork) {
        this.tomorrowWork = tomorrowWork;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }
}