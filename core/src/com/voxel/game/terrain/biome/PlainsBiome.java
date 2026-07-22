package com.voxel.game.terrain.biome;

import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.OakShape;
import com.voxel.game.terrain.decor.ScatterDecorator;
import com.voxel.game.terrain.decor.TreeDecorator;

/** Dong co: dat thoai, phu day co va hoa, thinh thoang mot cay soi le loi. */
public final class PlainsBiome extends Biome {

    public PlainsBiome(Blocks blocks) {
        super("plains", blocks,
                new TreeDecorator(0.006f, 1, new OakShape()),
                new ScatterDecorator(0.015f, 2, source -> source.flower),
                new ScatterDecorator(0.015f, 26, source -> source.flowerYellow),
                new ScatterDecorator(0.62f, 3, source -> source.tuft).inPatches(30));
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        return noise.seaLevel() + 5.0 + noise.hills(x, z) * 3.5;
    }
}
