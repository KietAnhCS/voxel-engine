package com.voxel.engine.block;

public interface BlockView {

    Block blockAt(int x, int y, int z);

    boolean occludes(int x, int y, int z, Block source);

    int lightAt(int x, int y, int z);
}
