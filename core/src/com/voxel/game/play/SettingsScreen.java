package com.voxel.game.play;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.voxel.engine.GameSettings;

/**
 * The ESC settings panel, drawn in the same Minecraft style as the rest of the HUD:
 * a raised charcoal panel with two SLIDERS (mouse sensitivity, field of view), two
 * TOGGLES (3D clouds, cinematic vignette) and a Done button.
 *
 * <p>Values are written straight into the {@link GameSettings} singleton, so the world
 * reacts while you drag - turn the FOV slider and the camera zooms live behind the panel.
 */
public final class SettingsScreen {

    private static final float PANEL_W = 230f;
    private static final float PANEL_H = 150f;
    private static final float SLIDER_W = 110f;
    private static final float SLIDER_H = 12f;
    private static final float ROW_STEP = 24f;
    private static final float BUTTON_W = 90f;
    private static final float BUTTON_H = 18f;

    private final MinecraftUi ui;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final GameSettings settings = GameSettings.get();

    /** Hit areas, recomputed every frame at draw time so drawing and clicking never drift. */
    private final Rectangle sensitivityTrack = new Rectangle();
    private final Rectangle fovTrack = new Rectangle();
    private final Rectangle cloudsButton = new Rectangle();
    private final Rectangle vignetteButton = new Rectangle();
    private final Rectangle doneButton = new Rectangle();

    private boolean open;
    /** Which slider is being dragged: 0 = sensitivity, 1 = fov, -1 = none. */
    private int draggingSlider = -1;

    public SettingsScreen(MinecraftUi ui, BitmapFont font) {
        this.ui = ui;
        this.font = font;
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
        draggingSlider = -1;
    }

    // ------------------------------------------------------------------- draw

    public void draw(SpriteBatch batch, int width, int height, float mouseX, float mouseY) {
        if (!open) {
            return;
        }
        ui.rect(batch, Color.BLACK, 0.55f, 0f, 0f, width, height);

        float panelW = px(PANEL_W);
        float panelH = px(PANEL_H);
        float panelX = Math.round((width - panelW) * 0.5f);
        float panelY = Math.round((height - panelH) * 0.5f);
        ui.panel(batch, panelX, panelY, panelW, panelH);

        centerText(batch, "SETTINGS", width, panelY + panelH - px(12f), Color.WHITE);

        float rowX = panelX + px(14f);
        float valueX = panelX + px(104f);
        float rowY = panelY + panelH - px(40f);

        rowY = drawSlider(batch, "Mouse speed", rowX, valueX, rowY, sensitivityTrack,
                fraction(settings.mouseSensitivity(), GameSettings.MIN_SENSITIVITY, GameSettings.MAX_SENSITIVITY),
                String.format("%.2f", settings.mouseSensitivity()));
        rowY = drawSlider(batch, "Field of view", rowX, valueX, rowY, fovTrack,
                fraction(settings.fieldOfView(), GameSettings.MIN_FOV, GameSettings.MAX_FOV),
                String.valueOf((int) settings.fieldOfView()));
        rowY = drawToggle(batch, "3D clouds", rowX, valueX, rowY, cloudsButton,
                settings.cloudsEnabled(), mouseX, mouseY);
        rowY = drawToggle(batch, "Vignette", rowX, valueX, rowY, vignetteButton,
                settings.vignetteEnabled(), mouseX, mouseY);

        doneButton.set(panelX + (panelW - px(BUTTON_W)) * 0.5f, panelY + px(10f),
                px(BUTTON_W), px(BUTTON_H));
        drawButton(batch, doneButton, "Done", doneButton.contains(mouseX, mouseY));
    }

