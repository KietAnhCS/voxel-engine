package com.voxel.game.terrain;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.EngineConfig;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.generation.TerrainSample;
import com.voxel.engine.world.Chunk;
import com.voxel.engine.world.ChunkFactory;
import com.voxel.game.Blocks;

public final class OverworldChunkFactory implements ChunkFactory {

    private final Blocks blocks;
    private final long seed;
    private final int seaLevel;
    private final int worldHeight;

    private TerrainShaper shaper;

    public OverworldChunkFactory(Blocks blocks, long seed, int seaLevel, int worldHeight) {
        this.blocks = blocks;
        this.seed = seed;
        this.seaLevel = seaLevel;
        this.worldHeight = worldHeight;
    }

    @Override
    public Chunk create(EngineConfig config, BlockRegistry registry, int chunkX, int chunkZ) {
        return new OverworldChunk(config, registry, chunkX, chunkZ, shaper(), blocks);
    }

    public Vector3 spawnPoint(int worldX, int worldZ) {
        TerrainSample sample = new TerrainSample();
        shaper().sample(worldX, worldZ, sample);
        float ground = (float) Math.max(sample.ridgeHeight(), Math.max(sample.surfaceHeight(), seaLevel));
        return new Vector3(worldX + 0.5f, ground + 3f, worldZ + 0.5f);
    }

    private synchronized TerrainShaper shaper() {
        if (shaper == null) {
            shaper = new TerrainShaper(blocks, seed, seaLevel, worldHeight);
        }
        return shaper;
    }
}
