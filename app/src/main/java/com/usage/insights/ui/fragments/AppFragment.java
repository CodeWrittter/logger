package com.usage.insights.ui.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.os.Build;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.usage.insights.BuildConfig;

import com.usage.insights.R;
import com.usage.insights.db.LogDao;
import com.usage.insights.ui.MainActivity;
import com.usage.insights.receivers.AdminReceiver;
import com.usage.insights.ui.LockActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class AppFragment extends Fragment {

    private TextView badgeNotifications, badgeLocation, badgeUsage, badgeAdmin, badgeBattery;
    private SwitchMaterial switchLauncherIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_app, container, false);

        bindViews(root);
        loadPermissions();
        loadLauncherToggle();
        loadAbout(root);
        setupListeners(root);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPermissions();
    }

    // ─── Bind ─────────────────────────────────────────────────────────────────

    private void bindViews(View root) {
        badgeNotifications = root.findViewById(R.id.badge_notifications);
        badgeLocation      = root.findViewById(R.id.badge_location);
        badgeUsage         = root.findViewById(R.id.badge_usage);
        badgeAdmin         = root.findViewById(R.id.badge_admin);
        badgeBattery       = root.findViewById(R.id.badge_battery);
        switchLauncherIcon = root.findViewById(R.id.switch_launcher_icon);
    }

    // ─── Permission badges ────────────────────────────────────────────────────

    private void loadPermissions() {
        Context ctx = requireContext();

        // Notification access
        Set<String> listeners = androidx.core.app.NotificationManagerCompat
                .getEnabledListenerPackages(ctx);
        setBadge(badgeNotifications, listeners.contains(ctx.getPackageName()),
                "Granted", "Not Granted");

        // Location — "Always" = fine + background, "While Using" = fine only
        boolean fine = ContextCompat.checkSelfPermission(ctx,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean background = ContextCompat.checkSelfPermission(ctx,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (fine && background) {
            setBadge(badgeLocation, true, "Always", "Always");
        } else if (fine) {
            setBadge(badgeLocation, false, "While Using", "While Using");
        } else {
            setBadge(badgeLocation, false, "Denied", "Denied");
        }

        // Usage stats
        AppOpsManager aom = (AppOpsManager) ctx.getSystemService(Context.APP_OPS_SERVICE);
        int usageMode = aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), ctx.getPackageName());
        setBadge(badgeUsage, usageMode == AppOpsManager.MODE_ALLOWED, "Granted", "Not Granted");

        // Device admin
        DevicePolicyManager dpm = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(ctx, AdminReceiver.class);
        setBadge(badgeAdmin, dpm.isAdminActive(admin), "Active", "Inactive");

        // Battery optimization — "Disabled" means we are whitelisted (good)
        android.os.PowerManager pm = (android.os.PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        boolean ignoring = pm.isIgnoringBatteryOptimizations(ctx.getPackageName());
        setBadge(badgeBattery, ignoring, "Disabled", "Enabled");
    }

    private void setBadge(TextView badge, boolean good, String goodLabel, String badLabel) {
        badge.setText(good ? goodLabel : badLabel);
        if (good) {
            badge.setTextColor(0xFF34C759);
            badge.setBackgroundResource(R.drawable.badge_green_bg);
        } else {
            badge.setTextColor(0xFFFF9500);
            badge.setBackgroundResource(R.drawable.badge_gray_bg);
        }
    }

    // ─── Launcher toggle ──────────────────────────────────────────────────────

    private void loadLauncherToggle() {
        ComponentName alias = launcherAlias();
        int state = requireContext().getPackageManager()
                .getComponentEnabledSetting(alias);
        // DEFAULT means "use the manifest value" which is enabled=true, so treat it as enabled
        boolean enabled = state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        switchLauncherIcon.setOnCheckedChangeListener(null);
        switchLauncherIcon.setChecked(enabled);
        switchLauncherIcon.setOnCheckedChangeListener((btn, on) -> setLauncherVisible(on));
    }

    private void setLauncherVisible(boolean visible) {
        requireContext().getPackageManager().setComponentEnabledSetting(
                launcherAlias(),
                visible ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private ComponentName launcherAlias() {
        return new ComponentName(requireContext(), "com.usage.insights.ui.LauncherAlias");
    }

    // ─── About ────────────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private void loadAbout(View root) {
        Context ctx = requireContext();

        ((TextView) root.findViewById(R.id.tv_version)).setText(BuildConfig.VERSION_NAME);
        ((TextView) root.findViewById(R.id.tv_package)).setText(BuildConfig.APPLICATION_ID);

        try {
            PackageInfo pi = ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            String installed = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(new Date(pi.firstInstallTime));
            ((TextView) root.findViewById(R.id.tv_installed)).setText(installed);
        } catch (PackageManager.NameNotFoundException ignored) {}

        long uptimeMs = SystemClock.elapsedRealtime();
        long days = uptimeMs / (1000 * 60 * 60 * 24);
        long hours = (uptimeMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        String bootLabel = days > 0
                ? days + " day" + (days > 1 ? "s" : "") + " ago"
                : hours + " hour" + (hours != 1 ? "s" : "") + " ago";
        ((TextView) root.findViewById(R.id.tv_last_boot)).setText(bootLabel);
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    private void suppress() {
        ((MainActivity) requireActivity()).suppressNextLeaveHint();
    }

    private void setupListeners(View root) {
        root.findViewById(R.id.row_perm_notifications).setOnClickListener(v -> {
            suppress();
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        });

        root.findViewById(R.id.row_perm_location).setOnClickListener(v -> {
            suppress();
            openAppSettings();
        });

        root.findViewById(R.id.row_perm_usage).setOnClickListener(v -> {
            suppress();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        });

        root.findViewById(R.id.row_perm_admin).setOnClickListener(v -> {
            suppress();
            ComponentName admin = new ComponentName(requireContext(), AdminReceiver.class);
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            startActivity(intent);
        });

        root.findViewById(R.id.row_perm_battery).setOnClickListener(v -> {
            suppress();
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        });

        root.findViewById(R.id.row_change_pin).setOnClickListener(v -> showChangePinDialog());
        root.findViewById(R.id.row_supabase_credentials).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showCredentialsDialog(false));
        root.findViewById(R.id.btn_setup_wizard).setOnClickListener(v -> runSetupWizard());

        root.findViewById(R.id.btn_deactivate_admin).setOnClickListener(v -> confirmDeactivateAdmin());

        root.findViewById(R.id.btn_disable_reset).setOnClickListener(v -> confirmDisableReset());

        root.findViewById(R.id.btn_lock).setOnClickListener(v -> lockApp());
    }

    // ─── Change PIN ───────────────────────────────────────────────────────────

    private void showChangePinDialog() {
        Context ctx = requireContext();
        SharedPreferences prefs = ctx.getSharedPreferences("usageinsights", Context.MODE_PRIVATE);

        int dp16 = (int) (16 * ctx.getResources().getDisplayMetrics().density);
        InputFilter[] maxSix = { new InputFilter.LengthFilter(6) };
        int numPwd = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD;

        EditText etCurrent = new EditText(ctx);
        etCurrent.setHint("Current PIN");
        etCurrent.setInputType(numPwd);
        etCurrent.setFilters(maxSix);

        EditText etNew = new EditText(ctx);
        etNew.setHint("New PIN (6 digits)");
        etNew.setInputType(numPwd);
        etNew.setFilters(maxSix);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp16;
        etNew.setLayoutParams(lp);

        EditText etConfirm = new EditText(ctx);
        etConfirm.setHint("Confirm new PIN");
        etConfirm.setInputType(numPwd);
        etConfirm.setFilters(maxSix);
        etConfirm.setLayoutParams(lp);

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16 + dp16, dp16, dp16 + dp16, 0);
        layout.addView(etCurrent);
        layout.addView(etNew);
        layout.addView(etConfirm);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle("Change PIN")
                .setView(layout)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String current = etCurrent.getText().toString();
            String newPin  = etNew.getText().toString();
            String confirm = etConfirm.getText().toString();

            String saved = prefs.getString("passcode", "593421");
            if (!current.equals(saved)) {
                etCurrent.setError("Incorrect PIN");
                return;
            }
            if (newPin.length() != 6) {
                etNew.setError("PIN must be 6 digits");
                return;
            }
            if (!newPin.equals(confirm)) {
                etConfirm.setError("PINs do not match");
                return;
            }
            prefs.edit().putString("passcode", newPin).apply();
            dialog.dismiss();
            Toast.makeText(ctx, "PIN updated", Toast.LENGTH_SHORT).show();
        });
    }

    // ─── Setup wizard ─────────────────────────────────────────────────────────

    private void runSetupWizard() {
        String[] steps = {
                "1. Notification Access\n→ Enable Usage Insights in the list",
                "2. Location\n→ Set to \"Allow all the time\"",
                "3. Usage Stats\n→ Enable for Usage Insights",
                "4. Device Admin\n→ Activate if prompted",
                "5. Battery Optimization\n→ Set to \"Don't optimize\""
        };
        new AlertDialog.Builder(requireContext())
                .setTitle("Setup Wizard")
                .setMessage("The following settings screens will open one at a time:\n\n" +
                        String.join("\n\n", steps))
                .setPositiveButton("Start", (d, w) -> openNextSetupStep(0))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private final Intent[] setupIntents = {
            new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
            new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            null, // device admin — built at runtime
            new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
    };

    private void openNextSetupStep(int step) {
        if (step >= setupIntents.length) return;
        if (step == 1) {
            setupIntents[1] = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + requireContext().getPackageName()));
        }
        if (step == 3) {
            ComponentName admin = new ComponentName(requireContext(), AdminReceiver.class);
            setupIntents[3] = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            setupIntents[3].putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
        }
        suppress();
        startActivity(setupIntents[step]);
    }

    // ─── Danger zone ──────────────────────────────────────────────────────────

    private void confirmDeactivateAdmin() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Deactivate Device Admin")
                .setMessage("This removes uninstall protection. The app can be uninstalled normally afterwards.")
                .setPositiveButton("Deactivate", (d, w) -> {
                    DevicePolicyManager dpm = (DevicePolicyManager)
                            requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
                    dpm.removeActiveAdmin(new ComponentName(requireContext(), AdminReceiver.class));
                    loadPermissions();
                    Toast.makeText(requireContext(), "Device admin deactivated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDisableReset() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Disable & Reset App")
                .setMessage("This will wipe all local data and preferences. The app will be returned to its initial state. Continue?")
                .setPositiveButton("Reset", (d, w) -> {
                    // Wipe DB
                    new LogDao(requireContext().getApplicationContext()).wipeAll();
                    // Wipe prefs
                    requireContext().getSharedPreferences("usageinsights", Context.MODE_PRIVATE)
                            .edit().clear().apply();
                    // Deactivate admin
                    DevicePolicyManager dpm = (DevicePolicyManager)
                            requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
                    dpm.removeActiveAdmin(new ComponentName(requireContext(), AdminReceiver.class));
                    Toast.makeText(requireContext(), "App reset. Restart to complete.",
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Lock ─────────────────────────────────────────────────────────────────

    private void lockApp() {
        Intent intent = new Intent(requireContext(), LockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        requireActivity().finish();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void openAppSettings() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + requireContext().getPackageName())));
    }
}
