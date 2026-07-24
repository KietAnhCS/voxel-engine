package com.voxel.game.terrain.structure;

import com.voxel.engine.block.Block;
import com.voxel.engine.world.ChunkWriter;

/**
 * Lets a structure be described in WORLD coordinates while only the part that falls
 * inside the current chunk is actually written. Every chunk overlapping the structure
 * replays the same build and keeps its own slice - blocks outside are silently skipped.
 */
final class StructureWriter {

    private final ChunkWriter writer;
    private final int chunkSize;
    private final int worldHeight;

    StructureWriter(ChunkWriter writer, int chunkSize, int worldHeight) {
        this.writer = writer;
        this.chunkSize = chunkSize;
        this.worldHeight = worldHeight;
    }

    int worldHeight() {
        return worldHeight;
    }

    /** Writes the block if (worldX, y, worldZ) lies inside the current chunk. */
    void place(int worldX, int y, int worldZ, Block block) {
        int localX = worldX - writer.originX();
        int localZ = worldZ - writer.originZ();
        if (localX < 0 || localX >= chunkSize || localZ < 0 || localZ >= chunkSize
                || y < 1 || y >= worldHeight) {
            return;
        }
        writer.set(localX, y, localZ, block);
    }
}
