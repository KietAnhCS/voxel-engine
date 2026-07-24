package com.voxel.engine.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import java.util.Random;

/**
 * Bau troi: MAT TROI, MAT TRANG va lop MAY.
 *
 * <p>Mat troi/mat trang la mot tam vuong luon quay mat ve phia nguoi choi (billboard), dat
 * o xa theo huong ma {@link DayNightCycle} chi ra - nen chung moc dang dong, lan dang tay
 * theo dung chu ky ngay dem. Cai nao khuat duoi duong chan troi thi mo dan roi tat han.
 *
 * <p>May la mot tam luoi phang o cao {@link #CLOUD_Y}, luon dat quanh nguoi choi nhung
 * TOA DO ANH duoc tinh tu vi tri THAT trong the gioi, nen may dung yen tai cho khi nguoi
 * choi di lai (co cam giac troi that) chu khong dinh theo dau nguoi. Ria tam luoi mo dan
 * ve 0 de khong lo ra mot canh thang tap giua troi.
 *
 * <p>Tat ca ve bang mot shader nho tu viet: chi co vi tri, anh va mau - khong dinh gi toi
 * anh sang hay suong mu cua the gioi khoi.
 */
public final class SkyRenderer implements Disposable {

    /** Do cao lop may (the gioi cao 128 nen may nam ngay tren dinh nui). */
    private static final float CLOUD_Y = 122f;
    /** Be rong tam may (khoi). */
    private static final float CLOUD_SPAN = 320f;
    /** So o chia moi chieu cua tam may - can du day de ria mo dan cho muot. */
    private static final int CLOUD_CELLS = 16;
    /** Bao nhieu khoi thi anh may lap lai mot lan. */
    private static final float CLOUD_TILE = 48f;
    /** May troi bao nhieu khoi moi giay. */
    private static final float CLOUD_SPEED = 0.6f;

    /**
     * Mat troi/mat trang dat cach nguoi choi bao nhieu phan tam nhin. De sat mep tam nhin
     * cho giong o vo cung xa, nhung van trong tam de khong bi cat mat.
     */
    private static final float SKY_DISTANCE_RATIO = 0.8f;
    /** Nua canh o vuong mat troi / mat trang, tinh theo phan tram khoang cach tren. */
    private static final float SUN_SIZE_RATIO = 0.075f;
    private static final float MOON_SIZE_RATIO = 0.05f;

    private static final int FLOATS_PER_VERTEX = 9;  // vi tri 3 + uv 2 + mau 4

    private final ShaderProgram shader;
    private final Mesh billboard;
    private final Mesh clouds;
    private final Texture sunTexture;
    private final Texture moonTexture;
    private final Texture cloudTexture;

    private final float[] quadVertices = new float[4 * FLOATS_PER_VERTEX];
    private final Vector3 centre = new Vector3();
    private final Vector3 right = new Vector3();
    private final Vector3 up = new Vector3();
    private final Vector3 moonDirection = new Vector3();
    private final Color tint = new Color();

    public SkyRenderer() {
        shader = compile();
        // Mat troi va mat trang la o VUONG dac nhu Minecraft, khong phai dia tron.
        sunTexture = square(16, new Color(1f, 1f, 0.92f, 1f), new Color(1f, 0.86f, 0.35f, 1f));
        moonTexture = square(16, new Color(1f, 1f, 1f, 1f), new Color(0.82f, 0.86f, 0.96f, 1f));
        cloudTexture = cloudPuffs(64);
        billboard = buildBillboard();
        clouds = buildClouds();
    }

