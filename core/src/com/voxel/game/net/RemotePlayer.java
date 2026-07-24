package com.voxel.game.net;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.render.WalkCycle;
import com.voxel.game.combat.Attackable;

/**
 * Mot nguoi choi KHAC dang o trong the gioi chung.
 *
 * Server chi gui vi tri thua thot (~15 lan/giay), neu ve dung theo do thi nhan vat giat cuc.
 * Nen ta luu vi tri "dich" moi nhat tu server, roi moi khung hinh keo vi tri hien tai truot
 * dan toi dich - nhin muot nhu dang di lien mach.
 *
 * <p>Server khong gui "dang di hay dung yen", nen nhip buoc duoc SUY RA tu quang duong nguoi
 * do vua truot duoc: co di thi tay chan vung, dung lai thi duoi thang - giong Minecraft.
 *
 * <p>Truong dich duoc ghi tu luong WebSocket, truong hien tai chi doc/ghi tren luong game -
 * dung volatile cho phan dich de hai luong thay du lieu moi cua nhau.
 */
public final class RemotePlayer implements Attackable {

    /** Toc do keo vi tri hien tai ve dich (cang lon cang bam sat, cang nho cang muot). */
    private static final float FOLLOW = 10f;

    private final String name;
    private final HitSender sender;
    private final WalkCycle walk = new WalkCycle();

    // Vi tri + huong dich moi nhat tu server (ghi tu luong WebSocket).
    private volatile float targetX, targetY, targetZ, targetYaw;

    // Vi tri + huong dang ve (chi cham tren luong game).
    private final Vector3 feet = new Vector3();
    private float yaw;
    private boolean placed;

    RemotePlayer(String name, HitSender sender) {
        this.name = name;
        this.sender = sender;
    }

    void setTarget(float x, float y, float z, float yaw) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.targetYaw = yaw;
    }

    /** Keo vi tri hien tai truot mot buoc ve dich. Goi moi khung hinh tren luong game. */
    void advance(float delta) {
        if (!placed) {
            // Lan dau thay: nhay thang toi noi, khong truot tu goc toa do.
            feet.set(targetX, targetY, targetZ);
            yaw = targetYaw;
            placed = true;
            return;
        }
        float fromX = feet.x;
        float fromZ = feet.z;

        float t = Math.min(1f, delta * FOLLOW);
        feet.x += (targetX - feet.x) * t;
        feet.y += (targetY - feet.y) * t;
        feet.z += (targetZ - feet.z) * t;
        yaw += shortestAngle(yaw, targetYaw) * t;

        float dx = feet.x - fromX;
        float dz = feet.z - fromZ;
        walk.update(delta, (float) Math.sqrt(dx * dx + dz * dz));
    }

    /** Nguoi nay vua pha khoi / danh ai do - quo tay phai ra mot cai cho thay. */
    void swingArm() {
        walk.swingArm();
    }

    // ------------------------------------------------------------- chiu don

    @Override
    public Vector3 feet() {
        return feet;
    }

    /**
     * Minh danh trung ho: mau cua ho nam tren MAY CUA HO, nen chi bao len server, server
     * chuyen tiep cho dung nguoi do tru mau.
     */
    @Override
    public void takeHit(int damage) {
        sender.sendHit(name, damage);
    }

    @Override
    public String displayName() {
        return name;
    }

    public String name() {
        return name;
    }

    public float yaw() {
        return yaw;
    }

    /** Nhip di de renderer vung tay chan cho dung. */
    public WalkCycle walk() {
        return walk;
    }

    /** Chenh lech goc ngan nhat tu {@code from} toi {@code to}, trong khoang (-180, 180] do. */
    private static float shortestAngle(float from, float to) {
        float diff = (to - from) % 360f;
        if (diff > 180f) {
            diff -= 360f;
        } else if (diff < -180f) {
            diff += 360f;
        }
        return diff;
    }
}
