package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;

/**
 * Picks a block from the game's block table (used by decorators that scatter objects).
 */
public interface BlockSelector {

    Block select(Blocks blocks);
}
