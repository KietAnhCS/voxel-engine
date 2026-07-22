package com.voxel.game.play;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.voxel.engine.block.Block;

/**
 * Bang tui do khi bam E.
 *
 * Kich thuoc va vi tri tung o lay DUNG theo bang kho do cua Minecraft (do tu
 * 2.png): tam bang 176 x 166 diem anh giao dien, moi o chiem 18 diem anh.
 * Toa do goc trong Minecraft tinh tu goc TREN-TRAI di xuong, con libGDX tinh tu
 * goc DUOI-TRAI di len, nen {@link #slotY} lam nhiem vu lat truc.
 *
 * <pre>
 *   (7,7)   cot 4 o giap        (25,7)  khung xem nhan vat   (97,17) che tao 2x2
 *   (76,61) o tay phu                                        (153,27) o ket qua
 *   (7,83)  kho chua 9 x 3
 *   (7,141) thanh nhanh 9 o
 * </pre>
 *
 * O che do sang tao, ba hang kho chua duoc thay bang KHO KHOI: bam vao la duoc
 * nguyen mot chong 64 cai.
 */
public final class InventoryScreen {

    private static final float PANEL_W = 176f;
    private static final float PANEL_H = 166f;
    private static final float CELL = 18f;

    private static final float ARMOR_X = 7f;
    private static final float ARMOR_TOP_Y = 7f;
    private static final float PREVIEW_X = 25f;
    private static final float PREVIEW_Y = 7f;
    private static final float PREVIEW_W = 52f;
    private static final float PREVIEW_H = 70f;
    private static final float OFFHAND_X = 76f;
    private static final float OFFHAND_Y = 61f;
    private static final float CRAFT_X = 97f;
    private static final float CRAFT_Y = 17f;
    private static final float ARROW_X = 135f;
    private static final float RESULT_X = 153f;
    private static final float RESULT_Y = 27f;
    private static final float STORAGE_X = 7f;
    private static final float STORAGE_Y = 83f;
    private static final float HOTBAR_Y = 141f;

    private final Inventory inventory;
    private final Crafting crafting;
    private final Block[] palette;
    private final ItemRenderer items;
    private final MinecraftUi ui;
    private final BitmapFont font;
    private final Avatar avatar;

    private boolean open;
    private float panelX;
    private float panelY;

    public InventoryScreen(Inventory inventory, Crafting crafting, Block[] palette,
                           ItemRenderer items, BitmapFont font) {
        this.inventory = inventory;
        this.crafting = crafting;
        this.palette = palette;
        this.items = items;
        this.ui = items.ui();
        this.font = font;
        this.avatar = new Avatar(ui);
    }

    private static float px(float guiPixels) {
        return guiPixels * MinecraftUi.SCALE;
    }

    public boolean isOpen() {
        return open;
    }

    public void open() {
        open = true;
    }

    public void close() {
        open = false;
        crafting.returnAll(inventory);
        ItemStack carried = inventory.carried();
        if (carried != null) {
            for (int i = 0; i < carried.count(); i++) {
                inventory.add(carried.block());
            }
            inventory.dropCarried();
        }
    }

    private void layout(int screenWidth, int screenHeight) {
        panelX = (screenWidth - px(PANEL_W)) * 0.5f;
        panelY = (screenHeight - px(PANEL_H)) * 0.5f;
    }

    /** Doi toa do y kieu Minecraft (tu tren xuong) sang kieu libGDX (tu duoi len). */
    private float slotY(float guiY, float heightInGui) {
        return panelY + px(PANEL_H - guiY - heightInGui);
    }

    private float slotX(float guiX) {
        return panelX + px(guiX);
    }

    // ------------------------------------------------------------------- ve

    public void draw(SpriteBatch batch, int screenWidth, int screenHeight, GameMode mode) {
        if (!open) {
            return;
        }
        layout(screenWidth, screenHeight);

        ui.rect(batch, Color.BLACK, 0.55f, 0f, 0f, screenWidth, screenHeight);
        ui.panel(batch, panelX, panelY, px(PANEL_W), px(PANEL_H));

        font.setColor(MinecraftUi.TEXT_DARK);
        font.draw(batch, mode.isCreative() ? "Kho khoi" : "Che tao",
                slotX(7f), slotY(5f, 0f));
        font.setColor(Color.WHITE);

        drawArmorAndPreview(batch);
        drawCrafting(batch);
        drawStorage(batch, mode);
        drawHotbarRow(batch);
    }

    private void drawArmorAndPreview(SpriteBatch batch) {
        for (int i = 0; i < 4; i++) {
            ui.slot(batch, slotX(ARMOR_X), slotY(ARMOR_TOP_Y + i * CELL, CELL), px(CELL));
        }

        float previewX = slotX(PREVIEW_X);
        float previewY = slotY(PREVIEW_Y, PREVIEW_H);
        ui.rect(batch, MinecraftUi.SLOT_DARK, previewX, previewY, px(PREVIEW_W), px(PREVIEW_H));
        ui.rect(batch, MinecraftUi.SLOT_BG, previewX + px(1f), previewY + px(1f),
                px(PREVIEW_W - 2f), px(PREVIEW_H - 2f));
        batch.setColor(Color.WHITE);
        avatar.draw(batch, previewX, previewY, px(PREVIEW_W), px(PREVIEW_H));

        ui.slot(batch, slotX(OFFHAND_X), slotY(OFFHAND_Y, CELL), px(CELL));
    }

