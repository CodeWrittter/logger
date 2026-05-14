package com.usageinsights.models;

public class LocationLog {
    public long id;
    public Double latitude;
    public Double longitude;
    public Double accuracy;
    public String positionStatus; // OK or DISABLED
    public long timestamp;
    public int synced;

    public LocationLog() {}

    public LocationLog(Double latitude, Double longitude, Double accuracy, String positionStatus, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.positionStatus = positionStatus;
        this.timestamp = timestamp;
        this.synced = 0;
    }
}
