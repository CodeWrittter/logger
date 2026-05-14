package com.usageinsights.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.usageinsights.db.LogDao;
import com.usageinsights.models.ErrorLog;
import com.usageinsights.models.ScreenLog;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("usageinsights", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("log_screen", true)) return;

        try {
            String action = intent.getAction();
            String eventType;
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                eventType = "SCREEN_ON";
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                eventType = "SCREEN_OFF";
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                eventType = "UNLOCKED";
            } else {
                return;
            }
            new LogDao(context).insertScreen(new ScreenLog(eventType, System.currentTimeMillis()));
        } catch (Exception e) {
            new LogDao(context).insertError(ErrorLog.from("SCREEN_CAPTURE_FAILED", e));
        }
    }
}
