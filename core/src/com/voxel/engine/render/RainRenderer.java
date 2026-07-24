package com.voxel.engine.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Rain: a few hundred thin vertical streaks falling around the player.
 *
 * <p>Each streak lives in a cylinder around the camera. Its x/z position comes from a hash
 * of its index (fixed), and its height falls with time and wraps around - so the same small
 * set of quads rains forever without any particle allocation. Streaks are billboarded
 * around the vertical axis so they always face the player.
 *
 * <p>Drawn AFTER the world with depth TEST on but depth WRITE off: drops disappear behind
 * hills, yet never punch holes into anything drawn later.
 */
public final class RainRenderer implements Disposable {

    private static final int STREAKS = 420;
    /** Rain falls inside this radius around the camera (blocks). */
    private static final float RADIUS = 14f;
    /** Height of the rain cylinder; streaks wrap around inside it. */
    private static final float HEIGHT = 20f;
    private static final float STREAK_LENGTH = 0.9f;
    private static final float STREAK_WIDTH = 0.03f;
    private static final float FALL_SPEED = 21f;

    private static final int FLOATS_PER_VERTEX = 9;

    private final Mesh mesh;
    private final float[] vertices = new float[STREAKS * 4 * FLOATS_PER_VERTEX];

    public RainRenderer() {
        short[] indices = new short[STREAKS * 6];
        for (int i = 0; i < STREAKS; i++) {
            short base = (short) (i * 4);
            int at = i * 6;
            indices[at] = base;
            indices[at + 1] = (short) (base + 1);
            indices[at + 2] = (short) (base + 2);
            indices[at + 3] = (short) (base + 2);
            indices[at + 4] = (short) (base + 3);
            indices[at + 5] = base;
        }
        mesh = new Mesh(false, STREAKS * 4, indices.length,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2,
                        ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        mesh.setIndices(indices);
    }

    /**
     * Draws the rain. Call with the sky shader bound, after the world has been drawn.
     *
     * @param strength 0..1 - how heavy the rain looks (drives streak opacity)
     */
    public void render(ShaderProgram shader, PerspectiveCamera camera, float elapsed, float strength) {
        if (strength <= 0.01f) {
            return;
        }

        // Billboard axes: streaks stretch up, and their width faces the camera.
        float rightX = -camera.direction.z;
        float rightZ = camera.direction.x;
        float norm = (float) Math.sqrt(rightX * rightX + rightZ * rightZ);
        if (norm < 1e-4f) {
            rightX = 1f;
            rightZ = 0f;
        } else {
            rightX /= norm;
            rightZ /= norm;
        }
        rightX *= STREAK_WIDTH;
        rightZ *= STREAK_WIDTH;

        float alpha = 0.32f * strength;
        int at = 0;
        for (int i = 0; i < STREAKS; i++) {
            float u = hash(i * 2);
            float v = hash(i * 2 + 1);
            // Even disc distribution: radius ~ sqrt(u), angle ~ v.
            float distance = (float) Math.sqrt(u) * RADIUS;
            double angle = v * Math.PI * 2.0;
            float x = camera.position.x + (float) Math.cos(angle) * distance;
            float z = camera.position.z + (float) Math.sin(angle) * distance;

            // The streak falls with time and wraps around inside the cylinder.
            float cycle = ((elapsed * FALL_SPEED + hash(i + 7331) * HEIGHT) % HEIGHT + HEIGHT) % HEIGHT;
            float top = camera.position.y + HEIGHT * 0.5f - cycle;
            float bottom = top - STREAK_LENGTH;

            at = vertex(at, x - rightX, bottom, z - rightZ, alpha);
            at = vertex(at, x + rightX, bottom, z + rightZ, alpha);
            at = vertex(at, x + rightX, top, z + rightZ, alpha * 0.4f);
            at = vertex(at, x - rightX, top, z - rightZ, alpha * 0.4f);
        }

        mesh.setVertices(vertices, 0, at);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        shader.setUniformf("u_offset", 0f, 0f, 0f);
        shader.setUniformf("u_uvFromWorld", 0f);
        shader.setUniformf("u_scroll", 0f, 0f);
        mesh.render(shader, GL20.GL_TRIANGLES);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private int vertex(int at, float x, float y, float z, float alpha) {
        vertices[at] = x;
        vertices[at + 1] = y;
        vertices[at + 2] = z;
        vertices[at + 3] = 0f;
        vertices[at + 4] = 0f;
        vertices[at + 5] = 0.62f;
        vertices[at + 6] = 0.68f;
        vertices[at + 7] = 0.80f;
        vertices[at + 8] = alpha;
        return at + FLOATS_PER_VERTEX;
    }

    /** Deterministic 0..1 hash of an index. */
    private static float hash(int i) {
        long h = i * 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 30;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    @Override
    public void dispose() {
        mesh.dispose();
    }
}
