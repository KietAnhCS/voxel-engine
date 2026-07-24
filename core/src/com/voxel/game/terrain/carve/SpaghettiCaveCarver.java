package com.voxel.game.terrain.carve;

import com.voxel.engine.generation.Carver;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;

/**
 * "Spaghetti" caves - the signature cave type of modern Minecraft (1.18+).
 *
 * <p>Idea: each 3D noise field is zero along a smooth SURFACE inside the world. The
 * INTERSECTION of two such surfaces is a long thin CURVE, so carving every block where
 * BOTH fields are close to zero produces endless narrow tunnels that twist through the
 * whole underground and connect the bigger cheese caverns and perlin-worm tunnels into
 * one explorable network.
 *
 * <p>The tunnel width breathes along the way (the allowed distance from zero grows a bit
 * with depth), so deep tunnels are comfortably walkable while tunnels near the surface
 * pinch shut on their own. {@link SurfaceGuard#withEntrances} still rules how close to
 * the surface they may break through, which is what creates natural cave entrances.
 */
public final class SpaghettiCaveCarver implements Carver {

    /** Base half-width of a tunnel, in noise units - roughly a 2-3 block wide corridor. */
    private static final double THICKNESS = 0.055;
    /** Extra width gained per block of depth below sea level (deep tunnels get roomier). */
    private static final double WIDEN_PER_DEPTH = 0.0007;
    private static final int MIN_Y = 6;
    /** Spaghetti may climb slightly above sea level so entrances can appear on hillsides. */
    private static final int CEILING_ABOVE_SEA = 8;

    private final TerrainNoise noise;
    private final Blocks blocks;

    public SpaghettiCaveCarver(TerrainNoise noise, Blocks blocks) {
        this.noise = noise;
        this.blocks = blocks;
    }

    @Override
    public void carve(ChunkWriter writer, int chunkX, int chunkZ, int chunkSize, int worldHeight) {
        CarveScope scope = new CarveScope(writer, blocks, SurfaceGuard.withEntrances(noise),
                writer.originX(), writer.originZ(), chunkSize, worldHeight);
        int maxY = Math.min(worldHeight - 2, noise.seaLevel() + CEILING_ABOVE_SEA);

        for (int localX = 0; localX < chunkSize; localX++) {
            int worldX = scope.originX() + localX;
            for (int localZ = 0; localZ < chunkSize; localZ++) {
                int worldZ = scope.originZ() + localZ;
                for (int y = MIN_Y; y <= maxY; y++) {
                    double width = THICKNESS + Math.max(0, noise.seaLevel() - y) * WIDEN_PER_DEPTH;
                    if (Math.abs(noise.spaghettiA(worldX, y, worldZ)) < width
                            && Math.abs(noise.spaghettiB(worldX, y, worldZ)) < width) {
                        scope.clear(localX, y, localZ);
                    }
                }
            }
        }
    }
}
