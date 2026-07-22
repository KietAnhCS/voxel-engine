package com.voxel.game.play;

import com.voxel.engine.block.Block;

/**
 * One slot in the bag: one block type plus a count. Like Minecraft, each slot
 * holds at most 64 items.
 *
 * This object is mutable so that picking up / placing blocks does not allocate
 * constantly.
 */
public final class ItemStack {

    public static final int MAX_COUNT = 64;

    private Block block;
    private int count;

    public ItemStack(Block block, int count) {
        this.block = block;
        this.count = count;
    }

    public static ItemStack of(Block block) {
        return new ItemStack(block, 1);
    }

    public Block block() {
        return block;
    }

    public int count() {
        return count;
    }

    public boolean isEmpty() {
        return block == null || count <= 0;
    }

    public boolean isSameBlock(Block other) {
        return block == other;
    }

    public int room() {
        return MAX_COUNT - count;
    }

    /**
     * Stuffs more items into this slot.
     *
     * @return how many items did NOT fit because the slot is full
     */
    public int add(int amount) {
        int fits = Math.min(amount, room());
        count += fits;
        return amount - fits;
    }

    /** Removes one item; when none are left the slot becomes empty. */
    public void shrink() {
        count--;
        if (count <= 0) {
            block = null;
            count = 0;
        }
    }

    public ItemStack copy() {
        return new ItemStack(block, count);
    }
}
