package com.phonelogger.workers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Tasks;
import com.phonelogger.db.LogDao;
import com.phonelogger.models.BatteryLog;
import com.phonelogger.models.ErrorLog;
import com.phonelogger.models.LocationLog;

public class LocationWorker extends Worker {

    public LocationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences("phonelogger", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("log_location", true)) return Result.success();

        LogDao dao = new LogDao(ctx);
        long now = System.currentTimeMillis();
        int accuracyPriority = prefs.getInt("location_accuracy", Priority.PRIORITY_HIGH_ACCURACY);

        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            boolean gpsEnabled = lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!gpsEnabled) {
                dao.insertLocation(new LocationLog(null, null, null, "DISABLED", now));
            } else {
                FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(ctx);
                android.location.Location loc = Tasks.await(
                        client.getCurrentLocation(accuracyPriority, null));
                if (loc != null) {
                    dao.insertLocation(new LocationLog(loc.getLatitude(), loc.getLongitude(),
                            (double) loc.getAccuracy(), "OK", now));
                } else {
                    dao.insertLocation(new LocationLog(null, null, null, "DISABLED", now));
                }
            }

            snapshotBattery(ctx, dao, now);
            return Result.success();
        } catch (Exception e) {
            dao.insertError(ErrorLog.from("LOCATION_CAPTURE_FAILED", e));
            return Result.retry();
        }
    }

    private void snapshotBattery(Context ctx, LogDao dao, long now) {
        try {
            Intent batteryStatus = ctx.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryStatus == null) return;
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
            dao.insertBattery(new BatteryLog(level, charging ? 1 : 0, "SNAPSHOT", now));
        } catch (Exception ignored) {}
    }
}
