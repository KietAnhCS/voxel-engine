package com.voxel.engine.physics.state;

import com.voxel.engine.physics.MovementInput;
import com.voxel.engine.physics.MovementState;
import com.voxel.engine.physics.PlayerBody;

public final class GroundState implements MovementState {

    private static final float GRAVITY = -26f;
    private static final float SPEED = 0.115f;
    private static final float JUMP_IMPULSE = 8.6f;

    @Override
    public void enter(PlayerBody body) {
        body.setGravity(GRAVITY);
    }

    @Override
    public MovementState update(PlayerBody body, MovementInput input, float delta) {
        if (input.consumeFlightToggle()) {
            return new FlightState();
        }
        if (body.isSubmerged()) {
            return new SwimmingState();
        }

        body.setWalkDirection(input.axisX() * SPEED, 0f, input.axisZ() * SPEED);

        if (input.rise() && body.onGround()) {
            body.jump(JUMP_IMPULSE);
        }
        return this;
    }

    @Override
    public String label() {
        return "ground";
    }
}
