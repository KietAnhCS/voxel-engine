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
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.input.BlockInteraction;
import com.voxel.engine.input.InteractionRequest;
import com.voxel.engine.input.PlayerController;
import com.voxel.engine.physics.MovementState;
import com.voxel.engine.physics.PhysicsWorld;
import com.voxel.engine.physics.PlayerBody;
import com.voxel.engine.physics.VoxelRaycaster;
import com.voxel.engine.physics.state.FlightState;
import com.voxel.engine.physics.state.GroundState;
import com.voxel.engine.render.DayNightCycle;
import com.voxel.engine.render.PlayerModel;
import com.voxel.engine.render.SkyRenderer;
import com.voxel.engine.render.ViewMode;
import com.voxel.engine.render.WorldRenderer;
import com.voxel.engine.world.Chunk;
import com.voxel.engine.world.ChunkFactory;
import com.voxel.engine.world.ChunkState;
import com.voxel.engine.world.FluidSimulator;
import com.voxel.engine.world.World;
import com.voxel.engine.world.WorldEventBus;

public final class VoxelEngine implements Disposable {

    private static final float FIXED_STEP = 1f / 60f;
    private static final int MAX_STEPS_PER_FRAME = 5;
    private static final float REACH = 6f;
    private static final float HEAD_BOB_AMPLITUDE = 0.045f;
    private static final float HEAD_BOB_FREQUENCY = 9.5f;
    /** Half height of the player capsule: radius 0.35 + half body 0.525. */
    private static final float PLAYER_HALF_HEIGHT = 0.875f;

    private final EngineConfig config;
    private final BlockRegistry registry;
    private final World world;
    private final WorldEventBus eventBus;
    private final PhysicsWorld physics;
    private final FluidSimulator fluids;
    private final PlayerBody player;
    private final WorldRenderer renderer;
    private final PlayerController controller;
    private final PerspectiveCamera camera;
    private final ModelBatch batch;
    private final ShaderProgram shaderProgram;
    private final Environment environment;
    private final VoxelRaycaster raycaster = new VoxelRaycaster();
    private final VoxelRaycaster.Hit hit = new VoxelRaycaster.Hit();
    private final VoxelRaycaster.Hit cameraHit = new VoxelRaycaster.Hit();
    private final PlayerModel playerModel = new PlayerModel();
    /** Dong ho ngay dem: quyet dinh do sang, mau troi va cho dung cua mat troi. */
    private final DayNightCycle dayCycle = new DayNightCycle();
    private final SkyRenderer sky = new SkyRenderer();
    /** Thoi tiet: mua hay tanh - keo theo suong mu, do sang va man mua. */
    private final com.voxel.engine.render.WeatherSystem weather = new com.voxel.engine.render.WeatherSystem();
    private final Vector3 orbit = new Vector3();
    private final Vector3 feet = new Vector3();
    /** Vi tri ban chan khung hinh truoc - de biet nguoi choi vua di duoc bao xa. */
    private final Vector3 previousFeet = new Vector3();
    private boolean feetTracked;
    private final Color skyColor = new Color(0.44f, 0.66f, 0.94f, 1f);
    private final Color waterColor = new Color(0.05f, 0.13f, 0.28f, 1f);
    private final ColorAttribute fogAttribute;
    private final Vector3 eye = new Vector3();
    private final Vector3 spawn = new Vector3();

    private MovementState movementState = new GroundState();
    private ViewMode viewMode = ViewMode.FIRST_PERSON;
    private BlockInteraction interaction;
    private float accumulator;
    private float elapsed;
    private float bobPhase;
    private boolean submerged;
    private boolean spawnSettled;
    /** True khi tai the gioi cu: giu nguyen vi tri da luu thay vi tim mat dat de tha xuong. */
    private final boolean restorePlacement;

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

        this.fluids = builder.waterLevels == null ? null : new FluidSimulator(world, builder.waterLevels);
        world.installFluids(fluids);

