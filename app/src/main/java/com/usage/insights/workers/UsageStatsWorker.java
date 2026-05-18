package com.usage.insights.workers;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.usage.insights.db.LogDao;
import com.usage.insights.models.AppUsageLog;
import com.usage.insights.models.ErrorLog;

import java.util.List;
import java.util.Map;

public class UsageStatsWorker extends Worker {

    public UsageStatsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences("usageinsights", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("log_app_usage", true)) return Result.success();

        LogDao dao = new LogDao(ctx);
        int intervalMin = prefs.getInt("usage_interval_min", 60);

        try {
            UsageStatsManager usm = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            long windowEnd = System.currentTimeMillis();
            long windowStart = windowEnd - ((long) intervalMin * 60 * 1000L);

            Map<String, UsageStats> statsMap = usm.queryAndAggregateUsageStats(windowStart, windowEnd);
            PackageManager pm = ctx.getPackageManager();

            for (Map.Entry<String, UsageStats> entry : statsMap.entrySet()) {
                long duration = entry.getValue().getTotalTimeInForeground();
                if (duration < 1000) continue; // skip under 1 second

                String pkg = entry.getKey();
                String appName = getAppName(pm, pkg);
                int seconds = (int) (duration / 1000);
                dao.insertAppUsage(new AppUsageLog(pkg, appName, seconds, windowStart, windowEnd));
            }

            return Result.success();
        } catch (Exception e) {
            dao.insertError(ErrorLog.from("USAGE_STATS_FAILED", e));
            return Result.retry();
        }
    }

    private String getAppName(PackageManager pm, String packageName) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }
}
