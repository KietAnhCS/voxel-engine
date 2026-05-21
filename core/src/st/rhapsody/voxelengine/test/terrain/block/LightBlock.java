package st.rhapsody.voxelengine.test.terrain.block;

import st.rhapsody.voxelengine.lib.terrain.block.Block;
import st.rhapsody.voxelengine.lib.terrain.block.IBlockProvider;
import st.rhapsody.voxelengine.lib.terrain.chunk.Chunk;

/**
 * Created by nicklas on 5/2/14.
 */
public class LightBlock extends Block {
    protected LightBlock(byte id, String textureRegion) {
        super(id, textureRegion,textureRegion,textureRegion);
    }

    @Override
    public boolean isLightSource() {
        return true;
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    protected boolean blockRenderSide(IBlockProvider blockProvider, Chunk chunk, int x, int y, int z, Side side) {
        byte blockAtSide = side.getBlockAt(chunk, x, y, z);
        if (blockAtSide == 0){
            return true;
        }
        if (blockProvider.getBlockById(blockAtSide) == this){
            return false;
        }

        return true;
    }
}
