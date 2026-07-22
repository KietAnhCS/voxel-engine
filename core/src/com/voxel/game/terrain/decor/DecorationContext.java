package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.Deterministic;

/**
 * The working environment of a {@link Decorator}: it knows which column it is standing on,
 * how high the ground is, and provides random functions that are stable per world coordinate.
 */
public final class DecorationContext {

    private final ChunkWriter writer;
    private final Blocks blocks;
    private final long seed;
    private final int worldHeight;

    private int localX;
    private int localZ;
    private int worldX;
    private int worldZ;
    private int surfaceY;

    public DecorationContext(ChunkWriter writer, Blocks blocks, long seed, int worldHeight) {
        this.writer = writer;
        this.blocks = blocks;
        this.seed = seed;
        this.worldHeight = worldHeight;
    }

    public void moveTo(int localX, int localZ, int worldX, int worldZ, int surfaceY) {
        this.localX = localX;
        this.localZ = localZ;
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.surfaceY = surfaceY;
    }

    public Blocks blocks() {
        return blocks;
    }

    public int surfaceY() {
        return surfaceY;
    }

    public int worldHeight() {
        return worldHeight;
    }

    public int worldX() {
        return worldX;
    }

    public int worldZ() {
        return worldZ;
    }

    /** Places a block using coordinates relative to the current column. */
    public void place(int dx, int y, int dz, Block block) {
        if (y < 1 || y >= worldHeight) {
            return;
        }
        writer.set(localX + dx, y, localZ + dz, block);
    }

    /** Only places the block if that spot is empty. */
    public void placeIfEmpty(int dx, int y, int dz, Block block) {
        if (y < 1 || y >= worldHeight) {
            return;
        }
        if (writer.get(localX + dx, y, localZ + dz).isAir()) {
            writer.set(localX + dx, y, localZ + dz, block);
        }
    }

    public Block blockAt(int dx, int y, int dz) {
        if (y < 0 || y >= worldHeight) {
            return blocks.air;
        }
        return writer.get(localX + dx, y, localZ + dz);
    }

    /** A random number 0..1 that is always the same for the same seed + coordinate + salt. */
    public float random(int salt) {
        return Deterministic.unit(seed, worldX, worldZ, salt);
    }

    public int randomInt(int salt, int minInclusive, int maxInclusive) {
        return Deterministic.range(seed, worldX, worldZ, salt, minInclusive, maxInclusive);
    }

    /**
     * A density "field" of 0..1 that changes SLOWLY with coordinates: a few blocks apart
     * gives nearly equal values, hundreds of blocks apart gives very different ones. That
     * way grass grows in patches instead of being sprinkled evenly like salt.
     *
     * How it works: hash a random number at the four corners of a grid cell of size
     * {@code cellSize}, then INTERPOLATE the current point in two dimensions (bilinear).
     * The smooth() function rounds off the corners so the boundaries are not sharp.
     *
     * @param cellSize the width of a patch, in blocks (for example 24)
     */
    public float patch(int salt, int cellSize) {
        int cellX = Math.floorDiv(worldX, cellSize);
        int cellZ = Math.floorDiv(worldZ, cellSize);
        float fx = smooth((worldX - cellX * cellSize) / (float) cellSize);
        float fz = smooth((worldZ - cellZ * cellSize) / (float) cellSize);

        float c00 = Deterministic.unit(seed, cellX, cellZ, salt);
        float c10 = Deterministic.unit(seed, cellX + 1, cellZ, salt);
        float c01 = Deterministic.unit(seed, cellX, cellZ + 1, salt);
        float c11 = Deterministic.unit(seed, cellX + 1, cellZ + 1, salt);

        float top = c00 + (c10 - c00) * fx;
        float bottom = c01 + (c11 - c01) * fx;
        return top + (bottom - top) * fz;
    }

    /** The curve 3t^2 - 2t^3: flat at both ends so the interpolation has no sharp corners. */
    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }
}
