package com.sandhyyasofttech.attendsmart.Models;

public class GeoFencingConfig {
    private boolean enabled;
    private double officeLat;
    private double officeLng;
    private int radiusMeters;
    private float accuracyThreshold;
    private long trackingInterval;
    private boolean trackingEnabled;
    private String updatedAt;
    private String updatedBy;

    public GeoFencingConfig() {
        // Default constructor for Firebase
        this.enabled = false;
        this.radiusMeters = 100;
        this.accuracyThreshold = 50;
        this.trackingInterval = 300000; // 5 minutes
        this.trackingEnabled = false;
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public double getOfficeLat() { return officeLat; }
    public void setOfficeLat(double officeLat) { this.officeLat = officeLat; }
    
    public double getOfficeLng() { return officeLng; }
    public void setOfficeLng(double officeLng) { this.officeLng = officeLng; }
    
    public int getRadiusMeters() { return radiusMeters; }
    public void setRadiusMeters(int radiusMeters) { this.radiusMeters = radiusMeters; }
    
    public float getAccuracyThreshold() { return accuracyThreshold; }
    public void setAccuracyThreshold(float accuracyThreshold) { 
        this.accuracyThreshold = accuracyThreshold; 
    }
    
    public long getTrackingInterval() { return trackingInterval; }
    public void setTrackingInterval(long trackingInterval) { 
        this.trackingInterval = trackingInterval; 
    }
    
    public boolean isTrackingEnabled() { return trackingEnabled; }
    public void setTrackingEnabled(boolean trackingEnabled) { 
        this.trackingEnabled = trackingEnabled; 
    }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}