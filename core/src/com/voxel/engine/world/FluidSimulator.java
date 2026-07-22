package com.voxel.engine.world;

import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.util.Direction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Minecraft-style flowing water.
 *
 * Every water cell has a LEVEL from 1 to 8: 8 is a source block (full cell), the smaller
 * the number the shallower the cell. The whole behaviour lives in a SINGLE formula,
 * applied to air cells and to water cells that are not sources:
 *
 *     new level = 8                              if 2 or more source blocks are adjacent (sea spreading)
 *               = 7                              if there is water directly above (water pouring down)
 *               = max(level of the 4 side cells) - 1      otherwise, minimum 0
 *
 * Level 0 means the cell turns back into air. One formula covers both directions:
 *  - Placing a source: the cells around it get 7, then 6, 5... so water spreads exactly 7 cells.
 *  - Removing a source: the largest level left in the area must drop by at least 1 each round
 *    (a cell only keeps level M while it has a neighbour of level M+1), so the water drains
 *    away on its own. No separate rule for draining is needed.
 *
 * Water starts flowing from two places: the player placing/breaking a block ({@link #schedule}),
 * and the terrain that was just generated ({@link #seed}) - that way seas and lakes pour into
 * caves without anyone touching them.
 *
 * How it runs: keep a QUEUE of cells that need recomputing. Each round take out THE WHOLE
 * current queue and compute it; any cell that changes goes through {@link World#setBlock},
 * which pushes its 6 neighbours into the queue for the next round. So each round the water
 * spreads exactly one cell further - the flow creeps outwards instead of appearing at once.
 *
 * Complexity: O(k) per round with k = number of waiting cells. Placing water once only touches
 * cells within the spread radius (7 cells), so k is bounded and does not depend on world size.
 */
public final class FluidSimulator {

    /** Level of water falling from above: almost a full cell, but not a source block. */
    private static final int FALLING_LEVEL = 7;
    /** Water spreads one more cell every this many physics steps: 4 rounds/second, the Minecraft rhythm. */
    private static final int STEPS_PER_SPREAD = 15;
    /** Upper bound on cells processed per round so a water explosion does not freeze the frame rate. */
    private static final int MAX_CELLS_PER_SPREAD = 4096;

    private static final Direction[] SIDES = {
            Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};

    private final World world;
    /** byLevel[0] = air, byLevel[1..7] = flowing water, byLevel[8] = source block. */
    private final Block[] byLevel;
    /**
     * Inbox of the cells that were just scheduled. Chunks are generated on a separate thread
     * so {@link #seed} runs off the main thread - the inbox is the only place the two threads meet.
     */
    private final Queue<Long> inbox = new ConcurrentLinkedQueue<Long>();
    private final Set<Long> pending = new LinkedHashSet<Long>();
    private final List<Long> batch = new ArrayList<Long>();
    /** Chunks changed in this round, so lighting is recomputed only ONCE per chunk. */
    private final Set<Chunk> dirty = new LinkedHashSet<Chunk>();
    private int steps;

    public FluidSimulator(World world, Block[] byLevel) {
        if (byLevel.length != Block.MAX_FLUID_LEVEL + 1) {
            throw new IllegalArgumentException("need exactly " + (Block.MAX_FLUID_LEVEL + 1) + " water levels");
        }
        this.world = world;
        this.byLevel = byLevel.clone();
    }

    /** Marks that this cell and its 6 neighbours must be recomputed next round. */
    public void schedule(int x, int y, int z) {
        inbox.add(pack(x, y, z));
        for (int i = 0; i < Direction.ALL.length; i++) {
            Direction side = Direction.ALL[i];
            inbox.add(pack(x + side.dx(), y + side.dy(), z + side.dz()));
        }
    }

    /**
     * Schedules the water cells that can flow as soon as a chunk is generated, so seas and
     * lakes pour into the caves carved into their walls - without the player digging anything.
     *
     * Only water cells WITH AN OUTLET are scheduled: the cell below or one of the 4 sides is
     * empty. On a flat sea surface no cell satisfies this, so a whole ocean is never woken up
     * for nothing. Neighbours outside the chunk are skipped - the next chunk scans its own part.
     *
     * Complexity: O(size^2 * height) once per chunk, and it only reads arrays inside the chunk.
     */
    public void seed(Chunk chunk) {
        ChunkStorage storage = chunk.storage();
        BlockRegistry registry = chunk.registry();
        int size = storage.size();

        for (int y = 1; y < storage.height(); y++) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    if (!registry.byId(storage.blockId(x, y, z)).isLiquid()) {
                        continue;
                    }
                    if (hasOutlet(storage, registry, x, y, z)) {
                        schedule(chunk.originX() + x, y, chunk.originZ() + z);
                    }
                }
            }
        }
    }

    private boolean hasOutlet(ChunkStorage storage, BlockRegistry registry, int x, int y, int z) {
        if (isOpen(storage, registry, x, y - 1, z)) {
            return true;
        }
        for (int i = 0; i < SIDES.length; i++) {
            if (isOpen(storage, registry, x + SIDES[i].dx(), y, z + SIDES[i].dz())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOpen(ChunkStorage storage, BlockRegistry registry, int x, int y, int z) {
        return storage.contains(x, y, z) && registry.byId(storage.blockId(x, y, z)).isAir();
    }

    /** Called on every physics step. */
    public void tick() {
        if (++steps < STEPS_PER_SPREAD) {
            return;
        }
        steps = 0;

        for (Long cell = inbox.poll(); cell != null; cell = inbox.poll()) {
            pending.add(cell);
        }
        if (pending.isEmpty()) {
            return;
        }

        // Copy the queue out then clear it: cells added while computing belong to the next round.
        batch.clear();
        batch.addAll(pending);
        pending.clear();

        int handled = Math.min(batch.size(), MAX_CELLS_PER_SPREAD);
        for (int i = 0; i < handled; i++) {
            update(batch.get(i));
        }
        for (int i = handled; i < batch.size(); i++) {
            pending.add(batch.get(i));
        }

        // The whole round recomputes lighting and rebuilds geometry ONCE per chunk, instead of
        // once per changed cell. This is what decides between smooth and stuttering.
        for (Chunk chunk : dirty) {
            world.relightAsync(chunk, true);
        }
        dirty.clear();
    }

    private void update(long cell) {
        int x = unpackX(cell);
        int y = unpackY(cell);
        int z = unpackZ(cell);

        Block current = world.blockAt(x, y, z);
        if (current.fluidLevel() == Block.MAX_FLUID_LEVEL) {
            return;                                     // a source block never has to move
        }
        if (!current.isAir() && !current.isLiquid()) {
            return;                                     // solid ground: water cannot enter
        }

        int target = inflow(x, y, z);
        if (target != current.fluidLevel() && world.setBlockDeferred(x, y, z, byLevel[target])) {
            markDirty(x, z);
        }
    }

    /**
     * Remembers the chunk holding the changed cell, plus the neighbouring chunks: for a cell
     * right at the border, the neighbour's geometry and lighting must be rebuilt too.
     */
    private void markDirty(int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk chunk = world.chunkContaining(x + dx, z + dz);
                if (chunk != null) {
                    dirty.add(chunk);
                }
            }
        }
    }

    /** The water level this cell will have next round - exactly the formula described in the class javadoc. */
    private int inflow(int x, int y, int z) {
        int best = 0;
        int sources = 0;
        for (int i = 0; i < SIDES.length; i++) {
            int nx = x + SIDES[i].dx();
            int nz = z + SIDES[i].dz();
            int level = world.blockAt(nx, y, nz).fluidLevel();
            if (level == Block.MAX_FLUID_LEVEL) {
                sources++;
            }
            if (level - 1 > best && !spillsDown(nx, y, nz)) {
                best = level - 1;
            }
        }

        // A cell between two source blocks becomes a source itself. Thanks to this rule a trench
        // dug out to the sea fills up as real sea water instead of a shallow trickle, and the
        // water in the sea never "drains away".
        if (sources >= 2) {
            return Block.MAX_FLUID_LEVEL;
        }
        if (world.blockAt(x, y + 1, z).isLiquid()) {
            return FALLING_LEVEL;
        }
        return best;
    }

    /**
     * Does this water cell have empty space below it? If so it pours straight down instead of
     * spreading sideways - that way water falling from a height forms a column instead of a
     * puddle splayed out in mid-air, exactly like Minecraft.
     */
    private boolean spillsDown(int x, int y, int z) {
        Block below = world.blockAt(x, y - 1, z);
        return below.isAir() || (below.isLiquid() && below.fluidLevel() < Block.MAX_FLUID_LEVEL);
    }

    /**
     * Packs a world coordinate into one long to use as a queue key: 26 bits for x, 12 bits
     * for y, 26 bits for z. That way no coordinate object has to be allocated per cell.
     */
    private static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | ((long) z & 0x3FFFFFFL);
    }

    private static int unpackX(long cell) {
        return (int) (cell >> 38);
    }

    private static int unpackY(long cell) {
        return (int) (cell >> 26) & 0xFFF;
    }

    private static int unpackZ(long cell) {
        return (int) (cell << 38 >> 38);
    }
}
