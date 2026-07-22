package com.voxel.engine.world;

import com.voxel.engine.EngineConfig;

import java.util.Arrays;

public final class ChunkStorage {

    private final int size;
    private final int height;
    private final int sizeShift;
    private final int areaShift;
    private final byte[] blocks;
    /**
     * Bo dem anh sang DANG DUOC DUNG DE VE.
     *
     * Luong mesh doc mang nay trong khi luong anh sang co the dang tinh lai chunk. Vi the
     * {@link LightEngine} khong bao gio sua truc tiep vao day: no tinh tron mot ban moi
     * roi goi {@link #commitLight} de doi sang - mot phep gan duy nhat. Nguoi doc luon
     * thay mot ban day du, khong bao gio kip nhin thay mang vua bi xoa trang (do chinh
     * la nguyen nhan lam chunk chop den).
     */
    private volatile byte[] light;
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

    /**
     * Trai mang 3 chieu ra mang 1 chieu. Vi chunkSize luon la luy thua cua 2 nen
     * phep nhan duoc thay bang dich bit va phep cong bang OR.
     *
     * Do phuc tap: O(1) - ba phep tinh bit, khong co vong lap nao.
     */
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

    /** Kich thuoc mot bo dem anh sang, de LightEngine tu cap phat ban dang tinh do. */
    public int lightBufferSize() {
        return size * size * height;
    }

    /** Byte anh sang tho cua ban DANG dung, de so xem ban vua tinh co khac khong. */
    public byte rawLight(int index) {
        return light[index];
    }

    /** Dua ban anh sang vua tinh xong vao su dung, thay cho ban cu. */
    public void commitLight(byte[] rebuilt) {
        this.light = rebuilt;
    }

    public int skyFloor(int x, int z) {
        return skyFloor[columnIndex(x, z)];
    }

    /**
     * Quet lai toan bo chunk de tim do cao khoi dac cao nhat cua tung cot.
     * Do phuc tap: O(size^2 * height) - chi goi mot lan sau khi sinh xong chunk.
     */
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

    /**
     * Cap nhat mot cot sau khi nguoi choi dat/pha mot khoi - re hon nhieu so voi
     * {@link #rebuildSkyFloor()}.
     *
     * Do phuc tap: O(1) khi dat khoi; O(y) khi pha dung khoi tren cung (phai do
     * xuong tim khoi dac ke tiep), O(1) khi pha khoi nam duoi mat.
     */
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
