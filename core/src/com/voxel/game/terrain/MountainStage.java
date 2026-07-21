package com.voxel.game.terrain;

import com.voxel.engine.block.Block;
import com.voxel.engine.generation.SimplexNoise;
import com.voxel.engine.generation.TerrainSample;
import com.voxel.engine.generation.TerrainStage;
import com.voxel.game.Blocks;

public final class MountainStage extends TerrainStage {

    private final Blocks blocks;
    private final SimplexNoise strata;

    public MountainStage(Blocks blocks, SimplexNoise strata) {
        this.blocks = blocks;
        this.strata = strata;
    }

    @Override
    protected Block tryResolve(TerrainSample sample, int x, int y, int z) {
        if (y > sample.ridgeHeight()) {
            return null;
        }
        return stoneAt(x, y, z);
    }

    private Block stoneAt(int x, int y, int z) {
        double band = strata.noise(x * 0.021, y * 0.115, z * 0.019);
        if (band > 0.42) {
            return blocks.sandstone;
        }
        if (band < -0.38) {
            return blocks.shale;
        }
        return blocks.stone;
    }
}
