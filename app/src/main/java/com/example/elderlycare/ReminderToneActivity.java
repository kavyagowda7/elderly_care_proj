package com.example.elderlycare;

import android.Manifest;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ReminderToneActivity extends AppCompatActivity {

    private RadioButton rbGentle, rbClassic, rbHarp, rbChime, rbVibration, rbCustom;
    private ImageView playGentle, playClassic, playHarp, playChime, playVibration, playCustom, btnBack;
    private Button btnFinish;
    private MediaPlayer mediaPlayer;
    private ImageView currentPlayButton;
    private Uri currentToneUri;
    private Uri selectedUri;
    private boolean isPlaying = false;

    private FirebaseAuth auth;
    private DatabaseReference ref;
    private ActivityResultLauncher<Intent> pickAudioLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_tone);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users");

        // Initialize UI
        rbGentle = findViewById(R.id.rbGentle);
        rbClassic = findViewById(R.id.rbClassic);
        rbHarp = findViewById(R.id.rbHarp);
        rbChime = findViewById(R.id.rbChime);
        rbVibration = findViewById(R.id.rbVibration);
        rbCustom = findViewById(R.id.rbCustom);

        playGentle = findViewById(R.id.playGentle);
        playClassic = findViewById(R.id.playClassic);
        playHarp = findViewById(R.id.playHarp);
        playChime = findViewById(R.id.playChime);
        playVibration = findViewById(R.id.playVibration);
        playCustom = findViewById(R.id.playCustom);
        btnBack = findViewById(R.id.btnBack);
        btnFinish = findViewById(R.id.btnFinish);

        // Request permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 1);
        else
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        // File picker for custom tone
        pickAudioLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedUri = result.getData().getData();
                        rbCustom.setChecked(true);
                        rbCustom.setText("Custom Tone Selected");
                        stopTone();
                    }
                });

        btnBack.setOnClickListener(v -> onBackPressed());

        // Set up play buttons
        setupPlayButton(playGentle, rbGentle, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.gentle));
        setupPlayButton(playClassic, rbClassic, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.classic));
        setupPlayButton(playHarp, rbHarp, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.harp));
        setupPlayButton(playChime, rbChime, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.chime));
        setupPlayButton(playVibration, rbVibration, null);

        playCustom.setOnClickListener(v -> {
            if (selectedUri != null) {
                if (isPlaying && currentToneUri != null && currentToneUri.equals(selectedUri)) {
                    stopTone();
                } else {
                    stopTone();
                    playTone(playCustom, selectedUri);
                    rbCustom.setChecked(true);
                }
            } else {
                Toast.makeText(this, "Select a tone first", Toast.LENGTH_SHORT).show();
            }
        });

        rbCustom.setOnClickListener(v -> openFilePicker());

        // Make RadioButtons exclusive manually
        CompoundButton.OnCheckedChangeListener exclusiveListener = (buttonView, isChecked) -> {
            if (isChecked) {
                uncheckAllExcept(buttonView);
            }
        };
        rbGentle.setOnCheckedChangeListener(exclusiveListener);
        rbClassic.setOnCheckedChangeListener(exclusiveListener);
        rbHarp.setOnCheckedChangeListener(exclusiveListener);
        rbChime.setOnCheckedChangeListener(exclusiveListener);
        rbVibration.setOnCheckedChangeListener(exclusiveListener);
        rbCustom.setOnCheckedChangeListener(exclusiveListener);

        btnFinish.setOnClickListener(v -> saveTone());
    }

    private void uncheckAllExcept(CompoundButton selected) {
        RadioButton[] all = {rbGentle, rbClassic, rbHarp, rbChime, rbVibration, rbCustom};
        for (RadioButton rb : all) {
            if (rb != selected) rb.setChecked(false);
        }
    }

    private void setupPlayButton(ImageView button, RadioButton relatedRadio, Uri toneUri) {
        button.setOnClickListener(v -> {
            if (toneUri != null) {
                if (isPlaying && currentToneUri != null && currentToneUri.equals(toneUri)) {
                    stopTone(); // Stop if same tone is playing
                } else {
                    stopTone(); // Stop any existing sound
                    playTone(button, toneUri);
                    relatedRadio.setChecked(true);
                }
            } else {
                Toast.makeText(this, "This option is vibration only.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void playTone(ImageView button, Uri uri) {
        try {
            stopTone();
            mediaPlayer = MediaPlayer.create(this, uri);
            if (mediaPlayer == null) {
                Toast.makeText(this, "Cannot play this tone", Toast.LENGTH_SHORT).show();
                return;
            }

            currentToneUri = uri;
            mediaPlayer.setOnCompletionListener(mp -> stopTone());
            mediaPlayer.start();
            button.setImageResource(R.drawable.ic_stop);
            currentPlayButton = button;
            isPlaying = true;

        } catch (Exception e) {
            Toast.makeText(this, "Error playing tone: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopTone() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        if (currentPlayButton != null) {
            currentPlayButton.setImageResource(R.drawable.ic_play);
            currentPlayButton = null;
        }

        currentToneUri = null;
        isPlaying = false;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        pickAudioLauncher.launch(intent);
    }

    private void saveTone() {
        String uid = auth.getCurrentUser().getUid();
        String selectedTone;

        if (rbGentle.isChecked()) selectedTone = "Gentle Alarm";
        else if (rbClassic.isChecked()) selectedTone = "Classic Bell";
        else if (rbHarp.isChecked()) selectedTone = "Harp";
        else if (rbChime.isChecked()) selectedTone = "Digital Chime";
        else if (rbVibration.isChecked()) selectedTone = "Vibration Only";
        else if (selectedUri != null) selectedTone = selectedUri.toString();
        else {
            Toast.makeText(this, "Please select a tone", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("reminderTone", selectedTone);

        ref.child(uid).child("medicationSchedule").updateChildren(map)
                .addOnSuccessListener(aVoid -> {
                    FirebaseDatabase.getInstance("https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("users")
                            .child(uid)
                            .child("onboardingStatus").child("reminderToneDone")

                            .setValue(true);

                    Toast.makeText(this, "Tone saved!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ReminderToneActivity.this, ElderDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTone();
    }
}
