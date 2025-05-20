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
            totalAccels[i] = Math.pow(accels[i][0],2) + Math.pow(accels[i][1], 2) + Math.pow(accels[i][2], 2);
            totalGyros[i] = Math.pow(gyros[i][0],2) + Math.pow(gyros[i][1], 2) + Math.pow(gyros[i][2], 2);

            accelX[i] = (double) accels[i][0];
            accelY[i] = (double) accels[i][1];
            accelZ[i] = (double) accels[i][2];

            accelX[i] = (double) accels[i][0];
            accelY[i] = (double) accels[i][1];
            accelZ[i] = (double) accels[i][2];
        }

        FastFourier ft = new FastFourier(accelX);
        ft.transform();
        boolean onlyPositive = true;
        double[] freq = ft.getMagnitude(onlyPositive);

        double[] freq_normalised = freq;
        double sum = 0;
        for (double i : freq) {
            sum += i;
        }
        for (int i = 0; i < freq_normalised.length; i++) {
            freq_normalised[i] /= sum;
        }
        sum = 0;
        for (double i: freq_normalised) {
            sum += i * Math.log(i + 1e-12)/Math.log(2); // Math trick to get log base 2
        }
        double entropyX = -sum;

        ft = new FastFourier(accelY);
        ft.transform();
        freq = ft.getMagnitude(onlyPositive);

        double powerY = 0;
        for (double i : freq) {
            powerY += Math.pow(i, 2);
        }
        double meanAmpY = 0;
        sum = 0;
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

        double powerZ = 0;
        for (double i : freq) {
            powerZ += Math.pow(i, 2);
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

        double meanAmpGyroX = 0;
        sum = 0;
        for (double i: freq) {
            sum += i;
        }
        meanAmpGyroX = sum / freq.length;

        Kurtosis kurt = new Kurtosis();
        double accelKurtosis = kurt.evaluate(totalAccels);
        Skewness skew = new Skewness();
        double accelSkew = skew.evaluate(totalAccels);
        double accelYSkew = skew.evaluate(accelY);
        Variance var = new Variance();
        double accelYVariance = var.evaluate(accelY);
        Mean mean = new Mean();
        double accelZMean = mean.evaluate(accelZ);
        double accelZSkew = skew.evaluate(accelZ);
        PearsonsCorrelation pc = new PearsonsCorrelation();
        double corrXY = pc.correlation(gyroX, gyroY);

        double energyGyroX = 0;
        for (double i : gyroX) {
            energyGyroX += Math.pow(i, 2);
        }

        double energyGyroY = 0;
        for (double i : gyroY) {
            energyGyroY += Math.pow(i, 2);
        }

        double maxZ = 0;
        for (double i : accelZ) {
            if (i > maxZ) {
                maxZ = i;
            }
        }

        double[] jerks = new double[accelY.length - 1];

        for (int i = 0; i < accelY.length - 1; i++) {
            jerks[i] = accelY[i+1] - accelY[i];
        }
        double accelYMeanJerk = 0;
        for (int i = 0; i < jerks.length; i++) {
            accelYMeanJerk += jerks[i];
        }
        accelYMeanJerk /= jerks.length;

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

        double gyroYZeroCrossings = 0;

        zeroCrossings = new double[gyroY.length];
        for (int i = 0; i < accelY.length; i++) {
            if (gyroY[i] > 0) {
                zeroCrossings[i] = 1;
            } else if (gyroY[i] < 0) {
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
                gyroYZeroCrossings++;
            }
        }

        double[] features = {accelKurtosis, accelSkew, entropyX, accelYVariance, accelYSkew, powerY, meanAmpY, accelYMeanJerk, accelZMean, maxZ, accelZSkew, accelZZeroCrossings, peakZ, powerZ, energyGyroX, peakGyroX, meanAmpGyroX, energyGyroY, gyroYZeroCrossings, corrXY};

        return getDrunkness(features);

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
