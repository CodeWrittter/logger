package com.usageinsights.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// Volume button sequence is handled via VolumeAccessibilityService.
// This receiver exists as a manifest placeholder for future key-event broadcasts.
public class VolumeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {}
}
