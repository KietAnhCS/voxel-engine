package com.voxel.game.terrain;

import com.voxel.engine.generation.SimplexNoise;

/**
 * Tap hop cac "truong nhieu" dung chung cho toan bo the gioi.
 * Tach rieng de biome nao cung doc cung mot nguon -> bien gioi giua cac biome muot.
 * Tat ca ham deu chi doc, an toan khi goi tu nhieu luong sinh chunk.
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

    /** Luc dia: am la bien, duong la dat lien. */
    public double continent(int x, int z) {
        return continentField.fractal2d(x, z, 3, 0.0013, 2.0, 0.5);
    }

    /** Xoi mon: cao la dat bang phang, thap la dia hinh go ghe / nui. */
    public double erosion(int x, int z) {
        return erosionField.fractal2d(x, z, 2, 0.0021, 2.0, 0.5);
    }

    /** Go doi nho, dung cho moi biome. */
    public double hills(int x, int z) {
        return hillField.fractal2d(x, z, 4, 0.0090, 2.0, 0.5);
    }

    /** Song nui: gia tri 0..1, gan 1 la dinh nui sac. */
    public double ridge(int x, int z) {
        return 1.0 - Math.abs(ridgeField.fractal2d(x, z, 4, 0.0042, 2.0, 0.5));
    }

    /** Nhiet do: am la lanh (tuyet), duong la nong (sa mac). */
    public double temperature(int x, int z) {
        return temperatureField.fractal2d(x, z, 2, 0.0011, 2.0, 0.5);
    }

    /** Do am: am la kho, duong la am uot (rung ram, dam lay). */
    public double humidity(int x, int z) {
        return humidityField.fractal2d(x, z, 2, 0.0016, 2.0, 0.5);
    }

    /** Via da ngam, dung de tron da voi / da phien / sa thach. */
    public double strata(int x, int y, int z) {
        return strataField.noise(x * 0.021, y * 0.115, z * 0.019);
    }

    /** Hang dong dang "pho mai": cac bong rong tron. */
    public double cave(int x, int y, int z) {
        return caveField.noise(x * 0.023, y * 0.040, z * 0.023);
    }

    /** Nhieu dieu khien huong bo cua perlin worm. */
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
