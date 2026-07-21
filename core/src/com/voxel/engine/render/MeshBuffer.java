package com.voxel.engine.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;

public final class MeshBuffer {

    public static final int FLOATS_PER_VERTEX = 12;
    private static final int MAX_VERTICES = 60000;

    private final FloatArray vertices = new FloatArray(4096);
    private final ShortArray indices = new ShortArray(6144);

    public boolean addQuad(float[] positions, float[] uvs, float nx, float ny, float nz, Color[] colors) {
        int base = vertices.size / FLOATS_PER_VERTEX;
        if (base + 4 > MAX_VERTICES) {
            return false;
        }

        for (int corner = 0; corner < 4; corner++) {
            int p = corner * 3;
            int t = corner * 2;
            Color color = colors[corner];
            vertices.add(positions[p]);
            vertices.add(positions[p + 1]);
            vertices.add(positions[p + 2]);
            vertices.add(uvs[t]);
            vertices.add(uvs[t + 1]);
            vertices.add(nx);
            vertices.add(ny);
            vertices.add(nz);
            vertices.add(color.r);
            vertices.add(color.g);
            vertices.add(color.b);
            vertices.add(color.a);
        }

        indices.add((short) base);
        indices.add((short) (base + 1));
        indices.add((short) (base + 2));
        indices.add((short) (base + 2));
        indices.add((short) (base + 3));
        indices.add((short) base);
        return true;
    }

    public boolean isEmpty() {
        return indices.size == 0;
    }

    public int vertexCount() {
        return vertices.size / FLOATS_PER_VERTEX;
    }

    public int indexCount() {
        return indices.size;
    }

    public float[] vertexData() {
        return vertices.toArray();
    }

    public short[] indexData() {
        return indices.toArray();
    }

    public void reset() {
        vertices.clear();
        indices.clear();
    }
}
