package com.voxel.engine.world;

import com.voxel.engine.EngineConfig;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.util.ChunkKey;

public abstract class Chunk {

    private final EngineConfig config;
    private final BlockRegistry registry;
    private final ChunkStorage storage;
    private final long key;
    private final int chunkX;
    private final int chunkZ;
    private final int originX;
    private final int originZ;

    private volatile ChunkState state = ChunkState.EMPTY;

    protected Chunk(EngineConfig config, BlockRegistry registry, int chunkX, int chunkZ) {
        this.config = config;
        this.registry = registry;
        this.storage = new ChunkStorage(config);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.originX = chunkX << config.chunkShift();
        this.originZ = chunkZ << config.chunkShift();
        this.key = ChunkKey.of(chunkX, chunkZ);
    }

    /**
     * Template Method: thu tu sinh chunk luon la do dia hinh -> duc hang -> trang tri.
     * Lop con chi dien noi dung tung buoc, khong doi duoc thu tu.
     */
    public final void generate(ChunkWriter writer) {
        state = ChunkState.GENERATING;
        fillTerrain(writer);
        carveCaves(writer);
        decorate(writer);
        storage.rebuildSkyFloor();
        state = ChunkState.GENERATED;
    }

    protected abstract void fillTerrain(ChunkWriter writer);

    protected void carveCaves(ChunkWriter writer) {
    }

    protected abstract void decorate(ChunkWriter writer);

    public final ChunkStorage storage() {
        return storage;
    }

    public final EngineConfig config() {
        return config;
    }

    public final BlockRegistry registry() {
        return registry;
    }

    public final Block blockAt(int localX, int y, int localZ) {
        return registry.byId(storage.blockId(localX, y, localZ));
    }

    public final long key() {
        return key;
    }

    public final int chunkX() {
        return chunkX;
    }

    public final int chunkZ() {
        return chunkZ;
    }

    public final int originX() {
        return originX;
    }

    public final int originZ() {
        return originZ;
    }

    public final ChunkState state() {
        return state;
    }

    public final void markState(ChunkState state) {
        this.state = state;
    }

    public final boolean isReadable() {
        return state == ChunkState.GENERATED || state == ChunkState.LIT || state == ChunkState.MESHED;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + ChunkKey.describe(key);
    }
}
