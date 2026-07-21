package com.voxel.game.terrain;

import com.voxel.engine.block.Block;
import com.voxel.engine.generation.SimplexNoise;
import com.voxel.engine.generation.TerrainSample;
import com.voxel.engine.generation.TerrainStage;
import com.voxel.game.Blocks;

public final class SurfaceStage extends TerrainStage {

    private static final int SOIL_DEPTH = 4;

    private final Blocks blocks;
    private final SimplexNoise strata;
    private final int seaLevel;

    public SurfaceStage(Blocks blocks, SimplexNoise strata, int seaLevel) {
        this.blocks = blocks;
        this.strata = strata;
        this.seaLevel = seaLevel;
    }

    @Override
    protected Block tryResolve(TerrainSample sample, int x, int y, int z) {
        double surface = sample.surfaceHeight();
        if (y > surface) {
            return null;
        }

        double depth = surface - y;
        if (depth < 1.0) {
            if (y < seaLevel + 1) {
                return blocks.sandstone;
            }
            return sample.humidity() < -0.55 ? blocks.sandstone : blocks.grass;
        }
        if (depth < SOIL_DEPTH) {
            return y < seaLevel ? blocks.sandstone : blocks.dirt;
        }

        double band = strata.noise(x * 0.021, y * 0.115, z * 0.019);
        if (band > 0.46) {
            return blocks.sandstone;
        }
        if (band < -0.42) {
            return blocks.shale;
        }
        return blocks.stone;
    }
}
