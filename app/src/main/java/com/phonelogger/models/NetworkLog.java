package com.phonelogger.models;

public class NetworkLog {
    public long id;
    public String eventType; // WIFI_CONNECTED, WIFI_DISCONNECTED, DATA_ENABLED, DATA_DISABLED, BT_CONNECTED, BT_DISCONNECTED
    public String detail; // SSID or Bluetooth device name
    public long timestamp;
    public int synced;

    public NetworkLog() {}

    public NetworkLog(String eventType, String detail, long timestamp) {
        this.eventType = eventType;
        this.detail = detail;
        this.timestamp = timestamp;
        this.synced = 0;
    }
}
