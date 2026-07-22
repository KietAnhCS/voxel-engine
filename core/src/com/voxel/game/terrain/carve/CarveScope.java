package com.voxel.game.terrain.carve;

import com.voxel.engine.block.Block;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;

/**
 * Vung lam viec cua mot carver: chi cho phep khoet trong pham vi chunk hien tai.
 * Nho vay hai chunk canh nhau khoet cung mot con "giun" ma khong dam nhau.
 */
public final class CarveScope {

    private final ChunkWriter writer;
    private final Blocks blocks;
    private final int originX;
    private final int originZ;
    private final int size;
    private final int worldHeight;

    public CarveScope(ChunkWriter writer, Blocks blocks, int originX, int originZ, int size, int worldHeight) {
        this.writer = writer;
        this.blocks = blocks;
        this.originX = originX;
        this.originZ = originZ;
        this.size = size;
        this.worldHeight = worldHeight;
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

    /** Xoa mot khoi da, tru khi lam thung day ho/bien. */
    public void clear(int localX, int y, int localZ) {
        if (y < 1 || y >= worldHeight) {
            return;
        }
        Block current = writer.get(localX, y, localZ);
        if (current.isAir() || current.isLiquid()) {
            return;
        }
        if (writer.get(localX, y + 1, localZ).isLiquid()) {
            return;
        }
        writer.set(localX, y, localZ, blocks.air);
    }
}
