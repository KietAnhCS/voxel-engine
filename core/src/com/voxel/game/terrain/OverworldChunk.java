package com.voxel.game.terrain;

import com.voxel.engine.EngineConfig;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.world.Chunk;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.biome.Biome;
import com.voxel.game.terrain.decor.DecorationContext;

/**
 * Mot chunk cua the gioi thuong: do dat da theo biome, khoet hang, roi trong cay co.
 */
public final class OverworldChunk extends Chunk {

    private final TerrainShaper shaper;
    private final Blocks blocks;

    public OverworldChunk(EngineConfig config, BlockRegistry registry, int chunkX, int chunkZ,
                          TerrainShaper shaper, Blocks blocks) {
        super(config, registry, chunkX, chunkZ);
        this.shaper = shaper;
        this.blocks = blocks;
    }

    @Override
    protected void fillTerrain(ChunkWriter writer) {
        ColumnSample sample = new ColumnSample();
        int size = config().chunkSize();
        int height = config().worldHeight();

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int worldX = originX() + x;
                int worldZ = originZ() + z;
                shaper.sample(worldX, worldZ, sample);

                int top = Math.min(height - 1, shaper.columnTop(sample));
                for (int y = 0; y <= top; y++) {
                    Block block = shaper.resolve(sample, worldX, y, worldZ);
                    if (!block.isAir()) {
                        writer.set(x, y, z, block);
                    }
                }
            }
        }
    }

    @Override
    protected void carveCaves(ChunkWriter writer) {
        shaper.carvers().carve(writer, chunkX(), chunkZ(), config().chunkSize(), config().worldHeight());
    }

    @Override
    protected void decorate(ChunkWriter writer) {
        int size = config().chunkSize();
        DecorationContext context = new DecorationContext(writer, blocks, shaper.seed(), config().worldHeight());

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int surfaceY = groundLevel(writer, x, z);
                if (surfaceY < 0) {
                    continue;
                }

                int worldX = originX() + x;
                int worldZ = originZ() + z;
                Biome biome = shaper.biomes().pick(worldX, worldZ);
                if (!biome.isDecorated()) {
                    continue;
                }

                context.moveTo(x, z, worldX, worldZ, surfaceY);
                biome.decorate(context);
            }
        }
    }

    /**
     * Tim mat dat: khoi dac cao nhat co khong khi ngay tren dau va thuoc loai
     * "trong duoc" (co, dat, cat, tuyet). Chay sau khi khoet hang nen mieng hang
     * khong bi phu cay.
     *
     * Do phuc tap: O(height) cho moi cot, tuc O(size^2 * height) cho ca chunk.
     */
    private int groundLevel(ChunkWriter writer, int x, int z) {
        int height = config().worldHeight();
        for (int y = height - 2; y > shaper.seaLevel(); y--) {
            Block block = writer.get(x, y, z);
            if (!isPlantable(block)) {
                continue;
            }
            if (writer.get(x, y + 1, z).isAir()) {
                return y;
            }
        }
        return -1;
    }

    /** Da cuoi nam trong danh sach vi no la mat cua nui va dong bang da - bo ra la mat sach thong. */
    private boolean isPlantable(Block block) {
        return block == blocks.grass
                || block == blocks.dirt
                || block == blocks.sand
                || block == blocks.cobblestone;
    }
}
