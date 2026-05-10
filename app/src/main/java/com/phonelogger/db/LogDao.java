package com.phonelogger.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.phonelogger.models.*;

import java.util.ArrayList;
import java.util.List;

public class LogDao {

    private final DatabaseHelper helper;

    public LogDao(Context context) {
        this.helper = DatabaseHelper.getInstance(context);
    }

    // ─── Insert methods ───────────────────────────────────────────────────────

    public void insertCall(CallLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("number", log.number);
        v.put("saved_name", log.savedName);
        v.put("call_type", log.callType);
        v.put("duration_seconds", log.durationSeconds);
        v.put("timestamp", log.timestamp);
        v.put("synced", 0);
        db.insert("call_logs", null, v);
    }

    public void insertSms(SmsLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("sender", log.sender);
        v.put("saved_name", log.savedName);
        v.put("content", log.content);
        v.put("timestamp", log.timestamp);
        v.put("synced", 0);
        db.insert("sms_logs", null, v);
    }

    public void insertNotification(NotificationLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("package_name", log.packageName);
        v.put("app_name", log.appName);
        v.put("title", log.title);
        v.put("content", log.content);
        v.put("timestamp", log.timestamp);
        v.put("synced", 0);
        db.insert("notification_logs", null, v);
    }

    public void insertLocation(LocationLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        if (log.latitude != null) v.put("latitude", log.latitude);
        if (log.longitude != null) v.put("longitude", log.longitude);
        if (log.accuracy != null) v.put("accuracy", log.accuracy);
        v.put("position_status", log.positionStatus);
        v.put("timestamp", log.timestamp);
        v.put("synced", 0);
        db.insert("location_logs", null, v);
    }

    public void insertSystemEvent(SystemEventLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("event_type", log.eventType);
        v.put("detail", log.detail);
        v.put("timestamp", log.timestamp);
        v.put("synced", 0);
        db.insert("system_event_logs", null, v);
    }

    public void insertScreen(ScreenLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("event_type", log.eventType);
        v.put("timestamp", log.timestamp);
        v.put("synced", 0);
        db.insert("screen_logs", null, v);
    }

    public void insertBattery(BatteryLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("level", log.level);
        v.put("is_charging", log.isCharging);
        v.put("event_type", log.eventType);
        v.put("timestamp", log.timestamp);
        v.put("synced", 0);
        db.insert("battery_logs", null, v);
    }

    public void insertAppUsage(AppUsageLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("package_name", log.packageName);
        v.put("app_name", log.appName);
        v.put("usage_duration_seconds", log.usageDurationSeconds);
        v.put("window_start", log.windowStart);
        v.put("window_end", log.windowEnd);
        v.put("synced", 0);
        db.insert("app_usage_logs", null, v);
    }

    public void insertNetwork(NetworkLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("event_type", log.eventType);
        v.put("detail", log.detail);
        v.put("timestamp", log.timestamp);
        v.put("synced", 0);
        db.insert("network_logs", null, v);
    }

