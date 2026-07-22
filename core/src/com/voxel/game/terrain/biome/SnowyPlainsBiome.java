package com.voxel.game.terrain.biome;

import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.PineShape;
import com.voxel.game.terrain.decor.TreeDecorator;

/** Dong bang da (taiga): phang, mat phu da cuoi, rung thong thua. */
public final class SnowyPlainsBiome extends Biome {

    public SnowyPlainsBiome(Blocks blocks) {
        super("snowy_plains", blocks,
                new TreeDecorator(0.030f, 25, new PineShape()));
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        return noise.seaLevel() + 6.0 + noise.hills(x, z) * 4.0;
    }

    @Override
    public Block topBlock(int y, int seaLevel) {
        return blocks.cobblestone;
    }
}
