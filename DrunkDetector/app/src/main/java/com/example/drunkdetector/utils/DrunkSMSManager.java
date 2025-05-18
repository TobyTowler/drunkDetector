package com.example.drunkdetector.utils;

import android.telephony.SmsManager;
import android.util.Log;

public class DrunkSMSManager {
    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            // Log or create notification for successful send
            Log.d("SMS", "Message sent successfully");
        } catch (Exception e) {
            Log.e("SMS", "Failed to send message", e);
            e.printStackTrace();
        }
    }
}
