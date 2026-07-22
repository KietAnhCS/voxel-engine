package com.voxel.game.tools;

import com.voxel.engine.EngineConfig;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.world.Chunk;
import com.voxel.engine.world.ChunkStorage;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.OverworldChunkFactory;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.biome.Biome;
import com.voxel.game.terrain.biome.BiomeSource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cong cu kiem tra bo sinh the gioi ma khong can mo cua so game:
 * sinh mot vung chunk roi in ra thong ke biome, hang dong, cay coi va thoi gian chay.
 *
 * Chay bang:  gradlew :desktop:worldProbe
 *
 * Nam trong lop "game" chu khong phai "engine": no dung Blocks, BiomeSource... cua game,
 * ma engine thi khong duoc phep biet gi ve game.
 */
public final class WorldProbe {

    private static final int CHUNK_SIZE = 16;
    private static final int WORLD_HEIGHT = 128;
    private static final int SEA_LEVEL = 48;
    private static final int RADIUS = 3;

    private WorldProbe() {
    }

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20260722L;

        EngineConfig config = new EngineConfig(CHUNK_SIZE, WORLD_HEIGHT, 8, SEA_LEVEL, seed, 1);
        BlockRegistry registry = new BlockRegistry();
        Blocks blocks = new Blocks(registry);
        TerrainNoise noise = new TerrainNoise(seed, SEA_LEVEL);
        BiomeSource biomes = new BiomeSource(blocks, noise);
        OverworldChunkFactory factory = new OverworldChunkFactory(blocks, biomes, noise, WORLD_HEIGHT);

        Map<String, Integer> biomeCounts = new LinkedHashMap<String, Integer>();
        for (Biome biome : biomes.all()) {
            biomeCounts.put(biome.name(), 0);
        }

        long cavities = 0;
        long trees = 0;
        long plants = 0;
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;

        long startedAt = System.nanoTime();
        int chunkCount = 0;

        for (int chunkX = -RADIUS; chunkX <= RADIUS; chunkX++) {
            for (int chunkZ = -RADIUS; chunkZ <= RADIUS; chunkZ++) {
                Chunk chunk = factory.create(config, registry, chunkX, chunkZ);
                chunk.generate(new LocalWriter(chunk, registry, blocks.air));
                chunkCount++;

                ChunkStorage storage = chunk.storage();
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        Biome biome = biomes.pick(chunk.originX() + x, chunk.originZ() + z);
                        biomeCounts.put(biome.name(), biomeCounts.get(biome.name()) + 1);

                        int floor = storage.skyFloor(x, z);
                        minHeight = Math.min(minHeight, floor);
                        maxHeight = Math.max(maxHeight, floor);

                        // Bong rong duoi long dat = khong khi ma phia tren van con da.
                        for (int y = 1; y < floor - 1; y++) {
                            Block block = registry.byId(storage.blockId(x, y, z));
                            if (block.isAir()) {
                                cavities++;
                            } else if (block == blocks.wood || block == blocks.birchWood) {
                                trees++;
                            } else if (block == blocks.tuft || block == blocks.flower || block == blocks.flowerYellow || block == blocks.deadBush) {
                                plants++;
                            }
                        }
                        for (int y = Math.max(1, floor - 1); y < floor + 1; y++) {
                            Block block = registry.byId(storage.blockId(x, y, z));
                            if (block == blocks.tuft || block == blocks.flower || block == blocks.flowerYellow || block == blocks.deadBush) {
                                plants++;
                            }
                        }
                    }
                }
            }
        }

        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;

        System.out.println("seed              : " + seed);
        System.out.println("chunks sinh ra    : " + chunkCount + " (" + elapsedMs + " ms, trung binh "
                + (elapsedMs / Math.max(1, chunkCount)) + " ms/chunk)");
        System.out.println("do cao mat dat    : " + minHeight + " .. " + maxHeight);
        System.out.println("o rong trong long dat : " + cavities);
        System.out.println("khoi than cay     : " + trees);
        System.out.println("co / hoa / bui    : " + plants);
        System.out.println("phan bo biome     :");
        for (Map.Entry<String, Integer> entry : biomeCounts.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.println("   " + entry.getKey() + " : " + entry.getValue() + " cot");
            }
        }
    }

    /** Writer don gian: chi ghi trong pham vi chunk, bo qua phan tran ra ngoai. */
    private static final class LocalWriter implements ChunkWriter {

        private final Chunk chunk;
        private final ChunkStorage storage;
        private final BlockRegistry registry;
        private final Block air;

        private LocalWriter(Chunk chunk, BlockRegistry registry, Block air) {
            this.chunk = chunk;
            this.storage = chunk.storage();
            this.registry = registry;
            this.air = air;
        }

        @Override
        public void set(int localX, int y, int localZ, Block block) {
            if (storage.contains(localX, y, localZ)) {
                storage.setBlockId(localX, y, localZ, block.id());
            }
        }

        @Override
        public Block get(int localX, int y, int localZ) {
            if (storage.contains(localX, y, localZ)) {
                return registry.byId(storage.blockId(localX, y, localZ));
            }
            return air;
        }

        @Override
        public int originX() {
            return chunk.originX();
        }

        @Override
        public int originZ() {
            return chunk.originZ();
        }
    }
}
