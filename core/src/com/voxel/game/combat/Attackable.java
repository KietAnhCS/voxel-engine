package com.voxel.game.combat;

import com.badlogic.gdx.math.Vector3;

/**
 * Thu co the bi nguoi choi danh: quai vat hoac mot nguoi choi khac.
 *
 * Nho co chung mot giao dien nay ma {@link MeleeAim} khong can biet minh dang ngam vao ai -
 * no chi hoi vi tri ban chan roi bao "trung", con chuyen dau mau la viec cua tung loai.
 */
public interface Attackable {

    /** Vi tri BAN CHAN - goc duoi cua hop va cham. */
    Vector3 feet();

    /** An mot cu danh {@code damage} mau. */
    void takeHit(int damage);

    /** Ten hien len khung chat khi bi danh trung. */
    String displayName();
}
