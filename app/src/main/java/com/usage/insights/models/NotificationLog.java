package com.usage.insights.models;

public class NotificationLog {
    public long id;
    public String packageName;
    public String appName;
    public String title;
    public String content;
    public long timestamp;
    public int synced;

    public NotificationLog() {}

    public NotificationLog(String packageName, String appName, String title, String content, long timestamp) {
        this.packageName = packageName;
        this.appName = appName;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.synced = 0;
    }
}
