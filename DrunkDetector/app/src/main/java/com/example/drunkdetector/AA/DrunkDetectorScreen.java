package com.example.drunkdetector.AA;

import static com.example.drunkdetector.calculateDrunkness.calculateDrunkness;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.Pane;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

import com.example.drunkdetector.R;


public class DrunkDetectorScreen extends Screen {
    private int drunkPercentage;
    private boolean isRedThemeApplied = false;

    public DrunkDetectorScreen(@NonNull CarContext carContext) {
        super(carContext);
        drunkPercentage = calculateDrunkness();

        isRedThemeApplied = drunkPercentage >= 50;

        getCarContext().getSharedPreferences("app_prefs", CarContext.MODE_PRIVATE)
                .edit()
                .putBoolean("use_alternate_theme", isRedThemeApplied)
                .apply();
    }
    @NonNull
    @Override
    public Template onGetTemplate() {
        drunkPercentage = calculateDrunkness();

        boolean shouldUseRedTheme = drunkPercentage >= 50;
        if (shouldUseRedTheme != isRedThemeApplied) {
            isRedThemeApplied = shouldUseRedTheme;

            getCarContext().getSharedPreferences("app_prefs", CarContext.MODE_PRIVATE)
                    .edit()
                    .putBoolean("use_warning_theme", isRedThemeApplied)
                    .apply();

            invalidate();
        }

        Row.Builder rowBuilder = new Row.Builder()
                .setTitle(drunkPercentage >= 50 ? "WARNING: HIGH DRUNK LEVEL" : "Drunk Detector");

        if (drunkPercentage >= 50) {
            rowBuilder.addText("Drunk level is " + drunkPercentage + "%")
                    .setImage(new CarIcon.Builder(
                            CarIcon.ALERT)
                            .setTint(CarColor.RED)
                            .build());
        } else {
            rowBuilder.addText("Safe: Drunk level is " + drunkPercentage + "%")
                    .setImage(new CarIcon.Builder(
                            CarIcon.PAN)
                            .setTint(CarColor.GREEN)
                            .build());
        }

        Row row = rowBuilder.build();

        Row infoRow = new Row.Builder()
                .setTitle(drunkPercentage >= 50 ? "     DO NOT DRIVE" : "     Safe to drive")
                .addText(drunkPercentage >= 50 ?
                        "      Arrange other transport" :
                        "      Drive safely")
                .build();


        Pane.Builder paneBuilder = new Pane.Builder()
                .addRow(row)
                .addRow(infoRow);

        return new PaneTemplate.Builder(paneBuilder.build())
                .setHeaderAction(Action.APP_ICON)
                .build();
    }

}
