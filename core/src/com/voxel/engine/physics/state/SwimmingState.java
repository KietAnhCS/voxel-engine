package com.voxel.engine.physics.state;

import com.voxel.engine.physics.MovementInput;
import com.voxel.engine.physics.MovementState;
import com.voxel.engine.physics.PlayerBody;

/**
 * Dang boi: nguoi choi dang ngap trong nuoc.
 *
 * <p>Giong Minecraft: giu PHIM CACH thi noi len dan toi mat nuoc, giu SHIFT thi lan xuong,
 * tha ca hai ra thi chim tu tu vi trong luc duoi nuoc rat nhe ({@link #GRAVITY}).
 * Khi than nguoi nho len khoi mat nuoc thi doi sang {@link GroundState} - o do van con
 * mot cu "nhun" nua de treo minh len bo.
 */
public final class SwimmingState implements MovementState {

    private static final float GRAVITY = -2.4f;
    private static final float SPEED = 0.045f;
    /**
     * Do day len moi buoc mo phong khi giu phim cach. Con so nay do bang SwimProbe chu khong
     * tinh nham: bo vat ly chay hai buoc con moi khung hinh, nen 0.028 cho ra khoang
     * 2.1 khoi/giay - dung nhip boi cua Minecraft.
     */
    private static final float SWIM_UP = 0.028f;
    /** Chu dong lan xuong khi giu SHIFT (~3.6 khoi/giay). */
    private static final float SWIM_DOWN = 0.02f;

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
            vertical += SWIM_UP;
        }
        if (input.sink()) {
            vertical -= SWIM_DOWN;
        }

        body.setWalkDirection(input.axisX() * SPEED, vertical, input.axisZ() * SPEED);
        return this;
    }

    @Override
    public String label() {
        return "swimming";
    }
}
