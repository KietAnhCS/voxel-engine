package com.voxel.engine.render;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;

public final class ChunkMeshSet {

    private final Mesh[] solid;
    private final Mesh[] translucent;
    private final BoundingBox[] solidBounds;
    private final BoundingBox[] translucentBounds;
    private final long[] solidKeys;
    private final long[] translucentKeys;
    private final boolean[] uploaded;
    private final Matrix4 transform = new Matrix4();

    public ChunkMeshSet(int sections, float originX, float originZ) {
        this.solid = new Mesh[sections];
        this.translucent = new Mesh[sections];
        this.solidBounds = new BoundingBox[sections];
        this.translucentBounds = new BoundingBox[sections];
        this.solidKeys = new long[sections];
        this.translucentKeys = new long[sections];
        this.uploaded = new boolean[sections];
        this.transform.setToTranslation(originX, 0f, originZ);
    }

    /**
     * Is this section identical to the previous upload?
     *
     * If so, skip it ENTIRELY: keep the old Mesh and keep the old collision shape. It is not
     * allowed to skip only the collision part while still replacing the Mesh - the BVH tree
     * reads the Mesh buffer directly, so replacing the Mesh and disposing the old one leaves
     * the BVH pointing into freed memory.
     */
    public boolean isUnchanged(int section, long solidKey, long translucentKey) {
        if (uploaded[section] && solidKeys[section] == solidKey && translucentKeys[section] == translucentKey) {
            return true;
        }
        uploaded[section] = true;
        solidKeys[section] = solidKey;
        translucentKeys[section] = translucentKey;
        return false;
    }

    public Matrix4 transform() {
        return transform;
    }

    public int sections() {
        return solid.length;
    }

    public Mesh solid(int section) {
        return solid[section];
    }

    public Mesh translucent(int section) {
        return translucent[section];
    }

    public BoundingBox solidBounds(int section) {
        return solidBounds[section];
    }

    public BoundingBox translucentBounds(int section) {
        return translucentBounds[section];
    }

    public void replaceSolid(int section, Mesh mesh) {
        if (solid[section] != null) {
            solid[section].dispose();
        }
        solid[section] = mesh;
        solidBounds[section] = boundsOf(mesh);
    }

    public void replaceTranslucent(int section, Mesh mesh) {
        if (translucent[section] != null) {
            translucent[section].dispose();
        }
        translucent[section] = mesh;
        translucentBounds[section] = boundsOf(mesh);
    }

    private BoundingBox boundsOf(Mesh mesh) {
        if (mesh == null) {
            return null;
        }
        BoundingBox box = new BoundingBox();
        mesh.calculateBoundingBox(box);
        box.min.add(transform.val[Matrix4.M03], 0f, transform.val[Matrix4.M23]);
        box.max.add(transform.val[Matrix4.M03], 0f, transform.val[Matrix4.M23]);
        box.set(box.min, box.max);
        return box;
    }

    public void dispose() {
        for (int i = 0; i < solid.length; i++) {
            if (solid[i] != null) {
                solid[i].dispose();
                solid[i] = null;
            }
            if (translucent[i] != null) {
                translucent[i].dispose();
                translucent[i] = null;
            }
        }
    }
}
