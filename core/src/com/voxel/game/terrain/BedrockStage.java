package com.voxel.game.terrain;

import com.voxel.engine.block.Block;
import com.voxel.engine.generation.TerrainStage;
import com.voxel.game.Blocks;

/** Lop day the gioi: khong the dao xuyen. */
public final class BedrockStage extends TerrainStage<ColumnSample> {

    private final Blocks blocks;

    public BedrockStage(Blocks blocks) {
        this.blocks = blocks;
    }

    @Override
    protected Block tryResolve(ColumnSample sample, int x, int y, int z) {
        return y == 0 ? blocks.cobblestone : null;
    }
}
