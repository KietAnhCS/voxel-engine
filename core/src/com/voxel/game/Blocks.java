package com.voxel.game;

import com.badlogic.gdx.graphics.Color;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.block.BlockTint;
import com.voxel.engine.block.geometry.CrossGeometry;
import com.voxel.engine.block.geometry.CubeGeometry;
import com.voxel.engine.block.geometry.LiquidGeometry;

public final class Blocks {

    private static final Color[] FOLIAGE = {
            new Color(0.36f, 0.68f, 0.28f, 1f),
            new Color(0.44f, 0.72f, 0.30f, 1f),
            new Color(0.52f, 0.70f, 0.26f, 1f),
            new Color(0.62f, 0.66f, 0.24f, 1f)};

    private static final Color[] PETALS = {
            new Color(0.95f, 0.86f, 0.24f, 1f),
            new Color(0.86f, 0.28f, 0.26f, 1f),
            new Color(0.42f, 0.36f, 0.88f, 1f),
            new Color(0.95f, 0.54f, 0.16f, 1f)};

    public final Block air;
    public final Block stone;
    public final Block sandstone;
    public final Block shale;
    public final Block dirt;
    public final Block grass;
    public final Block wood;
    public final Block leaves;
    public final Block lamp;
    public final Block brick;
    public final Block water;
    public final Block tuft;
    public final Block flower;

    public Blocks(BlockRegistry registry) {
        air = registry.register(Block.named("air")
                .geometry(CubeGeometry.TRANSLUCENT)
                .texture("textures/limestone")
                .translucent()
                .passable());

        stone = registry.register(Block.named("stone")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/limestone"));

        sandstone = registry.register(Block.named("sandstone")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/sandstone"));

        shale = registry.register(Block.named("shale")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/shale"));

        dirt = registry.register(Block.named("dirt")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/dirt"));

        grass = registry.register(Block.named("grass")
                .geometry(CubeGeometry.OPAQUE)
                .textures("textures/grass_top", "textures/dirt", "textures/grass_sides"));

        wood = registry.register(Block.named("wood")
                .geometry(CubeGeometry.OPAQUE)
                .textures("textures/trunk_top", "textures/trunk_top", "textures/trunk"));

        leaves = registry.register(Block.named("leaves")
                .geometry(CubeGeometry.TRANSLUCENT)
                .texture("textures/uncolored_leaves")
                .translucent()
                .attenuation(2)
                .tint(paletteTint(FOLIAGE, 4177)));

        lamp = registry.register(Block.named("lamp")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/lightbox")
                .luminance(14));

        brick = registry.register(Block.named("brick")
                .geometry(CubeGeometry.OPAQUE)
                .texture("textures/wall"));

        water = registry.register(Block.named("water")
                .geometry(LiquidGeometry.INSTANCE)
                .texture("textures/water")
                .liquid()
                .attenuation(3));

        tuft = registry.register(Block.named("tuft")
                .geometry(CrossGeometry.INSTANCE)
                .texture("textures/straw")
                .translucent()
                .passable()
                .windAffected()
                .tint(paletteTint(FOLIAGE, 9311)));

        flower = registry.register(Block.named("flower")
                .geometry(CrossGeometry.INSTANCE)
                .texture("textures/flower")
                .translucent()
                .passable()
                .windAffected()
                .tint(paletteTint(PETALS, 6151)));
    }

    private static BlockTint paletteTint(final Color[] palette, final int salt) {
        return new BlockTint() {
            @Override
            public void apply(int x, int y, int z, Color out) {
                int hash = x * 73856093 ^ z * 19349663 ^ y * 83492791 ^ salt;
                hash = (hash ^ (hash >>> 15)) * 668265261;
                out.set(palette[(hash >>> 24) % palette.length]);
            }
        };
    }
}
