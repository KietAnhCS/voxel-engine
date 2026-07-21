package com.voxel.engine.block;

public interface BlockGeometry {

    void emit(BlockView view, int x, int y, int z, Block block, QuadEmitter emitter);

    boolean occludesNeighbours();
}
