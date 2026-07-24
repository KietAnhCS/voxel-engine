package com.voxel.game.terrain.structure;

import com.voxel.engine.generation.Carver;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.ColumnSample;
import com.voxel.game.terrain.TerrainShaper;
import com.voxel.game.terrain.carve.CaveSeeds;

import java.util.Random;

/**
 * Grand structures: desert PYRAMIDS and huge stone WATCHTOWERS scattered across the world.
 *
 * <p>How structures can span several chunks even though every chunk generates alone:
 * the world is divided into square CELLS of {@link #CELL_CHUNKS} x {@link #CELL_CHUNKS}
 * chunks. Each cell deterministically decides (seed + cell coordinate) whether it holds a
 * structure, where, and of which kind - so every chunk that overlaps the structure computes
 * the exact same building and simply writes the part that falls inside itself. This is the
 * same replay trick the perlin-worm caves use.
 *
 * <p>Runs LAST in the carver pipeline so buildings stand on the already-carved terrain.
 */
public final class StructureCarver implements Carver {

    /** One structure cell is 12 x 12 chunks = 192 x 192 blocks. */
    private static final int CELL_CHUNKS = 12;
    /** Chance that a cell actually contains a structure. */
    private static final float SPAWN_CHANCE = 0.55f;
    /** Keep structure centers this many blocks away from the cell border. */
    private static final int CELL_MARGIN = 28;
    private static final int SALT = 77;

    private final TerrainShaper shaper;
    private final Blocks blocks;
    private final long seed;

    public StructureCarver(TerrainShaper shaper, Blocks blocks, long seed) {
        this.shaper = shaper;
        this.blocks = blocks;
        this.seed = seed;
    }

    @Override
    public void carve(ChunkWriter writer, int chunkX, int chunkZ, int chunkSize, int worldHeight) {
        StructureWriter out = new StructureWriter(writer, chunkSize, worldHeight);
        int cellX = Math.floorDiv(chunkX, CELL_CHUNKS);
        int cellZ = Math.floorDiv(chunkZ, CELL_CHUNKS);

        // A neighbouring cell's structure can reach into this chunk, so check 3x3 cells.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                buildCell(out, cellX + dx, cellZ + dz, chunkSize);
            }
        }
    }

    private void buildCell(StructureWriter out, int cellX, int cellZ, int chunkSize) {
        Random random = new Random(CaveSeeds.forChunk(seed, cellX, cellZ, SALT));
        if (random.nextFloat() > SPAWN_CHANCE) {
            return;
        }

        int cellBlocks = CELL_CHUNKS * chunkSize;
        int originX = cellX * cellBlocks + CELL_MARGIN + random.nextInt(cellBlocks - CELL_MARGIN * 2);
        int originZ = cellZ * cellBlocks + CELL_MARGIN + random.nextInt(cellBlocks - CELL_MARGIN * 2);

        // Ground height comes from the deterministic terrain formula, so every chunk
        // agrees on it without loading the neighbour chunk.
        ColumnSample sample = new ColumnSample();
        shaper.sample(originX, originZ, sample);
        int ground = (int) Math.floor(sample.surfaceHeight());
        if (ground <= shaper.seaLevel() + 1) {
            return;  // never build in the ocean or on a river bed
        }

        String biome = shaper.biomes().pick(originX, originZ).name();
        if ("desert".equals(biome)) {
            buildPyramid(out, random, originX, ground, originZ);
        } else {
            buildWatchtower(out, random, originX, ground, originZ);
        }
    }

    // ------------------------------------------------------------------ pyramid

    /** A stepped sandstone pyramid with a hidden lit burial chamber and an entrance tunnel. */
    private void buildPyramid(StructureWriter out, Random random, int cx, int ground, int cz) {
        int half = 12 + random.nextInt(4);              // base half-width 12..15 -> up to 31 wide
        int baseY = ground - 2;                          // sink the base so it hugs the dunes

        // Solid stepped body: every layer shrinks by one block per side.
        for (int layer = 0; baseY + layer < out.worldHeight() && layer <= half; layer++) {
            int reach = half - layer;
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    out.place(cx + dx, baseY + layer, cz + dz, blocks.sandstone);
                }
            }
        }

        // Burial chamber in the heart of the pyramid, lit by a glowstone lamp.
        int roomY = baseY + 1;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    out.place(cx + dx, roomY + dy, cz + dz, blocks.air);
                }
            }
        }
        out.place(cx, roomY, cz, blocks.lamp);
        out.place(cx - 3, roomY, cz - 3, blocks.brick);
        out.place(cx + 3, roomY, cz - 3, blocks.brick);
        out.place(cx - 3, roomY, cz + 3, blocks.brick);
        out.place(cx + 3, roomY, cz + 3, blocks.brick);

        // Entrance tunnel from the south face straight into the chamber.
        for (int dz = 4; dz <= half + 2; dz++) {
            for (int dy = 1; dy <= 2; dy++) {
                out.place(cx, roomY + dy - 1, cz + dz, blocks.air);
            }
        }
        out.place(cx, roomY + 2, cz + half, blocks.torch);
    }

    // ---------------------------------------------------------------- watchtower

    /** A huge round cobblestone watchtower with floors, battlements and torch light. */
    private void buildWatchtower(StructureWriter out, Random random, int cx, int ground, int cz) {
        int height = 20 + random.nextInt(8);            // 20..27 blocks tall
        int radius = 5;
        int baseY = ground + 1;

        // Foundation: fill straight down so the tower anchors into slopes.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (ring(dx, dz, radius) >= 0) {
                    for (int y = Math.max(1, ground - 6); y <= ground; y++) {
                        out.place(cx + dx, y, cz + dz, blocks.cobblestone);
                    }
                }
            }
        }

        for (int dy = 0; dy < height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int where = ring(dx, dz, radius);
                    if (where > 0) {
                        out.place(cx + dx, baseY + dy, cz + dz, blocks.cobblestone);
                    } else if (where == 0) {
                        // Hollow interior with a plank floor every 6 blocks.
                        out.place(cx + dx, baseY + dy, cz + dz,
                                dy % 6 == 0 ? blocks.planks : blocks.air);
                    }
                }
            }
        }

        // Doorway on the south wall, two blocks high.
        out.place(cx, baseY, cz + radius, blocks.air);
        out.place(cx, baseY + 1, cz + radius, blocks.air);
        out.place(cx, baseY + 2, cz + radius, blocks.planks);

        // Torches on every floor so the tower glows at night.
        for (int dy = 1; dy < height; dy += 6) {
            out.place(cx, baseY + dy, cz, blocks.torch);
        }

        // Battlements: alternating merlons around the open roof rim.
        int topY = baseY + height;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int where = ring(dx, dz, radius);
                if (where > 0 && (dx + dz & 1) == 0) {
                    out.place(cx + dx, topY, cz + dz, blocks.cobblestone);
                }
            }
        }
        out.place(cx, topY, cz, blocks.lamp);
    }

    /**
     * Where does (dx, dz) fall on a circle of this radius?
     * 1 = on the wall ring, 0 = inside, -1 = outside.
     */
    private static int ring(int dx, int dz, int radius) {
        int d2 = dx * dx + dz * dz;
        if (d2 > radius * radius + radius) {
            return -1;
        }
        return d2 >= (radius - 1) * (radius - 1) + radius ? 1 : 0;
    }
}
