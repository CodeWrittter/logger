package com.usageinsights.services;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.usageinsights.db.LogDao;
import com.usageinsights.models.ErrorLog;
import com.usageinsights.models.NotificationLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NotificationLogger extends NotificationListenerService {

    private static final long DEDUP_WINDOW_MS = 2000;

    private SharedPreferences prefs;
    private final Map<String, Long> lastLoggedAt = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("usageinsights", MODE_PRIVATE);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (!prefs.getBoolean("log_notifications", true)) return;

            String pkg = sbn.getPackageName();

            Set<String> excludedApps = prefs.getStringSet("notification_exclude_list", null);
            if (excludedApps != null && excludedApps.contains(pkg)) return;

            // Skip if same notification key was logged within the dedup window
            String key = sbn.getKey();
            long now = System.currentTimeMillis();
            Long last = lastLoggedAt.get(key);
            if (last != null && now - last < DEDUP_WINDOW_MS) return;
            lastLoggedAt.put(key, now);

            android.os.Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(android.app.Notification.EXTRA_TITLE, "");
            CharSequence textSeq = extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
            String text = textSeq != null ? textSeq.toString() : "";

            String appName = getAppName(pkg);
            new LogDao(this).insertNotification(
                    new NotificationLog(pkg, appName, title, text, now));
        } catch (Exception e) {
            new LogDao(this).insertError(ErrorLog.from("NOTIFICATION_CAPTURE_FAILED", e));
        }
    }

    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }
}
