package com.phonelogger.models;

public class SystemEventLog {
    public long id;
    public String eventType; // SHUTDOWN, REBOOT, AIRPLANE_ON, AIRPLANE_OFF, DATA_ON, DATA_OFF
    public String detail;
    public long timestamp;
    public int synced;

    public SystemEventLog() {}

    public SystemEventLog(String eventType, String detail, long timestamp) {
        this.eventType = eventType;
        this.detail = detail;
        this.timestamp = timestamp;
        this.synced = 0;
    }
}
