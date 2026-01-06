package com.sandhyyasofttech.attendsmart.Models;

public class AttendanceDayModel {
    public String date;     // yyyy-MM-dd
    public String status;   // Present, Absent, Half Day, Late, Future
    public boolean isEmpty; // for calendar blank cells

    public AttendanceDayModel() {}

    public AttendanceDayModel(String date, String status, boolean isEmpty) {
        this.date = date;
        this.status = status;
        this.isEmpty = isEmpty;
    }
}
