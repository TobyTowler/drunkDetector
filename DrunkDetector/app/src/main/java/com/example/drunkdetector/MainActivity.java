package com.example.drunkdetector;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.drunkdetector.databinding.ActivityMainBinding;
import com.example.drunkdetector.utils.DrunkNotificationManager;
import com.example.drunkdetector.utils.NotificationPermissionHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    // Permission launcher for notification permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Thank you! We'll notify you when you might be intoxicated.",
                            Toast.LENGTH_LONG).show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Notifications Required")
                            .setMessage("DrunkDetector needs to send notifications to alert you when you're " +
                                    "potentially intoxicated. This is critical for your safety and the " +
                                    "app's core functionality.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                // Open app settings so user can enable notifications
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Not Now", (dialog, which) -> {
                                // Remind users what they're missing
                                Toast.makeText(this,
                                        "Without notifications, DrunkDetector can't alert you when intoxicated.",
                                        Toast.LENGTH_LONG).show();
                            })
                            .setCancelable(false)
                            .show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Create notification channel
        DrunkNotificationManager.createNotificationChannel(this);

        // Request notification permission if needed
        NotificationPermissionHelper.requestNotificationPermissionFromActivity(
                this, requestPermissionLauncher);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }
}