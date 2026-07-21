package com.voxel.game.terrain;

import com.voxel.engine.block.Block;
import com.voxel.engine.generation.TerrainSample;
import com.voxel.engine.generation.TerrainStage;
import com.voxel.game.Blocks;

public final class OceanStage extends TerrainStage {

    private final Blocks blocks;
    private final int seaLevel;

    public OceanStage(Blocks blocks, int seaLevel) {
        this.blocks = blocks;
        this.seaLevel = seaLevel;
    }

    @Override
    protected Block tryResolve(TerrainSample sample, int x, int y, int z) {
        return y <= seaLevel ? blocks.water : null;
    }
}
