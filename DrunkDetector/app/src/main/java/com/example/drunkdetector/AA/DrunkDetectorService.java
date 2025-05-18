package com.example.drunkdetector.AA;


import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.validation.HostValidator;

import com.example.drunkdetector.R;

public final class DrunkDetectorService extends CarAppService {

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @Override
    @NonNull
    public DrunkDetectorSession onCreateSession() {
        boolean useRedTheme = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("use_alternate_theme", false);

        if (useRedTheme) {
            getApplication().setTheme(R.style.DrunkDetector_WarningTheme);
        } else {
            getApplication().setTheme(R.style.DrunkDetector_SafeTheme);
        }

        return new DrunkDetectorSession();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


}
