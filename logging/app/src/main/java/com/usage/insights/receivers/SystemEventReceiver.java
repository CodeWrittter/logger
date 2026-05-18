package com.usage.insights.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.usage.insights.db.LogDao;
import com.usage.insights.models.ErrorLog;
import com.usage.insights.models.SystemEventLog;

public class SystemEventReceiver extends BroadcastReceiver {

    private static final String PREF_LAST_SHUTDOWN = "last_shutdown_ts";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("usageinsights", Context.MODE_PRIVATE);

        try {
            LogDao dao = new LogDao(context);
            long now = System.currentTimeMillis();
            String action = intent.getAction();

            if (Intent.ACTION_SHUTDOWN.equals(action)) {
                if (!prefs.getBoolean("log_system", true)) return;
                prefs.edit().putLong(PREF_LAST_SHUTDOWN, now).apply();
                dao.insertSystemEvent(new SystemEventLog("SHUTDOWN", null, now));

            } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                if (!prefs.getBoolean("log_system", true)) return;
                long lastShutdown = prefs.getLong(PREF_LAST_SHUTDOWN, 0);
                String detail = lastShutdown > 0
                        ? "downtime_seconds=" + ((now - lastShutdown) / 1000)
                        : null;
                dao.insertSystemEvent(new SystemEventLog("REBOOT", detail, now));

            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                if (!prefs.getBoolean("log_airplane", true)) return;
                boolean isOn = intent.getBooleanExtra("state", false);
                dao.insertSystemEvent(new SystemEventLog(isOn ? "AIRPLANE_ON" : "AIRPLANE_OFF", null, now));
            }
        } catch (Exception e) {
            new LogDao(context).insertError(ErrorLog.from("SYSTEM_EVENT_FAILED", e));
        }
    }
}
