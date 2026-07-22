package com.voxel.game.terrain.biome;

import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.OakShape;
import com.voxel.game.terrain.decor.ScatterDecorator;
import com.voxel.game.terrain.decor.TreeDecorator;

/** Dam lay: dat thap sat muc nuoc, nhieu vung nuoc nong va co lau. */
public final class SwampBiome extends Biome {

    public SwampBiome(Blocks blocks) {
        super("swamp", blocks,
                new TreeDecorator(0.020f, 13, new OakShape()),
                new ScatterDecorator(0.65f, 14, source -> source.tuft).inPatches(20));
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        return noise.seaLevel() + 1.0 + noise.hills(x, z) * 2.0;
    }
}
