package com.phonelogger.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

import com.phonelogger.db.LogDao;
import com.phonelogger.models.CallLog;
import com.phonelogger.models.ErrorLog;
import com.phonelogger.utils.ContactUtils;

public class CallReceiver extends BroadcastReceiver {

    private static String lastNumber;
    private static long callStartTime;
    private static boolean callInProgress = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("phonelogger", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("log_calls", true)) return;

        try {
            String action = intent.getAction();
            if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
                lastNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                callStartTime = System.currentTimeMillis();
                callInProgress = true;
                return;
            }

            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (number != null) lastNumber = number;

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                callStartTime = System.currentTimeMillis();
                callInProgress = false;
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                callStartTime = System.currentTimeMillis();
                callInProgress = true;
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                if (lastNumber == null) return;
                long now = System.currentTimeMillis();
                int duration = callInProgress ? (int) ((now - callStartTime) / 1000) : 0;
                String type = callInProgress ? "INCOMING" : "MISSED";
                String savedName = ContactUtils.resolveName(context, lastNumber);
                new LogDao(context).insertCall(new CallLog(lastNumber, savedName, type, duration, now));
                callInProgress = false;
                lastNumber = null;
            }
        } catch (Exception e) {
            new LogDao(context).insertError(ErrorLog.from("CALL_CAPTURE_FAILED", e));
        }
    }
}
