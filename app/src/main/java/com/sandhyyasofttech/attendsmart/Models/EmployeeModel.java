package com.sandhyyasofttech.attendsmart.Models;

import java.io.Serializable;

public class EmployeeModel implements Serializable {
    private String employeeId;
    private String employeeName;
    private String employeeMobile;
    private String employeeRole;
    private String employeeEmail;
    private String employeeDepartment;  // ✅ CHANGED from "department"
    private String employeePassword;
    private String employeeStatus;
    private String employeeShift;
    private String createdAt;
    private String weeklyHoliday;
    private String joinDate;
    private String salary;
    private String address;
    private String emergencyContact;
    private String todayStatus; // "Present", "Absent", "Late", "Half Day"
    private String checkInTime;
    private String totalHours;
    private String checkInPhoto;
    private String checkOutTime;
    private String checkOutPhoto;


    // Default constructor REQUIRED for Firebase
    public EmployeeModel() {}

    public EmployeeModel(String employeeId, String employeeName, String employeeMobile,
                         String employeeRole, String employeeEmail) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.employeeMobile = employeeMobile;
        this.employeeRole = employeeRole;
        this.employeeEmail = employeeEmail;
    }
    public String getCheckInPhoto() { return checkInPhoto; }
    public void setCheckInPhoto(String checkInPhoto) { this.checkInPhoto = checkInPhoto;}
    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }
    public String getTotalHours() { return totalHours; }
    public void setTotalHours(String totalHours) { this.totalHours = totalHours; }
    public String getTodayStatus() { return todayStatus; }
    public void setTodayStatus(String todayStatus) { this.todayStatus = todayStatus; }
    // ✅ ALL 11 GETTERS & SETTERS
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeMobile() { return employeeMobile; }
    public void setEmployeeMobile(String employeeMobile) { this.employeeMobile = employeeMobile; }

    public String getEmployeeRole() { return employeeRole; }
    public void setEmployeeRole(String employeeRole) { this.employeeRole = employeeRole; }

    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }

    // ✅ FIXED: Now matches layout
    public String getEmployeeDepartment() { return employeeDepartment; }
    public void setEmployeeDepartment(String employeeDepartment) { this.employeeDepartment = employeeDepartment; }

    public String getEmployeePassword() { return employeePassword; }
    public void setEmployeePassword(String employeePassword) { this.employeePassword = employeePassword; }

    public String getEmployeeStatus() { return employeeStatus; }
    public void setEmployeeStatus(String employeeStatus) { this.employeeStatus = employeeStatus; }

    public String getEmployeeShift() { return employeeShift; }
    public void setEmployeeShift(String employeeShift) { this.employeeShift = employeeShift; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getWeeklyHoliday() { return weeklyHoliday; }
    public void setWeeklyHoliday(String weeklyHoliday) { this.weeklyHoliday = weeklyHoliday; }

    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    // ✅ ADD THESE GETTERS/SETTERS
    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }


    public String getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(String checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getCheckOutPhoto() {
        return checkOutPhoto;
    }

    public void setCheckOutPhoto(String checkOutPhoto) {
        this.checkOutPhoto = checkOutPhoto;
    }

}
