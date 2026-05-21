package st.rhapsody.voxelengine.test.terrain.block;

import st.rhapsody.voxelengine.lib.terrain.block.Block;

/**
 * Created by nicklas on 5/7/14.
 */
public class GlassBlock extends Block {
    protected GlassBlock(byte id, String textureRegion) {
        super(id, textureRegion, textureRegion, textureRegion);
    }

    @Override
    public int getOpacity() {
        return 8;
    }
}
