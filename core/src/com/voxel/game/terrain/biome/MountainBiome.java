package com.voxel.game.terrain.biome;

import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.BoulderDecorator;
import com.voxel.game.terrain.decor.PineShape;
import com.voxel.game.terrain.decor.ScatterDecorator;
import com.voxel.game.terrain.decor.TreeDecorator;

/** Nui: song nui sac nhon, tren cao lo da, duoi thap con co va thong. */
public final class MountainBiome extends Biome {

    private static final int TREE_LINE = 34;
    private static final int ROCK_LINE = 26;

    public MountainBiome(Blocks blocks) {
        super("mountains", blocks,
                new TreeDecorator(0.015f, 21, new PineShape()),
                new BoulderDecorator(0.006f, 22),
                new ScatterDecorator(0.16f, 23, source -> source.tuft));
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        double ridge = noise.ridge(x, z);
        return noise.seaLevel() + 20.0 + ridge * ridge * ridge * 40.0 + noise.hills(x, z) * 6.0;
    }

    @Override
    public Block topBlock(int y, int seaLevel) {
        if (y > seaLevel + TREE_LINE) {
            return blocks.cobblestone;
        }
        return y > seaLevel + ROCK_LINE ? blocks.stone : blocks.grass;
    }

    @Override
    public Block fillerBlock(int y, int seaLevel) {
        return y > seaLevel + ROCK_LINE ? blocks.stone : blocks.dirt;
    }
}
