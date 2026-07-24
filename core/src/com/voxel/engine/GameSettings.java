package com.voxel.engine;

/**
 * The player's options - SINGLETON pattern: there is exactly ONE settings object in the
 * whole game, and everyone reads the live values from it every frame. The settings screen
 * writes here; the mouse look, the camera, the sky and the HUD pick the changes up at once
 * without any wiring between them.
 */
public final class GameSettings {

    private static final GameSettings INSTANCE = new GameSettings();

    public static GameSettings get() {
        return INSTANCE;
    }

    public static final float MIN_SENSITIVITY = 0.05f;
    public static final float MAX_SENSITIVITY = 0.50f;
    public static final float MIN_FOV = 60f;
    public static final float MAX_FOV = 110f;

    private float mouseSensitivity = 0.22f;
    private float fieldOfView = 72f;
    private boolean cloudsEnabled = true;
    private boolean vignetteEnabled = true;

    private GameSettings() {
    }

    public float mouseSensitivity() {
        return mouseSensitivity;
    }

    public void setMouseSensitivity(float value) {
        mouseSensitivity = clamp(value, MIN_SENSITIVITY, MAX_SENSITIVITY);
    }

    public float fieldOfView() {
        return fieldOfView;
    }

    public void setFieldOfView(float value) {
        fieldOfView = clamp(value, MIN_FOV, MAX_FOV);
    }

    public boolean cloudsEnabled() {
        return cloudsEnabled;
    }

    public void toggleClouds() {
        cloudsEnabled = !cloudsEnabled;
    }

    public boolean vignetteEnabled() {
        return vignetteEnabled;
    }

    public void toggleVignette() {
        vignetteEnabled = !vignetteEnabled;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
