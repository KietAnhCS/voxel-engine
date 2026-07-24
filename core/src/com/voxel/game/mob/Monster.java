package com.voxel.game.mob;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.render.WalkCycle;
import com.voxel.engine.world.World;
import com.voxel.game.combat.Attackable;

/**
 * Mot con quai vat mang skin y het nhan vat. Giu vi tri, huong nhin va trang thai AI hien tai.
 *
 * <p>Bo nao chay theo mau <b>State</b>: moi khung hinh giao cho {@link MonsterState} hien tai
 * xu ly, no tra ve trang thai ke tiep (Idle -> Chase -> Attack). Ban than {@code Monster} chi
 * lo phan "than the": di chuyen, bam mat dat, quay mat, hoat hoa tay chan va chiu don.
 */
public final class Monster implements Attackable {

    /** The two mob kinds. Creepers reuse the zombie skin for now, telling them apart by pose. */
    public enum Kind { ZOMBIE, CREEPER }

    /** Mau toi da - bang zombie Minecraft (20 mau = 10 trai tim). */
    public static final int MAX_HEALTH = 20;
    /** Vet do nhap nhay bao nhieu giay sau moi cu an don. */
    private static final float HURT_FLASH = 0.35f;
    /** Moi giay dung ngoai nang thi mat bay nhieu mau. */
    private static final int BURN_DAMAGE = 4;

    private final Kind kind;
    private final Vector3 position = new Vector3();
    /** Nhip di: chan tay vung theo quang duong di duoc (dung chung voi nguoi choi). */
    private final WalkCycle walk = new WalkCycle();

    private float yaw;
    private int health = MAX_HEALTH;
    private float hurtTimer;
    private float burnTimer;
    /** Creeper only: 0..1 fuse progress while hissing; the renderer flashes with this. */
    private float fuse;
    /** Creeper only: the fuse burnt down - MonsterManager turns this into an explosion. */
    private boolean exploding;

    private MonsterState state = new IdleState();

    public Monster(Kind kind, float x, float y, float z) {
        this.kind = kind;
        position.set(x, y, z);
    }

    /** Chay mot khung hinh: giao cho trang thai AI, roi cap nhat hoat hoa tay chan. */
    public void update(MonsterContext ctx, float delta) {
        float fromX = position.x;
        float fromZ = position.z;

        MonsterState next = state.update(this, ctx, delta);
        if (next != state) {
            state = next;
        }

        // Di duoc bao nhieu thi vung tay chan bay nhieu - dung yen la tay chan duoi thang.
        float dx = position.x - fromX;
        float dz = position.z - fromZ;
        walk.update(delta, (float) Math.sqrt(dx * dx + dz * dz));
        hurtTimer = Math.max(0f, hurtTimer - delta);
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
        walk.swingArm();
    }

    /**
     * Dung ngoai nang giua ban ngay: chay dan. Cu moi giay mat {@link #BURN_DAMAGE} mau nen
     * sau khoang nam giay la boc hoi - dung nhu zombie Minecraft gap binh minh.
     */
    public void burn(float delta) {
        burnTimer += delta;
        while (burnTimer >= 1f) {
            burnTimer -= 1f;
            takeHit(BURN_DAMAGE);
        }
    }

    // ------------------------------------------------------------- chiu don

    @Override
    public Vector3 feet() {
        return position;
    }

    @Override
    public void takeHit(int damage) {
        if (damage <= 0 || isDead()) {
            return;
        }
        health = Math.max(0, health - damage);
        hurtTimer = HURT_FLASH;
    }

    @Override
    public String displayName() {
        return kind == Kind.CREEPER ? "Creeper" : "Zombie";
    }

    public Kind kind() {
        return kind;
    }

    // ------------------------------------------------------------- creeper fuse

    /** Creeper only: how far the fuse has burnt (0 = calm, 1 = boom). */
    public float fuse() {
        return fuse;
    }

    public void setFuse(float fuse) {
        this.fuse = Math.max(0f, Math.min(1f, fuse));
    }

    /** Called by the fuse state when the countdown finishes. */
    public void requestExplosion() {
        exploding = true;
    }

    /** True when the manager should replace this creeper with an explosion. */
    public boolean isExploding() {
        return exploding;
    }

    public boolean isDead() {
        return health <= 0;
    }

    /** Dang nhap nhay do vi vua an don. */
    public boolean isHurt() {
        return hurtTimer > 0f;
    }

    /** Ve bang mau do luc nay khong: vua an don, hoac creeper dang xi ngoi (nhap nhay). */
    public boolean isFlashing() {
        return isHurt() || (fuse > 0f && ((int) (fuse * 8f) & 1) == 1);
    }

    public int health() {
        return health;
    }

    public Vector3 position() {
        return position;
    }

    public float yaw() {
        return yaw;
    }

    public WalkCycle walk() {
        return walk;
    }
}
