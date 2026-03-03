package com.hyperos.gesturefix;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private TextView logView;

    public boolean isModuleActive() { return false; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView status = findViewById(R.id.tvStatus);
        logView = new TextView(this); // Quick debug view
        
        if (isModuleActive()) {
            status.setText("STATUS: INJECTED");
            status.setTextColor(Color.GREEN);
        } else {
            status.setText("STATUS: NOT HOOKED");
            status.setTextColor(Color.RED);
        }

        // Start a thread to read logs
        new Thread(this::updateLogs).start();
    }

    private void updateLogs() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d HGS_DEBUG:V *:S");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            runOnUiThread(() -> {
                // You can add a ScrollView/TextView in XML or just Toast this
                // For now, check your 'logcat' in Android Studio for "HGS_DEBUG"
            });
        } catch (Exception ignored) {}
    }
}