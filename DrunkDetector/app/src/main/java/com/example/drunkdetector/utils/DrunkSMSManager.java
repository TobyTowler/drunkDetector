package com.example.drunkdetector.utils;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;

public class DrunkSMSManager {
   private static final String TAG = "SMSManager";

    public static boolean sendSMS(Context context, String phoneNumber, String message) {
        if (!SMSPermissionHelper.hasSmsPermission(context)) {
            Log.e(TAG, "Cannot send SMS: Permission not granted");
            return false;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d(TAG, "SMS sent successfully to " + phoneNumber);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS", e);
            return false;
        }
    }
}
