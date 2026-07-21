package com.voxel.game.terrain;

public final class Deterministic {

    private Deterministic() {
    }

    public static float unit(long seed, int x, int z, int salt) {
        long hash = seed;
        hash ^= x * 0x9E3779B97F4A7C15L;
        hash ^= z * 0xC2B2AE3D27D4EB4FL;
        hash ^= salt * 0x165667B19E3779F9L;
        hash ^= hash >>> 33;
        hash *= 0xFF51AFD7ED558CCDL;
        hash ^= hash >>> 33;
        hash *= 0xC4CEB9FE1A85EC53L;
        hash ^= hash >>> 33;
        return (hash >>> 40) / (float) (1 << 24);
    }

    public static int range(long seed, int x, int z, int salt, int minInclusive, int maxInclusive) {
        float value = unit(seed, x, z, salt);
        return minInclusive + (int) (value * (maxInclusive - minInclusive + 1));
    }
}
