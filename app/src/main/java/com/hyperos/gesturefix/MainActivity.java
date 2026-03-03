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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvAudit, tvLogs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Process logcatProcess;
    private boolean isMonitoring = false;

    public boolean isModuleActive() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        tvStatus = findViewById(R.id.tvStatus);
        tvAudit = findViewById(R.id.tvAudit); // New fixed status view
        tvLogs = findViewById(R.id.tvLogs);   // New selectable log view
        
        Button btnFix = findViewById(R.id.btnFix);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnExport = findViewById(R.id.btnExport);
        Button btnStop = findViewById(R.id.btnStop);

        // Set log view as selectable
        if (tvLogs != null) {
            tvLogs.setTextIsSelectable(true);
        }

        updateLSPosedStatus();
        startLogcatMonitor();

        if (btnFix != null) {
            btnFix.setOnClickListener(v -> {
                new Thread(() -> {
                    ShellUtils.applyRootFix();
                    handler.post(() -> {
                        Toast.makeText(this, "Fix Applied. Rebooting UI...", Toast.LENGTH_SHORT).show();
                        if (!isMonitoring) startLogcatMonitor(); // Auto-restart logs if stopped
                    });
                    handler.postDelayed(this::runSystemAudit, 4500);
                }).start();
            });
        }

        if (btnStop != null) {
            btnStop.setOnClickListener(v -> stopLogcatMonitor());
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                if (tvLogs != null) {
                    tvLogs.setText("--- Logs Cleared ---\n");
                }
                Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportRealLogs());
        }

        runSystemAudit();
    }

    private void startLogcatMonitor() {
        if (isMonitoring) return;
        isMonitoring = true;
        if (tvLogs != null) tvLogs.append("\n[Monitor Started]\n");

        new Thread(() -> {
            try {
                // Monitor HGS_LOG and all System Errors
                logcatProcess = Runtime.getRuntime().exec(new String[]{"su", "-c", "logcat HGS_LOG:D *:E"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
                String line;

                while (isMonitoring && (line = reader.readLine()) != null) {
                    final String logLine = line;
                    handler.post(() -> {
                        if (tvLogs != null) {
                            tvLogs.append(logLine + "\n");
                            
                            // Auto-stop if critical failures are detected to preserve log state
                            if (logLine.contains("Permission denied") || logLine.contains("FATAL EXCEPTION")) {
                                // Optional: stopLogcatMonitor(); 
                            }

                            if (tvLogs.getLineCount() > 1500) {
                                tvLogs.setText("[Truncated]\n" + tvLogs.getText().toString().substring(2000));
                            }
                        }
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    if (tvLogs != null) tvLogs.append("Logcat Error: " + e.getMessage() + "\n");
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
        if (tvLogs != null) tvLogs.append("\n[Monitor Stopped]\n");
        Toast.makeText(this, "Log Capture Stopped", Toast.LENGTH_SHORT).show();
    }

    private void runSystemAudit() {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("AOSP Overlay: ").append(checkStatus("cmd overlay list | grep gestural")).append("\n");
            report.append("Nav Mode (2): ").append(checkCommandOutput("settings get secure navigation_mode")).append("\n");
            report.append("FSG Flag (1): ").append(checkCommandOutput("settings get global force_fsg_nav_bar")).append("\n");
            report.append("MIUI FSG: ").append(checkCommandOutput("settings get secure miui_fsg_gesture_status")).append("\n");
            report.append("Injection: ").append(isModuleActive() ? "ACTIVE" : "FAILED");

            handler.post(() -> {
                if (tvAudit != null) {
                    tvAudit.setText(report.toString());
                }
                updateLSPosedStatus();
            });
        }).start();
    }

    private String checkStatus(String cmd) {
        return checkCommandOutput(cmd).contains("[x]") ? "ENABLED" : "DISABLED";
    }

    private String checkCommandOutput(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            return (line != null) ? line.trim() : "null";
        } catch (Exception e) { return "error"; }
    }

    private void exportRealLogs() {
        if (tvLogs == null || tvLogs.getText().toString().isEmpty()) return;
        
        String logContent = tvLogs.getText().toString();
        try {
            File file = new File(getCacheDir(), "hyperos_gesture_logcat.txt");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(logContent.getBytes());
            stream.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Export Logcat"));
        } catch (Exception e) {
            Toast.makeText(this, "Export Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLSPosedStatus() {
        if (tvStatus != null) {
            boolean active = isModuleActive();
            tvStatus.setText("LSPosed: " + (active ? "ACTIVE" : "INACTIVE"));
            tvStatus.setTextColor(active ? Color.GREEN : Color.RED);
        }
    }

    @Override
    protected void onDestroy() {
        stopLogcatMonitor();
        super.onDestroy();
    }
}