package com.hyperos.gesturefix;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvAudit;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastAuditReport = ""; 

    /**
     * This method is hooked by LSPosed to return true.
     * If it remains false, the injection has failed.
     */
    public boolean isModuleActive() { 
        return false; 
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvAudit = findViewById(R.id.tvLogs); 
        Button btnFix = findViewById(R.id.btnFix);
        Button btnExport = findViewById(R.id.btnExport);

        updateLSPosedStatus();

        if (btnFix != null) {
            btnFix.setOnClickListener(v -> {
                runRootElevatedFix();
                // Delay re-audit to let SystemUI/Launcher finish restarting
                handler.postDelayed(this::runSystemAudit, 4000);
            });
        }

        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportAuditLog());
        }
        
        // Run initial audit on launch
        runSystemAudit();
    }

    private void updateLSPosedStatus() {
        if (tvStatus != null) {
            boolean active = isModuleActive();
            tvStatus.setText("LSPosed: " + (active ? "ACTIVE" : "INACTIVE"));
            tvStatus.setTextColor(active ? Color.GREEN : Color.RED);
        }
    }

    private void runSystemAudit() {
        StringBuilder report = new StringBuilder();
        report.append("<b>--- HYPEROS GESTURE AUDIT ---</b><br><br>");

        // 1. Check Overlay State
        String overlayRaw = checkCommandOutput("cmd overlay list | grep gestural");
        boolean isEnabled = overlayRaw.contains("[x]");
        report.append(formatAuditLine("AOSP Overlay (Enabled)", isEnabled));

        // 2. Check Database: navigation_mode (2 is gesture)
        String navMode = checkCommandOutput("settings get secure navigation_mode").trim();
        report.append(formatAuditLine("Nav Mode (Gesture=2): " + navMode, navMode.equals("2")));

        // 3. Check Database: force_fsg_nav_bar (1 is enabled)
        String fsgFlag = checkCommandOutput("settings get global force_fsg_nav_bar").trim();
        report.append(formatAuditLine("FSG Flag (ON=1): " + fsgFlag, fsgFlag.equals("1")));

        // 4. Check MIUI specific status
        String miuiFsg = checkCommandOutput("settings get secure miui_fsg_gesture_status").trim();
        report.append(formatAuditLine("MIUI FSG Status: " + miuiFsg, miuiFsg.equals("1")));

        // 5. Injection check
        report.append(formatAuditLine("LSPosed Injection Status", isModuleActive()));

        lastAuditReport = report.toString();
        if (tvAudit != null) {
            tvAudit.setText(Html.fromHtml(lastAuditReport, Html.FROM_HTML_MODE_COMPACT));
        }
        
        // Always refresh the top header status too
        updateLSPosedStatus();
    }

    private String formatAuditLine(String title, boolean passed) {
        String color = passed ? "#00FF00" : "#FF0000";
        String status = passed ? "[PASS] " : "[FAIL] ";
        return "<font color=\"" + color + "\">" + status + title + "</font><br>";
    }

    private String checkCommandOutput(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            return (line != null) ? line : "null";
        } catch (Exception e) { 
            return "error"; 
        }
    }

    private void runRootElevatedFix() {
        new Thread(() -> {
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());

                // Set database flags
                os.writeBytes("settings put secure navigation_mode 2\n");
                os.writeBytes("settings put global force_fsg_nav_bar 1\n");
                os.writeBytes("settings put secure sw_fs_gesture_fixed_mode 1\n");
                os.writeBytes("settings put secure sw_fs_gesture_navigation_mode 1\n");
                os.writeBytes("settings put secure miui_fsg_gesture_status 1\n");

                // Disable/Enable overlay to trigger system refresh
                os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.gestural\n");
                os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");
                os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.threebutton\n");

                // Restart SystemUI and Launcher
                os.writeBytes("pkill -f com.android.systemui\n");
                os.writeBytes("pkill -f com.miui.home\n"); 
                
                os.writeBytes("exit\n");
                os.flush();
                p.waitFor();

                handler.post(() -> Toast.makeText(MainActivity.this, "Fix Applied. Rebooting UI...", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(MainActivity.this, "Root Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void exportAuditLog() {
        if (lastAuditReport.isEmpty()) {
            Toast.makeText(this, "Run Audit first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Strip HTML for plain text export
        String plainText = lastAuditReport.replaceAll("<[^>]*>", "")
                                         .replace("[PASS]", "PASS:")
                                         .replace("[FAIL]", "FAIL:");
        try {
            File cachePath = new File(getCacheDir(), "logs");
            if (!cachePath.exists()) cachePath.mkdirs();
            
            File logFile = new File(cachePath, "gesture_audit.txt");
            FileOutputStream stream = new FileOutputStream(logFile);
            stream.write(plainText.getBytes());
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", logFile);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Audit Log"));
        } catch (Exception e) {
            Toast.makeText(this, "Export Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}