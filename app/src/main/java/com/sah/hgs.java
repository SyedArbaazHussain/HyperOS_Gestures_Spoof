package com.sah;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

public class hgs extends XposedModule {

    private static final String TAG = "HGS_LOG";
    private static final int NAV_BAR_MODE_GESTURAL = 2;

    public hgs(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam lp) {
        String packageName = lp.getPackageName();

        // 1. SELF-HOOK: Verifies activation status in your app's UI
        if (packageName.equals("com.sah.hgs")) {
            try {
                hook(lp.getClassLoader().loadClass("com.sah.main").getDeclaredMethod("isModuleActive"), 
                     ConstantTrueHooker.class);
            } catch (Exception e) {
                Log.e(TAG, "UI Self-Hook Failed", e);
            }
        }

        // 2. SYSTEM FRAMEWORK: Global runtime hooks for all processes
        if (packageName.equals("system")) {
            applySystemFrameworkHooks(lp);
        }

        // 3. ANDROID: High-level System Server logic (OMS, WMS, AMS)
        if (packageName.equals("android")) {
            applyFrameworkHooks(lp);
        }

        // 4. SYSTEM UI: Navigation bar and gesture engine management
        if (packageName.equals("com.android.systemui")) {
            applySystemUIHooks(lp);
        }
    }

    private void applySystemFrameworkHooks(PackageLoadedParam lp) {
        try {
            ClassLoader cl = lp.getClassLoader();
            // Global enforcement of navigation mode at the View configuration level
            // This tells every app that the system is currently in Gesture mode
            hook(cl.loadClass("android.view.ViewConfiguration").getDeclaredMethod("isDefaultScrollCaptureEnabled"), 
                 ConstantTrueHooker.class);
            
            Log.d(TAG, "Global System Framework hooked successfully");
        } catch (Throwable t) {
            Log.e(TAG, "System Framework Hook Error", t);
        }
    }

    private void applyFrameworkHooks(PackageLoadedParam lp) {
        try {
            ClassLoader cl = lp.getClassLoader();

            // A. SILENCE HWC SPAM: Prevents 'getLuts failed' from clogging logs and causing lag
            try {
                hook(cl.loadClass("android.hardware.display.IDisplayManager$Stub$Proxy")
                        .getDeclaredMethod("getCompositionLuts"), SilenceLutsHooker.class);
            } catch (NoSuchMethodException e) {
                hook(cl.loadClass("android.hardware.display.DisplayManager")
                        .getDeclaredMethod("getCompositionLuts"), SilenceLutsHooker.class);
            }

            // B. LAUNCHER SPOOF: Tricks HyperOS into thinking any launcher is gesture-compatible
            hook(cl.loadClass("com.android.server.wm.ActivityTaskManagerService")
                    .getDeclaredMethod("isRecentsComponentHomeActivity", int.class), ConstantTrueHooker.class);

            // C. OVERLAY ENFORCEMENT: Hard-enables the gestural navigation overlay
            hook(cl.loadClass("com.android.server.om.OverlayManagerService")
                    .getDeclaredMethod("setEnabled", String.class, boolean.class, int.class), OverlayEnforcementHooker.class);

            // D. HOME PROTECTION: Prevents fallback loops back to Stock MIUI Home
            hook(cl.loadClass("com.android.server.wm.ActivityTaskManagerService")
                    .getDeclaredMethod("updateDefaultHomeActivity", ComponentName.class), HomeProtectionHooker.class);

        } catch (Throwable t) {
            Log.e(TAG, "Framework (system_server) Hook Error", t);
        }
    }

    private void applySystemUIHooks(PackageLoadedParam lp) {
        try {
            ClassLoader cl = lp.getClassLoader();

            // E. NAVIGATION MODE LOCK: Setter and Getter hooks for SystemUI
            hook(cl.loadClass("com.android.systemui.navigationbar.NavigationModeController")
                    .getDeclaredMethod("onRequestedNavigationModeChange", int.class), NavModeForcerHooker.class);

            hook(cl.loadClass("com.android.systemui.navigationbar.NavigationModeController")
                    .getDeclaredMethod("getNavigationMode"), ConstantTwoHooker.class);

            // F. GESTURE STUB: Forces Xiaomi's MiuiGestureStubView to remain active
            hook(cl.loadClass("com.android.systemui.statusbar.phone.MiuiGestureStubView")
                    .getDeclaredMethod("isGestureEnable", Context.class), ConstantTrueHooker.class);

        } catch (Throwable t) {
            Log.e(TAG, "SystemUI Hook Error", t);
        }
    }

    // --- HOOKER IMPLEMENTATIONS ---

    @XposedHooker
    public static class SilenceLutsHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            // Stops the HWC poll immediately to prevent display micro-stutter
            cb.returnAndSkip(null);
        }
    }

    @XposedHooker
    public static class ConstantTrueHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            cb.returnAndSkip(true);
        }
    }

    @XposedHooker
    public static class ConstantTwoHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            cb.returnAndSkip(NAV_BAR_MODE_GESTURAL);
        }
    }

    @XposedHooker
    public static class NavModeForcerHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            cb.getArgs()[0] = NAV_BAR_MODE_GESTURAL;
        }
    }

    @XposedHooker
    public static class OverlayEnforcementHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            String pkg = (String) cb.getArgs()[0];
            if (pkg != null && pkg.contains("navbar.gestural")) {
                cb.getArgs()[1] = true; // Force 'enable' parameter to true
            }
        }
    }

    @XposedHooker
    public static class HomeProtectionHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            ComponentName cn = (ComponentName) cb.getArgs()[0];
            if (cn != null && (cn.getPackageName().contains("miui.home") || cn.getPackageName().contains("mi.launcher"))) {
                cb.returnAndSkip(null); 
            }
        }
    }
}