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
 * BitmapFont rendering:
 * - Desktop/Android: Uses BitmapFont normally
 * - GWT: Uses BitmapFont (supported)
 * - TeaVM: Uses shape placeholders (BitmapFont has issues)
 */
public class TextRenderer {

    // Cached values
    private static Boolean isWebCached = null;
    private static Boolean isTeaVMCached = null;

    /**
     * Check if running in web environment (WebGL).
     */
    public static boolean isWebBuild() {
        if (isWebCached == null && Gdx.app != null) {
            try {
                isWebCached = Gdx.app.getType() == Application.ApplicationType.WebGL;
            } catch (Exception e) {
                isWebCached = true;
            }
        }
        return isWebCached != null ? isWebCached : true;
    }

    /**
     * Check if running in TeaVM specifically (not GWT).
     * TeaVM has issues with BitmapFont Pool reflection.
     * GWT can handle BitmapFont fine.
     */
    public static boolean isTeaVMBuild() {
        if (isTeaVMCached == null) {
            isTeaVMCached = detectTeaVM();
        }
        return isTeaVMCached;
    }

    private static boolean detectTeaVM() {
        // TeaVM sets a specific system property or has specific class
        // In GWT, system properties are very limited
        // TeaVM: org.teavm.* classes exist
        // GWT: com.google.gwt.* classes exist
        try {
            // Try to detect TeaVM by checking for its runtime marker
            // TeaVM's JavaScript runtime has specific characteristics
            String vmName = System.getProperty("java.vm.name", "");
            if (vmName.toLowerCase().contains("teavm")) {
                return true;
            }
            // In browser, both GWT and TeaVM have limited System.getProperty
            // GWT returns null for most properties
            // For now, assume NOT TeaVM if we're in GWT (GWT can handle fonts)
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if fonts should be skipped (only for TeaVM).
     * GWT can handle BitmapFont.
     */
    public static boolean shouldSkipFonts() {
        return isTeaVMBuild();
    }

    /**
     * @deprecated Use {@link #isWebBuild()} or {@link #shouldSkipFonts()}
     */
    @Deprecated
    public static boolean isTeaVM() {
        return shouldSkipFonts();
    }

    /**
     * Draw text safely.
     * Works on Desktop, Android, and GWT.
     * Falls back to nothing on TeaVM.
     */
    public static void drawText(SpriteBatch batch, BitmapFont font,
                                String text, float x, float y) {
        if (shouldSkipFonts() || font == null || text == null) {
            return;
        }
        try {
            GlyphLayout layout = new GlyphLayout(font, text);
            font.draw(batch, layout, x, y);
        } catch (Exception e) {
            // Font rendering failed - ignore
        }
    }

    /**
     * Draw centered text safely.
     */
    public static void drawCenteredText(SpriteBatch batch, BitmapFont font,
                                        String text, float centerX, float y) {
        if (shouldSkipFonts() || font == null || text == null) {
            return;
        }
        try {
            GlyphLayout layout = new GlyphLayout(font, text);
            font.draw(batch, layout, centerX - layout.width / 2, y);
        } catch (Exception e) {
            // Font rendering failed - ignore
        }
    }

    /**
     * Draw a text placeholder rectangle (for builds that can't use fonts).
     */
    public static void drawTextPlaceholder(ShapeRenderer shapes,
                                           float x, float y,
                                           float width, float height,
                                           Color color) {
        shapes.setColor(color);
        shapes.rect(x, y, width, height);
    }
}
