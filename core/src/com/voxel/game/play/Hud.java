package com.voxel.game.play;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Thanh trang thai duoi day man hinh.
 *
 * Cach xep va tung con so duoi day DO TRUC TIEP tu anh chup Minecraft that
 * (1.png). Tinh bang diem anh giao dien, goc toa do la day man hinh:
 *
 * <pre>
 *   y 38..47 : bong bong khi (ben phai)
 *   y 28..37 : trai tim (trai)  -  dui ga (phai)
 *   y 22..27 : thanh kinh nghiem, so cap nam chinh giua
 *   y  0..22 : thanh nhanh rong 182, gom 9 o 20x20 DINH LIEN NHAU
 * </pre>
 *
 * Trai tim va dui ga cach nhau 8 diem anh trong khi hinh rong 9, nen hai icon ke
 * nhau dung chung mot cot vien - dung y nhu Minecraft.
 */
public final class Hud {

    private static final int ICONS = 10;
    private static final float ICON = 9f;
    private static final float ICON_STEP = 8f;
    private static final float SLOT = 20f;
    /** 9 o 20 diem anh + hai duong vien hai ben = 182, dung bang Minecraft. */
    private static final float BAR_WIDTH = Inventory.HOTBAR_SIZE * SLOT + 2f;
    private static final float BAR_HEIGHT = 22f;
    private static final float ROW_HEALTH_Y = 28f;
    private static final float ROW_AIR_Y = 38f;
    private static final float XP_BAR_Y = 22f;
    private static final float XP_BAR_HEIGHT = 5f;

    private final Inventory inventory;
    private final PlayerStats stats;
    private final CommandConsole console;
    private final ItemRenderer items;
    private final MinecraftUi ui;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    public Hud(Inventory inventory, PlayerStats stats, CommandConsole console,
               ItemRenderer items, BitmapFont font) {
        this.inventory = inventory;
        this.stats = stats;
        this.console = console;
        this.items = items;
        this.ui = items.ui();
        this.font = font;
    }

    private static float px(float guiPixels) {
        return guiPixels * MinecraftUi.SCALE;
    }

    /**
     * @param inventoryOpen dang mo tui do thi giau thanh nhanh di, giong Minecraft
     */
    public void draw(SpriteBatch batch, int width, int height, GameMode mode, boolean inventoryOpen) {
        drawDamageFlash(batch, width, height);

        if (!inventoryOpen) {
            float barX = (width - px(BAR_WIDTH)) * 0.5f;
            drawHotbar(batch, barX);

            if (mode.isSurvival()) {
                drawXpBar(batch, barX, width);
                drawHearts(batch, barX);
                drawHunger(batch, barX);
                if (stats.showAir()) {
                    drawBubbles(batch, barX);
                }
            }
            drawSelectedName(batch, width, mode);
        }

        drawChat(batch);
        if (stats.isDead()) {
            drawDeathScreen(batch, width, height);
        }
    }

    private void drawDamageFlash(SpriteBatch batch, int width, int height) {
        float flash = stats.damageFlash();
        if (flash > 0f) {
            ui.rect(batch, Color.RED, flash * 0.3f, 0f, 0f, width, height);
            batch.setColor(Color.WHITE);
        }
    }

    /**
     * Thanh nhanh: vien den bao ngoai cung, roi den vien xam #909090, ben trong la
     * 9 o den mo ngan nhau bang vach #656565. O dang cam co khung #C6C6C6 day gap
     * doi, tho han ra ngoai thanh nhanh.
     */
    private void drawHotbar(SpriteBatch batch, float x) {
        float edge = px(1f);
        float barW = px(BAR_WIDTH);
        float barH = px(BAR_HEIGHT);

        ui.rect(batch, Color.BLACK, x - edge, -edge, barW + edge * 2f, barH + edge * 2f);
        ui.rect(batch, MinecraftUi.BAR_EDGE, x, 0f, barW, barH);
        ui.rect(batch, Color.BLACK, 0.62f, x + edge, edge, barW - edge * 2f, barH - edge * 2f);

        for (int slot = 0; slot < Inventory.HOTBAR_SIZE; slot++) {
            float slotX = x + edge + px(slot * SLOT);
            if (slot > 0) {
                ui.rect(batch, MinecraftUi.BAR_DIVIDER, slotX - edge, edge, edge, barH - edge * 2f);
            }
            items.drawItem(batch, inventory.get(slot), slotX, edge, px(SLOT));
        }

        // Khung o dang chon: 24x24, tho ra 1 diem anh moi ben so voi thanh nhanh.
        float selectX = x + px(inventory.selected() * SLOT);
        batch.setColor(Color.BLACK);
        ui.frame(batch, selectX - edge, -edge, px(24f) + edge * 2f, px(24f) + edge * 2f, edge);
        batch.setColor(MinecraftUi.SELECTION);
        ui.frame(batch, selectX, 0f, px(24f), px(24f), px(2f));
        batch.setColor(Color.WHITE);
    }

