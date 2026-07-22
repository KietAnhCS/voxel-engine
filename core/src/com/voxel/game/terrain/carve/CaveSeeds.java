package com.voxel.game.terrain.carve;

/**
 * Bam toa do chunk thanh mot seed on dinh, de moi chunk luon sinh cung mot he thong hang
 * du no duoc nap lai bao nhieu lan hay duoc nhin tu chunk ben canh.
 */
public final class CaveSeeds {

    private CaveSeeds() {
    }

    public static long forChunk(long worldSeed, int chunkX, int chunkZ, int salt) {
        long hash = worldSeed;
        hash ^= chunkX * 0x9E3779B97F4A7C15L;
        hash ^= chunkZ * 0xC2B2AE3D27D4EB4FL;
        hash ^= salt * 0x165667B19E3779F9L;
        hash ^= hash >>> 33;
        hash *= 0xFF51AFD7ED558CCDL;
        hash ^= hash >>> 33;
        hash *= 0xC4CEB9FE1A85EC53L;
        hash ^= hash >>> 33;
        return hash;
    }
}
