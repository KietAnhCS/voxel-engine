package com.voxel.game.terrain.decor;

/**
 * Strategy: mot cach trang tri be mat (co, hoa, cay, tang tuyet, tang da...).
 * Biome chi can ghep vai decorator lai la co "phong canh" rieng.
 */
public interface Decorator {

    /**
     * @return true neu da dat vat the va cac decorator sau khong nen dung o cot nay
     */
    boolean decorate(DecorationContext context);
}
