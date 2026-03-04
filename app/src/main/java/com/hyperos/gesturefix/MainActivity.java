package com.hyperos.gesturefix;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvAudit, tvLogs;
    private ScrollView scrollLog;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Process logcatProcess;
    private boolean isMonitoring = false;

    /**
     * Hooked by LSPosed. Returns true if injection is successful.
     */
    public boolean isModuleActive() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        tvStatus = findViewById(R.id.tvStatus);
        tvAudit = findViewById(R.id.tvAudit); 
        tvLogs = findViewById(R.id.tvLogs);   
        scrollLog = (ScrollView) tvLogs.getParent();

        // Main Fix Button
        Button btnFix = findViewById(R.id.btnFix);
        
        // Terminal/Log Buttons
        Button btnStop = findViewById(R.id.btnStop);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnExport = findViewById(R.id.btnExport);

        // Configuration: Make logs selectable and copyable
        if (tvLogs != null) {
            tvLogs.setTextIsSelectable(true);
        }

        updateLSPosedStatus();
        startLogcatMonitor();

        // 1. APPLY FIX - High-visibility main action
        if (btnFix != null) {
            btnFix.setOnClickListener(v -> {
                new Thread(() -> {
                    ShellUtils.applyRootFix();
                    handler.post(() -> {
                        Toast.makeText(this, "Enforcing Gestures...", Toast.LENGTH_SHORT).show();
                        if (!isMonitoring) startLogcatMonitor(); 
                    });
                    handler.postDelayed(this::runSystemAudit, 4500);
                }).start();
            });
        }

        // 2. STOP LOG - Terminal Control
        if (btnStop != null) {
            btnStop.setOnClickListener(v -> stopLogcatMonitor());
        }

        // 3. CLEAR LOGS - Terminal Control
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                if (tvLogs != null) {
                    tvLogs.setText("--- Terminal Cleared ---\n");
                }
                Toast.makeText(this, "Terminal Cleared", Toast.LENGTH_SHORT).show();
            });
        }

        // 4. EXPORT LOGS - Diagnostic Control
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportRealLogs());
        }

        // Initial system check
        runSystemAudit();
    }

    private void runSystemAudit() {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            
            // 1. Overlay Check
            String overlayRaw = checkCommandOutput("cmd overlay list | grep gestural");
            int overlayState = overlayRaw.contains("[x]") ? 1 : (overlayRaw.contains("[ ]") ? 0 : -1);
            report.append(formatAuditLine("AOSP Gesture Overlay", overlayState));

            // 2. Nav Mode
            String navMode = checkCommandOutput("settings get secure navigation_mode").trim();
            int navState = navMode.equals("2") ? 1 : (navMode.equals("null") ? -1 : 0);
            report.append(formatAuditLine("System Navigation Mode", navState));

            // 3. FSG Flag
            String fsgFlag = checkCommandOutput("settings get global force_fsg_nav_bar").trim();
            int fsgState = fsgFlag.equals("1") ? 1 : (fsgFlag.equals("null") ? -1 : 0);
            report.append(formatAuditLine("Global FSG Flag", fsgState));

            // 4. MIUI FSG
            String miuiFsg = checkCommandOutput("settings get secure miui_fsg_gesture_status").trim();
            int miuiState = miuiFsg.equals("1") ? 1 : (miuiFsg.equals("null") ? -1 : 0);
            report.append(formatAuditLine("MIUI FSG Status", miuiState));

            // 5. Injection Status
            report.append(formatAuditLine("LSPosed Hook Injection", isModuleActive() ? 1 : 0));

            handler.post(() -> {
                if (tvAudit != null) {
                    tvAudit.setText(Html.fromHtml(report.toString(), Html.FROM_HTML_MODE_COMPACT));
                }
                updateLSPosedStatus();
            });
        }).start();
    }

    /**
     * Formats audit lines with specific colors:
     * Green (1) = Success
     * Red (0) = Fail
     * Yellow (-1) = Unknown/Detached
     */
    private String formatAuditLine(String label, int state) {
        String color;
        String icon;
        
        switch (state) {
            case 1: 
                color = "#4CAF50"; // Green
                icon = "✔ ";
                break;
            case 0: 
                color = "#F44336"; // Red
                icon = "✘ ";
                break;
            default: 
                color = "#FFEB3B"; // Yellow
                icon = "❓ ";
                break;
        }
        
        return "<font color=\"" + color + "\">" + icon + label + "</font><br/>";
    }

    private void startLogcatMonitor() {
        if (isMonitoring) return;
        isMonitoring = true;
        if (tvLogs != null) tvLogs.append("\n[Log Monitor Active]\n");

        new Thread(() -> {
            try {
                logcatProcess = Runtime.getRuntime().exec(new String[]{"su", "-c", "logcat HGS_LOG:D *:E"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
                String line;

                while (isMonitoring && (line = reader.readLine()) != null) {
                    final String logLine = line;
                    handler.post(() -> {
                        if (tvLogs != null) {
                            tvLogs.append(logLine + "\n");
                            
                            if (scrollLog != null) {
                                scrollLog.fullScroll(ScrollView.FOCUS_DOWN);
                            }

                            if (tvLogs.getLineCount() > 2000) {
                                String current = tvLogs.getText().toString();
                                tvLogs.setText("[Buffer Purged]\n" + current.substring(current.length() / 2));
                            }
                        }
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    if (tvLogs != null) tvLogs.append("Terminal Error: " + e.getMessage() + "\n");
                });
            }
        }).start();
    }

    private void stopLogcatMonitor() {
        isMonitoring = false;
        if (logcatProcess != null) {
            logcatProcess.destroy();
            logcatProcess = null;
        }
        if (tvLogs != null) tvLogs.append("\n[Log Monitor Suspended]\n");
        Toast.makeText(this, "Monitor Suspended", Toast.LENGTH_SHORT).show();
    }

    private String checkCommandOutput(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            return (line != null) ? line.trim() : "null";
        } catch (Exception e) { return "err"; }
    }

    private void exportRealLogs() {
        if (tvLogs == null || tvLogs.getText().toString().isEmpty()) return;
        
        String logContent = tvLogs.getText().toString();
        try {
            File cachePath = new File(getCacheDir(), "logs");
            if (!cachePath.exists()) cachePath.mkdirs();

            File logFile = new File(cachePath, "hgs_diagnostic_report.txt");
            FileOutputStream stream = new FileOutputStream(logFile);
            stream.write(logContent.getBytes());
            stream.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", logFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Diagnostic Report"));
        } catch (Exception e) {
            Toast.makeText(this, "Export Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLSPosedStatus() {
        if (tvStatus != null) {
            boolean active = isModuleActive();
            tvStatus.setText("LSPosed: " + (active ? "ACTIVE" : "INACTIVE"));
            tvStatus.setTextColor(active ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        }
    }

    @Override
    protected void onDestroy() {
        stopLogcatMonitor();
        super.onDestroy();
    }
}