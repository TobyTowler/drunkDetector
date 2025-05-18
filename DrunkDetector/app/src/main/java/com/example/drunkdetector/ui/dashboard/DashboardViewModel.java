package com.example.drunkdetector.ui.dashboard;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.drunkdetector.calculateDrunkness;
import com.example.drunkdetector.utils.DrunkNotificationManager;
import com.example.drunkdetector.utils.NotificationPermissionHelper;

public class DashboardViewModel extends AndroidViewModel {
    private static final String TAG = "DashboardViewModel";
    private final MutableLiveData<String> mText;
    private final Context appContext;
    private int currentDrunkPercentage = 0;
    private boolean initialValueSet = false;

    public DashboardViewModel(Application application) {
        super(application);
        appContext = application.getApplicationContext();
        mText = new MutableLiveData<>();
        mText.setValue("Turn on detection to check drunkenness level");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void sendTestNotification() {
        try {
            refreshDrunkness();

            if (NotificationPermissionHelper.hasNotificationPermission(appContext)) {
                DrunkNotificationManager.sendDrunkAlert(appContext, currentDrunkPercentage);
                Log.d(TAG, "Test notification sent with drunkenness: " + currentDrunkPercentage + "%");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending test notification", e);
        }
    }

    public void refreshDrunkness() {
        try {
            currentDrunkPercentage = (int) calculateDrunkness.getDrunkness(1.1);
            initialValueSet = true;

            // Update text display
            mText.setValue("Chance you are drunk: \n" + currentDrunkPercentage + "%");
            Log.d(TAG, "Refreshed drunkenness: " + currentDrunkPercentage + "%");
        } catch (Exception e) {
            mText.setValue("Error calculating drunkenness");
            Log.e(TAG, "Error refreshing drunkenness", e);
        }
    }
}