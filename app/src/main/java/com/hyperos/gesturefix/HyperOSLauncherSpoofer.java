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

import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    private static final String PACKAGE_NAME = "com.hyperos.gesturefix";
    private static final String PREFS_NAME = "launcher_prefs";
    private static final String TAG = "HyperOSFix: ";

    private XSharedPreferences getSafePrefs() {
        XSharedPreferences prefs = new XSharedPreferences(PACKAGE_NAME, PREFS_NAME);
        if (prefs.getFile().exists()) {
            prefs.makeWorldReadable();
        }
        prefs.reload();
        return prefs;
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        // 1. SELF-CHECK & LOGGING HOOK
        if (lpparam.packageName.equals(PACKAGE_NAME)) {
            XposedHelpers.findAndHookMethod(
                PACKAGE_NAME + ".MainActivity",
                lpparam.classLoader,
                "isModuleActive",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
        }

        // 2. FRAMEWORK PROTECTOR (The "Veto" System)
        if (lpparam.packageName.equals("android")) {
            
            // Intercept Global Settings Writes
            XposedHelpers.findAndHookMethod(
                "android.provider.Settings.Global",
                lpparam.classLoader,
                "putStringForUser",
                android.content.ContentResolver.class, String.class, String.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String name = (String) param.args[1];
                        String value = (String) param.args[2];
                        if ("force_fsg_nav_bar".equals(name) && !"1".equals(value)) {
                            XposedBridge.log(TAG + "Vetoed system attempt to disable gestures. Forcing '1'.");
                            param.args[2] = "1"; // Force full-screen gestures
                        }
                    }
                }
            );

            // Block Navigation Bar Appearance in WindowManager
            XC_MethodHook displayPolicyHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        XposedHelpers.setBooleanField(param.thisObject, "mForceShowNavigationBar", false);
                        XposedHelpers.setBooleanField(param.thisObject, "mHasNavigationBar", false);
                    } catch (Throwable ignored) {}
                }
            };

            XposedHelpers.findAndHookMethod("com.android.server.wm.DisplayPolicy", lpparam.classLoader, "updateSystemBarAttributes", displayPolicyHook);
        }

        // 3. LAUNCHER SPOOFING & REDIRECTION
        if (lpparam.packageName.equals("com.miui.home")) {
            XposedHelpers.findAndHookMethod(
                "com.miui.home.launcher.Launcher",
                lpparam.classLoader,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        XSharedPreferences prefs = getSafePrefs();
                        String targetPkg = prefs.getString("target_pkg", "");
                        String targetClass = prefs.getString("target_class", "");

                        if (targetPkg.isEmpty() || targetPkg.equals("com.miui.home")) return;

                        android.app.Activity miuiHome = (android.app.Activity) param.thisObject;
                        
                        // Intelligent Redirection Logic
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setComponent(new ComponentName(targetPkg, targetClass));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        
                        try {
                            miuiHome.startActivity(intent);
                            XposedBridge.log(TAG + "Redirected MIUI Home to: " + targetPkg);
                        } catch (Exception e) {
                            XposedBridge.log(TAG + "Redirection failed: " + e.getMessage());
                        }
                    }
                }
            );
        }

        // 4. SYSTEM UI CLEANUP
        if (lpparam.packageName.equals("com.android.systemui")) {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.navigationbar.NavigationBar",
                lpparam.classLoader,
                "onViewAttachedToWindow",
                View.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        View view = (View) param.args[0];
                        if (view != null) {
                            view.setVisibility(View.GONE);
                            XposedBridge.log(TAG + "Enforced Stealth Navigation Bar.");
                        }
                    }
                }
            );
        }
    }
}