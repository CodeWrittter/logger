package com.usageinsights.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.usageinsights.db.LogDao;
import com.usageinsights.models.ErrorLog;
import com.usageinsights.models.SmsLog;
import com.usageinsights.utils.ContactUtils;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("usageinsights", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("log_sms", true)) return;

        try {
            Bundle bundle = intent.getExtras();
            if (bundle == null) return;
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null) return;

            String format = bundle.getString("format");
            LogDao dao = new LogDao(context);
            long now = System.currentTimeMillis();

            for (Object pdu : pdus) {
                SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu, format);
                String sender = msg.getDisplayOriginatingAddress();
                String savedName = ContactUtils.resolveName(context, sender);
                String body = msg.getDisplayMessageBody();
                dao.insertSms(new SmsLog(sender, savedName, body, now));
            }
        } catch (Exception e) {
            new LogDao(context).insertError(ErrorLog.from("SMS_CAPTURE_FAILED", e));
        }
    }
}
