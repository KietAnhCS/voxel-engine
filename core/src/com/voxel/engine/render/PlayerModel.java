package com.voxel.engine.render;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/**
 * Nhan vat cua CHINH nguoi choi, nhin thay o goc nhin thu ba (phim F5).
 *
 * Hinh nguoi (dau, than, hai tay, hai chan) do {@link HumanoidFigure} lo, con nhip buoc
 * do {@link WalkCycle} lo. Lop nay chi noi hai thu do lai voi nhau.
 */
public final class PlayerModel implements Disposable {

    private final HumanoidFigure figure = new HumanoidFigure("data/skinzom.png", 0.85f);
    private final WalkCycle walk = new WalkCycle();

    /**
     * Dat nhan vat vao the gioi.
     *
     * @param feet     vi tri ban chan
     * @param yaw      huong nhan vat quay mat ve (do)
     * @param distance quang duong vua di trong khung hinh nay - di nhanh thi vung tay chan nhanh
     */
    public void update(Vector3 feet, float yaw, float distance, float delta) {
        walk.update(delta, distance);
        figure.pose(feet, yaw, walk);
    }

    /** Quo tay phai ra mot cai - goi khi nguoi choi danh, pha hoac dat khoi. */
    public void swingArm() {
        walk.swingArm();
    }

    public void render(PerspectiveCamera camera) {
        figure.begin(camera);
        figure.draw();
        figure.end();
    }

    @Override
    public void dispose() {
        figure.dispose();
    }
}
