package com.tactics.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.JsonValue;
import com.tactics.client.TacticsGame;
import com.tactics.client.net.GameMessageHandler;
import com.tactics.client.net.IWebSocketClient;
import com.tactics.client.net.WebSocketFactory;
import com.tactics.client.net.WebSocketListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Battle screen - main game screen with 5x5 grid.
 * Full Battle phase UI using placeholder graphics (colored rectangles).
 *
 * Features:
 * - 5x5 board grid with tile highlighting
 * - Units with HP bars
 * - Action buttons (MOVE, ATTACK, SKILL, END TURN)
 * - Turn indicator and action timer
 * - Unit info panel
 * - Death Choice dialog
 */
public class BattleScreen extends BaseScreen implements WebSocketListener, GameMessageHandler.GameMessageListener {

    private static final String TAG = "BattleScreen";

    // ========== Grid Layout ==========
    private static final int GRID_SIZE = 5;
    private static final float CELL_SIZE = 80;
    private static final float GRID_START_X = 50;
    private static final float GRID_START_Y = 80;

    // ========== Action Mode ==========
    private enum ActionMode {
        NONE,
        MOVE,
        ATTACK,
        SKILL
    }

    private ActionMode currentActionMode = ActionMode.NONE;

    // ========== Unit Data ==========
    private static class UnitData {
        String id;
        int x, y;
        int hp, maxHp;
        int atk;
        boolean isHero;
        boolean isAlly;
        String heroClass;
        List<String> buffs = new ArrayList<>();
        int skillCooldown;

