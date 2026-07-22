package com.voxel.game.play;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/**
 * Bo "vo" giao dien ve theo Minecraft.
 *
 * TAT CA mau va kich thuoc o day deu DO TRUC TIEP tu hai anh chup Minecraft that
 * (1.png cho thanh trang thai, 2.png cho bang kho do):
 *
 * <pre>
 *   ty le giao dien           = 3 (thanh nhanh 182 px logic = 546 px man hinh)
 *   vien thanh nhanh          = #909090      vach ngan giua hai o = #656565
 *   ruot o tren thanh nhanh   = den mo 62%   (do duoc #302E2B tren nen go)
 *   khung o dang chon         = #C6C6C6, day 2 px logic
 *   thanh xp: da day #80FF20  chua day #1B2B0B  (KHONG phai xam)
 *   tam bang kho do #C6C6C6, o do #8B8B8B, canh toi #373737
 * </pre>
 *
 * Hinh chu nhat ve thang bang mot diem anh trang keo gian ra nen sac net o moi co
 * man hinh. Rieng bieu tuong (trai tim, dui ga, bong bong) la tranh diem anh.
 */
public final class MinecraftUi implements Disposable {

    /** Mot diem anh giao dien cua Minecraft bang bay nhieu diem anh that. */
    public static final float SCALE = 3f;

    public static final Color PANEL_BG = rgb(0xC6C6C6);
    public static final Color PANEL_LIGHT = rgb(0xFFFFFF);
    public static final Color PANEL_DARK = rgb(0x555555);
    public static final Color SLOT_BG = rgb(0x8B8B8B);
    public static final Color SLOT_DARK = rgb(0x373737);
    public static final Color TEXT_DARK = rgb(0x404040);

    /** Vien ngoai va vach ngan cua thanh nhanh. */
    public static final Color BAR_EDGE = rgb(0x909090);
    public static final Color BAR_DIVIDER = rgb(0x656565);
    /** Khung bao quanh o dang cam tren tay. */
    public static final Color SELECTION = rgb(0xC6C6C6);

    public static final Color XP_GREEN = rgb(0x80FF20);
    public static final Color XP_GREEN_DARK = rgb(0x5FCC00);
    public static final Color XP_EMPTY = rgb(0x1B2B0B);

    /** Hinh trai tim 9x9. R = than tim, H = diem sang goc tren-trai moi buou. */
    private static final String[] HEART = {
            ".........",
            "..HR.HR..",
            ".RRRRRRR.",
            ".RRRRRRR.",
            ".RRRRRRR.",
            "..RRRRR..",
            "...RRR...",
            "....R....",
            "........."
    };

    /**
     * Dui ga 9x9, NGHIENG nhu trong Minecraft: cuc thit chech len goc tren-phai,
     * khuc xuong tho ra goc duoi-trai.
     * L = thit mat tren (sang), D = thit mat duoi (toi), B = xuong, W = diem sang.
     */
    private static final String[] HUNGER = {
            ".........",
            "....LLL..",
            "...LLLLL.",
            "...LLLLL.",
            "..DDDDDD.",
            "..DDDDD..",
            ".WDDD....",
            "BW.......",
            "........."
    };

    private static final int ICON = 9;
    /** Anh to hon hinh 2 diem de con cho ve vien. */
    private static final int ICON_PAD = ICON + 2;

    private static final int OUTLINE = argb(0, 0, 0);
    private static final int HEART_RED = argb(255, 0, 0);
    private static final int HEART_SHADE = argb(193, 0, 0);
    private static final int HEART_SHINE = argb(255, 107, 107);
    private static final int MEAT = argb(217, 160, 102);
    private static final int MEAT_SHADE = argb(143, 86, 59);
    private static final int BONE = argb(238, 195, 154);
    private static final int BONE_SHINE = argb(255, 255, 255);
    private static final int EMPTY_ICON = argb(76, 76, 76);
    private static final int BUBBLE = argb(150, 220, 255);
    private static final int BUBBLE_SHINE = argb(255, 255, 255);
    private static final int BUBBLE_EDGE = argb(70, 130, 190);

    public final Texture heartFull;
    public final Texture heartHalf;
    public final Texture heartEmpty;
    public final Texture hungerFull;
    public final Texture hungerHalf;
    public final Texture hungerEmpty;
    public final Texture bubbleFull;
    public final Texture bubblePop;
    /** Mot diem anh trang: keo gian ra de ve moi hinh chu nhat. */
    public final Texture white;

    public MinecraftUi() {
        heartFull = icon(HEART, false, false);
        heartHalf = icon(HEART, true, false);
        heartEmpty = icon(HEART, false, true);
        hungerFull = icon(HUNGER, false, false);
        hungerHalf = icon(HUNGER, true, false);
        hungerEmpty = icon(HUNGER, false, true);
        bubbleFull = bubble(false);
        bubblePop = bubble(true);
        white = onePixel();
    }

    // ------------------------------------------------------------- bieu tuong

    /**
     * Ve mot bieu tuong tu tranh chu.
     *
     * @param half  chi to mau nua trai, nua phai de lo nen xam
     * @param empty to toan bo bang mau xam (het mau / het do no)
     */
    private Texture icon(String[] art, boolean half, boolean empty) {
        Pixmap pixmap = blank(ICON_PAD, ICON_PAD);

        for (int row = 0; row < art.length; row++) {
            for (int col = 0; col < art[row].length(); col++) {
                char cell = art[row].charAt(col);
                if (cell == '.') {
                    continue;
                }
                int color = (empty || (half && col > 4)) ? EMPTY_ICON : shade(cell, row, col);
                pixmap.drawPixel(col + 1, row + 1, color);
            }
        }
        outline(pixmap);
        return upload(pixmap);
    }

