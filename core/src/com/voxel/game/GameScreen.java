package com.voxel.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.VoxelEngine;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.input.BlockInteraction;
import com.voxel.engine.physics.VoxelRaycaster;
import com.voxel.engine.world.World;
import com.voxel.game.net.Edit;
import com.voxel.game.net.PlayerState;
import com.voxel.game.net.RemotePlayerRenderer;
import com.voxel.game.net.Session;
import com.voxel.game.net.WorldClient;
import com.voxel.game.play.GameMode;
import com.voxel.game.play.PlaySession;
import com.voxel.game.terrain.OverworldChunkFactory;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.biome.BiomeSource;

public final class GameScreen extends ScreenAdapter implements BlockInteraction {

    private static final int WORLD_HEIGHT = 128;
    private static final int SEA_LEVEL = 48;
    private static final int VIEW_DISTANCE = 7;
    private static final float MIN_PLACEMENT_DISTANCE = 1.6f;
    /** Gui vi tri nguoi choi len server 15 lan/giay - du muot ma khong nghen mang. */
    private static final float MOVE_SEND_INTERVAL = 1f / 15f;

    /** Chi gui vi tri khi da nhuc nhich qua bay nhieu - dung phi mang luc dung yen. */
    private static final float MOVE_EPSILON = 0.02f;

    private final Session session;
    private WorldClient worldClient;
    private RemotePlayerRenderer remoteRenderer;
    private float moveTimer;
    private float lastSentX, lastSentY, lastSentZ, lastSentYaw, lastSentPitch;
    private boolean sentOnce;

    private PerspectiveCamera camera;
    private TextureAtlas atlas;
    private BlockRegistry registry;
    private Blocks blocks;
    private VoxelEngine engine;
    private BiomeSource biomes;
    private PlaySession play;
    private SpriteBatch ui;
    private BitmapFont font;
    private Texture crosshair;
    private final GlyphLayout layout = new GlyphLayout();

    public GameScreen(Session session) {
        this.session = session;
    }

