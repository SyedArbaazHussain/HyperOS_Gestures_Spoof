/*
 * Copyright (C) 2026 Syed Arbaaz Hussain
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package com.hyperos.gesturefix;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;

public class LogsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logs, container, false);
        
        MainActivity activity = (MainActivity) getActivity();
        MaterialCardView statusCard = view.findViewById(R.id.statusCard);
        TextView statusText = view.findViewById(R.id.statusText);
        TextView logContent = view.findViewById(R.id.logContent);

        if (activity != null && activity.isModuleActive()) {
            statusText.setText("ACTIVE: Gestures Guarded");
            statusCard.setCardBackgroundColor(0xFF2E7D32);
            logContent.setText("[INFO] Module hooked into android.provider.Settings\n" +
                               "[INFO] Veto system operational\n" +
                               "[SUCCESS] MIUI Home redirection enabled");
        } else {
            statusText.setText("INACTIVE: Hook Failed");
            statusCard.setCardBackgroundColor(0xFFC62828);
            logContent.setText("[ERROR] LSPosed scope not set or module disabled.\n" +
                               "[ADVICE] Please enable 'Android System' and 'Miui Home' in LSPosed.");
        }

        return view;
    }
}