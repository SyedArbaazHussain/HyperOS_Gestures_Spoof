package com.hyperos.gesturefix;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Internal check: No hooking required
    private boolean isModuleActive() {
        try {
            // This class only exists if Xposed is active in this process
            Class.forName("de.robv.android.xposed.XposedBridge", false, getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvStatus = findViewById(R.id.tvStatus);

        if (isModuleActive()) {
            tvStatus.setText("Module Status: ACTIVE");
            tvStatus.setTextColor(Color.parseColor("#00AA00")); // Green
        } else {
            tvStatus.setText("Module Status: INACTIVE");
            tvStatus.setTextColor(Color.RED);
        }
    }
}