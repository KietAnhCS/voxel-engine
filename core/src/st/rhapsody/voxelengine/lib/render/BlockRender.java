package st.rhapsody.voxelengine.lib.render;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import st.rhapsody.voxelengine.lib.terrain.block.Block;
import st.rhapsody.voxelengine.lib.terrain.block.IBlockProvider;
import st.rhapsody.voxelengine.lib.terrain.chunk.Chunk;

/**
 * Created by nicklaslof on 31/01/15.
 */
public interface BlockRender {

    boolean addBlock(Vector3 worldPosition, int x, int y, int z, IBlockProvider blockProvider, Chunk chunk, Block block, FloatArray vertices, ShortArray indicies);
}
