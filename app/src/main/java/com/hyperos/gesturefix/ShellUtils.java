package com.hyperos.gesturefix;

import java.io.DataOutputStream;

public class ShellUtils {
    public static void applyRootFix() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            // 1. CORE GESTURE CONFIGURATION (Force Settings)
            os.writeBytes("settings put secure navigation_mode 2\n");
            os.writeBytes("settings put global force_fsg_nav_bar 1\n");
            os.writeBytes("settings put secure miui_fsg_gesture_status 1\n");
            os.writeBytes("settings put secure hide_gesture_line 0\n");

            // 2. HARDWARE COMPOSER & SURFACEFLINGER RESET
            // Fixes the 'getLuts failed' and transaction errors in logs
            os.writeBytes("wm size reset\n");
            os.writeBytes("wm density reset\n");
            
            // Nuclear option: Restart the display composer service
            os.writeBytes("pkill -f vendor.qti.hardware.display.composer\n");
            os.writeBytes("pkill -f android.hardware.graphics.composer\n");

            // 3. NUCLEAR OVERLAY REFRESH
            os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.gestural\n");
            os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");
            os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.threebutton\n");

            // 4. TOUCH INTERACTION & INPUT HAL RESET
            // Clears the hardware input cache to bind new gesture touch areas
            os.writeBytes("settings put secure edge_handwriting_enabled 1\n");
            os.writeBytes("pkill -f android.hardware.input.processor\n");

            // 5. SYSTEM PROCESS RESTART
            os.writeBytes("pkill -f com.android.systemui\n");
            os.writeBytes("pkill -f com.miui.home\n");
            os.writeBytes("pkill -f com.xiaomi.misettings\n");

            // 6. RE-COMPOSITION LOOP (The "Fix" while system respawns)
            // We loop a harmless display command to keep HWC active during transition
            os.writeBytes("for i in {1..10}; do am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS; sleep 0.5; done &\n");

            // 7. CLEANUP
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
        } catch (Exception ignored) {}
    }
}