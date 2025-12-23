package com.example.elderlycare;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_TONE = 1;
    private Switch switchRecurring;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        LinearLayout notificationTone = findViewById(R.id.notificationTone);
        LinearLayout recurringReminder = findViewById(R.id.recurringReminder);
        switchRecurring = findViewById(R.id.switchRecurring);

        notificationTone.setOnClickListener(v -> openTonePicker());
        switchRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, isChecked ? "Recurring reminders ON" : "Recurring reminders OFF", Toast.LENGTH_SHORT).show();
        });
    }

    private void openTonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Tone");
        startActivityForResult(intent, REQUEST_TONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TONE && resultCode == RESULT_OK) {
            Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (ringtoneUri != null) {
                Toast.makeText(this, "Tone selected!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
