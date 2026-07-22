package com.voxel.engine.render;

import com.badlogic.gdx.graphics.Mesh;
import com.voxel.engine.world.Chunk;

public interface CollisionSink {

    /**
     * @param indexCount so chi so dau mesh duoc dung lam hinh va cham (0 = khong co va cham)
     */
    void updateSection(Chunk chunk, int section, Mesh mesh, int indexCount);

    void removeChunk(Chunk chunk);
}
