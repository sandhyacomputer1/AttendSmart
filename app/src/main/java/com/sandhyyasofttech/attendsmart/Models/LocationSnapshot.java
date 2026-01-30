package com.sandhyyasofttech.attendsmart.Models;

public class LocationSnapshot {
    private String timestamp;
    private double lat;
    private double lng;
    private float accuracy;
    private float distance;
    private boolean insideGeofence;

    public LocationSnapshot() {}

    public LocationSnapshot(String timestamp, double lat, double lng, 
                          float accuracy, float distance, boolean insideGeofence) {
        this.timestamp = timestamp;
        this.lat = lat;
        this.lng = lng;
        this.accuracy = accuracy;
        this.distance = distance;
        this.insideGeofence = insideGeofence;
    }

    // Getters and Setters
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
    
    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }
    
    public float getDistance() { return distance; }
    public void setDistance(float distance) { this.distance = distance; }
    
    public boolean isInsideGeofence() { return insideGeofence; }
    public void setInsideGeofence(boolean insideGeofence) { 
        this.insideGeofence = insideGeofence; 
    }
}