package com.phonelogger.ui;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.phonelogger.workers.LocationWorker;
import com.phonelogger.workers.SyncWorker;
import com.phonelogger.workers.UsageStatsWorker;

import java.util.concurrent.TimeUnit;

public class WorkerScheduler {

    public static void scheduleAll(Context context) {
        WorkManager wm = WorkManager.getInstance(context);

        PeriodicWorkRequest locationRequest = new PeriodicWorkRequest.Builder(
                LocationWorker.class, 60, TimeUnit.MINUTES)
                .build();
        wm.enqueueUniquePeriodicWork("location_logs",
                ExistingPeriodicWorkPolicy.KEEP, locationRequest);

        PeriodicWorkRequest usageRequest = new PeriodicWorkRequest.Builder(
                UsageStatsWorker.class, 60, TimeUnit.MINUTES)
                .build();
        wm.enqueueUniquePeriodicWork("usage_logs",
                ExistingPeriodicWorkPolicy.KEEP, usageRequest);

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 12, TimeUnit.HOURS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build();
        wm.enqueueUniquePeriodicWork("sync_logs",
                ExistingPeriodicWorkPolicy.KEEP, syncRequest);
    }

    public static void rescheduleSync(Context context, int intervalHours) {
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelUniqueWork("sync_logs");

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class, intervalHours, TimeUnit.HOURS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build();
        wm.enqueueUniquePeriodicWork("sync_logs",
                ExistingPeriodicWorkPolicy.REPLACE, syncRequest);
    }

    public static void rescheduleLocation(Context context, int intervalMinutes) {
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelUniqueWork("location_logs");

        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                LocationWorker.class, intervalMinutes, TimeUnit.MINUTES)
                .build();
        wm.enqueueUniquePeriodicWork("location_logs",
                ExistingPeriodicWorkPolicy.REPLACE, req);
    }

    public static void rescheduleUsageStats(Context context, int intervalMinutes) {
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelUniqueWork("usage_logs");

        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                UsageStatsWorker.class, intervalMinutes, TimeUnit.MINUTES)
                .build();
        wm.enqueueUniquePeriodicWork("usage_logs",
                ExistingPeriodicWorkPolicy.REPLACE, req);
    }
}
