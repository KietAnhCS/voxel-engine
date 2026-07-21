package com.voxel.engine.world;

import com.voxel.engine.EngineConfig;

import java.util.Arrays;

public final class ChunkStorage {

    private final int size;
    private final int height;
    private final int sizeShift;
    private final int areaShift;
    private final byte[] blocks;
    private final byte[] light;
    private final short[] skyFloor;
    private int nonAirCount;

    public ChunkStorage(EngineConfig config) {
        this.size = config.chunkSize();
        this.height = config.worldHeight();
        this.sizeShift = config.chunkShift();
        this.areaShift = sizeShift << 1;
        this.blocks = new byte[size * size * height];
        this.light = new byte[size * size * height];
        this.skyFloor = new short[size * size];
        Arrays.fill(skyFloor, (short) height);
    }

    public int index(int x, int y, int z) {
        return (y << areaShift) | (x << sizeShift) | z;
    }

    public int columnIndex(int x, int z) {
        return (x << sizeShift) | z;
    }

    public boolean contains(int x, int y, int z) {
        return x >= 0 && z >= 0 && y >= 0 && x < size && z < size && y < height;
    }

    public byte blockId(int x, int y, int z) {
        return blocks[index(x, y, z)];
    }

    public void setBlockId(int x, int y, int z, byte id) {
        int i = index(x, y, z);
        byte previous = blocks[i];
        if (previous == id) {
            return;
        }
        if (previous == 0) {
            nonAirCount++;
        } else if (id == 0) {
            nonAirCount--;
        }
        blocks[i] = id;
    }

    public int skyLight(int index) {
        return (light[index] >> 4) & 0x0F;
    }

    public int blockLight(int index) {
        return light[index] & 0x0F;
    }

    public int combinedLight(int index) {
        byte packed = light[index];
        int sky = (packed >> 4) & 0x0F;
        int block = packed & 0x0F;
        return sky > block ? sky : block;
    }

    public void setSkyLight(int index, int value) {
        light[index] = (byte) ((light[index] & 0x0F) | (value << 4));
    }

    public void setBlockLight(int index, int value) {
        light[index] = (byte) ((light[index] & 0xF0) | value);
    }

    public void clearLight() {
        Arrays.fill(light, (byte) 0);
    }

    public int skyFloor(int x, int z) {
        return skyFloor[columnIndex(x, z)];
    }

    public void rebuildSkyFloor() {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int floor = 0;
                for (int y = height - 1; y >= 0; y--) {
                    if (blocks[index(x, y, z)] != 0) {
                        floor = y + 1;
                        break;
                    }
                }
                skyFloor[columnIndex(x, z)] = (short) floor;
            }
        }
    }

    public void updateSkyFloor(int x, int y, int z, boolean placed) {
        int column = columnIndex(x, z);
        if (placed) {
            if (y + 1 > skyFloor[column]) {
                skyFloor[column] = (short) (y + 1);
            }
            return;
        }
        if (y + 1 == skyFloor[column]) {
            int floor = 0;
            for (int scan = y; scan >= 0; scan--) {
                if (blocks[index(x, scan, z)] != 0) {
                    floor = scan + 1;
                    break;
                }
            }
            skyFloor[column] = (short) floor;
        }
    }

    public int nonAirCount() {
        return nonAirCount;
    }

    public int size() {
        return size;
    }

    public int height() {
        return height;
    }
}
