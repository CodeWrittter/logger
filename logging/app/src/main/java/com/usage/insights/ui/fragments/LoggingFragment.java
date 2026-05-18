package com.usage.insights.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.usage.insights.R;
import com.usage.insights.ui.WorkerScheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        prefs = requireContext().getSharedPreferences("usageinsights", Context.MODE_PRIVATE);

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
        new Thread(() -> {
            PackageManager pm = requireContext().getPackageManager();
            List<ApplicationInfo> all = pm.getInstalledApplications(0);

            List<ApplicationInfo> launchable = new ArrayList<>();
            for (ApplicationInfo app : all) {
                if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                    launchable.add(app);
                }
            }
            launchable.sort((a, b) ->
                    pm.getApplicationLabel(a).toString()
                      .compareToIgnoreCase(pm.getApplicationLabel(b).toString()));

            Set<String> excluded = new HashSet<>(
                    prefs.getStringSet("notification_exclude_list", new HashSet<>()));

            boolean[] checked = new boolean[launchable.size()];
            for (int i = 0; i < launchable.size(); i++) {
                checked[i] = excluded.contains(launchable.get(i).packageName);
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;

                AppExcludeAdapter adapter =
                        new AppExcludeAdapter(requireContext(), pm, launchable, checked);
                ListView listView = new ListView(requireContext());
                listView.setAdapter(adapter);
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    checked[position] = !checked[position];
                    CheckBox cb = view.findViewById(R.id.cb_exclude);
                    if (cb != null) cb.setChecked(checked[position]);
                });

                new AlertDialog.Builder(requireContext())
                        .setTitle("Exclude from Notifications")
                        .setView(listView)
                        .setPositiveButton("Save", (d, w) -> {
                            Set<String> newExcluded = new HashSet<>();
                            for (int i = 0; i < launchable.size(); i++) {
                                if (checked[i]) newExcluded.add(launchable.get(i).packageName);
                            }
                            prefs.edit().putStringSet("notification_exclude_list", newExcluded).apply();
                            updateExcludeCount();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }).start();
    }

    private static class AppExcludeAdapter extends ArrayAdapter<ApplicationInfo> {
        private final PackageManager pm;
        private final boolean[] checked;

        AppExcludeAdapter(Context ctx, PackageManager pm,
                          List<ApplicationInfo> apps, boolean[] checked) {
            super(ctx, 0, apps);
            this.pm = pm;
            this.checked = checked;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_app_exclude, parent, false);
            }
            ApplicationInfo app = getItem(position);
            ((ImageView) convertView.findViewById(R.id.iv_app_icon))
                    .setImageDrawable(pm.getApplicationIcon(app));
            ((TextView) convertView.findViewById(R.id.tv_app_name))
                    .setText(pm.getApplicationLabel(app));
            ((CheckBox) convertView.findViewById(R.id.cb_exclude))
                    .setChecked(checked[position]);
            return convertView;
        }
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
