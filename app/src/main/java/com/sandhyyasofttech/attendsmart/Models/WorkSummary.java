package com.sandhyyasofttech.attendsmart.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class WorkSummary implements Parcelable {
    public String employeeName;
    public String workSummary;
    public String tasks;
    public String issues;
    public long submittedAt;
    public String workDate; // ← NEW FIELD

    public WorkSummary() {}

    protected WorkSummary(Parcel in) {
        employeeName = in.readString();
        workSummary = in.readString();
        tasks = in.readString();
        issues = in.readString();
        submittedAt = in.readLong();
        workDate = in.readString();
    }

    public static final Creator<WorkSummary> CREATOR = new Creator<WorkSummary>() {
        @Override
        public WorkSummary createFromParcel(Parcel in) {
            return new WorkSummary(in);
        }

        @Override
        public WorkSummary[] newArray(int size) {
            return new WorkSummary[size];
        }
    };

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(employeeName);
        dest.writeString(workSummary);
        dest.writeString(tasks);
        dest.writeString(issues);
        dest.writeLong(submittedAt);
        dest.writeString(workDate);
    }
}
