package com.example.drunkdetector.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.drunkdetector.MainActivity;
import com.example.drunkdetector.R;

public class DrunkNotificationManager {
    private static final String CHANNEL_ID = "drunk_detection_channel";
    private static final String CHANNEL_NAME = "Drunk Detection Alerts";
    private static final String CHANNEL_DESC = "Notifications about your intoxication level";
    private static final int NOTIFICATION_ID = 1001;

    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(CHANNEL_DESC);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void sendDrunkAlert(Context context, int drunkPercentage) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Drunk Alert";
        String message;

        if (drunkPercentage > 80) {
            message = "You are likely intoxicated! ("+drunkPercentage+"%)";
        } else if (drunkPercentage > 50) {
            message = "You may be intoxicated. ("+drunkPercentage+"%)";
        } else {
            message = "Your intoxication level: "+drunkPercentage+"%";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp) // Use an existing notification icon from your resources
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
        }
    }
}