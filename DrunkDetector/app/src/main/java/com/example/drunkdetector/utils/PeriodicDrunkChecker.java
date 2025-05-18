package com.example.drunkdetector.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.drunkdetector.calculateDrunkness;
import com.example.drunkdetector.utils.DrunkNotificationManager;
import com.example.drunkdetector.utils.DrunkSMSManager;

/**
 * Utility class that periodically checks drunkness and sends notifications
 * when detection is enabled via the switch
 */
public class PeriodicDrunkChecker {
    private static final String TAG = "PeriodicDrunkChecker";
    private static final String PREFS_NAME = "DrunkDetectorPrefs";
    private static final String PREF_DETECTION_ENABLED = "detectionEnabled";

    private static final long CHECK_INTERVAL_MS = 1; // Check every minute
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static boolean isRunning = false;
    private static Context applicationContext;

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
                int drunkPercentage = performDrunkCheck(applicationContext);
                if (drunkPercentage > 50 && NotificationPermissionHelper.hasNotificationPermission(context)) {
                    DrunkNotificationManager.sendDrunkAlert(context, drunkPercentage);
                    Log.d(TAG, "Notification sent for drunkness level: " + drunkPercentage + "%");
                }
                // Schedule next check
                handler.postDelayed(this, CHECK_INTERVAL_MS);
            } else {
                isRunning = false;
                // Do not schedule next check
            }
        }
    };

    /**
     * Start periodic checking if not already running
     */
    public static void startChecking(Context context) {
        // Store the application context to avoid memory leaks
        applicationContext = context.getApplicationContext();

        if (!isRunning) {
            isRunning = true;
            handler.post(periodicTask);
            Log.d(TAG, "Started periodic drunk checking");
        }
    }

    /**
     * Stop periodic checking
     */
    public static void stopChecking() {
        isRunning = false;
        handler.removeCallbacks(periodicTask);
        Log.d(TAG, "Stopped periodic drunk checking");
    }

    /**
     * Performs a single drunkness check and sends notification if needed
     */
    private static int performDrunkCheck(Context context) {
        int drunkPercentage = -1;
        try {
            // Calculate drunkness
            drunkPercentage = (int) calculateDrunkness.getDrunkness(1.1);
            Log.d(TAG, "Current drunkness: " + drunkPercentage + "%");

            // Send notification if drunkness is high
        } catch (Exception e) {
            Log.e(TAG, "Error checking drunkness", e);
        }
        return drunkPercentage;
    }
}