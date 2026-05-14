package com.usageinsights.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;

import com.usageinsights.db.LogDao;
import com.usageinsights.models.BatteryLog;
import com.usageinsights.models.ErrorLog;

public class BatteryReceiver extends BroadcastReceiver {

    private static int lastStatus = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("usageinsights", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("log_battery", true)) return;

        try {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;

            // Only log transitions, not every battery change tick
            if (lastStatus == -1) {
                lastStatus = status;
                return;
            }

            boolean wasCharging = lastStatus == BatteryManager.BATTERY_STATUS_CHARGING
                    || lastStatus == BatteryManager.BATTERY_STATUS_FULL;

            if (charging != wasCharging) {
                String eventType = charging ? "CHARGING_STARTED" : "CHARGING_STOPPED";
                new LogDao(context).insertBattery(
                        new BatteryLog(level, charging ? 1 : 0, eventType, System.currentTimeMillis()));
            }

            lastStatus = status;
        } catch (Exception e) {
            new LogDao(context).insertError(ErrorLog.from("BATTERY_CAPTURE_FAILED", e));
        }
    }
}
