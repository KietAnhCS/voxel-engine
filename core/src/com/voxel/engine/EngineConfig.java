package com.voxel.engine;

public final class EngineConfig {

    private final int chunkSize;
    private final int worldHeight;
    private final int viewDistance;
    private final int seaLevel;
    private final long seed;
    private final int workerThreads;

    /**
     * Doi tuong bat bien, tu kiem tra tham so ngay khi tao. Public de cac cong cu
     * ngoai engine (vi du WorldProbe) dung lai duoc cung mot cau hinh voi game that.
     */
    public EngineConfig(int chunkSize, int worldHeight, int viewDistance, int seaLevel, long seed, int workerThreads) {
        if (Integer.bitCount(chunkSize) != 1) {
            throw new IllegalArgumentException("chunkSize must be a power of two");
        }
        if (worldHeight % 16 != 0) {
            throw new IllegalArgumentException("worldHeight must be a multiple of 16");
        }
        this.chunkSize = chunkSize;
        this.worldHeight = worldHeight;
        this.viewDistance = viewDistance;
        this.seaLevel = seaLevel;
        this.seed = seed;
        this.workerThreads = workerThreads;
    }

    public int chunkSize() {
        return chunkSize;
    }

    public int chunkMask() {
        return chunkSize - 1;
    }

    public int chunkShift() {
        return Integer.numberOfTrailingZeros(chunkSize);
    }

    public int worldHeight() {
        return worldHeight;
    }

    public int viewDistance() {
        return viewDistance;
    }

    public int seaLevel() {
        return seaLevel;
    }

    public long seed() {
        return seed;
    }

    public int workerThreads() {
        return workerThreads;
    }

    public int blocksPerChunk() {
        return chunkSize * chunkSize * worldHeight;
    }
}
