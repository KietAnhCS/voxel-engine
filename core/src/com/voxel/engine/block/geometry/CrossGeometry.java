package com.voxel.engine.block.geometry;

import com.badlogic.gdx.graphics.Color;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockGeometry;
import com.voxel.engine.block.BlockView;
import com.voxel.engine.block.QuadEmitter;

public final class CrossGeometry implements BlockGeometry {

    public static final CrossGeometry INSTANCE = new CrossGeometry();

    private static final float SPREAD = 0.15f;

    private final ThreadLocal<FaceLighting> lighting = new ThreadLocal<FaceLighting>() {
        @Override
        protected FaceLighting initialValue() {
            return new FaceLighting();
        }
    };
    private final ThreadLocal<Color> tintBuffer = new ThreadLocal<Color>() {
        @Override
        protected Color initialValue() {
            return new Color();
        }
    };

    private CrossGeometry() {
    }

    @Override
    public void emit(BlockView view, int x, int y, int z, Block block, QuadEmitter emitter) {
        Color color = tintBuffer.get();
        lighting.get().flat(view, x, y, z, block, 0.94f, color);

        float jitterX = hash(x, z, 17) * SPREAD;
        float jitterZ = hash(x, z, 31) * SPREAD;

        String region = block.textureFor(com.voxel.engine.util.Direction.UP);
        emitter.billboard(x, y, z, jitterX, jitterZ, false, region, color);
        emitter.billboard(x, y, z, jitterX, jitterZ, true, region, color);
    }

    @Override
    public boolean occludesNeighbours() {
        return false;
    }

    private static float hash(int x, int z, int salt) {
        int h = x * 73856093 ^ z * 19349663 ^ salt * 83492791;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h >>> 16) & 0xFF) / 255f - 0.5f;
    }
}
