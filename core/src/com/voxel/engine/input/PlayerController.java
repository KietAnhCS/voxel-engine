package com.voxel.engine.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.physics.MovementInput;

import java.util.ArrayDeque;
import java.util.Deque;

public final class PlayerController extends InputAdapter {

    private static final float DEGREES_PER_PIXEL = 0.22f;
    private static final float MAX_PITCH = 89f;
    private static final long DOUBLE_TAP_WINDOW_MS = 260L;

    private final PerspectiveCamera camera;
    private final MovementInput input = new MovementInput();
    private final Deque<InteractionRequest> requests = new ArrayDeque<InteractionRequest>();
    private final Vector3 forward = new Vector3();
    private final Vector3 right = new Vector3();
    private final Vector3 rotationAxis = new Vector3();

    private long lastJumpTapMillis;
    private boolean jumpHeld;
    private boolean fullscreen;

    public PlayerController(PerspectiveCamera camera) {
        this.camera = camera;
    }

    public MovementInput input() {
        return input;
    }

    public InteractionRequest pollRequest() {
        synchronized (requests) {
            return requests.pollFirst();
        }
    }

    public void poll() {
        float forwardAxis = 0f;
        float strafeAxis = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            forwardAxis += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            forwardAxis -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            strafeAxis += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            strafeAxis -= 1f;
        }

        forward.set(camera.direction.x, 0f, camera.direction.z).nor();
        right.set(forward).crs(Vector3.Y).nor();

        float moveX = right.x * strafeAxis + forward.x * forwardAxis;
        float moveZ = right.z * strafeAxis + forward.z * forwardAxis;

        float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (length > 1f) {
            moveX /= length;
            moveZ /= length;
        }

        boolean rise = Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean sink = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        input.set(moveX, moveZ, rise, sink);
    }

    public void applyMouseLook() {
        if (!Gdx.input.isCursorCatched()) {
            return;
        }

        float yaw = -Gdx.input.getDeltaX() * DEGREES_PER_PIXEL;
        float pitchDelta = -Gdx.input.getDeltaY() * DEGREES_PER_PIXEL;

        camera.direction.rotate(Vector3.Y, yaw);

        rotationAxis.set(camera.direction).crs(Vector3.Y).nor();
        float currentPitch = (float) Math.toDegrees(Math.asin(camera.direction.y));
        float clamped = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, currentPitch + pitchDelta));
        camera.direction.rotate(rotationAxis, clamped - currentPitch);

        camera.up.set(Vector3.Y);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.SPACE && !jumpHeld) {
            jumpHeld = true;
            long now = System.currentTimeMillis();
            if (now - lastJumpTapMillis < DOUBLE_TAP_WINDOW_MS) {
                input.requestFlightToggle();
                lastJumpTapMillis = 0L;
            } else {
                lastJumpTapMillis = now;
            }
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.SPACE) {
            jumpHeld = false;
        }
        if (keycode == Input.Keys.ESCAPE) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
        }
        if (keycode == Input.Keys.F) {
            toggleFullscreen();
        }
        if (keycode == Input.Keys.Q) {
            Gdx.app.exit();
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!Gdx.input.isCursorCatched()) {
            Gdx.input.setCursorCatched(true);
            return true;
        }

        InteractionRequest request = null;
        if (button == Input.Buttons.LEFT) {
            request = InteractionRequest.BREAK;
        } else if (button == Input.Buttons.RIGHT) {
            boolean alternate = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                    || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
            request = alternate ? InteractionRequest.PLACE_ALTERNATE : InteractionRequest.PLACE;
        }

        if (request != null) {
            synchronized (requests) {
                requests.addLast(request);
            }
        }
        return true;
    }

    private void toggleFullscreen() {
        if (fullscreen) {
            Gdx.graphics.setWindowedMode(1280, 720);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
        fullscreen = !fullscreen;
    }
}
