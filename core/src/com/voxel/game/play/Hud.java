package com.voxel.game.play;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

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
    /** Toc do khung chon truot toi o moi (cang lon cang bam sat, cang nho cang muot). */
    private static final float SELECT_SLIDE_SPEED = 18f;
    /** Kich thuoc o hoi sinh tren man hinh chet (diem anh giao dien). */
    private static final float BUTTON_W = 90f;
    private static final float BUTTON_H = 20f;
    private static final String RESPAWN_TEXT = "HOI SINH";

    private final Inventory inventory;
    private final PlayerStats stats;
    private final CommandConsole console;
    private final ItemRenderer items;
    private final MinecraftUi ui;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    /** Vung o "HOI SINH", do lai moi khung hinh khi dang chet. */
    private final Rectangle respawnButton = new Rectangle();
    /** Vi tri o dang chon dang duoc ve, truot dan toi o that de nhin muot. -1 = chua dat. */
    private float animatedSlot = -1f;

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
        // Vignette phu len the gioi (duoi moi thu HUD) cho khung hinh "dien anh".
        batch.setColor(Color.WHITE);
        batch.draw(ui.vignette, 0f, 0f, width, height);

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

        drawSelection(batch, x, edge);
    }

    /**
     * Khung o dang chon: vien den ngoai, vien trang mong, roi vien magenta - phong cach
     * goi Better Modded GUI. Khung TRUOT muot toi o moi thay vi nhay thang cho da mat.
     */
    private void drawSelection(SpriteBatch batch, float x, float edge) {
        float target = inventory.selected();
        if (animatedSlot < 0f || Math.abs(target - animatedSlot) > Inventory.HOTBAR_SIZE / 2f) {
            // Lan dau, hoac cuon vong (o 8 -> o 0): bam thang, khong truot ngang het ca thanh.
            animatedSlot = target;
        } else {
            animatedSlot += (target - animatedSlot)
                    * Math.min(1f, Gdx.graphics.getDeltaTime() * SELECT_SLIDE_SPEED);
        }

        float selectX = x + px(animatedSlot * SLOT);
        float size = px(24f);
        batch.setColor(Color.BLACK);
        ui.frame(batch, selectX - edge, -edge, size + edge * 2f, size + edge * 2f, edge);
        batch.setColor(MinecraftUi.ACCENT_EDGE);
        ui.frame(batch, selectX, 0f, size, size, px(1f));
        batch.setColor(MinecraftUi.SELECTION);
        ui.frame(batch, selectX + px(1f), px(1f), size - px(2f), size - px(2f), px(2f));
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

    /** 10 trai tim cho 20 mau; con le thi ve nua trai tim, dem tu TRAI sang phai. */
    private void drawHearts(SpriteBatch batch, float x) {
        drawIconRow(batch, ui.heartEmpty, ui.heartHalf, ui.heartFull,
                stats.health(), false, x, px(ROW_HEALTH_Y));
    }

    /** 10 dui ga cho 20 do no, vo dan tu PHAI sang trai giong Minecraft. */
    private void drawHunger(SpriteBatch batch, float x) {
        drawIconRow(batch, ui.hungerEmpty, ui.hungerHalf, ui.hungerFull,
                stats.food(), true, x, px(ROW_HEALTH_Y));
    }

    /**
     * Ve mot hang 10 bieu tuong theo dung cach Minecraft: ve het cac o NEN rong (_empty)
     * truoc, roi da ban day (_full) hoac nua (_half) len tren - nho vay o nua van thay
     * duoc phan nen toi con lai.
     *
     * @param value    tong so nua (mau hoac do no), moi bieu tuong an hai nua
     * @param fromRight dem tu ben phai sang (dui ga) hay tu trai sang (trai tim)
     */
    private void drawIconRow(SpriteBatch batch, Texture empty, Texture half, Texture full,
                             int value, boolean fromRight, float x, float y) {
        float size = px(ICON);
        batch.setColor(Color.WHITE);
        for (int i = 0; i < ICONS; i++) {
            batch.draw(empty, iconX(x, i, fromRight), y, size, size);
        }
        for (int i = 0; i < ICONS; i++) {
            int slot = value - i * 2;
            Texture fill = slot >= 2 ? full : slot == 1 ? half : null;
            if (fill != null) {
                batch.draw(fill, iconX(x, i, fromRight), y, size, size);
            }
        }
    }

    /** Vi tri ngang cua bieu tuong thu i, dem tu trai hoac tu phai tuy hang. */
    private float iconX(float barX, int i, boolean fromRight) {
        return fromRight ? iconFromRight(barX, i) : barX + px(i * ICON_STEP);
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

    /**
     * Man hinh chet: nen do mo, dong chu, va mot O HOI SINH bam chuot vao duoc.
     *
     * O nay duoc do lai moi khung hinh vao {@link #respawnButton} de {@code PlaySession}
     * biet nguoi choi co bam trung hay khong - ve va bat chuot dung chung mot hinh chu nhat
     * nen khong bao gio lech nhau.
     */
    private void drawDeathScreen(SpriteBatch batch, int width, int height) {
        ui.rect(batch, Color.RED, 0.45f, 0f, 0f, width, height);
        batch.setColor(Color.WHITE);
        center(batch, "BAN DA CHET!", width, height * 0.5f + px(30f));

        float buttonW = px(BUTTON_W);
        float buttonH = px(BUTTON_H);
        respawnButton.set((width - buttonW) * 0.5f, height * 0.5f - px(24f), buttonW, buttonH);

        // Con tro dang di qua nut thi nut sang len mot chut cho biet bam duoc.
        boolean hover = respawnButton.contains(Gdx.input.getX(), height - Gdx.input.getY());
        ui.rect(batch, Color.BLACK, respawnButton.x - px(1f), respawnButton.y - px(1f),
                respawnButton.width + px(2f), respawnButton.height + px(2f));
        ui.rect(batch, hover ? MinecraftUi.PANEL_LIGHT : MinecraftUi.PANEL_BG,
                respawnButton.x, respawnButton.y, respawnButton.width, respawnButton.height);
        batch.setColor(hover ? MinecraftUi.ACCENT_EDGE : MinecraftUi.BAR_EDGE);
        ui.frame(batch, respawnButton.x, respawnButton.y,
                respawnButton.width, respawnButton.height, px(1f));
        batch.setColor(Color.WHITE);

        layout.setText(font, RESPAWN_TEXT);
        font.setColor(Color.WHITE);
        font.draw(batch, RESPAWN_TEXT,
                respawnButton.x + (respawnButton.width - layout.width) * 0.5f,
                respawnButton.y + (respawnButton.height + layout.height) * 0.5f);
    }

    /** True neu cu bam roi vao o hoi sinh (toa do y da doi ve he giao dien). */
    public boolean respawnButtonHit(float mouseX, float mouseY) {
        return stats.isDead() && respawnButton.contains(mouseX, mouseY);
    }

    private void center(SpriteBatch batch, String text, int width, float y) {
        layout.setText(font, text);
        font.setColor(Color.BLACK);
        font.draw(batch, text, (width - layout.width) * 0.5f + 2f, y - 2f);
        font.setColor(Color.WHITE);
        font.draw(batch, text, (width - layout.width) * 0.5f, y);
    }
}
