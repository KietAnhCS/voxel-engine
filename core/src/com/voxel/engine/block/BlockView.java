package com.voxel.engine.block;

public interface BlockView {

    Block blockAt(int x, int y, int z);

    boolean occludes(int x, int y, int z, Block source);

    int lightAt(int x, int y, int z);

    /**
     * Rieng phan anh sang den tu BAU TROI (0..15).
     *
     * Tach ra khoi anh sang cua duoc/den vi hai thu nay hanh xu khac nhau khi troi toi:
     * anh sang troi tat dan theo mat troi, con anh sang duoc thi khong.
     */
    int skyLightAt(int x, int y, int z);

    /** Rieng phan anh sang den tu duoc / den (0..15). */
    int blockLightAt(int x, int y, int z);
}
