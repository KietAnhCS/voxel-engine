package com.voxel.engine.world;

import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.util.Direction;
import com.voxel.engine.util.IntQueue;

public final class LightEngine {

    private final IntQueue queue = new IntQueue(4096);
    private boolean borderChanged;

    public boolean relight(Chunk chunk, World world) {
        ChunkStorage storage = chunk.storage();
        BlockRegistry registry = chunk.registry();

        borderChanged = false;
        storage.clearLight();

        seedSkyLight(storage, registry);
        seedNeighbourSkyLight(chunk, storage, registry, world);
        propagateSkyLight(storage, registry, chunk, world);

        seedBlockLight(storage, registry);
        seedNeighbourBlockLight(chunk, storage, registry, world);
        propagateBlockLight(storage, registry, chunk, world);

        chunk.markState(ChunkState.LIT);
        return borderChanged;
    }

    private void seedSkyLight(ChunkStorage storage, BlockRegistry registry) {
        int size = storage.size();
        int height = storage.height();

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int floor = storage.skyFloor(x, z);
                for (int y = floor; y < height; y++) {
                    storage.setSkyLight(storage.index(x, y, z), Block.MAX_LIGHT);
                }
                if (floor < height) {
                    queue.enqueue(storage.index(x, floor, z));
                }
                for (int y = floor + 1; y < height; y++) {
                    if (hasShadowedNeighbour(storage, x, y, z, size)) {
                        queue.enqueue(storage.index(x, y, z));
                    }
                }
            }
        }
    }

    private boolean hasShadowedNeighbour(ChunkStorage storage, int x, int y, int z, int size) {
        if (x == 0 || z == 0 || x == size - 1 || z == size - 1) {
            return true;
        }
        return storage.skyFloor(x - 1, z) > y
                || storage.skyFloor(x + 1, z) > y
                || storage.skyFloor(x, z - 1) > y
                || storage.skyFloor(x, z + 1) > y;
    }

    private void seedNeighbourSkyLight(Chunk chunk, ChunkStorage storage, BlockRegistry registry, World world) {
        forEachBorderCell(chunk, storage, registry, world, true);
    }

    private void seedNeighbourBlockLight(Chunk chunk, ChunkStorage storage, BlockRegistry registry, World world) {
        forEachBorderCell(chunk, storage, registry, world, false);
    }

    private void forEachBorderCell(Chunk chunk, ChunkStorage storage, BlockRegistry registry, World world, boolean sky) {
        int size = storage.size();
        int height = storage.height();
        int originX = chunk.originX();
        int originZ = chunk.originZ();

        for (int y = 0; y < height; y++) {
            for (int edge = 0; edge < size; edge++) {
                trySeed(storage, registry, world, sky, 0, y, edge, originX - 1, y, originZ + edge);
                trySeed(storage, registry, world, sky, size - 1, y, edge, originX + size, y, originZ + edge);
                trySeed(storage, registry, world, sky, edge, y, 0, originX + edge, y, originZ - 1);
                trySeed(storage, registry, world, sky, edge, y, size - 1, originX + edge, y, originZ + size);
            }
        }
    }

    private void trySeed(ChunkStorage storage, BlockRegistry registry, World world, boolean sky,
                         int localX, int y, int localZ,
                         int worldX, int worldY, int worldZ) {
        Block own = registry.byId(storage.blockId(localX, y, localZ));
        if (own.isOpaque()) {
            return;
        }
        int outside = sky ? world.skyLightAt(worldX, worldY, worldZ) : world.blockLightAt(worldX, worldY, worldZ);
        int target = outside - own.attenuation();
        if (target <= 0) {
            return;
        }
        int index = storage.index(localX, y, localZ);
        int current = sky ? storage.skyLight(index) : storage.blockLight(index);
        if (target > current) {
            if (sky) {
                storage.setSkyLight(index, target);
            } else {
                storage.setBlockLight(index, target);
            }
            queue.enqueue(index);
        }
    }

    private void seedBlockLight(ChunkStorage storage, BlockRegistry registry) {
        int size = storage.size();
        int height = storage.height();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    Block block = registry.byId(storage.blockId(x, y, z));
                    if (block.luminance() > 0) {
                        int index = storage.index(x, y, z);
                        storage.setBlockLight(index, block.luminance());
                        queue.enqueue(index);
                    }
                }
            }
        }
    }

    private void propagateSkyLight(ChunkStorage storage, BlockRegistry registry, Chunk chunk, World world) {
        propagate(storage, registry, chunk, true);
    }

    private void propagateBlockLight(ChunkStorage storage, BlockRegistry registry, Chunk chunk, World world) {
        propagate(storage, registry, chunk, false);
    }

    private void propagate(ChunkStorage storage, BlockRegistry registry, Chunk chunk, boolean sky) {
        int size = storage.size();
        int height = storage.height();
        int shift = chunk.config().chunkShift();
        int mask = chunk.config().chunkMask();

        while (!queue.isEmpty()) {
            int index = queue.dequeue();
            int level = sky ? storage.skyLight(index) : storage.blockLight(index);
            if (level <= 1) {
                continue;
            }

            int z = index & mask;
            int x = (index >> shift) & mask;
            int y = index >> (shift << 1);

            for (int i = 0; i < Direction.ALL.length; i++) {
                Direction direction = Direction.ALL[i];
                int nx = x + direction.dx();
                int ny = y + direction.dy();
                int nz = z + direction.dz();

                if (ny < 0 || ny >= height) {
                    continue;
                }
                if (nx < 0 || nz < 0 || nx >= size || nz >= size) {
                    borderChanged = true;
                    continue;
                }

                Block neighbour = registry.byId(storage.blockId(nx, ny, nz));
                if (neighbour.isOpaque()) {
                    continue;
                }

                int cost = neighbour.attenuation();
                if (sky && direction == Direction.DOWN && level == Block.MAX_LIGHT && cost == 1) {
                    cost = 0;
                }
                int target = level - cost;
                if (target <= 0) {
                    continue;
                }

                int neighbourIndex = storage.index(nx, ny, nz);
                int current = sky ? storage.skyLight(neighbourIndex) : storage.blockLight(neighbourIndex);
                if (target > current) {
                    if (sky) {
                        storage.setSkyLight(neighbourIndex, target);
                    } else {
                        storage.setBlockLight(neighbourIndex, target);
                    }
                    if (nx == 0 || nz == 0 || nx == size - 1 || nz == size - 1) {
                        borderChanged = true;
                    }
                    queue.enqueue(neighbourIndex);
                }
            }
        }
    }
}
