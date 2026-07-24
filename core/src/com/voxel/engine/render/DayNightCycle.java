package com.voxel.engine.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

/**
 * Dong ho ngay dem cua the gioi.
 *
 * <p>Mat troi quay quanh the gioi y nhu trai dat quay: moc o phia DONG (+X), leo len dinh
 * dau luc gio ngo, lan xuong phia TAY (-X), roi di duoi chan the gioi suot dem. Mat trang
 * di doi dien mat troi nen cu mat troi lan la mat trang moc.
 *
 * <p>Moi thu khac deu suy ra tu DO CAO cua mat troi:
 * <pre>
 *   cao   > 0.35 : giua ban ngay - troi xanh, sang nhat
 *   quanh 0      : binh minh / hoang hon - troi do cam
 *   cao   &lt; -0.2 : ban dem - troi xanh tham, zombie mo cua chuong
 * </pre>
 *
 * <p>Thoi gian chay theo mot vong {@link #DAY_LENGTH} giay; {@code time} 0 la binh minh,
 * 0.25 giua trua, 0.5 hoang hon, 0.75 nua dem.
 */
public final class DayNightCycle {

    /** Mot ngay dem keo dai bao nhieu giay that (Minecraft la 20 phut, o day 10 phut). */
    private static final float DAY_LENGTH = 600f;
    /** Bat dau van choi vao buoi sang som cho de nhin. */
    private static final float START_TIME = 0.06f;

    /** Do sang giua trua va giua dem (nhan vao anh sang bau troi cua ca the gioi). */
    private static final float FULL_DAY = 1f;
    private static final float FULL_NIGHT = 0.16f;
    /** Mat troi cao hon muc nay la sang han, thap hon -{@link #DUSK_BAND} la toi han. */
    private static final float DAWN_BAND = 0.35f;
    private static final float DUSK_BAND = 0.2f;

    private static final Color DAY_SKY = new Color(0.44f, 0.66f, 0.94f, 1f);
    private static final Color SUNSET_SKY = new Color(0.95f, 0.48f, 0.26f, 1f);
    private static final Color NIGHT_SKY = new Color(0.03f, 0.04f, 0.10f, 1f);

    private final Vector3 sunDirection = new Vector3();
    private final Color skyColor = new Color();

    private float time = START_TIME;

    public DayNightCycle() {
        recompute();
    }

    /** Chay dong ho mot khung hinh. */
    public void advance(float delta) {
        time = (time + delta / DAY_LENGTH) % 1f;
        recompute();
    }

    private void recompute() {
        double angle = time * Math.PI * 2.0;
        sunDirection.set((float) Math.cos(angle), (float) Math.sin(angle), 0f);
        mixSky(skyColor, sunDirection.y);
    }

    /** Nhay thang toi mot thoi diem trong ngay (0 binh minh, 0.25 trua, 0.5 hoang hon, 0.75 nua dem). */
    public void setTime(float value) {
        time = ((value % 1f) + 1f) % 1f;
        recompute();
    }

    /** Huong tu the gioi toi MAT TROI (da chuan hoa). Mat trang nam huong nguoc lai. */
    public Vector3 sunDirection() {
        return sunDirection;
    }

    /**
     * Do sang cua anh sang BAU TROI, 0.16 luc nua dem toi 1 luc giua trua.
     * Shader nhan so nay vao phan anh sang troi, khong dung toi quang duoc.
     */
    public float daylight() {
        return brightnessAt(sunDirection.y);
    }

    /** Mau bau troi (dong thoi la mau suong mu) tai thoi diem nay. */
    public Color skyColor() {
        return skyColor;
    }

    /** Ban dem theo nghia cua quai vat: mat troi da khuat han duoi duong chan troi. */
    public boolean isNight() {
        return sunDirection.y < -0.05f;
    }

    /** Gio trong game kieu "13:45" de hien len bang go loi F3. */
    public String clockLabel() {
        // time 0 la binh minh, quy uoc la 6 gio sang.
        float hours = (time * 24f + 6f) % 24f;
        return String.format("%02d:%02d", (int) hours, (int) ((hours - (int) hours) * 60f));
    }

    /** Do sang ung voi mot do cao mat troi cho truoc. */
    private static float brightnessAt(float sunHeight) {
        if (sunHeight >= DAWN_BAND) {
            return FULL_DAY;
        }
        if (sunHeight <= -DUSK_BAND) {
            return FULL_NIGHT;
        }
        float t = (sunHeight + DUSK_BAND) / (DAWN_BAND + DUSK_BAND);
        return FULL_NIGHT + (FULL_DAY - FULL_NIGHT) * t;
    }

    /**
     * Mau troi: mat troi cao thi xanh, sat duong chan troi thi do cam (binh minh va hoang
     * hon dung chung mot dai mau vi mat troi o cung do cao), khuat han thi xanh tham.
     */
    private static void mixSky(Color out, float sunHeight) {
        if (sunHeight >= DAWN_BAND) {
            out.set(DAY_SKY);
        } else if (sunHeight >= 0f) {
            out.set(SUNSET_SKY).lerp(DAY_SKY, sunHeight / DAWN_BAND);
        } else if (sunHeight >= -DUSK_BAND) {
            out.set(NIGHT_SKY).lerp(SUNSET_SKY, 1f + sunHeight / DUSK_BAND);
        } else {
            out.set(NIGHT_SKY);
        }
    }
}
