package com.voxel.game.play;

import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * The 2x2 crafting grid inside the bag.
 *
 * Recipes here are "shapeless": they only count what is in the four cells and how
 * many of each, not which cell holds what - like Minecraft's shapeless recipes. The
 * recipe list is just a LIST scanned in order; with a few recipes O(n) is fast
 * enough, and adding a new recipe does not touch the matching logic.
 */
public final class Crafting {

    /** One recipe: how many of one block type it needs, and what it produces. */
    private static final class Recipe {
        final Block ingredient;
        final int amount;
        final Block result;
        final int resultCount;

        Recipe(Block ingredient, int amount, Block result, int resultCount) {
            this.ingredient = ingredient;
            this.amount = amount;
            this.result = result;
            this.resultCount = resultCount;
        }
    }

    public static final int GRID = 4;

    private final List<Recipe> recipes = new ArrayList<Recipe>();
    private final ItemStack[] grid = new ItemStack[GRID];
    private ItemStack result;

    public Crafting(Blocks blocks) {
        // Use the real Minecraft recipes that this block set can make.
        recipes.add(new Recipe(blocks.wood, 1, blocks.planks, 4));
        recipes.add(new Recipe(blocks.birchWood, 1, blocks.planks, 4));
        recipes.add(new Recipe(blocks.sand, 4, blocks.sandstone, 1));
        recipes.add(new Recipe(blocks.cobblestone, 4, blocks.brick, 1));
    }

    public ItemStack get(int index) {
        return grid[index];
    }

    public void set(int index, ItemStack stack) {
        grid[index] = stack != null && stack.isEmpty() ? null : stack;
        refresh();
    }

    public ItemStack result() {
        return result;
    }

    /** Looks at what the four cells hold and finds a matching recipe. */
    private void refresh() {
        result = null;
        Block only = null;
        int count = 0;

        for (ItemStack stack : grid) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (only != null && only != stack.block()) {
                return;  // mixing two different types matches no recipe
            }
            only = stack.block();
            count += stack.count();
        }
        if (only == null) {
            return;
        }

        for (Recipe recipe : recipes) {
            if (recipe.ingredient == only && count >= recipe.amount) {
                result = new ItemStack(recipe.result, recipe.resultCount);
                return;
            }
        }
    }

    /**
     * The player takes the crafted item: the ingredients in the grid are used up.
     *
     * @return the crafted item, or null if nothing can be crafted yet
     */
    public ItemStack take() {
        if (result == null) {
            return null;
        }
        ItemStack taken = result.copy();
        int need = needed(taken.block());
        for (int i = 0; i < GRID && need > 0; i++) {
            ItemStack stack = grid[i];
            while (stack != null && !stack.isEmpty() && need > 0) {
                stack.shrink();
                need--;
                if (stack.isEmpty()) {
                    grid[i] = null;
                    stack = null;
                }
            }
        }
        refresh();
        return taken;
    }

    /** How many ingredients the recipe producing {@code made} requires. */
    private int needed(Block made) {
        for (Recipe recipe : recipes) {
            if (recipe.result == made) {
                return recipe.amount;
            }
        }
        return 1;
    }

    /** Returns every ingredient back to the bag when the panel closes (like Minecraft). */
    public void returnAll(Inventory inventory) {
        for (int i = 0; i < GRID; i++) {
            ItemStack stack = grid[i];
            if (stack != null) {
                for (int n = 0; n < stack.count(); n++) {
                    inventory.add(stack.block());
                }
            }
            grid[i] = null;
        }
        refresh();
    }
}
