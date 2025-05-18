package com.example.drunkdetector;

public class calculateDrunkness {


    public static int calculateDrunkness() {
        return getDrunkness(1);
    }


    public static int getDrunkness(double values){
        int randomNum = (int)(Math.random() * 101); // 0 to 100
        return randomNum;
    }
}
