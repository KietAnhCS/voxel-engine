package com.voxel.game.terrain.carve;

import com.voxel.game.terrain.TerrainNoise;

/**
 * Luat "mai hang": mot cot dat da phai con day bao nhieu khoi tren dau thi moi cho khoet.
 *
 * <p>Neu cam tiet doi thi ca the gioi khong co lay mot cai cua hang nao (nguoi choi khong bao
 * gio tim thay duong xuong long dat). Neu tha cua thi mat dat thung lo cho lo cho, nhin rat
 * lo lieu. Minecraft o giua: hang gan nhu luon nam kin trong long dat, thinh thoang moi tro
 * mot cai mieng ra suon doi.
 *
 * <p>Cach lam o day: mot vung nhieu 2D thoai ({@link TerrainNoise#entrance}) danh dau nhung
 * manh dat hiem (~3% dien tich) duoc phep khoet xuyen len tan mat dat. Vao ngay giua manh do
 * thi mai day 0 (thung han ra thanh cua hang), o vien thi mai mong dan, con lai giu day
 * {@link #ROOF} khoi. Nho vay mieng hang moc ra tu nhien theo dia hinh chu khong rai deu.
 *
 * <p>Rieng hang "pho mai" (nhung bong rong tron) thi dung ban {@link #sealed()} - luon giu
 * mai day, neu khong mat dat se ro nhu to ong.
 */
public final class SurfaceGuard {

    /** So khoi dat da toi thieu phai con lai tren tran hang o cho binh thuong. */
    private static final int ROOF = 4;
    /** Nhieu cua hang tren muc nay: khoet thoai mai len tan mat dat -> mieng hang. */
    private static final double OPEN = 0.44;
    /** Tren muc nay: vien mieng hang, mai chi con mong. */
    private static final double RIM = 0.32;
    /** Mai mong o vien mieng hang. */
    private static final int RIM_ROOF = 2;

    /** null nghia la khong bao gio cho tro mieng len mat dat. */
    private final TerrainNoise noise;

    private SurfaceGuard(TerrainNoise noise) {
        this.noise = noise;
    }

    /** Hang duoc phep tro mieng len mat dat o nhung vung hiem (duong ham, khe nut). */
    public static SurfaceGuard withEntrances(TerrainNoise noise) {
        return new SurfaceGuard(noise);
    }

    /** Hang luon nam kin trong long dat (hang pho mai). */
    public static SurfaceGuard sealed() {
        return new SurfaceGuard(null);
    }

    /** So khoi dat da phai con lai tren dau tai cot nay thi moi duoc khoet. */
    public int requiredRoof(int worldX, int worldZ) {
        if (noise == null) {
            return ROOF;
        }
        double value = noise.entrance(worldX, worldZ);
        if (value > OPEN) {
            return 0;
        }
        if (value > RIM) {
            return RIM_ROOF;
        }
        return ROOF;
    }
}
