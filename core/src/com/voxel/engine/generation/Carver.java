package com.voxel.engine.generation;

import com.voxel.engine.world.ChunkWriter;

/**
 * Buoc "duc" dia hinh: chay sau khi da do da/dat, dung de khoet hang dong,
 * khe nut, ho ngam... Moi carver chi duoc phep sua khoi nam trong chunk hien tai.
 */
public interface Carver {

    void carve(ChunkWriter writer, int chunkX, int chunkZ, int chunkSize, int worldHeight);
}
