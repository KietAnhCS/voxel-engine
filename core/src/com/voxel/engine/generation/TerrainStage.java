package com.voxel.engine.generation;

import com.voxel.engine.block.Block;

/**
 * Chain of Responsibility: moi stage tra loi cau hoi "o toa do nay la khoi gi?".
 * Tra ve null nghia la "toi khong biet", chuoi se hoi stage ke tiep.
 *
 * @param <S> kieu du lieu mo ta mot cot dia hinh (do cao, do am, biome...)
 */
public abstract class TerrainStage<S> {

    private TerrainStage<S> successor;

    public final TerrainStage<S> chainTo(TerrainStage<S> successor) {
        this.successor = successor;
        return successor;
    }

    public final Block resolve(S sample, int x, int y, int z) {
        Block resolved = tryResolve(sample, x, y, z);
        if (resolved != null) {
            return resolved;
        }
        return successor == null ? null : successor.resolve(sample, x, y, z);
    }

    protected abstract Block tryResolve(S sample, int x, int y, int z);
}
