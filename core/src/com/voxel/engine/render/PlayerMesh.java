package com.voxel.engine.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

/**
 * Builds the Minecraft-style player figure and wraps it in a 64x64 skin texture.
 *
 * The body is the classic six boxes (head, body, two arms, two legs). Every face of a
 * box is a rectangle whose texture coordinates point at the matching square of the skin,
 * following the standard "cross" unwrap that Minecraft uses for each box:
 *
 * <pre>
 *        [top ][bot ]
 *   [rgt][frnt][lft][back]
 * </pre>
 *
 * Each box is its own NODE (named legLeft, legRight, body, armLeft, armRight, head) so a
 * renderer can rotate a limb to make the character walk. On top of the base boxes a second,
 * slightly larger set draws the skin's overlay layer - hat, jacket, sleeves and trousers -
 * with the transparent pixels cut away.
 */
public final class PlayerMesh {

    private PlayerMesh() {
    }

    /** Skin side length in pixels; one pixel is therefore 1/64 of the texture. */
    private static final float TEX = 64f;
    /** How much bigger the overlay boxes are than the base, in half a skin pixel. */
    private static final float OVERLAY_INFLATE = 0.5f;

    /**
     * The four corners of each face, as +/-1 signs on (x, y, z) measured from the box
     * centre, in the order top-left, top-right, bottom-right, bottom-left of the skin
     * square. Faces are ordered front, back, right, left, top, bottom - the character
     * faces +Z, so the face on the head is on the +Z side.
     */
    private static final int[][][] FACES = {
        {{ 1, 1, 1}, {-1, 1, 1}, {-1,-1, 1}, { 1,-1, 1}}, // front  (+Z)
        {{-1, 1,-1}, { 1, 1,-1}, { 1,-1,-1}, {-1,-1,-1}}, // back   (-Z)
        {{ 1, 1, 1}, { 1, 1,-1}, { 1,-1,-1}, { 1,-1, 1}}, // right  (+X)
        {{-1, 1,-1}, {-1, 1, 1}, {-1,-1, 1}, {-1,-1,-1}}, // left   (-X)
        {{ 1, 1,-1}, {-1, 1,-1}, {-1, 1, 1}, { 1, 1, 1}}, // top    (+Y)
        {{ 1,-1, 1}, {-1,-1, 1}, {-1,-1,-1}, { 1,-1,-1}}, // bottom (-Y)
    };

    private static final float[][] NORMALS = {
        {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0},
    };

    /**
     * @param skin the 64x64 skin texture (use Nearest filtering for crisp pixels)
     * @param unit world size of one skin pixel - the block height divided by 32
     * @return a model whose nodes are legLeft, legRight, body, armLeft, armRight, head
     */
    public static Model build(Texture skin, float unit) {
        ModelBuilder builder = new ModelBuilder();
        long attributes = VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal
                | VertexAttributes.Usage.TextureCoordinates;

        Material base = new Material(
                TextureAttribute.createDiffuse(skin),
                IntAttribute.createCullFace(GL20.GL_NONE));
        // The overlay throws away fully transparent pixels instead of blending them, so
        // it never leaves a see-through hole in the depth buffer over the base layer.
        Material overlay = new Material(
                TextureAttribute.createDiffuse(skin),
                IntAttribute.createCullFace(GL20.GL_NONE),
                FloatAttribute.createAlphaTest(0.5f));

        builder.begin();
        //   node        base uv   centre         size       overlay uv
        limb(builder, attributes, base, overlay, unit, "legLeft",  16, 48,  -2,  6, 0,  4, 12, 4,  0, 48);
        limb(builder, attributes, base, overlay, unit, "legRight",  0, 16,   2,  6, 0,  4, 12, 4,  0, 32);
        limb(builder, attributes, base, overlay, unit, "body",     16, 16,   0, 18, 0,  8, 12, 4, 16, 32);
        limb(builder, attributes, base, overlay, unit, "armLeft",  32, 48,  -6, 18, 0,  4, 12, 4, 48, 48);
        limb(builder, attributes, base, overlay, unit, "armRight", 40, 16,   6, 18, 0,  4, 12, 4, 40, 32);
        limb(builder, attributes, base, overlay, unit, "head",      0,  0,   0, 28, 0,  8,  8, 8, 32,  0);
        return builder.end();
    }

