package com.example.drunkdetector.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.drunkdetector.calculateDrunkness;

public class PeriodicDrunkChecker {
    private static final String TAG = "PeriodicDrunkChecker";
    private static final String PREFS_NAME = "DrunkDetectorPrefs";
    private static final String PREF_DETECTION_ENABLED = "detectionEnabled";
    private static final String PREF_EMERGENCY_CONTACT = "emergency_contact";

    private static final long CHECK_INTERVAL_MS = 2000; // 2 seconds for testing
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static boolean isRunning = false;
    private static Context applicationContext;
    private static boolean skipNotify = false;
    private static int drunkPercentage = -1;

    private static final Runnable periodicTask = new Runnable() {
        @Override
        public void run() {
            if (applicationContext == null) {
                Log.e(TAG, "👉 Application context is null, stopping checks");
                isRunning = false;
                return;
            }

            SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean detectionEnabled = prefs.getBoolean(PREF_DETECTION_ENABLED, false);

            if (detectionEnabled) {
                // Calculate the drunkness percentage
                drunkPercentage = calculateDrunkness.calculateDrunkness();
                Log.d(TAG, "👉 Calculated drunk percentage: " + drunkPercentage);

                // Always update the dashboard, even if not drunk
                sendBroadcastUpdate(drunkPercentage);

                // Check if drunk enough for alerts
                if (drunkPercentage > 60 && !skipNotify) {
                    disableNotify10S();

                    // Send notification
                    if (NotificationPermissionHelper.hasNotificationPermission(applicationContext)) {
                        DrunkNotificationManager.sendDrunkAlert(applicationContext, drunkPercentage);
                        Log.d(TAG, "👉 Notification sent for drunkenness level: " + drunkPercentage + "%");
                    }

                    // Send SMS
                    if (SMSPermissionHelper.hasSmsPermission(applicationContext)) {
                        String phoneNumber = prefs.getString(PREF_EMERGENCY_CONTACT, "");

                        if (!phoneNumber.isEmpty()) {
                            String message = "The user of this phone is drunk. Please look after them :)";

                            try {
                                String formattedNumber = formatUKPhoneNumber(phoneNumber);

                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(formattedNumber, null, message, null, null);
                                Log.d(TAG, "👉 SMS alert sent to emergency contact: " + formattedNumber);
                            } catch (Exception e) {
                                Log.e(TAG, "👉 Failed to send SMS: " + e.getMessage(), e);
                            }
                        } else {
                            Log.w(TAG, "👉 No emergency contact set, SMS not sent");
                        }
                    } else {
                        Log.e(TAG, "👉 Cannot send SMS: No permission");
                    }
                }

                // Schedule the next check
                handler.postDelayed(this, CHECK_INTERVAL_MS);
            } else {
                isRunning = false;
                Log.d(TAG, "👉 Detection disabled, stopping checks");
            }
        }
    };

    // Separate method for broadcast to better debug
    private static void sendBroadcastUpdate(int drunkPercentage) {
        try {
            Log.d(TAG, "👉 Preparing to send broadcast with drunk percentage: " + drunkPercentage);

            // Create simple intent with action and data
            Intent intent = new Intent("com.example.drunkdetector.DRUNK_UPDATE");
            intent.putExtra("drunk_percentage", drunkPercentage);

            // Add flags to ensure delivery
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

            // Send the broadcast
            if (applicationContext != null) {
                applicationContext.sendBroadcast(intent);
                Log.d(TAG, "👉 Broadcast SENT with drunk percentage: " + drunkPercentage);
            } else {
                Log.e(TAG, "👉 Failed to send broadcast: applicationContext is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "👉 Error sending broadcast: " + e.getMessage(), e);
        }
    }

    public static void disableNotify10S() {
        skipNotify = true;
        Log.d(TAG, "👉 SMS alerts disabled for 10 seconds");

        handler.postDelayed(() -> {
            skipNotify = false;
            Log.d(TAG, "👉 SMS alerts re-enabled");
        }, 10000); // 10 seconds
    }

    public static void startChecking(Context context) {
        if (context == null) {
            Log.e(TAG, "👉 Cannot start checking: context is null");
            return;
        }

        applicationContext = context.getApplicationContext();

        // Force a preference check
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean detectionEnabled = prefs.getBoolean(PREF_DETECTION_ENABLED, false);

        if (!detectionEnabled) {
            Log.d(TAG, "👉 Detection is disabled in preferences, enabling it");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PREF_DETECTION_ENABLED, true);
            editor.apply();
        }

        if (!isRunning) {
            isRunning = true;

            // Send an immediate test update
            int initialValue = calculateDrunkness.calculateDrunkness();
            sendBroadcastUpdate(initialValue);

            // Start the periodic task
            handler.post(periodicTask);
            Log.d(TAG, "👉 Started periodic drunk checking with initial value: " + initialValue);
        } else {
            Log.d(TAG, "👉 Periodic checking already running");
        }
    }



    // Add a getter method
    public static int getLastDrunkPercentage() {
        return drunkPercentage;
    }
    public static void stopChecking() {
        isRunning = false;
        handler.removeCallbacks(periodicTask);
        Log.d(TAG, "👉 Stopped periodic drunk checking");
    }

    private static String formatUKPhoneNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");

        if (cleaned.startsWith("+")) {
            return cleaned;
        }

        if (cleaned.startsWith("44")) {
            return "+" + cleaned;
        }

        if (cleaned.startsWith("0")) {
            return "+44" + cleaned.substring(1);
        }

        return "+44" + cleaned;
    }
}