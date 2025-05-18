package com.example.drunkdetector.ui.home;

import android.annotation.SuppressLint;
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
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch detectionSwitch = binding.switch1;

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean detectionEnabled = prefs.getBoolean(PREF_DETECTION_ENABLED, false);

        detectionSwitch.setChecked(detectionEnabled);

        if (detectionEnabled) {
            PeriodicDrunkChecker.startChecking(requireContext());
            dashboardViewModel.refreshDrunkness();
        }

        detectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PREF_DETECTION_ENABLED, isChecked);
            editor.apply();

            if (isChecked) {
                PeriodicDrunkChecker.startChecking(requireContext());

                dashboardViewModel.refreshDrunkness();

            } else {
                PeriodicDrunkChecker.stopChecking();
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