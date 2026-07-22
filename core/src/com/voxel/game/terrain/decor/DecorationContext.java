package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.Deterministic;

/**
 * Moi truong lam viec cua mot {@link Decorator}: no biet dang dung o cot nao,
 * mat dat cao bao nhieu, va cung cap ham random on dinh theo toa do the gioi.
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

    /** Dat khoi bang toa do lech so voi cot dang xet. */
    public void place(int dx, int y, int dz, Block block) {
        if (y < 1 || y >= worldHeight) {
            return;
        }
        writer.set(localX + dx, y, localZ + dz, block);
    }

    /** Chi dat khi cho do dang trong. */
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

    /** So ngau nhien 0..1 nhung luon giong nhau voi cung seed + toa do + salt. */
    public float random(int salt) {
        return Deterministic.unit(seed, worldX, worldZ, salt);
    }

    public int randomInt(int salt, int minInclusive, int maxInclusive) {
        return Deterministic.range(seed, worldX, worldZ, salt, minInclusive, maxInclusive);
    }
}
