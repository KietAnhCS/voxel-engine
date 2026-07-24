package com.voxel.game.mob;

import com.badlogic.gdx.math.Vector3;

/**
 * Nguoi choi nhin tu goc do cua quai vat: chi can biet dang o dau, co the danh trung,
 * va da chet hay chua. Nho lop trung gian nay ma he quai vat khong dinh gi toi mau me
 * hay giao dien - de tach bach va de kiem thu.
 */
public interface PlayerTarget {

    /** Vi tri ban chan nguoi choi trong the gioi. */
    Vector3 position();

    /** Gay {@code damage} mau len nguoi choi. */
    void hit(int damage);

    /** True neu nguoi choi da chet (quai khong danh nua). */
    boolean isDead();
}
