package com.voxel.game.play;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.voxel.engine.block.Block;
import com.voxel.engine.util.Direction;

/**
 * Ve mot o do: khung o, hinh khoi ben trong va so luong o goc.
 *
 * Hinh dai dien cua khoi lay thang tu atlas cua the gioi (mat tren cua khoi), nen
 * do trong tui trong y het khoi ngoai the gioi ma khong can bo anh rieng.
 */
public final class ItemRenderer {

    private final TextureAtlas atlas;
    private final MinecraftUi ui;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    public ItemRenderer(TextureAtlas atlas, MinecraftUi ui, BitmapFont font) {
        this.atlas = atlas;
        this.ui = ui;
        this.font = font;
    }

    public MinecraftUi ui() {
        return ui;
    }

    /** Ve mon do nam trong o, kem so luong neu nhieu hon 1. */
    public void drawItem(SpriteBatch batch, ItemStack stack, float x, float y, float size) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        drawBlockIcon(batch, stack.block(), x, y, size);

        if (stack.count() > 1) {
            String count = String.valueOf(stack.count());
            layout.setText(font, count);
            float textX = x + size - layout.width - 2f;
            float textY = y + layout.height + 2f;
            font.setColor(Color.BLACK);
            font.draw(batch, count, textX + 2f, textY - 2f);
            font.setColor(Color.WHITE);
            font.draw(batch, count, textX, textY);
        }
    }

    public void drawBlockIcon(SpriteBatch batch, Block block, float x, float y, float size) {
        TextureRegion region = atlas.findRegion(block.textureFor(Direction.UP));
        if (region == null) {
            return;
        }
        batch.setColor(Color.WHITE);
        float inset = size * 0.15f;
        batch.draw(region, x + inset, y + inset, size - inset * 2f, size - inset * 2f);
    }
}
