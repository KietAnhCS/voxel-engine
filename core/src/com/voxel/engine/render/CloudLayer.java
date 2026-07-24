package com.voxel.engine.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * 3D BLOCK CLOUDS like Minecraft: instead of a flat picture in the sky, every cloud is a
 * real box (12 x 4 x 12 blocks) you can fly around and look at from below or above.
 *
 * <p>The sky is divided into a grid of cloud CELLS. A hash of the cell coordinate decides
 * whether a cell holds a cloud, so the pattern is fixed in the WORLD: clouds stay in place
 * while you walk, and the whole layer slowly drifts east with time (like Minecraft).
 *
 * <p>The mesh only holds the cells around the player and is rebuilt when the player (or the
 * drift) crosses into a new cell - about once every 20 seconds, costing well under a
 * millisecond. Faces get baked-in shading: bright tops, dark bottoms, medium sides.
 */
public final class CloudLayer implements Disposable {

    private static final float CELL = 12f;
    private static final float THICKNESS = 4f;
    /** How many cells to each side of the player the cloud field extends. */
    private static final int RADIUS_CELLS = 13;
    /** Cloud altitude - just above the highest mountains (world height 128). */
    private static final float CLOUD_Y = 132f;
    /** Eastward drift in blocks per second. */
    private static final float DRIFT_SPEED = 0.8f;
    /** Portion of cells that hold a cloud in clear weather. */
    private static final float COVERAGE = 0.38f;
    /** Extra coverage while it rains - the sky closes up. */
    private static final float RAIN_EXTRA_COVERAGE = 0.22f;

    private static final int FLOATS_PER_VERTEX = 9;
    /** top, bottom and 4 sides per box. */
    private static final int FACES_PER_BOX = 6;

    private final Mesh mesh;
    private final float[] vertices;
    private final short[] indices;
    private int indexCount;

    private int builtCellX = Integer.MIN_VALUE;
    private int builtCellZ = Integer.MIN_VALUE;
    private float builtCoverage = -1f;

