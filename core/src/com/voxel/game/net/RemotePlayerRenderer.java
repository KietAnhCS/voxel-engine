package com.voxel.game.net;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/**
 * Ve avatar cho nhung nguoi choi KHAC trong the gioi chung.
 *
 * Dung DUNG MOT hinh nguoi (sau khoi hop, ti le nhu Minecraft giong {@code PlayerModel}) roi
 * dat lai vi tri va ve lai cho tung nguoi - re, don gian, du cho vai nguoi choi cung luc.
 * Khac ban local: khong vung tay chan cho gon; ai cung mac ao mau khac de de phan biet ban be.
 */
public final class RemotePlayerRenderer implements Disposable {

    /** Chieu cao nhan vat (block), khop hinh va cham nguoi choi. */
    private static final float HEIGHT = 1.75f;
    /** Mot don vi 1/16 block sau khi thu nho. */
    private static final float U = HEIGHT / 32f;

    private static final Color SKIN = rgb(0xE8B98D);
    private static final Color HAIR = rgb(0x3F2A17);
    private static final Color SHIRT = rgb(0xC0392B);
    private static final Color PANTS = rgb(0x2C3E50);

    private final Model model;
    private final ModelInstance instance;
    private final ModelBatch batch = new ModelBatch();
    private final Environment environment = new Environment();
    private final Vector3 feet = new Vector3();

    public RemotePlayerRenderer() {
        ModelBuilder builder = new ModelBuilder();
        long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        builder.begin();
        // Goc toa do nam giua hai ban chan, truc y huong len - giong PlayerModel.
        box(builder, attributes, PANTS, 4f, 12f, 4f, -2f, 6f, 0f);   // chan trai
        box(builder, attributes, PANTS, 4f, 12f, 4f, 2f, 6f, 0f);    // chan phai
        box(builder, attributes, SHIRT, 8f, 12f, 4f, 0f, 18f, 0f);   // than
        box(builder, attributes, SHIRT, 4f, 12f, 4f, -6f, 18f, 0f);  // tay trai
        box(builder, attributes, SHIRT, 4f, 12f, 4f, 6f, 18f, 0f);   // tay phai
        box(builder, attributes, SKIN, 8f, 8f, 8f, 0f, 28f, 0f);     // dau
        box(builder, attributes, HAIR, 8.4f, 1.6f, 8.4f, 0f, 31.4f, 0f); // toc
        model = builder.end();

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

    private void box(ModelBuilder builder, long attributes, Color color,
                     float width, float height, float depth, float cx, float cy, float cz) {
        builder.part("part", GL20.GL_TRIANGLES, attributes,
                        new Material(ColorAttribute.createDiffuse(color)))
                .box(cx * U, cy * U, cz * U, width * U, height * U, depth * U);
    }

    private static Color rgb(int hex) {
        return new Color(((hex >> 16) & 0xFF) / 255f, ((hex >> 8) & 0xFF) / 255f,
                (hex & 0xFF) / 255f, 1f);
    }

    @Override
    public void dispose() {
        batch.dispose();
        model.dispose();
    }
}
