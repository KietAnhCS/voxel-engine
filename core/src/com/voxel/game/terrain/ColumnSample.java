package com.voxel.game.terrain;

import com.voxel.game.terrain.biome.Biome;

/**
 * Ket qua khao sat mot cot (x, z) cua the gioi: cao bao nhieu, thuoc biome nao.
 * Doi tuong nay duoc dung lai cho tung cot de tranh cap phat rac.
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
