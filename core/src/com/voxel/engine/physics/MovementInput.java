package com.voxel.engine.physics;

public final class MovementInput {

    private float axisX;
    private float axisZ;
    private boolean rise;
    private boolean sink;
    private boolean flightToggled;

    public void set(float axisX, float axisZ, boolean rise, boolean sink) {
        this.axisX = axisX;
        this.axisZ = axisZ;
        this.rise = rise;
        this.sink = sink;
    }

    public void requestFlightToggle() {
        flightToggled = true;
    }

    public boolean consumeFlightToggle() {
        boolean toggled = flightToggled;
        flightToggled = false;
        return toggled;
    }

    public float axisX() {
        return axisX;
    }

    public float axisZ() {
        return axisZ;
    }

    public boolean rise() {
        return rise;
    }

    public boolean sink() {
        return sink;
    }

    public boolean isMoving() {
        return axisX != 0f || axisZ != 0f;
    }
}
