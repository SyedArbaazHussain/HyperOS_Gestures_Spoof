# 1. Essential: Preserve the Xposed entry point and its requirements
-keep class com.hyperos.gesturefix.HyperOSLauncherSpoofer { *; }
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage { *; }
-keep class * implements de.robv.android.xposed.IXposedHookZygoteInit { *; }
-keep class * implements de.robv.android.xposed.IXposedHookInitPackageResources { *; }

# 2. Critical: Preserve the Heartbeat Method in MainActivity
# Without this, the Xposed module can't find the method to "Veto" its return value
-keepclassmembers class com.hyperos.gesturefix.MainActivity {
    public boolean isModuleActive();
}

# 3. UI Stability: Preserve Fragment constructors
# FragmentManager needs to reflectively call the empty constructor to restore tabs
-keep public class * extends androidx.fragment.app.Fragment {
    public <init>();
}

# 4. ViewPager2 & RecyclerView optimizations
# Keep the ViewHolder and layout managers used by the Launcher List
-keep class androidx.recyclerview.widget.LinearLayoutManager { *; }
-keepclassmembers class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder {
    public <init>(android.view.View);
}

# 5. Xposed API Suppressions
# This prevents warnings about the Xposed API being missing during the build
-dontwarn de.robv.android.xposed.**