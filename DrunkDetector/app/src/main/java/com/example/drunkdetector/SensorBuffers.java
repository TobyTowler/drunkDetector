package com.example.drunkdetector;

public class SensorBuffers {
    public static final CircularBuffer accelBuffer = new CircularBuffer(128, 3);
    public static final CircularBuffer gyroBuffer = new CircularBuffer(128, 3);
}

// holds the circular buffers for gyro and accel readings, so that they can be read from anywhere

