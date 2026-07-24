package com.voxel.game.combat;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;

/**
 * Ngam danh gan (danh tay khong nhu Minecraft).
 *
 * Ban mot tia thang tu mat nguoi choi ra huong dang nhin, coi moi sinh vat la mot hop chu
 * nhat rong 0.6 cao 1.85 dung tu ban chan, roi giu lai muc tieu CHAM TIA GAN NHAT. Nho vay
 * neu co ca quai vat lan nguoi choi khac chong len nhau thi chi con dung truoc an don.
 *
 * <p>Cach dung: {@link #aimFrom} mot lan, dua tung ung vien qua {@link #consider},
 * roi doc ket qua o {@link #target}.
 */
public final class MeleeAim {

    /** Nua chieu rong hop va cham cua mot nhan vat (khoi). */
    private static final float HALF_WIDTH = 0.35f;
    /** Chieu cao hop va cham - cao hon nhan vat mot chut cho de trung. */
    private static final float HEIGHT = 1.85f;

    private final Ray ray = new Ray();
    private final BoundingBox box = new BoundingBox();
    private final Vector3 point = new Vector3();
    private final Vector3 min = new Vector3();
    private final Vector3 max = new Vector3();

    private Attackable best;
    private float bestDistance;

    /**
     * Bat dau mot lan ngam.
     *
     * @param origin    vi tri mat nguoi choi
     * @param direction huong dang nhin
     * @param reach     tam voi cua canh tay (khoi) - xa hon thi khong voi toi
     */
    public void aimFrom(Vector3 origin, Vector3 direction, float reach) {
        ray.origin.set(origin);
        ray.direction.set(direction).nor();
        bestDistance = reach;
        best = null;
    }

    /** Xet mot ung vien: neu tia cham vao no va gan hon muc tieu dang giu thi thay the. */
    public void consider(Attackable candidate) {
        Vector3 feet = candidate.feet();
        min.set(feet.x - HALF_WIDTH, feet.y, feet.z - HALF_WIDTH);
        max.set(feet.x + HALF_WIDTH, feet.y + HEIGHT, feet.z + HALF_WIDTH);
        box.set(min, max);

        if (!Intersector.intersectRayBounds(ray, box, point)) {
            return;
        }
        float distance = point.dst(ray.origin);
        if (distance < bestDistance) {
            bestDistance = distance;
            best = candidate;
        }
    }

    /** Muc tieu gan nhat dinh tia ngam, hoac null neu danh vao khong khi. */
    public Attackable target() {
        return best;
    }
}
