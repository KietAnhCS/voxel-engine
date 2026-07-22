package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;

/**
 * Tang da lan lon vuong o suon nui.
 */
public final class BoulderDecorator implements Decorator {

    private final float chance;
    private final int salt;

    public BoulderDecorator(float chance, int salt) {
        this.chance = chance;
        this.salt = salt;
    }

    @Override
    public boolean decorate(DecorationContext context) {
        if (context.random(salt) >= chance) {
            return false;
        }

        Block stone = context.blocks().stone;
        int baseY = context.surfaceY() + 1;
        int radius = context.randomInt(salt + 1, 1, 2);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = 0; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius + 1) {
                        continue;
                    }
                    context.placeIfEmpty(dx, baseY + dy, dz, stone);
                }
            }
        }
        return true;
    }
}