    /**
     * Ve bau troi. Goi NGAY SAU khi xoa man hinh va TRUOC khi ve the gioi khoi.
     *
     * <p>Ca lop nay tat kiem tra do sau va khong ghi do sau: no chi la tam phong nen o vo
     * cung xa. Nho vay moi khoi dat da ve sau deu che len tren - mat troi luon nam SAU nui
     * chu khong bao gio de len khoi hay lo lung truoc mat nguoi choi.
     */
    public void render(PerspectiveCamera camera, DayNightCycle cycle, float elapsed) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);

        shader.bind();
        shader.setUniformMatrix("u_projViewTrans", camera.combined);
        shader.setUniformi("u_texture", 0);

        Vector3 sun = cycle.sunDirection();
        moonDirection.set(sun).scl(-1f);
        float distance = camera.far * SKY_DISTANCE_RATIO;

        // Cai nao dang o duoi chan troi thi mo dan roi tat.
        drawBillboard(camera, sunTexture, sun, distance, distance * SUN_SIZE_RATIO,
                horizonFade(sun.y), 1f, 1f, 1f);
        drawBillboard(camera, moonTexture, moonDirection, distance, distance * MOON_SIZE_RATIO,
                horizonFade(moonDirection.y), 0.9f, 0.93f, 1f);
        drawClouds(camera, cycle, elapsed);

        Gdx.gl.glDepthMask(true);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Sat duong chan troi thi mo di, khuat han thi bien mat. */
    private static float horizonFade(float height) {
        return Math.max(0f, Math.min(1f, (height + 0.08f) * 8f));
    }

    /** Dung mot tam vuong quay mat ve nguoi choi, o xa theo huong {@code direction}. */
    private void drawBillboard(PerspectiveCamera camera, Texture texture, Vector3 direction,
                               float distance, float size, float alpha,
                               float red, float green, float blue) {
        if (alpha <= 0.01f) {
            return;
        }
        centre.set(direction).scl(distance).add(camera.position);
        right.set(direction).crs(Vector3.Y).nor().scl(size);
        up.set(right).crs(direction).nor().scl(size);

        putCorner(0, -1f, -1f, 0f, 1f, red, green, blue, alpha);
        putCorner(1, 1f, -1f, 1f, 1f, red, green, blue, alpha);
        putCorner(2, 1f, 1f, 1f, 0f, red, green, blue, alpha);
        putCorner(3, -1f, 1f, 0f, 0f, red, green, blue, alpha);

        billboard.setVertices(quadVertices);
        shader.setUniformf("u_offset", 0f, 0f, 0f);
        shader.setUniformf("u_uvFromWorld", 0f);
        shader.setUniformf("u_scroll", 0f, 0f);
        texture.bind(0);
        billboard.render(shader, GL20.GL_TRIANGLES);
    }

    private void putCorner(int corner, float uSign, float vSign, float u, float v,
                           float red, float green, float blue, float alpha) {
        int at = corner * FLOATS_PER_VERTEX;
        quadVertices[at] = centre.x + right.x * uSign + up.x * vSign;
        quadVertices[at + 1] = centre.y + right.y * uSign + up.y * vSign;
        quadVertices[at + 2] = centre.z + right.z * uSign + up.z * vSign;
        quadVertices[at + 3] = u;
        quadVertices[at + 4] = v;
        quadVertices[at + 5] = red;
        quadVertices[at + 6] = green;
        quadVertices[at + 7] = blue;
        quadVertices[at + 8] = alpha;
    }

    /** Tam may: dat quanh nguoi choi, anh may neo theo toa do that nen may nhu dung yen. */
    private void drawClouds(PerspectiveCamera camera, DayNightCycle cycle, float elapsed) {
        // Ban dem may xam lai theo do sang bau troi, hoang hon thi an mau troi.
        float light = 0.35f + 0.65f * cycle.daylight();
        tint.set(cycle.skyColor()).lerp(Color.WHITE, 0.75f);

        // Tam may di theo nguoi choi tren mat phang ngang, nhung do cao la TUYET DOI trong
        // the gioi - lay theo camera.y la may tut xuong lan vao mat dat.
        shader.setUniformf("u_offset", camera.position.x, CLOUD_Y, camera.position.z);
        shader.setUniformf("u_uvFromWorld", 1f);
        shader.setUniformf("u_scroll", elapsed * CLOUD_SPEED / CLOUD_TILE, 0f);
        shader.setUniformf("u_tint", tint.r * light, tint.g * light, tint.b * light, 0.75f);
        cloudTexture.bind(0);
        clouds.render(shader, GL20.GL_TRIANGLES);
        shader.setUniformf("u_tint", 1f, 1f, 1f, 1f);
    }

    // ------------------------------------------------------------------ dung luoi

    private Mesh buildBillboard() {
        Mesh mesh = new Mesh(false, 4, 6, attributes());
        mesh.setIndices(new short[]{0, 1, 2, 2, 3, 0});
        return mesh;
    }

    /**
     * Tam luoi phang {@link #CLOUD_CELLS}x{@link #CLOUD_CELLS} o. Toa do dinh tinh quanh goc
     * (0,0), con do mo cua tung dinh giam dan tu giua ra ria de khong thay canh tam luoi.
     */
    private Mesh buildClouds() {
        int side = CLOUD_CELLS + 1;
        float[] vertices = new float[side * side * FLOATS_PER_VERTEX];
        short[] indices = new short[CLOUD_CELLS * CLOUD_CELLS * 6];
        float step = CLOUD_SPAN / CLOUD_CELLS;
        int at = 0;

        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                float x = -CLOUD_SPAN * 0.5f + column * step;
                float z = -CLOUD_SPAN * 0.5f + row * step;
                float distance = (float) Math.sqrt(x * x + z * z) / (CLOUD_SPAN * 0.5f);
                float alpha = Math.max(0f, 1f - distance * distance);

                vertices[at] = x;
                vertices[at + 1] = 0f;
                vertices[at + 2] = z;
                vertices[at + 3] = 0f;   // toa do anh se tinh lai trong shader tu vi tri that
                vertices[at + 4] = 0f;
                vertices[at + 5] = 1f;
                vertices[at + 6] = 1f;
                vertices[at + 7] = 1f;
                vertices[at + 8] = alpha;
                at += FLOATS_PER_VERTEX;
            }
        }

        int index = 0;
        for (int row = 0; row < CLOUD_CELLS; row++) {
            for (int column = 0; column < CLOUD_CELLS; column++) {
                short topLeft = (short) (row * side + column);
                short topRight = (short) (topLeft + 1);
                short bottomLeft = (short) (topLeft + side);
                short bottomRight = (short) (bottomLeft + 1);
                indices[index++] = topLeft;
                indices[index++] = topRight;
                indices[index++] = bottomRight;
                indices[index++] = bottomRight;
                indices[index++] = bottomLeft;
                indices[index++] = topLeft;
            }
        }

        Mesh mesh = new Mesh(true, vertices.length / FLOATS_PER_VERTEX, indices.length, attributes());
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        return mesh;
    }

    private static VertexAttributes attributes() {
        return new VertexAttributes(
                new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2,
                        ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
    }

    // ------------------------------------------------------------------ anh ve san

    /**
     * Mot O VUONG dac kieu Minecraft (khong phai dia tron): giua sang, ria dam mau hon mot
     * chut cho co chieu sau. Anh de nho va loc theo diem anh nen canh vuong sac net.
     */
    private static Texture square(int size, Color inner, Color outer) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float centre = (size - 1) * 0.5f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Khoang cach kieu o vuong: lay canh xa nhat, khong lay duong cheo.
                float distance = Math.max(Math.abs(x - centre), Math.abs(y - centre)) / centre;
                float mix = Math.min(1f, distance * distance);
                pixmap.setColor(
                        inner.r + (outer.r - inner.r) * mix,
                        inner.g + (outer.g - inner.g) * mix,
                        inner.b + (outer.b - inner.b) * mix,
                        1f);
                pixmap.drawPixel(x, y);
            }
        }
        return toTexture(pixmap, Texture.TextureWrap.ClampToEdge, Texture.TextureFilter.Nearest);
    }

    /**
     * Anh may lap lai duoc: rai nhung mang chu nhat trang vuong vuc kieu Minecraft. Moi mang
     * duoc ve them o ca chin vi tri lech mot o anh, nho vay noi hai bien anh khong thay duong.
     */
    private static Texture cloudPuffs(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();

        Random random = new Random(20260724L);
        pixmap.setColor(1f, 1f, 1f, 1f);
        for (int puff = 0; puff < 26; puff++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            int width = 5 + random.nextInt(11);
            int height = 4 + random.nextInt(8);
            for (int wrapX = -1; wrapX <= 1; wrapX++) {
                for (int wrapY = -1; wrapY <= 1; wrapY++) {
                    pixmap.fillRectangle(x + wrapX * size, y + wrapY * size, width, height);
                }
            }
        }
        return toTexture(pixmap, Texture.TextureWrap.Repeat, Texture.TextureFilter.Linear);
    }

    private static Texture toTexture(Pixmap pixmap, Texture.TextureWrap wrap,
                                     Texture.TextureFilter filter) {
        Texture texture = new Texture(pixmap);
        texture.setFilter(filter, filter);
        texture.setWrap(wrap, wrap);
        pixmap.dispose();
        return texture;
    }

    private static ShaderProgram compile() {
        String vertex =
                "attribute vec3 a_position;\n"
                + "attribute vec2 a_texCoord0;\n"
                + "attribute vec4 a_color;\n"
                + "uniform mat4 u_projViewTrans;\n"
                + "uniform vec3 u_offset;\n"
                + "uniform vec2 u_scroll;\n"
                + "uniform float u_uvFromWorld;\n"
                + "uniform float u_tileSize;\n"
                + "varying vec2 v_uv;\n"
                + "varying vec4 v_color;\n"
                + "void main() {\n"
                + "  vec3 world = a_position + u_offset;\n"
                + "  vec2 worldUv = world.xz / u_tileSize + u_scroll;\n"
                + "  v_uv = mix(a_texCoord0, worldUv, u_uvFromWorld);\n"
                + "  v_color = a_color;\n"
                + "  gl_Position = u_projViewTrans * vec4(world, 1.0);\n"
                + "}\n";
        String fragment =
                "#ifdef GL_ES\n"
                + "precision mediump float;\n"
                + "#endif\n"
                + "uniform sampler2D u_texture;\n"
                + "uniform vec4 u_tint;\n"
                + "varying vec2 v_uv;\n"
                + "varying vec4 v_color;\n"
                + "void main() {\n"
                + "  gl_FragColor = texture2D(u_texture, v_uv) * v_color * u_tint;\n"
                + "}\n";

        ShaderProgram program = new ShaderProgram(vertex, fragment);
        if (!program.isCompiled()) {
            throw new IllegalStateException("sky shader failed: " + program.getLog());
        }
        program.bind();
        program.setUniformf("u_tint", 1f, 1f, 1f, 1f);
        program.setUniformf("u_tileSize", CLOUD_TILE);
        return program;
    }

    @Override
    public void dispose() {
        shader.dispose();
        billboard.dispose();
        clouds.dispose();
        sunTexture.dispose();
        moonTexture.dispose();
        cloudTexture.dispose();
    }
}
