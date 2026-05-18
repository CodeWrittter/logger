package com.usage.insights.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.usage.insights.R;

public class LockActivity extends AppCompatActivity {

    private static final int PASSCODE_LENGTH = 6;
    private static final String PREF_PASSCODE = "passcode";
    private static final String DEFAULT_PASSCODE = "593421";

    private final StringBuilder entered = new StringBuilder();
    private View[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(0xFF000000);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        setContentView(R.layout.activity_lock);

        setupDots();
        setupNumpad();
    }

    // ─── Dots ─────────────────────────────────────────────────────────────────

    private void setupDots() {
        dots = new View[]{
            findViewById(R.id.dot_0),
            findViewById(R.id.dot_1),
            findViewById(R.id.dot_2),
            findViewById(R.id.dot_3),
            findViewById(R.id.dot_4),
            findViewById(R.id.dot_5)
        };
    }

    private void fillDot(int index) {
        View dot = dots[index];
        dot.setActivated(true);
        dot.animate().scaleX(1.3f).scaleY(1.3f).setDuration(80).withEndAction(() ->
                dot.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
        ).start();
    }

    private void clearDot(int index) {
        dots[index].setBackgroundResource(R.drawable.dot_bg);
        dots[index].setActivated(false);
    }

    private void clearAllDots() {
        for (int i = 0; i < PASSCODE_LENGTH; i++) clearDot(i);
    }

    // ─── Numpad ───────────────────────────────────────────────────────────────

    private void setupNumpad() {
        attachKey(R.id.btn_1, "1");
        attachKey(R.id.btn_2, "2");
        attachKey(R.id.btn_3, "3");
        attachKey(R.id.btn_4, "4");
        attachKey(R.id.btn_5, "5");
        attachKey(R.id.btn_6, "6");
        attachKey(R.id.btn_7, "7");
        attachKey(R.id.btn_8, "8");
        attachKey(R.id.btn_9, "9");
        attachKey(R.id.btn_0, "0");
        attachKey(R.id.btn_backspace, null);
    }

    private void attachKey(int id, String digit) {
        View btn = findViewById(id);
        btn.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(70).start();
            }
            return false;
        });
        btn.setOnClickListener(v -> {
            if (digit == null) onBackspace();
            else onDigit(digit);
        });
    }

    // ─── Input logic ──────────────────────────────────────────────────────────

    private void onDigit(String digit) {
        if (entered.length() >= PASSCODE_LENGTH) return;
        entered.append(digit);
        fillDot(entered.length() - 1);

        if (entered.length() == PASSCODE_LENGTH) {
            dots[PASSCODE_LENGTH - 1].postDelayed(this::validate, 150);
        }
    }

    private void onBackspace() {
        if (entered.length() == 0) return;
        entered.deleteCharAt(entered.length() - 1);
        clearDot(entered.length());
    }

    private void validate() {
        SharedPreferences prefs = getSharedPreferences("usageinsights", MODE_PRIVATE);
        String saved = prefs.getString(PREF_PASSCODE, DEFAULT_PASSCODE);
        if (entered.toString().equals(saved)) {
            onSuccess();
        } else {
            onWrongPasscode();
        }
    }

    private void onSuccess() {
        for (View dot : dots) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(0xFF34C759);
            dot.setBackground(d);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void onWrongPasscode() {
        LinearLayout dotsRow = findViewById(R.id.ll_dots);
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        for (View dot : dots) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(0xFFE53935);
            dot.setBackground(d);
        }

        shake.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) {
                entered.setLength(0);
                clearAllDots();
            }
        });

        dotsRow.startAnimation(shake);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        finishAndRemoveTask();
    }
}