    public void insertError(ErrorLog log) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("error_type", log.errorType);
        v.put("message", log.message);
        v.put("stacktrace", log.stacktrace);
        v.put("timestamp", log.timestamp);
        v.put("synced", 0);
        db.insert("error_logs", null, v);
    }

    // ─── Query unsynced ───────────────────────────────────────────────────────

    public List<CallLog> getUnsyncedCalls() {
        List<CallLog> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM call_logs WHERE synced=0", null);
        while (c.moveToNext()) {
            CallLog log = new CallLog();
            log.id = c.getLong(c.getColumnIndexOrThrow("id"));
            log.number = c.getString(c.getColumnIndexOrThrow("number"));
            log.savedName = c.getString(c.getColumnIndexOrThrow("saved_name"));
            log.callType = c.getString(c.getColumnIndexOrThrow("call_type"));
            log.durationSeconds = c.getInt(c.getColumnIndexOrThrow("duration_seconds"));
            log.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            list.add(log);
        }
        c.close();
        return list;
    }

    public List<SmsLog> getUnsyncedSms() {
        List<SmsLog> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM sms_logs WHERE synced=0", null);
        while (c.moveToNext()) {
            SmsLog log = new SmsLog();
            log.id = c.getLong(c.getColumnIndexOrThrow("id"));
            log.sender = c.getString(c.getColumnIndexOrThrow("sender"));
            log.savedName = c.getString(c.getColumnIndexOrThrow("saved_name"));
            log.content = c.getString(c.getColumnIndexOrThrow("content"));
            log.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            list.add(log);
        }
        c.close();
        return list;
    }

    public List<NotificationLog> getUnsyncedNotifications() {
        List<NotificationLog> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM notification_logs WHERE synced=0", null);
        while (c.moveToNext()) {
            NotificationLog log = new NotificationLog();
            log.id = c.getLong(c.getColumnIndexOrThrow("id"));
            log.packageName = c.getString(c.getColumnIndexOrThrow("package_name"));
            log.appName = c.getString(c.getColumnIndexOrThrow("app_name"));
            log.title = c.getString(c.getColumnIndexOrThrow("title"));
            log.content = c.getString(c.getColumnIndexOrThrow("content"));
            log.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            list.add(log);
        }
        c.close();
        return list;
    }

    public List<LocationLog> getUnsyncedLocations() {
        List<LocationLog> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM location_logs WHERE synced=0", null);
        while (c.moveToNext()) {
            LocationLog log = new LocationLog();
            log.id = c.getLong(c.getColumnIndexOrThrow("id"));
            int latIdx = c.getColumnIndexOrThrow("latitude");
            int lngIdx = c.getColumnIndexOrThrow("longitude");
            int accIdx = c.getColumnIndexOrThrow("accuracy");
            log.latitude = c.isNull(latIdx) ? null : c.getDouble(latIdx);
            log.longitude = c.isNull(lngIdx) ? null : c.getDouble(lngIdx);
            log.accuracy = c.isNull(accIdx) ? null : c.getDouble(accIdx);
            log.positionStatus = c.getString(c.getColumnIndexOrThrow("position_status"));
            log.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            list.add(log);
        }
        c.close();
        return list;
    }

    public List<SystemEventLog> getUnsyncedSystemEvents() {
        List<SystemEventLog> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM system_event_logs WHERE synced=0", null);
        while (c.moveToNext()) {
            SystemEventLog log = new SystemEventLog();
            log.id = c.getLong(c.getColumnIndexOrThrow("id"));
            log.eventType = c.getString(c.getColumnIndexOrThrow("event_type"));
            log.detail = c.getString(c.getColumnIndexOrThrow("detail"));
            log.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            list.add(log);
        }
        c.close();
        return list;
    }

    public List<ScreenLog> getUnsyncedScreenLogs() {
        List<ScreenLog> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM screen_logs WHERE synced=0", null);
        while (c.moveToNext()) {
            ScreenLog log = new ScreenLog();
            log.id = c.getLong(c.getColumnIndexOrThrow("id"));
            log.eventType = c.getString(c.getColumnIndexOrThrow("event_type"));
            log.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            list.add(log);
        }
        c.close();
        return list;
    }

    public List<BatteryLog> getUnsyncedBatteryLogs() {
        List<BatteryLog> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM battery_logs WHERE synced=0", null);
        while (c.moveToNext()) {
            BatteryLog log = new BatteryLog();
            log.id = c.getLong(c.getColumnIndexOrThrow("id"));
            log.level = c.getInt(c.getColumnIndexOrThrow("level"));
            log.isCharging = c.getInt(c.getColumnIndexOrThrow("is_charging"));
            log.eventType = c.getString(c.getColumnIndexOrThrow("event_type"));
            log.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            list.add(log);
        }
        c.close();
        return list;
    }

    public List<AppUsageLog> getUnsyncedAppUsage() {
        List<AppUsageLog> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM app_usage_logs WHERE synced=0", null);
        while (c.moveToNext()) {
            AppUsageLog log = new AppUsageLog();
            log.id = c.getLong(c.getColumnIndexOrThrow("id"));
            log.packageName = c.getString(c.getColumnIndexOrThrow("package_name"));
            log.appName = c.getString(c.getColumnIndexOrThrow("app_name"));
            log.usageDurationSeconds = c.getInt(c.getColumnIndexOrThrow("usage_duration_seconds"));
            log.windowStart = c.getLong(c.getColumnIndexOrThrow("window_start"));
            log.windowEnd = c.getLong(c.getColumnIndexOrThrow("window_end"));
            list.add(log);
        }
        c.close();
        return list;
    }

    public List<NetworkLog> getUnsyncedNetworkLogs() {
        List<NetworkLog> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM network_logs WHERE synced=0", null);
        while (c.moveToNext()) {
            NetworkLog log = new NetworkLog();
            log.id = c.getLong(c.getColumnIndexOrThrow("id"));
            log.eventType = c.getString(c.getColumnIndexOrThrow("event_type"));
            log.detail = c.getString(c.getColumnIndexOrThrow("detail"));
            log.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            list.add(log);
        }
        c.close();
        return list;
    }

    public List<ErrorLog> getUnsyncedErrors() {
        List<ErrorLog> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM error_logs WHERE synced=0", null);
        while (c.moveToNext()) {
            ErrorLog log = new ErrorLog();
            log.id = c.getLong(c.getColumnIndexOrThrow("id"));
            log.errorType = c.getString(c.getColumnIndexOrThrow("error_type"));
            log.message = c.getString(c.getColumnIndexOrThrow("message"));
            log.stacktrace = c.getString(c.getColumnIndexOrThrow("stacktrace"));
            log.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            list.add(log);
        }
        c.close();
        return list;
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    public int getTotalRecordCount() {
        String[] tables = {"call_logs", "sms_logs", "notification_logs", "location_logs",
                "system_event_logs", "screen_logs", "battery_logs", "app_usage_logs",
                "network_logs", "error_logs"};
        SQLiteDatabase db = helper.getReadableDatabase();
        int total = 0;
        for (String table : tables) {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
            if (c.moveToFirst()) total += c.getInt(0);
            c.close();
        }
        return total;
    }

    // ─── Wipe ─────────────────────────────────────────────────────────────────

    public void wipeAll() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            String[] tables = {"call_logs", "sms_logs", "notification_logs", "location_logs",
                    "system_event_logs", "screen_logs", "battery_logs", "app_usage_logs",
                    "network_logs", "error_logs"};
            for (String table : tables) {
                db.delete(table, null, null);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
