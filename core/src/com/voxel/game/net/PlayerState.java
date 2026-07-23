package com.voxel.game.net;

/**
 * Trang thai nguoi choi luu tren server: vi tri chan, huong nhin, che do choi.
 * yaw/pitch tinh bang do; mode: 0 = sinh ton, 1 = sang tao.
 */
public final class PlayerState {

    public final float x;
    public final float y;
    public final float z;
    public final float yaw;
    public final float pitch;
    public final int mode;

    public PlayerState(float x, float y, float z, float yaw, float pitch, int mode) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.mode = mode;
    }

    /** The gioi moi tinh: server tra ve y = 0 nghia la "chua tung choi", game tu tim mat dat. */
    public boolean isFresh() {
        return y <= 0f;
    }
}
