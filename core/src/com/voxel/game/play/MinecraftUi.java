package com.voxel.game.play;

import com.badlogic.gdx.Gdx;
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

    // Bang mau "toi + diem nhan tim" hoc tu goi Better Modded GUI (Better MC): panel than chi
    // mau, o do gan den, khung o dang chon mau magenta ruc voi vien trang.
    public static final Color PANEL_BG = rgb(0x2E2C37);
    public static final Color PANEL_LIGHT = rgb(0x76747C);
    public static final Color PANEL_DARK = rgb(0x131319);
    public static final Color SLOT_BG = rgb(0x1B1A21);
    public static final Color SLOT_DARK = rgb(0x000000);
    /** Chu tren panel: nen toi nen chu phai SANG. */
    public static final Color TEXT_DARK = rgb(0xDAD8E0);

    /** Vien ngoai va vach ngan cua thanh nhanh. */
    public static final Color BAR_EDGE = rgb(0x4A4753);
    public static final Color BAR_DIVIDER = rgb(0x47444E);
    /** Diem nhan magenta cho o dang chon; ACCENT_EDGE la vien trang sat mep. */
    public static final Color SELECTION = rgb(0xEF51D5);
    public static final Color ACCENT_EDGE = rgb(0xFFFFFF);
    public static final Color ACCENT_DARK = rgb(0x8234AC);

    public static final Color XP_GREEN = rgb(0x80FF20);
    public static final Color XP_GREEN_DARK = rgb(0x5FCC00);
    public static final Color XP_EMPTY = rgb(0x1B2B0B);

    /** Anh bong bong to hon hinh 2 diem de con cho ve vien. */
    private static final int ICON_PAD = 11;

    private static final int OUTLINE = argb(0, 0, 0);
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
    /** Lop toi mo dan tu giua ra vien man hinh - khung hinh "dien anh" hoc tu shader Complementary. */
    public final Texture vignette;

    public MinecraftUi() {
        // Trai tim va dui ga lay tu goi "White Hunger": moi bieu tuong la mot anh 9x9
        // rieng (nen xam _empty ve truoc, ban trang _full/_half da len tren).
        heartFull = load("heart_full");
        heartHalf = load("heart_half");
        heartEmpty = load("heart_empty");
        hungerFull = load("food_full");
        hungerHalf = load("food_half");
        hungerEmpty = load("food_empty");
        bubbleFull = bubble(false);
        bubblePop = bubble(true);
        white = onePixel();

        // Vignette: loc Linear cho mo dan muot khi keo gian toan man hinh (khong bi vo o vuong).
        vignette = new Texture(Gdx.files.internal("data/vignette.png"));
        vignette.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    // ------------------------------------------------------------- bieu tuong

    /** Nap mot bieu tuong 9x9 tu data/ va giu net vuong (khong lam mo khi phong to). */
    private static Texture load(String name) {
        Texture texture = new Texture(Gdx.files.internal("data/" + name + ".png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
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
        vignette.dispose();
    }
}
