package com.voxel.game.terrain.decor;

/**
 * Strategy: hinh dang cua mot cai cay. Doi shape la doi loai cay,
 * khong phai sua lai logic "khi nao thi moc cay".
 */
public interface TreeShape {

    void build(DecorationContext context, int groundY);
}
