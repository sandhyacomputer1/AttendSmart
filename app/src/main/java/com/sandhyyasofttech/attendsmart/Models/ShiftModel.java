package com.sandhyyasofttech.attendsmart.Models;

public class ShiftModel {
    private String shiftName;
    private String startTime;
    private String endTime;
    private long createdAt;
    private long updatedAt;
    private int employeeCount;

    // Empty constructor for Firebase
    public ShiftModel() {}

    // Constructor with parameters
    public ShiftModel(String shiftName, String startTime, String endTime) {
        this.shiftName = shiftName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = System.currentTimeMillis();
        this.employeeCount = 0;
    }

    // Getters and Setters
    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(int employeeCount) {
        this.employeeCount = employeeCount;
    }

    // For backward compatibility with old code
    public String getName() {
        return shiftName;
    }

    public void setName(String name) {
        this.shiftName = name;
    }
}