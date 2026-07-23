package com.voxel.server.world;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mot THE GIOI CHUNG, nhan dien bang mot MA do nguoi choi tu dat (vd "123"). Ai nhap cung
 * ma thi vao cung the gioi - giong "ma phong" trong game nhieu nguoi.
 *
 * Chi luu "seed" (hat giong sinh dia hinh) - toan bo dia hinh sinh lai tu seed nen khong can
 * luu. Cac khoi nguoi choi dat/pha them nam o bang rieng {@link BlockEdit}, khoa theo id the gioi.
 * Vi tri nguoi choi KHONG luu: che do nhieu nguoi ai vao cung spawn moi.
 */
@Entity
@Table(name = "game_worlds")
public class GameWorld {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ma the gioi do nguoi choi go de vao cung nhau - duy nhat. */
    @Column(nullable = false, unique = true)
    private String code;

    /** Hat giong sinh dia hinh - co dinh suot doi the gioi. */
    @Column(nullable = false)
    private long seed;

    protected GameWorld() {
    }

    public GameWorld(String code, long seed) {
        this.code = code;
        this.seed = seed;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public long getSeed() {
        return seed;
    }
}
