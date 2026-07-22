package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;

/**
 * Chon mot khoi tu bang khoi cua game (dung cho cac decorator rai vat the).
 */
public interface BlockSelector {

    Block select(Blocks blocks);
}
