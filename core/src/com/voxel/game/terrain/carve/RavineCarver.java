package com.voxel.game.terrain.carve;

import com.voxel.engine.generation.Carver;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;

import java.util.Random;

/**
 * Khe nut (ravine): mot vet nut dai, hep ngang nhung sau hun hut - hiem gap.
 * Cung y tuong perlin worm nhung khoet hinh elip cao thay vi hinh cau.
 */
public final class RavineCarver implements Carver {

    private static final int CHUNK_RADIUS = 5;
    private static final int SPAWN_RARITY = 90;

    private final TerrainNoise noise;
    private final Blocks blocks;
    private final long seed;

    public RavineCarver(TerrainNoise noise, Blocks blocks, long seed) {
        this.noise = noise;
        this.blocks = blocks;
        this.seed = seed;
    }

    @Override
    public void carve(ChunkWriter writer, int chunkX, int chunkZ, int chunkSize, int worldHeight) {
        CarveScope scope = new CarveScope(writer, blocks,
                writer.originX(), writer.originZ(), chunkSize, worldHeight);

        for (int offsetX = -CHUNK_RADIUS; offsetX <= CHUNK_RADIUS; offsetX++) {
            for (int offsetZ = -CHUNK_RADIUS; offsetZ <= CHUNK_RADIUS; offsetZ++) {
                spawnRavine(scope, chunkX + offsetX, chunkZ + offsetZ, chunkSize);
            }
        }
    }

    private void spawnRavine(CarveScope scope, int sourceChunkX, int sourceChunkZ, int chunkSize) {
        Random random = new Random(CaveSeeds.forChunk(seed, sourceChunkX, sourceChunkZ, 2));
        if (random.nextInt(SPAWN_RARITY) != 0) {
            return;
        }

        double x = sourceChunkX * chunkSize + random.nextInt(chunkSize);
        double z = sourceChunkZ * chunkSize + random.nextInt(chunkSize);
        double y = 24 + random.nextInt(20);
        double heading = random.nextDouble() * Math.PI * 2.0;
        double wobble = random.nextDouble() * 512.0;

        int steps = 90 + random.nextInt(70);
        double width = 2.0 + random.nextDouble() * 1.6;
        double depth = 11.0 + random.nextDouble() * 9.0;

        for (int step = 0; step < steps; step++) {
            heading += noise.worm(step * 0.04, wobble) * 0.20;
            x += Math.cos(heading);
            z += Math.sin(heading);
            y += noise.worm(step * 0.03, wobble + 256.0) * 0.5;

            double taper = Math.sin(Math.PI * (step + 1.0) / (steps + 1.0));
            double radiusX = width * (0.4 + 0.9 * taper);
            double radiusY = depth * (0.4 + 0.9 * taper);

            if (scope.touches(x, z, radiusX + 1.0)) {
                scope.clearEllipsoid(x, y, z, radiusX, radiusY, radiusX);
            }
        }
    }
}
