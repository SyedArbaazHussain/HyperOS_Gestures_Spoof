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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LauncherFragment extends Fragment {

    private SharedPreferences prefs;
    private RecyclerView recyclerView;
    private LauncherAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the lightweight fragment layout
        View view = inflater.inflate(R.layout.fragment_launcher, container, false);
        
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            prefs = activity.getPrefs();
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadLaunchers();

        return view;
    }

    private void loadLaunchers() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = requireContext().getPackageManager();
        
        // Query all activities that handle the HOME intent
        List<ResolveInfo> launchers = pm.queryIntentActivities(intent, 0);

        adapter = new LauncherAdapter(launchers, requireContext());
        recyclerView.setAdapter(adapter);
    }

    // --- High-Performance Adapter for Launcher Selection ---
    class LauncherAdapter extends RecyclerView.Adapter<LauncherAdapter.ViewHolder> {
        private final List<ResolveInfo> launchers;
        private final Context context;
        private final PackageManager pm;
        private String selectedPkg;

        public LauncherAdapter(List<ResolveInfo> launchers, Context context) {
            this.launchers = launchers;
            this.context = context;
            this.pm = context.getPackageManager();
            this.selectedPkg = prefs.getString("target_pkg", "");
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Reuses the item_launcher.xml we defined earlier
            View v = LayoutInflater.from(context).inflate(R.layout.item_launcher, parent, false);
            return new ViewHolder(v);
        }

        @Override
        @SuppressLint("NotifyDataSetChanged")
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ResolveInfo info = launchers.get(position);
            String pkg = info.activityInfo.packageName;
            String cls = info.activityInfo.name;

            holder.icon.setImageDrawable(info.loadIcon(pm));
            holder.name.setText(info.loadLabel(pm));
            holder.pkgName.setText(pkg);

            boolean isDark = prefs.getBoolean("dark_mode", false);
            
            // AMOLED-aware highlighting: Glassmorphism effect
            // White tint for Black background, Dark tint for Material You background
            int highlightColor = isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#22000000");
            holder.itemView.setBackgroundColor(pkg.equals(selectedPkg) ? highlightColor : Color.TRANSPARENT);

            holder.itemView.setOnClickListener(v -> {
                if (pkg.equals(selectedPkg)) return;

                selectedPkg = pkg;
                
                // Commit the selection to SharedPrefs
                prefs.edit()
                     .putString("target_pkg", pkg)
                     .putString("target_class", cls)
                     .apply();
                
                // CRITICAL: Trigger the file permission fix in MainActivity 
                // This allows the Xposed module to read the new selection immediately.
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).makePrefsReadable();
                }
                
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return launchers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name, pkgName;
            public ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.launcherIcon);
                name = itemView.findViewById(R.id.launcherName);
                pkgName = itemView.findViewById(R.id.launcherPkg);
            }
        }
    }
}