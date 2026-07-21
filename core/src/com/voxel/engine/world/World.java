package com.voxel.engine.world;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.EngineConfig;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.util.ChunkKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class World {

    private static final int MAX_RELIGHT_DEPTH = 2;

    private final EngineConfig config;
    private final BlockRegistry registry;
    private final ChunkFactory chunkFactory;
    private final WorldEventBus eventBus;
    private final ExecutorService workers;
    private final Map<Long, Chunk> chunks = new ConcurrentHashMap<Long, Chunk>();
    private final Map<Long, Collection<PendingEdit>> pendingEdits = new ConcurrentHashMap<Long, Collection<PendingEdit>>();
    private final ThreadLocal<LightEngine> lightEngines = new ThreadLocal<LightEngine>() {
        @Override
        protected LightEngine initialValue() {
            return new LightEngine();
        }
    };
    private final Block air;
    private final Vector3 anchor = new Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

    public World(EngineConfig config, BlockRegistry registry, ChunkFactory chunkFactory, WorldEventBus eventBus) {
        this.config = config;
        this.registry = registry;
        this.chunkFactory = chunkFactory;
        this.eventBus = eventBus;
        this.air = registry.byId((byte) 0);
        this.workers = Executors.newFixedThreadPool(config.workerThreads(), new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "voxel-worker-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public EngineConfig config() {
        return config;
    }

    public BlockRegistry registry() {
        return registry;
    }

    public int loadedChunkCount() {
        return chunks.size();
    }

    public void update(Vector3 viewer) {
        float step = config.chunkSize();
        if (Math.abs(viewer.x - anchor.x) < step && Math.abs(viewer.z - anchor.z) < step) {
            return;
        }
        anchor.set(viewer);

        int centerX = Math.floorDiv((int) Math.floor(viewer.x), config.chunkSize());
        int centerZ = Math.floorDiv((int) Math.floor(viewer.z), config.chunkSize());
        int radius = config.viewDistance();
        int radiusSquared = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }
                requestChunk(centerX + dx, centerZ + dz);
            }
        }

        unloadBeyond(centerX, centerZ, radius + 2);
    }

    private void requestChunk(int chunkX, int chunkZ) {
        final long key = ChunkKey.of(chunkX, chunkZ);
        if (chunks.containsKey(key)) {
            return;
        }
        final Chunk chunk = chunkFactory.create(config, registry, chunkX, chunkZ);
        if (chunks.putIfAbsent(key, chunk) != null) {
            return;
        }
        workers.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    chunk.generate(new RoutedWriter(chunk));
                    applyPendingEdits(chunk);
                    eventBus.fireGenerated(chunk);
                    relight(chunk, false, 0);
                } catch (Throwable failure) {
                    chunks.remove(key, chunk);
                    throw new IllegalStateException("chunk generation failed at " + ChunkKey.describe(key), failure);
                }
            }
        });
    }

    private void unloadBeyond(int centerX, int centerZ, int radius) {
        int radiusSquared = radius * radius;
        List<Long> doomed = new ArrayList<Long>();
        for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
            int dx = ChunkKey.x(entry.getKey()) - centerX;
            int dz = ChunkKey.z(entry.getKey()) - centerZ;
            if (dx * dx + dz * dz > radiusSquared) {
                doomed.add(entry.getKey());
            }
        }
        for (Long key : doomed) {
            Chunk chunk = chunks.remove(key);
            if (chunk != null) {
                pendingEdits.remove(key);
                eventBus.fireUnloaded(chunk);
            }
        }
    }

    public void relightAsync(final Chunk chunk, final boolean urgent) {
        workers.submit(new Runnable() {
            @Override
            public void run() {
                relight(chunk, urgent, 0);
            }
        });
    }

    private void relight(Chunk chunk, boolean urgent, int depth) {
        if (!chunk.isReadable()) {
            return;
        }
        boolean borderChanged = lightEngines.get().relight(chunk, this);
        eventBus.fireGeometryInvalid(chunk, urgent);

        if (!borderChanged || depth >= MAX_RELIGHT_DEPTH) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            int nx = chunk.chunkX() + (i == 0 ? 1 : i == 1 ? -1 : 0);
            int nz = chunk.chunkZ() + (i == 2 ? 1 : i == 3 ? -1 : 0);
            final Chunk neighbour = chunks.get(ChunkKey.of(nx, nz));
            if (neighbour == null || !neighbour.isReadable()) {
                continue;
            }
            final int nextDepth = depth + 1;
            workers.submit(new Runnable() {
                @Override
                public void run() {
                    relight(neighbour, false, nextDepth);
                }
            });
        }
    }

    private void applyPendingEdits(Chunk chunk) {
        Collection<PendingEdit> edits = pendingEdits.remove(chunk.key());
        if (edits == null) {
            return;
        }
        ChunkStorage storage = chunk.storage();
        for (PendingEdit edit : edits) {
            if (storage.contains(edit.localX(), edit.y(), edit.localZ())) {
                storage.setBlockId(edit.localX(), edit.y(), edit.localZ(), edit.blockId());
            }
        }
        storage.rebuildSkyFloor();
    }

    public Chunk chunkAt(int chunkX, int chunkZ) {
        return chunks.get(ChunkKey.of(chunkX, chunkZ));
    }

    public Chunk chunkContaining(int worldX, int worldZ) {
        return chunkAt(Math.floorDiv(worldX, config.chunkSize()), Math.floorDiv(worldZ, config.chunkSize()));
    }

    public Block blockAt(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= config.worldHeight()) {
            return air;
        }
        Chunk chunk = chunkContaining(worldX, worldZ);
        if (chunk == null || !chunk.isReadable()) {
            return air;
        }
        int mask = config.chunkMask();
        return chunk.blockAt(worldX & mask, worldY, worldZ & mask);
    }

    public int skyLightAt(int worldX, int worldY, int worldZ) {
        if (worldY >= config.worldHeight()) {
            return Block.MAX_LIGHT;
        }
        if (worldY < 0) {
            return 0;
        }
        Chunk chunk = chunkContaining(worldX, worldZ);
        if (chunk == null || !chunk.isReadable()) {
            return 0;
        }
        int mask = config.chunkMask();
        ChunkStorage storage = chunk.storage();
        return storage.skyLight(storage.index(worldX & mask, worldY, worldZ & mask));
    }

    public int blockLightAt(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= config.worldHeight()) {
            return 0;
        }
        Chunk chunk = chunkContaining(worldX, worldZ);
        if (chunk == null || !chunk.isReadable()) {
            return 0;
        }
        int mask = config.chunkMask();
        ChunkStorage storage = chunk.storage();
        return storage.blockLight(storage.index(worldX & mask, worldY, worldZ & mask));
    }

    public int lightAt(int worldX, int worldY, int worldZ) {
        if (worldY >= config.worldHeight()) {
            return Block.MAX_LIGHT;
        }
        if (worldY < 0) {
            return 0;
        }
        Chunk chunk = chunkContaining(worldX, worldZ);
        if (chunk == null || !chunk.isReadable()) {
            return Block.MAX_LIGHT;
        }
        int mask = config.chunkMask();
        ChunkStorage storage = chunk.storage();
        return storage.combinedLight(storage.index(worldX & mask, worldY, worldZ & mask));
    }

    public boolean setBlock(int worldX, int worldY, int worldZ, Block block) {
        if (worldY <= 0 || worldY >= config.worldHeight()) {
            return false;
        }
        Chunk chunk = chunkContaining(worldX, worldZ);
        if (chunk == null || !chunk.isReadable()) {
            return false;
        }

        int mask = config.chunkMask();
        int localX = worldX & mask;
        int localZ = worldZ & mask;

        ChunkStorage storage = chunk.storage();
        if (storage.blockId(localX, worldY, localZ) == block.id()) {
            return false;
        }

        storage.setBlockId(localX, worldY, localZ, block.id());
        storage.updateSkyFloor(localX, worldY, localZ, !block.isAir());

        relightAsync(chunk, true);
        touchNeighbourChunks(chunk, localX, localZ);
        return true;
    }

    private void touchNeighbourChunks(Chunk chunk, int localX, int localZ) {
        int last = config.chunkSize() - 1;
        if (localX == 0) {
            relightNeighbour(chunk.chunkX() - 1, chunk.chunkZ());
        }
        if (localX == last) {
            relightNeighbour(chunk.chunkX() + 1, chunk.chunkZ());
        }
        if (localZ == 0) {
            relightNeighbour(chunk.chunkX(), chunk.chunkZ() - 1);
        }
        if (localZ == last) {
            relightNeighbour(chunk.chunkX(), chunk.chunkZ() + 1);
        }
    }

    private void relightNeighbour(int chunkX, int chunkZ) {
        Chunk neighbour = chunkAt(chunkX, chunkZ);
        if (neighbour != null && neighbour.isReadable()) {
            relightAsync(neighbour, true);
        }
    }

    public boolean isSubmerged(Vector3 position) {
        Block block = blockAt(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y),
                (int) Math.floor(position.z));
        return block.isLiquid();
    }

    public void dispose() {
        workers.shutdownNow();
        chunks.clear();
        pendingEdits.clear();
    }

    private final class RoutedWriter implements ChunkWriter {

        private final Chunk chunk;
        private final ChunkStorage storage;

        private RoutedWriter(Chunk chunk) {
            this.chunk = chunk;
            this.storage = chunk.storage();
        }

        @Override
        public void set(int localX, int y, int localZ, Block block) {
            if (y < 0 || y >= config.worldHeight()) {
                return;
            }
            if (storage.contains(localX, y, localZ)) {
                storage.setBlockId(localX, y, localZ, block.id());
                return;
            }
            route(chunk.originX() + localX, y, chunk.originZ() + localZ, block);
        }

        @Override
        public Block get(int localX, int y, int localZ) {
            if (y < 0 || y >= config.worldHeight()) {
                return air;
            }
            if (storage.contains(localX, y, localZ)) {
                return registry.byId(storage.blockId(localX, y, localZ));
            }
            return blockAt(chunk.originX() + localX, y, chunk.originZ() + localZ);
        }

        @Override
        public int originX() {
            return chunk.originX();
        }

        @Override
        public int originZ() {
            return chunk.originZ();
        }

        private void route(int worldX, int y, int worldZ, Block block) {
            int size = config.chunkSize();
            int mask = config.chunkMask();
            long key = ChunkKey.of(Math.floorDiv(worldX, size), Math.floorDiv(worldZ, size));

            Chunk target = chunks.get(key);
            if (target != null && target.isReadable()) {
                target.storage().setBlockId(worldX & mask, y, worldZ & mask, block.id());
                target.storage().updateSkyFloor(worldX & mask, y, worldZ & mask, !block.isAir());
                return;
            }

            Collection<PendingEdit> queue = pendingEdits.get(key);
            if (queue == null) {
                queue = new ConcurrentLinkedQueue<PendingEdit>();
                Collection<PendingEdit> existing = pendingEdits.putIfAbsent(key, queue);
                if (existing != null) {
                    queue = existing;
                }
            }
            queue.add(new PendingEdit(worldX & mask, y, worldZ & mask, block.id()));
        }
    }
}
