package com.voxel.game.terrain.biome;

import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;

/**
 * Quyet dinh moi toa do (x, z) thuoc biome nao va tra ve do cao da duoc lam muot.
 *
 * Cach chon giong Minecraft: dua tren ba "tham so khi hau" doc lap
 *  - continent : bien hay dat lien
 *  - erosion   : dat bang phang hay nui non
 *  - temperature / humidity : nong lanh, kho am
 */
public final class BiomeSource {

    /** Khoang cach lay mau khi lam muot bien gioi biome (tinh bang khoi). */
    private static final int BLEND_STEP = 8;

    // --- Song: mot ranh hep uon luon theo duong khong cua river-noise ---
    private static final double RIVER_WIDTH = 0.045;   // ban rong long song (don vi noise)
    private static final double RIVER_BED = 4.0;       // day song sau bao nhieu duoi muc bien
    private static final double RIVER_MAX_RISE = 15.0; // dat cao hon muc bien qua ngan nay thi khong co song
    // --- Ho: long chao tron o noi lake-noise nhoi len cao ---
    private static final double LAKE_LEVEL = 0.55;     // lake-noise vuot nguong nay moi thanh ho
    private static final double LAKE_BED = 5.0;
    private static final double LAKE_MAX_RISE = 10.0;

    private final TerrainNoise noise;

    private final Biome ocean;
    private final Biome beach;
    private final Biome plains;
    private final Biome forest;
    private final Biome savanna;
    private final Biome desert;
    private final Biome swamp;
    private final Biome valley;
    private final Biome hills;
    private final Biome mountains;
    private final Biome snowyPeaks;
    private final Biome snowyPlains;
    private final Biome[] registry;

    public BiomeSource(Blocks blocks, TerrainNoise noise) {
        this.noise = noise;
        this.ocean = new OceanBiome(blocks);
        this.beach = new BeachBiome(blocks);
        this.plains = new PlainsBiome(blocks);
        this.forest = new ForestBiome(blocks);
        this.savanna = new SavannaBiome(blocks);
        this.desert = new DesertBiome(blocks);
        this.swamp = new SwampBiome(blocks);
        this.valley = new ValleyBiome(blocks);
        this.hills = new HillsBiome(blocks);
        this.mountains = new MountainBiome(blocks);
        this.snowyPeaks = new SnowyPeaksBiome(blocks);
        this.snowyPlains = new SnowyPlainsBiome(blocks);
        this.registry = new Biome[]{ocean, beach, plains, forest, savanna, desert,
                swamp, valley, hills, mountains, snowyPeaks, snowyPlains};
    }

    /** Bang tra biome tu cac tham so khi hau. */
    public Biome pick(int x, int z) {
        double continent = noise.continent(x, z);
        if (continent < -0.32) {
            return ocean;
        }
        if (continent < -0.24) {
            return beach;
        }

        double erosion = noise.erosion(x, z);
        double temperature = noise.temperature(x, z);
        double humidity = noise.humidity(x, z);

        if (erosion < -0.42) {
            return temperature < -0.25 ? snowyPeaks : mountains;
        }
        if (erosion < -0.12) {
            return hills;
        }
        if (erosion > 0.50) {
            return humidity > 0.30 ? swamp : valley;
        }
        if (temperature < -0.35) {
            return snowyPlains;
        }
        // Vung NONG: kho thi sa mac (RONG, thay vi ngUONg cUc tri >0.35 lam sa mac bé teo),
        // am hon thi savanna. Nguong ha xuong 0.22 & humidity<0 cho sa mac lien mach tu nhien.
        if (temperature > 0.22 && humidity < 0.02) {
            return desert;
        }
        if (temperature > 0.12 && humidity < 0.15) {
            return savanna;
        }
        if (humidity > 0.15) {
            return forest;
        }
        return plains;
    }

    /**
     * Do cao mat dat da lam muot: lay 9 biome xung quanh, tinh cong thuc do cao cua
     * tung biome NGAY TAI diem dang xet roi lay trung binh co trong so.
     * Nho vay bien gioi nui - dong bang la mot suon doc lien tuc, khong bi "vach dung".
     */
    /**
     * Do phuc tap: O(9) = O(1) - luon lay dung 9 diem mau, bat ke the gioi to co nao.
     * Nhung moi diem mau lai goi pick() (4 lan lay nhieu) nen day van la ham nang
     * nhat cua qua trinh sinh dia hinh: 16*16*9 lan cho moi chunk.
     */
    public double blendedHeight(int x, int z) {
        double total = 0.0;
        double weightSum = 0.0;

        for (int dx = -BLEND_STEP; dx <= BLEND_STEP; dx += BLEND_STEP) {
            for (int dz = -BLEND_STEP; dz <= BLEND_STEP; dz += BLEND_STEP) {
                double weight = (dx == 0 && dz == 0) ? 3.0 : 1.0;
                total += pick(x + dx, z + dz).surfaceHeight(noise, x, z) * weight;
                weightSum += weight;
            }
        }
        return carveWater(x, z, total / weightSum);
    }

    /**
     * Khoet SONG va HO vao be mat da lam muot: ha do cao xuong duoi muc nuoc bien theo
     * cac duong/vung nuoc, de {@code OceanStage} do nuoc len (SurfaceStage tu lot soi day nuoc).
     *
     * Song di theo duong "gan bang khong" cua river-noise nen keo dai, uon luon; ho la long
     * chao tron o noi lake-noise nhoi cao. Ca hai chi khoet o vung thap (gan muc bien) de
     * khong dam thung nui thanh hem nuoc ky quai.
     */
    private double carveWater(int x, int z, double height) {
        int sea = noise.seaLevel();

        if (height < sea + RIVER_MAX_RISE) {
            double r = Math.abs(noise.river(x, z));
            if (r < RIVER_WIDTH) {
                double t = 1.0 - r / RIVER_WIDTH;       // 0 o bo -> 1 giua dong
                t = t * t * (3.0 - 2.0 * t);            // smoothstep: bo song thoai
                double bed = sea - RIVER_BED;
                if (height > bed) {
                    height -= t * (height - bed);
                }
            }
        }

        if (height < sea + LAKE_MAX_RISE) {
            double l = noise.lake(x, z);
            if (l > LAKE_LEVEL) {
                double t = (l - LAKE_LEVEL) / (1.0 - LAKE_LEVEL);
                t = t * t * (3.0 - 2.0 * t);
                double bed = sea - LAKE_BED;
                if (height > bed) {
                    height -= t * (height - bed);
                }
            }
        }
        return height;
    }

    public Biome[] all() {
        return registry.clone();
    }
}
