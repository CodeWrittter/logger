package com.phonelogger.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AppFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("App Settings");
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(32f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setBackgroundColor(0xFF000000);
        return tv;
    }
}
