package com.voxel.game.play;

import com.voxel.engine.block.Block;

/**
 * The player's bag, laid out exactly like Minecraft:
 *
 * <pre>
 *   slots 0..8   : hotbar - what is currently held in hand
 *   slots 9..35  : storage, only visible when E is pressed
 * </pre>
 *
 * The data structure is just a fixed ARRAY of 36 elements - looking up any slot is
 * O(1). When picking up a block the array must be scanned for a slot of the same
 * type with room left (O(n) with n = 36), in exchange for no extra memory.
 */
public final class Inventory {

    public static final int HOTBAR_SIZE = 9;
    public static final int STORAGE_ROWS = 3;
    public static final int SIZE = HOTBAR_SIZE + STORAGE_ROWS * HOTBAR_SIZE;

    private final ItemStack[] slots = new ItemStack[SIZE];
    /** The item stuck to the mouse cursor while rearranging the bag (like Minecraft). */
    private ItemStack carried;
    private int selected;

    public int size() {
        return SIZE;
    }

    public ItemStack get(int index) {
        return slots[index];
    }

    public void set(int index, ItemStack stack) {
        slots[index] = stack != null && stack.isEmpty() ? null : stack;
    }

    public int selected() {
        return selected;
    }

    public void select(int index) {
        this.selected = Math.max(0, Math.min(HOTBAR_SIZE - 1, index));
    }

    /** Moves to the next slot on mouse scroll; scrolling past one end wraps around. */
    public void scroll(int amount) {
        selected = Math.floorMod(selected + amount, HOTBAR_SIZE);
    }

    public ItemStack selectedStack() {
        return slots[selected];
    }

    public Block selectedBlock() {
        ItemStack stack = slots[selected];
        return stack == null ? null : stack.block();
    }

    /** Removes one block from the held slot - called after a block is placed. */
    public void consumeSelected() {
        ItemStack stack = slots[selected];
        if (stack == null) {
            return;
        }
        stack.shrink();
        if (stack.isEmpty()) {
            slots[selected] = null;
        }
    }

    /**
     * Picks a block into the bag: prefers stacking onto an existing slot of the same
     * type, and otherwise finds the first empty slot. The hotbar is checked first for
     * convenience.
     *
     * @return false if the bag is full and nothing fits
     */
    public boolean add(Block block) {
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = slots[i];
            if (stack != null && stack.isSameBlock(block) && stack.room() > 0) {
                stack.add(1);
                return true;
            }
        }
        for (int i = 0; i < SIZE; i++) {
            if (slots[i] == null) {
                slots[i] = ItemStack.of(block);
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a block of the right type in the bag and removes one of it.
     *
     * @return false if the bag does not contain this block
     */
    public boolean consume(Block block) {
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = slots[i];
            if (stack != null && stack.isSameBlock(block)) {
                stack.shrink();
                if (stack.isEmpty()) {
                    slots[i] = null;
                }
                return true;
            }
        }
        return false;
    }

    public ItemStack carried() {
        return carried;
    }

    public void setCarried(ItemStack stack) {
        this.carried = stack != null && stack.isEmpty() ? null : stack;
    }

    /**
     * Clicking a slot: with an empty hand it picks the item up, while carrying one it
     * puts it down. If the target slot holds a different item the two are swapped.
     */
    public void clickSlot(int index) {
        ItemStack inSlot = slots[index];
        if (carried == null) {
            carried = inSlot;
            slots[index] = null;
            return;
        }
        if (inSlot != null && inSlot.isSameBlock(carried.block())) {
            int leftOver = inSlot.add(carried.count());
            carried = leftOver > 0 ? new ItemStack(carried.block(), leftOver) : null;
            return;
        }
        slots[index] = carried;
        carried = inSlot;
    }

    /** Throws away the carried item (pressing ESC or closing the bag while holding it). */
    public void dropCarried() {
        carried = null;
    }

    public void clear() {
        for (int i = 0; i < SIZE; i++) {
            slots[i] = null;
        }
        carried = null;
    }
}
