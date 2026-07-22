package com.voxel.game.terrain.biome;

import com.voxel.engine.block.Block;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.decor.CactusShape;
import com.voxel.game.terrain.decor.ScatterDecorator;
import com.voxel.game.terrain.decor.TreeDecorator;

/** Sa mac: doi cat, xuong rong va bui kho. */
public final class DesertBiome extends Biome {

    public DesertBiome(Blocks blocks) {
        super("desert", blocks,
                new TreeDecorator(0.008f, 11, new CactusShape()),
                // Bui kho chi moc tren CAT: tham so cuoi la khoi mat dat bat buoc.
                new ScatterDecorator(0.05f, 12, source -> source.deadBush, source -> source.sand));
    }

    @Override
    public double surfaceHeight(TerrainNoise noise, int x, int z) {
        return noise.seaLevel() + 6.0 + noise.hills(x, z) * 5.5;
    }

    @Override
    public Block topBlock(int y, int seaLevel) {
        return blocks.sand;
    }

    @Override
    public Block fillerBlock(int y, int seaLevel) {
        return blocks.sandstone;
    }
}
