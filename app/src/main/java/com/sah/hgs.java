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

        if (packageName.equals("com.sah.hgs")) {
            try {
                hook(lp.getClassLoader().loadClass("com.sah.main").getDeclaredMethod("isModuleActive"), 
                     ConstantTrueHooker.class);
            } catch (Exception e) {
                Log.e(TAG, "Self-hook failed", e);
            }
        }

        if (packageName.equals("android")) {
            applyFrameworkHooks(lp);
        }

        if (packageName.equals("com.android.systemui")) {
            applySystemUIHooks(lp);
        }
    }

    private void applyFrameworkHooks(PackageLoadedParam lp) {
        try {
            ClassLoader cl = lp.getClassLoader();

            try {
                hook(cl.loadClass("android.hardware.display.IDisplayManager$Stub$Proxy")
                        .getDeclaredMethod("getCompositionLuts"), 
                        SilenceLutsHooker.class);
            } catch (NoSuchMethodException e) {
                hook(cl.loadClass("android.hardware.display.DisplayManager")
                        .getDeclaredMethod("getCompositionLuts"), 
                        SilenceLutsHooker.class);
            }

            hook(cl.loadClass("com.android.server.om.OverlayManagerService")
                    .getDeclaredMethod("setEnabled", String.class, boolean.class, int.class),
                    OverlayEnforcementHooker.class);

            hook(cl.loadClass("com.android.server.wm.ActivityTaskManagerService")
                    .getDeclaredMethod("updateDefaultHomeActivity", ComponentName.class),
                    HomeProtectionHooker.class);

        } catch (Throwable t) {
            Log.e(TAG, "Framework Hook Error", t);
        }
    }

    private void applySystemUIHooks(PackageLoadedParam lp) {
        try {
            ClassLoader cl = lp.getClassLoader();

            hook(cl.loadClass("com.android.systemui.navigationbar.NavigationModeController")
                    .getDeclaredMethod("onRequestedNavigationModeChange", int.class),
                    NavModeForcerHooker.class);

            hook(cl.loadClass("com.android.systemui.navigationbar.NavigationModeController")
                    .getDeclaredMethod("getNavigationMode"),
                    ConstantTwoHooker.class);

            hook(cl.loadClass("com.android.systemui.statusbar.phone.MiuiGestureStubView")
                    .getDeclaredMethod("isGestureEnable", Context.class),
                    ConstantTrueHooker.class);

        } catch (Throwable t) {
            Log.e(TAG, "SystemUI Hook Error", t);
        }
    }

    // --- HOOKER IMPLEMENTATIONS ---

    @XposedHooker
    public static class SilenceLutsHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            // Returns null and skips the transaction to prevent HWC panic
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
            // Force the requested mode to 2 before the method executes
            cb.getArgs()[0] = NAV_BAR_MODE_GESTURAL;
        }
    }

    @XposedHooker
    public static class OverlayEnforcementHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback cb) {
            String pkg = (String) cb.getArgs()[0];
            if (pkg != null && pkg.contains("navbar.gestural")) {
                cb.getArgs()[1] = true; // Always force 'enabled' to true
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