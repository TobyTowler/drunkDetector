package com.example.drunkdetector.AA;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.Session;

public class DrunkDetectorSession extends Session {
    @Override
    @NonNull
    public Screen onCreateScreen(@NonNull Intent intent) {
        return new DrunkDetectorScreen(getCarContext());
    }
}
