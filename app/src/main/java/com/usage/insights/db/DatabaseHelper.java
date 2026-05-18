package com.usage.insights.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "usageinsights.db";
    private static final int DB_VERSION = 1;

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE call_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "number TEXT," +
                "saved_name TEXT," +
                "call_type TEXT," +
                "duration_seconds INTEGER," +
                "timestamp INTEGER," +
                "synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE sms_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sender TEXT," +
                "saved_name TEXT," +
                "content TEXT," +
                "timestamp INTEGER," +
                "synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE notification_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "package_name TEXT," +
                "app_name TEXT," +
                "title TEXT," +
                "content TEXT," +
                "timestamp INTEGER," +
                "synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE location_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "latitude REAL," +
                "longitude REAL," +
                "accuracy REAL," +
                "position_status TEXT," +
                "timestamp INTEGER," +
                "synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE system_event_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "event_type TEXT," +
                "detail TEXT," +
                "timestamp INTEGER," +
                "synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE screen_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "event_type TEXT," +
                "timestamp INTEGER," +
                "synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE battery_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "level INTEGER," +
                "is_charging INTEGER," +
                "event_type TEXT," +
                "timestamp INTEGER," +
                "synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE app_usage_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "package_name TEXT," +
                "app_name TEXT," +
                "usage_duration_seconds INTEGER," +
                "window_start INTEGER," +
                "window_end INTEGER," +
                "synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE network_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "event_type TEXT," +
                "detail TEXT," +
                "timestamp INTEGER," +
                "synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE error_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "error_type TEXT," +
                "message TEXT," +
                "stacktrace TEXT," +
                "timestamp INTEGER," +
                "synced INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future migrations here
    }
}
