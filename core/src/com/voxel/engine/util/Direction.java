package com.voxel.engine.util;

public enum Direction {

    EAST(1, 0, 0, 1, 0, 1, 0, 0, -1, 0, 1, 0, 0.72f),
    WEST(-1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0.72f),
    UP(0, 1, 0, 1, 1, 0, -1, 0, 0, 0, 0, 1, 1.00f),
    DOWN(0, -1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0.48f),
    SOUTH(0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0.86f),
    NORTH(0, 0, -1, 1, 0, 0, -1, 0, 0, 0, 1, 0, 0.86f);

    public static final Direction[] ALL = values();

    private final int dx;
    private final int dy;
    private final int dz;
    private final int ox;
    private final int oy;
    private final int oz;
    private final int ux;
    private final int uy;
    private final int uz;
    private final int vx;
    private final int vy;
    private final int vz;
    private final float shade;

    Direction(int dx, int dy, int dz,
              int ox, int oy, int oz,
              int ux, int uy, int uz,
              int vx, int vy, int vz,
              float shade) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.ux = ux;
        this.uy = uy;
        this.uz = uz;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.shade = shade;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public int dz() {
        return dz;
    }

    public int originX() {
        return ox;
    }

    public int originY() {
        return oy;
    }

    public int originZ() {
        return oz;
    }

    public int tangentX() {
        return ux;
    }

    public int tangentY() {
        return uy;
    }

    public int tangentZ() {
        return uz;
    }

    public int bitangentX() {
        return vx;
    }

    public int bitangentY() {
        return vy;
    }

    public int bitangentZ() {
        return vz;
    }

    public float shade() {
        return shade;
    }
}
