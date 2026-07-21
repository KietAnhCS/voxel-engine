package com.voxel.game.terrain;

import com.voxel.engine.EngineConfig;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.generation.TerrainSample;
import com.voxel.engine.world.Chunk;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;

public final class OverworldChunk extends Chunk {

    private static final float TREE_CHANCE = 0.014f;
    private static final float TUFT_CHANCE = 0.32f;
    private static final float FLOWER_CHANCE = 0.38f;

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
        TerrainSample sample = new TerrainSample();
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
    protected void decorate(ChunkWriter writer) {
        int size = config().chunkSize();
        long seed = shaper.seed();

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int surfaceY = topSoil(writer, x, z);
                if (surfaceY < 0) {
                    continue;
                }

                int worldX = originX() + x;
                int worldZ = originZ() + z;
                float roll = Deterministic.unit(seed, worldX, worldZ, 1);

                if (roll < TREE_CHANCE) {
                    plantTree(writer, x, surfaceY, z, worldX, worldZ, seed);
                } else if (roll < TUFT_CHANCE) {
                    writer.set(x, surfaceY + 1, z, blocks.tuft);
                } else if (roll < FLOWER_CHANCE) {
                    writer.set(x, surfaceY + 1, z, blocks.flower);
                }
            }
        }
    }

    private int topSoil(ChunkWriter writer, int x, int z) {
        int height = config().worldHeight();
        for (int y = height - 2; y > shaper.seaLevel(); y--) {
            if (writer.get(x, y, z) == blocks.grass && writer.get(x, y + 1, z).isAir()) {
                return y;
            }
        }
        return -1;
    }

    private void plantTree(ChunkWriter writer, int x, int groundY, int z, int worldX, int worldZ, long seed) {
        int trunkHeight = Deterministic.range(seed, worldX, worldZ, 2, 5, 8);
        int crownRadius = trunkHeight > 6 ? 3 : 2;
        int crownCentre = groundY + trunkHeight;

        if (crownCentre + crownRadius >= config().worldHeight() - 1) {
            return;
        }

        writer.set(x, groundY, z, blocks.dirt);
        for (int step = 1; step <= trunkHeight; step++) {
            writer.set(x, groundY + step, z, blocks.wood);
        }

        int radiusSquared = crownRadius * crownRadius;
        for (int dx = -crownRadius; dx <= crownRadius; dx++) {
            for (int dy = -crownRadius; dy <= crownRadius; dy++) {
                for (int dz = -crownRadius; dz <= crownRadius; dz++) {
                    int distance = dx * dx + dy * dy + dz * dz;
                    if (distance > radiusSquared) {
                        continue;
                    }
                    if (distance == radiusSquared
                            && Deterministic.unit(seed, worldX + dx, worldZ + dz, 3 + dy) < 0.45f) {
                        continue;
                    }
                    int leafX = x + dx;
                    int leafY = crownCentre + dy;
                    int leafZ = z + dz;
                    if (writer.get(leafX, leafY, leafZ).isAir()) {
                        writer.set(leafX, leafY, leafZ, blocks.leaves);
                    }
                }
            }
        }
    }
}
