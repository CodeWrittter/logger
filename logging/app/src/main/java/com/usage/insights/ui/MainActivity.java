package com.usage.insights.ui;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.usage.insights.R;
import com.usage.insights.ui.fragments.AppFragment;
import com.usage.insights.ui.fragments.DataFragment;
import com.usage.insights.ui.fragments.LoggingFragment;
import com.usage.insights.ui.fragments.SyncFragment;
import com.usage.insights.ui.fragments.TriggerFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(0xFF000000);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        setContentView(R.layout.activity_main);

        setupBottomNav();

        // Start on Trigger tab
        if (savedInstanceState == null) {
            loadFragment(new TriggerFragment());
        }

        scheduleWorkersIfNeeded();
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_trigger) {
                fragment = new TriggerFragment();
            } else if (id == R.id.nav_sync) {
                fragment = new SyncFragment();
            } else if (id == R.id.nav_logging) {
                fragment = new LoggingFragment();
            } else if (id == R.id.nav_data) {
                fragment = new DataFragment();
            } else if (id == R.id.nav_app) {
                fragment = new AppFragment();
            } else {
                return false;
            }
            loadFragment(fragment);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void scheduleWorkersIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("usageinsights", MODE_PRIVATE);
        if (prefs.getBoolean("workers_scheduled", false)) return;

        WorkerScheduler.scheduleAll(this);
        prefs.edit().putBoolean("workers_scheduled", true).apply();
    }

    // ─── Supabase credentials ─────────────────────────────────────────────────

    public void showCredentialsDialog(boolean required) {
        SharedPreferences prefs = getSharedPreferences("usageinsights", MODE_PRIVATE);
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);

        EditText etUrl = new EditText(this);
        etUrl.setHint("https://xxxxxxxxxxxx.supabase.co");
        etUrl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        etUrl.setText(prefs.getString("supabase_url", getString(R.string.supabase_url)));

        EditText etKey = new EditText(this);
        etKey.setHint("eyJ...");
        etKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        etKey.setText(prefs.getString("supabase_anon_key", getString(R.string.supabase_anon_key)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp16;
        etKey.setLayoutParams(lp);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16 * 2, dp16, dp16 * 2, 0);
        layout.addView(etUrl);
        layout.addView(etKey);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Supabase Credentials")
                .setMessage("Project URL and anon key — found in Supabase → Project Settings → API.")
                .setView(layout)
                .setPositiveButton("Save", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String key = etKey.getText().toString().trim();

            if (!url.startsWith("https://")) {
                etUrl.setError("Must start with https://");
                return;
            }
            if (key.isEmpty()) {
                etKey.setError("Anon key is required");
                return;
            }

            prefs.edit()
                    .putString("supabase_url", url)
                    .putString("supabase_anon_key", key)
                    .apply();
            dialog.dismiss();
            Toast.makeText(this, "Credentials saved", Toast.LENGTH_SHORT).show();
        });
    }

    // ─── Suppress leave hint ──────────────────────────────────────────────────

    private boolean suppressLeaveHint = false;

    public void suppressNextLeaveHint() {
        suppressLeaveHint = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressLeaveHint = false;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!suppressLeaveHint) {
            finishAndRemoveTask();
        }
        suppressLeaveHint = false;
    }

    public void hideIconIfSetupComplete() {
        SharedPreferences prefs = getSharedPreferences("usageinsights", MODE_PRIVATE);
        boolean setupComplete = prefs.getBoolean("setup_complete", false);
        boolean triggerConfigured = prefs.getBoolean("trigger_configured", false);

        if (setupComplete && triggerConfigured) {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(
                    new ComponentName(this, MainActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
