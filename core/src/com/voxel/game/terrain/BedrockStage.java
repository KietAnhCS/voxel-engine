package com.voxel.game.terrain;

import com.voxel.engine.block.Block;
import com.voxel.engine.generation.TerrainSample;
import com.voxel.engine.generation.TerrainStage;
import com.voxel.game.Blocks;

public final class BedrockStage extends TerrainStage {

    private final Blocks blocks;

    public BedrockStage(Blocks blocks) {
        this.blocks = blocks;
    }

    @Override
    protected Block tryResolve(TerrainSample sample, int x, int y, int z) {
        return y == 0 ? blocks.shale : null;
    }
}
