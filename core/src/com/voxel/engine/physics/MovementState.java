package com.voxel.engine.physics;

public interface MovementState {

    void enter(PlayerBody body);

    MovementState update(PlayerBody body, MovementInput input, float delta);

    String label();
}