    private void drawCrafting(SpriteBatch batch) {
        for (int i = 0; i < Crafting.GRID; i++) {
            float x = slotX(CRAFT_X + (i % 2) * CELL);
            float y = slotY(CRAFT_Y + (i / 2) * CELL, CELL);
            ui.slot(batch, x, y, px(CELL));
            items.drawItem(batch, crafting.get(i), x, y, px(CELL));
        }

        // Mui ten tro sang o ket qua.
        float arrowY = slotY(CRAFT_Y + CELL - 1f, 0f);
        ui.rect(batch, MinecraftUi.SLOT_DARK, slotX(ARROW_X), arrowY, px(12f), px(2f));
        ui.rect(batch, MinecraftUi.SLOT_DARK, slotX(ARROW_X + 9f), arrowY + px(2f), px(2f), px(2f));
        ui.rect(batch, MinecraftUi.SLOT_DARK, slotX(ARROW_X + 9f), arrowY - px(2f), px(2f), px(2f));
        batch.setColor(Color.WHITE);

        float x = slotX(RESULT_X);
        float y = slotY(RESULT_Y, CELL);
        ui.slot(batch, x, y, px(CELL));
        items.drawItem(batch, crafting.result(), x, y, px(CELL));
    }

    private void drawStorage(SpriteBatch batch, GameMode mode) {
        for (int row = 0; row < Inventory.STORAGE_ROWS; row++) {
            for (int column = 0; column < Inventory.HOTBAR_SIZE; column++) {
                float x = slotX(STORAGE_X + column * CELL);
                float y = slotY(STORAGE_Y + row * CELL, CELL);
                ui.slot(batch, x, y, px(CELL));

                int index = row * Inventory.HOTBAR_SIZE + column;
                if (mode.isCreative()) {
                    if (index < palette.length) {
                        items.drawBlockIcon(batch, palette[index], x, y, px(CELL));
                    }
                } else {
                    items.drawItem(batch, inventory.get(Inventory.HOTBAR_SIZE + index), x, y, px(CELL));
                }
            }
        }
    }

    private void drawHotbarRow(SpriteBatch batch) {
        for (int column = 0; column < Inventory.HOTBAR_SIZE; column++) {
            float x = slotX(STORAGE_X + column * CELL);
            float y = slotY(HOTBAR_Y, CELL);
            ui.slot(batch, x, y, px(CELL));
            items.drawItem(batch, inventory.get(column), x, y, px(CELL));
        }
    }

    /** Ve mon do dang dinh o dau con tro chuot (ve sau cung nen luon nam tren). */
    public void drawCarried(SpriteBatch batch, float mouseX, float mouseY) {
        if (open) {
            items.drawItem(batch, inventory.carried(),
                    mouseX - px(CELL) * 0.5f, mouseY - px(CELL) * 0.5f, px(CELL));
        }
    }

    // -------------------------------------------------------------- bat chuot

    /**
     * @param mouseY toa do y da doi ve he giao dien (goc o duoi man hinh)
     * @return true neu cu bam roi trung mot o
     */
    public boolean click(float mouseX, float mouseY, int screenWidth, int screenHeight, GameMode mode) {
        if (!open) {
            return false;
        }
        layout(screenWidth, screenHeight);
        float size = px(CELL);

        // O ket qua che tao: chi lay ra duoc, khong bo vao duoc.
        if (inside(mouseX, mouseY, slotX(RESULT_X), slotY(RESULT_Y, CELL), size)) {
            ItemStack made = crafting.take();
            if (made != null) {
                for (int i = 0; i < made.count(); i++) {
                    inventory.add(made.block());
                }
            }
            return true;
        }

        for (int i = 0; i < Crafting.GRID; i++) {
            float x = slotX(CRAFT_X + (i % 2) * CELL);
            float y = slotY(CRAFT_Y + (i / 2) * CELL, CELL);
            if (inside(mouseX, mouseY, x, y, size)) {
                ItemStack carried = inventory.carried();
                inventory.setCarried(crafting.get(i));
                crafting.set(i, carried);
                return true;
            }
        }

        for (int row = 0; row < Inventory.STORAGE_ROWS; row++) {
            for (int column = 0; column < Inventory.HOTBAR_SIZE; column++) {
                float x = slotX(STORAGE_X + column * CELL);
                float y = slotY(STORAGE_Y + row * CELL, CELL);
                if (!inside(mouseX, mouseY, x, y, size)) {
                    continue;
                }
                int index = row * Inventory.HOTBAR_SIZE + column;
                if (mode.isCreative()) {
                    if (index < palette.length) {
                        inventory.setCarried(new ItemStack(palette[index], ItemStack.MAX_COUNT));
                    }
                } else {
                    inventory.clickSlot(Inventory.HOTBAR_SIZE + index);
                }
                return true;
            }
        }

        for (int column = 0; column < Inventory.HOTBAR_SIZE; column++) {
            if (inside(mouseX, mouseY, slotX(STORAGE_X + column * CELL), slotY(HOTBAR_Y, CELL), size)) {
                inventory.clickSlot(column);
                return true;
            }
        }
        return false;
    }

    private boolean inside(float mouseX, float mouseY, float x, float y, float size) {
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }
}
