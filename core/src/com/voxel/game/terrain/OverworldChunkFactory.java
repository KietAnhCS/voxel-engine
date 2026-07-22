package com.voxel.game.terrain;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.EngineConfig;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.world.Chunk;
import com.voxel.engine.world.ChunkFactory;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.biome.BiomeSource;

/**
 * Factory Method: engine chi biet "toi can chunk (x, z)", con viec chunk do duoc
 * sinh theo luat nao la do factory nay quyet dinh.
 */
public final class OverworldChunkFactory implements ChunkFactory {

    private final Blocks blocks;
    private final TerrainShaper shaper;

    public OverworldChunkFactory(Blocks blocks, BiomeSource biomes, TerrainNoise noise, int worldHeight) {
        this.blocks = blocks;
        this.shaper = new TerrainShaper(blocks, biomes, noise, worldHeight);
    }

    @Override
    public Chunk create(EngineConfig config, BlockRegistry registry, int chunkX, int chunkZ) {
        return new OverworldChunk(config, registry, chunkX, chunkZ, shaper, blocks);
    }

    /** Tim mot cho dung chan tu te: uu tien dat lien, khong roi giua bien. */
    public Vector3 spawnPoint(int worldX, int worldZ) {
        ColumnSample sample = new ColumnSample();
        int x = worldX;
        int z = worldZ;

        for (int attempt = 0; attempt < 64; attempt++) {
            shaper.sample(x, z, sample);
            if (sample.surfaceHeight() > shaper.seaLevel() + 2) {
                break;
            }
            x += 96;
            z += 32;
        }

        shaper.sample(x, z, sample);
        float ground = (float) Math.max(sample.surfaceHeight(), shaper.seaLevel());
        return new Vector3(x + 0.5f, ground + 3f, z + 0.5f);
    }

    public TerrainShaper shaper() {
        return shaper;
    }
}
