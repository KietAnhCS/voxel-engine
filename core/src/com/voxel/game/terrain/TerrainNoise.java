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
    private final SimplexNoise riverField;
    private final SimplexNoise lakeField;
    private final SimplexNoise entranceField;
    private final SimplexNoise spaghettiFieldA;
    private final SimplexNoise spaghettiFieldB;

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
        this.riverField = new SimplexNoise(seed * 29L + 122949823L);
        this.lakeField = new SimplexNoise(seed * 37L + 217645199L);
        this.entranceField = new SimplexNoise(seed * 41L + 512927377L);
        this.spaghettiFieldA = new SimplexNoise(seed * 43L + 715225739L);
        this.spaghettiFieldB = new SimplexNoise(seed * 47L + 982451653L);
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

    /**
     * Temperature: negative is cold (snow), positive is hot (desert).
     * Tan so THAP (0.0008) cho cac vung khi hau RONG, lien mach nhu Minecraft - tranh sa mac
     * bé teo lac giua dong bang.
     */
    public double temperature(int x, int z) {
        return temperatureField.fractal2d(x, z, 2, 0.0008, 2.0, 0.5);
    }

    /** Humidity: negative is dry, positive is wet (dense forest, swamp). Tan so thap cho vung rong. */
    public double humidity(int x, int z) {
        return humidityField.fractal2d(x, z, 2, 0.0010, 2.0, 0.5);
    }

    /**
     * River field: a smooth low-frequency value ~[-1, 1]. The ZERO line of a smooth 2D noise
     * is a long winding curve, so treating "close to zero" as a river channel gives long,
     * meandering rivers. Two octaves keep the banks gently wiggly instead of geometric.
     */
    public double river(int x, int z) {
        return riverField.fractal2d(x, z, 2, 0.0016, 2.0, 0.5);
    }

    /** Lake field: rounded blobs; a high value marks an inland basin to sink into a lake. */
    public double lake(int x, int z) {
        return lakeField.fractal2d(x, z, 2, 0.0032, 2.0, 0.5);
    }

    /** Underground strata, used to mix limestone / slate / sandstone. */
    public double strata(int x, int y, int z) {
        return strataField.noise(x * 0.021, y * 0.115, z * 0.019);
    }

    /** "Cheese" caves: rounded hollow bubbles. */
    public double cave(int x, int y, int z) {
        return caveField.noise(x * 0.023, y * 0.040, z * 0.023);
    }

    /**
     * "Cua hang": mot vung nhieu 2D thoai, gia tri cang cao thi cho do cang de tro mieng hang
     * len mat dat. Tan so 0.012 -> moi vung rong khoang 80 khoi, du lon de mot duong ham
     * dam xuyen qua ma khong lam mat dat lo cho lo cho.
     */
    public double entrance(int x, int z) {
        return entranceField.fractal2d(x, z, 2, 0.012, 2.0, 0.5);
    }

    /**
     * "Spaghetti" caves, the modern Minecraft (1.18+) way: each of the two 3D fields is
     * zero along a smooth 2D SURFACE inside the world; the INTERSECTION of both zero
     * surfaces is a long thin winding CURVE. Carving where both |a| and |b| are small
     * therefore hollows out endless narrow tunnels that twist naturally in 3D.
     */
    public double spaghettiA(int x, int y, int z) {
        return spaghettiFieldA.noise(x * 0.014, y * 0.020, z * 0.014);
    }

    public double spaghettiB(int x, int y, int z) {
        return spaghettiFieldB.noise(x * 0.014, y * 0.020, z * 0.014);
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
