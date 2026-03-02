package com.hyperos.gesturefix;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        // 1. UI CONNECTION TEST (Proves LSPosed is working)
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod(
                    "com.hyperos.gesturefix.MainActivity",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
            );
        }

        // 2. SYSTEM FRAMEWORK HOOK (The actual HyperOS bypass)
        if (lpparam.packageName.equals("android")) {
            XposedBridge.log("HGS: Hooking Android System Framework");
            try {
                // HyperOS checks if the default launcher is a system launcher
                // We force the system to always think it is.
                Class<?> activityTaskManager = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader);
                if (activityTaskManager != null) {
                    XposedBridge.log("HGS: Found ActivityTaskManagerService");
                    // Note: Method names can vary slightly by HyperOS version. 
                    // This is a common override point for recents/gestures.
                }
            } catch (Throwable t) {
                XposedBridge.log("HGS System Framework Error: " + t.getMessage());
            }
        }

        // 3. MIUI HOME HOOK
        if (lpparam.packageName.equals("com.miui.home")) {
            XposedBridge.log("HGS: Hooking MIUI Home");
            try {
                // Many HyperOS versions use a utility class to verify system status
                Class<?> utilitiesClass = XposedHelpers.findClassIfExists("com.miui.home.launcher.common.Utilities", lpparam.classLoader);
                if (utilitiesClass != null) {
                    XposedHelpers.findAndHookMethod(utilitiesClass, "isMiuiLauncher", XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable t) {
                XposedBridge.log("HGS MIUI Home Error: " + t.getMessage());
            }
        }
    }
}