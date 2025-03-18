package com.example.drunkdetector.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public DashboardViewModel() {
        int drunkPercentage = 0;
        mText = new MutableLiveData<>();
        mText.setValue("Chance you are drunk: \n" + drunkPercentage + "%");
    }

    public LiveData<String> getText() {
        return mText;
    }
}