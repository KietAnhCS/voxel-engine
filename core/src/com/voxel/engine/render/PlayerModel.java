package com.voxel.engine.render;

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

/**
 * The player figure seen in third person view (F5 key).
 *
 * The character is built from six boxes in true Minecraft proportions (measured in 1/16 block):
 * head 8x8x8, body 8x12x4, the two arms and two legs 4x12x4 - 32 units tall in total,
 * that is 2 blocks, then scaled down to {@link #HEIGHT} to match the real player height.
 * A 64x64 skin texture is wrapped around the boxes by {@link PlayerMesh}.
 *
 * The arms and legs are separate NODES, so rotating a node is enough to make the character walk.
 */
public final class PlayerModel implements Disposable {

    /** Character height in blocks, matching the player's collision shape. */
    private static final float HEIGHT = 1.75f;
    /** One 1/16 block unit after scaling down. */
    private static final float U = HEIGHT / 32f;
    private static final float SWING_SPEED = 9f;
    private static final float SWING_ANGLE = 42f;
    /** Cu quo tay phai tat dan trong ~0.22 giay (1 / 4.5). */
    private static final float ARM_SWING_DECAY = 4.5f;
    /** Bien do tay phai vung ra khi pha / dat khoi. */
    private static final float PUNCH_ANGLE = 70f;

    private final Texture skin;
    private final Model model;
    private final ModelInstance instance;
    private final ModelBatch batch = new ModelBatch();
    private final Environment environment = new Environment();
    private final Vector3 tempPosition = new Vector3();

    private float swingPhase;
    /** Do "quo tay phai", 1 ngay sau cu bam roi tat dan ve 0. */
    private float armSwing;

    public PlayerModel() {
        skin = new Texture(Gdx.files.internal("data/skinzom.png"));
        skin.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        model = PlayerMesh.build(skin, U);
        instance = new ModelInstance(model);
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.85f, 0.85f, 0.85f, 1f));
    }

    /**
     * Places the character in the world.
     *
     * @param feet   feet position
     * @param yaw    direction the character faces, in degrees
     * @param moving when walking, the arms and legs swing
     */
    public void update(Vector3 feet, float yaw, boolean moving, float delta) {
        if (moving) {
            swingPhase += delta * SWING_SPEED;
        } else {
            // When standing still, ease the arms and legs back to straight.
            swingPhase *= Math.max(0f, 1f - delta * 8f);
        }

        // Cu quo tay tat dan sau moi lan pha / dat khoi.
        armSwing = Math.max(0f, armSwing - delta * ARM_SWING_DECAY);

        tempPosition.set(feet);
        instance.transform.setToTranslation(tempPosition).rotate(Vector3.Y, yaw);

        // Chan trai/phai (va tay) vung nguoc chieu nhau -> dang di tu nhien: chan truoc chan sau.
        float swing = (float) Math.sin(swingPhase) * SWING_ANGLE;
        // Tay phai vong ra truoc roi ve cho: mot cung sin(0..pi) khi armSwing chay tu 1 ve 0.
        float punch = (float) Math.sin(armSwing * Math.PI) * PUNCH_ANGLE;
        swingLimb("legLeft", swing, 12f);
        swingLimb("legRight", -swing, 12f);
        swingLimb("armLeft", -swing, 24f);
        swingLimb("armRight", swing - punch, 24f);
    }

    /** Quo tay phai ra mot cai - goi khi nguoi choi pha hoac dat khoi. */
    public void swingArm() {
        armSwing = 1f;
    }

    /** Rotates one arm/leg around the joint with the body at height {@code pivotY}. */
    private void swingLimb(String node, float degrees, float pivotY) {
        com.badlogic.gdx.graphics.g3d.model.Node limb = instance.getNode(node);
        if (limb == null) {
            return;
        }
        limb.localTransform.idt()
                .translate(0f, pivotY * U, 0f)
                .rotate(Vector3.X, degrees)
                .translate(0f, -pivotY * U, 0f);
        limb.calculateTransforms(true);
    }

    public void render(PerspectiveCamera camera) {
        batch.begin(camera);
        batch.render(instance, environment);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        model.dispose();
        skin.dispose();
    }
}
