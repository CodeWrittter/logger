package com.usageinsights.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.usageinsights.R;

import java.nio.charset.StandardCharsets;

public class TriggerFragment extends Fragment {

    private SharedPreferences prefs;

    // Access methods toggles
    private SwitchMaterial switchDial, switchNfc, switchVolume;
    private TextView tvDialStatus, tvNfcStatus, tvVolumeStatus;

    // Sub-sections
    private View labelDialCode, cardDialCode;
    private View labelNfc, cardNfc;
    private View labelVolume, cardVolume;

    private TextView tvDialCodeValue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_trigger, container, false);
        prefs = requireContext().getSharedPreferences("usageinsights", Context.MODE_PRIVATE);

        bindViews(root);
        loadState();
        setupListeners();

        return root;
    }

    private void bindViews(View root) {
        switchDial   = root.findViewById(R.id.switch_dial);
        switchNfc    = root.findViewById(R.id.switch_nfc);
        switchVolume = root.findViewById(R.id.switch_volume);

        tvDialStatus   = root.findViewById(R.id.tv_dial_status);
        tvNfcStatus    = root.findViewById(R.id.tv_nfc_status);
        tvVolumeStatus = root.findViewById(R.id.tv_volume_status);

        labelDialCode = root.findViewById(R.id.label_dial_code);
        cardDialCode  = root.findViewById(R.id.card_dial_code);
        labelNfc      = root.findViewById(R.id.label_nfc);
        cardNfc       = root.findViewById(R.id.card_nfc);
        labelVolume   = root.findViewById(R.id.label_volume);
        cardVolume    = root.findViewById(R.id.card_volume);

        tvDialCodeValue = root.findViewById(R.id.tv_dial_code_value);

        root.findViewById(R.id.btn_test_dial).setOnClickListener(v -> testDialCode());
        root.findViewById(R.id.btn_write_nfc).setOnClickListener(v -> writeNfcTag());
        root.findViewById(R.id.btn_scan_nfc).setOnClickListener(v -> scanNfcTag());
        tvDialCodeValue.setOnClickListener(v -> editDialCode());
        root.findViewById(R.id.row_volume_pattern).setOnClickListener(v -> editVolumePattern());
    }

    private void loadState() {
        boolean dialOn   = prefs.getBoolean("trigger_dial_enabled", true);
        boolean nfcOn    = prefs.getBoolean("trigger_nfc_enabled", false);
        boolean volumeOn = prefs.getBoolean("trigger_volume_enabled", true);

        switchDial.setChecked(dialOn);
        switchNfc.setChecked(nfcOn);
        switchVolume.setChecked(volumeOn);

        String dialCode = prefs.getString("dial_code", "*#01234#");
        tvDialCodeValue.setText(dialCode);
        tvDialStatus.setText(dialCode);

        tvNfcStatus.setText(nfcOn ? "Programmed" : "Not set");

        String volumePattern = prefs.getString("volume_sequence_display", "↑↑↓↑↓↓");
        tvVolumeStatus.setText(volumePattern);
        root_tv(R.id.tv_volume_pattern).setText(volumePattern);

        setSectionVisible(labelDialCode, cardDialCode, dialOn);
        setSectionVisible(labelNfc, cardNfc, nfcOn);
        setSectionVisible(labelVolume, cardVolume, volumeOn);
    }

    private void setupListeners() {
        switchDial.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("trigger_dial_enabled", checked).apply();
            setSectionVisible(labelDialCode, cardDialCode, checked);
            updateTriggerConfigured();
        });

        switchNfc.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("trigger_nfc_enabled", checked).apply();
            tvNfcStatus.setText(checked ? "Programmed" : "Not set");
            setSectionVisible(labelNfc, cardNfc, checked);
            updateTriggerConfigured();
        });

        switchVolume.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("trigger_volume_enabled", checked).apply();
            setSectionVisible(labelVolume, cardVolume, checked);
            // Start or stop accessibility service hint
            if (checked) {
                Toast.makeText(requireContext(),
                        "Enable Usage Insights in Accessibility settings",
                        Toast.LENGTH_LONG).show();
            }
            updateTriggerConfigured();
        });

    }

    private void setSectionVisible(View label, View card, boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        label.setVisibility(v);
        card.setVisibility(v);
    }

    private void updateTriggerConfigured() {
        boolean any = switchDial.isChecked() || switchNfc.isChecked()
                || switchVolume.isChecked();
        prefs.edit().putBoolean("trigger_configured", any).apply();
    }

    // ─── Volume pattern ───────────────────────────────────────────────────────

    private void editVolumePattern() {
        Context ctx = requireContext();
        int dp16 = (int) (16 * ctx.getResources().getDisplayMetrics().density);

        // Decode current saved pattern into a mutable list of keycodes
        String saved = prefs.getString("volume_sequence_pattern",
                KeyEvent.KEYCODE_VOLUME_UP + "," + KeyEvent.KEYCODE_VOLUME_UP + "," +
                KeyEvent.KEYCODE_VOLUME_DOWN + "," + KeyEvent.KEYCODE_VOLUME_UP + "," +
                KeyEvent.KEYCODE_VOLUME_DOWN + "," + KeyEvent.KEYCODE_VOLUME_DOWN);

        ArrayList<Integer> sequence = new ArrayList<>();
        for (String part : saved.split(",")) {
            try { sequence.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) {}
        }

        // Preview label
        TextView tvPreview = new TextView(ctx);
        tvPreview.setTextSize(22);
        tvPreview.setTextColor(0xFF34C759);
        tvPreview.setPadding(dp16 * 2, dp16, dp16 * 2, 0);
        tvPreview.setText(toDisplay(sequence));

        // Vol+ / Vol- / ⌫ buttons
        Button btnUp   = new Button(ctx);
        Button btnDown = new Button(ctx);
        Button btnDel  = new Button(ctx);
        btnUp.setText("↑  Vol+");
        btnDown.setText("↓  Vol−");
        btnDel.setText("⌫");

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnLp.setMargins(dp16 / 2, 0, dp16 / 2, 0);
        btnUp.setLayoutParams(btnLp);
        btnDown.setLayoutParams(btnLp);
        btnDel.setLayoutParams(btnLp);

        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(dp16, dp16, dp16, dp16);
        btnRow.addView(btnUp);
        btnRow.addView(btnDown);
        btnRow.addView(btnDel);

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(tvPreview);
        layout.addView(btnRow);

        btnUp.setOnClickListener(v -> {
            if (sequence.size() < 8) sequence.add(KeyEvent.KEYCODE_VOLUME_UP);
            tvPreview.setText(toDisplay(sequence));
        });
        btnDown.setOnClickListener(v -> {
            if (sequence.size() < 8) sequence.add(KeyEvent.KEYCODE_VOLUME_DOWN);
            tvPreview.setText(toDisplay(sequence));
        });
        btnDel.setOnClickListener(v -> {
            if (!sequence.isEmpty()) sequence.remove(sequence.size() - 1);
            tvPreview.setText(toDisplay(sequence));
        });

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle("Set Volume Sequence")
                .setMessage("Tap ↑ Vol+ and ↓ Vol− to build your sequence (3–8 presses).")
                .setView(layout)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (sequence.size() < 3) {
                tvPreview.setTextColor(0xFFFF3B30);
                tvPreview.setText("Minimum 3 presses");
                return;
            }
            StringBuilder pattern = new StringBuilder();
            for (int i = 0; i < sequence.size(); i++) {
                if (i > 0) pattern.append(",");
                pattern.append(sequence.get(i));
            }
            String display = toDisplay(sequence);
            prefs.edit()
                    .putString("volume_sequence_pattern", pattern.toString())
                    .putString("volume_sequence_display", display)
                    .apply();
            tvVolumeStatus.setText(display);
            root_tv(R.id.tv_volume_pattern).setText(display);
            dialog.dismiss();
            Toast.makeText(ctx, "Volume sequence updated", Toast.LENGTH_SHORT).show();
        });
    }

    private String toDisplay(ArrayList<Integer> sequence) {
        if (sequence.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (int key : sequence) {
            sb.append(key == KeyEvent.KEYCODE_VOLUME_UP ? "↑" : "↓");
        }
        return sb.toString();
    }

    // ─── Dial code ────────────────────────────────────────────────────────────

    private void editDialCode() {
        EditText input = new EditText(requireContext());
        input.setText(prefs.getString("dial_code", "*#01234#"));
        input.setSelectAllOnFocus(true);

        new AlertDialog.Builder(requireContext())
                .setTitle("Set Dial Code")
                .setMessage("Format: *#NNNNN# (digits only between *# and #)")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String code = input.getText().toString().trim();
                    if (code.isEmpty()) return;
                    prefs.edit().putString("dial_code", code).apply();
                    tvDialCodeValue.setText(code);
                    tvDialStatus.setText(code);
                    Toast.makeText(requireContext(),
                            "Dial code updated. Restart app to apply manifest change.",
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void testDialCode() {
        String code = prefs.getString("dial_code", "*#01234#");
        // Strip * and # for the URI — dialer interprets *#NNNNN# on its own
        try {
            Intent dial = new Intent(Intent.ACTION_DIAL);
            dial.setData(Uri.parse("tel:" + Uri.encode(code)));
            startActivity(dial);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not open dialer", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── NFC ─────────────────────────────────────────────────────────────────

    private void writeNfcTag() {
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(requireContext());
        if (nfc == null) {
            Toast.makeText(requireContext(), "NFC not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!nfc.isEnabled()) {
            Toast.makeText(requireContext(), "Please enable NFC in Settings", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(),
                "Hold a writable NFC tag to the back of your phone",
                Toast.LENGTH_LONG).show();
        // Actual write happens in onNewIntent when the tag is detected
        prefs.edit().putBoolean("nfc_write_pending", true).apply();
    }

    private void scanNfcTag() {
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(requireContext());
        if (nfc == null || !nfc.isEnabled()) {
            Toast.makeText(requireContext(), "NFC not available or disabled", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(),
                "Hold your programmed NFC tag to the back of your phone",
                Toast.LENGTH_LONG).show();
    }

    public void onNfcTagDetected(Tag tag) {
        if (!prefs.getBoolean("nfc_write_pending", false)) return;
        prefs.edit().putBoolean("nfc_write_pending", false).apply();

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                Toast.makeText(requireContext(), "Tag is not NDEF formatted", Toast.LENGTH_SHORT).show();
                return;
            }
            NdefRecord record = NdefRecord.createUri("usageinsights://open");
            NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
            ndef.connect();
            ndef.writeNdefMessage(msg);
            ndef.close();
            Toast.makeText(requireContext(), "NFC tag written successfully", Toast.LENGTH_SHORT).show();
            prefs.edit().putBoolean("trigger_nfc_enabled", true).apply();
            switchNfc.setChecked(true);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to write tag: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Helper to access TextViews from the inflated view ───────────────────

    private TextView root_tv(int id) {
        View root = getView();
        return root != null ? root.findViewById(id) : new TextView(requireContext());
    }
}
