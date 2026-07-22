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
     * Van tay cua hinh mot section: bam toan bo dinh va chi so (thuat toan FNV-1a).
     *
     * Hai lan dung hinh cho ra cung van tay nghia la section do khong doi gi het, va
     * {@link ChunkMeshSet} bo qua duoc han - khong tao Mesh moi, nhat la khong dung lai
     * cay BVH va cham (phan dat nhat cua mot lan upload). Chuyen nay xay ra rat thuong:
     * nuoc chay hay den bat sang o mot goc chunk lam doi mau vai dinh, con 7 section kia
     * y nguyen.
     *
     * Tinh o luong mesh nen khung hinh khong phai tra gia.
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

    /** So chi so dau tien cua mesh solid dung cho va cham (phan con lai chi de ve). */
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
