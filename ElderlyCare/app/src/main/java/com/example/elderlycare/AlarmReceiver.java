package com.example.elderlycare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "elderlycare_reminders_channel";
    public static final int CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_HIGH;
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_TEXT = "extra_text";


    @Override
    public void onReceive(Context context, Intent intent) {

        String alarmId = intent.getStringExtra("alarmId");

        Intent i = new Intent(context, FullScreenAlarmActivity.class);
        i.putExtra("alarmId", alarmId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_HISTORY);

        context.startActivity(i);
    }


    private void createNotificationChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Medication reminders",
                        CHANNEL_IMPORTANCE
                );
                channel.setDescription("Notifications for medication reminders");
                channel.enableLights(true);
                channel.setLightColor(Color.MAGENTA);
                channel.enableVibration(true);
                nm.createNotificationChannel(channel);
            }
        }
    }
}
