package st.rhapsody.voxelengine.lib.physics;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;

public class FlyingCameraController extends CameraController {
    public FlyingCameraController(Camera camera) {
        super(camera, false);
    }

    @Override
    void update(float deltaTime) {
        super.update(deltaTime);
        int UP = Input.Keys.SPACE;
        int SHIFT = Input.Keys.SHIFT_LEFT;

        if (keys.containsKey(UP)) {
            moveVector.add(0, deltaTime * velocity, 0);
        }

        if (keys.containsKey(SHIFT)){
            moveVector.add(0, -deltaTime * velocity, 0);
        }
    }

    @Override
    protected void movePlayer(Vector3 moveVector, boolean jump) {
        camera.translate(moveVector);
    }
}