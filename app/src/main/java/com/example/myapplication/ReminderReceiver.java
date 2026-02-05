package com.example.myapplication;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ReminderScheduler.createNotificationChannel(context);

        long productId = intent.getLongExtra(ReminderScheduler.EXTRA_PRODUCT_ID, 0L);
        String name = intent.getStringExtra(ReminderScheduler.EXTRA_PRODUCT_NAME);
        long expiryEpoch = intent.getLongExtra(ReminderScheduler.EXTRA_EXPIRY_EPOCH_DAY, 0L);

        if (name == null) {
            name = context.getString(R.string.default_product_name);
        }

        LocalDate expiryDate = LocalDate.ofEpochDay(expiryEpoch);
        LocalDate today = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(today, expiryDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String content;
        if (daysRemaining <= 0) {
            content = context.getString(R.string.notification_content_due_today, name);
        } else {
            content = context.getString(
                    R.string.notification_content_due_in_days,
                    name,
                    formatter.format(expiryDate),
                    daysRemaining
            );
        }

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification notification = ReminderScheduler.baseNotification(context)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(content)
                .setContentIntent(contentIntent)
                .build();

        NotificationManagerCompat.from(context).notify((int) productId, notification);
    }
}
