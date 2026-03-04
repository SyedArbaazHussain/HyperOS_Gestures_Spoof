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
        
        // 1. SELF-HOOK: Verify module is active in App UI
        if (lpparam.packageName.equals("com.hyperos.gesturefix")) {
            XposedHelpers.findAndHookMethod("com.hyperos.gesturefix.MainActivity", lpparam.classLoader, 
                "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        // 2. SYSTEM FRAMEWORK HOOKS (android)
        if (lpparam.packageName.equals("android")) {
            try {
                // Persistent Overlay Enforcement
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

                // Default Home Protection (Prevent fallback to MIUI Home)
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

                // Global Settings Spoof
                Class<?> miuiSettings = XposedHelpers.findClassIfExists("android.provider.MiuiSettings$System", lpparam.classLoader);
                if (miuiSettings != null) {
                    XposedHelpers.findAndHookMethod(miuiSettings, "isSupportFullscreenGesture", 
                        Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }

        // 3. SETTINGS BYPASS (Bypasses verification in Xiaomi Misettings)
        if (lpparam.packageName.equals("com.xiaomi.misettings") || lpparam.packageName.equals("com.android.settings")) {
            try {
                Class<?> navUtils = XposedHelpers.findClassIfExists("miui.util.MiuiNavUtils", lpparam.classLoader);
                if (navUtils != null) {
                    XposedHelpers.findAndHookMethod(navUtils, "isDefaultSysLauncher", Context.class, XC_MethodReplacement.returnConstant(true));
                    XposedHelpers.findAndHookMethod(navUtils, "isSupportFullscreenGesture", Context.class, XC_MethodReplacement.returnConstant(true));
                }
            } catch (Throwable ignored) {}
        }

        // 4. SYSTEM UI HOOKS (The Gesture & Touch Engine)
        if (lpparam.packageName.equals("com.android.systemui")) {
            try {
                // STABILITY FIX: Prevent Notification Panel crash during config changes
                Class<?> npvcInjector = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.MiuiNotificationPanelViewControllerInjector", lpparam.classLoader);
                if (npvcInjector != null) {
                    XposedHelpers.findAndHookMethod(npvcInjector, "onConfigurationChanged", 
                        "android.content.res.Configuration", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            // Log and allow; just hooking this often stabilizes the animator caller
                            Log.d(TAG, "NPVC Config Change Hooked - Stabilizing Animator");
                        }
                    });
                }

                // FORCE NAV MODE: Prevent system from reverting to buttons
                Class<?> navModeController = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.NavigationModeController", lpparam.classLoader);
                if (navModeController != null) {
                    XposedHelpers.findAndHookMethod(navModeController, "onRequestedNavigationModeChange", 
                        int.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.args[0] = 2; // Force GESTURAL (2)
                            }
                        });
                    XposedHelpers.findAndHookMethod(navModeController, "getNavigationMode", 
                        XC_MethodReplacement.returnConstant(2)); 
                }

                // EDGE HANDLERS: Force physical back swipe detection
                Class<?> edgeBackHandler = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader);
                if (edgeBackHandler != null) {
                    XposedHelpers.findAndHookMethod(edgeBackHandler, "isHandlingGestures", 
                        XC_MethodReplacement.returnConstant(true));
                }

                Class<?> miuiEdgeBack = XposedHelpers.findClassIfExists("com.android.systemui.navigationbar.gestural.MiuiEdgeBackGestureHandler", lpparam.classLoader);
                if (miuiEdgeBack != null) {
                    XposedHelpers.findAndHookMethod(miuiEdgeBack, "isHandlingGestures", 
                        XC_MethodReplacement.returnConstant(true));
                }

                // TOUCH STUBS: Force gesture regions to bind to the display composer
                Class<?> miuiGestureStub = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.MiuiGestureStubView", lpparam.classLoader);
                if (miuiGestureStub != null) {
                    XposedHelpers.findAndHookMethod(miuiGestureStub, "isGestureEnable", 
                        Context.class, XC_MethodReplacement.returnConstant(true));
                    XposedHelpers.findAndHookMethod(miuiGestureStub, "getGestureHeight", 
                        XC_MethodReplacement.returnConstant(200)); 
                }

                Class<?> gestureStub = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.GestureStubView", lpparam.classLoader);
                if (gestureStub != null) {
                    XposedHelpers.findAndHookMethod(gestureStub, "isGestureEnable", Context.class, XC_MethodReplacement.returnConstant(true));
                }

                // CORE FSG UTILS
                Class<?> miuiUtils = XposedHelpers.findClassIfExists("com.android.systemui.MiuiGestureUtils", lpparam.classLoader);
                if (miuiUtils != null) {
                    XposedHelpers.findAndHookMethod(miuiUtils, "isFsgMode", Context.class, XC_MethodReplacement.returnConstant(true));
                }

            } catch (Throwable t) { Log.e(TAG, "SystemUI Hook Failed: " + t.getMessage()); }
        }
    }
}