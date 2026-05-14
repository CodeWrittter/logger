package com.usageinsights.models;

public class CallLog {
    public long id;
    public String number;
    public String savedName;
    public String callType; // INCOMING, OUTGOING, MISSED
    public int durationSeconds;
    public long timestamp;
    public int synced;

    public CallLog() {}

    public CallLog(String number, String savedName, String callType, int durationSeconds, long timestamp) {
        this.number = number;
        this.savedName = savedName;
        this.callType = callType;
        this.durationSeconds = durationSeconds;
        this.timestamp = timestamp;
        this.synced = 0;
    }
}
