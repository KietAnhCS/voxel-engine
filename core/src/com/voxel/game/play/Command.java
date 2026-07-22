package com.voxel.game.play;

/**
 * A command typed in the chat box, for example {@code /gamemode 1}.
 *
 * This is the Command pattern: every command is its own object, {@link CommandConsole}
 * only keeps a lookup table "command name -> object" and calls {@link #run} without
 * knowing what the command does inside. Adding a new command means registering one
 * more, not editing the console.
 */
public interface Command {

    /**
     * @param args the words after the command name (already split on spaces)
     * @return the line reported back to the player, null if nothing to report
     */
    String run(String[] args);

    /** A short one-line description shown in {@code /help}. */
    String usage();
}
