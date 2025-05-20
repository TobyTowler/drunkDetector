package com.example.drunkdetector;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.drunkdetector.databinding.ActivityMainBinding;
import com.example.drunkdetector.utils.DrunkNotificationManager;
import com.example.drunkdetector.utils.NotificationPermissionHelper;
import com.example.drunkdetector.utils.SMSPermissionHelper;
import com.example.drunkdetector.utils.NotificationPermissionHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private boolean smsDialogShownThisSession = false;
    private boolean notificationDialogShownThisSession = false;

    private ActivityMainBinding binding;

    private ActivityResultLauncher<String> requestSmsPermissionLauncher;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        smsDialogShownThisSession = false;
        notificationDialogShownThisSession = false;
        super.onCreate(savedInstanceState);

        try {
            ModelHolder.initialize(this); // create the model so other parts of the app can use it
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent serviceIntent = new Intent(this, SensorService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        // start the service to collect sensor data

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // NOTIFICATION CHANNEL
        DrunkNotificationManager.createNotificationChannel(this);

        // PERMISIONS
         requestSmsPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "SMS alerts enabled", Toast.LENGTH_SHORT).show();
                } else {
                    showPermissionRationaleDialog("SMS Permission",
                        "SMS permission is needed to alert your emergency contacts when you're intoxicated.");
                }
            }
        );

        requestNotificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                } else {
                    showPermissionRationaleDialog("Notification Permission",
                        "Notifications are needed to alert you when you're potentially intoxicated.");
                }
            }
        );



        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }


    @Override
    protected void onResume() {
        super.onResume();

        checkAndRequestSmsPermission();
        checkAndRequestNotificationPermission();
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);

        smsDialogShownThisSession = false;
        notificationDialogShownThisSession = false;
    }

public void showPermissionRationaleDialog(String title, String message) {
    new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("Not Now", (dialog, which) -> {
            })
            .setCancelable(false)
            .show();
}
 private void checkAndRequestSmsPermission() {
    if (!SMSPermissionHelper.hasSmsPermission(this) && !smsDialogShownThisSession) {
        SMSPermissionHelper.requestSmsPermission(this, requestSmsPermissionLauncher);
        smsDialogShownThisSession = true;
    }
}

private void checkAndRequestNotificationPermission() {
    if (!NotificationPermissionHelper.hasNotificationPermission(this) && !notificationDialogShownThisSession) {
        NotificationPermissionHelper.requestNotificationPermission(this, requestNotificationPermissionLauncher);
        notificationDialogShownThisSession = true;
    }
}
}