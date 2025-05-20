package com.example.drunkdetector;




public class CircularBuffer {
    private final float[][] buffer;
    private int index = 0; // where the current "head" of the array is
    private final int size;
    private boolean filled = false;

    public CircularBuffer(int size, int dimensions) {
        this.size = size;
        buffer = new float[size][dimensions];
    }

    public synchronized void add(float[] data) {
        buffer[index] = data.clone();
        index = (index + 1) % size; // the array "wraps around" to the start when it reaches the end
        if (index == 0) filled = true;
    }

    public synchronized float[][] getSnapshot() {
        float[][] snapshot = new float[size][];
        for (int i = 0; i < size; i++) {
            int pos = (index + i) % size;
            snapshot[i] = buffer[pos]; // since the "head" of the array could be anywhere, we have to assemble manually
        }
        return snapshot;
    }

    public synchronized boolean isFilled() {
        return filled; // so we can check enough data is available to use
    }
}

