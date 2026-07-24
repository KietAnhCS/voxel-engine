package com.voxel.game.play;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * The player seen from the front, drawn in the preview box of the bag.
 *
 * It shows the SAME 64x64 skin ({@code skinzom.png}) that wraps the 3D player, cut into the
 * front faces of head, body, arms and legs and stacked in Minecraft character proportions
 * (head 8x8, body 8x12, arms and legs 4x12 - 16 wide and 32 tall in total). The skin's
 * overlay layer (hat, jacket, sleeves, trousers) is drawn on top so the preview matches
 * the character in the world.
 */
public final class Avatar implements Disposable {

    private static final float MODEL_WIDTH = 16f;
    private static final float MODEL_HEIGHT = 32f;

    private final Texture skin;

    // Front faces of the base layer, as {model x, model y, model w, model h, skin x, skin y}.
    // The character's own right side is on the left of a front view, so the right arm/leg
    // sit on the left columns. Skin y is measured from the top of the image.
    private static final int[][] BASE = {
            {4, 24, 8, 8,   8,  8},   // head
            {4, 12, 8, 12, 20, 20},   // body
            {0, 12, 4, 12, 44, 20},   // right arm
            {12, 12, 4, 12, 36, 52},  // left arm
            {4, 0, 4, 12,   4, 20},   // right leg
            {8, 0, 4, 12,  20, 52},   // left leg
    };
    private static final int[][] OVERLAY = {
            {4, 24, 8, 8,  40,  8},   // hat
            {4, 12, 8, 12, 20, 36},   // jacket
            {0, 12, 4, 12, 44, 36},   // right sleeve
            {12, 12, 4, 12, 52, 52},  // left sleeve
            {4, 0, 4, 12,   4, 36},   // right trouser
            {8, 0, 4, 12,   4, 52},   // left trouser
    };

    private final TextureRegion[] base;
    private final TextureRegion[] overlay;

    public Avatar(MinecraftUi ui) {
        this.skin = new Texture(Gdx.files.internal("data/skinzom.png"));
        this.skin.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        this.base = regions(BASE);
        this.overlay = regions(OVERLAY);
    }

    private TextureRegion[] regions(int[][] table) {
        TextureRegion[] out = new TextureRegion[table.length];
        for (int i = 0; i < table.length; i++) {
            int[] p = table[i];
            out[i] = new TextureRegion(skin, p[4], p[5], p[2], p[3]);
        }
        return out;
    }

    /** Draws the character to fit the given box while keeping the right proportions. */
    public void draw(SpriteBatch batch, float x, float y, float width, float height) {
        float scale = Math.min(width / (MODEL_WIDTH + 4f), height / (MODEL_HEIGHT + 4f));
        float originX = x + (width - MODEL_WIDTH * scale) * 0.5f;
        float originY = y + (height - MODEL_HEIGHT * scale) * 0.5f;

        batch.setColor(Color.WHITE);
        drawLayer(batch, BASE, base, originX, originY, scale);
        drawLayer(batch, OVERLAY, overlay, originX, originY, scale);
    }

    private void drawLayer(SpriteBatch batch, int[][] table, TextureRegion[] regions,
                           float originX, float originY, float scale) {
        for (int i = 0; i < table.length; i++) {
            int[] p = table[i];
            batch.draw(regions[i], originX + p[0] * scale, originY + p[1] * scale,
                    p[2] * scale, p[3] * scale);
        }
    }

    @Override
    public void dispose() {
        skin.dispose();
    }
}
