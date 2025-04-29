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

    // Initialize notification channel (call this in Application onCreate or MainActivity onCreate)
    public static void createNotificationChannel(Context context) {
        // Only needed for Android 8.0 (API level 26) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);

            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Send drunk alert notification
    public static void sendDrunkAlert(Context context, int drunkPercentage) {
        // Create an intent that opens the MainActivity when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification content based on drunk percentage
        String title = "Drunk Alert";
        String message;

        if (drunkPercentage > 80) {
            message = "You are likely intoxicated! ("+drunkPercentage+"%)";
        } else if (drunkPercentage > 50) {
            message = "You may be intoxicated. ("+drunkPercentage+"%)";
        } else {
            message = "Your intoxication level: "+drunkPercentage+"%";
        }

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp) // Use an existing notification icon from your resources
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Handle the case where notification permission is not granted
            // For Android 13+ you need to request POST_NOTIFICATIONS permission
        }
    }
}