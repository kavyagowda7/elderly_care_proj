package com.example.elderlycare;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import android.content.pm.PackageManager;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FullScreenAlarmActivity extends AppCompatActivity {

    public static final String CHANNEL_ID = "elderlycare_reminders_channel";

    String alarmId, medicineId, userId, medicineName;
    ;
    MediaPlayer mp;

    TextView txtMedicine;
    Button btnTaken, btnSkip, btnSnooze;

    String selectedTone = "medicine_alarm";   // fallback tone resource name (raw/medicine_alarm.mp3)
    private static final int SNOOZE_MINUTES = 10;
    private static final long MISSED_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(10); // 10 minutes -> mark missed

    private Handler missedHandler = new Handler();
    private Runnable missedRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_fullscreen);

        // retrieve extras
        alarmId = getIntent().getStringExtra("alarmId");
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        txtMedicine = findViewById(R.id.txtMedicine);
        btnTaken = findViewById(R.id.btnTaken);
        btnSkip = findViewById(R.id.btnSkip);

        // NOTE: your layout must include a snooze button with id btnSnooze.
        btnSnooze = findViewById(R.id.btnSnooze);

        unlockScreen();
        // Fetch tone first, then play; also load alarm details
        fetchToneFromFirebase();
        loadAlarmDetails();

        // start missed timeout which will mark the alarm as missed after MISSED_TIMEOUT_MS
        startMissedTimeout();

        btnTaken.setOnClickListener(v -> {
            cancelMissedTimeout();
            markTaken();
        });

        btnSkip.setOnClickListener(v -> {
            // user just closes the full screen alarm — still keep it ringing until we stop
            stopSound();
            finish();
        });

        if (btnSnooze != null) {
            btnSnooze.setOnClickListener(v -> {
                cancelMissedTimeout();
                snoozeAlarm(SNOOZE_MINUTES);
                stopSound();
                finish();
            });
        }
    }

    // ------------------------------------------------------------
    // Unlock / show over lockscreen (best-effort)
    // ------------------------------------------------------------
    private void unlockScreen() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager kg = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (kg != null) kg.requestDismissKeyguard(this, null);
        }
    }

    // ------------------------------------------------------------
    // Load alarm details (medicine name) from RTDB
    // Path: alarms/{userId}/{alarmId}
    // ------------------------------------------------------------
    private void loadAlarmDetails() {
        if (userId == null || alarmId == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("alarms")
                .child(userId)
                .child(alarmId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot ds) {
                if (!ds.exists()) return;

                medicineId = ds.child("medicineId").getValue(String.class);
                medicineName = ds.child("medicineName").getValue(String.class);
                if (medicineName != null) {
                    txtMedicine.setText("Time to take " + medicineName);
                }

            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // ------------------------------------------------------------
    // Fetch tone name from RTDB (users/{uid}/medicationSchedule/reminderTone)
    // Then play the tone.
    // ------------------------------------------------------------
    private void fetchToneFromFirebase() {
        if (userId == null) {
            playSound(); // fallback
            return;
        }
        DatabaseReference toneRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("medicationSchedule")
                .child("reminderTone");

        toneRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String v = snapshot.getValue(String.class);
                    if (v != null && !v.isEmpty()) selectedTone = v;
                }
                playSound();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                playSound();
            }
        });
    }

    // ------------------------------------------------------------
    // Play the selected alarm tone (raw resource expected)
    // ------------------------------------------------------------
    private void playSound() {
        try {
            int resId = getResources().getIdentifier(selectedTone, "raw", getPackageName());
            if (resId == 0) resId = R.raw.harp; // fallback
            stopSound();
            mp = MediaPlayer.create(this, resId);
            if (mp != null) {
                mp.setLooping(true);
                mp.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopSound() {
        try {
            if (mp != null) {
                mp.stop();
                mp.release();
                mp = null;
            }
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------
    // Snooze: schedule a one-off alarm in SNOOZE_MINUTES from now
    // - Creates a new AlarmManager exact/while idle alarm
    // - Writes a simple "snoozed" child under alarms/{uid}/{newAlarmId} (optional)
    // ------------------------------------------------------------
    private void snoozeAlarm(int minutes) {
        try {
            long triggerAt = System.currentTimeMillis() + minutes * 60L * 1000L;

            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("alarmId", "snooze_" + triggerAt);
            intent.putExtra("medicineId", medicineId);
            intent.putExtra("medName", medicineName);
            intent.putExtra("dosage", "");

            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    ("snooze_" + medicineId + triggerAt).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                    // Permission not granted — fail silently
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pi
                    );
                } else {
                    am.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pi
                    );
                }
            }


            Toast.makeText(this, "Snoozed for " + minutes + " minutes", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void scheduleOneOffSnooze(int minutes, String medId, String alarmIdentifier) {
        try {
            long triggerAt = System.currentTimeMillis() + minutes * 60L * 1000L;
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("alarmId", alarmIdentifier);
            intent.putExtra("medicineId", medId);
            intent.putExtra("snoozed", true);

            int requestCode = ("snooze_" + alarmIdentifier + medId + triggerAt).hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // Missed logic: if user doesn't mark as taken within MISSED_TIMEOUT_MS
    // update DB and send a local notification to remind.
    // ------------------------------------------------------------
    private void startMissedTimeout() {
        cancelMissedTimeout();
        missedRunnable = () -> {
            markMissed();
        };
        missedHandler.postDelayed(missedRunnable, MISSED_TIMEOUT_MS);
    }

    private void cancelMissedTimeout() {
        if (missedRunnable != null) {
            missedHandler.removeCallbacks(missedRunnable);
            missedRunnable = null;
        }
    }

    private void markMissed() {
        stopSound();

        if (userId == null || alarmId == null) {
            // still notify locally
            sendMissedNotification("Missed medication", "You missed a medication reminder.");
            finish();
            return;
        }

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        root.child("alarms").child(userId).child(alarmId).child("status").setValue("missed");

        // also increment a missed count (optional) under medicines or user_analytics
        if (medicineId != null) {
            root.child("users")
                    .child(userId)
                    .child("current_prescriptions")
                    .child(medicineId)
                    .child("missedCount")
                    .setValue(ServerValue.increment(1));
        }

        sendMissedNotification("Missed medicine", "You missed taking your medicine. Please check.");

        finish();
    }

    private void sendMissedNotification(String title, String text) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_appointments)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text));

            NotificationManagerCompat nm = NotificationManagerCompat.from(this);

            // ⚠ Permission check (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    return; // No permission → don't send
                }
            }

            nm.notify((int) System.currentTimeMillis(), builder.build());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // Mark taken: update DB, increment progress, check course end
    // ------------------------------------------------------------
    private void markTaken() {
        stopSound();

        if (userId == null || medicineId == null) {
            finish();
            return;
        }

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        // 1) Mark alarm status as taken
        root.child("alarms").child(userId).child(alarmId).child("status").setValue("taken");

        // 2) Update prescription: takenToday = true, increment daysCompleted (smart progress)
        DatabaseReference presRef = root.child("users").child(userId).child("current_prescriptions").child(medicineId);

        presRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData == null) return Transaction.success(currentData);

                // takenToday
                currentData.child("takenToday").setValue(true);

                // daysCompleted (if not present, default to 0)
                Long daysCompleted = currentData.child("daysCompleted").getValue(Long.class);
                if (daysCompleted == null) daysCompleted = 0L;
                daysCompleted = daysCompleted + 1;
                currentData.child("daysCompleted").setValue(daysCompleted);

                // totalDays: used to determine completion
                Long totalDays = currentData.child("totalDays").getValue(Long.class);
                if (totalDays == null) totalDays = 0L;

                // if reached or exceeded totalDays => mark completed
                if (totalDays > 0 && daysCompleted >= totalDays) {
                    currentData.child("status").setValue("completed");
                }

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                // additional work: increment a global medicine progress node or taken count
                // increment takenCount under medicines (if you store structured medicine tracking)
                // Optionally remove / disable alarms when completed
                if (currentData != null && currentData.exists()) {
                    Long daysCompleted = currentData.child("daysCompleted").getValue(Long.class);
                    Long totalDays = currentData.child("totalDays").getValue(Long.class);
                    if (totalDays != null && daysCompleted != null && daysCompleted >= totalDays) {
                        // course finished — disable alarms for this medicine
                        disableAlarmsForMedicine(userId, medicineId);
                    }
                }
            }
        });

        finish();
    }

    // ------------------------------------------------------------
    // Disable alarms for a finished medicine:
    // - Remove alarms entries in /alarms/{uid} where medicineId matches
    // - Attempt to cancel AlarmManager PendingIntents if we can reconstruct them (best-effort)
    // ------------------------------------------------------------
    private void disableAlarmsForMedicine(String uid, String medId) {
        if (uid == null || medId == null) return;

        DatabaseReference alarmsRef = FirebaseDatabase.getInstance().getReference("alarms").child(uid);
        alarmsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String mid = child.child("medicineId").getValue(String.class);
                    String key = child.getKey();
                    String timeStr = null;
                    Object tObj = child.child("timeStr").getValue();
                    if (tObj != null) timeStr = tObj.toString();

                    if (mid != null && mid.equals(medId)) {
                        // remove DB entry
                        child.getRef().removeValue();

                        // attempt to cancel pending intent for this alarm if we can
                        // If timeStr exists, we try to reconstruct request code like your scheduler uses
                        if (timeStr != null) {
                            try {
                                int requestCode = (medId + timeStr).hashCode();
                                Intent intent = new Intent(FullScreenAlarmActivity.this, ReminderReceiver.class);
                                PendingIntent pi = PendingIntent.getBroadcast(FullScreenAlarmActivity.this, requestCode, intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
                                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                                if (am != null) am.cancel(pi);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        cancelMissedTimeout();
        stopSound();
        super.onDestroy();
    }
}
