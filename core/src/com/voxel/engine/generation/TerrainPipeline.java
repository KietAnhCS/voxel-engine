package com.voxel.engine.generation;

import com.voxel.engine.block.Block;

public final class TerrainPipeline {

    private final TerrainStage head;

    private TerrainPipeline(TerrainStage head) {
        this.head = head;
    }

    public static TerrainPipeline of(TerrainStage... stages) {
        if (stages.length == 0) {
            throw new IllegalArgumentException("pipeline needs at least one stage");
        }
        for (int i = 0; i < stages.length - 1; i++) {
            stages[i].chainTo(stages[i + 1]);
        }
        return new TerrainPipeline(stages[0]);
    }

    public Block resolve(TerrainSample sample, int x, int y, int z) {
        return head.resolve(sample, x, y, z);
    }
}
