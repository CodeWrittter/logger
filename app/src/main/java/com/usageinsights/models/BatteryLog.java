package com.usageinsights.models;

public class BatteryLog {
    public long id;
    public int level;
    public int isCharging; // 0 or 1
    public String eventType; // SNAPSHOT, CHARGING_STARTED, CHARGING_STOPPED
    public long timestamp;
    public int synced;

    public BatteryLog() {}

    public BatteryLog(int level, int isCharging, String eventType, long timestamp) {
        this.level = level;
        this.isCharging = isCharging;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.synced = 0;
    }
}
