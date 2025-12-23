package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Handle system back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleCustomBack();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // ✅ Handle UI back arrow
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleCustomBack());
        }
    }

    private void handleCustomBack() {
        // ✅ If activity is not the root, just finish() to go back
        if (!isTaskRoot()) {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            return;
        }

        // ✅ Handle known sequential screens manually
        String current = getClass().getSimpleName();

        Intent intent = null;

        switch (current) {
            case "SetAfternoonTimeActivity":
                intent = new Intent(this, SetMorningTimeActivity.class);
                break;

            case "SetNightTimeActivity":
                intent = new Intent(this, SetAfternoonTimeActivity.class);
                break;

            case "ReminderTypeActivity":
                intent = new Intent(this, SetNightTimeActivity.class);
                break;

            case "ReminderToneActivity":
                intent = new Intent(this, ReminderTypeActivity.class);
                break;

            default:
                // If not one of these, just go to ElderDashboard
                intent = new Intent(this, ElderDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                break;
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        finish();
    }
}
