package com.voxel.engine.render;

import com.badlogic.gdx.graphics.Mesh;
import com.voxel.engine.world.Chunk;

public interface CollisionSink {

    void updateSection(Chunk chunk, int section, Mesh mesh);

    void removeChunk(Chunk chunk);
}
