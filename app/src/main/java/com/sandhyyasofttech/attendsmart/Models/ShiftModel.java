package com.sandhyyasofttech.attendsmart.Models;

public class ShiftModel {
    public String name;
    public String startTime;
    public String endTime;

    public ShiftModel() {} // Firebase required

    public ShiftModel(String name, String startTime, String endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
