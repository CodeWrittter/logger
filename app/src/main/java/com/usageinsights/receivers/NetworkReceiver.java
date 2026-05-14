package com.usageinsights.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.usageinsights.db.LogDao;
import com.usageinsights.models.ErrorLog;
import com.usageinsights.models.NetworkLog;

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("usageinsights", Context.MODE_PRIVATE);
        boolean logWifi   = prefs.getBoolean("log_wifi", true);
        boolean logMobile = prefs.getBoolean("log_mobile_data", true);
        if (!logWifi && !logMobile) return;

        try {
            LogDao dao = new LogDao(context);
            long now = System.currentTimeMillis();
            String action = intent.getAction();

            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

                if (logWifi && wifi != null) {
                    if (wifi.isConnected()) {
                        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wi = wm.getConnectionInfo();
                        String ssid = wi != null ? wi.getSSID() : null;
                        dao.insertNetwork(new NetworkLog("WIFI_CONNECTED", ssid, now));
                    } else if (!wifi.isConnectedOrConnecting()) {
                        dao.insertNetwork(new NetworkLog("WIFI_DISCONNECTED", null, now));
                    }
                }

                if (logMobile && mobile != null) {
                    if (mobile.isConnected()) {
                        dao.insertNetwork(new NetworkLog("DATA_ENABLED", null, now));
                    }
                }
            }
        } catch (Exception e) {
            new LogDao(context).insertError(ErrorLog.from("NETWORK_CAPTURE_FAILED", e));
        }
    }
}
