package com.phonelogger.workers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.phonelogger.db.LogDao;
import com.phonelogger.models.ErrorLog;
import com.phonelogger.network.SupabaseClient;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        LogDao dao = new LogDao(ctx);
        SupabaseClient supabase = new SupabaseClient(ctx);

        try {
            // Count all unsynced rows before pushing (for the UI counter)
            int totalRecords = dao.getTotalRecordCount();

            boolean ok =
                    supabase.insert("call_logs", dao.getUnsyncedCalls()) &&
                    supabase.insert("sms_logs", dao.getUnsyncedSms()) &&
                    supabase.insert("notification_logs", dao.getUnsyncedNotifications()) &&
                    supabase.insert("location_logs", dao.getUnsyncedLocations()) &&
                    supabase.insert("system_event_logs", dao.getUnsyncedSystemEvents()) &&
                    supabase.insert("screen_logs", dao.getUnsyncedScreenLogs()) &&
                    supabase.insert("battery_logs", dao.getUnsyncedBatteryLogs()) &&
                    supabase.insert("app_usage_logs", dao.getUnsyncedAppUsage()) &&
                    supabase.insert("network_logs", dao.getUnsyncedNetworkLogs()) &&
                    supabase.insert("error_logs", dao.getUnsyncedErrors());

            if (ok) {
                dao.wipeAll();
                SharedPreferences prefs = ctx.getSharedPreferences("phonelogger", Context.MODE_PRIVATE);
                prefs.edit()
                        .putLong("last_sync_ts", System.currentTimeMillis())
                        .putInt("last_sync_records", totalRecords)
                        .apply();
                return Result.success();
            } else {
                dao.insertError(new ErrorLog("SYNC_FAILED", "Supabase returned non-2xx", null, System.currentTimeMillis()));
                return Result.retry();
            }
        } catch (Exception e) {
            dao.insertError(ErrorLog.from("SYNC_FAILED", e));
            return Result.retry();
        }
    }
}
