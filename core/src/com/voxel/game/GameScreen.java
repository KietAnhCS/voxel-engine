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
import com.voxel.game.net.Hurt;
import com.voxel.game.net.PlayerState;
import com.voxel.game.net.RemotePlayerRenderer;
import com.voxel.game.net.Session;
import com.voxel.game.net.WorldClient;
import com.voxel.game.play.Command;
import com.voxel.game.play.CommandConsole;
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
    /** Do mo cua man hinh "dang tao the gioi": 1 = che kin, giam dan ve 0 khi the gioi xong. */
    private float loadingAlpha = 1f;

    /**
     * Bang go loi F3 chi tinh lai chu {@link #DEBUG_REFRESH} giay mot lan: dung chuoi
     * String.format ~20 dong MOI KHUNG HINH chi de doc bang mat - do la rac GC vo ich.
     */
    private static final float DEBUG_REFRESH = 0.25f;
    private String[] debugLeft = new String[0];
    private String[] debugRight = new String[0];
    private float debugTimer;

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
        // Luon vao bang che do SINH TON, tui do trong - muon xay thoai mai thi go /gamemode 1.
        play.setMode(GameMode.SURVIVAL);
        connectChat();
        // Creeper no lam vo khoi: bao server tung o mot de nguoi choi khac cung thay ho bom.
        play.setExplosionListener(new com.voxel.game.mob.MonsterManager.ExplosionListener() {
            @Override
            public void blockDestroyed(int x, int y, int z) {
                worldClient.sendEdit(x, y, z, 0);
            }
        });
        // Giao dien duoc hoi truoc: khi tui do hay khung chat dang mo thi no giu lai
        // phim bam, khong cho lot xuong phan dieu khien nhan vat.
        Gdx.input.setInputProcessor(new InputMultiplexer(play, engine.controller()));
    }

    /**
     * Noi khung chat voi mang: dong go thuong (khong co "/") duoc gui cho ca phong va in
     * ngay len may minh kem ten; dong nguoi khac gui ve thi {@link #applyChatMessages} in ra.
     * Them lenh /list de xem ai dang online cung phong.
     */
    private void connectChat() {
        final CommandConsole console = play.console();
        console.setChatListener(new CommandConsole.ChatListener() {
            @Override
            public void chat(String message) {
                console.log("<" + session.username + "> " + message);
                worldClient.sendChat(message);
            }
        });
        console.register("list", new Command() {
            @Override
            public String run(String[] args) {
                StringBuilder online = new StringBuilder("Online: ").append(session.username);
                for (String name : worldClient.players().names()) {
                    online.append(", ").append(name);
                }
                return online.toString();
            }

            @Override
            public String usage() {
                return "/list  - see who is online on this map";
            }
        });
        console.register("tp", new Command() {
            @Override
            public String run(String[] args) {
                // /tp <x> <y> <z> - jump to a coordinate.
                if (args.length == 3) {
                    try {
                        float x = Float.parseFloat(args[0]);
                        float y = Float.parseFloat(args[1]);
                        float z = Float.parseFloat(args[2]);
                        engine.teleportPlayer(x, y, z);
                        return String.format("Teleported to %.0f %.0f %.0f", x, y, z);
                    } catch (NumberFormatException bad) {
                        return "Coordinates must be numbers: /tp <x> <y> <z>";
                    }
                }
                // /tp <player> - jump to another online player.
                if (args.length == 1) {
                    for (com.voxel.game.net.RemotePlayer other : worldClient.players().all()) {
                        if (other.name().equalsIgnoreCase(args[0])) {
                            com.badlogic.gdx.math.Vector3 feet = other.feet();
                            engine.teleportPlayer(feet.x, feet.y + 0.5f, feet.z);
                            return "Teleported to " + other.name();
                        }
                    }
                    return "No player named " + args[0] + " here - try /list";
                }
                return "Usage: /tp <x> <y> <z> or /tp <player>";
            }

            @Override
            public String usage() {
                return "/tp <x y z | player>  - teleport";
            }
        });
        console.log("Press T to chat with friends, /list shows who is online.");
    }

    @Override
    public void render(float delta) {
        applyRemoteEdits();
        applyIncomingHits();
        applyChatMessages();
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

    /** In cac dong chat / thong bao vao-ra vua nhan len khung chat (chay tren luong game). */
    private void applyChatMessages() {
        String line;
        while ((line = worldClient.pollChat()) != null) {
            play.console().log(line);
        }
    }

    /** Lay het cu danh nguoi khac vua giang vao minh va tru mau (chay tren luong game). */
    private void applyIncomingHits() {
        Hurt hurt;
        while ((hurt = worldClient.pollHurt()) != null) {
            play.hurtBy(hurt.from, hurt.damage);
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
        // Gui vi tri BAN CHAN: may khac dung so nay de dat nhan vat dung tren mat dat.
        float feetY = engine.playerFeetY();
        float yaw = yaw();
        float pitch = pitch();
        if (sentOnce && !movedSince(pos.x, feetY, pos.z, yaw, pitch)) {
            return;
        }

        worldClient.sendMove(pos.x, feetY, pos.z, yaw, pitch);
        lastSentX = pos.x;
        lastSentY = feetY;
        lastSentZ = pos.z;
        lastSentYaw = yaw;
        lastSentPitch = pitch;
        sentOnce = true;
    }

    /** True neu vi tri hoac huong nhin da lech qua {@link #MOVE_EPSILON} so voi lan gui truoc. */
    private boolean movedSince(float x, float y, float z, float yaw, float pitch) {
        return Math.abs(x - lastSentX) > MOVE_EPSILON
                || Math.abs(y - lastSentY) > MOVE_EPSILON
                || Math.abs(z - lastSentZ) > MOVE_EPSILON
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
        drawLoadingScreen(width, height);
        ui.end();
    }

    /**
     * Man hinh "Dang tao the gioi" che len tren cung khi moi vao game: nen toi, thanh tien
     * trinh va so PHAN TRAM chunk da dung xong. Khi nguoi choi duoc dat xuong dat thi mo dan
     * roi bien mat - vao game muot chu khong bi "rot bup" giua troi.
     */
    private void drawLoadingScreen(int width, int height) {
        if (engine.isReady()) {
            loadingAlpha -= Gdx.graphics.getDeltaTime() / 0.6f;
        }
        if (loadingAlpha <= 0f) {
            return;
        }
        float alpha = Math.min(1f, loadingAlpha);

        // Tong so chunk trong tam nhin; dem duoc bao nhieu chunk da nap la ra phan tram.
        int total = (2 * VIEW_DISTANCE + 1) * (2 * VIEW_DISTANCE + 1);
        float progress = engine.isReady() ? 1f
                : Math.min(1f, engine.loadedChunks() / (float) total);
        int percent = (int) (progress * 100f);

        ui.setColor(0.08f, 0.08f, 0.11f, alpha);
        ui.draw(play.pixel(), 0f, 0f, width, height);

        String title = "GENERATING WORLD...";
        layout.setText(font, title);
        font.setColor(1f, 1f, 1f, alpha);
        font.draw(ui, title, (width - layout.width) * 0.5f, height * 0.5f + 46f);

        float barWidth = Math.min(width * 0.45f, 520f);
        float barHeight = 16f;
        float barX = (width - barWidth) * 0.5f;
        float barY = height * 0.5f;

        // Vien trang mong, ruot toi, phan da xong to mau xanh kinh nghiem.
        ui.setColor(1f, 1f, 1f, alpha);
        ui.draw(play.pixel(), barX - 2f, barY - 2f, barWidth + 4f, barHeight + 4f);
        ui.setColor(0.05f, 0.05f, 0.07f, alpha);
        ui.draw(play.pixel(), barX, barY, barWidth, barHeight);
        ui.setColor(0.5f, 1f, 0.12f, alpha);
        ui.draw(play.pixel(), barX, barY, barWidth * progress, barHeight);
        ui.setColor(Color.WHITE);

        String text = percent + "%";
        layout.setText(font, text);
        font.setColor(1f, 1f, 1f, alpha);
        font.draw(ui, text, (width - layout.width) * 0.5f, barY - 12f);
        font.setColor(Color.WHITE);
    }

    /**
     * Bang go loi kieu F3 cua Minecraft: cot trai la thong tin the gioi, cot phai
     * la thong tin may. Moi dong co nen den mo cho de doc tren nen sang.
     */
    private void drawDebugText(int width, int height) {
        debugTimer -= Gdx.graphics.getDeltaTime();
        if (debugTimer <= 0f || debugLeft.length == 0) {
            debugTimer = DEBUG_REFRESH;
            rebuildDebugText(width, height);
        }
        drawDebugColumn(debugLeft, 6f, height - 6f, false, width);
        drawDebugColumn(debugRight, width - 6f, height - 6f, true, width);
    }

    private void rebuildDebugText(int width, int height) {
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
                        + "   queued " + engine.pendingMeshUpdates(),
                "triangles " + engine.visibleTriangles(),
                "",
                String.format("XYZ  %.3f / %.5f / %.3f", eye.x, eye.y, eye.z),
                "Block  " + blockX + " " + blockY + " " + blockZ,
                "Chunk  " + Math.floorDiv(blockX, 16) + " " + Math.floorDiv(blockZ, 16),
                "Facing  " + facing() + String.format("  (%.1f / %.1f)", yaw(), pitch()),
                "Biome  " + biomes.pick(blockX, blockZ),
                "Time  " + engine.dayCycle().clockLabel()
                        + (engine.dayCycle().isNight() ? "  (night - beware of monsters)" : "  (day)"),
                "State  " + engine.movementLabel() + "   View  " + engine.viewMode().label()
        };
        String[] right = {
                "Java " + System.getProperty("java.version"),
                "Memory  " + usedMb + " / " + maxMb + " MB",
                "Screen  " + width + "x" + height,
                "Mode  " + play.mode().label(),
                "",
                "F3 toggle this      F5 change view",
                "E open inventory    / open command",
                "T chat    ESC settings",
        };

        debugLeft = left;
        debugRight = right;
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
            return "South (+Z)";
        }
        if (angle < 135f) {
            return "West (-X)";
        }
        if (angle < 225f) {
            return "North (-Z)";
        }
        return "East (+X)";
    }

    private float yaw() {
        return (float) Math.toDegrees(Math.atan2(camera.direction.x, camera.direction.z));
    }

    private float pitch() {
        return (float) Math.toDegrees(Math.asin(-camera.direction.y));
    }

    /**
     * Chuot trai: uu tien danh quai vat / nguoi choi khac dang o trong tam ngam. Danh trung thi
     * bao ca phong biet de avatar cua minh ben may ho cung quo tay theo.
     */
    @Override
    public boolean onAttack(Vector3 origin, Vector3 direction, float reach) {
        boolean hitSomething = play.attack(origin, direction, reach, worldClient.players().all());
        if (hitSomething) {
            worldClient.sendSwing();
        }
        return hitSomething;
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
