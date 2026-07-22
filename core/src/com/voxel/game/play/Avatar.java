package com.voxel.game.play;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * The player seen from the front, drawn in the preview box of the bag.
 *
 * It is only a few rectangles stacked in the right Minecraft character proportions
 * (head 8x8, body 8x12, arms and legs 4x12 - 16 wide and 32 tall in total), sharing
 * the palette with the 3D player model in {@code PlayerModel} so both look the same.
 */
public final class Avatar {

    private static final float MODEL_WIDTH = 16f;
    private static final float MODEL_HEIGHT = 32f;

    public static final Color SKIN = rgb(0xE8B98D);
    public static final Color HAIR = rgb(0x3F2A17);
    public static final Color SHIRT = rgb(0x00A8A8);
    public static final Color PANTS = rgb(0x3B44AA);
    public static final Color SHOES = rgb(0x4A4A4A);
    public static final Color EYE = rgb(0x2A3FBF);

    private final MinecraftUi ui;

    public Avatar(MinecraftUi ui) {
        this.ui = ui;
    }

    /** Draws the character to fit the given box while keeping the right proportions. */
    public void draw(SpriteBatch batch, float x, float y, float width, float height) {
        float scale = Math.min(width / (MODEL_WIDTH + 4f), height / (MODEL_HEIGHT + 4f));
        float originX = x + (width - MODEL_WIDTH * scale) * 0.5f;
        float originY = y + (height - MODEL_HEIGHT * scale) * 0.5f;

        // Legs (y 0..12), body and arms (y 12..24), head (y 24..32).
        part(batch, SHOES, originX, originY, scale, 4f, 0f, 4f, 2f);
        part(batch, SHOES, originX, originY, scale, 8f, 0f, 4f, 2f);
        part(batch, PANTS, originX, originY, scale, 4f, 2f, 4f, 10f);
        part(batch, PANTS, originX, originY, scale, 8f, 2f, 4f, 10f);

        part(batch, SHIRT, originX, originY, scale, 4f, 12f, 8f, 12f);
        part(batch, SHIRT, originX, originY, scale, 0f, 16f, 4f, 8f);
        part(batch, SHIRT, originX, originY, scale, 12f, 16f, 4f, 8f);
        part(batch, SKIN, originX, originY, scale, 0f, 12f, 4f, 4f);
        part(batch, SKIN, originX, originY, scale, 12f, 12f, 4f, 4f);

        part(batch, SKIN, originX, originY, scale, 4f, 24f, 8f, 8f);
        part(batch, HAIR, originX, originY, scale, 4f, 30f, 8f, 2f);
        part(batch, EYE, originX, originY, scale, 5f, 27f, 2f, 1f);
        part(batch, EYE, originX, originY, scale, 9f, 27f, 2f, 1f);

        batch.setColor(Color.WHITE);
    }

    private void part(SpriteBatch batch, Color color, float originX, float originY, float scale,
                      float x, float y, float width, float height) {
        ui.rect(batch, color, originX + x * scale, originY + y * scale, width * scale, height * scale);
    }

    private static Color rgb(int hex) {
        return new Color(((hex >> 16) & 0xFF) / 255f, ((hex >> 8) & 0xFF) / 255f, (hex & 0xFF) / 255f, 1f);
    }
}
