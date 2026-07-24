package com.voxel.game.mob;

/**
 * Mot trang thai trong bo nao quai vat theo mau thiet ke <b>State</b>: Idle -> Chase -> Attack.
 * Moi trang thai tu quyet dinh khung hinh sau se o trang thai nao (tra ve chinh no de o nguyen).
 * Cach nay giong het {@code MovementState} cua nguoi choi trong engine.
 */
public interface MonsterState {

    /**
     * Chay mot khung hinh cua trang thai nay.
     *
     * @return trang thai cho khung sau (co the la chinh no)
     */
    MonsterState update(Monster monster, MonsterContext ctx, float delta);
}
