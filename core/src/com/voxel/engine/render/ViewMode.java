package com.voxel.engine.render;

/**
 * The three Minecraft view modes, cycled with the F5 key.
 */
public enum ViewMode {

    /** Seen through the player's own eyes - the body is not visible. */
    FIRST_PERSON("first person", 0f, false),
    /** The camera pulls back behind the player. */
    THIRD_BACK("third person - back", 4.5f, false),
    /** The camera moves in front and looks back at the player. */
    THIRD_FRONT("third person - front", 4.5f, true);

    private final String label;
    private final float distance;
    private final boolean facingPlayer;

    ViewMode(String label, float distance, boolean facingPlayer) {
        this.label = label;
        this.distance = distance;
        this.facingPlayer = facingPlayer;
    }

    public String label() {
        return label;
    }

    /** How many blocks the camera pulls away from the player. */
    public float distance() {
        return distance;
    }

    /** true when the camera stands in front and looks back at the player. */
    public boolean facingPlayer() {
        return facingPlayer;
    }

    public boolean showsPlayer() {
        return this != FIRST_PERSON;
    }

    /** The next view mode when F5 is pressed. */
    public ViewMode next() {
        ViewMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