    /** Thanh kinh nghiem: chua day mau xanh den, da day mau xanh chanh ba sac. */
    private void drawXpBar(SpriteBatch batch, float x, int screenWidth) {
        float barWidth = px(BAR_WIDTH);
        float y = px(XP_BAR_Y);
        float height = px(XP_BAR_HEIGHT);
        float filled = barWidth * Math.max(0f, Math.min(1f, stats.progress()));

        ui.rect(batch, MinecraftUi.XP_EMPTY, x, y, barWidth, height);
        ui.rect(batch, Color.BLACK, x, y, barWidth, px(1f));
        if (filled > 0f) {
            ui.rect(batch, MinecraftUi.XP_GREEN, x, y + px(1f), filled, height - px(1f));
            ui.rect(batch, MinecraftUi.XP_GREEN_DARK, x, y, filled, px(1f));
        }
        batch.setColor(Color.WHITE);

        if (stats.level() > 0) {
            String text = String.valueOf(stats.level());
            layout.setText(font, text);
            float textX = (screenWidth - layout.width) * 0.5f;
            float textY = y + height + layout.height + px(1f);
            // Vien den bao bon phia cho so noi len tren nen sang.
            font.setColor(Color.BLACK);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        font.draw(batch, text, textX + dx * 2f, textY + dy * 2f);
                    }
                }
            }
            font.setColor(MinecraftUi.XP_GREEN);
            font.draw(batch, text, textX, textY);
            font.setColor(Color.WHITE);
        }
    }

    /** 10 trai tim cho 20 mau; con le thi ve nua trai tim. */
    private void drawHearts(SpriteBatch batch, float x) {
        int health = stats.health();
        batch.setColor(Color.WHITE);
        for (int i = 0; i < ICONS; i++) {
            int value = health - i * 2;
            Texture texture = value >= 2 ? ui.heartFull : value == 1 ? ui.heartHalf : ui.heartEmpty;
            batch.draw(texture, x + px(i * ICON_STEP), px(ROW_HEALTH_Y), px(ICON + 2f), px(ICON + 2f));
        }
    }

    /** 10 dui ga cho 20 do no, vo dan tu PHAI sang trai giong Minecraft. */
    private void drawHunger(SpriteBatch batch, float x) {
        int food = stats.food();
        batch.setColor(Color.WHITE);
        for (int i = 0; i < ICONS; i++) {
            int value = food - i * 2;
            Texture texture = value >= 2 ? ui.hungerFull : value == 1 ? ui.hungerHalf : ui.hungerEmpty;
            batch.draw(texture, iconFromRight(x, i), px(ROW_HEALTH_Y), px(ICON + 2f), px(ICON + 2f));
        }
    }

    /** Bong bong khi nam ngay tren day dui ga, cung ben phai. */
    private void drawBubbles(SpriteBatch batch, float x) {
        float ratio = stats.air() / PlayerStats.MAX_AIR;
        int full = (int) Math.ceil(ratio * ICONS);
        batch.setColor(Color.WHITE);
        for (int i = 0; i < ICONS; i++) {
            if (i < full) {
                Texture texture = i == full - 1 ? ui.bubblePop : ui.bubbleFull;
                batch.draw(texture, iconFromRight(x, i), px(ROW_AIR_Y), px(ICON + 2f), px(ICON + 2f));
            }
        }
    }

    /** Vi tri cua bieu tuong thu i tren day dem tu ben PHAI sang. */
    private float iconFromRight(float barX, int i) {
        return barX + px(BAR_WIDTH) - px(ICON + 1f) - px(i * ICON_STEP);
    }

    private void drawSelectedName(SpriteBatch batch, int width, GameMode mode) {
        ItemStack stack = inventory.selectedStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        String name = stack.block().name().replace('_', ' ');
        layout.setText(font, name);
        float y = px(mode.isSurvival() ? ROW_AIR_Y + 12f : XP_BAR_Y + 6f) + layout.height;
        font.setColor(Color.BLACK);
        font.draw(batch, name, (width - layout.width) * 0.5f + 2f, y - 2f);
        font.setColor(1f, 1f, 1f, 0.9f);
        font.draw(batch, name, (width - layout.width) * 0.5f, y);
        font.setColor(Color.WHITE);
    }

    private void drawChat(SpriteBatch batch) {
        float margin = px(4f);
        float y = px(ROW_AIR_Y + 22f);
        for (int i = console.lines().size() - 1; i >= 0; i--) {
            CommandConsole.Line line = console.lines().get(i);
            float alpha = line.alpha();
            layout.setText(font, line.text);
            ui.rect(batch, Color.BLACK, alpha * 0.5f, margin, y - 4f, layout.width + 8f, layout.height + 10f);
            font.setColor(1f, 1f, 1f, alpha);
            font.draw(batch, line.text, margin + 4f, y + layout.height + 1f);
            y += layout.height + 12f;
        }
        font.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);

        if (console.isOpen()) {
            boolean caret = (System.currentTimeMillis() / 400L) % 2L == 0L;
            ui.rect(batch, Color.BLACK, 0.7f, margin, px(ROW_AIR_Y + 8f), 640f, px(9f));
            font.setColor(Color.WHITE);
            font.draw(batch, console.input() + (caret ? "_" : ""), margin + 4f, px(ROW_AIR_Y + 8f) + px(7f));
        }
    }

    private void drawDeathScreen(SpriteBatch batch, int width, int height) {
        ui.rect(batch, Color.RED, 0.45f, 0f, 0f, width, height);
        batch.setColor(Color.WHITE);
        center(batch, "BAN DA CHET!", width, height * 0.5f + 40f);
        center(batch, "Bam R de hoi sinh", width, height * 0.5f);
    }

    private void center(SpriteBatch batch, String text, int width, float y) {
        layout.setText(font, text);
        font.setColor(Color.BLACK);
        font.draw(batch, text, (width - layout.width) * 0.5f + 2f, y - 2f);
        font.setColor(Color.WHITE);
        font.draw(batch, text, (width - layout.width) * 0.5f, y);
    }
}
