package st.rhapsody.voxelengine.test.terrain.biome;

import st.rhapsody.voxelengine.lib.terrain.biome.Biome;
import st.rhapsody.voxelengine.test.terrain.block.BlockProvider;

/**
 * Created by nicklas on 6/18/14.
 */
public class LowMountainBiome extends Biome {
    @Override
    public int getHeight() {
        return 35;
    }

    @Override
    public double getFieldObstacleAmmount() {
        return 3;
    }

    @Override
    public byte getGroundFillerBlock() {
        return BlockProvider.limeStone.getId();
    }

    @Override
    public byte getMountainFillerBlock() {
        return BlockProvider.limeStone.getId();
    }

    @Override
    public boolean hasSandBeach() {
        return false;
    }

    @Override
    public double getAmmountOfWater() {
        return 5;
    }
}
