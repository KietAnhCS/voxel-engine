package com.voxel.game.terrain.biome;

import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;

/** Dai duong: day bien thap hon muc nuoc bien, phu cat va soi. */
public final class OceanBiome extends Biome {

    public OceanBiome(Blocks blocks) {
        super("ocean", blocks);
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        double depth = Math.min(0.0, noise.continent(x, z) + 0.30) * 40.0;
        return noise.seaLevel() - 6.0 + depth + noise.hills(x, z) * 3.0;
    }

    @Override
    public Block topBlock(int y, int seaLevel) {
        return y < seaLevel - 12 ? blocks.gravel : blocks.sand;
    }

    @Override
    public Block fillerBlock(int y, int seaLevel) {
        return blocks.sand;
    }
}
