package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.room.Room;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase database = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "product-db"
            ).build();
            List<ProductEntity> entities = database.productDao().getAll();
            for (ProductEntity entity : entities) {
                ProductItem item = ProductItem.fromEntity(entity);
                ReminderScheduler.scheduleReminder(context.getApplicationContext(), item);
            }
            database.close();
        });
        executor.shutdown();
    }
}
