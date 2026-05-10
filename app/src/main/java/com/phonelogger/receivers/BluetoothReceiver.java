package com.phonelogger.receivers;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.phonelogger.db.LogDao;
import com.phonelogger.models.ErrorLog;
import com.phonelogger.models.NetworkLog;

public class BluetoothReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("phonelogger", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("log_bluetooth", true)) return;

        try {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String name = device != null ? device.getName() : null;
            long now = System.currentTimeMillis();
            LogDao dao = new LogDao(context);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                dao.insertNetwork(new NetworkLog("BT_CONNECTED", name, now));
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                dao.insertNetwork(new NetworkLog("BT_DISCONNECTED", name, now));
            }
        } catch (Exception e) {
            new LogDao(context).insertError(ErrorLog.from("BT_CAPTURE_FAILED", e));
        }
    }
}
