package com.voxel.game.terrain.biome;

import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.OakShape;
import com.voxel.game.terrain.decor.ScatterDecorator;
import com.voxel.game.terrain.decor.TreeDecorator;

/** Thung lung: dai dat trung giua cac vung cao, thuong co suoi va nhieu hoa. */
public final class ValleyBiome extends Biome {

    public ValleyBiome(Blocks blocks) {
        super("valley", blocks,
                new TreeDecorator(0.012f, 15, new OakShape()),
                new ScatterDecorator(0.02f, 16, source -> source.flower),
                new ScatterDecorator(0.02f, 27, source -> source.flowerYellow),
                new ScatterDecorator(0.58f, 17, source -> source.tuft).inPatches(24));
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        return noise.seaLevel() + 2.0 + noise.hills(x, z) * 2.5;
    }
}
