package com.voxel.engine.physics;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btKinematicCharacterController;

public final class PlayerBody {

    public static final float EYE_HEIGHT = 0.72f;

    private final btPairCachingGhostObject ghost;
    private final btCapsuleShape shape;
    private final btKinematicCharacterController controller;
    private final btDiscreteDynamicsWorld dynamicsWorld;
    private final Vector3 position = new Vector3();
    private final Vector3 walkDirection = new Vector3();
    private final Matrix4 transform = new Matrix4();

    private boolean submerged;
    /** Chan con dam trong nuoc (du than nguoi da nho len khoi mat nuoc). */
    private boolean feetInWater;

    PlayerBody(btDiscreteDynamicsWorld dynamicsWorld, Vector3 spawn) {
        this.dynamicsWorld = dynamicsWorld;
        this.shape = new btCapsuleShape(0.35f, 1.05f);
        this.ghost = new btPairCachingGhostObject();
        this.ghost.setWorldTransform(new Matrix4().setToTranslation(spawn));
        this.ghost.setCollisionShape(shape);
        this.ghost.setCollisionFlags(btCollisionObject.CollisionFlags.CF_CHARACTER_OBJECT);
        this.controller = new btKinematicCharacterController(ghost, shape, 0.55f);
        this.position.set(spawn);

        dynamicsWorld.addCollisionObject(ghost,
                (short) btBroadphaseProxy.CollisionFilterGroups.CharacterFilter,
                (short) (btBroadphaseProxy.CollisionFilterGroups.StaticFilter
                        | btBroadphaseProxy.CollisionFilterGroups.DefaultFilter));
        dynamicsWorld.addAction(controller);
    }

    public void setGravity(float gravity) {
        controller.setGravity(new Vector3(0f, gravity, 0f));
    }

    /** Xoa het van toc dang co (dung khi vao che do bay de khong con troi theo cu nhay truoc do). */
    public void clearVelocity() {
        controller.setLinearVelocity(new Vector3(0f, 0f, 0f));
    }

    public void setWalkDirection(float x, float y, float z) {
        walkDirection.set(x, y, z);
        controller.setWalkDirection(walkDirection);
    }

    public void jump(float impulse) {
        controller.jump(new Vector3(0f, impulse, 0f));
    }

    public boolean onGround() {
        return controller.onGround();
    }

    public Vector3 position() {
        ghost.getWorldTransform(transform);
        transform.getTranslation(position);
        return position;
    }

    public void teleport(Vector3 target) {
        transform.setToTranslation(target);
        ghost.setWorldTransform(transform);
        position.set(target);
    }

    public boolean isSubmerged() {
        return submerged;
    }

    public void setSubmerged(boolean submerged) {
        this.submerged = submerged;
    }

    /** True khi ban chan con o trong nuoc - luc do van nhun len duoc de treo len bo. */
    public boolean isFeetInWater() {
        return feetInWater;
    }

    public void setFeetInWater(boolean feetInWater) {
        this.feetInWater = feetInWater;
    }

    void dispose() {
        dynamicsWorld.removeAction(controller);
        dynamicsWorld.removeCollisionObject(ghost);
        controller.dispose();
        ghost.dispose();
        shape.dispose();
    }
}
