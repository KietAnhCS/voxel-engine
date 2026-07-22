package com.voxel.game.terrain.biome;

import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.PineShape;
import com.voxel.game.terrain.decor.TreeDecorator;

/** Dinh nui da: cao nhat the gioi, mat phu da cuoi, long toan da. */
public final class SnowyPeaksBiome extends Biome {

    public SnowyPeaksBiome(Blocks blocks) {
        super("snowy_peaks", blocks,
                new TreeDecorator(0.004f, 24, new PineShape()));
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        double ridge = noise.ridge(x, z);
        return noise.seaLevel() + 26.0 + ridge * ridge * ridge * 42.0 + noise.hills(x, z) * 5.0;
    }

    @Override
    public Block topBlock(int y, int seaLevel) {
        return blocks.cobblestone;
    }

    /** Van phai ghi de: mac dinh cua Biome la dat, ma dinh nui thi duoi lop mat la da. */
    @Override
    public Block fillerBlock(int y, int seaLevel) {
        return blocks.stone;
    }
}
