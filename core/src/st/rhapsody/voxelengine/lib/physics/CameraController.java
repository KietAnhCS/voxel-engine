package st.rhapsody.voxelengine.lib.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntIntMap;

public class CameraController extends InputAdapter {
    private static final Vector3 playerPosition = new Vector3();
    private static final Vector3 cameraPosition = new Vector3();
    public static Camera camera;
    protected final IntIntMap keys = new IntIntMap();
    protected final Vector3 tmp = new Vector3();
    private final float acceleration = 50f;
    private final float maxForce = 10f;
    private final Vector3 previousCameraDirection = new Vector3();
    protected final Vector3 moveVector = new Vector3();
    private float currentForce = 0f;
    protected float velocity = 0.05f;
    private boolean fullscreen;
    private boolean jump = false;
    private long timeLastSpace;

    public CameraController(Camera camera, boolean usePhysicsForCamera) {
        CameraController.camera = camera;
        PhysicsController.addCamera(camera, usePhysicsForCamera);
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public boolean keyDown(int keycode) {
        keys.put(keycode, keycode);
        previousCameraDirection.set(Vector3.Zero);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        keys.remove(keycode, 0);
        if (keycode == Input.Keys.F) {
            if (fullscreen) {
                Gdx.graphics.setWindowedMode(1280, 768);
                fullscreen = false;
            } else {
                Graphics.DisplayMode desktopDisplayMode = Gdx.graphics.getDisplayMode();
                Gdx.graphics.setFullscreenMode(desktopDisplayMode);
                fullscreen = true;
            }
        }

        if (keycode == Input.Keys.Q) {
            Gdx.app.exit();
        }

        if (keycode == Input.Keys.SPACE) {
            if (System.currentTimeMillis() - timeLastSpace < 200) {
                PhysicsController.toggleFlight();
            }
            timeLastSpace = System.currentTimeMillis();
        }
        if (keycode == Input.Keys.ESCAPE) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
        }

        return true;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity * 10f;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        PhysicsController.rayPick(button);
        return true;
    }

    public void update() {
        try {
            update(Gdx.graphics.getDeltaTime());
            movePlayer(moveVector, jump);
            camera.update(true);
            jump = false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void update(float deltaTime) {
        float degreesPerPixel = 0.3f;
        float deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
        float deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;

        camera.direction.rotate(Vector3.Y, deltaX);

        tmp.set(camera.direction).crs(Vector3.Y).nor();

        float currentPitch = (float) Math.toDegrees(Math.asin(camera.direction.y));
        float newPitch = currentPitch + deltaY;

        if (newPitch > 89f) {
            deltaY = 89f - currentPitch;
        } else if (newPitch < -89f) {
            deltaY = -89f - currentPitch;
        }

        camera.direction.rotate(tmp, deltaY);

        camera.up.set(Vector3.Y);

        previousCameraDirection.set(camera.direction);
        moveVector.set(0, 0, 0);

        Vector3 forward = new Vector3(camera.direction.x, 0, camera.direction.z).nor();
        Vector3 right = new Vector3(camera.direction).crs(camera.up);
        right.y = 0;
        right.nor();

        if (keys.containsKey(Input.Keys.W) || keys.containsKey(Input.Keys.UP)) {
            moveVector.add(new Vector3(forward).scl(velocity));
        }
        if (keys.containsKey(Input.Keys.S) || keys.containsKey(Input.Keys.DOWN)) {
            moveVector.add(new Vector3(forward).scl(-velocity));
        }
        if (keys.containsKey(Input.Keys.A) || keys.containsKey(Input.Keys.LEFT)) {
            moveVector.add(new Vector3(right).scl(-velocity));
        }
        if (keys.containsKey(Input.Keys.D) || keys.containsKey(Input.Keys.RIGHT)) {
            moveVector.add(new Vector3(right).scl(velocity));
        }

        if (keys.containsKey(Input.Keys.SPACE)) {
            jump = true;
        }

    }

    protected void movePlayer(Vector3 moveVector, boolean jump) {
        PhysicsController.movePlayer(moveVector, jump);
    }
}