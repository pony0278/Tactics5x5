package com.tactics.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tactics.client.TacticsGame;
import com.tactics.client.util.TextRenderer;

/**
 * Initial "Hello Tactics" screen to verify the setup works.
 * Displays a simple message and colored background.
 */
public class HelloTacticsScreen implements Screen {

    private final TacticsGame game;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;  // Only used on desktop/Android
    private boolean fontLoaded = false;

    public HelloTacticsScreen(TacticsGame game) {
        this.game = game;
    }

    /**
     * Check if running in web browser (dynamically, not cached at construction).
     */
    private boolean isWebBuild() {
        return TextRenderer.isWebBuild();
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        Gdx.app.log("HelloTacticsScreen", "Screen shown");
    }

    /**
     * Lazily load font on desktop/Android only.
     */
    private BitmapFont getFont() {
        if (!fontLoaded && !isWebBuild()) {
            try {
                font = new BitmapFont(Gdx.files.internal("default.fnt"));
                font.setColor(Color.WHITE);
            } catch (Exception e) {
                Gdx.app.error("HelloTacticsScreen", "Failed to load font: " + e.getMessage());
            }
            fontLoaded = true;
        }
        return font;
    }

    @Override
    public void render(float delta) {
        // Clear screen with dark blue background
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        // Draw a 5x5 grid in the center
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.CYAN);

        float gridSize = Math.min(width, height) * 0.6f;
        float cellSize = gridSize / 5;
        float startX = (width - gridSize) / 2;
        float startY = (height - gridSize) / 2;

        // Draw grid lines
        for (int i = 0; i <= 5; i++) {
            // Vertical lines
            shapeRenderer.line(startX + i * cellSize, startY, startX + i * cellSize, startY + gridSize);
            // Horizontal lines
            shapeRenderer.line(startX, startY + i * cellSize, startX + gridSize, startY + i * cellSize);
        }
        shapeRenderer.end();

        // Draw some colored squares to represent units
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Blue unit (player)
        shapeRenderer.setColor(Color.BLUE);
        float unitPadding = cellSize * 0.1f;
        float unitSize = cellSize - 2 * unitPadding;
        shapeRenderer.rect(startX + 2 * cellSize + unitPadding, startY + 1 * cellSize + unitPadding, unitSize, unitSize);

        // Red unit (enemy)
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(startX + 2 * cellSize + unitPadding, startY + 3 * cellSize + unitPadding, unitSize, unitSize);

        shapeRenderer.end();

        // Draw text - use shapes on web, BitmapFont on desktop
        if (isWebBuild()) {
            // Web: Draw placeholder rectangles for text
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(1, 1, 1, 0.3f));
            // Title placeholder
            shapeRenderer.rect(width / 2 - 100, height - 70, 200, 30);
            // Subtitle placeholder
            shapeRenderer.rect(width / 2 - 150, height - 105, 300, 20);
            // Press any key placeholder
            shapeRenderer.rect(width / 2 - 100, 35, 200, 20);
            shapeRenderer.end();
        } else {
            // Desktop: Use BitmapFont
            BitmapFont f = getFont();
            if (f != null) {
                game.batch.begin();
                TextRenderer.drawCenteredText(game.batch, f, "Hello Tactics!", width / 2, height - 50);
                TextRenderer.drawCenteredText(game.batch, f, "5x5 Tactics Engine - LibGDX Client", width / 2, height - 90);
                TextRenderer.drawCenteredText(game.batch, f, "Press any key to continue...", width / 2, 50);
                game.batch.end();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        // Handle resize if needed
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }
}