        // Nap lai cac o khoi da luu TRUOC khi sinh chunk dau tien, de the gioi da xay hien lai.
        for (int[] edit : builder.loadedEdits) {
            world.queueLoadedEdit(edit[0], edit[1], edit[2], (byte) edit[3]);
        }
        this.restorePlacement = builder.restorePlacement;

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
        dayCycle.advance(delta);
        weather.update(delta);
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
        updatePlayerModel(delta);
        processInteractions();
        renderer.update();
        drawWorld();
    }

    private void stepSimulation() {
        if (!spawnSettled && !settleSpawn()) {
            return;
        }

        Vector3 position = player.position();
        if (position.y < 1f) {
            player.teleport(spawn);
            position = player.position();
        }
        world.update(position);

        eye.set(position.x, position.y + PlayerBody.EYE_HEIGHT, position.z);
        submerged = world.isSubmerged(eye);
        player.setSubmerged(world.isSubmerged(position));
        // Rieng ban chan: dung de biet co con dam nuoc de nhun len bo khong.
        feet.set(position.x, position.y - PLAYER_HALF_HEIGHT + 0.1f, position.z);
        player.setFeetInWater(world.isSubmerged(feet));

        MovementState next = movementState.update(player, controller.input(), FIXED_STEP);
        if (next != movementState) {
            movementState = next;
            movementState.enter(player);
        }

        physics.step(FIXED_STEP);

        if (fluids != null) {
            fluids.tick();
        }

        if (controller.input().isMoving() && player.onGround()) {
            bobPhase += FIXED_STEP * HEAD_BOB_FREQUENCY;
        }
    }

    /**
     * Chunks are generated on another thread, so when the game starts there is no collision
     * shape yet. If the player were dropped right away they would fall through the ground and
     * get stuck inside it once the collision mesh appears. So: hold the player in place, wait
     * for the spawn chunk to finish meshing, then put them on top of the highest block.
     *
     * @return true once the player has been placed on the ground
     */
    private boolean settleSpawn() {
        world.update(spawn);
        player.teleport(spawn);

        int chunkX = Math.floorDiv((int) Math.floor(spawn.x), world.config().chunkSize());
        int chunkZ = Math.floorDiv((int) Math.floor(spawn.z), world.config().chunkSize());
        Chunk chunk = world.chunkAt(chunkX, chunkZ);
        if (chunk == null || chunk.state() != ChunkState.MESHED) {
            return false;
        }

        // Tai the gioi cu: cho chunk ghep xong (khoi va cham san sang) roi dat nguoi choi
        // dung vi tri da luu - KHONG tinh lai mat dat, neu khong se roi tot xuong noc nha da xay.
        if (restorePlacement) {
            player.teleport(spawn);
            spawnSettled = true;
            return true;
        }

        int blockX = (int) Math.floor(spawn.x);
        int blockZ = (int) Math.floor(spawn.z);
        int ground = highestSolid(blockX, blockZ);
        spawn.set(blockX + 0.5f, ground + 1f + PLAYER_HALF_HEIGHT + 0.05f, blockZ + 0.5f);

        player.teleport(spawn);
        spawnSettled = true;
        return true;
    }

    /** Height of the highest blocking block in this column. */
    private int highestSolid(int blockX, int blockZ) {
        for (int y = world.config().worldHeight() - 1; y > 0; y--) {
            Block block = world.blockAt(blockX, y, blockZ);
            if (!block.isAir() && block.isCollidable()) {
                return y;
            }
        }
        return world.config().seaLevel();
    }

    private void updateCamera(float delta) {
        // FOV doc song tu Settings: keo thanh truot la ong kinh zoom ngay sau tam bang.
        camera.fieldOfView = com.voxel.engine.GameSettings.get().fieldOfView();
        Vector3 position = player.position();
        float bob = controller.input().isMoving() && player.onGround()
                ? HEAD_BOB_AMPLITUDE * MathUtils.sin(bobPhase)
                : 0f;
        float eyeY = position.y + PlayerBody.EYE_HEIGHT + bob;

        if (viewMode == ViewMode.FIRST_PERSON) {
            camera.position.set(position.x, eyeY, position.z);
            camera.update(true);
            return;
        }

        // Third person: pull the camera back (or forward) along the view direction.
        // If a wall is in the way, stop in front of it so the camera does not go through.
        eye.set(position.x, eyeY, position.z);
        orbit.set(camera.direction).nor().scl(viewMode.facingPlayer() ? 1f : -1f);
        float distance = freeDistance(eye, orbit, viewMode.distance());
        camera.position.set(eye).mulAdd(orbit, distance);

        if (viewMode.facingPlayer()) {
            camera.direction.scl(-1f);
            camera.update(true);
            camera.direction.scl(-1f);
        } else {
            camera.update(true);
        }
    }

    /**
     * Measures how far a ray from the player's eyes along {@code direction} travels
     * before hitting a solid block. Returns that distance minus a small margin so the
     * camera does not sit flush against the wall.
     */
    private float freeDistance(Vector3 from, Vector3 direction, float wanted) {
        if (raycaster.cast(world, from, direction, wanted, cameraHit)) {
            float dx = cameraHit.blockX() + 0.5f - from.x;
            float dy = cameraHit.blockY() + 0.5f - from.y;
            float dz = cameraHit.blockZ() + 0.5f - from.z;
            float hitDistance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            return Math.max(0.4f, hitDistance - 0.4f);
        }
        return wanted;
    }

    /** Switches the view mode (F5 key): first person -> back -> front. */
    public ViewMode cycleViewMode() {
        viewMode = viewMode.next();
        return viewMode;
    }

    public ViewMode viewMode() {
        return viewMode;
    }

    /**
     * Places the player model in the right spot and swings the limbs with the walk cycle.
     *
     * Nhip tay chan tinh theo QUANG DUONG chan vua di duoc (khong phai theo phim bam), nen
     * di cham thi vung cham, bi day / truot tren bang cung vung dung, dung lai thi duoi thang.
     */
    private void updatePlayerModel(float delta) {
        Vector3 position = player.position();
        feet.set(position.x, position.y - PLAYER_HALF_HEIGHT, position.z);

        float dx = feet.x - previousFeet.x;
        float dz = feet.z - previousFeet.z;
        float distance = feetTracked ? (float) Math.sqrt(dx * dx + dz * dz) : 0f;
        previousFeet.set(feet);
        feetTracked = true;

        if (!viewMode.showsPlayer()) {
            return;
        }
        float yaw = MathUtils.atan2(camera.direction.x, camera.direction.z) * MathUtils.radiansToDegrees;
        playerModel.update(feet, yaw, distance, delta);
    }

    private void processInteractions() {
        InteractionRequest request = controller.pollRequest();
        while (request != null) {
            // Moi lan bam chuot la nhan vat quo tay phai ra tuong tac (thay ro o goc nhin thu 3).
            playerModel.swingArm();
            handleInteraction(request);
            request = controller.pollRequest();
        }
    }

    /**
     * Mot lan bam chuot. Chuot trai uu tien DANH sinh vat dang o trong tam ngam, khong trung
     * ai moi pha khoi - dung nhu Minecraft. Khoi da chan tam nhin thi sinh vat nap sau khoi do
     * cung khong an don, nen tam voi bi cat ngan bang khoang cach toi khoi.
     */
    private void handleInteraction(InteractionRequest request) {
        if (interaction == null) {
            return;
        }
        boolean blockInSight = raycaster.cast(world, camera.position, camera.direction, REACH, hit);

        if (request == InteractionRequest.BREAK) {
            float reach = blockInSight ? distanceTo(hit) : REACH;
            if (interaction.onAttack(camera.position, camera.direction, reach)) {
                return;
            }
        }
        if (!blockInSight) {
            return;
        }
        if (request == InteractionRequest.BREAK) {
            interaction.onBreak(world, hit);
        } else {
            interaction.onPlace(world, hit, request == InteractionRequest.PLACE_ALTERNATE);
        }
    }

    /** Khoang cach tu mat nguoi choi toi giua o khoi dang ngam. */
    private float distanceTo(VoxelRaycaster.Hit target) {
        float dx = target.blockX() + 0.5f - camera.position.x;
        float dy = target.blockY() + 0.5f - camera.position.y;
        float dz = target.blockZ() + 0.5f - camera.position.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void drawWorld() {
        // Mau nen VA mau suong mu deu lay tu dong ho ngay dem: sang xanh, chieu cam, dem
        // tham - troi mua thi pha them xam ({@link WeatherSystem#blendSky}).
        skyColor.set(weather.blendSky(dayCycle.skyColor()));
        Color background = submerged ? waterColor : skyColor;
        fogAttribute.color.set(background);

        Gdx.gl.glClearColor(background.r, background.g, background.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);

        // Mat troi / mat trang / may ve TRUOC TIEN nhu tam phong nen: moi khoi dat da ve sau
        // deu che len tren, nen mat troi luon nam sau nui chu khong de len khoi nao.
        if (!submerged) {
            sky.render(camera, dayCycle, elapsed, weather.rain());
        }

        shaderProgram.bind();
        // Suong mu: duoi nuoc dac nhat; troi mua thi day dan len theo con mua.
        float fog = submerged ? 0.11f : 0.028f * weather.fogFactor();
        shaderProgram.setUniformf("u_fogstr", fog);
        shaderProgram.setUniformf("u_time", elapsed);
        shaderProgram.setUniformf("u_daylight", dayCycle.daylight() * weather.daylightFactor());

        batch.begin(camera);
        batch.render(renderer.solidPass(), environment);
        batch.end();

        if (viewMode.showsPlayer()) {
            playerModel.render(camera);
        }

        Gdx.gl.glDepthMask(false);
        batch.begin(camera);
        batch.render(renderer.translucentPass(), environment);
        batch.end();
        Gdx.gl.glDepthMask(true);

        // Man mua ve sau cung: giot mua khuat sau doi nui nho bo dem do sau cua the gioi.
        if (!submerged) {
            sky.renderRain(camera, elapsed, weather.rain());
        }
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

    /** True khi nguoi choi da duoc dat xuong dat - luc do man hinh "dang tao the gioi" tat di. */
    public boolean isReady() {
        return spawnSettled;
    }

    public PlayerController controller() {
        return controller;
    }

    /** Dong ho ngay dem - phan luat choi hoi de biet dem chua (quai vat) va de hien gio. */
    public DayNightCycle dayCycle() {
        return dayCycle;
    }

    /** Thoi tiet hien tai - lenh /weather doi qua day. */
    public com.voxel.engine.render.WeatherSystem weather() {
        return weather;
    }

    /** Dich chuyen tuc thoi toi mot vi tri (toa do BAN CHAN) - dung cho lenh /tp. */
    public void teleportPlayer(float x, float feetY, float z) {
        eye.set(x, feetY + PLAYER_HALF_HEIGHT + 0.1f, z);
        player.teleport(eye);
        player.clearVelocity();
    }

    /** Do sang bau troi tai o khoi nay: dung de biet cho do co du toi cho quai vat sinh ra khong. */
    public int skyLightAt(int blockX, int blockY, int blockZ) {
        return world.skyLightAt(blockX, blockY, blockZ);
    }

    /** Do sang cua duoc / den tai o khoi nay. */
    public int blockLightAt(int blockX, int blockY, int blockZ) {
        return world.blockLightAt(blockX, blockY, blockZ);
    }

    /** Position of the player's feet. Used to compute fall damage. */
    public Vector3 playerPosition() {
        return player.position();
    }

    /**
     * Cao do BAN CHAN nguoi choi. {@link #playerPosition()} tra ve tam hinh va cham (cao hon
     * ban chan nua than nguoi), nen khi gui vi tri cho may khac ve avatar phai dung so nay -
     * neu khong nhan vat cua minh se lo lung tren khong ben man hinh ho.
     */
    public float playerFeetY() {
        return player.position().y - PLAYER_HALF_HEIGHT;
    }

    public boolean playerOnGround() {
        return player.onGround();
    }

    /** Is the player walking - used to compute how fast hunger drains. */
    public boolean isPlayerMoving() {
        return controller.input().isMoving();
    }

    /** Is the player's body submerged in water (used to cancel fall damage). */
    public boolean playerInWater() {
        return player.isSubmerged();
    }

    /** Returns the player to the spawn point (after dying). */
    public void respawnPlayer() {
        player.teleport(spawn);
    }

    /**
     * Forbids flight in survival mode. If the player is already flying, drop them
     * back to the ground at once, otherwise they would hang in mid-air forever.
     */
    public void setFlightAllowed(boolean allowed) {
        controller.setFlightAllowed(allowed);
        if (!allowed && movementState instanceof FlightState) {
            movementState = new GroundState();
            movementState.enter(player);
        }
    }

    @Override
    public void dispose() {
        playerModel.dispose();
        sky.dispose();
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
        private Block[] waterLevels;
        private final java.util.List<int[]> loadedEdits = new java.util.ArrayList<int[]>();
        private boolean restorePlacement;

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

        /** Them mot o khoi da luu ({x, y, z, blockId}) can dat lai khi the gioi sinh ra. */
        public Builder loadedEdit(int worldX, int worldY, int worldZ, int blockId) {
            this.loadedEdits.add(new int[]{worldX, worldY, worldZ, blockId});
            return this;
        }

        /** Bat che do tai the gioi cu: giu nguyen vi tri {@link #spawn} thay vi tim mat dat. */
        public Builder restorePlacement(boolean restorePlacement) {
            this.restorePlacement = restorePlacement;
            return this;
        }

        /**
         * Enables flowing water simulation. Omit it and water stays still like a normal block.
         *
         * @param levels block table by water level: [0] is air, [8] is the source block
         */
        public Builder water(Block[] levels) {
            this.waterLevels = levels;
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
