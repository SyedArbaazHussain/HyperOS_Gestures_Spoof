package com.hyperos.gesturefix;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvLogs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // This method is hooked by LSPosed to return true
    public boolean isModuleActive() { return false; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvLogs = findViewById(R.id.tvLogs);
        Button btnFix = findViewById(R.id.btnFix);

        // Update UI based on LSPosed status
        if (isModuleActive()) {
            tvStatus.setText("LSPosed: ACTIVE");
            tvStatus.setTextColor(Color.GREEN);
        } else {
            tvStatus.setText("LSPosed: INACTIVE");
            tvStatus.setTextColor(Color.RED);
        }

        if (btnFix != null) {
            btnFix.setOnClickListener(v -> runRootElevatedFix());
        }
        
        startLogStream();
    }

    private void runRootElevatedFix() {
        new Thread(() -> {
            try {
                // Start the root process
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());

                // Write the commands to the root shell
                os.writeBytes("settings put global force_fsg_nav_bar 1\n");
                os.writeBytes("settings put global hide_gesture_line 1\n");
                os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");
                os.writeBytes("pkill -f com.android.systemui\n");
                
                os.writeBytes("exit\n");
                os.flush();

                // FIX: Call waitFor() on the Process 'p', not the DataOutputStream 'os'
                int result = p.waitFor(); 
                
                runOnUiThread(() -> {
                    if (tvLogs != null) {
                        tvLogs.append("\n[ROOT] Applied changes (Exit Code: " + result + "). Restarting SystemUI...");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (tvLogs != null) {
                        tvLogs.append("\n[ROOT ERROR] " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void startLogStream() {
        new Thread(() -> {
            try {
                // Fetch current logs for our specific tag
                Process process = Runtime.getRuntime().exec("logcat -d HGS_LOG:V *:S");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                String logs = sb.toString();
                
                handler.post(() -> {
                    if (tvLogs != null) {
                        tvLogs.setText(logs.isEmpty() ? "Waiting for system events..." : logs);
                    }
                });
            } catch (Exception ignored) {}

            // Repeat the log check every 2.5 seconds
            handler.postDelayed(this::startLogStream, 2500);
        }).start();
    }
}