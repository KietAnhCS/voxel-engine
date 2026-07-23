package com.voxel.game.net;

import java.util.List;

/**
 * Toan bo the gioi mot nguoi choi, do server gui ve khi dang nhap: hat giong dia hinh,
 * trang thai nguoi choi va danh sach o khoi da sua.
 */
public final class WorldSnapshot {

    public final long seed;
    public final PlayerState player;
    public final List<Edit> edits;

    public WorldSnapshot(long seed, PlayerState player, List<Edit> edits) {
        this.seed = seed;
        this.player = player;
        this.edits = edits;
    }
}
