package com.voxel.engine.render;

import com.badlogic.gdx.graphics.Mesh;
import com.voxel.engine.world.Chunk;

public interface CollisionSink {

    /**
     * @param indexCount how many leading mesh indices form the collision shape (0 = no collision)
     */
    void updateSection(Chunk chunk, int section, Mesh mesh, int indexCount);

    void removeChunk(Chunk chunk);
}
