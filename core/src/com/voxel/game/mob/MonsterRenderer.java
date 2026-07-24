package com.voxel.game.mob;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.voxel.engine.render.PlayerMesh;

import java.util.List;

/**
 * Ve tat ca quai vat. Dung DUNG MOT hinh nguoi ({@link PlayerMesh}, boc skin {@code skinzom.png}
 * y het nhan vat) roi dat lai vi tri va vung tay chan cho tung con, giong cach
 * {@code RemotePlayerRenderer} ve nguoi choi khac - re va gon.
 *
 * <p>Khac nguoi choi: anh sang moi truong toi hon mot chut de quai trong "am u" hon.
 */
public final class MonsterRenderer implements Disposable {

    private static final float HEIGHT = 1.75f;
    private static final float U = HEIGHT / 32f;
    private static final float SWING_ANGLE = 42f;
    private static final float PUNCH_ANGLE = 70f;

    private final Texture skin;
    private final Model model;
    private final ModelInstance instance;
    private final ModelBatch batch = new ModelBatch();
    private final Environment environment = new Environment();
    private final Vector3 feet = new Vector3();

    public MonsterRenderer() {
        skin = new Texture(Gdx.files.internal("data/skinzom.png"));
        skin.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        model = PlayerMesh.build(skin, U);
        instance = new ModelInstance(model);
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.72f, 0.68f, 0.68f, 1f));
    }

    /** Ve moi con quai: dat lai hinh dung, vung chan theo buoc di va vung tay phai khi danh. */
    public void render(PerspectiveCamera camera, List<Monster> monsters) {
        if (monsters.isEmpty()) {
            return;
        }
        batch.begin(camera);
        for (Monster monster : monsters) {
            feet.set(monster.position());
            instance.transform.setToTranslation(feet).rotate(Vector3.Y, monster.yaw());

            float swing = (float) Math.sin(monster.walkPhase()) * SWING_ANGLE;
            float punch = (float) Math.sin(monster.attackSwing() * Math.PI) * PUNCH_ANGLE;
            swingLimb("legLeft", swing, 12f);
            swingLimb("legRight", -swing, 12f);
            swingLimb("armLeft", -swing, 24f);
            swingLimb("armRight", swing - punch, 24f);

            batch.render(instance, environment);
        }
        batch.end();
    }

    /** Xoay mot tay/chan quanh khop noi voi than o cao do {@code pivotY}. */
    private void swingLimb(String node, float degrees, float pivotY) {
        Node limb = instance.getNode(node);
        if (limb == null) {
            return;
        }
        limb.localTransform.idt()
                .translate(0f, pivotY * U, 0f)
                .rotate(Vector3.X, degrees)
                .translate(0f, -pivotY * U, 0f);
        limb.calculateTransforms(true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        model.dispose();
        skin.dispose();
    }
}
