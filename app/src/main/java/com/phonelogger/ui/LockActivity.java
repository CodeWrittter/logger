package com.phonelogger.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.phonelogger.R;

public class LockActivity extends AppCompatActivity {

    private static final int PASSCODE_LENGTH = 6;
    private static final String PREF_PASSCODE = "passcode";
    private static final String DEFAULT_PASSCODE = "123456";

    private final StringBuilder entered = new StringBuilder();
    private ImageView[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(0xFF000000);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        setContentView(R.layout.activity_lock);

        dots = new ImageView[PASSCODE_LENGTH];
        buildDots();
        buildNumpad();
        updateHint();
    }

    // ─── Dots ─────────────────────────────────────────────────────────────────

    private void buildDots() {
        LinearLayout ll = findViewById(R.id.ll_dots);
        int sizePx = dp(18);
        int marginPx = dp(14);

        for (int i = 0; i < PASSCODE_LENGTH; i++) {
            ImageView dot = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMargins(marginPx, 0, marginPx, 0);
            dot.setLayoutParams(lp);
            dot.setImageDrawable(dotDrawable(false));
            dots[i] = dot;
            ll.addView(dot);
        }
    }

    private GradientDrawable dotDrawable(boolean filled) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        if (filled) {
            d.setColor(0xFFFFFFFF);
        } else {
            d.setColor(0x00000000);
            d.setStroke(dp(2), 0xFFFFFFFF);
        }
        return d;
    }

    private void fillDot(int index) {
        ImageView dot = dots[index];
        dot.setImageDrawable(dotDrawable(true));
        dot.animate().scaleX(1.3f).scaleY(1.3f).setDuration(80).withEndAction(() ->
                dot.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
        ).start();
    }

    private void clearDot(int index) {
        dots[index].setImageDrawable(dotDrawable(false));
    }

    private void clearAllDots() {
        for (int i = 0; i < PASSCODE_LENGTH; i++) clearDot(i);
    }

    // ─── Numpad ───────────────────────────────────────────────────────────────

    private static final String[][][] KEYS = {
            {{"1", ""}, {"2", "ABC"}, {"3", "DEF"}},
            {{"4", "GHI"}, {"5", "JKL"}, {"6", "MNO"}},
            {{"7", "PQRS"}, {"8", "TUV"}, {"9", "WXYZ"}},
            {{"", ""}, {"0", ""}, {"⌫", ""}}   // ⌫ = backspace
    };

    private void buildNumpad() {
        TableLayout table = findViewById(R.id.tl_numpad);

        for (String[][] rowData : KEYS) {
            TableRow row = new TableRow(this);
            row.setGravity(Gravity.CENTER);

            for (String[] key : rowData) {
                View btn = makeKeyView(key[0], key[1]);
                TableRow.LayoutParams lp = new TableRow.LayoutParams(
                        0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                lp.gravity = Gravity.CENTER;
                btn.setLayoutParams(lp);
                row.addView(btn);
            }

            table.addView(row);
        }
    }

    private View makeKeyView(String digit, String letters) {
        // Invisible spacer for empty slots
        if (digit.isEmpty()) {
            View spacer = new View(this);
            spacer.setVisibility(View.INVISIBLE);
            int size = dp(72);
            spacer.setMinimumWidth(size);
            spacer.setMinimumHeight(size);
            FrameLayout wrap = new FrameLayout(this);
            wrap.setPadding(dp(10), dp(10), dp(10), dp(10));
            wrap.addView(spacer);
            return wrap;
        }

        // Circle container
        LinearLayout circle = new LinearLayout(this);
        circle.setOrientation(LinearLayout.VERTICAL);
        circle.setGravity(Gravity.CENTER);
        int sizePx = dp(72);
        circle.setMinimumWidth(sizePx);
        circle.setMinimumHeight(sizePx);
        circle.setBackground(circleBackground());
        circle.setClickable(true);
        circle.setFocusable(true);

        // Large digit/symbol text
        TextView tvDigit = new TextView(this);
        tvDigit.setText(digit);
        tvDigit.setTextColor(0xFFFFFFFF);
        tvDigit.setTextSize(TypedValue.COMPLEX_UNIT_SP, digit.equals("⌫") ? 22 : 28);
        tvDigit.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        tvDigit.setGravity(Gravity.CENTER);
        circle.addView(tvDigit);

        // Letters below (if any)
        if (!letters.isEmpty()) {
            TextView tvLetters = new TextView(this);
            tvLetters.setText(letters);
            tvLetters.setTextColor(0xFF9E9E9E);
            tvLetters.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvLetters.setLetterSpacing(0.15f);
            tvLetters.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(1);
            tvLetters.setLayoutParams(lp);
            circle.addView(tvLetters);
        }

        // Press scale feedback
        circle.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(70).start();
            }
            return false;
        });

        circle.setOnClickListener(v -> {
            if (digit.equals("⌫")) {
                onBackspace();
            } else {
                onDigit(digit);
            }
        });

        // Wrap with padding so circles don't touch edge-to-edge
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setPadding(dp(10), dp(8), dp(10), dp(8));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        lp.gravity = Gravity.CENTER;
        circle.setLayoutParams(lp);
        wrapper.addView(circle);
        return wrapper;
    }

    private GradientDrawable circleBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(0xFF3A3A3A);
        return d;
    }

    // ─── Input logic ──────────────────────────────────────────────────────────

    private void onDigit(String digit) {
        if (entered.length() >= PASSCODE_LENGTH) return;
        entered.append(digit);
        fillDot(entered.length() - 1);

        if (entered.length() == PASSCODE_LENGTH) {
            // Small delay so last dot animation is visible
            dots[PASSCODE_LENGTH - 1].postDelayed(this::validate, 150);
        }
    }

    private void onBackspace() {
        if (entered.length() == 0) return;
        entered.deleteCharAt(entered.length() - 1);
        clearDot(entered.length());
    }

    private void validate() {
        SharedPreferences prefs = getSharedPreferences("phonelogger", MODE_PRIVATE);
        String saved = prefs.getString(PREF_PASSCODE, DEFAULT_PASSCODE);

        if (entered.toString().equals(saved)) {
            onSuccess();
        } else {
            onWrongPasscode();
        }
    }

    private void onSuccess() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void onWrongPasscode() {
        LinearLayout dotsRow = findViewById(R.id.ll_dots);
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        // Briefly turn dots red to signal error
        for (ImageView dot : dots) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(0xFFE53935);
            dot.setImageDrawable(d);
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

    // ─── Hint ─────────────────────────────────────────────────────────────────

    private void updateHint() {
        SharedPreferences prefs = getSharedPreferences("phonelogger", MODE_PRIVATE);
        String code = prefs.getString(PREF_PASSCODE, DEFAULT_PASSCODE);
        TextView hint = findViewById(R.id.tv_hint);
        hint.setText("First connection? Use " + code);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
