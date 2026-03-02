package com.hyperos.gesturefix;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.provider.Settings;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        // 1. UI HEARTBEAT
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        // 2. GLOBAL SYSTEM & UI HOOKS
        if (lpparam.packageName.equals("android") || 
            lpparam.packageName.equals("com.android.systemui") || 
            lpparam.packageName.equals("com.android.settings") || 
            lpparam.packageName.equals("com.xiaomi.misettings")) {

            try {
                // FORCE GESTURE FLAG: Intercepts when any app asks if Gestures are enabled
                XposedHelpers.findAndHookMethod(Settings.Global.class, "getInt", 
                    ContentResolver.class, String.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String name = (String) param.args[1];
                        if ("force_fsg_nav_bar".equals(name) || "hide_gesture_line".equals(name)) {
                            param.setResult(1); 
                        }
                    }
                });

                // SPOOF NAV UTILS: Lies about launcher compatibility
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                    XposedHelpers.findAndHookMethod(navUtils, "isAllowThirdPartyLauncherGestures", android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable t) { XposedBridge.log("HGS Framework Error: " + t.getMessage()); }
        }

        // 3. ACTIVITY MANAGER LOCKDOWN
        if (lpparam.packageName.equals("android")) {
            try {
                Class<?> ams = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader);
                if (ams != null) {
                    // Prevent the system from "Safety Resetting" the Home app
                    XposedHelpers.findAndHookMethod(ams, "updateDefaultHomeActivity", ComponentName.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            ComponentName cn = (ComponentName) param.args[0];
                            if (cn != null && cn.getPackageName().equals("com.miui.home")) {
                                param.setResult(null); // Stop the reset
                            }
                        }
                    });
                    
                    // Tell the Task Manager the current Home is supported
                    XposedHelpers.findAndHookMethod(ams, "isCurrentHomeSupported", XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable t) { XposedBridge.log("HGS ATMS Error: " + t.getMessage()); }
        }

        // 4. LAUNCHER SIDE BYPASS
        if (lpparam.packageName.equals("com.miui.home")) {
            try {
                Class<?> deviceConfig = XposedHelpers.findClassIfExists("com.miui.home.launcher.DeviceConfig", lpparam.classLoader);
                if (deviceConfig != null) {
                    XposedHelpers.findAndHookMethod(deviceConfig, "isThirdPartyLauncherSupported", XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }
    }
}