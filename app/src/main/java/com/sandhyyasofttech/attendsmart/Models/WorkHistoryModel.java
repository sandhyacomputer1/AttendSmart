package com.sandhyyasofttech.attendsmart.Models;

import java.io.Serializable;

public class WorkHistoryModel implements Serializable {
    private String workDate;
    private String completedWork;
    private String ongoingWork;
    private String tomorrowWork;
    private long submittedAt;

    public WorkHistoryModel() {
    }

    public WorkHistoryModel(String workDate, String completedWork, String ongoingWork, 
                           String tomorrowWork, long submittedAt) {
        this.workDate = workDate;
        this.completedWork = completedWork;
        this.ongoingWork = ongoingWork;
        this.tomorrowWork = tomorrowWork;
        this.submittedAt = submittedAt;
    }

    // Getters and Setters
    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
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