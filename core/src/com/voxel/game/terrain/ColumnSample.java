package com.voxel.game.terrain;

import com.voxel.game.terrain.biome.Biome;

/**
 * The survey result of one world column (x, z): how high it is, which biome it belongs to.
 * This object is reused for each column to avoid allocating garbage.
 */
public final class ColumnSample {

    private double surfaceHeight;
    private double temperature;
    private double humidity;
    private Biome biome;

    public void prepare(double surfaceHeight, double temperature, double humidity, Biome biome) {
        this.surfaceHeight = surfaceHeight;
        this.temperature = temperature;
        this.humidity = humidity;
        this.biome = biome;
    }

    public double surfaceHeight() {
        return surfaceHeight;
    }

    public double temperature() {
        return temperature;
    }

    public double humidity() {
        return humidity;
    }

    public Biome biome() {
        return biome;
    }
}
