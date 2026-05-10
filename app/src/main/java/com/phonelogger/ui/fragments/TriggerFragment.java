package com.phonelogger.ui.fragments;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.phonelogger.R;
import com.phonelogger.services.ShakeDetectionService;

import java.nio.charset.StandardCharsets;

public class TriggerFragment extends Fragment {

    private SharedPreferences prefs;

    // Access methods toggles
    private SwitchMaterial switchDial, switchNfc, switchVolume, switchShake;
    private TextView tvDialStatus, tvNfcStatus, tvVolumeStatus, tvShakeStatus;

    // Sub-sections
    private View labelDialCode, cardDialCode;
    private View labelNfc, cardNfc;
    private View labelVolume, cardVolume;
    private View labelShake, cardShake;

    private TextView tvDialCodeValue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_trigger, container, false);
        prefs = requireContext().getSharedPreferences("phonelogger", Context.MODE_PRIVATE);

        bindViews(root);
        loadState();
        setupListeners();

        return root;
    }

    private void bindViews(View root) {
        switchDial   = root.findViewById(R.id.switch_dial);
        switchNfc    = root.findViewById(R.id.switch_nfc);
        switchVolume = root.findViewById(R.id.switch_volume);
        switchShake  = root.findViewById(R.id.switch_shake);

        tvDialStatus   = root.findViewById(R.id.tv_dial_status);
        tvNfcStatus    = root.findViewById(R.id.tv_nfc_status);
        tvVolumeStatus = root.findViewById(R.id.tv_volume_status);
        tvShakeStatus  = root.findViewById(R.id.tv_shake_status);

        labelDialCode = root.findViewById(R.id.label_dial_code);
        cardDialCode  = root.findViewById(R.id.card_dial_code);
        labelNfc      = root.findViewById(R.id.label_nfc);
        cardNfc       = root.findViewById(R.id.card_nfc);
        labelVolume   = root.findViewById(R.id.label_volume);
        cardVolume    = root.findViewById(R.id.card_volume);
        labelShake    = root.findViewById(R.id.label_shake);
        cardShake     = root.findViewById(R.id.card_shake);

        tvDialCodeValue = root.findViewById(R.id.tv_dial_code_value);

        root.findViewById(R.id.btn_test_dial).setOnClickListener(v -> testDialCode());
        root.findViewById(R.id.btn_write_nfc).setOnClickListener(v -> writeNfcTag());
        root.findViewById(R.id.btn_scan_nfc).setOnClickListener(v -> scanNfcTag());
        tvDialCodeValue.setOnClickListener(v -> editDialCode());
    }

    private void loadState() {
        boolean dialOn   = prefs.getBoolean("trigger_dial_enabled", true);
        boolean nfcOn    = prefs.getBoolean("trigger_nfc_enabled", false);
        boolean volumeOn = prefs.getBoolean("trigger_volume_enabled", false);
        boolean shakeOn  = prefs.getBoolean("trigger_shake_enabled", false);

        switchDial.setChecked(dialOn);
        switchNfc.setChecked(nfcOn);
        switchVolume.setChecked(volumeOn);
        switchShake.setChecked(shakeOn);

        String dialCode = prefs.getString("dial_code", "*#00000#");
        tvDialCodeValue.setText(dialCode);
        tvDialStatus.setText(dialCode);

        tvNfcStatus.setText(nfcOn ? "Programmed" : "Not set");

        String volumePattern = prefs.getString("volume_sequence_display", "↑↑↑↑↓↓");
        tvVolumeStatus.setText(volumePattern);
        root_tv(R.id.tv_volume_pattern).setText(volumePattern);

        String shakeSensitivity = prefs.getString("shake_sensitivity", "Medium");
        tvShakeStatus.setText(shakeSensitivity);
        root_tv(R.id.tv_shake_sensitivity).setText(shakeSensitivity);

        setSectionVisible(labelDialCode, cardDialCode, dialOn);
        setSectionVisible(labelNfc, cardNfc, nfcOn);
        setSectionVisible(labelVolume, cardVolume, volumeOn);
        setSectionVisible(labelShake, cardShake, shakeOn);
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
                        "Enable PhoneLogger in Accessibility settings",
                        Toast.LENGTH_LONG).show();
            }
            updateTriggerConfigured();
        });

        switchShake.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("trigger_shake_enabled", checked).apply();
            setSectionVisible(labelShake, cardShake, checked);
            Context ctx = requireContext();
            Intent svcIntent = new Intent(ctx, ShakeDetectionService.class);
            if (checked) {
                ctx.startService(svcIntent);
            } else {
                ctx.stopService(svcIntent);
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
                || switchVolume.isChecked() || switchShake.isChecked();
        prefs.edit().putBoolean("trigger_configured", any).apply();
    }

    // ─── Dial code ────────────────────────────────────────────────────────────

    private void editDialCode() {
        EditText input = new EditText(requireContext());
        input.setText(prefs.getString("dial_code", "*#00000#"));
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
        String code = prefs.getString("dial_code", "*#00000#");
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
            NdefRecord record = NdefRecord.createUri("phonelogger://open");
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
