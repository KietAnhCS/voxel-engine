package com.voxel.game.terrain.biome;

import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.OakShape;
import com.voxel.game.terrain.decor.ScatterDecorator;
import com.voxel.game.terrain.decor.TreeDecorator;

/** Thao nguyen: co cao bat ngan, cay thua thot. */
public final class SavannaBiome extends Biome {

    public SavannaBiome(Blocks blocks) {
        super("savanna", blocks,
                new TreeDecorator(0.004f, 8, new OakShape()),
                new ScatterDecorator(0.04f, 9, source -> source.deadBush),
                new ScatterDecorator(0.55f, 10, source -> source.tuft));
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        return noise.seaLevel() + 6.0 + noise.hills(x, z) * 4.0;
    }
}
