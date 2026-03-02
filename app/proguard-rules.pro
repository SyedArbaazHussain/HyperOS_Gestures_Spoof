# Prevent R8 from removing or renaming your Xposed entry point
-keep class com.hyperos.gesturefix.HyperOSLauncherSpoofer { *; }

# Prevent R8 from removing the internal activation check in MainActivity
-keepclassmembers class com.hyperos.gesturefix.MainActivity {
    public boolean isModuleActive();
}

# Keep Xposed API classes
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**