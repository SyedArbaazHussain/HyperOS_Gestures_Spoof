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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "launcher_prefs";

    /**
     * Heartbeat method: Hooked by Xposed to return true.
     */
    public boolean isModuleActive() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);

        // AMOLED vs Material You: Dynamic Color only for Light Mode
        if (!isDark) {
            DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        }

        // Apply System Theme
        AppCompatDelegate.setDefaultNightMode(isDark ? 
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initNavigation(isDark);
    }

    private void initNavigation(boolean isDark) {
        // 1. Setup Toolbar & Theme Toggle
        SwitchCompat themeSwitch = findViewById(R.id.themeSwitch);
        themeSwitch.setChecked(isDark);
        themeSwitch.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            makePrefsReadable();
            recreate();
        });

        // 2. Setup ViewPager2 and TabLayout (The Navigation Menu)
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return (position == 0) ? new LauncherFragment() : new LogsFragment();
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        // Attach Tabs to ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Launchers" : "Logs");
        }).attach();
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void makePrefsReadable() {
        File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, PREFS_NAME + ".xml");
        if (prefsDir.exists()) {
            prefsDir.setExecutable(true, false);
            prefsDir.setReadable(true, false);
        }
        if (prefsFile.exists()) {
            prefsFile.setReadable(true, false);
        }
    }
}