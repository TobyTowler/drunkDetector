package com.example.drunkdetector.ui.dashboard;
import com.example.drunkdetector.calculateDrunkness;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


public class DashboardViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public DashboardViewModel() throws InterruptedException {
        int drunkPercentage = 1;
//        while(true) {
            drunkPercentage = (int) calculateDrunkness.getDrunkness(1.1);
            mText = new MutableLiveData<>();
            mText.setValue("Chance you are drunk: \n" + drunkPercentage + "%");
//            wait(100);
//        }
    }

    public LiveData<String> getText() {
        return mText;
    }
}