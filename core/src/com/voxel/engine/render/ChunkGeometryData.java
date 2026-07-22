package com.voxel.engine.render;

public final class ChunkGeometryData {

    private final float[][] solidVertices;
    private final short[][] solidIndices;
    private final int[] collisionIndexCounts;
    private final float[][] translucentVertices;
    private final short[][] translucentIndices;
    private final long[] solidKeys;
    private final long[] translucentKeys;

    public ChunkGeometryData(int sections) {
        this.solidVertices = new float[sections][];
        this.solidIndices = new short[sections][];
        this.collisionIndexCounts = new int[sections];
        this.translucentVertices = new float[sections][];
        this.translucentIndices = new short[sections][];
        this.solidKeys = new long[sections];
        this.translucentKeys = new long[sections];
    }

    public void setSolid(int section, float[] vertices, short[] indices, int collisionIndexCount) {
        solidVertices[section] = vertices;
        solidIndices[section] = indices;
        collisionIndexCounts[section] = collisionIndexCount;
        solidKeys[section] = fingerprint(vertices, indices, collisionIndexCount);
    }

    public long solidKey(int section) {
        return solidKeys[section];
    }

    public long translucentKey(int section) {
        return translucentKeys[section];
    }

    /**
     * Fingerprint of one section's geometry: hashes all vertices and indices (FNV-1a algorithm).
     *
     * If two builds give the same fingerprint, that section did not change at all, and
     * {@link ChunkMeshSet} can skip it entirely - no new Mesh, and above all no rebuilding of
     * the collision BVH tree (the most expensive part of an upload). This happens very often:
     * flowing water or a lamp lighting up in one corner of the chunk changes a few vertex
     * colours while the other 7 sections stay identical.
     *
     * Computed on the mesher thread, so the frame does not pay for it.
     */
    private static long fingerprint(float[] vertices, short[] indices, int extra) {
        long key = (0xcbf29ce484222325L ^ extra) * 0x100000001b3L;
        for (int i = 0; i < vertices.length; i++) {
            key = (key ^ Float.floatToRawIntBits(vertices[i])) * 0x100000001b3L;
        }
        for (int i = 0; i < indices.length; i++) {
            key = (key ^ indices[i]) * 0x100000001b3L;
        }
        return key;
    }

    /** How many leading indices of the solid mesh are used for collision (the rest is only drawn). */
    public int collisionIndexCount(int section) {
        return collisionIndexCounts[section];
    }

    public void setTranslucent(int section, float[] vertices, short[] indices) {
        translucentVertices[section] = vertices;
        translucentIndices[section] = indices;
        translucentKeys[section] = fingerprint(vertices, indices, 0);
    }

    public int sections() {
        return solidVertices.length;
    }

    public float[] solidVertices(int section) {
        return solidVertices[section];
    }

    public short[] solidIndices(int section) {
        return solidIndices[section];
    }

    public float[] translucentVertices(int section) {
        return translucentVertices[section];
    }

    public short[] translucentIndices(int section) {
        return translucentIndices[section];
    }
}
