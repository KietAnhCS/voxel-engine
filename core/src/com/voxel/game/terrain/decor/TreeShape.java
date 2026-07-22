package com.voxel.game.terrain.decor;

/**
 * Strategy: the shape of a tree. Swapping the shape swaps the tree species,
 * without rewriting the logic of "when does a tree grow".
 */
public interface TreeShape {

    void build(DecorationContext context, int groundY);
}
