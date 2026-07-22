package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;

/**
 * Classic Minecraft-style oak: a straight branchless trunk with a "lollipop" crown
 * made of four layers - the two lower layers are 5x5 wrapping the top of the trunk,
 * the two upper ones are 3x3, and the four corners of each layer are trimmed so the
 * crown has rounded edges.
 *
 * The previous version used RECURSION to grow branches like a giant oak. A normal
 * Minecraft oak has no branches at all, and such a thin crown lets you see straight
 * through the middle of the tree - exactly what people complain about.
 */
public final class OakShape implements TreeShape {

    private static final int MIN_TRUNK = 4;
    private static final int MAX_TRUNK = 6;

    @Override
    public void build(DecorationContext context, int groundY) {
        int trunkHeight = context.randomInt(11, MIN_TRUNK, MAX_TRUNK);
        Block wood = context.blocks().wood;
        Block leaves = context.blocks().leaves;

        context.place(0, groundY, 0, context.blocks().dirt);
        for (int step = 1; step <= trunkHeight; step++) {
            context.place(0, groundY + step, 0, wood);
        }

        int crown = groundY + trunkHeight;
        for (int dy = -2; dy <= 1; dy++) {
            int radius = dy <= -1 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (isTrimmedCorner(context, dx, dz, radius, dy)) {
                        continue;
                    }
                    context.placeIfEmpty(dx, crown + dy, dz, leaves);
                }
            }
        }
    }

    /**
     * The corners of the top layer are always removed, the corners of the lower layers
     * are removed half the time at random - that way every tree looks different instead
     * of coming out as four identical boxes.
     */
    private boolean isTrimmedCorner(DecorationContext context, int dx, int dz, int radius, int dy) {
        if (Math.abs(dx) != radius || Math.abs(dz) != radius) {
            return false;
        }
        return dy == 1 || context.random(20 + dy * 7 + dx * 3 + dz) < 0.5f;
    }
}
