package com.voxel.game.terrain.decor;

/**
 * Desert cactus: a column 2..4 blocks tall.
 */
public final class CactusShape implements TreeShape {

    @Override
    public void build(DecorationContext context, int groundY) {
        int height = context.randomInt(51, 2, 4);
        for (int step = 1; step <= height; step++) {
            context.placeIfEmpty(0, groundY + step, 0, context.blocks().cactus);
        }
    }
}
