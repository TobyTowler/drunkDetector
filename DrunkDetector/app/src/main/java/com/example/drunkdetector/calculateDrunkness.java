package com.example.drunkdetector;

import com.github.psambit9791.jdsp.transform.FastFourier;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

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
            totalAccels[i] = Math.sqrt((float) Math.pow(accels[i][0],2) + Math.pow(accels[i][1], 2) + Math.pow(accels[i][2], 2));
            totalGyros[i] = Math.sqrt((float) Math.pow(gyros[i][0],2) + Math.pow(gyros[i][1], 2) + Math.pow(gyros[i][2], 2));

            accelX[i] = accels[i][0];
            accelY[i] = accels[i][1];
            accelZ[i] = accels[i][2];

            gyroX[i] = gyros[i][0];
            gyroY[i] = gyros[i][1];
            gyroZ[i] = gyros[i][2];
        }

        boolean onlyPositive = true;
        Kurtosis kurt = new Kurtosis();
        Skewness skew = new Skewness();
        Variance var = new Variance();
        Mean mean = new Mean();
        PearsonsCorrelation pc = new PearsonsCorrelation();

        double maxAccel = 0;
        for (double i : totalAccels) {
            if (i > maxAccel) {
                maxAccel = i;
            }
        }

        double kurtosisAccel = kurt.evaluate(totalAccels);
        double skewAccel = skew.evaluate(totalAccels);

        FastFourier ft = new FastFourier(totalAccels);
        ft.transform();
        double[] freq = ft.getMagnitude(onlyPositive);

        double sum = 0;
        for (double i: freq) {
            sum += i;
        }
        double[] freq_normalised = new double[totalAccels.length];
        for (int i = 0; i < totalAccels.length; i++) {
            freq_normalised[i] = totalAccels[i] / sum;
        }
        sum = 0;
        for (double i : freq_normalised) {
            sum += i * Math.log(i + 1e-12) / Math.log(2);
        }
        double entropyAccel = -sum;

        double[] jerks = new double[totalAccels.length - 1];
        for (int i = 0; i < totalAccels.length - 1; i++) {
            jerks[i] = totalAccels[i+1] - totalAccels[i];
        }
        double meanJerkAccels = 0;
        for (double jerk : jerks) {
            meanJerkAccels += jerk;
        }
        meanJerkAccels /= jerks.length;

        double kurtosisAccelY = kurt.evaluate(accelY);

        ft = new FastFourier(accelY);
        ft.transform();
        freq = ft.getMagnitude(onlyPositive);

        double peakAccelY = 0;
        for (double i : freq) {
            if (i > peakAccelY) {
                peakAccelY = i;
            }
        }

        double powerAccelY = 0;
        for (double i : freq) {
            powerAccelY += Math.pow(i, 2);
        }

        jerks = new double[accelY.length - 1];
        for (int i = 0; i < accelY.length - 1; i++) {
            jerks[i] = accelY[i+1] - accelY[i];
        }
        double meanJerkAccelY = 0;
        for (double jerk : jerks) {
            meanJerkAccelY += jerk;
        }
        meanJerkAccelY /= jerks.length;

        double meanAccelZ = mean.evaluate(accelZ);
        double varAccelZ = var.evaluate(accelZ);
        double skewAccelZ = skew.evaluate(accelZ);

        double zeroCrossAccelZ = 0;

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
                zeroCrossAccelZ++;
            }
        }

        ft = new FastFourier(accelZ);
        ft.transform();
        freq = ft.getMagnitude(onlyPositive);

        double peakAccelZ = 0;
        for (double i : freq) {
            if (i > peakAccelZ) {
                peakAccelZ = i;
            }
        }

        jerks = new double[accelZ.length - 1];
        for (int i = 0; i < accelZ.length - 1; i++) {
            jerks[i] = accelZ[i+1] - accelZ[i];
        }
        double meanJerkAccelZ = 0;
        for (double jerk : jerks) {
            meanJerkAccelZ += jerk;
        }
        meanJerkAccelZ /= jerks.length;

        double kurtosisGyroX = kurt.evaluate(gyroX);

        double energyGyroX = 0;
        for (double i : gyroX) {
            energyGyroX += Math.pow(i, 2);
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

        jerks = new double[gyroY.length - 1];
        for (int i = 0; i < gyroY.length - 1; i++) {
            jerks[i] = gyroY[i+1] - gyroY[i];
        }
        double meanJerkGyroY = 0;
        for (double jerk : jerks) {
            meanJerkGyroY += jerk;
        }
        meanJerkGyroY /= jerks.length;

        double correlationGyroZX = pc.correlation(gyroZ, gyroX);

        float[] features = {(float)maxAccel, (float)kurtosisAccel, (float)skewAccel, (float)entropyAccel, (float)meanJerkAccels, (float)kurtosisAccelY, (float)peakAccelY, (float)powerAccelY, (float)meanJerkAccelY, (float)meanAccelZ, (float)varAccelZ, (float)skewAccelZ, (float)zeroCrossAccelZ, (float)peakAccelZ, (float)meanJerkAccelZ, (float)kurtosisGyroX, (float)energyGyroX, (float)peakGyroX, (float)meanJerkGyroY, (float)correlationGyroZX};

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
