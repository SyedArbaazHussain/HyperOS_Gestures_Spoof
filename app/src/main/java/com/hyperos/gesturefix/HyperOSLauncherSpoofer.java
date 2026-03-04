package com.hyperos.gesturefix;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HyperOSLauncherSpoofer implements IXposedHookLoadPackage {

    private static final String TAG = "HGS_LOG";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        
        // 1. SELF-HOOK: Update app UI status to "ACTIVE"
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, 
                "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        // 2. SYSTEM FRAMEWORK HOOKS (android)
        if (lpparam.packageName.equals("android")) {
            // Block attempt to disable Gestural Overlay
            try {
                Class<?> oms = XposedHelpers.findClassIfExists("com.android.server.om.OverlayManagerService", lpparam.classLoader);
                if (oms != null) {
                    XposedHelpers.findAndHookMethod(oms, "setEnabled", 
                        String.class, boolean.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String packageName = (String) param.args[0];
                            boolean enable = (boolean) param.args[1];
                            if (packageName.equals("com.android.internal.systemui.navbar.gestural") && !enable) {
                                param.setResult(true); 
                            }
                        }
                    });
                }
            } catch (Throwable ignored) {}

            // Prevent auto-revert to MIUI Launcher
            try {
                Class<?> ams = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader);
                if (ams != null) {
                    XposedHelpers.findAndHookMethod(ams, "updateDefaultHomeActivity", ComponentName.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            ComponentName cn = (ComponentName) param.args[0];
                            if (cn != null && cn.getPackageName().equals("com.miui.home")) {
                                param.setResult(null); 
                            }
                        }
                    });
                }
            } catch (Throwable ignored) {}

            // Support Fullscreen Gestures in Framework Settings
            try {
                Class<?> miuiSettings = XposedHelpers.findClassIfExists("android.provider.MiuiSettings$System", lpparam.classLoader);
                if (miuiSettings != null) {
                    XposedHelpers.findAndHookMethod(miuiSettings, "isSupportFullscreenGesture", 
                        Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }

        // 3. SETTINGS BYPASS (Xiaomi Settings app logic)
        if (lpparam.packageName.equals("com.xiaomi.misettings") || lpparam.packageName.equals("com.android.settings")) {
            try {
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", Context.class, XC_MethodReplacement.returnConstant(true));
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }

        // 4. SYSTEM UI HOOKS (Touch engine and Nav Bar)
        if (lpparam.packageName.equals("com.android.systemui")) {
            try {
                // NAVIGATION MODE CONTROLLER: Intercept change requests and force getter
                Class<?> navModeController = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.NavigationModeController", lpparam.classLoader);
                if (navModeController != null) {
                    XposedHelpers.findAndHookMethod(navModeController, "onRequestedNavigationModeChange", 
                        int.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.args[0] = 2; // Force requested mode to Gestures (2)
                            }
                        });

                    XposedHelpers.findAndHookMethod(navModeController, "getNavigationMode", 
                        XC_MethodReplacement.returnConstant(2)); 
                }

                // TOUCH ENGINE: Force Back Gesture regions to stay active
                Class<?> edgeBackHandler = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader);
                if (edgeBackHandler != null) {
                    XposedHelpers.findAndHookMethod(edgeBackHandler, "isHandlingGestures", 
                        XC_MethodReplacement.returnConstant(true));
                }

                // XIAOMI STUB: Bypass Xiaomi's "Touch Guard" which kills gestures on non-MIUI launchers
                Class<?> miuiGestureStub = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.MiuiGestureStubView", lpparam.classLoader);
                if (miuiGestureStub != null) {
                    XposedHelpers.findAndHookMethod(miuiGestureStub, "isGestureEnable", 
                        Context.class, XC_MethodReplacement.returnConstant(true));
                    
                    // Force the touch area height to be valid (not 0)
                    XposedHelpers.findAndHookMethod(miuiGestureStub, "getGestureHeight", 
                        XC_MethodReplacement.returnConstant(200)); 
                }

                // CORE UTILS: MIUI Specific Gesture Utils
                Class<?> miuiUtils = XposedHelpers.findClassIfExists("com.android.systemui.MiuiGestureUtils", lpparam.classLoader);
                if (miuiUtils != null) {
                    XposedHelpers.findAndHookMethod(miuiUtils, "isFsgMode", Context.class, XC_MethodReplacement.returnConstant(true));
                }

                // INTERNAL NAV: Xiaomi internal NavigationUtils for SystemUI
                Class<?> miuiNavUtils = XposedHelpers.findClassIfExists("com.android.systemui.NavigationUtils", lpparam.classLoader);
                if (miuiNavUtils != null) {
                    XposedHelpers.findAndHookMethod(miuiNavUtils, "isFsgMode", Context.class, XC_MethodReplacement.returnConstant(true));
                }

                // GESTURE STUB: Handles the back gesture touch area visibility
                Class<?> gestureStub = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.GestureStubView", lpparam.classLoader);
                if (gestureStub != null) {
                    XposedHelpers.findAndHookMethod(gestureStub, "isGestureEnable", Context.class, XC_MethodReplacement.returnConstant(true));
                }

                // AOSP FALLBACK: Modern AOSP Navigation Bar Controller for HyperOS
                Class<?> navBarController = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.NavigationBarController", lpparam.classLoader);
                if (navBarController != null) {
                    XposedHelpers.findAndHookMethod(navBarController, "isGesturalMode", int.class, XC_MethodReplacement.returnConstant(true));
                }

            } catch (Throwable t) { Log.e(TAG, "SystemUI Hook Failed: " + t.getMessage()); }
        }
    }
}