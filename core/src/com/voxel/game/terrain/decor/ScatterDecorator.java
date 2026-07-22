package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;

/**
 * Scatters a thin block (grass, flowers, dead bushes...) on the surface by probability.
 *
 * It can require the ground to be a specific block - for example dead bushes only grow
 * on sand - by passing an extra {@link BlockSelector} for the ground.
 */
public final class ScatterDecorator implements Decorator {

    /** Below this threshold counts as bare ground, above the other one the patch is densest. */
    private static final float PATCH_MIN = 0.45f;
    private static final float PATCH_MAX = 0.78f;

    private final float chance;
    private final int salt;
    private final BlockSelector selector;
    private final BlockSelector ground;

    /** 0 means scatter evenly everywhere; > 0 means grow in patches of that width. */
    private int patchSize;

    public ScatterDecorator(float chance, int salt, BlockSelector selector) {
        this(chance, salt, selector, null);
    }

    /** @param ground the block required directly below the placement; null means place anywhere */
    public ScatterDecorator(float chance, int salt, BlockSelector selector, BlockSelector ground) {
        this.chance = chance;
        this.salt = salt;
        this.selector = selector;
        this.ground = ground;
    }

    /**
     * Makes blocks grow in patches instead of evenly: some spots are packed, some are bare.
     *
     * @param cellSize the average width of a patch, in blocks
     */
    public ScatterDecorator inPatches(int cellSize) {
        this.patchSize = cellSize;
        return this;
    }

    @Override
    public boolean decorate(DecorationContext context) {
        if (context.random(salt) >= chance * density(context)) {
            return false;
        }
        int y = context.surfaceY() + 1;
        if (!context.blockAt(0, y, 0).isAir()) {
            return false;
        }
        if (ground != null) {
            Block below = context.blockAt(0, y - 1, 0);
            if (below != ground.select(context.blocks())) {
                return false;
            }
        }
        context.place(0, y, 0, selector.select(context.blocks()));
        return true;
    }

    /**
     * The factor multiplied into the probability: 0 is a completely bare area, 1 is the
     * middle of a patch. It stretches the noise value from [PATCH_MIN, PATCH_MAX] to
     * [0, 1] and clamps both ends, so most of the map falls to 0 - those are the bare gaps.
     */
    private float density(DecorationContext context) {
        if (patchSize <= 0) {
            return 1f;
        }
        float noise = context.patch(salt, patchSize);
        float scaled = (noise - PATCH_MIN) / (PATCH_MAX - PATCH_MIN);
        return Math.max(0f, Math.min(1f, scaled));
    }
}
