package com.tactics.client.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tactics.client.ui.GameColors;
import com.tactics.client.util.TextRenderer;

import java.util.List;
import java.util.Set;

/**
 * Renders the 5x5 game board, units, and related visual elements.
 * Extracted from BattleScreen to improve maintainability and enable animation support.
 */
public class BoardRenderer {

    // ========== Grid Constants ==========
    public static final int GRID_SIZE = 5;
    public static final float CELL_SIZE = 80;
    public static final float GRID_START_X = 50;
    public static final float GRID_START_Y = 80;

    // ========== Unit Rendering ==========
    private static final float UNIT_PADDING = 12;
    private static final float UNIT_SIZE = CELL_SIZE - 2 * UNIT_PADDING;
    private static final float HP_BAR_HEIGHT = 6;
    private static final float HP_BAR_OFFSET = 3;
    private static final float BUFF_DOT_SIZE = 8;
    private static final float BUFF_DOT_SPACING = 3;

    // ========== Dependencies ==========
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final boolean isTeaVM;

    public BoardRenderer(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        this.shapeRenderer = shapeRenderer;
        this.batch = batch;
        this.font = font;
        this.isTeaVM = TextRenderer.isTeaVM();
    }

    /**
     * Render the game board grid with highlighting for valid targets.
     *
     * @param validMoveTargets   Set of "x,y" strings for valid move positions
     * @param validAttackTargets Set of "x,y" strings for valid attack positions
     * @param actionMode         Current action mode: "MOVE", "ATTACK", "SKILL", or "NONE"
     * @param selectedX          X position of selected unit (-1 if none)
     * @param selectedY          Y position of selected unit (-1 if none)
     * @param showCoordinates    Whether to show debug coordinates
     */
    public void renderGrid(Set<String> validMoveTargets, Set<String> validAttackTargets,
                          String actionMode, int selectedX, int selectedY, boolean showCoordinates) {
        // Draw tile backgrounds
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                float drawX = GRID_START_X + x * CELL_SIZE;
                float drawY = GRID_START_Y + y * CELL_SIZE;

                // Determine tile color based on state
                Color tileColor = GameColors.TILE_EMPTY;
                Color borderColor = Color.DARK_GRAY;

                String posKey = x + "," + y;

                // Highlight valid targets based on action mode
                if ("MOVE".equals(actionMode) && validMoveTargets.contains(posKey)) {
                    tileColor = GameColors.TILE_VALID_MOVE;
                } else if ("ATTACK".equals(actionMode) && validAttackTargets.contains(posKey)) {
                    tileColor = GameColors.TILE_VALID_ATTACK;
                } else if ("SKILL".equals(actionMode) && validAttackTargets.contains(posKey)) {
                    tileColor = GameColors.TILE_VALID_SKILL;
                }

                // Highlight selected unit's tile
                if (selectedX == x && selectedY == y) {
                    borderColor = GameColors.SELECTION_BORDER;
                }

                // Draw tile
                drawFilledRect(drawX, drawY, CELL_SIZE - 2, CELL_SIZE - 2, tileColor, borderColor);
            }
        }

        // Draw grid coordinates (debug) - skip on TeaVM
        if (showCoordinates && !isTeaVM && font != null) {
            font.getData().setScale(0.5f);
            batch.begin();
            for (int x = 0; x < GRID_SIZE; x++) {
                for (int y = 0; y < GRID_SIZE; y++) {
                    float drawX = GRID_START_X + x * CELL_SIZE + 5;
                    float drawY = GRID_START_Y + y * CELL_SIZE + CELL_SIZE - 5;
                    font.setColor(new Color(0.4f, 0.4f, 0.4f, 1));
                    font.draw(batch, x + "," + y, drawX, drawY);
                }
            }
            batch.end();
            font.getData().setScale(1f);
        }
    }

    /**
     * Render buff tiles on the board.
     *
     * @param buffTiles List of buff tiles with x, y, and buffType
     */
    public void renderBuffTiles(List<BuffTileData> buffTiles) {
        for (BuffTileData tile : buffTiles) {
            float drawX = GRID_START_X + tile.x * CELL_SIZE + 5;
            float drawY = GRID_START_Y + tile.y * CELL_SIZE + 5;
            float size = CELL_SIZE - 12;

            Color buffColor = GameColors.getBuffColor(tile.buffType).cpy();
            buffColor.a = 0.4f;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(buffColor);
            shapeRenderer.rect(drawX, drawY, size, size);
            shapeRenderer.end();

            // Draw buff letter - use placeholder on TeaVM
            if (isTeaVM || font == null) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(new Color(1, 1, 1, 0.5f));
                shapeRenderer.rect(drawX + size / 2 - 4, drawY + size / 2 - 6, 10, 12);
                shapeRenderer.end();
            } else {
                font.getData().setScale(0.6f);
                batch.begin();
                String letter = tile.buffType.substring(0, 1);
                float textX = drawX + size / 2 - 4;
                float textY = drawY + size / 2 + 5;
                font.setColor(Color.WHITE);
                font.draw(batch, letter, textX, textY);
                batch.end();
                font.getData().setScale(1f);
            }
        }
    }

    /**
     * Render all units on the board.
     *
     * @param units        List of unit data
     * @param selectedUnit The currently selected unit (or null)
     */
    public void renderUnits(List<UnitRenderData> units, UnitRenderData selectedUnit) {
        for (UnitRenderData unit : units) {
            if (unit.hp <= 0) continue;

            float drawX = GRID_START_X + unit.x * CELL_SIZE + UNIT_PADDING;
            float drawY = GRID_START_Y + unit.y * CELL_SIZE + UNIT_PADDING;

            // Unit color
            Color unitColor;
            if (unit.isAlly) {
                unitColor = unit.isHero ? GameColors.UNIT_ALLY_HERO : GameColors.UNIT_ALLY_MINION;
            } else {
                unitColor = unit.isHero ? GameColors.UNIT_ENEMY_HERO : GameColors.UNIT_ENEMY_MINION;
            }

            // Border color
            Color borderColor = (unit == selectedUnit) ? GameColors.SELECTION_BORDER : Color.WHITE;

            // Draw unit body
            drawFilledRect(drawX, drawY, UNIT_SIZE, UNIT_SIZE, unitColor, borderColor);

            // Draw unit label (H=Hero, M=Minion) - use placeholder on TeaVM
            if (isTeaVM || font == null) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(new Color(1, 1, 1, 0.5f));
                shapeRenderer.rect(drawX + UNIT_SIZE / 2 - 6, drawY + UNIT_SIZE / 2 - 8, 12, 16);
                shapeRenderer.end();
            } else {
                font.getData().setScale(0.9f);
                batch.begin();
                String label = unit.isHero ? "H" : "M";
                font.setColor(Color.WHITE);
                float textWidth = font.getXHeight() * label.length() * 0.6f;
                font.draw(batch, label, drawX + UNIT_SIZE / 2 - textWidth / 2, drawY + UNIT_SIZE / 2 + 5);
                batch.end();
                font.getData().setScale(1f);
            }

            // Draw HP bar
            renderHPBar(drawX, drawY + UNIT_SIZE + HP_BAR_OFFSET, UNIT_SIZE, HP_BAR_HEIGHT, unit.hp, unit.maxHp);

            // Draw buff indicators
            renderBuffIndicators(drawX, drawY - 10, unit.buffs);
        }
    }

    /**
     * Render an HP bar.
     */
    public void renderHPBar(float x, float y, float width, float height, int current, int max) {
        // Background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(GameColors.HP_BACKGROUND);
        shapeRenderer.rect(x, y, width, height);

        // HP fill
        float fillWidth = width * ((float) current / max);
        Color hpColor;
        float hpPercent = (float) current / max;
        if (hpPercent > 0.6f) {
            hpColor = GameColors.HP_HIGH;
        } else if (hpPercent > 0.3f) {
            hpColor = GameColors.HP_MEDIUM;
        } else {
            hpColor = GameColors.HP_LOW;
        }
        shapeRenderer.setColor(hpColor);
        shapeRenderer.rect(x, y, fillWidth, height);
        shapeRenderer.end();

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();
    }

    /**
     * Render buff indicator dots.
     */
    public void renderBuffIndicators(float x, float y, List<String> buffs) {
        if (buffs == null || buffs.isEmpty()) return;

        float startX = x;

        for (String buff : buffs) {
            Color buffColor = GameColors.getBuffColor(buff);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(buffColor);
            shapeRenderer.rect(startX, y, BUFF_DOT_SIZE, BUFF_DOT_SIZE);
            shapeRenderer.end();

            startX += BUFF_DOT_SIZE + BUFF_DOT_SPACING;
        }
    }

    /**
     * Convert screen X to grid X.
     */
    public int screenToGridX(float worldX) {
        if (worldX < GRID_START_X || worldX >= GRID_START_X + GRID_SIZE * CELL_SIZE) {
            return -1;
        }
        return (int) ((worldX - GRID_START_X) / CELL_SIZE);
    }

    /**
     * Convert screen Y to grid Y.
     */
    public int screenToGridY(float worldY) {
        if (worldY < GRID_START_Y || worldY >= GRID_START_Y + GRID_SIZE * CELL_SIZE) {
            return -1;
        }
        return (int) ((worldY - GRID_START_Y) / CELL_SIZE);
    }

    /**
     * Check if a point is within the grid bounds.
     */
    public boolean isInGrid(float worldX, float worldY) {
        return worldX >= GRID_START_X && worldX < GRID_START_X + GRID_SIZE * CELL_SIZE &&
               worldY >= GRID_START_Y && worldY < GRID_START_Y + GRID_SIZE * CELL_SIZE;
    }

    // ========== Helper Methods ==========

    private void drawFilledRect(float x, float y, float width, float height, Color fillColor, Color borderColor) {
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

    // ========== Data Classes ==========

    /**
     * Data class for unit rendering information.
     */
    public static class UnitRenderData {
        public String id;
        public int x, y;
        public int hp, maxHp;
        public boolean isHero;
        public boolean isAlly;
        public List<String> buffs;

        public UnitRenderData(String id, int x, int y, int hp, int maxHp,
                             boolean isHero, boolean isAlly, List<String> buffs) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.maxHp = maxHp;
            this.isHero = isHero;
            this.isAlly = isAlly;
            this.buffs = buffs;
        }
    }

    /**
     * Data class for buff tile rendering information.
     */
    public static class BuffTileData {
        public int x, y;
        public String buffType;

        public BuffTileData(int x, int y, String buffType) {
            this.x = x;
            this.y = y;
            this.buffType = buffType;
        }
    }
}
