package com.voxel.game.tools;

import com.voxel.engine.block.BlockRegistry;
import com.voxel.game.Blocks;
import com.voxel.game.play.Crafting;
import com.voxel.game.play.GameMode;
import com.voxel.game.play.Inventory;
import com.voxel.game.play.ItemStack;
import com.voxel.game.play.PlayerStats;

/** Checks the gameplay rules without opening a graphics window. */
public final class PlayProbe {

    private PlayProbe() {
    }

    public static void main(String[] args) {
        Blocks blocks = new Blocks(new BlockRegistry());
        int failed = 0;

        // --- crafting table ---
        Crafting crafting = new Crafting(blocks);
        crafting.set(0, new ItemStack(blocks.wood, 1));
        failed += check("1 log -> 4 planks",
                crafting.result() != null && crafting.result().block() == blocks.planks
                        && crafting.result().count() == 4);
        ItemStack made = crafting.take();
        failed += check("grid is empty after taking the result", made != null && crafting.get(0) == null);

        for (int i = 0; i < 4; i++) {
            crafting.set(i, new ItemStack(blocks.sand, 1));
        }
        failed += check("4 sand -> 1 sandstone",
                crafting.result() != null && crafting.result().block() == blocks.sandstone);

        crafting.set(0, new ItemStack(blocks.dirt, 1));
        failed += check("mixing two block types -> no recipe", crafting.result() == null);

        // --- inventory ---
        Inventory inventory = new Inventory();
        for (int i = 0; i < 70; i++) {
            inventory.add(blocks.stone);
        }
        failed += check("70 stone -> one stack of 64 and one of 6",
                inventory.get(0).count() == 64 && inventory.get(1).count() == 6);
        failed += check("removing one block", inventory.consume(blocks.stone) && inventory.get(0).count() == 63);

        // --- health, hunger, experience ---
        PlayerStats stats = new PlayerStats();
        stats.addExperience(7);
        failed += check("7 points -> reach level 1", stats.level() == 1);

        stats.update(1f, GameMode.SURVIVAL, false, 60f, false, false, false);
        stats.update(1f, GameMode.SURVIVAL, true, 50f, false, false, false);
        failed += check("falling 10 blocks -> lose 7 health", stats.health() == 13);

        PlayerStats hungry = new PlayerStats();
        for (int i = 0; i < 60; i++) {
            hungry.update(1f, GameMode.SURVIVAL, true, 60f, false, false, true);
        }
        failed += check("walking 60 seconds -> lose 3 hunger points", hungry.food() == 17);

        PlayerStats drowning = new PlayerStats();
        for (int i = 0; i < 20; i++) {
            drowning.update(1f, GameMode.SURVIVAL, true, 60f, true, true, false);
        }
        failed += check("drowning 20 seconds -> no air left and health lost",
                drowning.air() == 0f && drowning.health() < PlayerStats.MAX_HEALTH);

        System.out.println(failed == 0 ? "ALL CHECKS PASSED" : failed + " CHECKS FAILED");
    }

    private static int check(String what, boolean ok) {
        System.out.println((ok ? "  pass  " : "  FAIL  ") + what);
        return ok ? 0 : 1;
    }
}
