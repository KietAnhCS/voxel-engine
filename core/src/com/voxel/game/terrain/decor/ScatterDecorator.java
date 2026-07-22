package com.voxel.game.terrain.decor;

/**
 * Rai mot khoi mong (co, hoa, bui kho...) len be mat theo xac suat.
 */
public final class ScatterDecorator implements Decorator {

    private final float chance;
    private final int salt;
    private final BlockSelector selector;

    public ScatterDecorator(float chance, int salt, BlockSelector selector) {
        this.chance = chance;
        this.salt = salt;
        this.selector = selector;
    }

    @Override
    public boolean decorate(DecorationContext context) {
        if (context.random(salt) >= chance) {
            return false;
        }
        int y = context.surfaceY() + 1;
        if (!context.blockAt(0, y, 0).isAir()) {
            return false;
        }
        context.place(0, y, 0, selector.select(context.blocks()));
        return true;
    }
}
