package com.tactics.client.util;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Web-safe text rendering utility.
 *
 * BitmapFont uses Pool reflection (GlyphLayout, GlyphRun) which doesn't work in TeaVM/GWT.
 * This class detects the runtime environment and provides appropriate rendering:
 * - Desktop/Android: Uses BitmapFont normally
 * - Web (TeaVM/GWT): Skips text, uses shape placeholders
 */
public class TextRenderer {

    // Cached value - null means not yet detected
    private static Boolean isWebCached = null;

    /**
     * Detect if running in web environment using LibGDX Application type.
     * This method is GWT-compatible (no reflection).
     */
    private static boolean detectWebBuild() {
        // Use LibGDX's Application type - works in GWT
        if (Gdx.app != null) {
            Application.ApplicationType type = Gdx.app.getType();
            return type == Application.ApplicationType.WebGL;
        }
        // Fallback: assume desktop if Gdx.app not yet initialized
        return false;
    }

    /**
     * Check if running in web environment (TeaVM or GWT).
     * Value is cached after first call for performance.
     */
    public static boolean isWebBuild() {
        if (isWebCached == null) {
            isWebCached = detectWebBuild();
        }
        return isWebCached;
    }

    /**
     * Check if running in TeaVM/web environment.
     * @deprecated Use {@link #isWebBuild()} instead
     */
    @Deprecated
    public static boolean isTeaVM() {
        return isWebBuild();
    }

    /**
     * Draw text safely - skips on web builds, uses BitmapFont on desktop.
     *
     * @param batch SpriteBatch for text rendering
     * @param font BitmapFont to use (ignored on web)
     * @param text Text to draw
     * @param x X position
     * @param y Y position
     */
    public static void drawText(SpriteBatch batch, BitmapFont font,
                                String text, float x, float y) {
        if (isWebBuild()) {
            // Skip text on web - use HTML overlay or shapes instead
            return;
        }
        // Desktop: use BitmapFont with pre-allocated GlyphLayout
        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, layout, x, y);
    }

    /**
     * Draw centered text safely.
     */
    public static void drawCenteredText(SpriteBatch batch, BitmapFont font,
                                        String text, float centerX, float y) {
        if (isWebBuild()) {
            return;
        }
        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, layout, centerX - layout.width / 2, y);
    }

    /**
     * Draw a text placeholder rectangle (for TeaVM builds).
     * Use this to indicate where text would appear.
     */
    public static void drawTextPlaceholder(ShapeRenderer shapes,
                                           float x, float y,
                                           float width, float height,
                                           Color color) {
        shapes.setColor(color);
        shapes.rect(x, y, width, height);
    }
}