    public CloudLayer() {
        int side = RADIUS_CELLS * 2 + 1;
        int maxBoxes = side * side;
        vertices = new float[maxBoxes * FACES_PER_BOX * 4 * FLOATS_PER_VERTEX];
        indices = new short[maxBoxes * FACES_PER_BOX * 6];
        mesh = new Mesh(false, maxBoxes * FACES_PER_BOX * 4, indices.length,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2,
                        ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
    }

    /**
     * Draws the cloud boxes. Must be called while the sky shader is bound, with blending on
     * and depth testing OFF (the clouds are part of the sky backdrop).
     *
     * @param rain 0 = clear sky, 1 = full rain (denser, is handled by the caller's tint)
     */
    public void render(ShaderProgram shader, PerspectiveCamera camera, float elapsed, float rain) {
        float drift = elapsed * DRIFT_SPEED;
        int cellX = (int) Math.floor((camera.position.x - drift) / CELL);
        int cellZ = (int) Math.floor(camera.position.z / CELL);
        float coverage = COVERAGE + RAIN_EXTRA_COVERAGE * rain;

        if (cellX != builtCellX || cellZ != builtCellZ || Math.abs(coverage - builtCoverage) > 0.05f) {
            rebuild(cellX, cellZ, coverage);
        }

        shader.setUniformf("u_offset", cellX * CELL + drift, CLOUD_Y, cellZ * CELL);
        shader.setUniformf("u_uvFromWorld", 0f);
        shader.setUniformf("u_scroll", 0f, 0f);
        mesh.render(shader, GL20.GL_TRIANGLES, 0, indexCount);
    }

    /** Rebuilds the box mesh in "cloud space" around cell (cellX, cellZ). */
    private void rebuild(int cellX, int cellZ, float coverage) {
        builtCellX = cellX;
        builtCellZ = cellZ;
        builtCoverage = coverage;

        int floatCursor = 0;
        int indexCursor = 0;
        short vertexCount = 0;

        for (int dx = -RADIUS_CELLS; dx <= RADIUS_CELLS; dx++) {
            for (int dz = -RADIUS_CELLS; dz <= RADIUS_CELLS; dz++) {
                if (!cloudAt(cellX + dx, cellZ + dz, coverage)) {
                    continue;
                }
                // Fade the far clouds out so the layer has no hard square edge.
                float distance = (float) Math.sqrt((double) dx * dx + dz * dz) / RADIUS_CELLS;
                float alpha = 0.82f * Math.max(0f, 1f - distance * distance);
                if (alpha < 0.03f) {
                    continue;
                }

                float x0 = dx * CELL;
                float z0 = dz * CELL;
                float x1 = x0 + CELL;
                float z1 = z0 + CELL;

                // Neighbouring cloud cells merge: skip the wall between two touching clouds.
                boolean west = cloudAt(cellX + dx - 1, cellZ + dz, coverage);
                boolean east = cloudAt(cellX + dx + 1, cellZ + dz, coverage);
                boolean north = cloudAt(cellX + dx, cellZ + dz - 1, coverage);
                boolean south = cloudAt(cellX + dx, cellZ + dz + 1, coverage);

                // top (bright) and bottom (dark)
                floatCursor = quad(floatCursor, alpha, 1f,
                        x0, THICKNESS, z0, x1, THICKNESS, z0, x1, THICKNESS, z1, x0, THICKNESS, z1);
                indexCursor = face(indexCursor, vertexCount);
                vertexCount += 4;
                floatCursor = quad(floatCursor, alpha, 0.68f,
                        x0, 0f, z1, x1, 0f, z1, x1, 0f, z0, x0, 0f, z0);
                indexCursor = face(indexCursor, vertexCount);
                vertexCount += 4;

                if (!west) {
                    floatCursor = quad(floatCursor, alpha, 0.84f,
                            x0, 0f, z0, x0, 0f, z1, x0, THICKNESS, z1, x0, THICKNESS, z0);
                    indexCursor = face(indexCursor, vertexCount);
                    vertexCount += 4;
                }
                if (!east) {
                    floatCursor = quad(floatCursor, alpha, 0.84f,
                            x1, 0f, z1, x1, 0f, z0, x1, THICKNESS, z0, x1, THICKNESS, z1);
                    indexCursor = face(indexCursor, vertexCount);
                    vertexCount += 4;
                }
                if (!north) {
                    floatCursor = quad(floatCursor, alpha, 0.90f,
                            x1, 0f, z0, x0, 0f, z0, x0, THICKNESS, z0, x1, THICKNESS, z0);
                    indexCursor = face(indexCursor, vertexCount);
                    vertexCount += 4;
                }
                if (!south) {
                    floatCursor = quad(floatCursor, alpha, 0.90f,
                            x0, 0f, z1, x1, 0f, z1, x1, THICKNESS, z1, x0, THICKNESS, z1);
                    indexCursor = face(indexCursor, vertexCount);
                    vertexCount += 4;
                }
            }
        }

        mesh.setVertices(vertices, 0, floatCursor);
        mesh.setIndices(indices, 0, indexCursor);
        indexCount = indexCursor;
    }

    /** Writes one quad (4 vertices) with a given brightness and alpha. */
    private int quad(int at, float alpha, float shade,
                     float ax, float ay, float az, float bx, float by, float bz,
                     float cx, float cy, float cz, float dx, float dy, float dz) {
        at = vertex(at, ax, ay, az, shade, alpha);
        at = vertex(at, bx, by, bz, shade, alpha);
        at = vertex(at, cx, cy, cz, shade, alpha);
        at = vertex(at, dx, dy, dz, shade, alpha);
        return at;
    }

    private int vertex(int at, float x, float y, float z, float shade, float alpha) {
        vertices[at] = x;
        vertices[at + 1] = y;
        vertices[at + 2] = z;
        vertices[at + 3] = 0f;
        vertices[at + 4] = 0f;
        vertices[at + 5] = shade;
        vertices[at + 6] = shade;
        vertices[at + 7] = shade;
        vertices[at + 8] = alpha;
        return at + FLOATS_PER_VERTEX;
    }

    private int face(int at, short base) {
        indices[at] = base;
        indices[at + 1] = (short) (base + 1);
        indices[at + 2] = (short) (base + 2);
        indices[at + 3] = (short) (base + 2);
        indices[at + 4] = (short) (base + 3);
        indices[at + 5] = base;
        return at + 6;
    }

    /**
     * Does this cell hold a cloud? Two hash octaves: a coarse one groups cells into big
     * cloud banks, a fine one nibbles at their edges - pure hashing, no noise library.
     */
    private static boolean cloudAt(int cellX, int cellZ, float coverage) {
        float coarse = hash(cellX >> 1, cellZ >> 1);
        float fine = hash(cellX, cellZ);
        return coarse * 0.65f + fine * 0.35f < coverage;
    }

    /** Deterministic 0..1 hash of a cell coordinate. */
    private static float hash(int x, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    @Override
    public void dispose() {
        mesh.dispose();
    }
}
