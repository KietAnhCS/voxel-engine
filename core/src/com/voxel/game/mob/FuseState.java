package com.voxel.game.mob;

import com.badlogic.gdx.math.Vector3;

/**
 * Creeper fuse state: the creeper has reached the player, stops dead, hisses and swells
 * for {@link #FUSE_TIME} seconds, then explodes like TNT. Exactly like Minecraft:
 * if the player sprints out of range in time, the creeper calms down and resumes the chase.
 *
 * <p>The state only burns the fuse; the actual explosion (breaking blocks, hurting the
 * player, telling the server) is done by {@link MonsterManager} on the game loop, because
 * a state object should not need a reference to the network.
 */
public final class FuseState implements MonsterState {

    /** Seconds from first hiss to boom - same as a Minecraft creeper. */
    private static final float FUSE_TIME = 1.5f;
    /** The player must get this far away (blocks) for the creeper to give up. */
    private static final float DEFUSE_RANGE = 4.5f;

    private float timer;

    @Override
    public MonsterState update(Monster monster, MonsterContext ctx, float delta) {
        Vector3 target = ctx.player().position();
        float distance = MonsterContext.horizontalDist(monster.position(), target);

        if (ctx.player().isDead() || distance > DEFUSE_RANGE) {
            monster.setFuse(0f);
            return new ChaseState();
        }

        monster.faceToward(target.x, target.z);
        timer += delta;
        monster.setFuse(timer / FUSE_TIME);
        if (timer >= FUSE_TIME) {
            monster.requestExplosion();
        }
        return this;
    }
}
