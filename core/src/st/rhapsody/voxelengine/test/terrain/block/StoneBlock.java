package st.rhapsody.voxelengine.test.terrain.block;

import st.rhapsody.voxelengine.lib.terrain.block.Block;

/**
 * Created by nicklas on 5/5/14.
 */
public class StoneBlock extends Block {
    protected StoneBlock(byte id, String textureRegion) {
        super(id, textureRegion,textureRegion,textureRegion);
    }
}
