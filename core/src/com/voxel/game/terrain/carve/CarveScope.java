package com.voxel.game.terrain.carve;

import com.voxel.engine.block.Block;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;

/**
 * Vung lam viec cua mot carver: chi cho phep khoet trong pham vi chunk hien tai.
 * Nho vay hai chunk canh nhau khoet cung mot con "giun" ma khong dam nhau.
 *
 * <p>Truoc khi khoet mot o, scope hoi {@link SurfaceGuard}: cot nay phai con day bao nhieu
 * khoi tren dau? Do day duoc do bang MAT DAT cua cot (khoi dac cao nhat) chu khong phai bang
 * "co khong khi ngay tren dau khong" - nho vay hai duong ham cat nhau van thong nhau binh
 * thuong, chi rieng cho sap thung len troi moi bi chan.
 */
public final class CarveScope {

    /** Chua do mat dat cua cot nay. */
    private static final int UNKNOWN = Integer.MIN_VALUE;

    private final ChunkWriter writer;
    private final Blocks blocks;
    private final SurfaceGuard guard;
    private final int originX;
    private final int originZ;
    private final int size;
    private final int worldHeight;

    /** Mat dat cua tung cot, do mot lan roi nho lai (do phuc tap O(height) cho moi cot). */
    private final int[] surface;

    public CarveScope(ChunkWriter writer, Blocks blocks, SurfaceGuard guard,
                      int originX, int originZ, int size, int worldHeight) {
        this.writer = writer;
        this.blocks = blocks;
        this.guard = guard;
        this.originX = originX;
        this.originZ = originZ;
        this.size = size;
        this.worldHeight = worldHeight;
        this.surface = new int[size * size];
        java.util.Arrays.fill(this.surface, UNKNOWN);
    }

    public int originX() {
        return originX;
    }

    public int originZ() {
        return originZ;
    }

    public int size() {
        return size;
    }

    public int worldHeight() {
        return worldHeight;
    }

    /** Kiem tra nhanh: qua cau ban kinh nay co cham vao chunk khong? */
    public boolean touches(double worldX, double worldZ, double radius) {
        return worldX + radius >= originX
                && worldX - radius < originX + size
                && worldZ + radius >= originZ
                && worldZ - radius < originZ + size;
    }

    public void clearSphere(double worldX, double worldY, double worldZ, double radius) {
        clearEllipsoid(worldX, worldY, worldZ, radius, radius, radius);
    }

    /** Khoet mot khoi elip (dung cho khe nut: hep ngang, sau doc). */
    public void clearEllipsoid(double worldX, double worldY, double worldZ,
                               double radiusX, double radiusY, double radiusZ) {
        int minX = Math.max(originX, (int) Math.floor(worldX - radiusX));
        int maxX = Math.min(originX + size - 1, (int) Math.ceil(worldX + radiusX));
        int minZ = Math.max(originZ, (int) Math.floor(worldZ - radiusZ));
        int maxZ = Math.min(originZ + size - 1, (int) Math.ceil(worldZ + radiusZ));
        int minY = Math.max(1, (int) Math.floor(worldY - radiusY));
        int maxY = Math.min(worldHeight - 1, (int) Math.ceil(worldY + radiusY));

        for (int x = minX; x <= maxX; x++) {
            double nx = (x + 0.5 - worldX) / radiusX;
            for (int z = minZ; z <= maxZ; z++) {
                double nz = (z + 0.5 - worldZ) / radiusZ;
                if (nx * nx + nz * nz > 1.0) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    double ny = (y + 0.5 - worldY) / radiusY;
                    if (nx * nx + ny * ny + nz * nz > 1.0) {
                        continue;
                    }
                    clear(x - originX, y, z - originZ);
                }
            }
        }
    }

    /** Xoa mot khoi da, tru khi lam thung day ho/bien hoac lam mai hang mong qua. */
    public void clear(int localX, int y, int localZ) {
        if (y < 1 || y >= worldHeight) {
            return;
        }
        Block current = writer.get(localX, y, localZ);
        if (current.isAir() || current.isLiquid()) {
            return;
        }
        // Ngay tren dau la nuoc -> khoet la thung day ho, nuoc chay het xuong hang.
        if (writer.get(localX, y + 1, localZ).isLiquid()) {
            return;
        }

        int roof = guard.requiredRoof(originX + localX, originZ + localZ);
        if (y + roof > surfaceHeight(localX, localZ)) {
            return;
        }
        writer.set(localX, y, localZ, blocks.air);
    }

    /**
     * Cao do mat dat cua mot cot: khoi DAC cao nhat (nuoc khong tinh, de hang khong dam
     * len day ho). Ket qua duoc nho lai vi moi cot bi hoi rat nhieu lan.
     */
    private int surfaceHeight(int localX, int localZ) {
        int index = localX * size + localZ;
        if (surface[index] != UNKNOWN) {
            return surface[index];
        }
        int top = 0;
        for (int y = worldHeight - 1; y > 0; y--) {
            Block block = writer.get(localX, y, localZ);
            if (!block.isAir() && !block.isLiquid()) {
                top = y;
                break;
            }
        }
        surface[index] = top;
        return top;
    }
}
