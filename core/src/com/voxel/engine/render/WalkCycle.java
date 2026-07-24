package com.voxel.engine.render;

/**
 * Nhip di bo cua mot nhan vat: hai tay hai chan dua len dua xuong.
 *
 * Nhip chay theo QUANG DUONG di duoc chu khong theo thoi gian, nen di nhanh thi vung nhanh,
 * di cham thi vung cham, dung lai thi tay chan tu duoi thang ra - giong het Minecraft.
 *
 * Ca ba loai nhan vat (nguoi choi, quai vat, nguoi choi khac) deu dung chung lop nay nen
 * cu dong y het nhau.
 */
public final class WalkCycle {

    /** Goc vung toi da cua tay/chan (do) - chan truoc chan sau xoac nhau 120 do luc chay. */
    private static final float SWING_ANGLE = 60f;
    /** Bien do tay phai quo ra khi danh / pha khoi (do). */
    private static final float PUNCH_ANGLE = 70f;

    /** Moi khoi di duoc thi nhip tien bay nhieu radian (mot buoc chan ~ 1.6 khoi). */
    private static final float STRIDE = 2f;
    /** Toc do di bo binh thuong (khoi/giay) - toi day la vung het bien do. */
    private static final float FULL_SPEED = 4.3f;
    /** Bien do dau len / tat di trong khoang 1/8 giay cho muot. */
    private static final float BLEND = 8f;
    /** Cu quo tay tat dan trong ~0.22 giay (1 / 4.5). */
    private static final float ARM_DECAY = 4.5f;

    private float phase;
    private float amount;
    private float armSwing;
    /** Dong ho rieng cho cu dong "tho": tay kha khe dua ngay ca khi dung yen. */
    private float time;

    /**
     * Chay mot khung hinh.
     *
     * @param distance quang duong NGANG nhan vat vua di trong khung hinh nay (khoi)
     */
    public void update(float delta, float distance) {
        if (delta > 0f) {
            float speed = distance / delta;
            float target = Math.min(1f, speed / FULL_SPEED);
            amount += (target - amount) * Math.min(1f, delta * BLEND);
        }
        phase += distance * STRIDE;
        armSwing = Math.max(0f, armSwing - delta * ARM_DECAY);
        time += delta;
    }

    /** Quo tay phai ra mot cai - goi khi danh, pha hoac dat khoi. */
    public void swingArm() {
        armSwing = 1f;
    }

    /** Goc vung cua chan TRAI (do). Chan phai va hai tay lay nguoc dau lai. */
    public float legAngle() {
        return (float) Math.sin(phase) * SWING_ANGLE * amount;
    }

    /** Goc tay phai vong ra truoc roi ve cho khi danh (do). */
    public float punchAngle() {
        return (float) Math.sin(armSwing * Math.PI) * PUNCH_ANGLE;
    }

    /**
     * Cu dua tay rat nhe theo thoi gian (nhu tho) - lam nhan vat dung yen van "song".
     * Hai tan so lech nhau chut xiu nen cu dong khong bao gio lap lai y het (hoc Minecraft).
     */
    public float idleSway() {
        return (float) (Math.sin(time * 1.13) * 2.2 + Math.sin(time * 1.91) * 0.8);
    }
}
