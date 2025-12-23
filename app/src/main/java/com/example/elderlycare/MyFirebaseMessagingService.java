package com.example.elderlycare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String CALL_CHANNEL = "CALL_CHANNEL";

    @Override
    public void onMessageReceived(RemoteMessage message) {

        Log.e("SERVICE_CHECK", "SERVICE FILE IS RUNNING");
        Log.d("FCM_DEBUG", "DATA: " + message.getData());

        Map<String, String> data = message.getData();
        String type = data.get("type");

        // 🔥 VIDEO CALL HANDLING — FULL SCREEN
        if ("video_call".equals(type)) {

            Log.e("CALL_DEBUG", "VIDEO CALL BRANCH TRIGGERED");
            createCallChannel();

            showIncomingCallUI(data);
            return;  // VERY IMPORTANT → prevents overriding full-screen call
        }

        // 🔔 NORMAL NOTIFICATIONS (Appointment, etc.)
        String title = data.get("title");
        String body = data.get("body");

        // ⭐⭐⭐ RESTORED APPOINTMENT NOTIFICATION LOGIC ⭐⭐⭐
        if ("appointment_request".equals(type) || "appointment_status".equals(type)) {
            showNotification(title, body, type);
            return;
        }

        // Fallback for any other notifications
        showNotification(title, body, type);

        if (message.getData() != null &&
                "appointment_reminder".equals(message.getData().get("type"))) {

            showNotification(
                    "Appointment Reminder",
                    message.getData().get("body"),
                    "appointment"
            );
        }
        if ("doctor_video_reminder".equals(message.getData().get("type"))) {
            showNotification(
                    "Video Consultation Reminder",
                    message.getData().get("body"),
                    "video"
            );
        }


    }


    // ================================================================
    // 📞 Full Screen Incoming Call (WhatsApp style)
    // ================================================================
    private void showIncomingCallUI(Map<String, String> data) {

        String roomId = data.get("roomId");
        String doctorUid = data.get("doctorUid");
        String appointmentId = data.get("appointmentId");

        Intent intent = new Intent(this, IncomingCallActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        intent.putExtra("roomId", roomId);
        intent.putExtra("doctorUid", doctorUid);
        intent.putExtra("appointmentId", appointmentId);

        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                this,
                12345,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CALL_CHANNEL)
                        .setSmallIcon(R.drawable.ic_call)
                        .setContentTitle("Incoming Video Call")
                        .setContentText("Tap to answer")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setOngoing(true)
                        .setAutoCancel(true)
                        .setFullScreenIntent(fullScreenIntent, true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(5555, builder.build());
    }


    // ================================================================
    // 📞 Notification Channel for Calls
    // ================================================================
    private void createCallChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Uri soundUri =
                    Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.ringtone);

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel channel =
                    new NotificationChannel(
                            CALL_CHANNEL,
                            "Incoming Calls",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription("Video call alerts");
            channel.setSound(soundUri, attrs);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }


    // ================================================================
    // 🔔 OLD NOTIFICATION SYSTEM (Appointment alerts)
    // ================================================================
    private void showNotification(String title, String body, String type) {

        Log.e("NOTIF_DEBUG", "NORMAL NOTIFICATION CALLED for type=" + type);

        String channelId = "eldercare_channel";
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Intent intent;

        if ("appointment_request".equals(type)) {
            intent = new Intent(this, DoctorAppointmentsActivity.class);

        } else if ("appointment_status".equals(type)) {
            intent = new Intent(this, ElderAppointmentsActivity.class);

        } else {
            intent = new Intent(this, RoleSelectionActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            channelId,
                            "ElderCare Alerts",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setSound(sound, null);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_bell)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setSound(sound)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
