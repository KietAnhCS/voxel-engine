package com.voxel.server.world;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * MOT o khoi ma nguoi choi da dat hoac pha, lech so voi dia hinh goc sinh tu seed.
 * blockId = 0 nghia la o do da bi pha thanh khong khi.
 *
 * Danh chi muc theo world_id de tai het edit cua mot the gioi that nhanh.
 */
@Entity
@Table(name = "block_edits", indexes = @Index(name = "idx_edit_world", columnList = "world_id"))
public class BlockEdit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "world_id", nullable = false)
    private Long worldId;

    @Column(nullable = false)
    private int x;
    @Column(nullable = false)
    private int y;
    @Column(nullable = false)
    private int z;

    /** Ma khoi (0..255). Dung short vi byte cua Java co dau. */
    @Column(name = "block_id", nullable = false)
    private short blockId;

    protected BlockEdit() {
    }

    public BlockEdit(Long worldId, int x, int y, int z, short blockId) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockId = blockId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public short getBlockId() {
        return blockId;
    }
}
