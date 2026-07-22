package com.voxel.game.terrain.biome;

import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.BirchShape;
import com.voxel.game.terrain.decor.OakShape;
import com.voxel.game.terrain.decor.ScatterDecorator;
import com.voxel.game.terrain.decor.TreeDecorator;

/** Rung: nhieu cay soi va bach duong, nen rung day co. */
public final class ForestBiome extends Biome {

    public ForestBiome(Blocks blocks) {
        super("forest", blocks,
                new TreeDecorator(0.045f, 4, new OakShape()),
                new TreeDecorator(0.018f, 5, new BirchShape()),
                new ScatterDecorator(0.02f, 6, source -> source.flower),
                new ScatterDecorator(0.55f, 7, source -> source.tuft).inPatches(22));
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        return noise.seaLevel() + 7.0 + noise.hills(x, z) * 5.0;
    }
}
