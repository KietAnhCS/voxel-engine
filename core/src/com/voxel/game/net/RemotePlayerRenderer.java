package com.voxel.game.net;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.Disposable;
import com.voxel.engine.render.HumanoidFigure;

/**
 * Ve avatar cho nhung nguoi choi KHAC trong the gioi chung.
 *
 * Dung DUNG MOT hinh nguoi ({@link HumanoidFigure}) roi dat lai vi tri va tu the cho tung
 * nguoi - re, don gian, du cho vai nguoi choi cung luc. Tay chan cua ho vung theo nhip di
 * ma {@link RemotePlayer} tu suy ra tu quang duong ho di duoc, nen nhin ho buoc di y het
 * nhan vat cua minh chu khong phai mot cuc troi tren mat dat.
 */
public final class RemotePlayerRenderer implements Disposable {

    private final HumanoidFigure figure = new HumanoidFigure("data/skinzom.png", 0.85f);

    /** Ve tat ca nguoi choi khac bang cach dat lai cung mot hinh roi ve lai cho tung nguoi. */
    public void render(PerspectiveCamera camera, RemotePlayers players) {
        if (players.all().isEmpty()) {
            return;
        }
        figure.begin(camera);
        for (RemotePlayer player : players.all()) {
            figure.pose(player.feet(), player.yaw(), player.walk());
            figure.draw();
        }
        figure.end();
    }

    @Override
    public void dispose() {
        figure.dispose();
    }
}