        UnitData(String id, int x, int y, boolean isHero, boolean isAlly) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.isHero = isHero;
            this.isAlly = isAlly;
            this.hp = isHero ? 12 : 8;
            this.maxHp = this.hp;
            this.atk = isHero ? 3 : 2;
            this.heroClass = isHero ? "WARRIOR" : null;
            this.skillCooldown = 0;
        }
    }

    private List<UnitData> units = new ArrayList<>();
    private UnitData selectedUnit = null;
    private Set<String> validMoveTargets = new HashSet<>();
    private Set<String> validAttackTargets = new HashSet<>();

    // ========== Buff Tile Data ==========
    private static class BuffTile {
        int x, y;
        String buffType;

        BuffTile(int x, int y, String buffType) {
            this.x = x;
            this.y = y;
            this.buffType = buffType;
        }
    }

    private List<BuffTile> buffTiles = new ArrayList<>();

    // ========== Game State ==========
    private boolean isPlayerTurn = true;
    private String currentUnitId = "";
    private int currentRound = 1;

    // ========== Timer ==========
    private float actionTimer = 10f;
    private boolean timerActive = true;

    // ========== Death Choice Dialog ==========
    private boolean showDeathChoiceDialog = false;
    private float deathChoiceTimer = 5f;
    private String deathChoiceKillerId = "";

    // ========== UI Layout Constants ==========

    // Turn indicator area (top)
    private static final float TURN_INDICATOR_Y = WORLD_HEIGHT - 50;
    private static final float TURN_INDICATOR_HEIGHT = 40;

    // Action buttons (right side)
    private static final float ACTION_PANEL_X = GRID_START_X + GRID_SIZE * CELL_SIZE + 30;
    private static final float ACTION_BUTTON_WIDTH = 130;
    private static final float ACTION_BUTTON_HEIGHT = 45;
    private static final float ACTION_BUTTON_SPACING = 10;

    // Unit info panel (right side, below buttons)
    private static final float INFO_PANEL_X = ACTION_PANEL_X;
    private static final float INFO_PANEL_Y = GRID_START_Y + 150;
    private static final float INFO_PANEL_WIDTH = 180;
    private static final float INFO_PANEL_HEIGHT = 150;

    // Death choice dialog (center modal)
    private static final float DIALOG_WIDTH = 400;
    private static final float DIALOG_HEIGHT = 200;
    private static final float DIALOG_X = (WORLD_WIDTH - DIALOG_WIDTH) / 2;
    private static final float DIALOG_Y = (WORLD_HEIGHT - DIALOG_HEIGHT) / 2;

    // Buff colors
    private static final Color BUFF_POWER_COLOR = new Color(0.9f, 0.5f, 0.1f, 1);   // Orange
    private static final Color BUFF_LIFE_COLOR = new Color(0.2f, 0.7f, 0.2f, 1);    // Green
    private static final Color BUFF_SPEED_COLOR = new Color(0.9f, 0.9f, 0.2f, 1);   // Yellow
    private static final Color BUFF_WEAKNESS_COLOR = new Color(0.5f, 0.2f, 0.6f, 1); // Purple
    private static final Color BUFF_BLEED_COLOR = new Color(0.6f, 0.1f, 0.1f, 1);   // Dark Red
    private static final Color BUFF_SLOW_COLOR = new Color(0.4f, 0.4f, 0.4f, 1);    // Gray

    // ========== WebSocket ==========
    private IWebSocketClient webSocket;
    private GameMessageHandler messageHandler;

    public BattleScreen(TacticsGame game) {
        super(game);
        backgroundColor = new Color(0.1f, 0.1f, 0.15f, 1);

        messageHandler = new GameMessageHandler();
        messageHandler.setMessageListener(this);

        initPlaceholderUnits();
    }

    private void initPlaceholderUnits() {
        units.clear();

        // Player units (bottom)
        units.add(new UnitData("ally-minion-1", 0, 0, false, true));
        units.add(new UnitData("ally-hero", 2, 1, true, true));
        units.add(new UnitData("ally-minion-2", 4, 0, false, true));

        // Enemy units (top)
        units.add(new UnitData("enemy-minion-1", 0, 4, false, false));
        units.add(new UnitData("enemy-hero", 2, 3, true, false));
        units.add(new UnitData("enemy-minion-2", 4, 4, false, false));

        // Add some placeholder buffs for demo
        units.get(1).buffs.add("POWER");
        units.get(4).buffs.add("SPEED");
    }

    @Override
    public void show() {
        super.show();

        try {
            webSocket = WebSocketFactory.getInstance();
            webSocket.setListener(this);
        } catch (Exception e) {
            Gdx.app.error(TAG, "WebSocket error: " + e.getMessage());
        }
    }

    /**
     * Reset battle state for a new game.
     */
    public void reset() {
        selectedUnit = null;
        currentActionMode = ActionMode.NONE;
        validMoveTargets.clear();
        validAttackTargets.clear();
        actionTimer = 10f;
        timerActive = true;
        isPlayerTurn = true;
        currentRound = 1;
        showDeathChoiceDialog = false;
        buffTiles.clear();

        initPlaceholderUnits();
    }

    @Override
    protected void update(float delta) {
        // Poll WebSocket messages
        processWebSocketMessages(webSocket, messageHandler);

        // Death choice timer
        if (showDeathChoiceDialog) {
            deathChoiceTimer -= delta;
            if (deathChoiceTimer <= 0) {
                deathChoiceTimer = 0;
                autoSelectDeathChoice();
            }
            return; // Don't process other timers during death choice
        }

        // Action timer
        if (timerActive && isPlayerTurn) {
            actionTimer -= delta;
            if (actionTimer <= 0) {
                actionTimer = 0;
                onTimerExpired();
            }
        }
    }

    private void onTimerExpired() {
        Gdx.app.log(TAG, "Action timer expired - auto END_TURN");
        if (webSocket != null && webSocket.isConnected()) {
            String msg = messageHandler.createEndTurnAction("player-1");
            webSocket.send(msg);
        }
        resetTimer();
    }

    private void resetTimer() {
        actionTimer = 10f;
    }

    private void autoSelectDeathChoice() {
        Gdx.app.log(TAG, "Death choice timer expired - auto select OBSTACLE");
        // In real implementation, this would create an obstacle
        // For now, just close the dialog
        showDeathChoiceDialog = false;
        deathChoiceTimer = 5f;
    }

    @Override
    protected void draw() {
        // Draw main UI
        drawTurnIndicator();
        drawGrid();
        drawBuffTiles();
        drawUnits();
        drawActionButtons();
        drawUnitInfoPanel();

        // Draw modal dialog on top if active
        if (showDeathChoiceDialog) {
            drawDeathChoiceDialog();
        }
    }

    // ========== Drawing Methods ==========

    private void drawTurnIndicator() {
        // Background
        Color bgColor = isPlayerTurn ? new Color(0.1f, 0.3f, 0.1f, 1) : new Color(0.3f, 0.1f, 0.1f, 1);
        drawButton(0, TURN_INDICATOR_Y - TURN_INDICATOR_HEIGHT, WORLD_WIDTH, TURN_INDICATOR_HEIGHT, bgColor, Color.DARK_GRAY);

        // Turn text
        String turnText = isPlayerTurn ? "YOUR TURN" : "OPPONENT'S TURN";
        Color turnColor = isPlayerTurn ? Color.GREEN : Color.RED;
        font.getData().setScale(1.3f);
        drawText(turnText, 20, TURN_INDICATOR_Y - 10, turnColor);

        // Round number
        drawText("Round " + currentRound, 200, TURN_INDICATOR_Y - 10, Color.WHITE);

        // Current unit indicator
        if (selectedUnit != null) {
            String unitName = selectedUnit.isHero ? selectedUnit.heroClass : "Minion";
            drawText("Unit: " + unitName, 350, TURN_INDICATOR_Y - 10, Color.CYAN);
        }

        // Timer
        Color timerBgColor = actionTimer < 3 ? new Color(0.4f, 0.1f, 0.1f, 1) : new Color(0.2f, 0.2f, 0.25f, 1);
        float timerWidth = 120;
        float timerX = WORLD_WIDTH - timerWidth - 10;
        drawButton(timerX, TURN_INDICATOR_Y - TURN_INDICATOR_HEIGHT + 5, timerWidth, TURN_INDICATOR_HEIGHT - 10, timerBgColor, Color.DARK_GRAY);

        Color timerColor;
        if (actionTimer < 3) {
            timerColor = Color.RED;
        } else if (actionTimer < 5) {
            timerColor = Color.YELLOW;
        } else {
            timerColor = Color.WHITE;
        }
        String timerText = String.format("%.1fs", actionTimer);
        drawCenteredText(timerText, timerX + timerWidth / 2, TURN_INDICATOR_Y - 10);
        font.getData().setScale(1f);
    }

    private void drawGrid() {
        // Draw tile backgrounds
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                float drawX = GRID_START_X + x * CELL_SIZE;
                float drawY = GRID_START_Y + y * CELL_SIZE;

                // Tile color based on state
                Color tileColor = new Color(0.2f, 0.2f, 0.25f, 1);
                Color borderColor = Color.DARK_GRAY;

                String posKey = x + "," + y;

                // Highlight valid targets based on action mode
                if (currentActionMode == ActionMode.MOVE && validMoveTargets.contains(posKey)) {
                    tileColor = new Color(0.2f, 0.4f, 0.2f, 0.8f); // Green for move
                } else if (currentActionMode == ActionMode.ATTACK && validAttackTargets.contains(posKey)) {
                    tileColor = new Color(0.4f, 0.2f, 0.2f, 0.8f); // Red for attack
                } else if (currentActionMode == ActionMode.SKILL && validAttackTargets.contains(posKey)) {
                    tileColor = new Color(0.3f, 0.2f, 0.4f, 0.8f); // Purple for skill
                }

                // Highlight selected unit's tile
                if (selectedUnit != null && selectedUnit.x == x && selectedUnit.y == y) {
                    borderColor = Color.YELLOW;
                }

                drawButton(drawX, drawY, CELL_SIZE - 2, CELL_SIZE - 2, tileColor, borderColor);
            }
        }

        // Draw grid coordinates (optional, for debug)
        font.getData().setScale(0.5f);
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                float drawX = GRID_START_X + x * CELL_SIZE + 5;
                float drawY = GRID_START_Y + y * CELL_SIZE + CELL_SIZE - 5;
                drawText(x + "," + y, drawX, drawY, new Color(0.4f, 0.4f, 0.4f, 1));
            }
        }
        font.getData().setScale(1f);
    }

    private void drawBuffTiles() {
        for (BuffTile tile : buffTiles) {
            float drawX = GRID_START_X + tile.x * CELL_SIZE + 5;
            float drawY = GRID_START_Y + tile.y * CELL_SIZE + 5;
            float size = CELL_SIZE - 12;

            Color buffColor = getBuffColor(tile.buffType);
            buffColor = buffColor.cpy();
            buffColor.a = 0.4f;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(buffColor);
            shapeRenderer.rect(drawX, drawY, size, size);
            shapeRenderer.end();

            // Draw buff letter
            font.getData().setScale(0.6f);
            String letter = tile.buffType.substring(0, 1);
            drawCenteredText(letter, drawX + size / 2, drawY + size / 2 + 5);
            font.getData().setScale(1f);
        }
    }

    private void drawUnits() {
        float unitPadding = 12;
        float unitSize = CELL_SIZE - 2 * unitPadding;

        for (UnitData unit : units) {
            if (unit.hp <= 0) continue;

            float drawX = GRID_START_X + unit.x * CELL_SIZE + unitPadding;
            float drawY = GRID_START_Y + unit.y * CELL_SIZE + unitPadding;

            // Unit color
            Color unitColor;
            if (unit.isAlly) {
                unitColor = unit.isHero ? new Color(0.2f, 0.4f, 0.8f, 1) : new Color(0.4f, 0.6f, 0.9f, 1);
            } else {
                unitColor = unit.isHero ? new Color(0.8f, 0.2f, 0.2f, 1) : new Color(0.9f, 0.5f, 0.5f, 1);
            }

            // Border color
            Color borderColor = (unit == selectedUnit) ? Color.YELLOW : Color.WHITE;

            // Draw unit body
            drawButton(drawX, drawY, unitSize, unitSize, unitColor, borderColor);

            // Draw unit label (H=Hero, M=Minion)
            font.getData().setScale(0.9f);
            String label = unit.isHero ? "H" : "M";
            drawCenteredText(label, drawX + unitSize / 2, drawY + unitSize / 2 + 5);
            font.getData().setScale(1f);

            // Draw HP bar
            drawHPBar(drawX, drawY + unitSize + 3, unitSize, 6, unit.hp, unit.maxHp);

            // Draw buff indicators
            drawBuffIndicators(drawX, drawY - 10, unit.buffs);
        }
    }

    private void drawHPBar(float x, float y, float width, float height, int current, int max) {
        // Background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, width, height);

        // HP fill
        float fillWidth = width * ((float) current / max);
        Color hpColor;
        float hpPercent = (float) current / max;
        if (hpPercent > 0.6f) {
            hpColor = Color.GREEN;
        } else if (hpPercent > 0.3f) {
            hpColor = Color.YELLOW;
        } else {
            hpColor = Color.RED;
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

    private void drawBuffIndicators(float x, float y, List<String> buffs) {
        if (buffs.isEmpty()) return;

        float dotSize = 8;
        float spacing = 3;
        float startX = x;

        for (String buff : buffs) {
            Color buffColor = getBuffColor(buff);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(buffColor);
            shapeRenderer.rect(startX, y, dotSize, dotSize);
            shapeRenderer.end();

            startX += dotSize + spacing;
        }
    }

    private Color getBuffColor(String buffType) {
        switch (buffType) {
            case "POWER": return BUFF_POWER_COLOR;
            case "LIFE": return BUFF_LIFE_COLOR;
            case "SPEED": return BUFF_SPEED_COLOR;
            case "WEAKNESS": return BUFF_WEAKNESS_COLOR;
            case "BLEED": return BUFF_BLEED_COLOR;
            case "SLOW": return BUFF_SLOW_COLOR;
            default: return Color.GRAY;
        }
    }

    private void drawActionButtons() {
        String[] actions = {"MOVE", "ATTACK", "SKILL", "END TURN"};
        Color[] activeColors = {
                new Color(0.2f, 0.5f, 0.2f, 1),  // Green for MOVE
                new Color(0.5f, 0.2f, 0.2f, 1),  // Red for ATTACK
                new Color(0.3f, 0.2f, 0.5f, 1),  // Purple for SKILL
                new Color(0.3f, 0.3f, 0.3f, 1)   // Gray for END TURN
        };

        float y = GRID_START_Y + GRID_SIZE * CELL_SIZE - ACTION_BUTTON_HEIGHT;

        for (int i = 0; i < actions.length; i++) {
            boolean enabled = isPlayerTurn && selectedUnit != null && selectedUnit.isAlly;
            boolean isActive = false;

            // Check if this action mode is active
            if (i == 0 && currentActionMode == ActionMode.MOVE) isActive = true;
            if (i == 1 && currentActionMode == ActionMode.ATTACK) isActive = true;
            if (i == 2 && currentActionMode == ActionMode.SKILL) isActive = true;

            // SKILL only available for heroes
            if (i == 2 && (selectedUnit == null || !selectedUnit.isHero)) {
                enabled = false;
            }

            // Determine colors
            Color fillColor;
            Color borderColor;

            if (!enabled) {
                fillColor = new Color(0.2f, 0.2f, 0.2f, 1);
                borderColor = Color.GRAY;
            } else if (isActive) {
                fillColor = activeColors[i].cpy();
                fillColor.r = Math.min(1f, fillColor.r + 0.2f);
                fillColor.g = Math.min(1f, fillColor.g + 0.2f);
                fillColor.b = Math.min(1f, fillColor.b + 0.2f);
                borderColor = Color.WHITE;
            } else {
                fillColor = activeColors[i];
                borderColor = Color.WHITE;
            }

            drawButton(ACTION_PANEL_X, y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT, fillColor, borderColor);

            // Button text
            String text = actions[i];
            if (i == 2 && selectedUnit != null && selectedUnit.isHero) {
                // Show skill name for hero
                text = "SKILL";
                if (selectedUnit.skillCooldown > 0) {
                    text += " (" + selectedUnit.skillCooldown + ")";
                }
            }

            font.getData().setScale(0.85f);
            drawCenteredText(text, ACTION_PANEL_X + ACTION_BUTTON_WIDTH / 2, y + ACTION_BUTTON_HEIGHT / 2 + 5);
            font.getData().setScale(1f);

            y -= ACTION_BUTTON_HEIGHT + ACTION_BUTTON_SPACING;
        }
    }

    private void drawUnitInfoPanel() {
        // Panel background
        Color panelColor = new Color(0.15f, 0.15f, 0.2f, 1);
        drawButton(INFO_PANEL_X, INFO_PANEL_Y - INFO_PANEL_HEIGHT, INFO_PANEL_WIDTH, INFO_PANEL_HEIGHT, panelColor, Color.DARK_GRAY);

        // Panel title
        font.getData().setScale(0.9f);
        drawText("UNIT INFO", INFO_PANEL_X + 10, INFO_PANEL_Y - 10, Color.CYAN);
        font.getData().setScale(0.8f);

        float lineY = INFO_PANEL_Y - 35;
        float lineSpacing = 20;

        if (selectedUnit != null) {
            // Unit type
            String unitType = selectedUnit.isHero ? selectedUnit.heroClass : "MINION";
            String owner = selectedUnit.isAlly ? "Ally" : "Enemy";
            drawText(owner + " " + unitType, INFO_PANEL_X + 10, lineY, Color.WHITE);
            lineY -= lineSpacing;

            // HP
            String hpText = "HP: " + selectedUnit.hp + "/" + selectedUnit.maxHp;
            Color hpColor = selectedUnit.hp > selectedUnit.maxHp / 2 ? Color.GREEN : Color.YELLOW;
            if (selectedUnit.hp <= selectedUnit.maxHp / 4) hpColor = Color.RED;
            drawText(hpText, INFO_PANEL_X + 10, lineY, hpColor);
            lineY -= lineSpacing;

            // ATK
            drawText("ATK: " + selectedUnit.atk, INFO_PANEL_X + 10, lineY, Color.WHITE);
            lineY -= lineSpacing;

            // Buffs
            if (!selectedUnit.buffs.isEmpty()) {
                drawText("Buffs:", INFO_PANEL_X + 10, lineY, Color.CYAN);
                lineY -= lineSpacing;

                for (String buff : selectedUnit.buffs) {
                    Color buffColor = getBuffColor(buff);
                    drawText("  " + buff, INFO_PANEL_X + 10, lineY, buffColor);
                    lineY -= lineSpacing;
                }
            }

            // Skill cooldown (for heroes)
            if (selectedUnit.isHero) {
                String cdText = selectedUnit.skillCooldown > 0 ?
                        "Skill CD: " + selectedUnit.skillCooldown :
                        "Skill: READY";
                Color cdColor = selectedUnit.skillCooldown > 0 ? Color.GRAY : Color.GREEN;
                drawText(cdText, INFO_PANEL_X + 10, lineY, cdColor);
            }
        } else {
            drawText("(Select a unit)", INFO_PANEL_X + 10, lineY, Color.GRAY);
        }

        font.getData().setScale(1f);
    }

    private void drawDeathChoiceDialog() {
        // Dim background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0, 0, 0, 0.7f));
        shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        shapeRenderer.end();

        // Dialog background
        Color dialogBg = new Color(0.15f, 0.15f, 0.2f, 1);
        drawButton(DIALOG_X, DIALOG_Y, DIALOG_WIDTH, DIALOG_HEIGHT, dialogBg, Color.WHITE);

        // Title
        font.getData().setScale(1.2f);
        drawCenteredText("MINION DIED!", WORLD_WIDTH / 2, DIALOG_Y + DIALOG_HEIGHT - 25);
        font.getData().setScale(0.9f);
        drawCenteredText("Choose buff for killer:", WORLD_WIDTH / 2, DIALOG_Y + DIALOG_HEIGHT - 55);

        // Timer
        Color timerColor = deathChoiceTimer < 2 ? Color.RED : Color.YELLOW;
        String timerText = String.format("Time: %.1fs", deathChoiceTimer);
        drawCenteredText(timerText, WORLD_WIDTH / 2, DIALOG_Y + DIALOG_HEIGHT - 80);
        font.getData().setScale(1f);

        // Buff buttons (2 rows of 3)
        String[] buffs = {"POWER", "LIFE", "SPEED", "WEAKNESS", "BLEED", "SLOW"};
        Color[] buffColors = {BUFF_POWER_COLOR, BUFF_LIFE_COLOR, BUFF_SPEED_COLOR, BUFF_WEAKNESS_COLOR, BUFF_BLEED_COLOR, BUFF_SLOW_COLOR};

        float buttonWidth = 100;
        float buttonHeight = 40;
        float spacing = 15;
        float rowWidth = 3 * buttonWidth + 2 * spacing;
        float startX = DIALOG_X + (DIALOG_WIDTH - rowWidth) / 2;
        float row1Y = DIALOG_Y + 70;
        float row2Y = DIALOG_Y + 20;

        for (int i = 0; i < buffs.length; i++) {
            int col = i % 3;
            float x = startX + col * (buttonWidth + spacing);
            float y = (i < 3) ? row1Y : row2Y;

            drawButton(x, y, buttonWidth, buttonHeight, buffColors[i], Color.WHITE);

            font.getData().setScale(0.75f);
            drawCenteredText(buffs[i], x + buttonWidth / 2, y + buttonHeight / 2 + 5);
            font.getData().setScale(1f);
        }
    }

    // ========== Input Handling ==========

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float worldX = screenToWorldX(screenX);
        float worldY = screenToWorldY(screenY);

        // Handle death choice dialog
        if (showDeathChoiceDialog) {
            return handleDeathChoiceClick(worldX, worldY);
        }

        // Not player's turn - ignore input
        if (!isPlayerTurn) return false;

        // Check action buttons
        if (handleActionButtonClick(worldX, worldY)) {
            return true;
        }

        // Check grid click
        if (handleGridClick(worldX, worldY)) {
            return true;
        }

        return false;
    }

    private boolean handleDeathChoiceClick(float worldX, float worldY) {
        String[] buffs = {"POWER", "LIFE", "SPEED", "WEAKNESS", "BLEED", "SLOW"};

        float buttonWidth = 100;
        float buttonHeight = 40;
        float spacing = 15;
        float rowWidth = 3 * buttonWidth + 2 * spacing;
        float startX = DIALOG_X + (DIALOG_WIDTH - rowWidth) / 2;
        float row1Y = DIALOG_Y + 70;
        float row2Y = DIALOG_Y + 20;

        for (int i = 0; i < buffs.length; i++) {
            int col = i % 3;
            float x = startX + col * (buttonWidth + spacing);
            float y = (i < 3) ? row1Y : row2Y;

            if (isPointInRect(worldX, worldY, x, y, buttonWidth, buttonHeight)) {
                selectDeathChoice(buffs[i]);
                return true;
            }
        }

        return false;
    }

    private void selectDeathChoice(String buffType) {
        Gdx.app.log(TAG, "Death choice selected: " + buffType);

        if (webSocket != null && webSocket.isConnected()) {
            String msg = messageHandler.createDeathChoiceAction("player-1", buffType);
            webSocket.send(msg);
        }

        showDeathChoiceDialog = false;
        deathChoiceTimer = 5f;
    }

    private boolean handleActionButtonClick(float worldX, float worldY) {
        float y = GRID_START_Y + GRID_SIZE * CELL_SIZE - ACTION_BUTTON_HEIGHT;

        // MOVE button
        if (isPointInRect(worldX, worldY, ACTION_PANEL_X, y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)) {
            toggleActionMode(ActionMode.MOVE);
            return true;
        }
        y -= ACTION_BUTTON_HEIGHT + ACTION_BUTTON_SPACING;

        // ATTACK button
        if (isPointInRect(worldX, worldY, ACTION_PANEL_X, y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)) {
            toggleActionMode(ActionMode.ATTACK);
            return true;
        }
        y -= ACTION_BUTTON_HEIGHT + ACTION_BUTTON_SPACING;

        // SKILL button
        if (isPointInRect(worldX, worldY, ACTION_PANEL_X, y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)) {
            if (selectedUnit != null && selectedUnit.isHero) {
                toggleActionMode(ActionMode.SKILL);
            }
            return true;
        }
        y -= ACTION_BUTTON_HEIGHT + ACTION_BUTTON_SPACING;

        // END TURN button
        if (isPointInRect(worldX, worldY, ACTION_PANEL_X, y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)) {
            sendEndTurn();
            return true;
        }

        return false;
    }

    private void toggleActionMode(ActionMode mode) {
        if (selectedUnit == null || !selectedUnit.isAlly) return;

        if (currentActionMode == mode) {
            // Cancel mode
            currentActionMode = ActionMode.NONE;
            validMoveTargets.clear();
            validAttackTargets.clear();
        } else {
            currentActionMode = mode;
            calculateValidTargets();
        }

        Gdx.app.log(TAG, "Action mode: " + currentActionMode);
    }

    private void calculateValidTargets() {
        validMoveTargets.clear();
        validAttackTargets.clear();

        if (selectedUnit == null) return;

        switch (currentActionMode) {
            case MOVE:
                // Valid move targets: empty tiles within range 1-2 (orthogonal)
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        int dist = Math.abs(dx) + Math.abs(dy);
                        if (dist > 2) continue;

                        int tx = selectedUnit.x + dx;
                        int ty = selectedUnit.y + dy;

                        if (tx >= 0 && tx < GRID_SIZE && ty >= 0 && ty < GRID_SIZE) {
                            if (getUnitAt(tx, ty) == null) {
                                validMoveTargets.add(tx + "," + ty);
                            }
                        }
                    }
                }
                break;

            case ATTACK:
                // Valid attack targets: enemy units within range 1
                for (UnitData unit : units) {
                    if (unit.hp <= 0 || unit.isAlly) continue;
                    int dist = Math.abs(unit.x - selectedUnit.x) + Math.abs(unit.y - selectedUnit.y);
                    if (dist <= 1) {
                        validAttackTargets.add(unit.x + "," + unit.y);
                    }
                }
                break;

            case SKILL:
                // Valid skill targets depend on skill type (placeholder: range 3)
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dy = -3; dy <= 3; dy++) {
                        int dist = Math.abs(dx) + Math.abs(dy);
                        if (dist > 3 || dist == 0) continue;

                        int tx = selectedUnit.x + dx;
                        int ty = selectedUnit.y + dy;

                        if (tx >= 0 && tx < GRID_SIZE && ty >= 0 && ty < GRID_SIZE) {
                            validAttackTargets.add(tx + "," + ty);
                        }
                    }
                }
                break;
        }
    }

    private boolean handleGridClick(float worldX, float worldY) {
        if (worldX < GRID_START_X || worldX >= GRID_START_X + GRID_SIZE * CELL_SIZE ||
            worldY < GRID_START_Y || worldY >= GRID_START_Y + GRID_SIZE * CELL_SIZE) {
            return false;
        }

        int cellX = (int) ((worldX - GRID_START_X) / CELL_SIZE);
        int cellY = (int) ((worldY - GRID_START_Y) / CELL_SIZE);
        String posKey = cellX + "," + cellY;

        Gdx.app.log(TAG, "Grid click: " + cellX + ", " + cellY + " Mode: " + currentActionMode);

        // Handle action mode clicks
        switch (currentActionMode) {
            case MOVE:
                if (validMoveTargets.contains(posKey)) {
                    sendMoveAction(cellX, cellY);
                    return true;
                }
                break;

            case ATTACK:
                if (validAttackTargets.contains(posKey)) {
                    UnitData target = getUnitAt(cellX, cellY);
                    if (target != null) {
                        sendAttackAction(target.id);
                        return true;
                    }
                }
                break;

            case SKILL:
                if (validAttackTargets.contains(posKey)) {
                    sendSkillAction(cellX, cellY);
                    return true;
                }
                break;
        }

        // Default: select/deselect unit
        UnitData clickedUnit = getUnitAt(cellX, cellY);
        if (clickedUnit != null && clickedUnit.isAlly) {
            selectedUnit = clickedUnit;
            currentActionMode = ActionMode.NONE;
            validMoveTargets.clear();
            validAttackTargets.clear();
            Gdx.app.log(TAG, "Selected unit: " + clickedUnit.id);
        } else {
            // Deselect
            selectedUnit = null;
            currentActionMode = ActionMode.NONE;
            validMoveTargets.clear();
            validAttackTargets.clear();
        }

        return true;
    }

    private UnitData getUnitAt(int x, int y) {
        for (UnitData unit : units) {
            if (unit.hp > 0 && unit.x == x && unit.y == y) {
                return unit;
            }
        }
        return null;
    }

    // ========== Action Sending ==========

    private void sendMoveAction(int targetX, int targetY) {
        if (selectedUnit == null) return;

        Gdx.app.log(TAG, "Sending MOVE action to " + targetX + ", " + targetY);

        if (webSocket != null && webSocket.isConnected()) {
            String msg = messageHandler.createMoveAction("player-1", targetX, targetY);
            webSocket.send(msg);
        }

        // Optimistic update (placeholder)
        selectedUnit.x = targetX;
        selectedUnit.y = targetY;

        currentActionMode = ActionMode.NONE;
        validMoveTargets.clear();
    }

    private void sendAttackAction(String targetId) {
        if (selectedUnit == null) return;

        Gdx.app.log(TAG, "Sending ATTACK action to " + targetId);

        if (webSocket != null && webSocket.isConnected()) {
            String msg = messageHandler.createAttackAction("player-1", selectedUnit.x, selectedUnit.y, targetId);
            webSocket.send(msg);
        }

        currentActionMode = ActionMode.NONE;
        validAttackTargets.clear();
    }

    private void sendSkillAction(int targetX, int targetY) {
        if (selectedUnit == null || !selectedUnit.isHero) return;

        Gdx.app.log(TAG, "Sending USE_SKILL action to " + targetX + ", " + targetY);

        if (webSocket != null && webSocket.isConnected()) {
            String msg = messageHandler.createUseSkillAction("player-1", "skill-1", targetX, targetY, null);
            webSocket.send(msg);
        }

        currentActionMode = ActionMode.NONE;
        validAttackTargets.clear();
    }

    private void sendEndTurn() {
        Gdx.app.log(TAG, "Sending END_TURN action");

        if (webSocket != null && webSocket.isConnected()) {
            String msg = messageHandler.createEndTurnAction("player-1");
            webSocket.send(msg);
        }

        currentActionMode = ActionMode.NONE;
        validMoveTargets.clear();
        validAttackTargets.clear();
    }

    // ========== WebSocket Listener ==========

    @Override
    public void onConnected() {
        Gdx.app.log(TAG, "Connected");
    }

    @Override
    public void onMessage(String message) {
        messageHandler.parseMessage(message);
    }

    @Override
    public void onDisconnected() {
        Gdx.app.log(TAG, "Disconnected");
    }

    @Override
    public void onError(String error) {
        Gdx.app.error(TAG, "Error: " + error);
    }

    // ========== Game Message Listener ==========

    @Override
    public void onMatchJoined(String matchId, String playerId, JsonValue state) {
        // Already in match
    }

    @Override
    public void onStateUpdate(JsonValue state) {
        Gdx.app.log(TAG, "State update received");

        if (state != null) {
            // Update turn state
            String currentPlayer = state.getString("currentPlayer", "");
            isPlayerTurn = "player-1".equals(currentPlayer) || "P1".equals(currentPlayer);
            currentRound = state.getInt("round", currentRound);
            resetTimer();

            // Check for death choice event
            JsonValue deathChoice = state.get("pendingDeathChoice");
            if (deathChoice != null) {
                String chooserPlayer = deathChoice.getString("chooser", "");
                if ("player-1".equals(chooserPlayer) || "P1".equals(chooserPlayer)) {
                    showDeathChoiceDialog = true;
                    deathChoiceTimer = 5f;
                    deathChoiceKillerId = deathChoice.getString("killerId", "");
                }
            }

            // TODO: Parse and update units from state
            // This would parse state.units array and update our units list
        }
    }

    @Override
    public void onValidationError(String message, JsonValue action) {
        Gdx.app.error(TAG, "Action error: " + message);
        // Reset action mode on error
        currentActionMode = ActionMode.NONE;
        validMoveTargets.clear();
        validAttackTargets.clear();
    }

    @Override
    public void onGameOver(String winner, JsonValue state) {
        Gdx.app.log(TAG, "Game over! Winner: " + winner);

        boolean isVictory = "player-1".equals(winner) || "P1".equals(winner);

        Gdx.app.postRunnable(() -> {
            ScreenManager.getInstance().showResultScreen(isVictory, winner);
        });
    }

    @Override
    public void onPong() {
    }

    @Override
    public void onUnknownMessage(String type, JsonValue payload) {
        Gdx.app.log(TAG, "Unknown message: " + type);
    }

    // ========== Public Methods for Testing ==========

    /**
     * Show the death choice dialog (for testing).
     */
    public void showDeathChoice(String killerId) {
        this.showDeathChoiceDialog = true;
        this.deathChoiceTimer = 5f;
        this.deathChoiceKillerId = killerId;
    }
}
