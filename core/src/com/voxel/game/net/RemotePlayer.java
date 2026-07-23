package com.voxel.game.net;

/**
 * Mot nguoi choi KHAC dang o trong the gioi chung.
 *
 * Server chi gui vi tri thua thot (~15 lan/giay), neu ve dung theo do thi nhan vat giat cuc.
 * Nen ta luu vi tri "dich" moi nhat tu server, roi moi khung hinh keo vi tri hien tai truot
 * dan toi dich - nhin muot nhu dang di lien mach.
 *
 * Truong dich duoc ghi tu luong WebSocket, truong hien tai chi doc/ghi tren luong game -
 * dung volatile cho phan dich de hai luong thay du lieu moi cua nhau.
 */
public final class RemotePlayer {

    /** Toc do keo vi tri hien tai ve dich (cang lon cang bam sat, cang nho cang muot). */
    private static final float FOLLOW = 10f;

    // Vi tri + huong dich moi nhat tu server (ghi tu luong WebSocket).
    private volatile float targetX, targetY, targetZ, targetYaw;

    // Vi tri + huong dang ve (chi cham tren luong game).
    private float x, y, z, yaw;
    private boolean placed;

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
            x = targetX;
            y = targetY;
            z = targetZ;
            yaw = targetYaw;
            placed = true;
            return;
        }
        float t = Math.min(1f, delta * FOLLOW);
        x += (targetX - x) * t;
        y += (targetY - y) * t;
        z += (targetZ - z) * t;
        yaw += shortestAngle(yaw, targetYaw) * t;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float z() {
        return z;
    }

    public float yaw() {
        return yaw;
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
