package com.voxel.game.terrain;

import com.voxel.engine.block.Block;
import com.voxel.engine.generation.TerrainStage;
import com.voxel.game.Blocks;

/** Stage cuoi chuoi: con lai thi la troi. */
public final class SkyStage extends TerrainStage<ColumnSample> {

    private final Blocks blocks;

    public SkyStage(Blocks blocks) {
        this.blocks = blocks;
    }

    @Override
    protected Block tryResolve(ColumnSample sample, int x, int y, int z) {
        return blocks.air;
    }
}
