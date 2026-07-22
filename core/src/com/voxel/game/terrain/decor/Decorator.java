package com.voxel.game.terrain.decor;

/**
 * Strategy: one way of decorating the surface (grass, flowers, trees, snow layer, rock layer...).
 * A biome only needs to combine a few decorators to get its own "landscape".
 */
public interface Decorator {

    /**
     * @return true if an object was placed and later decorators should not use this column
     */
    boolean decorate(DecorationContext context);
}
