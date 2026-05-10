package com.phonelogger.services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import com.phonelogger.ui.LockActivity;

public class ShakeDetectionService extends Service implements SensorEventListener {

    private static final float SHAKE_THRESHOLD_G = 2.7f;
    private static final int MIN_TIME_BETWEEN_SHAKES_MS = 1500;
    private static final int SHAKE_COUNT_REQUIRED = 3;

    private SensorManager sensorManager;
    private long lastShakeTime = 0;
    private int shakeCount = 0;
    private long firstShakeTime = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gForce = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;

        if (gForce > SHAKE_THRESHOLD_G) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime < MIN_TIME_BETWEEN_SHAKES_MS) return;

            if (shakeCount == 0 || now - firstShakeTime > 3000) {
                shakeCount = 1;
                firstShakeTime = now;
            } else {
                shakeCount++;
            }

            lastShakeTime = now;

            if (shakeCount >= SHAKE_COUNT_REQUIRED) {
                shakeCount = 0;
                Intent launch = new Intent(this, LockActivity.class);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
