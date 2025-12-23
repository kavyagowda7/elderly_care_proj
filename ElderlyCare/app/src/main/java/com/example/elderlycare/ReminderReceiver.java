package com.example.elderlycare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "med_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {

        String medName = intent.getStringExtra("medName");
        String dosage = intent.getStringExtra("dosage");
        String tone = intent.getStringExtra("tone"); // ringtone name
        String medicineId = intent.getStringExtra("medicineId");

        // Create channel if needed
        createChannel(context);

        // Select ringtone
        Uri soundUri;
        if (tone != null && tone.equals("Harp")) {
            soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.harp);
        } else {
            // default sound
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        // Open notification screen when user taps
        Intent tapIntent = new Intent(context, NotificationsActivity.class);
        tapIntent.putExtra("medicineId", medicineId);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                medicineId.hashCode(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medicine) // make sure this exists
                .setContentTitle("Time to take your medicine")
                .setContentText(medName + " • " + dosage)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        nm.notify(medicineId.hashCode(), builder.build());
    }

    private void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Medicine Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Daily medicine reminder alerts");

            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }
}
