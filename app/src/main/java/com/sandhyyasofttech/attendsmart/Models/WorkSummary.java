package com.sandhyyasofttech.attendsmart.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class WorkSummary implements Parcelable {

    public String workDate;
    public String employeeName;
    public String completedWork;
    public String ongoingWork;
    public String tomorrowWork;
    public long submittedAt;

    // Default Constructor (Required for Firebase)
    public WorkSummary() {
    }

    // Full Constructor
    public WorkSummary(String workDate, String employeeName, String completedWork,
                       String ongoingWork, String tomorrowWork, long submittedAt) {
        this.workDate = workDate;
        this.employeeName = employeeName;
        this.completedWork = completedWork;
        this.ongoingWork = ongoingWork;
        this.tomorrowWork = tomorrowWork;
        this.submittedAt = submittedAt;
    }

    // Parcelable Implementation
    protected WorkSummary(Parcel in) {
        workDate = in.readString();
        employeeName = in.readString();
        completedWork = in.readString();
        ongoingWork = in.readString();
        tomorrowWork = in.readString();
        submittedAt = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(workDate);
        dest.writeString(employeeName);
        dest.writeString(completedWork);
        dest.writeString(ongoingWork);
        dest.writeString(tomorrowWork);
        dest.writeLong(submittedAt);
    }

    @Override
    public int describeContents() {
        return 0;
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

    // Getter/Setter (Optional - Firebase works with public fields too)
    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
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