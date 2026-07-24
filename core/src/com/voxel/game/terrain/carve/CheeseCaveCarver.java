package com.voxel.game.terrain.carve;

import com.voxel.engine.generation.Carver;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;

/**
 * Hang "pho mai": dung truc tiep nhieu 3D lam nguong - cho ra cac bong rong tron,
 * xen ke voi duong ham cua perlin worm de hang khong bi don dieu.
 */
public final class CheeseCaveCarver implements Carver {

    private static final double THRESHOLD = 0.58;
    private static final int MIN_Y = 6;
    /**
     * Hang pho mai chi khoet duoi muc nay (tinh tu sea level). Nhung bong rong tron nay nam
     * khap noi, neu de cham mat dat thi mat dat se ro nhu to ong - nen chung LUON bi
     * {@link SurfaceGuard#sealed()} giu kin, va tran cua chung cung khong vuot qua muc nay.
     */
    private static final int CEILING_BELOW_SEA = -2;
    /** So khoi fade-out: cang gan tran, threshold cang khat -> hang nho dan roi tat. */
    private static final int FADE_BAND = 6;

    private final TerrainNoise noise;
    private final Blocks blocks;

    public CheeseCaveCarver(TerrainNoise noise, Blocks blocks) {
        this.noise = noise;
        this.blocks = blocks;
    }

    @Override
    public void carve(ChunkWriter writer, int chunkX, int chunkZ, int chunkSize, int worldHeight) {
        CarveScope scope = new CarveScope(writer, blocks, SurfaceGuard.sealed(),
                writer.originX(), writer.originZ(), chunkSize, worldHeight);
        int maxY = Math.min(worldHeight - 2, noise.seaLevel() + CEILING_BELOW_SEA);

        for (int localX = 0; localX < chunkSize; localX++) {
            int worldX = scope.originX() + localX;
            for (int localZ = 0; localZ < chunkSize; localZ++) {
                int worldZ = scope.originZ() + localZ;
                for (int y = MIN_Y; y <= maxY; y++) {
                    // Fade-out: gan tran hang thi doi threshold cao hon -> it khoet hon.
                    double fadeThreshold = THRESHOLD;
                    int distToTop = maxY - y;
                    if (distToTop < FADE_BAND) {
                        fadeThreshold += (1.0 - THRESHOLD) * (1.0 - (double) distToTop / FADE_BAND);
                    }
                    if (noise.cave(worldX, y, worldZ) > fadeThreshold) {
                        scope.clear(localX, y, localZ);
                    }
                }
            }
        }
    }
}
