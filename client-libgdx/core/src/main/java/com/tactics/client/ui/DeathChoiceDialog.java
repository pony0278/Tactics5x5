package com.tactics.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tactics.client.util.TextRenderer;

/**
 * Modal dialog for death choice selection.
 * When a minion dies, the killer's owner chooses a buff to place on the board.
 */
public class DeathChoiceDialog {

    /**
     * Callback interface for buff selection.
     */
    public interface OnBuffSelected {
        void onBuffSelected(String buffType);
    }

    // ========== Layout Constants ==========
    private static final float DIALOG_WIDTH = 400;
    private static final float DIALOG_HEIGHT = 200;

    private static final float BUTTON_WIDTH = 100;
    private static final float BUTTON_HEIGHT = 40;
    private static final float BUTTON_SPACING = 15;

    private static final String[] BUFF_TYPES = {"POWER", "LIFE", "SPEED", "WEAKNESS", "BLEED", "SLOW"};

    // ========== State ==========
    private boolean visible = false;
    private float timer = 5f;
    private String killerId = "";
    private OnBuffSelected callback;

    // ========== Screen dimensions (set externally) ==========
    private float screenWidth;
    private float screenHeight;

    public DeathChoiceDialog(float screenWidth, float screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * Show the death choice dialog.
     *
     * @param killerId ID of the unit that made the kill
     * @param callback Callback when a buff is selected
     */
    public void show(String killerId, OnBuffSelected callback) {
        this.visible = true;
        this.timer = 5f;
        this.killerId = killerId;
        this.callback = callback;
    }

    /**
     * Hide the dialog.
     */
    public void hide() {
        this.visible = false;
        this.timer = 5f;
        this.killerId = "";
    }

    /**
     * Update the dialog timer.
     *
     * @param delta Time since last frame
     * @return The auto-selected buff type if timer expired, null otherwise
     */
    public String update(float delta) {
        if (!visible) return null;

        timer -= delta;
        if (timer <= 0) {
            timer = 0;
            // Auto-select default (creates obstacle in real implementation)
            hide();
            return "OBSTACLE";
        }
        return null;
    }

    /**
     * Render the dialog.
     */
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font) {
        if (!visible) return;

        float dialogX = (screenWidth - DIALOG_WIDTH) / 2;
        float dialogY = (screenHeight - DIALOG_HEIGHT) / 2;

        // Dim background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0, 0, 0, 0.7f));
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        // Dialog background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(GameColors.PANEL_BACKGROUND);
        shapeRenderer.rect(dialogX, dialogY, DIALOG_WIDTH, DIALOG_HEIGHT);
        shapeRenderer.end();

        // Dialog border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(dialogX, dialogY, DIALOG_WIDTH, DIALOG_HEIGHT);
        shapeRenderer.end();

        // Title - use placeholders on web builds
        boolean isWebBuild = TextRenderer.isWebBuild();
        if (isWebBuild || font == null) {
            // Draw placeholder rectangles for text
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(1, 1, 1, 0.3f));
            // Title placeholder
            shapeRenderer.rect(screenWidth / 2 - 60, dialogY + DIALOG_HEIGHT - 40, 120, 20);
            // Subtitle placeholder
            shapeRenderer.rect(screenWidth / 2 - 80, dialogY + DIALOG_HEIGHT - 65, 160, 15);
            // Timer placeholder
            Color timerColor = timer < 2 ? GameColors.TIMER_CRITICAL : GameColors.TIMER_WARNING;
            shapeRenderer.setColor(new Color(timerColor.r, timerColor.g, timerColor.b, 0.5f));
            shapeRenderer.rect(screenWidth / 2 - 40, dialogY + DIALOG_HEIGHT - 90, 80, 15);
            shapeRenderer.end();
        } else {
            batch.begin();
            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            String title = "MINION DIED!";
            float titleWidth = title.length() * font.getXHeight() * 0.6f;
            font.draw(batch, title, screenWidth / 2 - titleWidth / 2, dialogY + DIALOG_HEIGHT - 25);

            font.getData().setScale(0.9f);
            String subtitle = "Choose buff for killer:";
            float subtitleWidth = subtitle.length() * font.getXHeight() * 0.5f;
            font.draw(batch, subtitle, screenWidth / 2 - subtitleWidth / 2, dialogY + DIALOG_HEIGHT - 55);

            // Timer
            Color timerColor = timer < 2 ? GameColors.TIMER_CRITICAL : GameColors.TIMER_WARNING;
            font.setColor(timerColor);
            String timerText = "Time: " + formatTimer(timer) + "s";
            float timerWidth = timerText.length() * font.getXHeight() * 0.5f;
            font.draw(batch, timerText, screenWidth / 2 - timerWidth / 2, dialogY + DIALOG_HEIGHT - 80);
            font.getData().setScale(1f);
            batch.end();
        }

