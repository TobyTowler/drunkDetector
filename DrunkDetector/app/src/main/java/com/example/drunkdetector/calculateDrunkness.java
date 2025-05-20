package com.example.drunkdetector;

import android.content.Context;

import java.nio.FloatBuffer;

public class calculateDrunkness {

    public static int calculateDrunkness() {

        float[][] accels = SensorBuffers.accelBuffer.getSnapshot();
        float[][] gyros = SensorBuffers.gyroBuffer.getSnapshot(); // get the latest gyro/accel readings

        // need to transform the data into the features

        double[] test = {1.0, 2.0}; // just to stop errors for now

        return getDrunkness(test);

    }


    public static int getDrunkness(double[] values){

        try {

            OnnxModel model = ModelHolder.getModel(); // get the model

            double[] probabilities = model.predict(values);

            return (int) probabilities[0] * 100; // turn probability into percentage

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;

    }

}
