package com.voxel.engine.physics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btAxisSweep3;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btGhostPairCallback;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.utils.Array;
import com.voxel.engine.render.CollisionSink;
import com.voxel.engine.world.Chunk;

import java.util.HashMap;
import java.util.Map;

public final class PhysicsWorld implements CollisionSink {

    private static final float WORLD_EXTENT = 4096f;

    private static boolean nativesLoaded;

    private final btDefaultCollisionConfiguration collisionConfiguration;
    private final btCollisionDispatcher dispatcher;
    private final btAxisSweep3 broadphase;
    private final btSequentialImpulseConstraintSolver solver;
    private final btDiscreteDynamicsWorld dynamicsWorld;
    private final btGhostPairCallback ghostPairCallback;
    private final Map<Chunk, SectionBodies> chunkBodies = new HashMap<Chunk, SectionBodies>();

    private PlayerBody player;

    public PhysicsWorld() {
        if (!nativesLoaded) {
            Bullet.init(true, true);
            nativesLoaded = true;
        }

        collisionConfiguration = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfiguration);
        broadphase = new btAxisSweep3(
                new Vector3(-WORLD_EXTENT, -WORLD_EXTENT, -WORLD_EXTENT),
                new Vector3(WORLD_EXTENT, WORLD_EXTENT, WORLD_EXTENT));
        solver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
        dynamicsWorld.setGravity(new Vector3(0f, -26f, 0f));

        ghostPairCallback = new btGhostPairCallback();
        broadphase.getOverlappingPairCache().setInternalGhostPairCallback(ghostPairCallback);
    }

    public PlayerBody spawnPlayer(Vector3 position) {
        player = new PlayerBody(dynamicsWorld, position);
        return player;
    }

    public void step(float delta) {
        dynamicsWorld.stepSimulation(delta, 4, 1f / 120f);
    }

    @Override
    public void updateSection(Chunk chunk, int section, Mesh mesh) {
        SectionBodies bodies = chunkBodies.get(chunk);
        if (bodies == null) {
            bodies = new SectionBodies(chunk.config().worldHeight() / 16);
            chunkBodies.put(chunk, bodies);
        }
        bodies.release(dynamicsWorld, section);

        if (mesh == null || mesh.getNumIndices() == 0) {
            return;
        }

        Array<MeshPart> parts = new Array<MeshPart>(1);
        parts.add(new MeshPart("collision", mesh, 0, mesh.getNumIndices(), GL20.GL_TRIANGLES));

        btBvhTriangleMeshShape shape = new btBvhTriangleMeshShape(parts);
        btRigidBody body = new btRigidBody(0f, null, shape, Vector3.Zero);
        body.setWorldTransform(new Matrix4().setToTranslation(chunk.originX(), 0f, chunk.originZ()));
        body.setFriction(0.4f);

        dynamicsWorld.addRigidBody(body,
                (short) btBroadphaseProxy.CollisionFilterGroups.StaticFilter,
                (short) (btBroadphaseProxy.CollisionFilterGroups.CharacterFilter
                        | btBroadphaseProxy.CollisionFilterGroups.DefaultFilter));

        bodies.store(section, body, shape);
    }

    @Override
    public void removeChunk(Chunk chunk) {
        SectionBodies bodies = chunkBodies.remove(chunk);
        if (bodies != null) {
            bodies.releaseAll(dynamicsWorld);
        }
    }

    public int collisionBodyCount() {
        return dynamicsWorld.getNumCollisionObjects();
    }

    public void dispose() {
        for (SectionBodies bodies : chunkBodies.values()) {
            bodies.releaseAll(dynamicsWorld);
        }
        chunkBodies.clear();

        if (player != null) {
            player.dispose();
            player = null;
        }

        dynamicsWorld.dispose();
        solver.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        collisionConfiguration.dispose();
        ghostPairCallback.dispose();
    }

    private static final class SectionBodies {

        private final btRigidBody[] bodies;
        private final btBvhTriangleMeshShape[] shapes;

        private SectionBodies(int sections) {
            bodies = new btRigidBody[sections];
            shapes = new btBvhTriangleMeshShape[sections];
        }

        private void store(int section, btRigidBody body, btBvhTriangleMeshShape shape) {
            bodies[section] = body;
            shapes[section] = shape;
        }

        private void release(btDiscreteDynamicsWorld world, int section) {
            if (bodies[section] != null) {
                world.removeRigidBody(bodies[section]);
                bodies[section].dispose();
                bodies[section] = null;
            }
            if (shapes[section] != null) {
                shapes[section].dispose();
                shapes[section] = null;
            }
        }

        private void releaseAll(btDiscreteDynamicsWorld world) {
            for (int section = 0; section < bodies.length; section++) {
                release(world, section);
            }
        }
    }
}
