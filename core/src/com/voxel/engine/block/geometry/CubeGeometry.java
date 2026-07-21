package com.voxel.engine.block.geometry;

import com.badlogic.gdx.graphics.Color;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockGeometry;
import com.voxel.engine.block.BlockView;
import com.voxel.engine.block.QuadEmitter;
import com.voxel.engine.util.Direction;

public final class CubeGeometry implements BlockGeometry {

    public static final CubeGeometry OPAQUE = new CubeGeometry(true);
    public static final CubeGeometry TRANSLUCENT = new CubeGeometry(false);

    private final boolean occluding;
    private final ThreadLocal<FaceLighting> lighting = new ThreadLocal<FaceLighting>() {
        @Override
        protected FaceLighting initialValue() {
            return new FaceLighting();
        }
    };

    private CubeGeometry(boolean occluding) {
        this.occluding = occluding;
    }

    @Override
    public void emit(BlockView view, int x, int y, int z, Block block, QuadEmitter emitter) {
        FaceLighting shader = lighting.get();
        Color[] corners = emitter.cornerBuffer();

        for (int i = 0; i < Direction.ALL.length; i++) {
            Direction face = Direction.ALL[i];
            if (view.occludes(x + face.dx(), y + face.dy(), z + face.dz(), block)) {
                continue;
            }
            shader.shade(view, x, y, z, block, face, corners);
            emitter.quad(x, y, z, face, 1f, 1f, block.textureFor(face), corners);
        }
    }

    @Override
    public boolean occludesNeighbours() {
        return occluding;
    }
}