        // Buff buttons (2 rows of 3)
        float rowWidth = 3 * BUTTON_WIDTH + 2 * BUTTON_SPACING;
        float startX = dialogX + (DIALOG_WIDTH - rowWidth) / 2;
        float row1Y = dialogY + 70;
        float row2Y = dialogY + 20;

        for (int i = 0; i < BUFF_TYPES.length; i++) {
            int col = i % 3;
            float x = startX + col * (BUTTON_WIDTH + BUTTON_SPACING);
            float y = (i < 3) ? row1Y : row2Y;

            Color buffColor = GameColors.getBuffColor(BUFF_TYPES[i]);

            // Button fill
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(buffColor);
            shapeRenderer.rect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
            shapeRenderer.end();

            // Button border
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.rect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
            shapeRenderer.end();

            // Button text - use placeholder on TeaVM
            if (isWebBuild || font == null) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(new Color(1, 1, 1, 0.3f));
                float textWidth = BUFF_TYPES[i].length() * 6;
                shapeRenderer.rect(x + BUTTON_WIDTH / 2 - textWidth / 2, y + BUTTON_HEIGHT / 2 - 5, textWidth, 12);
                shapeRenderer.end();
            } else {
                batch.begin();
                font.getData().setScale(0.75f);
                font.setColor(Color.WHITE);
                float textWidth = BUFF_TYPES[i].length() * font.getXHeight() * 0.5f;
                font.draw(batch, BUFF_TYPES[i], x + BUTTON_WIDTH / 2 - textWidth / 2, y + BUTTON_HEIGHT / 2 + 5);
                font.getData().setScale(1f);
                batch.end();
            }
        }
    }

    /**
     * Handle input and return the selected buff type if a button was clicked.
     *
     * @param worldX World X coordinate of click
     * @param worldY World Y coordinate of click
     * @return Selected buff type, or null if no button was clicked
     */
    public String handleInput(float worldX, float worldY) {
        if (!visible) return null;

        float dialogX = (screenWidth - DIALOG_WIDTH) / 2;
        float dialogY = (screenHeight - DIALOG_HEIGHT) / 2;

        float rowWidth = 3 * BUTTON_WIDTH + 2 * BUTTON_SPACING;
        float startX = dialogX + (DIALOG_WIDTH - rowWidth) / 2;
        float row1Y = dialogY + 70;
        float row2Y = dialogY + 20;

        for (int i = 0; i < BUFF_TYPES.length; i++) {
            int col = i % 3;
            float x = startX + col * (BUTTON_WIDTH + BUTTON_SPACING);
            float y = (i < 3) ? row1Y : row2Y;

            if (worldX >= x && worldX <= x + BUTTON_WIDTH &&
                worldY >= y && worldY <= y + BUTTON_HEIGHT) {
                String selected = BUFF_TYPES[i];
                if (callback != null) {
                    callback.onBuffSelected(selected);
                }
                hide();
                return selected;
            }
        }

        return null;
    }

    /**
     * Check if the dialog is visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Get the remaining time.
     */
    public float getTimer() {
        return timer;
    }

    /**
     * Get the killer unit ID.
     */
    public String getKillerId() {
        return killerId;
    }

    /**
     * Format timer (GWT doesn't support String.format).
     */
    private String formatTimer(float value) {
        int intPart = (int) value;
        int fracPart = (int) ((value - intPart) * 10);
        return intPart + "." + fracPart;
    }
}
