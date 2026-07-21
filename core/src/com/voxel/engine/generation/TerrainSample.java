package com.voxel.engine.generation;

public final class TerrainSample {

    private double surfaceHeight;
    private double ridgeHeight;
    private double humidity;

    public void prepare(double surfaceHeight, double ridgeHeight, double humidity) {
        this.surfaceHeight = surfaceHeight;
        this.ridgeHeight = ridgeHeight;
        this.humidity = humidity;
    }

    public double surfaceHeight() {
        return surfaceHeight;
    }

    public double ridgeHeight() {
        return ridgeHeight;
    }

    public double humidity() {
        return humidity;
    }
}
