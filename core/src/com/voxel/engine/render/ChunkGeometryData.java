package com.voxel.engine.render;

public final class ChunkGeometryData {

    private final float[][] solidVertices;
    private final short[][] solidIndices;
    private final float[][] translucentVertices;
    private final short[][] translucentIndices;

    public ChunkGeometryData(int sections) {
        this.solidVertices = new float[sections][];
        this.solidIndices = new short[sections][];
        this.translucentVertices = new float[sections][];
        this.translucentIndices = new short[sections][];
    }

    public void setSolid(int section, float[] vertices, short[] indices) {
        solidVertices[section] = vertices;
        solidIndices[section] = indices;
    }

    public void setTranslucent(int section, float[] vertices, short[] indices) {
        translucentVertices[section] = vertices;
        translucentIndices[section] = indices;
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
