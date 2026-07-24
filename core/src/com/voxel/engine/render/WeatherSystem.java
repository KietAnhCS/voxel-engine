package com.voxel.engine.render;

import com.badlogic.gdx.graphics.Color;

/**
 * The world's weather: CLEAR or RAIN, exactly like Minecraft's /weather command.
 *
 * <p>The switch is never instant: {@link #rain()} is a 0..1 value that glides toward the
 * target over a few seconds, and everything visual reads from it - the sky grays out, the
 * fog thickens, the daylight dims, the clouds close up and the rain streaks fade in. One
 * number drives the whole mood change.
 */
public final class WeatherSystem {

    /** Seconds for a full clear -> rain transition. */
    private static final float TRANSITION_TIME = 4f;
    /**
     * Rainy sky / fog color that the day sky blends toward: a BRIGHT gray-white, so the
     * thicker rain fog reads as white mist rolling in rather than the world going dark.
     */
    private static final Color RAIN_SKY = new Color(0.72f, 0.75f, 0.79f, 1f);

    private final Color blendedSky = new Color();

    private float rain;
    private boolean raining;

    /** Moves the transition one frame forward. */
    public void update(float delta) {
        float target = raining ? 1f : 0f;
        float step = delta / TRANSITION_TIME;
        if (rain < target) {
            rain = Math.min(target, rain + step);
        } else if (rain > target) {
            rain = Math.max(target, rain - step);
        }
    }

    public void setRaining(boolean raining) {
        this.raining = raining;
    }

    public boolean isRaining() {
        return raining;
    }

    /** How rainy it looks right now: 0 = clear, 1 = full downpour. */
    public float rain() {
        return rain;
    }

    /** The sky color with the rain gray mixed in. */
    public Color blendSky(Color daySky) {
        return blendedSky.set(daySky).lerp(RAIN_SKY, rain * 0.75f);
    }

    /** Multiplier for the sky light - rain dims the world a little (the mist stays white). */
    public float daylightFactor() {
        return 1f - 0.18f * rain;
    }

    /** Multiplier for the fog density - mist closes in while it rains. */
    public float fogFactor() {
        return 1f + 2.6f * rain;
    }
}
