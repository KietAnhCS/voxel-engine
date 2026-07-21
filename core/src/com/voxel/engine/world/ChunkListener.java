package com.voxel.engine.world;

public interface ChunkListener {

    void onChunkGenerated(Chunk chunk);

    void onChunkLightChanged(Chunk chunk);

    void onChunkGeometryInvalid(Chunk chunk, boolean urgent);

    void onChunkUnloaded(Chunk chunk);
}
