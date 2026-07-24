package com.voxel.game.tools;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.physics.MovementInput;
import com.voxel.engine.physics.MovementState;
import com.voxel.engine.physics.PhysicsWorld;
import com.voxel.engine.physics.PlayerBody;
import com.voxel.engine.physics.state.GroundState;
import com.voxel.engine.physics.state.SwimmingState;

/**
 * Kiem tra chuyen dong duoi nuoc bang bo vat ly THAT (Bullet), khong can mo cua so game.
 *
 * Chay bang:  gradlew :desktop:physicsProbe
 *
 * Cach do: tha nguoi choi vao khoang khong, bao cho than the biet "dang ngap nuoc", chay
 * mot giay mo phong roi xem no len xuong bao nhieu khoi. Nho vay chinh mot con so trong
 * {@code SwimmingState} la do lai duoc ngay, khong phai vao game boi thu.
 */
public final class PhysicsProbe {

    private static final float STEP = 1f / 60f;
    private static final int ONE_SECOND = 60;

    private PhysicsProbe() {
    }

    public static void main(String[] args) {
        float rising = swim(true, false);
        float sinking = swim(false, false);
        float diving = swim(false, true);
        float hop = waterHop();

        int failed = 0;
        failed += check("giu phim cach duoi nuoc -> NOI LEN", rising > 0.5f);
        failed += check("noi len khoang 2 khoi/giay nhu Minecraft", rising > 1.4f && rising < 3.2f);
        failed += check("tha tay ra -> chim tu tu chu khong roi thang", sinking < 0f && sinking > -3f);
        failed += check("giu SHIFT -> lan xuong nhanh hon", diving < sinking);
        failed += check("chan con dam nuoc -> nhun len du cao de treo len bo", hop > 1f);

        System.out.printf("giu phim cach: %+.2f o/giay | tha tay: %+.2f | giu SHIFT: %+.2f | nhun len bo: %+.2f o%n",
                rising, sinking, diving, hop);
        System.out.println(failed == 0 ? "ALL CHECKS PASSED" : failed + " CHECKS FAILED");
    }

    /** Len (duong) hay xuong (am) bao nhieu khoi sau mot giay khi dang ngap nuoc. */
    private static float swim(boolean rise, boolean sink) {
        PhysicsWorld physics = new PhysicsWorld();
        PlayerBody body = physics.spawnPlayer(new Vector3(0f, 60f, 0f));
        body.setSubmerged(true);

        MovementState state = new SwimmingState();
        state.enter(body);
        MovementInput input = new MovementInput();

        float start = body.position().y;
        for (int i = 0; i < ONE_SECOND; i++) {
            input.set(0f, 0f, rise, sink);
            state = state.update(body, input, STEP);
            physics.step(STEP);
        }
        float moved = body.position().y - start;
        physics.dispose();
        return moved;
    }

    /**
     * Dung o mat nuoc (than da nho len, chan con dam nuoc): giu phim cach thi nhun len duoc
     * bao nhieu khoi. Phai hon 1 khoi thi moi treo duoc len bo.
     */
    private static float waterHop() {
        PhysicsWorld physics = new PhysicsWorld();
        PlayerBody body = physics.spawnPlayer(new Vector3(0f, 60f, 0f));
        body.setSubmerged(false);
        body.setFeetInWater(true);

        MovementState state = new GroundState();
        state.enter(body);
        MovementInput input = new MovementInput();

        float start = body.position().y;
        float highest = start;
        for (int i = 0; i < 20; i++) {
            input.set(0f, 0f, true, false);
            state = state.update(body, input, STEP);
            physics.step(STEP);
            highest = Math.max(highest, body.position().y);
        }
        physics.dispose();
        return highest - start;
    }

    private static int check(String what, boolean ok) {
        System.out.println((ok ? "  pass  " : "  FAIL  ") + what);
        return ok ? 0 : 1;
    }
}
