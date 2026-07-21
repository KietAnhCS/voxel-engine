package com.voxel.engine.world;

import com.voxel.engine.EngineConfig;
import com.voxel.engine.block.BlockRegistry;

public interface ChunkFactory {

    Chunk create(EngineConfig config, BlockRegistry registry, int chunkX, int chunkZ);
}
