package com.voxel.game.mob;

import com.badlogic.gdx.math.Vector3;

/**
 * Trang thai tan cong: quai da ap sat nguoi choi. Dung tai cho, quay mat vao nguoi choi va
 * cu moi {@link MonsterContext#ATTACK_INTERVAL} giay lai quo tay danh mot cai. Neu nguoi choi
 * chay ra xa thi quay lai duoi theo.
 *
 * <p>Gan thoi chua du: canh tay quai chi voi cao co han. Nguoi choi ke gach leo len vai o thi
 * quai van dung duoi ngong co ma khong danh toi - phai tut xuong moi an don.
 */
public final class AttackState implements MonsterState {

    /** Voi khong toi thi cho chut nua thu lai, dung de danh mien phi ngay khi con moi tut xuong. */
    private static final float RETRY_DELAY = 0.25f;

    private float cooldown;

    @Override
    public MonsterState update(Monster monster, MonsterContext ctx, float delta) {
        Vector3 target = ctx.player().position();
        float distance = MonsterContext.horizontalDist(monster.position(), target);

        if (ctx.player().isDead() || distance > MonsterContext.ATTACK_RANGE + 0.5f) {
            return new ChaseState();
        }

        monster.faceToward(target.x, target.z);
        cooldown -= delta;
        if (cooldown > 0f) {
            return this;
        }
        if (!MonsterContext.withinReachHeight(monster.position(), target)) {
            cooldown = RETRY_DELAY;
            return this;
        }

        cooldown = MonsterContext.ATTACK_INTERVAL;
        monster.swingArm();
        ctx.player().hit(MonsterContext.ATTACK_DAMAGE);
        return this;
    }
}
