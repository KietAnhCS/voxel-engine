package com.voxel.engine.util;

/**
 * Gop toa do chunk (x, z) thanh MOT khoa long duy nhat de lam khoa cua bang bam.
 *
 * 32 bit cao giu x, 32 bit thap giu z. Cach nay khong bao gio dung khoa (moi cap
 * (x, z) cho dung mot long khac nhau), va khong phai cap phat object khoa nhu khi
 * dung Map&lt;Point, Chunk&gt; - quan trong vi bang chunk bi tra cuu hang van lan moi giay.
 *
 * Do phuc tap: O(1) cho ca ma hoa va giai ma.
 */
public final class ChunkKey {

    private ChunkKey() {
    }

    public static long of(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int x(long key) {
        return (int) (key >> 32);
    }

    public static int z(long key) {
        return (int) key;
    }

    public static String describe(long key) {
        return "(" + x(key) + ", " + z(key) + ")";
    }
}
