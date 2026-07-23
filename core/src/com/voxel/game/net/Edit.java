package com.voxel.game.net;

/**
 * Mot o khoi nguoi choi da dat/pha, lech so voi dia hinh goc. blockId = 0 la o da bi pha
 * thanh khong khi. Toa do la toa do the gioi (khong phai toa do trong chunk).
 */
public final class Edit {

    public final int x;
    public final int y;
    public final int z;
    public final int blockId;

    public Edit(int x, int y, int z, int blockId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockId = blockId;
    }
}
