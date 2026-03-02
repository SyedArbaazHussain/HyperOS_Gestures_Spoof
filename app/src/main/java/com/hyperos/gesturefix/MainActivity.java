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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

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
     * Hook point for LSPosed. 
     * If the module is active, the hook will force this to return true.
     */
    public boolean isModuleActive() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Initialize Preferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);

        // 2. Prevent Infinite Loop: Only set mode if it differs from current
        int targetMode = isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }

        // 3. Apply Material You (Dynamic Colors)
        // Note: Dynamic colors usually look best in Light Mode or Standard Dark.
        // For AMOLED, we skip dynamic colors to keep it pure black.
        if (!isDark) {
            DynamicColors.applyToActivityIfAvailable(this);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 4. Initialize the UI Components
        initNavigation(isDark);
        
        // Ensure the preference file is readable by the System (HyperOS Launcher)
        makePrefsReadable();
    }

    private void initNavigation(boolean isDark) {
        // Setup Theme Switcher
        SwitchCompat themeSwitch = findViewById(R.id.themeSwitch);
        if (themeSwitch != null) {
            themeSwitch.setChecked(isDark);
            themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Save and Recreate ONLY if the value changed
                if (isChecked != isDark) {
                    prefs.edit().putBoolean("dark_mode", isChecked).apply();
                    makePrefsReadable();
                    recreate();
                }
            });
        }

        // Setup Tabs and ViewPager
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        if (viewPager != null && tabLayout != null) {
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

            // Sync TabLayout with ViewPager2
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                tab.setText(position == 0 ? "Launchers" : "Logs");
            }).attach();
        }
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    /**
     * Fixes the permission of the XML file so the hooked Launcher 
     * can read the user's preference settings.
     */
    @SuppressLint("SetWorldReadable")
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void makePrefsReadable() {
        try {
            File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, PREFS_NAME + ".xml");
            
            if (prefsDir.exists()) {
                prefsDir.setExecutable(true, false);
                prefsDir.setReadable(true, false);
            }
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}