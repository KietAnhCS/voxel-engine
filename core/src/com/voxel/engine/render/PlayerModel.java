package com.voxel.engine.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/**
 * The player figure seen in third person view (F5 key).
 *
 * The character is built from six boxes in true Minecraft proportions (measured in 1/16 block):
 * head 8x8x8, body 8x12x4, the two arms and two legs 4x12x4 - 32 units tall in total,
 * that is 2 blocks, then scaled down to {@link #HEIGHT} to match the real player height.
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

    private static final Color SKIN = rgb(0xE8B98D);
    private static final Color HAIR = rgb(0x3F2A17);
    private static final Color SHIRT = rgb(0x00A8A8);
    private static final Color PANTS = rgb(0x3B44AA);

    private final Model model;
    private final ModelInstance instance;
    private final ModelBatch batch = new ModelBatch();
    private final Environment environment = new Environment();
    private final Matrix4 transform = new Matrix4();
    private final Vector3 tempPosition = new Vector3();

    private float swingPhase;

    public PlayerModel() {
        ModelBuilder builder = new ModelBuilder();
        long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        builder.begin();
        // The origin sits under the character's feet, with the y axis pointing up.
        box(builder, attributes, "legLeft", PANTS, 4f, 12f, 4f, -2f, 6f, 0f);
        box(builder, attributes, "legRight", PANTS, 4f, 12f, 4f, 2f, 6f, 0f);
        box(builder, attributes, "body", SHIRT, 8f, 12f, 4f, 0f, 18f, 0f);
        box(builder, attributes, "armLeft", SHIRT, 4f, 12f, 4f, -6f, 18f, 0f);
        box(builder, attributes, "armRight", SHIRT, 4f, 12f, 4f, 6f, 18f, 0f);
        box(builder, attributes, "head", SKIN, 8f, 8f, 8f, 0f, 28f, 0f);
        box(builder, attributes, "hair", HAIR, 8.4f, 1.6f, 8.4f, 0f, 31.4f, 0f);
        model = builder.end();

        instance = new ModelInstance(model);
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.85f, 0.85f, 0.85f, 1f));
    }

    /**
     * Adds a box as its own node.
     *
     * @param cx,cy,cz box centre, in 1/16 block relative to the point between the feet
     */
    private void box(ModelBuilder builder, long attributes, String name, Color color,
                     float width, float height, float depth, float cx, float cy, float cz) {
        builder.node().id = name;
        builder.part(name, com.badlogic.gdx.graphics.GL20.GL_TRIANGLES, attributes,
                        new Material(ColorAttribute.createDiffuse(color)))
                .box(cx * U, cy * U, cz * U, width * U, height * U, depth * U);
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

        tempPosition.set(feet);
        instance.transform.setToTranslation(tempPosition).rotate(Vector3.Y, yaw);

        float swing = (float) Math.sin(swingPhase) * SWING_ANGLE;
        swingLimb("legLeft", swing, 12f);
        swingLimb("legRight", -swing, 12f);
        swingLimb("armLeft", -swing, 24f);
        swingLimb("armRight", swing, 24f);
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
