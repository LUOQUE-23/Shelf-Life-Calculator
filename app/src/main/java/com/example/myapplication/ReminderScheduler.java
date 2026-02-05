package com.example.myapplication;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class ReminderScheduler {

    public static final String CHANNEL_ID = "expiry_reminders";
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    public static final String EXTRA_PRODUCT_NAME = "extra_product_name";
    public static final String EXTRA_EXPIRY_EPOCH_DAY = "extra_expiry_epoch_day";

    private static final int DEFAULT_REMIND_HOUR = 9;
    private static final int DEFAULT_REMIND_MINUTE = 0;

    private ReminderScheduler() {
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) {
                return;
            }
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notification_channel_description));
            manager.createNotificationChannel(channel);
        }
    }

    public static void scheduleReminder(Context context, ProductItem item) {
        if (item == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        if (item.getExpiryDate().isBefore(today)) {
            return;
        }

        LocalDate remindDate = item.getRemindDate();
        if (remindDate.isBefore(today)) {
            return;
        }

        long triggerAtMillis;
        if (remindDate.isEqual(today)) {
            triggerAtMillis = System.currentTimeMillis() + 5000L;
        } else {
            triggerAtMillis = toEpochMillis(remindDate, DEFAULT_REMIND_HOUR, DEFAULT_REMIND_MINUTE);
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.example.myapplication.REMIND");
        intent.putExtra(EXTRA_PRODUCT_ID, item.getId());
        intent.putExtra(EXTRA_PRODUCT_NAME, item.getName());
        intent.putExtra(EXTRA_EXPIRY_EPOCH_DAY, item.getExpiryDate().toEpochDay());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                safeRequestCode(item.getId()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void cancelReminder(Context context, long productId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.example.myapplication.REMIND");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                safeRequestCode(productId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static NotificationCompat.Builder baseNotification(Context context) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
    }

    private static long toEpochMillis(LocalDate date, int hour, int minute) {
        LocalDateTime dateTime = date.atTime(hour, minute);
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static int safeRequestCode(long productId) {
        long safeValue = productId % Integer.MAX_VALUE;
        return (int) Math.max(1, safeValue);
    }
}
