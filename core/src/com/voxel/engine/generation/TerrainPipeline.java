package com.voxel.engine.generation;

import com.voxel.engine.block.Block;

/**
 * Noi cac {@link TerrainStage} thanh mot chuoi va cho phep goi tu dau chuoi.
 */
public final class TerrainPipeline<S> {

    private final TerrainStage<S> head;

    private TerrainPipeline(TerrainStage<S> head) {
        this.head = head;
    }

    @SafeVarargs
    public static <S> TerrainPipeline<S> of(TerrainStage<S>... stages) {
        if (stages.length == 0) {
            throw new IllegalArgumentException("pipeline needs at least one stage");
        }
        for (int i = 0; i < stages.length - 1; i++) {
            stages[i].chainTo(stages[i + 1]);
        }
        return new TerrainPipeline<S>(stages[0]);
    }

    public Block resolve(S sample, int x, int y, int z) {
        return head.resolve(sample, x, y, z);
    }
}
