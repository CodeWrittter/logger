package com.usageinsights.services;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.usageinsights.ui.LockActivity;

import java.util.ArrayList;
import java.util.List;

public class VolumeAccessibilityService extends AccessibilityService {

    private final List<Integer> sequence = new ArrayList<>();
    private long lastKeyTime = 0;
    private static final long SEQUENCE_TIMEOUT_MS = 3000;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

        SharedPreferences prefs = getSharedPreferences("usageinsights", MODE_PRIVATE);
        if (!prefs.getBoolean("trigger_volume_enabled", false)) return false;

        int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastKeyTime > SEQUENCE_TIMEOUT_MS) {
            sequence.clear();
        }
        lastKeyTime = now;
        sequence.add(keyCode);

        String savedPattern = prefs.getString("volume_sequence_pattern",
                KeyEvent.KEYCODE_VOLUME_UP + "," +
                KeyEvent.KEYCODE_VOLUME_UP + "," +
                KeyEvent.KEYCODE_VOLUME_DOWN + "," +
                KeyEvent.KEYCODE_VOLUME_UP + "," +
                KeyEvent.KEYCODE_VOLUME_DOWN + "," +
                KeyEvent.KEYCODE_VOLUME_DOWN);

        String[] parts = savedPattern.split(",");
        if (sequence.size() == parts.length) {
            boolean match = true;
            for (int i = 0; i < parts.length; i++) {
                if (sequence.get(i) != Integer.parseInt(parts[i].trim())) {
                    match = false;
                    break;
                }
            }
            if (match) {
                sequence.clear();
                Intent launch = new Intent(this, LockActivity.class);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
                return true;
            }
            sequence.remove(0);
        }

        return false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
