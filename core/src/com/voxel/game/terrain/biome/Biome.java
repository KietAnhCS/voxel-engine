package com.voxel.game.terrain.biome;

import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.DecorationContext;
import com.voxel.game.terrain.decor.Decorator;

/**
 * Mot vung sinh thai: quyet dinh dia hinh cao thap the nao, be mat la khoi gi
 * va tren do moc cai gi.
 *
 * Day la Template Method: lop con chi phai dien {@link #surfaceHeight} va
 * (neu muon) doi khoi be mat; phan trang tri dung chung mot khung.
 */
public abstract class Biome {

    private final String name;
    protected final Blocks blocks;
    private final Decorator[] decorators;

    protected Biome(String name, Blocks blocks, Decorator... decorators) {
        this.name = name;
        this.blocks = blocks;
        this.decorators = decorators;
    }

    /** Do cao mat dat tai (x, z) cua rieng biome nay. */
    public abstract double surfaceHeight(TerrainNoise noise, int x, int z);

    /** Khoi tren cung (co, cat, tuyet, da...). */
    public Block topBlock(int y, int seaLevel) {
        return blocks.grass;
    }

    /** Vai lop ngay duoi be mat. */
    public Block fillerBlock(int y, int seaLevel) {
        return blocks.dirt;
    }

    /** Bien co the moc cay/co tren biome nay khong. */
    public boolean isDecorated() {
        return decorators.length > 0;
    }

    /**
     * Chay lan luot cac decorator; decorator dau tien "nhan viec" se dung chuoi lai
     * de khong dat hai vat the chong len nhau.
     */
    public final void decorate(DecorationContext context) {
        for (Decorator decorator : decorators) {
            if (decorator.decorate(context)) {
                return;
            }
        }
    }

    public final String name() {
        return name;
    }

    @Override
    public final String toString() {
        return name;
    }
}
