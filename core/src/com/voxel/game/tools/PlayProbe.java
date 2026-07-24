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

        // --- gom do bang cu bam dup chuot ---
        Inventory scattered = new Inventory();
        scattered.set(0, new ItemStack(blocks.dirt, 20));
        scattered.set(3, new ItemStack(blocks.dirt, 30));
        scattered.set(9, new ItemStack(blocks.stone, 40));
        scattered.set(12, new ItemStack(blocks.dirt, 25));
        scattered.setCarried(new ItemStack(blocks.dirt, 5));
        scattered.collectIntoCarried();
        failed += check("bam dup -> gom dat vao mot chong day 64",
                scattered.carried().count() == 64);
        failed += check("bam dup -> khong dung toi loai khac",
                scattered.get(9) != null && scattered.get(9).count() == 40);
        failed += check("bam dup -> phan dat con lai van nam trong tui",
                20 + 30 + 25 + 5 - 64 == countOf(scattered, blocks.dirt));

        // --- shift + bam: chuyen nhanh giua thanh nhanh va kho chua ---
        Inventory moving = new Inventory();
        moving.set(0, new ItemStack(blocks.planks, 12));
        moving.quickMove(0);
        failed += check("shift+bam o thanh nhanh -> do bay len kho chua",
                moving.get(0) == null && moving.get(9) != null && moving.get(9).count() == 12);

        // --- chuot phai: nhat mot nua / tha xuong mot cai ---
        Inventory splitting = new Inventory();
        splitting.set(0, new ItemStack(blocks.stone, 9));
        splitting.clickSlotRight(0);
        failed += check("chuot phai tay khong -> cam mot nua (lam tron len)",
                splitting.carried().count() == 5 && splitting.get(0).count() == 4);
        splitting.clickSlotRight(1);
        failed += check("chuot phai dang cam do -> tha xuong dung mot cai",
                splitting.get(1).count() == 1 && splitting.carried().count() == 4);

        // --- do an ---
        failed += check("qua tao la do an", blocks.isFood(blocks.apple));
        failed += check("khoi dat khong phai do an", !blocks.isFood(blocks.dirt));

        PlayerStats hungryEater = new PlayerStats();
        for (int i = 0; i < 200; i++) {
            hungryEater.update(1f, GameMode.SURVIVAL, true, 60f, false, false, true);
        }
        int beforeEating = hungryEater.food();
        hungryEater.eat(blocks.foodValue(blocks.apple));
        failed += check("an mot qua tao -> day len 4 nac do no",
                hungryEater.food() == Math.min(PlayerStats.MAX_FOOD, beforeEating + 4));

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

    /** Dem tong so khoi loai nay dang nam trong tui (khong tinh cai dang cam tren tay). */
    private static int countOf(Inventory inventory, com.voxel.engine.block.Block block) {
        int total = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (stack != null && stack.isSameBlock(block)) {
                total += stack.count();
            }
        }
        return total;
    }

    private static int check(String what, boolean ok) {
        System.out.println((ok ? "  pass  " : "  FAIL  ") + what);
        return ok ? 0 : 1;
    }
}
