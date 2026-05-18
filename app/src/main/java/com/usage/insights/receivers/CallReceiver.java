package com.usage.insights.receivers;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.telephony.TelephonyManager;

import com.usage.insights.db.LogDao;
import com.usage.insights.models.CallLog;
import com.usage.insights.models.ErrorLog;
import com.usage.insights.utils.ContactUtils;

public class CallReceiver extends BroadcastReceiver {

    private static String lastNumber;
    private static long callStartTime;
    private static boolean callInProgress = false;
    private static boolean wasRinging = false;
    private static boolean isOutgoing = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("usageinsights", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("log_calls", true)) return;

        try {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (number != null) lastNumber = number;

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                wasRinging = true;
                callInProgress = false;
                isOutgoing = false;
                callStartTime = System.currentTimeMillis();

            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                if (!wasRinging) {
                    // OFFHOOK without prior RINGING = outgoing call
                    isOutgoing = true;
                }
                callStartTime = System.currentTimeMillis();
                callInProgress = true;

            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                if (!callInProgress && !wasRinging) return;

                long now = System.currentTimeMillis();
                int duration = callInProgress ? (int) ((now - callStartTime) / 1000) : 0;
                String type;
                if (isOutgoing) {
                    type = "OUTGOING";
                    if (lastNumber == null) lastNumber = queryLastOutgoingNumber(context);
                } else if (callInProgress) {
                    type = "INCOMING";
                } else {
                    type = "MISSED";
                }

                if (lastNumber == null) lastNumber = "unknown";
                String savedName = ContactUtils.resolveName(context, lastNumber);
                new LogDao(context).insertCall(new CallLog(lastNumber, savedName, type, duration, now));

                callInProgress = false;
                wasRinging = false;
                isOutgoing = false;
                lastNumber = null;
            }
        } catch (Exception e) {
            new LogDao(context).insertError(ErrorLog.from("CALL_CAPTURE_FAILED", e));
        }
    }

    @SuppressLint("MissingPermission")
    private String queryLastOutgoingNumber(Context context) {
        try (Cursor c = context.getContentResolver().query(
                android.provider.CallLog.Calls.CONTENT_URI,
                new String[]{android.provider.CallLog.Calls.NUMBER},
                android.provider.CallLog.Calls.TYPE + "=?",
                new String[]{String.valueOf(android.provider.CallLog.Calls.OUTGOING_TYPE)},
                android.provider.CallLog.Calls.DATE + " DESC")) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {}
        return null;
    }
}
