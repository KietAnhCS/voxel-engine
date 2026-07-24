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

    /**
     * Nhet {@code amount} khoi loai nay vao tui: uu tien don vao chong cung loai con cho,
     * het cho moi mo o trong.
     *
     * @return so khoi KHONG nhet duoc vi tui da day
     */
    public int addStack(Block block, int amount) {
        return moveInto(block, amount, 0, SIZE);
    }

    /**
     * Shift + bam chuot: chuyen nhanh ca chong do sang khu vuc con lai - do o thanh nhanh
     * thi bay len kho chua, do trong kho thi rot xuong thanh nhanh. Dung y nhu Minecraft.
     */
    public void quickMove(int index) {
        ItemStack stack = slots[index];
        if (stack == null || stack.isEmpty()) {
            return;
        }
        boolean fromHotbar = index < HOTBAR_SIZE;
        int start = fromHotbar ? HOTBAR_SIZE : 0;
        int end = fromHotbar ? SIZE : HOTBAR_SIZE;

        int leftOver = moveInto(stack.block(), stack.count(), start, end);
        slots[index] = leftOver > 0 ? new ItemStack(stack.block(), leftOver) : null;
    }

    /** Don do vao cac o trong khoang [start, end); tra ve so con thua khong nhet duoc. */
    private int moveInto(Block block, int amount, int start, int end) {
        for (int i = start; i < end && amount > 0; i++) {
            ItemStack target = slots[i];
            if (target != null && target.isSameBlock(block)) {
                amount = target.add(amount);
            }
        }
        for (int i = start; i < end && amount > 0; i++) {
            if (slots[i] == null) {
                int put = Math.min(amount, ItemStack.MAX_COUNT);
                slots[i] = new ItemStack(block, put);
                amount -= put;
            }
        }
        return amount;
    }

    /**
     * Bam chuot PHAI vao mot o: tay khong thi boc len MOT NUA chong, dang cam do thi
     * tha xuong MOT cai - giong Minecraft.
     */
    public void clickSlotRight(int index) {
        ItemStack inSlot = slots[index];
        if (carried == null) {
            if (inSlot == null) {
                return;
            }
            int half = (inSlot.count() + 1) / 2;
            int left = inSlot.count() - half;
            carried = new ItemStack(inSlot.block(), half);
            slots[index] = left > 0 ? new ItemStack(inSlot.block(), left) : null;
            return;
        }
        if (inSlot == null) {
            slots[index] = new ItemStack(carried.block(), 1);
        } else if (inSlot.isSameBlock(carried.block()) && inSlot.room() > 0) {
            inSlot.add(1);
        } else {
            return;
        }
        carried.shrink();
        if (carried.isEmpty()) {
            carried = null;
        }
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

    /**
     * BAM DUP chuot trai: hut het khoi CUNG LOAI dang nam rai rac trong tui vao chong tren
     * tay, toi da 64 - dung nhu Minecraft. Uu tien nhung chong con dang do dang truoc de tui
     * do gon lai, het roi moi dung toi chong day.
     */
    public void collectIntoCarried() {
        if (carried == null || carried.isEmpty()) {
            return;
        }
        gather(false);
        gather(true);
    }

    /** Mot luot hut do vao chong dang cam. {@code full} = co dung toi ca nhung chong da day khong. */
    private void gather(boolean full) {
        for (int i = 0; i < SIZE && carried.room() > 0; i++) {
            ItemStack stack = slots[i];
            if (stack == null || !stack.isSameBlock(carried.block())) {
                continue;
            }
            if (!full && stack.count() >= ItemStack.MAX_COUNT) {
                continue;
            }
            int moved = Math.min(stack.count(), carried.room());
            carried.add(moved);
            for (int n = 0; n < moved; n++) {
                stack.shrink();
            }
            if (stack.isEmpty()) {
                slots[i] = null;
            }
        }
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
