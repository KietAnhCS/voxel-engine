package com.voxel.game.mob;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.world.World;

/**
 * Mot con quai vat mang skin y het nhan vat. Giu vi tri, huong nhin va trang thai AI hien tai.
 *
 * <p>Bo nao chay theo mau <b>State</b>: moi khung hinh giao cho {@link MonsterState} hien tai
 * xu ly, no tra ve trang thai ke tiep (Idle -> Chase -> Attack). Ban than {@code Monster} chi
 * lo phan "than the": di chuyen, bam mat dat, quay mat va hoat hoa tay chan.
 */
public final class Monster {

    private static final float WALK_ANIM_SPEED = 8f;
    private static final float ARM_SWING_DECAY = 4.5f;

    private final Vector3 position = new Vector3();
    private float yaw;

    // Hoat hoa: chan vung khi di, tay phai vung khi danh (dung chung PlayerMesh voi nguoi choi).
    private float walkPhase;
    private float attackSwing;
    private boolean moving;

    private MonsterState state = new IdleState();

    public Monster(float x, float y, float z) {
        position.set(x, y, z);
    }

    /** Chay mot khung hinh: giao cho trang thai AI, roi cap nhat hoat hoa tay chan. */
    public void update(MonsterContext ctx, float delta) {
        moving = false;
        MonsterState next = state.update(this, ctx, delta);
        if (next != state) {
            state = next;
        }

        if (moving) {
            walkPhase += delta * WALK_ANIM_SPEED;
        } else {
            walkPhase *= Math.max(0f, 1f - delta * WALK_ANIM_SPEED);
        }
        attackSwing = Math.max(0f, attackSwing - delta * ARM_SWING_DECAY);
    }

    /** Di ngang toi (wx,wz) voi toc do cho, bam mat dat va quay mat theo huong di. */
    public void stepToward(World world, float wx, float wz, float speed, float delta) {
        float dx = wx - position.x;
        float dz = wz - position.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist > 1e-4f) {
            float step = Math.min(dist, speed * delta);
            position.x += dx / dist * step;
            position.z += dz / dist * step;
            yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
            moving = true;
        }
        snapToGround(world);
    }

    /** Keo ban chan len dinh khoi ran o cot hien tai - leo len xuong dia hinh mem mai. */
    private void snapToGround(World world) {
        int x = (int) Math.floor(position.x);
        int z = (int) Math.floor(position.z);
        int top = Math.min(world.config().worldHeight() - 1, (int) Math.floor(position.y) + 1);
        for (int y = top; y > 0; y--) {
            if (world.blockAt(x, y - 1, z).isCollidable() && !world.blockAt(x, y, z).isCollidable()) {
                position.y = y;
                return;
            }
        }
    }

    /** Quay mat ve huong (wx,wz) ma khong di chuyen - dung khi dung yen danh nguoi choi. */
    public void faceToward(float wx, float wz) {
        float dx = wx - position.x;
        float dz = wz - position.z;
        if (dx * dx + dz * dz > 1e-6f) {
            yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        }
    }

    /** Vung tay phai ra mot cai (goi khi tung cu danh). */
    public void swingArm() {
        attackSwing = 1f;
    }

    public Vector3 position() {
        return position;
    }

    public float yaw() {
        return yaw;
    }

    public float walkPhase() {
        return walkPhase;
    }

    public float attackSwing() {
        return attackSwing;
    }
}
