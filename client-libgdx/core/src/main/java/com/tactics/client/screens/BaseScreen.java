package com.tactics.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.tactics.client.TacticsGame;
import com.tactics.client.net.GameMessageHandler;
import com.tactics.client.net.IWebSocketClient;
import com.tactics.client.util.TextRenderer;

/**
 * Base screen class providing common functionality for all game screens.
 * Handles SpriteBatch, Camera, Viewport setup and input handling.
 */
public abstract class BaseScreen extends InputAdapter implements Screen {

    protected static final float WORLD_WIDTH = 800;
    protected static final float WORLD_HEIGHT = 600;

    protected final TacticsGame game;
    protected final SpriteBatch batch;
    protected final ShapeRenderer shapeRenderer;
    protected BitmapFont font;  // Only used on desktop/Android, lazy-loaded
    protected boolean fontLoaded = false;  // Track if we've attempted to load font
    protected final OrthographicCamera camera;
    protected final Viewport viewport;
    protected final InputMultiplexer inputMultiplexer;

    protected Color backgroundColor = new Color(0.1f, 0.1f, 0.2f, 1);

    /**
     * Check if running in web browser (TeaVM or GWT).
     */
    protected boolean isWebBuild() {
        return TextRenderer.isWebBuild();
    }

    /**
     * Check if fonts should be skipped (TeaVM only, not GWT).
     */
    protected boolean shouldSkipFonts() {
        return TextRenderer.shouldSkipFonts();
    }

    public BaseScreen(TacticsGame game) {
        this.game = game;
        this.batch = game.batch;
        this.shapeRenderer = new ShapeRenderer();

        // Don't load font here - do it lazily when first needed
        this.font = null;

        // Setup camera and viewport
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);

        // Setup input handling
        this.inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(this);
    }

    /**
     * Lazily load font when first needed.
     * Works on Desktop, Android, and GWT (but not TeaVM).
     */
    protected BitmapFont getFont() {
        if (!fontLoaded && !shouldSkipFonts()) {
            try {
                this.font = new BitmapFont(Gdx.files.internal("default.fnt"));
                this.font.setColor(Color.WHITE);
            } catch (Exception e) {
                Gdx.app.error("BaseScreen", "Failed to load font: " + e.getMessage());
            }
            fontLoaded = true;
        }
        return font;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public final void render(float delta) {
        // Update logic
        update(delta);

        // Clear screen
        Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update camera
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Render
        draw();
    }

    /**
     * Update game logic. Called every frame before draw().
     * @param delta Time since last frame in seconds
     */
    protected abstract void update(float delta);

    /**
     * Draw the screen. Called every frame after update().
     */
    protected abstract void draw();

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        if (font != null) {
            font.dispose();
        }
    }

    // ========== Input Handling Helpers ==========

    /**
     * Convert screen coordinates to world coordinates.
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @return World X coordinate
     */
    protected float screenToWorldX(int screenX) {
        return screenX * WORLD_WIDTH / Gdx.graphics.getWidth();
    }

    /**
     * Convert screen coordinates to world coordinates.
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @return World Y coordinate (flipped for LibGDX coordinate system)
     */
    protected float screenToWorldY(int screenY) {
        return WORLD_HEIGHT - (screenY * WORLD_HEIGHT / Gdx.graphics.getHeight());
    }

    /**
     * Check if a point is within a rectangle.
     * @param px Point X
     * @param py Point Y
     * @param rx Rectangle X (bottom-left)
     * @param ry Rectangle Y (bottom-left)
     * @param rw Rectangle width
     * @param rh Rectangle height
     * @return true if point is inside rectangle
     */
    protected boolean isPointInRect(float px, float py, float rx, float ry, float rw, float rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    // ========== Drawing Helpers ==========

    /**
     * Draw a filled rectangle with border.
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param fillColor Fill color
     * @param borderColor Border color
     */
    protected void drawButton(float x, float y, float width, float height,
                              Color fillColor, Color borderColor) {
        // Fill
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(fillColor);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(borderColor);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();
    }

    /**
     * Draw centered text. On web builds, draws a placeholder rectangle instead.
     * @param text Text to draw
     * @param centerX Center X position
     * @param y Y position (baseline)
     */
    protected void drawCenteredText(String text, float centerX, float y) {
        if (text == null) return;
        if (shouldSkipFonts()) {
            // TeaVM: draw a placeholder rectangle
            float width = text.length() * 8; // Approximate width
            try {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0.8f, 0.8f, 0.8f, 0.3f);
                shapeRenderer.rect(centerX - width / 2, y - 15, width, 20);
            } finally {
                shapeRenderer.end();
            }
        } else {
            // Desktop/GWT: use BitmapFont
            BitmapFont f = getFont();
            if (f != null) {
                batch.begin();
                TextRenderer.drawCenteredText(batch, f, text, centerX, y);
                batch.end();
            }
        }
    }

    /**
     * Draw text at position. On web builds, draws a placeholder rectangle instead.
     * @param text Text to draw
     * @param x X position
     * @param y Y position (baseline)
     */
    protected void drawText(String text, float x, float y) {
        if (text == null) return;
        if (shouldSkipFonts()) {
            // TeaVM: draw a placeholder rectangle
            float width = text.length() * 8;
            try {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0.8f, 0.8f, 0.8f, 0.3f);
                shapeRenderer.rect(x, y - 15, width, 20);
            } finally {
                shapeRenderer.end();
            }
        } else {
            // Desktop/GWT: use BitmapFont
            BitmapFont f = getFont();
            if (f != null) {
                batch.begin();
                TextRenderer.drawText(batch, f, text, x, y);
                batch.end();
            }
        }
    }

    /**
     * Draw text at position with specific color. On web builds, draws a placeholder rectangle.
     * @param text Text to draw
     * @param x X position
     * @param y Y position
     * @param color Text color
     */
    protected void drawText(String text, float x, float y, Color color) {
        if (text == null || color == null) return;
        if (shouldSkipFonts()) {
            // TeaVM: draw a colored placeholder rectangle
            float width = text.length() * 8;
            try {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(color.r, color.g, color.b, 0.5f);
                shapeRenderer.rect(x, y - 15, width, 20);
            } finally {
                shapeRenderer.end();
            }
        } else {
            // Desktop/GWT: use BitmapFont
            BitmapFont f = getFont();
            if (f != null) {
                batch.begin();
                f.setColor(color);
                TextRenderer.drawText(batch, f, text, x, y);
                f.setColor(Color.WHITE);
                batch.end();
            }
        }
    }

    // ========== WebSocket Helpers ==========

    /**
     * Process WebSocket messages from the queue.
     * Call this in your update() method to handle messages thread-safely.
     * @param client The WebSocket client
     * @param handler The message handler to parse messages
     */
    protected void processWebSocketMessages(IWebSocketClient client, GameMessageHandler handler) {
        if (client == null || handler == null) return;

        String msg;
        while ((msg = client.pollMessage()) != null) {
            handler.parseMessage(msg);
        }
    }
}
