package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;

/**
 * Minecraft-style pine (spruce): the crown is NOT a smooth cone but is stacked into
 * several stepped skirt layers.
 *
 * How the layers are made: going from the top down to the base, the radius grows
 * bigger; but every time it hits the cap of the current layer it drops back to 1 and
 * the cap is raised by one unit. Repeating this makes the tree pinch in at each drop,
 * and that is the boundary between two skirt layers. The previous version used a
 * uniformly shrinking radius formula, so it came out as a smooth cone - it did not
 * look like a spruce.
 */
public final class PineShape implements TreeShape {

    @Override
    public void build(DecorationContext context, int groundY) {
        int trunkHeight = context.randomInt(41, 7, 11);
        int maxRadius = context.randomInt(42, 2, 3);
        int skirtDepth = trunkHeight - context.randomInt(43, 2, 3);
        Block wood = context.blocks().wood;
        Block needles = context.blocks().pineLeaves;

        // The trunk stops three blocks below the tip: if it reached all the way up, the
        // radius-0 rings near the top would land on the trunk, leaving a bare log stub
        // right under the tip. Keeping the trunk lower makes the top three rows solid leaves.
        int crown = groundY + trunkHeight;
        context.place(0, groundY, 0, context.blocks().dirt);
        for (int step = 1; step <= trunkHeight - 3; step++) {
            context.place(0, groundY + step, 0, wood);
        }

        int radius = 0;
        int layerLimit = 1;
        int radiusAfterReset = 0;

        for (int step = 0; step <= skirtDepth; step++) {
            ring(context, crown - step, radius, needles);

            if (radius < layerLimit) {
                radius++;
                continue;
            }
            radius = radiusAfterReset;
            radiusAfterReset = 1;
            if (++layerLimit > maxRadius) {
                layerLimit = maxRadius;
            }
        }
    }

    private void ring(DecorationContext context, int y, int radius, Block needles) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (radius > 0 && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }
                context.placeIfEmpty(dx, y, dz, needles);
            }
        }
    }
}
