package com.usage.insights.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.usage.insights.ui.LockActivity;

public class SecretCodeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent launch = new Intent(context, LockActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
    }
}
