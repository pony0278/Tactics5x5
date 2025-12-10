package com.tactics.client.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * TeaVM-safe text rendering utility.
 *
 * BitmapFont uses Pool reflection (GlyphLayout, GlyphRun) which doesn't work in TeaVM.
 * This class detects the runtime environment and provides appropriate rendering:
 * - Desktop/Android: Uses BitmapFont normally
 * - Web/TeaVM: Skips text, uses shape placeholders
 */
public class TextRenderer {

    private static final boolean IS_TEAVM;

    static {
        // Detect if running in TeaVM by checking for JSObject class
        boolean teavm = false;
        try {
            Class.forName("org.teavm.jso.JSObject");
            teavm = true;
        } catch (ClassNotFoundException e) {
            teavm = false;
        }
        IS_TEAVM = teavm;
    }

    /**
     * Check if running in TeaVM/web environment.
     */
    public static boolean isTeaVM() {
        return IS_TEAVM;
    }

    /**
     * Draw text safely - skips on TeaVM, uses BitmapFont on desktop.
     *
     * @param batch SpriteBatch for text rendering
     * @param font BitmapFont to use (ignored on TeaVM)
     * @param text Text to draw
     * @param x X position
     * @param y Y position
     */
    public static void drawText(SpriteBatch batch, BitmapFont font,
                                String text, float x, float y) {
        if (IS_TEAVM) {
            // Skip text on TeaVM - use HTML overlay or shapes instead
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
        if (IS_TEAVM) {
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
