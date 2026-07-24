package com.voxel.game.play;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.voxel.engine.block.Block;

import java.util.ArrayList;
import java.util.List;

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
    /** Hai cu bam cach nhau duoi bay nhieu mili giay thi tinh la mot cu bam dup. */
    private static final long DOUBLE_CLICK_MS = 260L;

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

    /** Cac o con tro da di qua trong luot keo hien tai, de chia deu chong do luc nha chuot. */
    private final List<Slot> dragged = new ArrayList<Slot>();
    private boolean dragging;
    private boolean dragRight;
    /** Luc bam chuot trai lan truoc, de nhan ra cu bam dup. */
    private long lastClickAt;

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
     * Bam chuot xuong mot o.
     *
     * <p>Neu tay dang cam do thi CHUA lam gi ngay ma mo mot luot "keo": moi o con tro di
     * qua se duoc ghi lai, den luc nha chuot moi chia deu chong do cho tat ca cac o do -
     * dung nhu Minecraft. Nha ngay tai cho (chi mot o) thi coi nhu mot cu bam binh thuong.
     *
     * @param mouseY toa do y da doi ve he giao dien (goc o duoi man hinh)
     * @param shift  dang giu SHIFT - chuyen nhanh ca chong do
     * @return true neu cu bam roi trung mot o
     */
    public boolean touchDown(float mouseX, float mouseY, int screenWidth, int screenHeight,
                             GameMode mode, boolean rightButton, boolean shift) {
        if (!open) {
            return false;
        }
        layout(screenWidth, screenHeight);
        Slot slot = locate(mouseX, mouseY, mode);
        if (slot == null) {
            return false;
        }

        if (shift) {
            quickMove(slot);
            lastClickAt = 0L;
            return true;
        }

        // Bam dup chuot trai khi dang cam do: gom het khoi cung loai lai thanh mot chong.
        long now = System.currentTimeMillis();
        boolean doubleClick = !rightButton
                && now - lastClickAt <= DOUBLE_CLICK_MS
                && inventory.carried() != null;
        lastClickAt = rightButton ? 0L : now;
        if (doubleClick) {
            inventory.collectIntoCarried();
            return true;
        }

        if (inventory.carried() != null && slot.canReceive()) {
            dragging = true;
            dragRight = rightButton;
            dragged.clear();
            dragged.add(slot);
            return true;
        }
        apply(slot, rightButton);
        return true;
    }

    /** Con tro truot qua mot o moi trong luc dang giu chuot: ghi o do vao danh sach chia. */
    public void touchDragged(float mouseX, float mouseY, int screenWidth, int screenHeight, GameMode mode) {
        if (!open || !dragging) {
            return;
        }
        layout(screenWidth, screenHeight);
        Slot slot = locate(mouseX, mouseY, mode);
        if (slot == null || !slot.canReceive()) {
            return;
        }
        for (Slot visited : dragged) {
            if (visited.sameAs(slot)) {
                return;
            }
        }
        dragged.add(slot);
    }

    /** Nha chuot: keo qua nhieu o thi chia deu, chi mot o thi la cu bam binh thuong. */
    public void touchUp() {
        if (!dragging) {
            return;
        }
        dragging = false;
        if (dragged.size() >= 2) {
            distribute();
        } else if (dragged.size() == 1) {
            apply(dragged.get(0), dragRight);
        }
        dragged.clear();
    }

    /**
     * Chia deu chong do dang cam cho tat ca cac o vua keo qua. O nao khong nhan duoc
     * (dang chua loai khac, hoac da day) thi bi bo qua, phan thua van dinh tren con tro.
     */
    private void distribute() {
        ItemStack carried = inventory.carried();
        if (carried == null || carried.isEmpty()) {
            return;
        }
        Block block = carried.block();
        int share = Math.max(1, carried.count() / dragged.size());

        for (Slot slot : dragged) {
            if (carried.count() <= 0) {
                break;
            }
            int put = Math.min(share, carried.count());
            ItemStack target = slot.stack();
            if (target == null) {
                slot.setStack(new ItemStack(block, put));
            } else if (target.isSameBlock(block)) {
                put -= target.add(put);
                slot.setStack(target);
            } else {
                continue;  // o nay dang chua loai khac - bo qua
            }
            for (int i = 0; i < put; i++) {
                carried.shrink();
            }
        }
        inventory.setCarried(carried.isEmpty() ? null : carried);
    }

    /** Mot cu bam binh thuong len mot o. */
    private void apply(Slot slot, boolean rightButton) {
        switch (slot.area) {
            case RESULT:
                takeResult();
                break;
            case PALETTE:
                inventory.setCarried(new ItemStack(palette[slot.index], ItemStack.MAX_COUNT));
                break;
            case CRAFT:
                if (rightButton) {
                    placeOneInCraft(slot.index);
                } else {
                    ItemStack carried = inventory.carried();
                    inventory.setCarried(crafting.get(slot.index));
                    crafting.set(slot.index, carried);
                }
                break;
            default:
                if (rightButton) {
                    inventory.clickSlotRight(slot.index);
                } else {
                    inventory.clickSlot(slot.index);
                }
                break;
        }
    }

    /** Chuot phai vao o che tao: tha xuong dung mot khoi. */
    private void placeOneInCraft(int index) {
        ItemStack carried = inventory.carried();
        if (carried == null || carried.isEmpty()) {
            return;
        }
        ItemStack target = crafting.get(index);
        if (target == null) {
            crafting.set(index, new ItemStack(carried.block(), 1));
        } else if (target.isSameBlock(carried.block()) && target.room() > 0) {
            target.add(1);
            crafting.set(index, target);
        } else {
            return;
        }
        carried.shrink();
        inventory.setCarried(carried.isEmpty() ? null : carried);
    }

    /** Lay mon vua che tao ra, do thang vao tui. */
    private void takeResult() {
        ItemStack made = crafting.take();
        if (made != null) {
            inventory.addStack(made.block(), made.count());
        }
    }

    /**
     * Shift + bam: o ket qua thi che tao lien tuc cho toi khi het nguyen lieu hoac day tui,
     * o che tao thi tra do ve tui, o tui thi nhay qua khu vuc con lai.
     */
    private void quickMove(Slot slot) {
        switch (slot.area) {
            case RESULT:
                // Che tao het co: moi vong lam mot me, dung khi khong con cong thuc nao khop.
                while (crafting.result() != null) {
                    ItemStack made = crafting.take();
                    if (made == null || inventory.addStack(made.block(), made.count()) > 0) {
                        break;  // tui day roi, thoi.
                    }
                }
                break;
            case CRAFT:
                ItemStack stack = crafting.get(slot.index);
                if (stack != null) {
                    int leftOver = inventory.addStack(stack.block(), stack.count());
                    crafting.set(slot.index, leftOver > 0 ? new ItemStack(stack.block(), leftOver) : null);
                }
                break;
            case PALETTE:
                inventory.addStack(palette[slot.index], ItemStack.MAX_COUNT);
                break;
            default:
                inventory.quickMove(slot.index);
                break;
        }
    }

    // ------------------------------------------------------- tim o duoi con tro

    /** Khu vuc ma mot o thuoc ve. */
    private enum Area { CRAFT, RESULT, BAG, PALETTE }

    /**
     * Mot o dang nam duoi con tro chuot. Nho co lop nay ma phan bat chuot khong can biet
     * o do la cua tui do hay cua bang che tao - doc ghi deu qua {@link #stack}/{@link #setStack}.
     */
    private final class Slot {
        final Area area;
        final int index;

        Slot(Area area, int index) {
            this.area = area;
            this.index = index;
        }

        boolean sameAs(Slot other) {
            return area == other.area && index == other.index;
        }

        /** O ket qua che tao va kho khoi sang tao chi lay ra duoc, khong do vao duoc. */
        boolean canReceive() {
            return area == Area.CRAFT || area == Area.BAG;
        }

        ItemStack stack() {
            return area == Area.CRAFT ? crafting.get(index) : inventory.get(index);
        }

        void setStack(ItemStack stack) {
            if (area == Area.CRAFT) {
                crafting.set(index, stack);
            } else {
                inventory.set(index, stack);
            }
        }
    }

    /** O nao dang nam duoi con tro, hoac null neu tro vao cho trong. */
    private Slot locate(float mouseX, float mouseY, GameMode mode) {
        float size = px(CELL);

        if (inside(mouseX, mouseY, slotX(RESULT_X), slotY(RESULT_Y, CELL), size)) {
            return new Slot(Area.RESULT, 0);
        }
        for (int i = 0; i < Crafting.GRID; i++) {
            float x = slotX(CRAFT_X + (i % 2) * CELL);
            float y = slotY(CRAFT_Y + (i / 2) * CELL, CELL);
            if (inside(mouseX, mouseY, x, y, size)) {
                return new Slot(Area.CRAFT, i);
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
                if (!mode.isCreative()) {
                    return new Slot(Area.BAG, Inventory.HOTBAR_SIZE + index);
                }
                return index < palette.length ? new Slot(Area.PALETTE, index) : null;
            }
        }
        for (int column = 0; column < Inventory.HOTBAR_SIZE; column++) {
            if (inside(mouseX, mouseY, slotX(STORAGE_X + column * CELL), slotY(HOTBAR_Y, CELL), size)) {
                return new Slot(Area.BAG, column);
            }
        }
        return null;
    }

    private boolean inside(float mouseX, float mouseY, float x, float y, float size) {
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }
}
