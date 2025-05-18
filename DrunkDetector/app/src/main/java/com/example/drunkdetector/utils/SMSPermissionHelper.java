package com.example.drunkdetector.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

public class SMSPermissionHelper {

    public static boolean hasSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestSmsPermission(Activity activity, ActivityResultLauncher<String> requestPermissionLauncher) {
        if (!hasSmsPermission(activity)) {
            requestPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }
    }


}
