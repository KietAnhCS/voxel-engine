package st.rhapsody.voxelengine.test.terrain.block;

import st.rhapsody.voxelengine.lib.terrain.block.Block;

/**
 * Created by nicklas on 5/6/14.
 */
public class GrassBlock extends Block {
    protected GrassBlock(byte id, String textureRegionTop, String textureRegionBottom, String textureRegionSides) {
        super(id, textureRegionTop, textureRegionBottom, textureRegionSides);
    }
}
