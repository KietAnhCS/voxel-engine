package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;

/**
 * Cay thong (van sam) kieu Minecraft: tan la KHONG phai hinh non tron ma xep thanh
 * nhieu tang vay giat cap.
 *
 * Cach tao ra cac tang: di tu ngon xuong goc, ban kinh phinh to dan len; nhung moi
 * khi cham tran cua tang hien tai thi tut han ve 1 va tran duoc noi them mot don vi.
 * Cu the ma lap lai, thanh ra than cay bi that lai o cho tut, va do la duong ranh
 * giua hai tang vay. Ban truoc dung cong thuc ban kinh giam deu nen ra hinh non tron
 * mut - trong khong ra van sam.
 */
public final class PineShape implements TreeShape {

    @Override
    public void build(DecorationContext context, int groundY) {
        int trunkHeight = context.randomInt(41, 7, 11);
        int maxRadius = context.randomInt(42, 2, 3);
        int skirtDepth = trunkHeight - context.randomInt(43, 2, 3);
        Block wood = context.blocks().wood;
        Block needles = context.blocks().pineLeaves;

        // Than dung lai ba khoi duoi chop: neu de no vuon toi tan ngon thi cac vong
        // ban kinh 0 o gan dinh roi trung vao than, thanh ra co khuc go tro ngay duoi
        // chop. De than thap hon thi ba hang tren cung deu la la dac.
        int crown = groundY + trunkHeight;
        context.place(0, groundY, 0, context.blocks().dirt);
        for (int step = 1; step <= trunkHeight - 3; step++) {
            context.place(0, groundY + step, 0, wood);
        }

        int radius = 0;
        int layerLimit = 1;
        int radiusAfterReset = 0;

        for (int step = 0; step <= skirtDepth; step++) {
            ring(context, crown - step, radius, needles);

            if (radius < layerLimit) {
                radius++;
                continue;
            }
            radius = radiusAfterReset;
            radiusAfterReset = 1;
            if (++layerLimit > maxRadius) {
                layerLimit = maxRadius;
            }
        }
    }

    private void ring(DecorationContext context, int y, int radius, Block needles) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (radius > 0 && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }
                context.placeIfEmpty(dx, y, dz, needles);
            }
        }
    }
}
