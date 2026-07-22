package com.voxel.engine.world;

import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.util.Direction;
import com.voxel.engine.util.IntQueue;

/**
 * Tinh anh sang cho mot chunk bang DUYET DO THI THEO CHIEU RONG (BFS).
 *
 * Coi moi o trong chunk la mot DINH cua do thi, hai o ke nhau theo 6 huong la mot CANH.
 * Gieo anh sang vao cac dinh nguon (bau troi, den) roi lan toa dan ra, moi buoc giam
 * mot muc. Vi BFS di theo tung lop, o nao duoc cham toi truoc thi da nhan gia tri lon
 * nhat co the -> khong phai tinh lai.
 *
 * Hang doi BFS la {@link IntQueue} tu cai (mang vong), khong dung Queue cua Java de
 * tranh dong goi int thanh Integer hang trieu lan.
 */
public final class LightEngine {

    private final IntQueue queue = new IntQueue(4096);
    /** Ban anh sang dang tinh do. Tinh xong moi giao cho chunk - xem {@link ChunkStorage}. */
    private byte[] light;
    private boolean borderChanged;

    /**
     * Do phuc tap: O(V + E) voi V = size^2 * height so o trong chunk, E = 6V so canh.
     * Vi moi o chi vao hang doi khi anh sang cua no TANG len, va muc sang bi chan tren
     * boi 15, nen tong so lan enqueue la huu han va tuyen tinh theo V.
     */
    public boolean relight(Chunk chunk, World world) {
        ChunkStorage storage = chunk.storage();
        BlockRegistry registry = chunk.registry();

        // Mang MOI, khong dung lai mang cu: mang cu con dang duoc luong mesh doc de ve.
        light = new byte[storage.lightBufferSize()];

        seedSkyLight(storage, registry);
        seedNeighbourSkyLight(chunk, storage, registry, world);
        propagateSkyLight(storage, registry, chunk, world);

        seedBlockLight(storage, registry);
        seedNeighbourBlockLight(chunk, storage, registry, world);
        propagateBlockLight(storage, registry, chunk, world);

        borderChanged = borderDiffers(storage);
        storage.commitLight(light);
        light = null;
        chunk.markState(ChunkState.LIT);
        return borderChanged;
    }

    /**
     * Vien chunk co doi gia tri so voi lan tinh truoc khong?
     *
     * Chunk ben canh lay anh sang tu day, nen chi khi VIEN doi thi ho moi can tinh lai.
     * Truoc day cho nay tra loi "co" mien la anh sang co the lan ra khoi chunk - ma chunk
     * nao cung co cot troi sang cham mep, nen no luon dung, va moi lan tinh lai keo theo
     * 1 + 4 + 16 = 21 lan quet. So sanh that thi hau het cac lan sua o giua chunk deu
     * khong dong den vien, va day chuyen do tat han.
     *
     * Do phuc tap: O(size * height) - chi 4 mat ben, re hon mot lan quet chunk hang chuc lan.
     */
    private boolean borderDiffers(ChunkStorage storage) {
        int size = storage.size();
        int last = size - 1;
        for (int y = 0; y < storage.height(); y++) {
            for (int edge = 0; edge < size; edge++) {
                if (differsAt(storage, 0, y, edge)
                        || differsAt(storage, last, y, edge)
                        || differsAt(storage, edge, y, 0)
                        || differsAt(storage, edge, y, last)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean differsAt(ChunkStorage storage, int x, int y, int z) {
        int index = storage.index(x, y, z);
        return light[index] != storage.rawLight(index);
    }

    private int skyLight(int index) {
        return (light[index] >> 4) & 0x0F;
    }

    private int blockLight(int index) {
        return light[index] & 0x0F;
    }

    private void setSkyLight(int index, int value) {
        light[index] = (byte) ((light[index] & 0x0F) | (value << 4));
    }

    private void setBlockLight(int index, int value) {
        light[index] = (byte) ((light[index] & 0xF0) | value);
    }

    private void seedSkyLight(ChunkStorage storage, BlockRegistry registry) {
        int size = storage.size();
        int height = storage.height();

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int floor = storage.skyFloor(x, z);
                for (int y = floor; y < height; y++) {
                    setSkyLight(storage.index(x, y, z), Block.MAX_LIGHT);
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
        int current = sky ? skyLight(index) : blockLight(index);
        if (target > current) {
            if (sky) {
                setSkyLight(index, target);
            } else {
                setBlockLight(index, target);
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
                        setBlockLight(index, block.luminance());
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

    /**
     * Vong lap BFS: lay mot o ra khoi hang doi, thu roi anh sang sang 6 o ke.
     * O nao sang len thi lai duoc day vao hang doi de tiep tuc lan toa.
     */
    private void propagate(ChunkStorage storage, BlockRegistry registry, Chunk chunk, boolean sky) {
        int size = storage.size();
        int height = storage.height();
        int shift = chunk.config().chunkShift();
        int mask = chunk.config().chunkMask();

        while (!queue.isEmpty()) {
            int index = queue.dequeue();
            int level = sky ? skyLight(index) : blockLight(index);
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
                int current = sky ? skyLight(neighbourIndex) : blockLight(neighbourIndex);
                if (target > current) {
                    if (sky) {
                        setSkyLight(neighbourIndex, target);
                    } else {
                        setBlockLight(neighbourIndex, target);
                    }
                    queue.enqueue(neighbourIndex);
                }
            }
        }
    }
}
