package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;

/**
 * Birch tree: a tall, slim trunk with a small compact crown at the top.
 */
public final class BirchShape implements TreeShape {

    @Override
    public void build(DecorationContext context, int groundY) {
        int trunkHeight = context.randomInt(31, 6, 9);
        Block birch = context.blocks().birchWood;
        Block leaves = context.blocks().leaves;

        context.place(0, groundY, 0, context.blocks().dirt);
        for (int step = 1; step <= trunkHeight; step++) {
            context.place(0, groundY + step, 0, birch);
        }

        int crown = groundY + trunkHeight;
        for (int dy = -2; dy <= 1; dy++) {
            int radius = dy <= -1 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius) {
                        continue;
                    }
                    context.placeIfEmpty(dx, crown + dy, dz, leaves);
                }
            }
        }
    }
}
