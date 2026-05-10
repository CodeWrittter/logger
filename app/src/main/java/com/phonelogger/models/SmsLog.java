package com.phonelogger.models;

public class SmsLog {
    public long id;
    public String sender;
    public String savedName;
    public String content;
    public long timestamp;
    public int synced;

    public SmsLog() {}

    public SmsLog(String sender, String savedName, String content, long timestamp) {
        this.sender = sender;
        this.savedName = savedName;
        this.content = content;
        this.timestamp = timestamp;
        this.synced = 0;
    }
}
