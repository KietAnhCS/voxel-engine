package com.voxel.game.play;

/**
 * The two Minecraft-like game modes. The id number is the number used in the
 * {@code /gamemode 0} and {@code /gamemode 1} commands.
 */
public enum GameMode {

    SURVIVAL(0, "survival", "survival"),
    CREATIVE(1, "creative", "creative");

    private final int id;
    private final String key;
    private final String label;

    GameMode(int id, String key, String label) {
        this.id = id;
        this.key = key;
        this.label = label;
    }

    public int id() {
        return id;
    }

    public String label() {
        return label;
    }

    /** In survival you lose health and need the block in your bag to place it. */
    public boolean isSurvival() {
        return this == SURVIVAL;
    }

    /** In creative you can fly, never die, and blocks are unlimited. */
    public boolean isCreative() {
        return this == CREATIVE;
    }

    /**
     * Reads the argument of the /gamemode command: accepts both the number
     * ("0", "1") and the name ("survival", "creative", "s", "c") like Minecraft.
     *
     * @return null if it cannot be understood
     */
    public static GameMode parse(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim().toLowerCase();
        for (GameMode mode : values()) {
            if (value.equals(String.valueOf(mode.id))
                    || value.equals(mode.key)
                    || value.equals(mode.key.substring(0, 1))) {
                return mode;
            }
        }
        return null;
    }
}
