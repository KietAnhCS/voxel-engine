package com.voxel.engine.physics.state;

import com.voxel.engine.physics.MovementInput;
import com.voxel.engine.physics.MovementState;
import com.voxel.engine.physics.PlayerBody;

public final class SwimmingState implements MovementState {

    private static final float GRAVITY = -2.4f;
    private static final float SPEED = 0.045f;
    private static final float BUOYANCY = 0.055f;

    @Override
    public void enter(PlayerBody body) {
        body.setGravity(GRAVITY);
    }

    @Override
    public MovementState update(PlayerBody body, MovementInput input, float delta) {
        if (input.consumeFlightToggle()) {
            return new FlightState();
        }
        if (!body.isSubmerged()) {
            return new GroundState();
        }

        float vertical = 0f;
        if (input.rise()) {
            vertical += BUOYANCY;
        }
        if (input.sink()) {
            vertical -= BUOYANCY;
        }

        body.setWalkDirection(input.axisX() * SPEED, vertical, input.axisZ() * SPEED);
        return this;
    }

    @Override
    public String label() {
        return "swimming";
    }
}
