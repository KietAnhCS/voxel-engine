package com.voxel.game.mob;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.Disposable;
import com.voxel.engine.render.HumanoidFigure;

import java.util.List;

/**
 * Ve tat ca quai vat. Dung DUNG MOT hinh nguoi ({@link HumanoidFigure}, boc skin
 * {@code skinzom.png} y het nhan vat) roi dat lai vi tri va vung tay chan cho tung con,
 * giong cach {@code RemotePlayerRenderer} ve nguoi choi khac - re va gon.
 *
 * <p>Khac nguoi choi: anh sang moi truong toi hon mot chut de quai trong "am u" hon, va
 * con nao vua an don thi ve bang anh sang DO trong chua day nua giay.
 */
public final class MonsterRenderer implements Disposable {

    private final HumanoidFigure figure = new HumanoidFigure("data/skinzom.png", 0.72f);
    /** Hinh thu hai, anh sang do rue - dung ve con vua an don cho de thay. */
    private final HumanoidFigure hurtFigure = new HumanoidFigure("data/skinzom.png", 1.6f, 0.3f, 0.3f);

    /** Ve moi con quai: dat lai hinh dung, vung chan theo buoc di va vung tay phai khi danh. */
    public void render(PerspectiveCamera camera, List<Monster> monsters) {
        if (monsters.isEmpty()) {
            return;
        }
        draw(camera, monsters, false);
        draw(camera, monsters, true);
    }

    /** Goc gio tay ra truoc cua zombie (do) - tu the kinh dien. Creeper thi tha tay doc. */
    private static final float ZOMBIE_ARMS = 85f;

    /**
     * Ve nhung con dang (hoac khong dang) nhap nhay do. Hai lan ve de doi mau anh sang:
     * vua an don HOAC creeper dang chay ngoi thi do ruc len.
     */
    private void draw(PerspectiveCamera camera, List<Monster> monsters, boolean hurt) {
        HumanoidFigure target = hurt ? hurtFigure : figure;
        boolean began = false;
        for (Monster monster : monsters) {
            if (monster.isFlashing() != hurt) {
                continue;
            }
            if (!began) {
                target.begin(camera);
                began = true;
            }
            float arms = monster.kind() == Monster.Kind.ZOMBIE ? ZOMBIE_ARMS : 0f;
            target.pose(monster.position(), monster.yaw(), monster.walk(), arms);
            target.draw();
        }
        if (began) {
            target.end();
        }
    }

    @Override
    public void dispose() {
        figure.dispose();
        hurtFigure.dispose();
    }
}
