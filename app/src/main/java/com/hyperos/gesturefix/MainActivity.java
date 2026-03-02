package com.hyperos.gesturefix;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // LSPosed will intercept this method and force it to return true
    public boolean isModuleActive() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvStatus = findViewById(R.id.tvStatus);

        if (isModuleActive()) {
            tvStatus.setText("Module is ACTIVE \nGestures Spoofed!");
            tvStatus.setTextColor(Color.parseColor("#00AA00")); // Green
        }
    }
}