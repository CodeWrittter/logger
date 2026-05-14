package com.usageinsights.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.usageinsights.R;
import com.usageinsights.db.LogDao;
import com.usageinsights.models.ErrorLog;
import com.usageinsights.network.SupabaseClient;
import com.usageinsights.ui.WorkerScheduler;
import com.usageinsights.workers.SyncWorker;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncFragment extends Fragment {

    private SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    // Connection
    private TextView tvBadge, tvPingAction;
    // Schedule
    private ImageView check12h, check24h, checkManual;
    // Last sync
    private TextView tvLastSync, tvRecords, tvNextSync;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_sync, container, false);
        prefs = requireContext().getSharedPreferences("usageinsights", Context.MODE_PRIVATE);

        bindViews(root);
        loadConnectionBadge();
        loadSchedule();
        loadLastSyncInfo();
        setupListeners(root);

        return root;
    }

    // ─── Bind ─────────────────────────────────────────────────────────────────

    private void bindViews(View root) {
        tvBadge      = root.findViewById(R.id.tv_connection_badge);
        tvPingAction = root.findViewById(R.id.tv_ping_action);
        check12h     = root.findViewById(R.id.check_12h);
        check24h     = root.findViewById(R.id.check_24h);
        checkManual  = root.findViewById(R.id.check_manual);
        tvLastSync   = root.findViewById(R.id.tv_last_sync);
        tvRecords    = root.findViewById(R.id.tv_records_uploaded);
        tvNextSync   = root.findViewById(R.id.tv_next_sync);
    }

    // ─── Connection badge ─────────────────────────────────────────────────────

    private void loadConnectionBadge() {
        int status = prefs.getInt("connection_status", 0); // 0=untested, 1=ok, -1=failed
        applyBadge(status);
    }

    private void applyBadge(int status) {
        if (status == 1) {
            tvBadge.setText("Connected");
            tvBadge.setTextColor(0xFF34C759);
            tvBadge.setBackgroundResource(R.drawable.badge_green_bg);
        } else if (status == -1) {
            tvBadge.setText("Failed");
            tvBadge.setTextColor(0xFFFF3B30);
            tvBadge.setBackgroundResource(R.drawable.badge_red_bg);
        } else {
            tvBadge.setText("Not Tested");
            tvBadge.setTextColor(0xFF8E8E93);
            tvBadge.setBackgroundResource(R.drawable.badge_gray_bg);
        }
    }

    // ─── Schedule ────────────────────────────────────────────────────────────

    private void loadSchedule() {
        int hours = prefs.getInt("sync_interval_hours", 12);
        applyScheduleCheck(hours);
    }

    private void applyScheduleCheck(int hours) {
        check12h.setVisibility(hours == 12 ? View.VISIBLE : View.GONE);
        check24h.setVisibility(hours == 24 ? View.VISIBLE : View.GONE);
        checkManual.setVisibility(hours == 0 ? View.VISIBLE : View.GONE);
    }

    private void selectSchedule(int hours) {
        prefs.edit().putInt("sync_interval_hours", hours).apply();
        applyScheduleCheck(hours);
        if (hours > 0) {
            WorkerScheduler.rescheduleSync(requireContext(), hours);
        } else {
            WorkManager.getInstance(requireContext()).cancelUniqueWork("sync_logs");
        }
        updateNextSyncLabel();
    }

    // ─── Last sync info ───────────────────────────────────────────────────────

    private void loadLastSyncInfo() {
        long lastTs = prefs.getLong("last_sync_ts", 0);
        int records = prefs.getInt("last_sync_records", 0);

        tvLastSync.setText(lastTs > 0 ? formatTimestamp(lastTs) : "Never");
        tvRecords.setText(NumberFormat.getNumberInstance(Locale.getDefault()).format(records));
        updateNextSyncLabel();
    }

    private void updateNextSyncLabel() {
        int hours = prefs.getInt("sync_interval_hours", 12);
        if (hours == 0) {
            tvNextSync.setText("Manual only");
            return;
        }
        long lastTs = prefs.getLong("last_sync_ts", 0);
        if (lastTs == 0) {
            tvNextSync.setText("Soon");
            return;
        }
        long nextTs = lastTs + (long) hours * 3600 * 1000;
        long remaining = nextTs - System.currentTimeMillis();
        if (remaining <= 0) {
            tvNextSync.setText("Soon");
        } else {
            long h = remaining / 3600000;
            long m = (remaining % 3600000) / 60000;
            tvNextSync.setText(h > 0 ? "In " + h + "h " + m + "m" : "In " + m + "m");
        }
    }

    private String formatTimestamp(long ts) {
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(ts);
        Calendar now = Calendar.getInstance();
        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(ts));
        if (then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                && then.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            return "Today, " + time;
        }
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (then.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
                && then.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)) {
            return "Yesterday, " + time;
        }
        String date = new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(ts));
        return date + ", " + time;
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    private void setupListeners(View root) {
        root.findViewById(R.id.btn_ping).setOnClickListener(v -> pingSupabase());
        root.findViewById(R.id.row_12h).setOnClickListener(v -> selectSchedule(12));
        root.findViewById(R.id.row_24h).setOnClickListener(v -> selectSchedule(24));
        root.findViewById(R.id.row_manual).setOnClickListener(v -> selectSchedule(0));
        root.findViewById(R.id.btn_view_log).setOnClickListener(v -> showSyncLog());
        root.findViewById(R.id.btn_sync_now).setOnClickListener(v -> syncNow());
    }

    // ─── Ping ────────────────────────────────────────────────────────────────

    private void pingSupabase() {
        tvPingAction.setText("…");
        tvPingAction.setTextColor(0xFF8E8E93);
        applyBadge(0);

        executor.execute(() -> {
            int code = new SupabaseClient(requireContext()).ping();
            boolean ok = code >= 200 && code < 300;
            String msg;
            if (code == 0)        msg = "Network error — no connection";
            else if (ok)          msg = "Connected (" + code + ")";
            else if (code == 401) msg = "Invalid credentials (401)";
            else if (code == 403) msg = "Access denied (403) — check RLS";
            else if (code == 404) msg = "Wrong project URL (404)";
            else                  msg = "Failed (HTTP " + code + ")";

            uiHandler.post(() -> {
                if (!isAdded()) return;
                prefs.edit().putInt("connection_status", ok ? 1 : -1).apply();
                applyBadge(ok ? 1 : -1);
                tvPingAction.setText("Ping");
                tvPingAction.setTextColor(getResources().getColor(R.color.blue_accent, null));
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            });
        });
    }

    // ─── Sync now ─────────────────────────────────────────────────────────────

    private void syncNow() {
        TextView action = requireView().findViewById(R.id.tv_sync_now_action);
        action.setText("Running…");
        action.setTextColor(0xFF8E8E93);

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(requireContext()).enqueue(req);

        // Observe the work — simplified: just re-enable button after a delay
        uiHandler.postDelayed(() -> {
            if (!isAdded()) return;
            action.setText("Run");
            action.setTextColor(getResources().getColor(R.color.blue_accent, null));
            loadLastSyncInfo(); // refresh stats
            Toast.makeText(requireContext(), "Sync queued", Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    // ─── Sync log dialog ──────────────────────────────────────────────────────

    private void showSyncLog() {
        executor.execute(() -> {
            List<ErrorLog> errors = new LogDao(requireContext()).getUnsyncedErrors();
            uiHandler.post(() -> {
                if (!isAdded()) return;
                if (errors.isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Sync Log")
                            .setMessage("No errors recorded.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm:ss", Locale.getDefault());
                StringBuilder sb = new StringBuilder();
                for (ErrorLog e : errors) {
                    sb.append(sdf.format(new Date(e.timestamp)))
                      .append(" - [").append(e.errorType).append("] ")
                      .append(e.message).append("\n\n");
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("Sync Log (" + errors.size() + " errors)")
                        .setMessage(sb.toString().trim())
                        .setPositiveButton("OK", null)
                        .show();
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
