package com.voxel.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.input.BlockInteraction;
import com.voxel.engine.input.InteractionRequest;
import com.voxel.engine.input.PlayerController;
import com.voxel.engine.physics.MovementState;
import com.voxel.engine.physics.PhysicsWorld;
import com.voxel.engine.physics.PlayerBody;
import com.voxel.engine.physics.VoxelRaycaster;
import com.voxel.engine.physics.state.GroundState;
import com.voxel.engine.render.WorldRenderer;
import com.voxel.engine.world.ChunkFactory;
import com.voxel.engine.world.World;
import com.voxel.engine.world.WorldEventBus;

public final class VoxelEngine implements Disposable {

    private static final float FIXED_STEP = 1f / 60f;
    private static final int MAX_STEPS_PER_FRAME = 5;
    private static final float REACH = 6f;
    private static final float HEAD_BOB_AMPLITUDE = 0.045f;
    private static final float HEAD_BOB_FREQUENCY = 9.5f;

    private final EngineConfig config;
    private final BlockRegistry registry;
    private final World world;
    private final WorldEventBus eventBus;
    private final PhysicsWorld physics;
    private final PlayerBody player;
    private final WorldRenderer renderer;
    private final PlayerController controller;
    private final PerspectiveCamera camera;
    private final ModelBatch batch;
    private final ShaderProgram shaderProgram;
    private final Environment environment;
    private final VoxelRaycaster raycaster = new VoxelRaycaster();
    private final VoxelRaycaster.Hit hit = new VoxelRaycaster.Hit();
    private final Color skyColor = new Color(0.44f, 0.66f, 0.94f, 1f);
    private final Color waterColor = new Color(0.05f, 0.13f, 0.28f, 1f);
    private final ColorAttribute fogAttribute;
    private final Vector3 eye = new Vector3();
    private final Vector3 spawn = new Vector3();

    private MovementState movementState = new GroundState();
    private BlockInteraction interaction;
    private float accumulator;
    private float elapsed;
    private float bobPhase;
    private boolean submerged;

    private VoxelEngine(Builder builder) {
        this.config = new EngineConfig(builder.chunkSize, builder.worldHeight, builder.viewDistance,
                builder.seaLevel, resolveSeed(builder.seed), builder.workerThreads);
        this.registry = builder.registry;
        this.camera = builder.camera;
        this.eventBus = new WorldEventBus();
        this.world = new World(config, registry, builder.chunkFactory, eventBus);

        this.shaderProgram = compileShader();
        this.environment = new Environment();
        this.fogAttribute = new ColorAttribute(ColorAttribute.Fog, skyColor);
        this.environment.set(fogAttribute);

        Material solidMaterial = new Material("voxel-solid",
                TextureAttribute.createDiffuse(builder.atlas.getTextures().first()));
        Material fluidMaterial = new Material("voxel-fluid",
                TextureAttribute.createDiffuse(builder.atlas.getTextures().first()),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.8f));

        this.batch = new ModelBatch(new DefaultShaderProvider() {
            @Override
            protected Shader createShader(Renderable renderable) {
                return new DefaultShader(renderable, new DefaultShader.Config(), shaderProgram);
            }
        });

        this.renderer = new WorldRenderer(world, camera, builder.atlas, solidMaterial, fluidMaterial);
        this.physics = new PhysicsWorld();
        this.renderer.attachCollisionSink(physics);
        eventBus.subscribe(renderer);

        this.spawn.set(builder.spawn);
        this.player = physics.spawnPlayer(builder.spawn);
        this.movementState.enter(player);

        this.controller = new PlayerController(camera);
        Gdx.input.setInputProcessor(controller);
        Gdx.input.setCursorCatched(true);

        world.update(builder.spawn);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setInteraction(BlockInteraction interaction) {
        this.interaction = interaction;
    }

    public void render(float delta) {
        elapsed += delta;
        controller.applyMouseLook();
        controller.poll();

        accumulator += Math.min(delta, 0.25f);
        int steps = 0;
        while (accumulator >= FIXED_STEP && steps < MAX_STEPS_PER_FRAME) {
            stepSimulation();
            accumulator -= FIXED_STEP;
            steps++;
        }

        updateCamera(delta);
        processInteractions();
        renderer.update();
        drawWorld();
    }

    private void stepSimulation() {
        Vector3 position = player.position();
        if (position.y < 1f) {
            player.teleport(spawn);
            position = player.position();
        }
        world.update(position);

        eye.set(position.x, position.y + PlayerBody.EYE_HEIGHT, position.z);
        submerged = world.isSubmerged(eye);
        player.setSubmerged(world.isSubmerged(position));

        MovementState next = movementState.update(player, controller.input(), FIXED_STEP);
        if (next != movementState) {
            movementState = next;
            movementState.enter(player);
        }

        physics.step(FIXED_STEP);

        if (controller.input().isMoving() && player.onGround()) {
            bobPhase += FIXED_STEP * HEAD_BOB_FREQUENCY;
        }
    }

