package com.example.drunkdetector.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.drunkdetector.databinding.FragmentHomeBinding;
import com.example.drunkdetector.ui.dashboard.DashboardViewModel;
import com.example.drunkdetector.utils.PeriodicDrunkChecker;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private DashboardViewModel dashboardViewModel;
    private static final String PREFS_NAME = "DrunkDetectorPrefs";
    private static final String PREF_DETECTION_ENABLED = "detectionEnabled";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Get both view models
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set up the title text
        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // Set up the detection switch - using the existing switch1 in your layout
        Switch detectionSwitch = binding.switch1;

        // Load saved preference
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean detectionEnabled = prefs.getBoolean(PREF_DETECTION_ENABLED, false);

        // Set initial switch state based on saved preference
        detectionSwitch.setChecked(detectionEnabled);

        // If detection is enabled at startup, start the periodic checking
        // but don't refresh the dashboard yet
        if (detectionEnabled) {
            PeriodicDrunkChecker.startChecking(requireContext());
            // No refresh here - only refresh when user explicitly turns on the switch
        }

        // Set up the switch listener
        detectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PREF_DETECTION_ENABLED, isChecked);
            editor.apply();

            // Start or stop periodic checking and update dashboard
            if (isChecked) {
                // Start periodic checking
                PeriodicDrunkChecker.startChecking(requireContext());

                // Immediately update dashboard ONLY when turning ON
                dashboardViewModel.refreshDrunkness();

                Toast.makeText(requireContext(),
                        "Drunkness detection activated", Toast.LENGTH_SHORT).show();
            } else {
                // Stop periodic checking
                PeriodicDrunkChecker.stopChecking();

                // Do NOT update the dashboard text when turning OFF
                // The dashboard will keep showing the last detected value

                Toast.makeText(requireContext(),
                        "Drunkness detection deactivated", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}