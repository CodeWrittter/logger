package com.usage.insights.models;

public class AppUsageLog {
    public long id;
    public String packageName;
    public String appName;
    public int usageDurationSeconds;
    public long windowStart;
    public long windowEnd;
    public int synced;

    public AppUsageLog() {}

    public AppUsageLog(String packageName, String appName, int usageDurationSeconds, long windowStart, long windowEnd) {
        this.packageName = packageName;
        this.appName = appName;
        this.usageDurationSeconds = usageDurationSeconds;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.synced = 0;
    }
}
