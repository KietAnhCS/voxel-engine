package com.voxel.game;

import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.block.geometry.CrossGeometry;
import com.voxel.engine.block.geometry.CubeGeometry;
import com.voxel.engine.block.geometry.LiquidGeometry;

/**
 * Bang khoi cua game. Moi khoi chi ton tai DUY NHAT mot the hien va duoc dung lai
 * cho hang trieu o trong the gioi - do la mau Flyweight.
 *
 * Anh cua tung khoi nam trong texturepacker/textures/, dong goi bang
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
    public final Block brick;
    public final Block planks;
    /** Khoi nuoc nguon - thu nguoi choi dat xuong. Cac muc chay xem {@link #waterLevels()}. */
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

        // Bang: nhin xuyen duoc nhung van dung len duoc (co va cham).
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

        brick = registry.register(Block.named("brick")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/bricks"));

        planks = registry.register(Block.named("planks")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/planks"));

        // Nuoc gom 8 muc day: muc 8 la khoi nguon, muc 1..7 la nuoc dang chay - cang xa
        // nguon mat nuoc cang thap. Moi muc la mot Block rieng nhung dung chung mot hinh
        // va mot anh, nen van chi la 8 the hien cho ca the gioi (Flyweight).
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

        // Cac khoi mong duoi day deu passable(): di xuyen qua duoc, giong Minecraft.
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

    /** Bang khoi nuoc theo muc cho FluidSimulator: [0] khong khi ... [8] khoi nguon. */
    public Block[] waterLevels() {
        return waterLevels;
    }
}
