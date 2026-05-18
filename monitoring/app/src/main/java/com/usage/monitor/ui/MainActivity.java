package com.usage.monitor.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.usage.monitor.R;
import com.usage.monitor.ui.fragments.TodayFragment;
import com.usage.monitor.ui.fragments.AppsFragment;
import com.usage.monitor.ui.fragments.ActivityFragment;
import com.usage.monitor.ui.fragments.DeviceFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        if (savedInstanceState == null) {
            loadFragment(new TodayFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_today) {
                fragment = new TodayFragment();
            } else if (id == R.id.nav_apps) {
                fragment = new AppsFragment();
            } else if (id == R.id.nav_activity) {
                fragment = new ActivityFragment();
            } else if (id == R.id.nav_device) {
                fragment = new DeviceFragment();
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
}
