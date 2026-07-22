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
import com.voxel.game.play.PlaySession;
import com.voxel.game.terrain.OverworldChunkFactory;
import com.voxel.game.terrain.TerrainNoise;
import com.voxel.game.terrain.biome.BiomeSource;

public final class GameScreen extends ScreenAdapter implements BlockInteraction {

    private static final int WORLD_HEIGHT = 128;
    private static final int SEA_LEVEL = 48;
    private static final int VIEW_DISTANCE = 7;
    private static final float MIN_PLACEMENT_DISTANCE = 1.6f;

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

    @Override
    public void show() {
        long seed = System.nanoTime();

        camera = new PerspectiveCamera(72f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = VIEW_DISTANCE * 16f + 48f;
        camera.direction.set(0f, 0f, 1f);
        camera.up.set(Vector3.Y);
        camera.update();

        atlas = new TextureAtlas(Gdx.files.internal("data/textureatlas.atlas"));
        registry = new BlockRegistry();
        blocks = new Blocks(registry);

        TerrainNoise noise = new TerrainNoise(seed, SEA_LEVEL);
        biomes = new BiomeSource(blocks, noise);

        OverworldChunkFactory chunkFactory = new OverworldChunkFactory(blocks, biomes, noise, WORLD_HEIGHT);
        Vector3 spawn = chunkFactory.spawnPoint(0, 0);

        engine = VoxelEngine.builder()
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
                .water(blocks.waterLevels())
                .build();
        engine.setInteraction(this);

        ui = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        crosshair = new Texture(Gdx.files.internal("data/crosshair.png"));

        play = new PlaySession(engine, blocks, atlas, font);
        // Giao dien duoc hoi truoc: khi tui do hay khung chat dang mo thi no giu lai
        // phim bam, khong cho lot xuong phan dieu khien nhan vat.
        Gdx.input.setInputProcessor(new InputMultiplexer(play, engine.controller()));
    }

    @Override
    public void render(float delta) {
        engine.render(delta);
        play.update(delta);
        drawOverlay();
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
        world.setBlock(hit.blockX(), hit.blockY(), hit.blockZ(), blocks.air);
        play.onBreak(broken);
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
        if (block != null) {
            world.setBlock(x, y, z, block);
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
        play.dispose();
        engine.dispose();
        atlas.dispose();
        ui.dispose();
        font.dispose();
        crosshair.dispose();
    }
}
