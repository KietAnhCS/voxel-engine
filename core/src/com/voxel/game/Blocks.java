package com.voxel.game;

import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.block.geometry.CrossGeometry;
import com.voxel.engine.block.geometry.CubeGeometry;
import com.voxel.engine.block.geometry.LiquidGeometry;

/**
 * The game's block table. Each block exists as exactly ONE instance that is reused
 * for millions of cells in the world - that is the Flyweight pattern.
 *
 * The image of each block lives in texturepacker/textures/ and is packed with
 * "gradlew :desktop:packTextures".
 */
public final class Blocks {

    public final Block air;
    public final Block stone;
    public final Block cobblestone;
    public final Block sandstone;
    public final Block sand;
    public final Block gravel;
    public final Block dirt;
    public final Block grass;
    public final Block snow;
    public final Block ice;
    public final Block wood;
    public final Block birchWood;
    public final Block leaves;
    public final Block pineLeaves;
    public final Block cactus;
    public final Block lamp;
    /** Torch: a thin block you can walk through that gives off light, like Minecraft. */
    public final Block torch;
    public final Block brick;
    public final Block planks;
    /** Source water block - what the player places. For flowing levels see {@link #waterLevels()}. */
    public final Block water;
    public final Block tuft;
    public final Block deadBush;
    public final Block flower;
    public final Block flowerYellow;

    private final Block[] waterLevels = new Block[Block.MAX_FLUID_LEVEL + 1];

    public Blocks(BlockRegistry registry) {
        air = registry.register(Block.named("air")
                .geometry(CubeGeometry.TRANSLUCENT)
                .texture("textures/stone")
                .translucent()
                .passable());

        stone = registry.register(Block.named("stone")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/stone"));

        cobblestone = registry.register(Block.named("cobblestone")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/cobblestone"));

        sandstone = registry.register(Block.named("sandstone")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/sandstone"));

        sand = registry.register(Block.named("sand")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/sand"));

        gravel = registry.register(Block.named("gravel")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/gravel"));

        dirt = registry.register(Block.named("dirt")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/dirt"));

        grass = registry.register(Block.named("grass")
                .geometry(CubeGeometry.OPAQUE)
                .textures("textures/grass_top", "textures/dirt", "textures/grass_side"));

        snow = registry.register(Block.named("snow")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/snow"));

        // Ice: you can see through it but still stand on it (it has collision).
        ice = registry.register(Block.named("ice")
                .geometry(CubeGeometry.TRANSLUCENT)
                .texture("textures/ice")
                .translucent()
                .attenuation(2));

        wood = registry.register(Block.named("wood")
                .geometry(CubeGeometry.OPAQUE)
                .textures("textures/log_top", "textures/log_top", "textures/log_side"));

        birchWood = registry.register(Block.named("birch_wood")
                .geometry(CubeGeometry.OPAQUE)
                .textures("textures/log_top", "textures/log_top", "textures/birch_side"));

        leaves = registry.register(Block.named("leaves")
                .geometry(CubeGeometry.TRANSLUCENT)
                .texture("textures/leaves_oak")
                .translucent()
                .attenuation(2));

        pineLeaves = registry.register(Block.named("pine_leaves")
                .geometry(CubeGeometry.TRANSLUCENT)
                .texture("textures/leaves_pine")
                .translucent()
                .attenuation(2));

        cactus = registry.register(Block.named("cactus")
                .geometry(CubeGeometry.OPAQUE)
                .textures("textures/cactus_top", "textures/cactus_top", "textures/cactus_side"));

        lamp = registry.register(Block.named("lamp")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/glowstone")
                .luminance(14));

        // The torch reuses the cross shape of grass/flowers so it needs no geometry of
        // its own, and luminance(14) makes it glow exactly like a Minecraft torch.
        torch = registry.register(Block.named("torch")
                .geometry(CrossGeometry.INSTANCE)
                .texture("textures/torch")
                .translucent()
                .passable()
                .luminance(14));

        brick = registry.register(Block.named("brick")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/bricks"));

        planks = registry.register(Block.named("planks")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/planks"));

        // Water has 8 depth levels: level 8 is the source, levels 1..7 are flowing water -
        // the further from the source the lower the surface. Each level is its own Block but
        // they share one shape and one image, so the whole world still uses 8 instances (Flyweight).
        waterLevels[0] = air;
        for (int level = 1; level < Block.MAX_FLUID_LEVEL; level++) {
            waterLevels[level] = registry.register(Block.named("water_" + level)
                    .geometry(LiquidGeometry.INSTANCE)
                    .texture("textures/water")
                    .liquid(level)
                    .attenuation(3));
        }
        water = registry.register(Block.named("water")
                .geometry(LiquidGeometry.INSTANCE)
                .texture("textures/water")
                .liquid(Block.MAX_FLUID_LEVEL)
                .attenuation(3));
        waterLevels[Block.MAX_FLUID_LEVEL] = water;

        // The thin blocks below are all passable(): you can walk through them, like Minecraft.
        tuft = registry.register(Block.named("tuft")
                .geometry(CrossGeometry.INSTANCE)
                .texture("textures/tall_grass")
                .translucent()
                .passable()
                .windAffected());

        deadBush = registry.register(Block.named("dead_bush")
                .geometry(CrossGeometry.INSTANCE)
                .texture("textures/dead_bush")
                .translucent()
                .passable()
                .windAffected());

        flower = registry.register(Block.named("flower")
                .geometry(CrossGeometry.INSTANCE)
                .texture("textures/flower_red")
                .translucent()
                .passable()
                .windAffected());

        flowerYellow = registry.register(Block.named("flower_yellow")
                .geometry(CrossGeometry.INSTANCE)
                .texture("textures/flower_yellow")
                .translucent()
                .passable()
                .windAffected());
    }

    /** Water blocks by level for FluidSimulator: [0] air ... [8] the source block. */
    public Block[] waterLevels() {
        return waterLevels;
    }

    /**
     * The blocks the player can place, in the order shown in the creative block
     * palette. It leaves out "air" and the flowing water levels.
     */
    public Block[] palette() {
        return new Block[]{
                grass, dirt, stone, cobblestone, sand, sandstone, gravel,
                wood, birchWood, planks, brick, leaves, pineLeaves,
                snow, ice, cactus, lamp, torch, water,
                tuft, deadBush, flower, flowerYellow
        };
    }
}
