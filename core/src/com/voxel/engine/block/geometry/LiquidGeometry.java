package com.voxel.engine.block.geometry;

import com.badlogic.gdx.graphics.Color;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockGeometry;
import com.voxel.engine.block.BlockView;
import com.voxel.engine.block.QuadEmitter;
import com.voxel.engine.util.Direction;

public final class LiquidGeometry implements BlockGeometry {

    public static final LiquidGeometry INSTANCE = new LiquidGeometry();

    private static final float SURFACE = 0.875f;
    private static final float UNDERSIDE = 0.873f;

    private static final Direction[] SIDES = {
            Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};

    private final ThreadLocal<FaceLighting> lighting = new ThreadLocal<FaceLighting>() {
        @Override
        protected FaceLighting initialValue() {
            return new FaceLighting();
        }
    };

    private LiquidGeometry() {
    }

    @Override
    public void emit(BlockView view, int x, int y, int z, Block block, QuadEmitter emitter) {
        FaceLighting shader = lighting.get();
        Color[] corners = emitter.cornerBuffer();
        String region = block.textureFor(Direction.UP);

        boolean surface = view.blockAt(x, y + 1, z) != block;

        if (surface) {
            shader.shade(view, x, y, z, block, Direction.UP, corners);
            emitter.quad(x, y - (1f - SURFACE), z, Direction.UP, 1f, 1f, region, corners);

            shader.shade(view, x, y, z, block, Direction.DOWN, corners);
            emitter.quad(x, y + UNDERSIDE, z, Direction.DOWN, 1f, 1f, region, corners);
        }

        for (int i = 0; i < SIDES.length; i++) {
            Direction face = SIDES[i];
            if (view.occludes(x + face.dx(), y + face.dy(), z + face.dz(), block)) {
                continue;
            }
            shader.shade(view, x, y, z, block, face, corners);
            emitter.quad(x, y, z, face, 1f, surface ? SURFACE : 1f, region, corners);
        }

        if (!view.occludes(x, y - 1, z, block)) {
            shader.shade(view, x, y, z, block, Direction.DOWN, corners);
            emitter.quad(x, y, z, Direction.DOWN, 1f, 1f, region, corners);
        }
    }

    @Override
    public boolean occludesNeighbours() {
        return false;
    }
}
