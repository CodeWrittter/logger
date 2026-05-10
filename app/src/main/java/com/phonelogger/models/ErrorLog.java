package com.phonelogger.models;

public class ErrorLog {
    public long id;
    public String errorType;
    public String message;
    public String stacktrace;
    public long timestamp;
    public int synced;

    public ErrorLog() {}

    public ErrorLog(String errorType, String message, String stacktrace, long timestamp) {
        this.errorType = errorType;
        this.message = message;
        this.stacktrace = stacktrace;
        this.timestamp = timestamp;
        this.synced = 0;
    }

    public static ErrorLog from(String errorType, Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement el : e.getStackTrace()) {
            sb.append(el.toString()).append('\n');
        }
        return new ErrorLog(errorType, e.getMessage(), sb.toString(), System.currentTimeMillis());
    }
}
