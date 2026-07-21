package com.voxel.engine.world;

public final class PendingEdit {

    private final int localX;
    private final int y;
    private final int localZ;
    private final byte blockId;

    public PendingEdit(int localX, int y, int localZ, byte blockId) {
        this.localX = localX;
        this.y = y;
        this.localZ = localZ;
        this.blockId = blockId;
    }

    public int localX() {
        return localX;
    }

    public int y() {
        return y;
    }

    public int localZ() {
        return localZ;
    }

    public byte blockId() {
        return blockId;
    }
}
