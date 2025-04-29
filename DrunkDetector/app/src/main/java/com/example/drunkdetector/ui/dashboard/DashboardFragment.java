package com.example.drunkdetector.ui.dashboard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.drunkdetector.databinding.FragmentDashboardBinding;
import com.example.drunkdetector.utils.NotificationPermissionHelper;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;

    // Permission launcher for requesting notification permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, we can send test notification
                    dashboardViewModel.sendTestNotification();
                    Toast.makeText(requireContext(), "Thank you! Now sending a test notification.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied, show explanation dialog
                    showNotificationPermissionExplanationDialog();
                }
            });

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set up the text view for drunk percentage
        final TextView textView = binding.percentageDrunkTextBox;
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // Keep only the notification button functionality
        Button notifyButton = binding.notifyButton;
        notifyButton.setOnClickListener(v -> {
            // Check permission before sending notification
            if (NotificationPermissionHelper.hasNotificationPermission(requireContext())) {
                dashboardViewModel.sendTestNotification();
            } else {
                // Request permission if we don't have it
                NotificationPermissionHelper.requestNotificationPermissionFromFragment(
                        this, requestPermissionLauncher);
            }
        });

        // We'll leave the refresh button in the layout but remove its functionality
        // as per requirement to move it to the switch in HomeFragment
        Button refreshButton = binding.refreshButton;
        if (refreshButton != null) {
            refreshButton.setVisibility(View.GONE); // Hide the refresh button
        }

        return root;
    }

    /**
     * Shows a dialog explaining why notification permissions are needed
     * and provides an option to open settings
     */
    private void showNotificationPermissionExplanationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Notifications Required")
                .setMessage("DrunkDetector needs to send notifications to alert you when you're " +
                        "potentially intoxicated. This is critical for your safety and the " +
                        "app's core functionality.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open app settings so user can enable notifications
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Not Now", (dialog, which) -> {
                    // Remind users what they're missing
                    Toast.makeText(requireContext(),
                            "Without notifications, DrunkDetector can't alert you when intoxicated.",
                            Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}