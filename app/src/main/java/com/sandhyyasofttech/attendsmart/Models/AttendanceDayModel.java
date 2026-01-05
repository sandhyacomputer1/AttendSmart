package com.sandhyyasofttech.attendsmart.Models;

public class AttendanceDayModel {
    public String date;
    public String status;
    public boolean isEmpty;

    public AttendanceDayModel(String date, String status, boolean isEmpty) {
        this.date = date;
        this.status = status;
        this.isEmpty = isEmpty;
    }
}