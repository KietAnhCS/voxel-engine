package com.voxel.engine.world;

import com.voxel.engine.block.Block;

public interface ChunkWriter {

    void set(int localX, int y, int localZ, Block block);

    Block get(int localX, int y, int localZ);

    int originX();

    int originZ();
}