    /** Mau cua mot diem anh, da tinh ca dam nhat de hinh co khoi. */
    private int shade(char cell, int row, int col) {
        switch (cell) {
            case 'R':
                return row >= 6 ? HEART_SHADE : HEART_RED;
            case 'H':
                return HEART_SHINE;
            case 'L':
                return MEAT;
            case 'D':
                return MEAT_SHADE;
            case 'B':
                return BONE;
            case 'W':
                return BONE_SHINE;
            default:
                return EMPTY_ICON;
        }
    }

    /** Bong bong khi: hinh tron nho, ban "vo" thi khuyet mot mieng. */
    private Texture bubble(boolean popped) {
        Pixmap pixmap = blank(ICON_PAD, ICON_PAD);
        float center = ICON_PAD / 2f - 0.5f;
        float radius = popped ? 2.6f : 4.1f;

        for (int y = 0; y < ICON_PAD; y++) {
            for (int x = 0; x < ICON_PAD; x++) {
                float dx = x - center;
                float dy = y - center;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance > radius) {
                    continue;
                }
                int color = distance > radius - 1f ? BUBBLE_EDGE : BUBBLE;
                if (dx < -1f && dy < -1f) {
                    color = BUBBLE_SHINE;
                }
                pixmap.drawPixel(x, y, color);
            }
        }
        outline(pixmap);
        return upload(pixmap);
    }

    /**
     * To vien den quanh hinh. Chi xet BON huong (khong xet cheo) nen vien om sat
     * hinh - xet ca cheo se lam bieu tuong bi phinh to va mat net.
     */
    private void outline(Pixmap pixmap) {
        int w = pixmap.getWidth();
        int h = pixmap.getHeight();
        boolean[][] filled = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                filled[y][x] = (pixmap.getPixel(x, y) & 0xFF) != 0;
            }
        }
        int[][] near = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (filled[y][x]) {
                    continue;
                }
                for (int[] step : near) {
                    int nx = x + step[0];
                    int ny = y + step[1];
                    if (nx >= 0 && ny >= 0 && nx < w && ny < h && filled[ny][nx]) {
                        pixmap.drawPixel(x, y, OUTLINE);
                        break;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------ ve khung va o do

    public void rect(SpriteBatch batch, Color color, float x, float y, float width, float height) {
        batch.setColor(color);
        batch.draw(white, x, y, width, height);
    }

    public void rect(SpriteBatch batch, Color color, float alpha,
                     float x, float y, float width, float height) {
        batch.setColor(color.r, color.g, color.b, alpha);
        batch.draw(white, x, y, width, height);
    }

    /**
     * Tam bang cua Minecraft: nen xam, canh tren-trai sang va canh duoi-phai toi
     * nen trong nhu mot mieng kim loai NOI len.
     */
    public void panel(SpriteBatch batch, float x, float y, float width, float height) {
        float edge = SCALE * 2f;
        rect(batch, PANEL_BG, x, y, width, height);
        rect(batch, PANEL_LIGHT, x, y + height - edge, width - edge, edge);
        rect(batch, PANEL_LIGHT, x, y + edge, edge, height - edge);
        rect(batch, PANEL_DARK, x + edge, y, width - edge, edge);
        rect(batch, PANEL_DARK, x + width - edge, y, edge, height - edge);
        batch.setColor(Color.WHITE);
    }

    /** O dung do: canh tren-trai TOI, duoi-phai SANG - trong nhu bi LOM xuong. */
    public void slot(SpriteBatch batch, float x, float y, float size) {
        float edge = SCALE;
        rect(batch, SLOT_BG, x, y, size, size);
        rect(batch, SLOT_DARK, x, y + size - edge, size, edge);
        rect(batch, SLOT_DARK, x, y, edge, size);
        rect(batch, PANEL_LIGHT, x + edge, y, size - edge, edge);
        rect(batch, PANEL_LIGHT, x + size - edge, y + edge, edge, size - edge);
        batch.setColor(Color.WHITE);
    }

    /** Khung vien rong; mau lay tu mau dang dat cua batch. */
    public void frame(SpriteBatch batch, float x, float y, float width, float height, float thickness) {
        batch.draw(white, x, y, width, thickness);
        batch.draw(white, x, y + height - thickness, width, thickness);
        batch.draw(white, x, y, thickness, height);
        batch.draw(white, x + width - thickness, y, thickness, height);
    }

    private static Pixmap blank(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        return pixmap;
    }

    private static Texture onePixel() {
        Pixmap pixmap = blank(1, 1);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        return upload(pixmap);
    }

    private static Texture upload(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private static int argb(int r, int g, int b) {
        return (r << 24) | (g << 16) | (b << 8) | 0xFF;
    }

    private static Color rgb(int hex) {
        return new Color(((hex >> 16) & 0xFF) / 255f, ((hex >> 8) & 0xFF) / 255f,
                (hex & 0xFF) / 255f, 1f);
    }

    @Override
    public void dispose() {
        heartFull.dispose();
        heartHalf.dispose();
        heartEmpty.dispose();
        hungerFull.dispose();
        hungerHalf.dispose();
        hungerEmpty.dispose();
        bubbleFull.dispose();
        bubblePop.dispose();
        white.dispose();
    }
}
