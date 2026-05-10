package com.phonelogger.models;

public class ScreenLog {
    public long id;
    public String eventType; // SCREEN_ON, SCREEN_OFF, UNLOCKED
    public long timestamp;
    public int synced;

    public ScreenLog() {}

    public ScreenLog(String eventType, long timestamp) {
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.synced = 0;
    }
}
