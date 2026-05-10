package com.phonelogger.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.phonelogger.R;
import com.phonelogger.ui.WorkerScheduler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class LoggingFragment extends Fragment {

    private SharedPreferences prefs;

    private SwitchMaterial switchSms, switchCalls, switchNotifications;
    private TextView tvExcludeCount;

    private SwitchMaterial switchLocation;
    private TextView tvLocationInterval, tvLocationAccuracy;

    private SwitchMaterial switchScreen, switchBattery, switchSystem, switchAirplane, switchMobileData;

    private SwitchMaterial switchAppUsage, switchWifi, switchBluetooth;
    private TextView tvUsageInterval;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_logging, container, false);
        prefs = requireContext().getSharedPreferences("phonelogger", Context.MODE_PRIVATE);

        bindViews(root);
        loadState();
        setupListeners(root);

        return root;
    }

    // ─── Bind ─────────────────────────────────────────────────────────────────

    private void bindViews(View root) {
        switchSms           = root.findViewById(R.id.switch_sms);
        switchCalls         = root.findViewById(R.id.switch_calls);
        switchNotifications = root.findViewById(R.id.switch_notifications);
        tvExcludeCount      = root.findViewById(R.id.tv_exclude_count);

        switchLocation      = root.findViewById(R.id.switch_location);
        tvLocationInterval  = root.findViewById(R.id.tv_location_interval);
        tvLocationAccuracy  = root.findViewById(R.id.tv_location_accuracy);

        switchScreen        = root.findViewById(R.id.switch_screen);
        switchBattery       = root.findViewById(R.id.switch_battery);
        switchSystem        = root.findViewById(R.id.switch_system);
        switchAirplane      = root.findViewById(R.id.switch_airplane);
        switchMobileData    = root.findViewById(R.id.switch_mobile_data);

        switchAppUsage      = root.findViewById(R.id.switch_app_usage);
        tvUsageInterval     = root.findViewById(R.id.tv_usage_interval);
        switchWifi          = root.findViewById(R.id.switch_wifi);
        switchBluetooth     = root.findViewById(R.id.switch_bluetooth);
    }

    // ─── Load saved state ─────────────────────────────────────────────────────

    private void loadState() {
        switchSms.setChecked(prefs.getBoolean("log_sms", true));
        switchCalls.setChecked(prefs.getBoolean("log_calls", true));
        switchNotifications.setChecked(prefs.getBoolean("log_notifications", true));
        updateExcludeCount();

        switchLocation.setChecked(prefs.getBoolean("log_location", true));
        tvLocationInterval.setText(formatMinutes(prefs.getInt("location_interval_min", 60)));
        tvLocationAccuracy.setText(accuracyLabel(prefs.getInt("location_accuracy", 100)));

        switchScreen.setChecked(prefs.getBoolean("log_screen", true));
        switchBattery.setChecked(prefs.getBoolean("log_battery", true));
        switchSystem.setChecked(prefs.getBoolean("log_system", true));
        switchAirplane.setChecked(prefs.getBoolean("log_airplane", true));
        switchMobileData.setChecked(prefs.getBoolean("log_mobile_data", true));

        switchAppUsage.setChecked(prefs.getBoolean("log_app_usage", true));
        tvUsageInterval.setText(formatMinutes(prefs.getInt("usage_interval_min", 60)));
        switchWifi.setChecked(prefs.getBoolean("log_wifi", true));
        switchBluetooth.setChecked(prefs.getBoolean("log_bluetooth", true));
    }

    // ─── Setup listeners ──────────────────────────────────────────────────────

    private void setupListeners(View root) {
        switchSms.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean("log_sms", on).apply());
        switchCalls.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean("log_calls", on).apply());
        switchNotifications.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean("log_notifications", on).apply());
        root.findViewById(R.id.row_exclude_list).setOnClickListener(v -> showExcludeListDialog());

        switchLocation.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean("log_location", on).apply();
            if (on) WorkerScheduler.rescheduleLocation(requireContext(),
                    prefs.getInt("location_interval_min", 60));
        });
        root.findViewById(R.id.row_location_interval).setOnClickListener(v -> showLocationIntervalDialog());
        root.findViewById(R.id.row_location_accuracy).setOnClickListener(v -> showLocationAccuracyDialog());

        switchScreen.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean("log_screen", on).apply());
        switchBattery.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean("log_battery", on).apply());
        switchSystem.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean("log_system", on).apply());
        switchAirplane.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean("log_airplane", on).apply());
        switchMobileData.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean("log_mobile_data", on).apply());

        switchAppUsage.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean("log_app_usage", on).apply();
            if (on) WorkerScheduler.rescheduleUsageStats(requireContext(),
                    prefs.getInt("usage_interval_min", 60));
        });
        root.findViewById(R.id.row_usage_interval).setOnClickListener(v -> showUsageIntervalDialog());
        switchWifi.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean("log_wifi", on).apply());
        switchBluetooth.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean("log_bluetooth", on).apply());
    }

    // ─── Exclude list dialog ──────────────────────────────────────────────────

    private void showExcludeListDialog() {
        Set<String> excluded = new LinkedHashSet<>(
                prefs.getStringSet("notification_exclude_list", new HashSet<>()));
        String[] items = excluded.toArray(new String[0]);

        if (items.length == 0) {
            EditText et = new EditText(requireContext());
            et.setHint("com.example.app");
            new AlertDialog.Builder(requireContext())
                    .setTitle("Notification Exclude List")
                    .setMessage("No apps excluded. Enter a package name to add:")
                    .setView(et)
                    .setPositiveButton("Add", (d, w) -> {
                        String pkg = et.getText().toString().trim();
                        if (!pkg.isEmpty()) {
                            excluded.add(pkg);
                            prefs.edit().putStringSet("notification_exclude_list", excluded).apply();
                            updateExcludeCount();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        boolean[] checked = new boolean[items.length];
        Arrays.fill(checked, false);

        new AlertDialog.Builder(requireContext())
                .setTitle("Excluded Apps (" + items.length + ")")
                .setMultiChoiceItems(items, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Remove Selected", (d, w) -> {
                    for (int i = items.length - 1; i >= 0; i--) {
                        if (checked[i]) excluded.remove(items[i]);
                    }
                    prefs.edit().putStringSet("notification_exclude_list", excluded).apply();
                    updateExcludeCount();
                })
                .setNeutralButton("Add New", (d, w) -> {
                    EditText et = new EditText(requireContext());
                    et.setHint("com.example.app");
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Add Package")
                            .setView(et)
                            .setPositiveButton("Add", (d2, w2) -> {
                                String pkg = et.getText().toString().trim();
                                if (!pkg.isEmpty()) {
                                    excluded.add(pkg);
                                    prefs.edit().putStringSet("notification_exclude_list", excluded).apply();
                                    updateExcludeCount();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void updateExcludeCount() {
        int count = prefs.getStringSet("notification_exclude_list", new HashSet<>()).size();
        tvExcludeCount.setText(count == 0 ? "None" : count + " app" + (count == 1 ? "" : "s"));
    }

    // ─── Location interval dialog ─────────────────────────────────────────────

    private void showLocationIntervalDialog() {
        int[] options = {15, 30, 60, 120, 240};
        String[] labels = {"15 min", "30 min", "60 min", "2 hours", "4 hours"};
        int current = prefs.getInt("location_interval_min", 60);
        int selected = indexOfOrDefault(options, current, 2);

        new AlertDialog.Builder(requireContext())
                .setTitle("Location Interval")
                .setSingleChoiceItems(labels, selected, (d, which) -> {
                    int min = options[which];
                    prefs.edit().putInt("location_interval_min", min).apply();
                    tvLocationInterval.setText(formatMinutes(min));
                    if (prefs.getBoolean("log_location", true))
                        WorkerScheduler.rescheduleLocation(requireContext(), min);
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Location accuracy dialog ─────────────────────────────────────────────

    private void showLocationAccuracyDialog() {
        int[] options = {100, 102, 104};
        String[] labels = {"High", "Balanced", "Low Power"};
        int current = prefs.getInt("location_accuracy", 100);
        int selected = indexOfOrDefault(options, current, 0);

        new AlertDialog.Builder(requireContext())
                .setTitle("Location Accuracy")
                .setSingleChoiceItems(labels, selected, (d, which) -> {
                    prefs.edit().putInt("location_accuracy", options[which]).apply();
                    tvLocationAccuracy.setText(labels[which]);
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Usage interval dialog ────────────────────────────────────────────────

    private void showUsageIntervalDialog() {
        int[] options = {15, 30, 60, 120};
        String[] labels = {"15 min", "30 min", "60 min", "2 hours"};
        int current = prefs.getInt("usage_interval_min", 60);
        int selected = indexOfOrDefault(options, current, 2);

        new AlertDialog.Builder(requireContext())
                .setTitle("Usage Stats Interval")
                .setSingleChoiceItems(labels, selected, (d, which) -> {
                    int min = options[which];
                    prefs.edit().putInt("usage_interval_min", min).apply();
                    tvUsageInterval.setText(formatMinutes(min));
                    if (prefs.getBoolean("log_app_usage", true))
                        WorkerScheduler.rescheduleUsageStats(requireContext(), min);
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String formatMinutes(int min) {
        if (min < 60) return min + " min";
        int h = min / 60;
        int m = min % 60;
        return m == 0 ? h + " hour" + (h > 1 ? "s" : "") : h + "h " + m + "m";
    }

    private String accuracyLabel(int priority) {
        if (priority == 102) return "Balanced";
        if (priority == 104) return "Low Power";
        return "High";
    }

    private int indexOfOrDefault(int[] arr, int value, int def) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) return i;
        }
        return def;
    }
}
