package com.voxel.game.mob;

/**
 * Trang thai dung yen: quai chua thay nguoi choi. Chi ngoi im, dao mat quanh vung,
 * cho toi khi nguoi choi lot vao tam phat hien thi chuyen sang duoi theo.
 */
public final class IdleState implements MonsterState {

    @Override
    public MonsterState update(Monster monster, MonsterContext ctx, float delta) {
        if (!ctx.player().isDead()
                && MonsterContext.horizontalDist(monster.position(), ctx.player().position())
                <= MonsterContext.DETECT_RANGE) {
            return new ChaseState();
        }
        return this;
    }
}