    @Override
    public void show() {
        // Hat giong lay tu the gioi da luu tren server, KHONG ngau nhien: dia hinh sinh lai y het.
        long seed = session.world.seed;
        PlayerState saved = session.world.player;
        boolean fresh = saved.isFresh();

        camera = new PerspectiveCamera(72f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = VIEW_DISTANCE * 16f + 48f;
        camera.up.set(Vector3.Y);
        if (fresh) {
            camera.direction.set(0f, 0f, 1f);
        } else {
            aimCameraFrom(saved.yaw, saved.pitch);
        }
        camera.update();

        atlas = new TextureAtlas(Gdx.files.internal("data/textureatlas.atlas"));
        registry = new BlockRegistry();
        blocks = new Blocks(registry);

        TerrainNoise noise = new TerrainNoise(seed, SEA_LEVEL);
        biomes = new BiomeSource(blocks, noise);

        OverworldChunkFactory chunkFactory = new OverworldChunkFactory(blocks, biomes, noise, WORLD_HEIGHT);
        // The gioi moi: rot xuong diem xuat phat va tim mat dat. The gioi cu: dat dung vi tri da luu.
        // VOXEL_SPAWN_X/Z cho phep dat diem xuat phat khac nhau (dung khi demo nhieu nguoi 1 map).
        Vector3 spawn = fresh ? chunkFactory.spawnPoint(intEnv("VOXEL_SPAWN_X", 0), intEnv("VOXEL_SPAWN_Z", 0))
                : new Vector3(saved.x, saved.y, saved.z);

        VoxelEngine.Builder builder = VoxelEngine.builder()
                .chunkSize(16)
                .worldHeight(WORLD_HEIGHT)
                .viewDistance(VIEW_DISTANCE)
                .seaLevel(SEA_LEVEL)
                .seed(seed)
                .blocks(registry)
                .chunks(chunkFactory)
                .atlas(atlas)
                .camera(camera)
                .spawn(spawn)
                .restorePlacement(!fresh)
                .water(blocks.waterLevels());

        // Nap tung o khoi da xay vao engine truoc khi sinh chunk.
        for (Edit edit : session.world.edits) {
            builder.loadedEdit(edit.x, edit.y, edit.z, edit.blockId);
        }

        engine = builder.build();
        engine.setInteraction(this);

        // Mo ket noi the gioi chung: tu day thay nguoi choi khac va o khoi ho dat/pha theo thoi gian thuc.
        worldClient = new WorldClient(session);
        worldClient.connect();
        remoteRenderer = new RemotePlayerRenderer();

        ui = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        crosshair = new Texture(Gdx.files.internal("data/crosshair.png"));

        play = new PlaySession(engine, blocks, atlas, font);
        // Khoi phuc che do choi da luu (0 = sinh ton, 1 = sang tao).
        play.setMode(saved.mode == 0 ? GameMode.SURVIVAL : GameMode.CREATIVE);
        // Giao dien duoc hoi truoc: khi tui do hay khung chat dang mo thi no giu lai
        // phim bam, khong cho lot xuong phan dieu khien nhan vat.
        Gdx.input.setInputProcessor(new InputMultiplexer(play, engine.controller()));
    }

    @Override
    public void render(float delta) {
        applyRemoteEdits();
        engine.render(delta);
        // Ve avatar nguoi choi khac SAU khi engine ve xong the gioi (dung chung camera va bo dem do sau).
        worldClient.players().advance(delta);
        remoteRenderer.render(camera, worldClient.players());
        sendLocalState(delta);
        play.update(delta);
        // Quai vat ve chung camera va bo dem do sau voi the gioi (sau engine, truoc giao dien 2D).
        play.renderMonsters(camera);
        drawOverlay();
    }

    /** Lay het o khoi nguoi khac vua sua tu hang doi va ap vao the gioi (chay tren luong game). */
    private void applyRemoteEdits() {
        int[] edit;
        while ((edit = worldClient.pollRemoteEdit()) != null) {
            engine.world().applyRemoteEdit(edit[0], edit[1], edit[2], registry.byId((byte) edit[3]));
        }
    }

    /**
     * Gui vi tri + huong nhin cua nguoi choi nay len server, toi da {@link #MOVE_SEND_INTERVAL}
     * mot lan VA chi khi that su co thay doi - dung yen thi khong gui gi de do ton mang.
     */
    private void sendLocalState(float delta) {
        moveTimer += delta;
        if (moveTimer < MOVE_SEND_INTERVAL) {
            return;
        }
        moveTimer = 0f;

        Vector3 pos = engine.playerPosition();
        float yaw = yaw();
        float pitch = pitch();
        if (sentOnce && !movedSince(pos, yaw, pitch)) {
            return;
        }

        worldClient.sendMove(pos.x, pos.y, pos.z, yaw, pitch);
        lastSentX = pos.x;
        lastSentY = pos.y;
        lastSentZ = pos.z;
        lastSentYaw = yaw;
        lastSentPitch = pitch;
        sentOnce = true;
    }

    /** True neu vi tri hoac huong nhin da lech qua {@link #MOVE_EPSILON} so voi lan gui truoc. */
    private boolean movedSince(Vector3 pos, float yaw, float pitch) {
        return Math.abs(pos.x - lastSentX) > MOVE_EPSILON
                || Math.abs(pos.y - lastSentY) > MOVE_EPSILON
                || Math.abs(pos.z - lastSentZ) > MOVE_EPSILON
                || Math.abs(yaw - lastSentYaw) > MOVE_EPSILON
                || Math.abs(pitch - lastSentPitch) > MOVE_EPSILON;
    }

    /** Doc mot bien moi truong so nguyen, tra ve {@code fallback} neu khong co / khong hop le. */
    private static int intEnv(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    /** Quay camera theo yaw/pitch da luu (do) - dao nguoc phep tinh trong {@link #yaw}/{@link #pitch}. */
    private void aimCameraFrom(float yawDegrees, float pitchDegrees) {
        double yawRad = Math.toRadians(yawDegrees);
        double pitchRad = Math.toRadians(pitchDegrees);
        float horizontal = (float) Math.cos(pitchRad);
        camera.direction.set(
                (float) Math.sin(yawRad) * horizontal,
                (float) -Math.sin(pitchRad),
                (float) Math.cos(yawRad) * horizontal).nor();
    }

    private void drawOverlay() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        ui.getProjectionMatrix().setToOrtho2D(0f, 0f, width, height);
        ui.begin();

        if (play.isDebugVisible()) {
            drawDebugText(width, height);
        }

        if (play.showCrosshair()) {
            ui.draw(crosshair,
                    (width - crosshair.getWidth()) * 0.5f,
                    (height - crosshair.getHeight()) * 0.5f);
        }

        play.draw(ui, width, height);
        ui.end();
    }

    /**
     * Bang go loi kieu F3 cua Minecraft: cot trai la thong tin the gioi, cot phai
     * la thong tin may. Moi dong co nen den mo cho de doc tren nen sang.
     */
    private void drawDebugText(int width, int height) {
        Vector3 eye = camera.position;
        int blockX = (int) Math.floor(eye.x);
        int blockY = (int) Math.floor(eye.y);
        int blockZ = (int) Math.floor(eye.z);
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) >> 20;
        long maxMb = runtime.maxMemory() >> 20;

        String[] left = {
                "Voxel Engine  " + Gdx.graphics.getFramesPerSecond() + " fps",
                "chunks " + engine.visibleChunks() + "/" + engine.loadedChunks()
                        + "   sections " + engine.visibleSections()
                        + "   cho ghep " + engine.pendingMeshUpdates(),
                "tam giac " + engine.visibleTriangles(),
                "",
                String.format("XYZ  %.3f / %.5f / %.3f", eye.x, eye.y, eye.z),
                "Khoi  " + blockX + " " + blockY + " " + blockZ,
                "Chunk  " + Math.floorDiv(blockX, 16) + " " + Math.floorDiv(blockZ, 16),
                "Huong  " + facing() + String.format("  (%.1f / %.1f)", yaw(), pitch()),
                "Biome  " + biomes.pick(blockX, blockZ),
                "Trang thai  " + engine.movementLabel() + "   Goc nhin  " + engine.viewMode().label()
        };
        String[] right = {
                "Java " + System.getProperty("java.version"),
                "Bo nho  " + usedMb + " / " + maxMb + " MB",
                "Man hinh  " + width + "x" + height,
                "Che do  " + play.mode().label(),
                "",
                "F3 an bang nay      F5 doi goc nhin",
                "E tui do            / go lenh",
                "1-9 hoac lan chuot doi khoi",
                "chuot trai pha      chuot phai dat",
                "CTRL+phai dat duoc  Q thoat"
        };

        drawDebugColumn(left, 6f, height - 6f, false, width);
        drawDebugColumn(right, width - 6f, height - 6f, true, width);
    }

