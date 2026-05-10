package com.phonelogger.services;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.phonelogger.db.LogDao;
import com.phonelogger.models.ErrorLog;
import com.phonelogger.models.NotificationLog;

import java.util.Set;

public class NotificationLogger extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            SharedPreferences prefs = getSharedPreferences("phonelogger", MODE_PRIVATE);
            if (!prefs.getBoolean("log_notifications", true)) return;

            String pkg = sbn.getPackageName();

            Set<String> excludedApps = prefs.getStringSet("notification_exclude_list", null);
            if (excludedApps != null && excludedApps.contains(pkg)) return;

            android.os.Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(android.app.Notification.EXTRA_TITLE, "");
            CharSequence textSeq = extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
            String text = textSeq != null ? textSeq.toString() : "";

            String appName = getAppName(pkg);
            new LogDao(this).insertNotification(
                    new NotificationLog(pkg, appName, title, text, System.currentTimeMillis()));
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
