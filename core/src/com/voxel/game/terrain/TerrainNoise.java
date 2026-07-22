package com.voxel.game.terrain;

import com.voxel.engine.generation.SimplexNoise;

/**
 * The set of shared "noise fields" used by the whole world.
 * Kept separate so every biome reads from the same source -> smooth borders between biomes.
 * All methods are read-only, safe to call from multiple chunk generation threads.
 */
public final class TerrainNoise {

    private final SimplexNoise continentField;
    private final SimplexNoise erosionField;
    private final SimplexNoise hillField;
    private final SimplexNoise ridgeField;
    private final SimplexNoise temperatureField;
    private final SimplexNoise humidityField;
    private final SimplexNoise strataField;
    private final SimplexNoise caveField;
    private final SimplexNoise wormField;

    private final long seed;
    private final int seaLevel;

    public TerrainNoise(long seed, int seaLevel) {
        this.seed = seed;
        this.seaLevel = seaLevel;
        this.continentField = new SimplexNoise(seed);
        this.erosionField = new SimplexNoise(seed * 31L + 17L);
        this.hillField = new SimplexNoise(seed ^ 0x5DEECE66DL);
        this.ridgeField = new SimplexNoise(seed * 131L + 7919L);
        this.temperatureField = new SimplexNoise(seed * 7L + 104729L);
        this.humidityField = new SimplexNoise(seed * 13L + 15485863L);
        this.strataField = new SimplexNoise(seed * 17L + 32452843L);
        this.caveField = new SimplexNoise(seed * 19L + 49979687L);
        this.wormField = new SimplexNoise(seed * 23L + 86028121L);
    }

    /** Continent: negative is ocean, positive is land. */
    public double continent(int x, int z) {
        return continentField.fractal2d(x, z, 3, 0.0013, 2.0, 0.5);
    }

    /** Erosion: high means flat land, low means rough terrain / mountains. */
    public double erosion(int x, int z) {
        return erosionField.fractal2d(x, z, 2, 0.0021, 2.0, 0.5);
    }

    /** Small hills, used by every biome. */
    public double hills(int x, int z) {
        return hillField.fractal2d(x, z, 4, 0.0090, 2.0, 0.5);
    }

    /** Mountain ridges: value 0..1, close to 1 is a sharp peak. */
    public double ridge(int x, int z) {
        return 1.0 - Math.abs(ridgeField.fractal2d(x, z, 4, 0.0042, 2.0, 0.5));
    }

    /** Temperature: negative is cold (snow), positive is hot (desert). */
    public double temperature(int x, int z) {
        return temperatureField.fractal2d(x, z, 2, 0.0011, 2.0, 0.5);
    }

    /** Humidity: negative is dry, positive is wet (dense forest, swamp). */
    public double humidity(int x, int z) {
        return humidityField.fractal2d(x, z, 2, 0.0016, 2.0, 0.5);
    }

    /** Underground strata, used to mix limestone / slate / sandstone. */
    public double strata(int x, int y, int z) {
        return strataField.noise(x * 0.021, y * 0.115, z * 0.019);
    }

    /** "Cheese" caves: rounded hollow bubbles. */
    public double cave(int x, int y, int z) {
        return caveField.noise(x * 0.023, y * 0.040, z * 0.023);
    }

    /** Noise that controls the crawling direction of a perlin worm. */
    public double worm(double t, double offset) {
        return wormField.noise(t, offset);
    }

    public long seed() {
        return seed;
    }

    public int seaLevel() {
        return seaLevel;
    }
}
