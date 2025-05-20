package com.example.drunkdetector.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.drunkdetector.databinding.FragmentDashboardBinding;
import com.example.drunkdetector.utils.NotificationPermissionHelper;
import com.example.drunkdetector.utils.PeriodicDrunkChecker;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private static final String PREFS_NAME = "DrunkDetectorPrefs";
    private static final String PREF_EMERGENCY_CONTACT = "emergency_contact";
    private static final String PREF_DETECTION_ENABLED = "detectionEnabled";

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        EditText emergencyContactInput = binding.emergencyContactInput;
        Button saveButton = binding.saveButton;

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedContact = prefs.getString(PREF_EMERGENCY_CONTACT, "");
        emergencyContactInput.setText(savedContact);

        saveButton.setOnClickListener(v -> {
            String phoneNumber = emergencyContactInput.getText().toString().trim();
            SharedPreferences.Editor editor = prefs.edit();

            if (phoneNumber.isEmpty()) {
                editor.putString(PREF_EMERGENCY_CONTACT, phoneNumber);
                editor.apply();
                Toast.makeText(requireContext(), "Removed emergency contact", Toast.LENGTH_SHORT).show();
            }

            if (!isValidUKMobileNumber(phoneNumber)) {
                Toast.makeText(requireContext(), "Please enter a valid UK mobile number", Toast.LENGTH_SHORT).show();
                return;
            }

            String formattedNumber = formatUKPhoneNumber(phoneNumber);

            editor.putString(PREF_EMERGENCY_CONTACT, formattedNumber);
            editor.apply();

            Toast.makeText(requireContext(), "Emergency contact saved", Toast.LENGTH_SHORT).show();
        });

        Button notifyButton = binding.notifyButton;
        notifyButton.setOnClickListener(v -> {
            if (NotificationPermissionHelper.hasNotificationPermission(requireContext())) {
                dashboardViewModel.sendTestNotification();

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(PREF_DETECTION_ENABLED, true);
                editor.apply();

                PeriodicDrunkChecker.startChecking(requireContext());

                Toast.makeText(requireContext(), "Test notification sent", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Notifications Disabled", Toast.LENGTH_SHORT).show();
            }
        });


        return root;
    }

    private void updateTextFromChecker() {
        int percentage = PeriodicDrunkChecker.getLastDrunkPercentage();
        binding.percentageDrunkTextBox.setText("Chance you are drunk: \n" + percentage + "%");
    }

    private Runnable textUpdater = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && binding != null) {
                updateTextFromChecker();
                // Schedule next update
                if (requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getBoolean(PREF_DETECTION_ENABLED, false)) {
                    binding.getRoot().postDelayed(this, 2000); // Every 2 seconds
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_DETECTION_ENABLED, false)) {
            binding.getRoot().post(textUpdater);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (binding != null) {
            binding.getRoot().removeCallbacks(textUpdater);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.getRoot().removeCallbacks(textUpdater);
        }
        binding = null;
    }

    private boolean isValidUKMobileNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");

        return (cleaned.startsWith("+447") && cleaned.length() == 13) ||
                (cleaned.startsWith("447") && cleaned.length() == 12) ||
                (cleaned.startsWith("07") && cleaned.length() == 11) ||
                (cleaned.startsWith("7") && cleaned.length() == 10);
    }

    private String formatUKPhoneNumber(String phoneNumber) {
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