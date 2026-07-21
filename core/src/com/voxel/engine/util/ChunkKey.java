package com.voxel.engine.util;

public final class ChunkKey {

    private ChunkKey() {
    }

    public static long of(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int x(long key) {
        return (int) (key >> 32);
    }

    public static int z(long key) {
        return (int) key;
    }

    public static String describe(long key) {
        return "(" + x(key) + ", " + z(key) + ")";
    }
}