    /** One slider row: label on the left, track + handle + value on the right. */
    private float drawSlider(SpriteBatch batch, String label, float rowX, float trackX, float rowY,
                             Rectangle track, float fraction, String value) {
        text(batch, label, rowX, rowY + px(9f), MinecraftUi.TEXT_DARK);

        track.set(trackX, rowY, px(SLIDER_W), px(SLIDER_H));
        ui.slot(batch, track.x, track.y, track.height);           // left cap look
        ui.rect(batch, MinecraftUi.SLOT_BG, track.x, track.y, track.width, track.height);
        ui.rect(batch, MinecraftUi.SLOT_DARK, track.x, track.y, track.width, px(1f));

        // Filled part plus the magenta handle, matching the hotbar selection accent.
        ui.rect(batch, MinecraftUi.ACCENT_DARK, track.x, track.y, track.width * fraction, track.height);
        float handleX = track.x + track.width * fraction - px(2f);
        ui.rect(batch, MinecraftUi.SELECTION, handleX, track.y - px(1f), px(4f), track.height + px(2f));
        batch.setColor(Color.WHITE);

        text(batch, value, track.x + track.width + px(6f), rowY + px(9f), Color.WHITE);
        return rowY - px(ROW_STEP);
    }

    /** One toggle row: label plus an ON / OFF button. */
    private float drawToggle(SpriteBatch batch, String label, float rowX, float buttonX, float rowY,
                             Rectangle button, boolean on, float mouseX, float mouseY) {
        text(batch, label, rowX, rowY + px(9f), MinecraftUi.TEXT_DARK);
        button.set(buttonX, rowY - px(2f), px(44f), px(SLIDER_H + 4f));
        drawButton(batch, button, on ? "ON" : "OFF", button.contains(mouseX, mouseY));
        return rowY - px(ROW_STEP);
    }

    private void drawButton(SpriteBatch batch, Rectangle area, String label, boolean hover) {
        ui.rect(batch, Color.BLACK, area.x - px(1f), area.y - px(1f),
                area.width + px(2f), area.height + px(2f));
        ui.rect(batch, hover ? MinecraftUi.PANEL_LIGHT : MinecraftUi.PANEL_BG,
                area.x, area.y, area.width, area.height);
        batch.setColor(hover ? MinecraftUi.ACCENT_EDGE : MinecraftUi.BAR_EDGE);
        ui.frame(batch, area.x, area.y, area.width, area.height, px(1f));
        batch.setColor(Color.WHITE);

        layout.setText(font, label);
        font.setColor(Color.WHITE);
        font.draw(batch, label, area.x + (area.width - layout.width) * 0.5f,
                area.y + (area.height + layout.height) * 0.5f);
    }

    private void text(SpriteBatch batch, String value, float x, float y, Color color) {
        font.setColor(color);
        font.draw(batch, value, x, y);
        font.setColor(Color.WHITE);
    }

    private void centerText(SpriteBatch batch, String value, int width, float y, Color color) {
        layout.setText(font, value);
        text(batch, value, (width - layout.width) * 0.5f, y, color);
    }

    // ------------------------------------------------------------------- input

    /**
     * A click inside the panel. Returns true when the click asked to CLOSE the screen
     * (the Done button), so the caller can hand the controls back.
     */
    public boolean touchDown(float mouseX, float mouseY) {
        if (doneButton.contains(mouseX, mouseY)) {
            return true;
        }
        if (cloudsButton.contains(mouseX, mouseY)) {
            settings.toggleClouds();
        } else if (vignetteButton.contains(mouseX, mouseY)) {
            settings.toggleVignette();
        } else if (sensitivityTrack.contains(mouseX, mouseY)) {
            draggingSlider = 0;
            dragTo(mouseX);
        } else if (fovTrack.contains(mouseX, mouseY)) {
            draggingSlider = 1;
            dragTo(mouseX);
        }
        return false;
    }

    public void touchDragged(float mouseX) {
        dragTo(mouseX);
    }

    public void touchUp() {
        draggingSlider = -1;
    }

    /** Moves the grabbed slider handle to the mouse position. */
    private void dragTo(float mouseX) {
        if (draggingSlider == 0) {
            settings.setMouseSensitivity(valueAt(sensitivityTrack, mouseX,
                    GameSettings.MIN_SENSITIVITY, GameSettings.MAX_SENSITIVITY));
        } else if (draggingSlider == 1) {
            settings.setFieldOfView(valueAt(fovTrack, mouseX,
                    GameSettings.MIN_FOV, GameSettings.MAX_FOV));
        }
    }

    private static float valueAt(Rectangle track, float mouseX, float min, float max) {
        float t = (mouseX - track.x) / track.width;
        return min + Math.max(0f, Math.min(1f, t)) * (max - min);
    }

    private static float fraction(float value, float min, float max) {
        return (value - min) / (max - min);
    }
}
