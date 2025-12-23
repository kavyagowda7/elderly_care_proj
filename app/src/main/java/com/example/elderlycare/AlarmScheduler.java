package com.example.elderlycare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Date;

public class AlarmScheduler {

    public static void schedule(Context ctx, long triggerAt, String alarmId) {

        Log.d("ALARM_DEBUG", "Scheduling alarm at: " + new Date(triggerAt));

        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.putExtra("alarmId", alarmId);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                alarmId.hashCode(),
                intent,
                flags
        );

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        if (am == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!am.canScheduleExactAlarms()) {
                    Log.w("ALARM_DEBUG", "Exact alarm permission not granted");
                    return;
                }
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

        } catch (SecurityException e) {
            Log.e("ALARM_DEBUG", "Alarm permission error", e);
        }
    }
}
