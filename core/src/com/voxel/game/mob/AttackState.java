package com.voxel.game.mob;

import com.badlogic.gdx.math.Vector3;

/**
 * Trang thai tan cong: quai da ap sat nguoi choi. Dung tai cho, quay mat vao nguoi choi va
 * cu moi {@link MonsterContext#ATTACK_INTERVAL} giay lai quo tay danh mot cai. Neu nguoi choi
 * chay ra xa thi quay lai duoi theo.
 */
public final class AttackState implements MonsterState {

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
        if (cooldown <= 0f) {
            cooldown = MonsterContext.ATTACK_INTERVAL;
            monster.swingArm();
            ctx.player().hit(MonsterContext.ATTACK_DAMAGE);
        }
        return this;
    }
}