    private void drawDebugColumn(String[] lines, float x, float top, boolean alignRight, int width) {
        float y = top;
        for (String line : lines) {
            if (!line.isEmpty()) {
                layout.setText(font, line);
                float textX = alignRight ? x - layout.width : x;
                ui.setColor(0f, 0f, 0f, 0.45f);
                ui.draw(play.pixel(), textX - 2f, y - layout.height - 4f, layout.width + 4f, layout.height + 6f);
                ui.setColor(Color.WHITE);
                font.setColor(Color.WHITE);
                font.draw(ui, line, textX, y);
            }
            y -= 20f;
        }
    }

    /** Huong dang nhin, goi ten nhu Minecraft. */
    private String facing() {
        float angle = (yaw() % 360f + 360f) % 360f;
        if (angle >= 315f || angle < 45f) {
            return "nam (+Z)";
        }
        if (angle < 135f) {
            return "tay (-X)";
        }
        if (angle < 225f) {
            return "bac (-Z)";
        }
        return "dong (+X)";
    }

    private float yaw() {
        return (float) Math.toDegrees(Math.atan2(camera.direction.x, camera.direction.z));
    }

    private float pitch() {
        return (float) Math.toDegrees(Math.asin(-camera.direction.y));
    }

    @Override
    public void onBreak(World world, VoxelRaycaster.Hit hit) {
        Block broken = world.blockAt(hit.blockX(), hit.blockY(), hit.blockZ());
        if (world.setBlock(hit.blockX(), hit.blockY(), hit.blockZ(), blocks.air)) {
            // Bao server o vua bi pha (blockId 0 = khong khi) de luu va phat cho nguoi choi khac.
            worldClient.sendEdit(hit.blockX(), hit.blockY(), hit.blockZ(), 0);
            play.onBreak(broken);
        }
    }

    @Override
    public void onPlace(World world, VoxelRaycaster.Hit hit, boolean alternate) {
        int x = hit.adjacentX();
        int y = hit.adjacentY();
        int z = hit.adjacentZ();

        float dx = x + 0.5f - camera.position.x;
        float dy = y + 0.5f - camera.position.y;
        float dz = z + 0.5f - camera.position.z;
        if (dx * dx + dy * dy + dz * dz < MIN_PLACEMENT_DISTANCE * MIN_PLACEMENT_DISTANCE) {
            return;
        }

        // Hoi tui do TRUOC khi dat: o che do sinh ton thao tac nay bot mot khoi,
        // nen chi duoc goi khi cho dat da hop le.
        Block block = play.blockToPlace(alternate);
        if (block != null && world.setBlock(x, y, z, block)) {
            // Bao server o vua dat de luu va phat cho nguoi choi khac.
            worldClient.sendEdit(x, y, z, block.id() & 0xFF);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (width == 0 || height == 0) {
            return;
        }
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update(true);
    }

    @Override
    public void dispose() {
        // Bao server minh thoat de cac may khac bo avatar cua minh.
        worldClient.close();

        remoteRenderer.dispose();
        play.dispose();
        engine.dispose();
        atlas.dispose();
        ui.dispose();
        font.dispose();
        crosshair.dispose();
    }
}
