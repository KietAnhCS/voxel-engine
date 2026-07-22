package com.voxel.game.terrain.biome;

import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.BoulderDecorator;
import com.voxel.game.terrain.decor.OakShape;
import com.voxel.game.terrain.decor.ScatterDecorator;
import com.voxel.game.terrain.decor.TreeDecorator;

/** Vung doi: cao hon dong bang, nhap nho, co da tang lo thien. */
public final class HillsBiome extends Biome {

    public HillsBiome(Blocks blocks) {
        super("hills", blocks,
                new TreeDecorator(0.020f, 18, new OakShape()),
                new BoulderDecorator(0.004f, 19),
                new ScatterDecorator(0.30f, 20, source -> source.tuft));
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        double ridge = noise.ridge(x, z);
        return noise.seaLevel() + 13.0 + noise.hills(x, z) * 9.0 + ridge * ridge * 10.0;
    }
}
