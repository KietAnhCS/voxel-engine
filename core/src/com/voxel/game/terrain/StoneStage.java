package com.voxel.game.terrain;

import com.voxel.engine.block.Block;
import com.voxel.engine.generation.TerrainStage;
import com.voxel.game.Blocks;

/** Toan bo phan da ngam duoi lop dat, tron thanh cac via cho do don dieu. */
public final class StoneStage extends TerrainStage<ColumnSample> {

    private final Blocks blocks;
    private final TerrainNoise noise;

    public StoneStage(Blocks blocks, TerrainNoise noise) {
        this.blocks = blocks;
        this.noise = noise;
    }

    @Override
    protected Block tryResolve(ColumnSample sample, int x, int y, int z) {
        if (y > sample.surfaceHeight()) {
            return null;
        }

        double band = noise.strata(x, y, z);
        if (band > 0.46) {
            return blocks.sandstone;
        }
        if (band < -0.42) {
            return blocks.cobblestone;
        }
        return blocks.stone;
    }
}
