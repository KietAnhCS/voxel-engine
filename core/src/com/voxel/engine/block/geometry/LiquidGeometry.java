package com.voxel.engine.block.geometry;

import com.badlogic.gdx.graphics.Color;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockGeometry;
import com.voxel.engine.block.BlockView;
import com.voxel.engine.block.QuadEmitter;
import com.voxel.engine.util.Direction;

/**
 * Hinh cua mot o chat long.
 *
 * Mat nuoc khong nam sat tran o ma thap xuong theo MUC cua khoi (1..8), nen mot dong
 * nuoc chay ra xa nguon se mong dan - nhin thay ro huong chay. O nao co nuoc ngay tren
 * dau thi coi nhu day o (khong ve mat tren) de trong long ho khong bi ke soc.
 */
public final class LiquidGeometry implements BlockGeometry {

    public static final LiquidGeometry INSTANCE = new LiquidGeometry();

    /** Do cao mat nuoc cua khoi nguon; cac muc thap hon lay theo ti le. */
    private static final float FULL_SURFACE = 0.875f;
    /** Mat duoi cua mang nuoc, lui xuong mot chut de khong z-fight voi mat tren. */
    private static final float SKIN = 0.002f;

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

        float top = topOf(view, x, y, z, block);
        boolean covered = top == 1f;

        if (!covered) {
            shader.shade(view, x, y, z, block, Direction.UP, corners);
            emitter.quad(x, y - (1f - top), z, Direction.UP, 1f, 1f, region, corners);

            shader.shade(view, x, y, z, block, Direction.DOWN, corners);
            emitter.quad(x, y + top - SKIN, z, Direction.DOWN, 1f, 1f, region, corners);
        }

        for (int i = 0; i < SIDES.length; i++) {
            Direction face = SIDES[i];
            if (!needsSide(view, x, y, z, block, face, top)) {
                continue;
            }
            shader.shade(view, x, y, z, block, face, corners);
            emitter.quad(x, y, z, face, 1f, top, region, corners);
        }

        if (!view.blockAt(x, y - 1, z).isLiquid() && !view.occludes(x, y - 1, z, block)) {
            shader.shade(view, x, y, z, block, Direction.DOWN, corners);
            emitter.quad(x, y, z, Direction.DOWN, 1f, 1f, region, corners);
        }
    }

    /**
     * Chi ve mat ben khi co gi do de nhin thay: o ben canh la nuoc THAP hon (lo ra mot
     * bac nuoc) hoac la o trong. Nuoc cao bang hoac cao hon thi mat nay nam trong long
     * nuoc, ve chi ton tam giac.
     */
    private boolean needsSide(BlockView view, int x, int y, int z, Block block, Direction face, float top) {
        int nx = x + face.dx();
        int nz = z + face.dz();
        Block neighbour = view.blockAt(nx, y, nz);
        if (neighbour.isLiquid()) {
            return topOf(view, nx, y, nz, neighbour) < top;
        }
        return !view.occludes(nx, y, nz, block);
    }

    /** Do cao mat nuoc cua o: day o neu con nuoc de len tren, khong thi theo muc. */
    private float topOf(BlockView view, int x, int y, int z, Block block) {
        if (view.blockAt(x, y + 1, z).isLiquid()) {
            return 1f;
        }
        return FULL_SURFACE * block.fluidLevel() / Block.MAX_FLUID_LEVEL;
    }

    @Override
    public boolean occludesNeighbours() {
        return false;
    }
}
