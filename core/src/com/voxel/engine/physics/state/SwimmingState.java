package com.voxel.engine.physics.state;

import com.voxel.engine.physics.MovementInput;
import com.voxel.engine.physics.MovementState;
import com.voxel.engine.physics.PlayerBody;

/**
 * Dang boi: nguoi choi dang ngap trong nuoc.
 *
 * <p>Giong Minecraft: giu PHIM CACH thi boi len, giu SHIFT thi lan xuong, tha ca hai ra
 * thi chim tu tu. Khi than nguoi nho len khoi mat nuoc thi doi sang {@link GroundState} -
 * o do van con mot cu "nhun" nua de treo minh len bo.
 *
 * <p>Trong nuoc KHONG dung trong luc cua bo vat ly ({@code setGravity(0)}): neu de trong luc
 * chay, van toc roi cu tich luy mai (duoi nuoc khong bao gio "cham dat" de xoa) - ngam cang
 * lau cang chim nhanh va giu phim cach cung khong noi len duoc. Thay vao do van toc doc duoc
 * dat TRUC TIEP moi buoc, nen boi len / chim xuong luc nao cung deu va doan truoc duoc.
 */
public final class SwimmingState implements MovementState {

    private static final float SPEED = 0.045f;
    /**
     * Do day len moi buoc mo phong khi giu phim cach. Bo vat ly chay 120 buoc/giay nen
     * 0.032 cho ra khoang 3.8 khoi/giay - dung nhip boi cua Minecraft.
     */
    private static final float SWIM_UP = 0.032f;
    /** Chu dong lan xuong khi giu SHIFT (~3.6 khoi/giay). */
    private static final float SWIM_DOWN = 0.03f;
    /** Tha het phim thi chim tu tu (~1 khoi/giay) - nhu tha noi trong Minecraft. */
    private static final float DRIFT_DOWN = 0.008f;

    @Override
    public void enter(PlayerBody body) {
        body.setGravity(0f);
        // Xoa van toc roi mang theo tu tren khong: nuoc "do" cu roi lai ngay khi cham nuoc.
        body.clearVelocity();
    }

    @Override
    public MovementState update(PlayerBody body, MovementInput input, float delta) {
        if (input.consumeFlightToggle()) {
            return new FlightState();
        }
        if (!body.isSubmerged()) {
            return new GroundState();
        }

        float vertical = -DRIFT_DOWN;
        if (input.rise()) {
            vertical = SWIM_UP;
        } else if (input.sink()) {
            vertical = -SWIM_DOWN;
        }

        body.setWalkDirection(input.axisX() * SPEED, vertical, input.axisZ() * SPEED);
        return this;
    }

    @Override
    public String label() {
        return "swimming";
    }
}
