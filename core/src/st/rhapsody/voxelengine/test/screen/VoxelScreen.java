package st.rhapsody.voxelengine.test.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectSet;
import st.rhapsody.voxelengine.lib.VoxelEngine;
import st.rhapsody.voxelengine.test.terrain.biome.BiomeProvider;
import st.rhapsody.voxelengine.test.terrain.block.BlockProvider;
import st.rhapsody.voxelengine.test.terrain.chunk.ChunkProvider;

import java.nio.FloatBuffer;

public class VoxelScreen implements Screen {
    private PerspectiveCamera camera;
    private VoxelEngine voxelEngine;
    private TextureAtlas textureAtlas;

    private SpriteBatch spriteBatch;
    private BitmapFont font;

    @Override
    public void render(float delta) {
        clearOpenGL();

        voxelEngine.render(delta);

        if (spriteBatch != null && font != null) {
            spriteBatch.begin();
            String coords = String.format("X: %.2f  Y: %.2f  Z: %.2f",
                    camera.position.x, camera.position.y, camera.position.z);

            font.draw(spriteBatch, coords, 10, Gdx.graphics.getHeight() - 10);
            spriteBatch.end();
        }
    }

    private void clearOpenGL() {
        Vector3 direction = camera.direction;
        float v = Math.abs(MathUtils.atan2(direction.x, direction.z) * MathUtils.radiansToDegrees);
        v = Math.min(90, v / 2);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glClearColor((v / 2) / 255f, (v / 1) / 255f, ((255 - v) - 50) / 255f, 1);
        voxelEngine.setSkyColor((v / 2) / 255f, (v / 1) / 255f, ((255 - v) - 50) / 255f, 1);
    }

    @Override
    public void resize(int width, int height) {
        createCamera(width, height);
        setup(width, height);
    }

    private void setup(int width, int height) {
        BlockProvider blockProvider = new BlockProvider();
        BiomeProvider biomeProvider = new BiomeProvider();
        ChunkProvider chunkProvider = new ChunkProvider(blockProvider, biomeProvider);

        textureAtlas = new TextureAtlas(Gdx.files.internal("data/textureatlas.atlas"));
        voxelEngine = new VoxelEngine(camera, blockProvider, chunkProvider, biomeProvider, textureAtlas);
        enableAnisotropy();

        if (spriteBatch == null) {
            spriteBatch = new SpriteBatch();
            font = new BitmapFont();
        }
        spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    private void enableAnisotropy() {
        FloatBuffer buffer = BufferUtils.newFloatBuffer(64);
        if (Gdx.gl20 != null) {
            Gdx.gl20.glGetFloatv(GL20.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer);
        } else {
            throw new GdxRuntimeException("GL20 not available");
        }

        float maxAnisotropy = buffer.get(0);
        ObjectSet<Texture> textures = textureAtlas.getTextures();
        for (Texture tex : textures) {
            tex.bind();
            Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_LEVEL, 4);
        }
    }

    private void createCamera(int width, int height) {
        camera = new PerspectiveCamera(70f, width, height);
        camera.near = 0.1f;
        camera.far = 200;
        camera.position.set(0, 110, 0);
        camera.lookAt(0, 100, 1);
        camera.update();
    }

    @Override
    public void show() {}

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        voxelEngine.dispose();
        textureAtlas.dispose();

        if (spriteBatch != null) spriteBatch.dispose();
        if (font != null) font.dispose();
    }
}