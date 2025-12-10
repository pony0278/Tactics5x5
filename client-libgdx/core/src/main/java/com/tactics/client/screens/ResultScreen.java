package com.tactics.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.tactics.client.TacticsGame;
import com.tactics.client.net.WebSocketFactory;

/**
 * Result screen - displays victory or defeat and allows replay.
 * Placeholder UI with colored rectangles.
 */
public class ResultScreen extends BaseScreen {

    private static final String TAG = "ResultScreen";

    // Result state
    private boolean isVictory = false;
    private String winnerText = "";

    // UI Layout
    private static final float BUTTON_WIDTH = 200;
    private static final float BUTTON_HEIGHT = 50;
    private static final float PLAY_AGAIN_X = (WORLD_WIDTH - BUTTON_WIDTH) / 2;
    private static final float PLAY_AGAIN_Y = 150;

    // Animation
    private float animationTime = 0;
    private static final float ANIMATION_DURATION = 2f;

    public ResultScreen(TacticsGame game) {
        super(game);
    }

    /**
     * Set the result to display.
     * @param isVictory true if player won
     * @param winner The winner's ID or name
     */
    public void setResult(boolean isVictory, String winner) {
        this.isVictory = isVictory;
        this.winnerText = winner;
        this.animationTime = 0;

        // Update background color based on result
        if (isVictory) {
            backgroundColor = new Color(0.1f, 0.2f, 0.15f, 1); // Greenish
        } else {
            backgroundColor = new Color(0.2f, 0.1f, 0.1f, 1); // Reddish
        }
    }

    @Override
    public void show() {
        super.show();

        // Disconnect WebSocket for new game
        try {
            WebSocketFactory.clearInstance();
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    protected void update(float delta) {
        animationTime += delta;
    }

    @Override
    protected void draw() {
        // Animated result text
        float scale = Math.min(animationTime / ANIMATION_DURATION, 1f) * 3f + 1f;
        if (!shouldSkipFonts() && getFont() != null) getFont().getData().setScale(scale);

        String resultText = isVictory ? "VICTORY!" : "DEFEAT";
        Color resultColor = isVictory ? Color.GOLD : Color.RED;

        // Add pulsing effect
        float pulse = (float) (Math.sin(animationTime * 3) * 0.1 + 1);
        if (!shouldSkipFonts() && getFont() != null) getFont().getData().setScale(scale * pulse);

        float textY = WORLD_HEIGHT / 2 + 100;
        drawText(resultText, WORLD_WIDTH / 2 - resultText.length() * 10, textY, resultColor);

        if (!shouldSkipFonts() && getFont() != null) getFont().getData().setScale(1.5f);

        // Winner info
        String winnerInfo = isVictory ? "You won the battle!" : "Your hero has fallen...";
        drawCenteredText(winnerInfo, WORLD_WIDTH / 2, textY - 80);

        if (!shouldSkipFonts() && getFont() != null) getFont().getData().setScale(1f);

        // Stats placeholder
        drawCenteredText("Rounds played: 5", WORLD_WIDTH / 2, textY - 130);
        drawCenteredText("Units lost: 2", WORLD_WIDTH / 2, textY - 155);

        // Play Again button
        Color buttonColor = new Color(0.2f, 0.4f, 0.3f, 1);
        Color borderColor = Color.WHITE;
        drawButton(PLAY_AGAIN_X, PLAY_AGAIN_Y, BUTTON_WIDTH, BUTTON_HEIGHT, buttonColor, borderColor);
        drawCenteredText("PLAY AGAIN", WORLD_WIDTH / 2, PLAY_AGAIN_Y + BUTTON_HEIGHT / 2 + 6);

        // Footer
        if (!shouldSkipFonts() && getFont() != null) getFont().getData().setScale(0.8f);
        drawCenteredText("Thank you for playing 5x5 Tactics!", WORLD_WIDTH / 2, 50);
        if (!shouldSkipFonts() && getFont() != null) getFont().getData().setScale(1f);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float worldX = screenToWorldX(screenX);
        float worldY = screenToWorldY(screenY);

        // Check Play Again button
        if (isPointInRect(worldX, worldY, PLAY_AGAIN_X, PLAY_AGAIN_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            onPlayAgainClicked();
            return true;
        }

        return false;
    }

    private void onPlayAgainClicked() {
        Gdx.app.log(TAG, "Play Again clicked");

        // Go back to connect screen
        ScreenManager.getInstance().showConnectScreen();
    }
}
