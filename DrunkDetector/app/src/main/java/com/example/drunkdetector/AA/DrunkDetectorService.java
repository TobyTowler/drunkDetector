package com.example.drunkdetector.AA;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.validation.HostValidator;

public final class DrunkDetectorService extends CarAppService {

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @Override
    @NonNull
    public DrunkDetectorSession onCreateSession() {
        return new DrunkDetectorSession();
    }
}
