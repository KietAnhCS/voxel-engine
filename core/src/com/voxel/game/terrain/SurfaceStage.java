package com.voxel.game.terrain;

import com.voxel.engine.block.Block;
import com.voxel.engine.generation.TerrainStage;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.biome.Biome;

/**
 * Vai lop tren cung cua mat dat - chinh biome quyet dinh o day la co, cat hay tuyet.
 */
public final class SurfaceStage extends TerrainStage<ColumnSample> {

    private static final int SOIL_DEPTH = 4;

    private final Blocks blocks;
    private final int seaLevel;

    public SurfaceStage(Blocks blocks, int seaLevel) {
        this.blocks = blocks;
        this.seaLevel = seaLevel;
    }

    @Override
    protected Block tryResolve(ColumnSample sample, int x, int y, int z) {
        double surface = sample.surfaceHeight();
        if (y > surface) {
            return null;
        }

        Biome biome = sample.biome();
        double depth = surface - y;

        if (depth < 1.0) {
            // Nam duoi mat nuoc thi thay co bang soi cho hop ly.
            if (y < seaLevel - 1) {
                return blocks.gravel;
            }
            return biome.topBlock(y, seaLevel);
        }
        if (depth < SOIL_DEPTH) {
            return biome.fillerBlock(y, seaLevel);
        }
        return null;
    }
}
