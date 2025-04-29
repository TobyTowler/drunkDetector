package com.example.drunkdetector.ui.dashboard;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.drunkdetector.calculateDrunkness;
import com.example.drunkdetector.utils.DrunkNotificationManager;
import com.example.drunkdetector.utils.NotificationPermissionHelper;

public class DashboardViewModel extends AndroidViewModel {
    private static final String TAG = "DashboardViewModel";
    private MutableLiveData<String> mText;
    private final Context appContext;
    private int currentDrunkPercentage = 0;
    private boolean initialValueSet = false;

    public DashboardViewModel(Application application) {
        super(application);
        appContext = application.getApplicationContext();
        mText = new MutableLiveData<>();

        // Set initial message without calculating drunkness
        mText.setValue("Turn on detection to check drunkness level");
    }

    public LiveData<String> getText() {
        return mText;
    }

    // Method to manually trigger a notification for testing
    public void sendTestNotification() {
        try {
            // Only calculate if we haven't already
            if (!initialValueSet) {
                currentDrunkPercentage = (int) calculateDrunkness.getDrunkness(1.1);
                initialValueSet = true;
            }

            if (NotificationPermissionHelper.hasNotificationPermission(appContext)) {
                DrunkNotificationManager.sendDrunkAlert(appContext, currentDrunkPercentage);
                Log.d(TAG, "Test notification sent with drunkness: " + currentDrunkPercentage + "%");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending test notification", e);
        }
    }

    // Method to refresh drunkness calculation and potentially send a notification
    // This will ONLY be called when the Home switch is toggled ON
    public void refreshDrunkness() {
        try {
            // Calculate drunkness percentage
            currentDrunkPercentage = (int) calculateDrunkness.getDrunkness(1.1);
            initialValueSet = true;

            // Update text display
            mText.setValue("Chance you are drunk: \n" + currentDrunkPercentage + "%");
            Log.d(TAG, "Refreshed drunkness: " + currentDrunkPercentage + "%");

            // Send notification if drunkness is high and we have permission
            if (currentDrunkPercentage > 50 && NotificationPermissionHelper.hasNotificationPermission(appContext)) {
                DrunkNotificationManager.sendDrunkAlert(appContext, currentDrunkPercentage);
                Log.d(TAG, "Notification sent for high drunkness");
            }
        } catch (Exception e) {
            mText.setValue("Error calculating drunkness");
            Log.e(TAG, "Error refreshing drunkness", e);
        }
    }
}