package com.voxel.game.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.voxel.engine.render.PlayerMesh;

/**
 * Ve avatar cho nhung nguoi choi KHAC trong the gioi chung.
 *
 * Dung DUNG MOT hinh nguoi (sau khoi hop, ti le nhu Minecraft giong {@code PlayerModel}) roi
 * dat lai vi tri va ve lai cho tung nguoi - re, don gian, du cho vai nguoi choi cung luc.
 * Hinh nguoi boc cung mot skin 64x64 ({@code skinzom.png}) nhu ban local; khong vung tay chan
 * cho gon.
 */
public final class RemotePlayerRenderer implements Disposable {

    /** Chieu cao nhan vat (block), khop hinh va cham nguoi choi. */
    private static final float HEIGHT = 1.75f;
    /** Mot don vi 1/16 block sau khi thu nho. */
    private static final float U = HEIGHT / 32f;

    private final Texture skin;
    private final Model model;
    private final ModelInstance instance;
    private final ModelBatch batch = new ModelBatch();
    private final Environment environment = new Environment();
    private final Vector3 feet = new Vector3();

    public RemotePlayerRenderer() {
        skin = new Texture(Gdx.files.internal("data/skinzom.png"));
        skin.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        model = PlayerMesh.build(skin, U);
        instance = new ModelInstance(model);
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.85f, 0.85f, 0.85f, 1f));
    }

    /** Ve tat ca nguoi choi khac bang cach dat lai cung mot hinh roi ve lai cho tung nguoi. */
    public void render(PerspectiveCamera camera, RemotePlayers players) {
        if (players.all().isEmpty()) {
            return;
        }
        batch.begin(camera);
        for (RemotePlayer player : players.all()) {
            feet.set(player.x(), player.y(), player.z());
            instance.transform.setToTranslation(feet).rotate(Vector3.Y, player.yaw());
            batch.render(instance, environment);
        }
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        model.dispose();
        skin.dispose();
    }
}
