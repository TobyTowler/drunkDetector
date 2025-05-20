package com.example.drunkdetector.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.drunkdetector.databinding.FragmentDashboardBinding;
import com.example.drunkdetector.utils.NotificationPermissionHelper;

public class DashboardFragment extends Fragment {

    private static final String PREFS_NAME = "DrunkDetectorPrefs"; // Fixed empty string
    private static final String PREF_EMERGENCY_CONTACT = "emergency_contact";

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.percentageDrunkTextBox;
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);


        EditText emergencyContactInput = binding.emergencyContactInput;
        Button saveButton = binding.saveButton;

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedContact = prefs.getString(PREF_EMERGENCY_CONTACT, "");
        emergencyContactInput.setText(savedContact);

        saveButton.setOnClickListener(v -> {
            String phoneNumber = emergencyContactInput.getText().toString().trim();

            if (phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidUKMobileNumber(phoneNumber)) {
                Toast.makeText(requireContext(), "Please enter a valid UK mobile number", Toast.LENGTH_SHORT).show();
                return;
            }

            String formattedNumber = formatUKPhoneNumber(phoneNumber);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_EMERGENCY_CONTACT, formattedNumber);
            editor.apply();

            Toast.makeText(requireContext(), "Emergency contact saved", Toast.LENGTH_SHORT).show();
        });

        Button notifyButton = binding.notifyButton;
        notifyButton.setOnClickListener(v -> {
            if (NotificationPermissionHelper.hasNotificationPermission(requireContext())) {
                dashboardViewModel.sendTestNotification();
                Toast.makeText(requireContext(), "Test notification sent", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Notifications Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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