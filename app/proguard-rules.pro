# 1. Xposed Entry Point Protection
# Ensures the class defined in assets/xposed_init is never renamed or removed
-keep class com.hyperos.gesturefix.HyperOSLauncherSpoofer { *; }

# 2. Activity Hook Protection
# This is critical. If R8 "inlines" this method because it thinks it always returns false, 
# Xposed will have no method to hook into.
-keepclassmembers class com.hyperos.gesturefix.MainActivity {
    public boolean isModuleActive();
}

# 3. Xposed API Framework
# Keep the Xposed API and ignore warnings about missing system classes it references
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# 4. Reflection and Metadata Protection
# Prevents R8 from stripping attributes that Xposed uses to identify the module
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
    @de.robv.android.xposed.hook.* <methods>;
}

# 5. AndroidX/Support Library Safety
# Prevents stripping of classes used by FileProvider and AppCompat
-keep class androidx.core.content.FileProvider { *; }
-dontwarn androidx.core.**