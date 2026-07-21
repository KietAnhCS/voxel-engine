package com.voxel.engine.generation;

import com.voxel.engine.block.Block;

public abstract class TerrainStage {

    private TerrainStage successor;

    public final TerrainStage chainTo(TerrainStage successor) {
        this.successor = successor;
        return successor;
    }

    public final Block resolve(TerrainSample sample, int x, int y, int z) {
        Block resolved = tryResolve(sample, x, y, z);
        if (resolved != null) {
            return resolved;
        }
        return successor == null ? null : successor.resolve(sample, x, y, z);
    }

    protected abstract Block tryResolve(TerrainSample sample, int x, int y, int z);
}
