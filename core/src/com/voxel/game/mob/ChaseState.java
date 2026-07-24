package com.voxel.game.mob;

import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * Trang thai duoi theo: quai da thay nguoi choi va chay lai gan.
 *
 * <p>Day la cho dung <b>A*</b>: cu moi {@link #REPATH_INTERVAL} giay lai nho
 * {@link AStarPathFinder} tinh lai duong (vi nguoi choi luon di chuyen), roi bam theo tung
 * diem giua cua duong do. Vao du gan thi chuyen sang danh, ra qua xa thi bo cuoc.
 */
public final class ChaseState implements MonsterState {

    /** Tinh lai duong moi nua giay - du bam sat nguoi choi ma khong ton CPU moi khung. */
    private static final float REPATH_INTERVAL = 0.5f;
    /** Coi nhu da toi diem giua khi cach no duoi nguong nay. */
    private static final float WAYPOINT_REACHED = 0.35f;

    private final List<Vector3> path = new ArrayList<Vector3>();
    private int index;
    private float repathTimer;

    @Override
    public MonsterState update(Monster monster, MonsterContext ctx, float delta) {
        Vector3 target = ctx.player().position();
        float distance = MonsterContext.horizontalDist(monster.position(), target);

        if (ctx.player().isDead() || distance > MonsterContext.LOSE_RANGE) {
            return new IdleState();
        }
        if (distance <= MonsterContext.ATTACK_RANGE) {
            return new AttackState();
        }

        // Dinh ky tinh lai duong bang A* vi nguoi choi luon chay.
        repathTimer -= delta;
        if (repathTimer <= 0f || index >= path.size()) {
            path.clear();
            path.addAll(ctx.pathFinder().findPath(monster.position(), target));
            index = 0;
            repathTimer = REPATH_INTERVAL;
        }

        if (index < path.size()) {
            Vector3 waypoint = path.get(index);
            monster.stepToward(ctx.world(), waypoint.x, waypoint.z, MonsterContext.CHASE_SPEED, delta);
            if (MonsterContext.horizontalDist(monster.position(), waypoint) < WAYPOINT_REACHED) {
                index++;
            }
        } else {
            // Khong tim ra duong (bi ket): cu lao thang toi nguoi choi.
            monster.stepToward(ctx.world(), target.x, target.z, MonsterContext.CHASE_SPEED, delta);
        }
        return this;
    }
}
