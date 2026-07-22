package com.voxel.game.terrain.decor;

/**
 * Gieo cay theo xac suat. Hinh dang cay do {@link TreeShape} quyet dinh.
 */
public final class TreeDecorator implements Decorator {

    private final float chance;
    private final int salt;
    private final TreeShape shape;

    public TreeDecorator(float chance, int salt, TreeShape shape) {
        this.chance = chance;
        this.salt = salt;
        this.shape = shape;
    }

    @Override
    public boolean decorate(DecorationContext context) {
        if (context.random(salt) >= chance) {
            return false;
        }
        int groundY = context.surfaceY();
        if (groundY + 12 >= context.worldHeight()) {
            return false;
        }
        shape.build(context, groundY);
        return true;
    }
}
