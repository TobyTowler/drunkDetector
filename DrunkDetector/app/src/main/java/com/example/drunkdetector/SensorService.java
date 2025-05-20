package com.example.drunkdetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class SensorService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelSensor, gyroSensor;

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel("sensor_channel", "Drunk Testing", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        return new NotificationCompat.Builder(this, "sensor_channel")
                .setContentTitle("Collecting sensor data")
                .setContentText("Running in background")
                .build();
    } // if the app is to run in the foreground (won't be interrupted by the OS), it requires a notification

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        int samplingPeriodUs = 31250; // 32 Hz = (1/32) * 1000 * 1000 for microseconds equivalent
        sensorManager.registerListener(this, accelSensor, samplingPeriodUs);
        sensorManager.registerListener(this, gyroSensor, samplingPeriodUs);

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            SensorBuffers.accelBuffer.add(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            SensorBuffers.gyroBuffer.add(event.values);
        } // if there has been a reading, add to the buffers

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {} // this is needed for some reason

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // this is apparently required?
    }
}
