package com.voxel.game.play;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minecraft-like chat box and command line: pressing "/" shows a text field at the
 * bottom of the screen, ENTER runs what was typed, ESC cancels.
 *
 * Inside there are two data structures:
 * <ul>
 *   <li>a HASH MAP (LinkedHashMap) mapping a command name to its {@link Command}
 *       object - lookup is O(1) no matter how many commands there are, and it still
 *       keeps the registration order so {@code /help} prints readably;</li>
 *   <li>a LIST of the lines shown, where old lines fade out and then disappear.</li>
 * </ul>
 */
public final class CommandConsole {

    /** Maximum number of chat lines shown at once. */
    private static final int MAX_LINES = 8;
    /** How long (seconds) a chat line lives before it fades out. */
    private static final float LINE_LIFETIME = 9f;
    private static final float FADE_TIME = 1.5f;

    /** One line of text in the chat box, with its remaining time. */
    public static final class Line {
        public final String text;
        float life = LINE_LIFETIME;

        Line(String text) {
            this.text = text;
        }

        /** Text opacity: 1 is fully visible, 0 is completely gone. */
        public float alpha() {
            return Math.min(1f, life / FADE_TIME);
        }
    }

    private final Map<String, Command> commands = new LinkedHashMap<String, Command>();
    private final List<Line> lines = new ArrayList<Line>();
    private final StringBuilder input = new StringBuilder();

    private boolean open;

    public void register(String name, Command command) {
        commands.put(name, command);
    }

    public Map<String, Command> commands() {
        return commands;
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Opens the text field.
     *
     * @param prefix text already in the field - pressing "/" opens it with a slash
     */
    public void open(String prefix) {
        open = true;
        input.setLength(0);
        input.append(prefix);
    }

    public void close() {
        open = false;
        input.setLength(0);
    }

    public String input() {
        return input.toString();
    }

    public void type(char character) {
        if (character >= ' ' && character != 127 && input.length() < 100) {
            input.append(character);
        }
    }

    public void backspace() {
        if (input.length() > 0) {
            input.setLength(input.length() - 1);
        }
    }

    /** Runs the typed line and then closes the chat box. */
    public void submit() {
        String text = input.toString().trim();
        close();
        if (text.isEmpty()) {
            return;
        }
        if (!text.startsWith("/")) {
            log(text);
            return;
        }

        String[] parts = text.substring(1).split("\\s+");
        Command command = commands.get(parts[0].toLowerCase());
        if (command == null) {
            log("No such command /" + parts[0] + " - type /help for the list");
            return;
        }

        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        String reply = command.run(args);
        if (reply != null) {
            log(reply);
        }
    }

    public void log(String text) {
        lines.add(new Line(text));
        while (lines.size() > MAX_LINES) {
            lines.remove(0);
        }
    }

    /** Counts down the age of the chat lines; expired lines are dropped. */
    public void update(float delta) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            Line line = lines.get(i);
            // While the chat box is open the text stays readable, like Minecraft.
            if (!open) {
                line.life -= delta;
            }
            if (line.life <= 0f) {
                lines.remove(i);
            }
        }
    }

    public List<Line> lines() {
        return lines;
    }
}
