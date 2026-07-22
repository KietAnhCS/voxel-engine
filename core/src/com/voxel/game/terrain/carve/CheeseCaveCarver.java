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

    private final TerrainNoise noise;
    private final Blocks blocks;

    public CheeseCaveCarver(TerrainNoise noise, Blocks blocks) {
        this.noise = noise;
        this.blocks = blocks;
    }

    @Override
    public void carve(ChunkWriter writer, int chunkX, int chunkZ, int chunkSize, int worldHeight) {
        CarveScope scope = new CarveScope(writer, blocks,
                writer.originX(), writer.originZ(), chunkSize, worldHeight);
        int maxY = Math.min(worldHeight - 2, noise.seaLevel() + 4);

        for (int localX = 0; localX < chunkSize; localX++) {
            int worldX = scope.originX() + localX;
            for (int localZ = 0; localZ < chunkSize; localZ++) {
                int worldZ = scope.originZ() + localZ;
                for (int y = MIN_Y; y <= maxY; y++) {
                    if (noise.cave(worldX, y, worldZ) > THRESHOLD) {
                        scope.clear(localX, y, localZ);
                    }
                }
            }
        }
    }
}
