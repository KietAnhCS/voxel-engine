package com.voxel.game.terrain.biome;

import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;

/** Bai bien: dai cat hep noi bien voi dat lien. */
public final class BeachBiome extends Biome {

    public BeachBiome(Blocks blocks) {
        super("beach", blocks);
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        return noise.seaLevel() + 1.0 + noise.hills(x, z) * 1.5;
    }

    @Override
    public Block topBlock(int y, int seaLevel) {
        return blocks.sand;
    }

    @Override
    public Block fillerBlock(int y, int seaLevel) {
        return blocks.sand;
    }
}
