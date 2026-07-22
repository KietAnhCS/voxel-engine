package com.voxel.game.terrain.decor;

import com.voxel.engine.block.Block;

/**
 * Cay soi kieu Minecraft co dien: than thang khong nhanh, tan la hinh "keo mut"
 * gom bon tang - hai tang duoi rong 5x5 om lay ngon than, hai tang tren 3x3, bon
 * goc cua moi tang bi khoet bot cho tan tron canh.
 *
 * Ban truoc dung DE QUY de dam nhanh nhu cay soi khong lo. Cay soi thuong cua
 * Minecraft khong he co nhanh, va tan la mong kieu do lam nhin xuyen thau vao
 * giua cay - dung cai nguoi ta hay che.
 */
public final class OakShape implements TreeShape {

    private static final int MIN_TRUNK = 4;
    private static final int MAX_TRUNK = 6;

    @Override
    public void build(DecorationContext context, int groundY) {
        int trunkHeight = context.randomInt(11, MIN_TRUNK, MAX_TRUNK);
        Block wood = context.blocks().wood;
        Block leaves = context.blocks().leaves;

        context.place(0, groundY, 0, context.blocks().dirt);
        for (int step = 1; step <= trunkHeight; step++) {
            context.place(0, groundY + step, 0, wood);
        }

        int crown = groundY + trunkHeight;
        for (int dy = -2; dy <= 1; dy++) {
            int radius = dy <= -1 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (isTrimmedCorner(context, dx, dz, radius, dy)) {
                        continue;
                    }
                    context.placeIfEmpty(dx, crown + dy, dz, leaves);
                }
            }
        }
    }

    /**
     * Goc cua tang chop luon bi bo, goc cac tang duoi bo mot nua theo may rui -
     * nho the moi cay mot khac chu khong ra bon hinh hop giong het nhau.
     */
    private boolean isTrimmedCorner(DecorationContext context, int dx, int dz, int radius, int dy) {
        if (Math.abs(dx) != radius || Math.abs(dz) != radius) {
            return false;
        }
        return dy == 1 || context.random(20 + dy * 7 + dx * 3 + dz) < 0.5f;
    }
}
