package st.rhapsody.voxelengine.test;

import com.badlogic.gdx.Game;
import st.rhapsody.voxelengine.lib.physics.PhysicsController;
import st.rhapsody.voxelengine.test.screen.VoxelScreen;

public class VoxelEngineTest extends Game {

    @Override
    public void create() {
        setScreen(new VoxelScreen());
    }
}
