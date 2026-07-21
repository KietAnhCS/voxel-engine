package com.voxel.engine.block;

import com.badlogic.gdx.graphics.Color;

public interface BlockTint {

    BlockTint NEUTRAL = new BlockTint() {
        @Override
        public void apply(int x, int y, int z, Color out) {
            out.set(1f, 1f, 1f, 1f);
        }
    };

    void apply(int x, int y, int z, Color out);
}