    /** Adds one body box (base plus overlay) as a single node the caller can rotate. */
    private static void limb(ModelBuilder builder, long attributes, Material base, Material overlay,
                             float unit, String name, int ox, int oy,
                             float cx, float cy, float cz, int w, int h, int d,
                             int overlayOx, int overlayOy) {
        builder.node().id = name;
        box(builder.part(name, GL20.GL_TRIANGLES, attributes, base),
                unit, ox, oy, cx, cy, cz, w, h, d, 0f);
        box(builder.part(name + "Overlay", GL20.GL_TRIANGLES, attributes, overlay),
                unit, overlayOx, overlayOy, cx, cy, cz, w, h, d, OVERLAY_INFLATE);
    }

    /**
     * Emits the six textured faces of one box.
     *
     * @param ox,oy   the box's texture origin in skin pixels (top-left of its unwrap)
     * @param cx,cy,cz box centre, in skin pixels above the point between the feet
     * @param w,h,d   box size in skin pixels
     * @param inflate extra half-pixels added to every side (0 for the base layer)
     */
    private static void box(MeshPartBuilder part, float unit, int ox, int oy,
                            float cx, float cy, float cz, int w, int h, int d, float inflate) {
        float hx = (w * 0.5f + inflate) * unit;
        float hy = (h * 0.5f + inflate) * unit;
        float hz = (d * 0.5f + inflate) * unit;
        float mx = cx * unit, my = cy * unit, mz = cz * unit;

        for (int f = 0; f < FACES.length; f++) {
            int[] region = uvRegion(f, ox, oy, w, h, d);
            float u0 = region[0] / TEX, v0 = region[1] / TEX;
            float u1 = (region[0] + region[2]) / TEX, v1 = (region[1] + region[3]) / TEX;
            int[][] c = FACES[f];
            float[] n = NORMALS[f];
            part.rect(
                    corner(mx, my, mz, hx, hy, hz, c[0], n, u0, v0),  // top-left
                    corner(mx, my, mz, hx, hy, hz, c[1], n, u1, v0),  // top-right
                    corner(mx, my, mz, hx, hy, hz, c[2], n, u1, v1),  // bottom-right
                    corner(mx, my, mz, hx, hy, hz, c[3], n, u0, v1)); // bottom-left
        }
    }

    private static MeshPartBuilder.VertexInfo corner(float mx, float my, float mz,
                                                     float hx, float hy, float hz,
                                                     int[] sign, float[] normal, float u, float v) {
        MeshPartBuilder.VertexInfo info = new MeshPartBuilder.VertexInfo();
        info.setPos(mx + sign[0] * hx, my + sign[1] * hy, mz + sign[2] * hz);
        info.setNor(normal[0], normal[1], normal[2]);
        info.setUV(u, v);
        return info;
    }

    /** The skin rectangle for one face of a box in the standard cross unwrap. */
    private static int[] uvRegion(int face, int ox, int oy, int w, int h, int d) {
        switch (face) {
            case 0: return new int[]{ox + d,             oy + d, w, h}; // front
            case 1: return new int[]{ox + 2 * d + w,     oy + d, w, h}; // back
            case 2: return new int[]{ox,                 oy + d, d, h}; // right
            case 3: return new int[]{ox + d + w,         oy + d, d, h}; // left
            case 4: return new int[]{ox + d,             oy,     w, d}; // top
            default: return new int[]{ox + d + w,        oy,     w, d}; // bottom
        }
    }
}
