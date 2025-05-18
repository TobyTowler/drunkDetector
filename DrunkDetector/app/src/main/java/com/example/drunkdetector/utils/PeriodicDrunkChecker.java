package com.example.drunkdetector.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.drunkdetector.calculateDrunkness;
import com.example.drunkdetector.ui.dashboard.DashboardViewModel;

public class PeriodicDrunkChecker {
    private static final String TAG = "PeriodicDrunkChecker";
    private static final String PREFS_NAME = "DrunkDetectorPrefs";
    private static final String PREF_DETECTION_ENABLED = "detectionEnabled";
    private static final String PREF_EMERGENCY_CONTACT = "emergency_contact";

    private static final long CHECK_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static boolean isRunning = false;
    private static Context applicationContext;
    private static DashboardViewModel dashboardViewModel;

    private static final Runnable periodicTask = new Runnable() {
        @Override
        public void run() {
            if (applicationContext == null) {
                Log.e(TAG, "Application context is null, stopping checks");
                isRunning = false;
                return;
            }

            SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean detectionEnabled = prefs.getBoolean(PREF_DETECTION_ENABLED, false);

            if (detectionEnabled) {
                int drunkPercentage = calculateDrunkness.getDrunkness(1);

                updateDashboard(drunkPercentage);

                if (drunkPercentage > 60) {
                    if (NotificationPermissionHelper.hasNotificationPermission(applicationContext)) {
                        DrunkNotificationManager.sendDrunkAlert(applicationContext, drunkPercentage);
                        Log.d(TAG, "Notification sent for drunkenness level: " + drunkPercentage + "%");
                    }

                    if (SMSPermissionHelper.hasSmsPermission(applicationContext)) {
                        String phoneNumber = prefs.getString(PREF_EMERGENCY_CONTACT, "");

                        if (!phoneNumber.isEmpty()) {
                            String message = "The user of this phone is drunk. Please look after them :)";

                            try {
                                String formattedNumber = formatUKPhoneNumber(phoneNumber);

                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(formattedNumber, null, message, null, null);
                                Log.d(TAG, "SMS alert sent to emergency contact: " + formattedNumber);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to send SMS", e);
                            }
                        } else {
                            Log.w(TAG, "No emergency contact set, SMS not sent");
                        }
                    } else {
                        Log.e(TAG, "Cannot send SMS: No permission");
                    }
                }

                handler.postDelayed(this, CHECK_INTERVAL_MS);
            } else {
                isRunning = false;
                Log.d(TAG, "Detection disabled, stopping checks");
            }
        }
    };

    public static void startChecking(Context context) {
        applicationContext = context.getApplicationContext();

        if (!isRunning) {
            isRunning = true;
            handler.post(periodicTask);
            Log.d(TAG, "Started periodic drunk checking");
        }
    }

    public static void stopChecking() {
        isRunning = false;
        handler.removeCallbacks(periodicTask);
        Log.d(TAG, "Stopped periodic drunk checking");
    }


    private static void updateDashboard(int drunkPercentage) {
        try {
            android.content.Intent intent = new android.content.Intent("com.example.drunkdetector.DRUNK_UPDATE");
            intent.putExtra("drunk_percentage", drunkPercentage);
            applicationContext.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error updating dashboard", e);
        }
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