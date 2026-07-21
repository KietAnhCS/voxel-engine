package com.voxel.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.VoxelEngine;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.input.BlockInteraction;
import com.voxel.engine.physics.VoxelRaycaster;
import com.voxel.engine.world.World;
import com.voxel.game.terrain.OverworldChunkFactory;

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
    private SpriteBatch ui;
    private BitmapFont font;
    private Texture crosshair;

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

        OverworldChunkFactory chunkFactory = new OverworldChunkFactory(blocks, seed, SEA_LEVEL, WORLD_HEIGHT);
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
                .build();
        engine.setInteraction(this);

        ui = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        crosshair = new Texture(Gdx.files.internal("data/crosshair.png"));
    }

    @Override
    public void render(float delta) {
        engine.render(delta);
        drawOverlay();
    }

    private void drawOverlay() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        ui.getProjectionMatrix().setToOrtho2D(0f, 0f, width, height);
        ui.begin();

        font.draw(ui, "fps " + Gdx.graphics.getFramesPerSecond()
                + "   chunks " + engine.visibleChunks() + "/" + engine.loadedChunks()
                + "   sections " + engine.visibleSections()
                + "   triangles " + engine.visibleTriangles()
                + "   queued " + engine.pendingMeshUpdates(), 12f, height - 12f);

        font.draw(ui, String.format("x %.1f  y %.1f  z %.1f   state %s",
                camera.position.x, camera.position.y, camera.position.z,
                engine.movementLabel()), 12f, height - 32f);

        font.draw(ui, "WASD move   SPACE jump   SPACE x2 fly   LMB break   RMB lamp   CTRL+RMB brick   F fullscreen   Q quit",
                12f, 24f);

        ui.draw(crosshair,
                (width - crosshair.getWidth()) * 0.5f,
                (height - crosshair.getHeight()) * 0.5f);

        ui.end();
    }

    @Override
    public void onBreak(World world, VoxelRaycaster.Hit hit) {
        world.setBlock(hit.blockX(), hit.blockY(), hit.blockZ(), blocks.air);
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

        world.setBlock(x, y, z, alternate ? blocks.brick : blocks.lamp);
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
        engine.dispose();
        atlas.dispose();
        ui.dispose();
        font.dispose();
        crosshair.dispose();
    }
}
