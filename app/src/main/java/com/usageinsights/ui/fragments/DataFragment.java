package com.usageinsights.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.usageinsights.R;
import com.usageinsights.db.LogDao;
import com.usageinsights.network.SupabaseClient;

import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataFragment extends Fragment {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final NumberFormat numFmt = NumberFormat.getNumberInstance(Locale.getDefault());

    private TextView tvDbSize, tvDbRecords;
    private TextView tvRemoteSize, tvRemoteRecords, tvPing;
    private TextView tvNotifications, tvAppUsage, tvCalls, tvSms, tvLocation, tvErrors;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_data, container, false);

        bindViews(root);
        loadLocalStats();
        loadRemoteStats();
        setupListeners(root);

        return root;
    }

    // ─── Bind ─────────────────────────────────────────────────────────────────

    private void bindViews(View root) {
        tvDbSize        = root.findViewById(R.id.tv_db_size);
        tvDbRecords     = root.findViewById(R.id.tv_db_records);
        tvRemoteSize    = root.findViewById(R.id.tv_remote_size);
        tvRemoteRecords = root.findViewById(R.id.tv_remote_records);
        tvPing          = root.findViewById(R.id.tv_ping);
        tvNotifications = root.findViewById(R.id.tv_count_notifications);
        tvAppUsage      = root.findViewById(R.id.tv_count_app_usage);
        tvCalls         = root.findViewById(R.id.tv_count_calls);
        tvSms           = root.findViewById(R.id.tv_count_sms);
        tvLocation      = root.findViewById(R.id.tv_count_location);
        tvErrors        = root.findViewById(R.id.tv_count_errors);
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    private void setupListeners(View root) {
        root.findViewById(R.id.btn_view_records).setOnClickListener(v -> showRecordsDialog());
        root.findViewById(R.id.btn_export_json).setOnClickListener(v -> exportJson());
        root.findViewById(R.id.btn_wipe_db).setOnClickListener(v -> confirmWipe());
        root.findViewById(R.id.btn_download_all).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Not available offline", Toast.LENGTH_SHORT).show());
    }

    // ─── Local stats ──────────────────────────────────────────────────────────

    private void loadLocalStats() {
        executor.execute(() -> {
            Context ctx = requireContext().getApplicationContext();
            LogDao dao = new LogDao(ctx);
            long sizeBytes = dao.getDbSizeBytes(ctx);
            int total = dao.getTotalRecordCount();
            int notifications = dao.getCount("notification_logs");
            int appUsage      = dao.getCount("app_usage_logs");
            int calls         = dao.getCount("call_logs");
            int sms           = dao.getCount("sms_logs");
            int location      = dao.getCount("location_logs");
            int errors        = dao.getCount("error_logs");

            uiHandler.post(() -> {
                if (!isAdded()) return;
                tvDbSize.setText(formatBytes(sizeBytes));
                tvDbRecords.setText(numFmt.format(total));
                tvNotifications.setText(numFmt.format(notifications));
                tvAppUsage.setText(numFmt.format(appUsage));
                tvCalls.setText(numFmt.format(calls));
                tvSms.setText(numFmt.format(sms));
                tvLocation.setText(numFmt.format(location));
                tvErrors.setText(numFmt.format(errors));
            });
        });
    }

    // ─── Remote stats ─────────────────────────────────────────────────────────

    private void loadRemoteStats() {
        executor.execute(() -> {
            Context ctx = requireContext().getApplicationContext();
            SupabaseClient supabase = new SupabaseClient(ctx);

            long ping = supabase.ping();

            // Sum counts across the 10 tables
            String[] tables = {"call_logs", "sms_logs", "notification_logs", "location_logs",
                    "system_event_logs", "screen_logs", "battery_logs", "app_usage_logs",
                    "network_logs", "error_logs"};
            long remoteTotal = 0;
            boolean remoteOk = true;
            for (String table : tables) {
                long count = supabase.getRemoteCount(table);
                if (count < 0) { remoteOk = false; break; }
                remoteTotal += count;
            }

            long finalRemoteTotal = remoteTotal;
            boolean finalRemoteOk = remoteOk;
            uiHandler.post(() -> {
                if (!isAdded()) return;
                tvPing.setText(ping >= 0 ? ping + " ms" : "—");
                tvRemoteRecords.setText(finalRemoteOk ? numFmt.format(finalRemoteTotal) : "—");
                tvRemoteSize.setText("—");
            });
        });
    }

    // ─── View records dialog ──────────────────────────────────────────────────

    private void showRecordsDialog() {
        executor.execute(() -> {
            Context ctx = requireContext().getApplicationContext();
            LogDao dao = new LogDao(ctx);
            String[][] rows = {
                    {"Calls",         String.valueOf(dao.getCount("call_logs"))},
                    {"SMS",           String.valueOf(dao.getCount("sms_logs"))},
                    {"Notifications", String.valueOf(dao.getCount("notification_logs"))},
                    {"Location",      String.valueOf(dao.getCount("location_logs"))},
                    {"System Events", String.valueOf(dao.getCount("system_event_logs"))},
                    {"Screen",        String.valueOf(dao.getCount("screen_logs"))},
                    {"Battery",       String.valueOf(dao.getCount("battery_logs"))},
                    {"App Usage",     String.valueOf(dao.getCount("app_usage_logs"))},
                    {"Network",       String.valueOf(dao.getCount("network_logs"))},
                    {"Errors",        String.valueOf(dao.getCount("error_logs"))},
            };

            uiHandler.post(() -> {
                if (!isAdded()) return;
                StringBuilder sb = new StringBuilder();
                for (String[] row : rows) {
                    sb.append(String.format(Locale.getDefault(), "%-18s %s\n", row[0], row[1]));
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("Local Records")
                        .setMessage(sb.toString().trim())
                        .setPositiveButton("OK", null)
                        .show();
            });
        });
    }

    // ─── Export JSON ──────────────────────────────────────────────────────────

    private void exportJson() {
        executor.execute(() -> {
            try {
                Context ctx = requireContext().getApplicationContext();
                LogDao dao = new LogDao(ctx);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                Map<String, Object> data = new HashMap<>();
                data.put("calls",          dao.getUnsyncedCalls());
                data.put("sms",            dao.getUnsyncedSms());
                data.put("notifications",  dao.getUnsyncedNotifications());
                data.put("locations",      dao.getUnsyncedLocations());
                data.put("system_events",  dao.getUnsyncedSystemEvents());
                data.put("screen",         dao.getUnsyncedScreenLogs());
                data.put("battery",        dao.getUnsyncedBatteryLogs());
                data.put("app_usage",      dao.getUnsyncedAppUsage());
                data.put("network",        dao.getUnsyncedNetworkLogs());
                data.put("errors",         dao.getUnsyncedErrors());

                File outFile = new File(ctx.getCacheDir(), "usageinsights_export.json");
                try (FileWriter fw = new FileWriter(outFile)) {
                    gson.toJson(data, fw);
                }

                Uri uri = FileProvider.getUriForFile(ctx,
                        ctx.getPackageName() + ".fileprovider", outFile);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("application/json");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                uiHandler.post(() -> {
                    if (!isAdded()) return;
                    startActivity(Intent.createChooser(share, "Export logs"));
                });
            } catch (Exception e) {
                uiHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Export failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ─── Wipe ─────────────────────────────────────────────────────────────────

    private void confirmWipe() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Wipe Local DB")
                .setMessage("Delete all locally stored records? This cannot be undone.")
                .setPositiveButton("Wipe", (d, w) -> {
                    executor.execute(() -> {
                        new LogDao(requireContext().getApplicationContext()).wipeAll();
                        uiHandler.post(() -> {
                            if (!isAdded()) return;
                            loadLocalStats();
                            Toast.makeText(requireContext(), "Local database wiped",
                                    Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
