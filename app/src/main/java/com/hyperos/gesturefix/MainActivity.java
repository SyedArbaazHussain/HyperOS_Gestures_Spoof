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
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            // 1. Force the Global Navigation Mode to '2' (Gestures)
            os.writeBytes("settings put secure navigation_mode 2\n");
            
            // 2. Enable the AOSP Gestural Overlay (This is the magic fix)
            os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");

            // 3. Disable the Button navigation overlay just in case
            os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.threebutton\n");

            // 4. Force Xiaomi FSG Flag
            os.writeBytes("settings put global force_fsg_nav_bar 1\n");

            // 5. Restart SystemUI to bind the new overlay
            os.writeBytes("pkill -f com.android.systemui\n");
            
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            
            runOnUiThread(() -> tvLogs.append("\n[ROOT] AOSP Gestures forced via overlay."));
        } catch (Exception e) {
            runOnUiThread(() -> tvLogs.append("\n[ROOT ERROR] " + e.getMessage()));
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