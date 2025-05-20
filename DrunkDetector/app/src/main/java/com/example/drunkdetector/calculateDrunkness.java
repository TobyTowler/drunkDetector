package com.example.drunkdetector;

import android.content.Context;
import com.github.psambit9791.jdsp.transform.FastFourier;
import com.github.psambit9791.jdsp.transform._Fourier;

import org.apache.commons.math3.analysis.function.Max;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import java.nio.FloatBuffer;
import java.util.Collections;

public class calculateDrunkness {

    public static int calculateDrunkness() {

        // [  4   5  14  22  23  28  29  32  33  34  36  37  41  42  49  50  59  74
        //  81 100]

        float[][] accels = SensorBuffers.accelBuffer.getSnapshot();
        float[][] gyros = SensorBuffers.gyroBuffer.getSnapshot(); // get the latest gyro/accel readings

        double[] totalAccels = new double[accels.length];
        double[] accelX = new double[accels.length];
        double[] accelY = new double[accels.length];
        double[] accelZ = new double[accels.length];

        double[] totalGyros = new double[accels.length];
        double[] gyroX = new double[accels.length];
        double[] gyroY = new double[accels.length];
        double[] gyroZ = new double[accels.length];

        for (int i = 0; i < accels.length; i++) {
            totalAccels[i] = (float) Math.pow(accels[i][0],2) + Math.pow(accels[i][1], 2) + Math.pow(accels[i][2], 2);
            totalGyros[i] =(float) Math.pow(gyros[i][0],2) + Math.pow(gyros[i][1], 2) + Math.pow(gyros[i][2], 2);

            accelX[i] = accels[i][0];
            accelY[i] = accels[i][1];
            accelZ[i] = accels[i][2];

            accelX[i] = accels[i][0];
            accelY[i] = accels[i][1];
            accelZ[i] = accels[i][2];
        }

        FastFourier ft = new FastFourier(accelX);
        ft.transform();
        boolean onlyPositive = true;
        double[] freq = ft.getMagnitude(onlyPositive);

        double peakX = 0;
        for (double i : freq) {
            if (i > peakX) {
                peakX = i;
            }
        }

        double maxY = 0;
        for (double i : accelY) {
            if (i > maxY) {
                maxY = i;
            }
        }

        double powerX = 0;
        for (double i : freq) {
            powerX += Math.pow(i, 2);
        }

        ft = new FastFourier(accelY);
        ft.transform();
        freq = ft.getMagnitude(onlyPositive);

        double powerY = 0;
        for (double i : freq) {
            powerY += Math.pow(i, 2);
        }
        double meanAmpY = 0;
        int sum = 0;
        for (double i: freq) {
            sum += i;
        }
        meanAmpY = sum / freq.length;

        ft = new FastFourier(accelZ);
        ft.transform();
        freq = ft.getMagnitude(onlyPositive);

        double peakZ = 0;
        for (double i : freq) {
            if (i > peakZ) {
                peakZ = i;
            }
        }

        ft = new FastFourier(gyroX);
        ft.transform();
        freq = ft.getMagnitude(onlyPositive);

        double peakGyroX = 0;
        for (double i : freq) {
            if (i > peakGyroX) {
                peakGyroX = i;
            }
        }

        double energyAccelY = 0;
        for (double i : accelY) {
            energyAccelY += Math.pow(i, 2);
        }

        Kurtosis kurt = new Kurtosis();
        double accelKurtosis = kurt.evaluate(totalAccels);
        Skewness skew = new Skewness();
        double accelSkew = skew.evaluate(totalAccels);
        double accelYSkew = skew.evaluate(accelY);
        Variance var = new Variance();
        double accelYVariance = var.evaluate(accelY);
        Mean mean = new Mean();
        double accelZMean = mean.evaluate(accelZ);
        double accelXMean = mean.evaluate(accelX);
        double gyroMean = mean.evaluate(totalGyros);
        double gyroXVariance = var.evaluate(gyroX);

        double[] jerks = new double[accelY.length - 1];

        for (int i = 0; i < accelY.length - 1; i++) {
            jerks[i] = accelY[i+1] - accelY[i];
        }
        double accelYMeanJerk = 0;
        for (int i = 0; i < jerks.length; i++) {
            accelYMeanJerk += jerks[i];
        }
        accelYMeanJerk /= jerks.length;



        jerks = new double[gyroY.length - 1];

        for (int i = 0; i < gyroY.length - 1; i++) {
            jerks[i] = gyroY[i+1] - gyroY[i];
        }
        double gyroYMeanJerk = 0;
        for (int i = 0; i < jerks.length; i++) {
            gyroYMeanJerk += jerks[i];
        }
        gyroYMeanJerk /= jerks.length;

        double accelZZeroCrossings = 0;

        double[] zeroCrossings = new double[accelZ.length];
        for (int i = 0; i < accelZ.length; i++) {
            if (accelZ[i] > 0) {
                zeroCrossings[i] = 1;
            } else if (accelZ[i] < 0) {
                zeroCrossings[i] = -1;
            } else {
                zeroCrossings[i] = 0;
            }
        }
        for (int i = 0; i < zeroCrossings.length - 1; i++) {
            zeroCrossings[i] = zeroCrossings[i] - zeroCrossings[i + 1];
        }
        for (int i = 0; i < zeroCrossings.length - 1; i++) {
            if (zeroCrossings[i] == 0) {
                continue;
            } else {
                accelZZeroCrossings++;
            }
        }

        double accelYZeroCrossings = 0;

        zeroCrossings = new double[accelY.length];
        for (int i = 0; i < accelY.length; i++) {
            if (accelY[i] > 0) {
                zeroCrossings[i] = 1;
            } else if (accelY[i] < 0) {
                zeroCrossings[i] = -1;
            } else {
                zeroCrossings[i] = 0;
            }
        }
        for (int i = 0; i < zeroCrossings.length - 1; i++) {
            zeroCrossings[i] = zeroCrossings[i] - zeroCrossings[i + 1];
        }
        for (int i = 0; i < zeroCrossings.length - 1; i++) {
            if (zeroCrossings[i] == 0) {
                continue;
            } else {
                accelYZeroCrossings++;
            }
        }

        float[] features = {(float)accelKurtosis, (float)accelSkew, (float)accelXMean, (float)peakX, (float)powerX, (float)accelYVariance, (float)maxY, (float)accelYSkew, (float)energyAccelY, (float)accelYZeroCrossings, (float)powerY, (float)meanAmpY, (float)accelYMeanJerk, (float)accelZMean, (float)accelZZeroCrossings, (float)peakZ, (float)gyroMean, (float)gyroXVariance, (float)peakGyroX, (float)gyroYMeanJerk};

        return getDrunkness(features);

    }


    public static int getDrunkness(float[] values){

        try {

            OnnxModel model = ModelHolder.getModel(); // get the model

            double probability = model.predict(values);
            System.out.println((int)(probability *100));
            return (int) (probability * 100); // turn probability into percentage

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;

    }

}
