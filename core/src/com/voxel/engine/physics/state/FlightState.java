package com.voxel.engine.physics.state;

import com.voxel.engine.physics.MovementInput;
import com.voxel.engine.physics.MovementState;
import com.voxel.engine.physics.PlayerBody;

public final class FlightState implements MovementState {

    private static final float SPEED = 0.24f;
    private static final float VERTICAL_SPEED = 0.2f;

    @Override
    public void enter(PlayerBody body) {
        body.setGravity(0f);
    }

    @Override
    public MovementState update(PlayerBody body, MovementInput input, float delta) {
        if (input.consumeFlightToggle()) {
            return new GroundState();
        }

        float vertical = 0f;
        if (input.rise()) {
            vertical += VERTICAL_SPEED;
        }
        if (input.sink()) {
            vertical -= VERTICAL_SPEED;
        }

        body.setWalkDirection(input.axisX() * SPEED, vertical, input.axisZ() * SPEED);
        return this;
    }

    @Override
    public String label() {
        return "flight";
    }
}
