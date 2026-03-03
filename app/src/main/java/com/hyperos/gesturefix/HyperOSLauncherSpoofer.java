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

    private static final String TAG = "HGS_DEBUG: ";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        
        // HEARTBEAT & SELF-LOGGING
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, 
                "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        // BRUTAL FRAMEWORK HOOKS
        if (lpparam.packageName.equals("android") || 
            lpparam.packageName.equals("com.android.systemui") || 
            lpparam.packageName.equals("com.xiaomi.misettings")) {

            XposedBridge.log(TAG + "Infiltrating " + lpparam.packageName);

            // 1. Force the Gesture Flag at the Database level
            XposedHelpers.findAndHookMethod(Settings.Global.class, "getInt", 
                ContentResolver.class, String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[1];
                    if ("force_fsg_nav_bar".equals(key) || "hide_gesture_line".equals(key)) {
                        param.setResult(1);
                    }
                }
            });

            // 2. Kill the "Default Launcher" check
            try {
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", 
                        android.content.Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable t) { XposedBridge.log(TAG + "NavUtils Hook Failed"); }
        }

        // 3. PREVENT AUTO-REVERT (Activity Manager)
        if (lpparam.packageName.equals("android")) {
            try {
                Class<?> ams = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader);
                if (ams != null) {
                    XposedHelpers.findAndHookMethod(ams, "updateDefaultHomeActivity", ComponentName.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            ComponentName cn = (ComponentName) param.args[0];
                            if (cn != null && cn.getPackageName().equals("com.miui.home")) {
                                XposedBridge.log(TAG + "BLOCKED HyperOS from stealing back the Home screen!");
                                param.setResult(null);
                            }
                        }
                    });
                }
            } catch (Throwable t) { XposedBridge.log(TAG + "AMS Lockdown Failed"); }
        }
    }
}