    private void updateCamera(float delta) {
        Vector3 position = player.position();
        float bob = controller.input().isMoving() && player.onGround()
                ? HEAD_BOB_AMPLITUDE * MathUtils.sin(bobPhase)
                : 0f;
        camera.position.set(position.x, position.y + PlayerBody.EYE_HEIGHT + bob, position.z);
        camera.update(true);
    }

    private void processInteractions() {
        InteractionRequest request = controller.pollRequest();
        while (request != null) {
            if (interaction != null && raycaster.cast(world, camera.position, camera.direction, REACH, hit)) {
                if (request == InteractionRequest.BREAK) {
                    interaction.onBreak(world, hit);
                } else {
                    interaction.onPlace(world, hit, request == InteractionRequest.PLACE_ALTERNATE);
                }
            }
            request = controller.pollRequest();
        }
    }

    private void drawWorld() {
        Color background = submerged ? waterColor : skyColor;
        fogAttribute.color.set(background);

        Gdx.gl.glClearColor(background.r, background.g, background.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);

        shaderProgram.bind();
        shaderProgram.setUniformf("u_fogstr", submerged ? 0.11f : 0.028f);
        shaderProgram.setUniformf("u_time", elapsed);

        batch.begin(camera);
        batch.render(renderer.solidPass(), environment);
        batch.end();

        Gdx.gl.glDepthMask(false);
        batch.begin(camera);
        batch.render(renderer.translucentPass(), environment);
        batch.end();
        Gdx.gl.glDepthMask(true);
    }

    private ShaderProgram compileShader() {
        ShaderProgram.pedantic = false;
        ShaderProgram program = new ShaderProgram(
                Gdx.files.internal("data/shaders/shader.vs"),
                Gdx.files.internal("data/shaders/shader.fs"));
        if (!program.isCompiled()) {
            throw new IllegalStateException("shader compilation failed: " + program.getLog());
        }
        return program;
    }

    private static long resolveSeed(long seed) {
        return seed != 0L ? seed : System.nanoTime();
    }

    public World world() {
        return world;
    }

    public BlockRegistry registry() {
        return registry;
    }

    public EngineConfig config() {
        return config;
    }

    public String movementLabel() {
        return movementState.label();
    }

    public int loadedChunks() {
        return world.loadedChunkCount();
    }

    public int visibleChunks() {
        return renderer.visibleChunks();
    }

    public int visibleSections() {
        return renderer.visibleSections();
    }

    public int visibleTriangles() {
        return renderer.visibleTriangles();
    }

    public int pendingMeshUpdates() {
        return renderer.pendingMeshUpdates();
    }

    public boolean isSubmerged() {
        return submerged;
    }

    @Override
    public void dispose() {
        renderer.dispose();
        world.dispose();
        physics.dispose();
        batch.dispose();
        shaderProgram.dispose();
    }

    public static final class Builder {

        private int chunkSize = 16;
        private int worldHeight = 128;
        private int viewDistance = 6;
        private int seaLevel = 48;
        private long seed;
        private int workerThreads = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        private BlockRegistry registry;
        private ChunkFactory chunkFactory;
        private TextureAtlas atlas;
        private PerspectiveCamera camera;
        private Vector3 spawn = new Vector3(8f, 90f, 8f);

        private Builder() {
        }

        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder worldHeight(int worldHeight) {
            this.worldHeight = worldHeight;
            return this;
        }

        public Builder viewDistance(int viewDistance) {
            this.viewDistance = viewDistance;
            return this;
        }

        public Builder seaLevel(int seaLevel) {
            this.seaLevel = seaLevel;
            return this;
        }

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public Builder workerThreads(int workerThreads) {
            this.workerThreads = Math.max(1, workerThreads);
            return this;
        }

        public Builder blocks(BlockRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder chunks(ChunkFactory chunkFactory) {
            this.chunkFactory = chunkFactory;
            return this;
        }

        public Builder atlas(TextureAtlas atlas) {
            this.atlas = atlas;
            return this;
        }

        public Builder camera(PerspectiveCamera camera) {
            this.camera = camera;
            return this;
        }

        public Builder spawn(Vector3 spawn) {
            this.spawn = spawn;
            return this;
        }

        public VoxelEngine build() {
            require(registry, "block registry");
            require(chunkFactory, "chunk factory");
            require(atlas, "texture atlas");
            require(camera, "camera");
            return new VoxelEngine(this);
        }

        private static void require(Object value, String what) {
            if (value == null) {
                throw new IllegalStateException("missing " + what);
            }
        }
    }
}
