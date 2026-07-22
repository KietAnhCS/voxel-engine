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
        if (temperature > 0.35 && humidity < -0.10) {
            return desert;
        }
        if (temperature > 0.15 && humidity < 0.15) {
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
        return total / weightSum;
    }

    public Biome[] all() {
        return registry.clone();
    }
}
