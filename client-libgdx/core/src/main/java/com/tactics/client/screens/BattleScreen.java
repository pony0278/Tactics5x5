package com.tactics.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonValue;
import com.tactics.client.GameSession;
import com.tactics.client.TacticsGame;
import com.tactics.client.net.GameMessageHandler;
import com.tactics.client.net.IWebSocketClient;
import com.tactics.client.net.WebSocketFactory;
import com.tactics.client.net.WebSocketListener;
import com.tactics.client.render.BoardRenderer;
import com.tactics.client.ui.DeathChoiceDialog;
import com.tactics.client.ui.GameColors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Battle screen - main game screen with 5x5 grid.
 * Coordinates input handling, WebSocket communication, and delegates rendering.
 */
public class BattleScreen extends BaseScreen implements WebSocketListener, GameMessageHandler.GameMessageListener {

    private static final String TAG = "BattleScreen";

    // ========== Action Mode ==========
    private enum ActionMode {
        NONE, MOVE, ATTACK, SKILL
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

    // ========== Buff Tiles ==========
    private List<BoardRenderer.BuffTileData> buffTiles = new ArrayList<>();

    // ========== Game State ==========
    private boolean isPlayerTurn = true;
    private int currentRound = 1;

    // ========== Timer ==========
    private float actionTimer = 10f;
    private boolean timerActive = true;

    // ========== UI Layout Constants ==========
    private static final float TURN_INDICATOR_Y = WORLD_HEIGHT - 50;
    private static final float TURN_INDICATOR_HEIGHT = 40;
    private static final float ACTION_PANEL_X = BoardRenderer.GRID_START_X + BoardRenderer.GRID_SIZE * BoardRenderer.CELL_SIZE + 30;
    private static final float ACTION_BUTTON_WIDTH = 130;
    private static final float ACTION_BUTTON_HEIGHT = 45;
    private static final float ACTION_BUTTON_SPACING = 10;
    private static final float INFO_PANEL_X = ACTION_PANEL_X;
    private static final float INFO_PANEL_Y = BoardRenderer.GRID_START_Y + 150;
    private static final float INFO_PANEL_WIDTH = 180;
    private static final float INFO_PANEL_HEIGHT = 150;

    // ========== Components ==========
    private BoardRenderer boardRenderer;
    private DeathChoiceDialog deathChoiceDialog;

    // ========== WebSocket ==========
    private IWebSocketClient webSocket;
    private GameMessageHandler messageHandler;

    public BattleScreen(TacticsGame game) {
        super(game);
        backgroundColor = GameColors.BG_BATTLE;

        messageHandler = new GameMessageHandler();
        messageHandler.setMessageListener(this);

        boardRenderer = new BoardRenderer(shapeRenderer, batch, font);
        deathChoiceDialog = new DeathChoiceDialog(WORLD_WIDTH, WORLD_HEIGHT);

        initPlaceholderUnits();
    }

    private void initPlaceholderUnits() {
        units.clear();
        units.add(new UnitData("ally-minion-1", 0, 0, false, true));
        units.add(new UnitData("ally-hero", 2, 1, true, true));
        units.add(new UnitData("ally-minion-2", 4, 0, false, true));
        units.add(new UnitData("enemy-minion-1", 0, 4, false, false));
        units.add(new UnitData("enemy-hero", 2, 3, true, false));
        units.add(new UnitData("enemy-minion-2", 4, 4, false, false));

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

    public void reset() {
        selectedUnit = null;
        currentActionMode = ActionMode.NONE;
        validMoveTargets.clear();
        validAttackTargets.clear();
        actionTimer = 10f;
        timerActive = true;
        isPlayerTurn = true;
        currentRound = 1;
        deathChoiceDialog.hide();
        buffTiles.clear();
        initPlaceholderUnits();
    }

    @Override
    protected void update(float delta) {
        processWebSocketMessages(webSocket, messageHandler);

        // Death choice dialog update
        if (deathChoiceDialog.isVisible()) {
            String autoSelected = deathChoiceDialog.update(delta);
            if (autoSelected != null) {
                Gdx.app.log(TAG, "Death choice timer expired - auto select: " + autoSelected);
            }
            return;
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
        GameSession session = GameSession.getInstance();
        if (webSocket != null && webSocket.isConnected() && session.hasValidSession()) {
            webSocket.send(messageHandler.createEndTurnAction(session.getMatchId(), session.getPlayerId()));
        }
        actionTimer = 10f;
    }

    @Override
    protected void draw() {
        drawTurnIndicator();

        // Use BoardRenderer for grid and units
        int selX = selectedUnit != null ? selectedUnit.x : -1;
        int selY = selectedUnit != null ? selectedUnit.y : -1;
        boardRenderer.renderGrid(validMoveTargets, validAttackTargets, currentActionMode.name(), selX, selY, true);
        boardRenderer.renderBuffTiles(buffTiles);

        List<BoardRenderer.UnitRenderData> renderUnits = convertUnitsForRenderer();
        BoardRenderer.UnitRenderData selectedRenderUnit = selectedUnit != null ? findRenderUnit(renderUnits, selectedUnit.id) : null;
        boardRenderer.renderUnits(renderUnits, selectedRenderUnit);

        drawActionButtons();
        drawUnitInfoPanel();

        // Death choice dialog renders on top
        deathChoiceDialog.render(batch, shapeRenderer, font);
    }

    private List<BoardRenderer.UnitRenderData> convertUnitsForRenderer() {
        List<BoardRenderer.UnitRenderData> result = new ArrayList<>();
        for (UnitData u : units) {
            result.add(new BoardRenderer.UnitRenderData(u.id, u.x, u.y, u.hp, u.maxHp, u.isHero, u.isAlly, u.buffs));
        }
        return result;
    }

    private BoardRenderer.UnitRenderData findRenderUnit(List<BoardRenderer.UnitRenderData> list, String id) {
        for (BoardRenderer.UnitRenderData u : list) {
            if (u.id.equals(id)) return u;
        }
        return null;
    }

    // ========== Turn Indicator ==========

    private void drawTurnIndicator() {
        Color bgColor = isPlayerTurn ? GameColors.TURN_PLAYER : GameColors.TURN_OPPONENT;
        drawButton(0, TURN_INDICATOR_Y - TURN_INDICATOR_HEIGHT, WORLD_WIDTH, TURN_INDICATOR_HEIGHT, bgColor, Color.DARK_GRAY);

        String turnText = isPlayerTurn ? "YOUR TURN" : "OPPONENT'S TURN";
        Color turnColor = isPlayerTurn ? Color.GREEN : Color.RED;
        if (!isTeaVM && font != null) font.getData().setScale(1.3f);
        drawText(turnText, 20, TURN_INDICATOR_Y - 10, turnColor);
        drawText("Round " + currentRound, 200, TURN_INDICATOR_Y - 10, Color.WHITE);

        if (selectedUnit != null) {
            String unitName = selectedUnit.isHero ? selectedUnit.heroClass : "Minion";
            drawText("Unit: " + unitName, 350, TURN_INDICATOR_Y - 10, Color.CYAN);
        }

        // Timer
        Color timerBgColor = actionTimer < 3 ? GameColors.TIMER_BG_CRITICAL : GameColors.TIMER_BG_NORMAL;
        float timerWidth = 120;
        float timerX = WORLD_WIDTH - timerWidth - 10;
        drawButton(timerX, TURN_INDICATOR_Y - TURN_INDICATOR_HEIGHT + 5, timerWidth, TURN_INDICATOR_HEIGHT - 10, timerBgColor, Color.DARK_GRAY);

        Color timerColor = actionTimer < 3 ? GameColors.TIMER_CRITICAL : (actionTimer < 5 ? GameColors.TIMER_WARNING : GameColors.TIMER_NORMAL);
        drawCenteredText(String.format("%.1fs", actionTimer), timerX + timerWidth / 2, TURN_INDICATOR_Y - 10);
        if (!isTeaVM && font != null) font.getData().setScale(1f);
    }

    // ========== Action Buttons ==========

    private void drawActionButtons() {
        String[] actions = {"MOVE", "ATTACK", "SKILL", "END TURN"};
        Color[] activeColors = {GameColors.BUTTON_MOVE, GameColors.BUTTON_ATTACK, GameColors.BUTTON_SKILL, GameColors.BUTTON_NEUTRAL};

        float y = BoardRenderer.GRID_START_Y + BoardRenderer.GRID_SIZE * BoardRenderer.CELL_SIZE - ACTION_BUTTON_HEIGHT;

        for (int i = 0; i < actions.length; i++) {
            boolean enabled = isPlayerTurn && selectedUnit != null && selectedUnit.isAlly;
            boolean isActive = (i == 0 && currentActionMode == ActionMode.MOVE) ||
                               (i == 1 && currentActionMode == ActionMode.ATTACK) ||
                               (i == 2 && currentActionMode == ActionMode.SKILL);

            if (i == 2 && (selectedUnit == null || !selectedUnit.isHero)) {
                enabled = false;
            }

            Color fillColor, borderColor;
            if (!enabled) {
                fillColor = GameColors.BUTTON_DISABLED;
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

            String text = actions[i];
            if (i == 2 && selectedUnit != null && selectedUnit.isHero && selectedUnit.skillCooldown > 0) {
                text = "SKILL (" + selectedUnit.skillCooldown + ")";
            }

            if (!isTeaVM && font != null) font.getData().setScale(0.85f);
            drawCenteredText(text, ACTION_PANEL_X + ACTION_BUTTON_WIDTH / 2, y + ACTION_BUTTON_HEIGHT / 2 + 5);
            if (!isTeaVM && font != null) font.getData().setScale(1f);

            y -= ACTION_BUTTON_HEIGHT + ACTION_BUTTON_SPACING;
        }
    }

    // ========== Unit Info Panel ==========

    private void drawUnitInfoPanel() {
        drawButton(INFO_PANEL_X, INFO_PANEL_Y - INFO_PANEL_HEIGHT, INFO_PANEL_WIDTH, INFO_PANEL_HEIGHT, GameColors.PANEL_BACKGROUND, Color.DARK_GRAY);

        if (!isTeaVM && font != null) font.getData().setScale(0.9f);
        drawText("UNIT INFO", INFO_PANEL_X + 10, INFO_PANEL_Y - 10, Color.CYAN);
        if (!isTeaVM && font != null) font.getData().setScale(0.8f);

        float lineY = INFO_PANEL_Y - 35;
        float lineSpacing = 20;

        if (selectedUnit != null) {
            String unitType = selectedUnit.isHero ? selectedUnit.heroClass : "MINION";
            String owner = selectedUnit.isAlly ? "Ally" : "Enemy";
            drawText(owner + " " + unitType, INFO_PANEL_X + 10, lineY, Color.WHITE);
            lineY -= lineSpacing;

            Color hpColor = selectedUnit.hp > selectedUnit.maxHp / 2 ? Color.GREEN : (selectedUnit.hp <= selectedUnit.maxHp / 4 ? Color.RED : Color.YELLOW);
            drawText("HP: " + selectedUnit.hp + "/" + selectedUnit.maxHp, INFO_PANEL_X + 10, lineY, hpColor);
            lineY -= lineSpacing;

            drawText("ATK: " + selectedUnit.atk, INFO_PANEL_X + 10, lineY, Color.WHITE);
            lineY -= lineSpacing;

            if (!selectedUnit.buffs.isEmpty()) {
                drawText("Buffs:", INFO_PANEL_X + 10, lineY, Color.CYAN);
                lineY -= lineSpacing;
                for (String buff : selectedUnit.buffs) {
                    drawText("  " + buff, INFO_PANEL_X + 10, lineY, GameColors.getBuffColor(buff));
                    lineY -= lineSpacing;
                }
            }

            if (selectedUnit.isHero) {
                String cdText = selectedUnit.skillCooldown > 0 ? "Skill CD: " + selectedUnit.skillCooldown : "Skill: READY";
                drawText(cdText, INFO_PANEL_X + 10, lineY, selectedUnit.skillCooldown > 0 ? Color.GRAY : Color.GREEN);
            }
        } else {
            drawText("(Select a unit)", INFO_PANEL_X + 10, lineY, Color.GRAY);
        }
        if (!isTeaVM && font != null) font.getData().setScale(1f);
    }

    // ========== Input Handling ==========

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float worldX = screenToWorldX(screenX);
        float worldY = screenToWorldY(screenY);

        // Death choice dialog has priority
        if (deathChoiceDialog.isVisible()) {
            String selected = deathChoiceDialog.handleInput(worldX, worldY);
            if (selected != null) {
                sendDeathChoice(selected);
            }
            return true;
        }

        if (!isPlayerTurn) return false;

        if (handleActionButtonClick(worldX, worldY)) return true;
        if (handleGridClick(worldX, worldY)) return true;

        return false;
    }

    private void sendDeathChoice(String buffType) {
        Gdx.app.log(TAG, "Death choice selected: " + buffType);
        GameSession session = GameSession.getInstance();
        if (webSocket != null && webSocket.isConnected() && session.hasValidSession()) {
            webSocket.send(messageHandler.createDeathChoiceAction(session.getMatchId(), session.getPlayerId(), buffType));
        }
    }

    private boolean handleActionButtonClick(float worldX, float worldY) {
        float y = BoardRenderer.GRID_START_Y + BoardRenderer.GRID_SIZE * BoardRenderer.CELL_SIZE - ACTION_BUTTON_HEIGHT;

        for (int i = 0; i < 4; i++) {
            if (isPointInRect(worldX, worldY, ACTION_PANEL_X, y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)) {
                if (i == 0) toggleActionMode(ActionMode.MOVE);
                else if (i == 1) toggleActionMode(ActionMode.ATTACK);
                else if (i == 2 && selectedUnit != null && selectedUnit.isHero) toggleActionMode(ActionMode.SKILL);
                else if (i == 3) sendEndTurn();
                return true;
            }
            y -= ACTION_BUTTON_HEIGHT + ACTION_BUTTON_SPACING;
        }
        return false;
    }

    private void toggleActionMode(ActionMode mode) {
        if (selectedUnit == null || !selectedUnit.isAlly) return;

        if (currentActionMode == mode) {
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

        int gridSize = BoardRenderer.GRID_SIZE;

        switch (currentActionMode) {
            case MOVE:
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        if (Math.abs(dx) + Math.abs(dy) > 2) continue;
                        int tx = selectedUnit.x + dx, ty = selectedUnit.y + dy;
                        if (tx >= 0 && tx < gridSize && ty >= 0 && ty < gridSize && getUnitAt(tx, ty) == null) {
                            validMoveTargets.add(tx + "," + ty);
                        }
                    }
                }
                break;
            case ATTACK:
                for (UnitData unit : units) {
                    if (unit.hp <= 0 || unit.isAlly) continue;
                    if (Math.abs(unit.x - selectedUnit.x) + Math.abs(unit.y - selectedUnit.y) <= 1) {
                        validAttackTargets.add(unit.x + "," + unit.y);
                    }
                }
                break;
            case SKILL:
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dy = -3; dy <= 3; dy++) {
                        int dist = Math.abs(dx) + Math.abs(dy);
                        if (dist > 3 || dist == 0) continue;
                        int tx = selectedUnit.x + dx, ty = selectedUnit.y + dy;
                        if (tx >= 0 && tx < gridSize && ty >= 0 && ty < gridSize) {
                            validAttackTargets.add(tx + "," + ty);
                        }
                    }
                }
                break;
        }
    }

    private boolean handleGridClick(float worldX, float worldY) {
        int cellX = boardRenderer.screenToGridX(worldX);
        int cellY = boardRenderer.screenToGridY(worldY);
        if (cellX < 0 || cellY < 0) return false;

        String posKey = cellX + "," + cellY;
        Gdx.app.log(TAG, "Grid click: " + cellX + ", " + cellY + " Mode: " + currentActionMode);

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

        // Select/deselect unit
        UnitData clickedUnit = getUnitAt(cellX, cellY);
        if (clickedUnit != null && clickedUnit.isAlly) {
            selectedUnit = clickedUnit;
        } else {
            selectedUnit = null;
        }
        currentActionMode = ActionMode.NONE;
        validMoveTargets.clear();
        validAttackTargets.clear();
        return true;
    }

    private UnitData getUnitAt(int x, int y) {
        for (UnitData unit : units) {
            if (unit.hp > 0 && unit.x == x && unit.y == y) return unit;
        }
        return null;
    }

    // ========== Action Sending ==========

    private void sendMoveAction(int targetX, int targetY) {
        if (selectedUnit == null) return;
        Gdx.app.log(TAG, "Sending MOVE to " + targetX + ", " + targetY);
        GameSession session = GameSession.getInstance();
        if (webSocket != null && webSocket.isConnected() && session.hasValidSession()) {
            webSocket.send(messageHandler.createMoveAction(session.getMatchId(), session.getPlayerId(), targetX, targetY));
        }
        selectedUnit.x = targetX;
        selectedUnit.y = targetY;
        currentActionMode = ActionMode.NONE;
        validMoveTargets.clear();
    }

    private void sendAttackAction(String targetId) {
        if (selectedUnit == null) return;
        Gdx.app.log(TAG, "Sending ATTACK to " + targetId);
        GameSession session = GameSession.getInstance();
        if (webSocket != null && webSocket.isConnected() && session.hasValidSession()) {
            webSocket.send(messageHandler.createAttackAction(session.getMatchId(), session.getPlayerId(), selectedUnit.x, selectedUnit.y, targetId));
        }
        currentActionMode = ActionMode.NONE;
        validAttackTargets.clear();
    }

    private void sendSkillAction(int targetX, int targetY) {
        if (selectedUnit == null || !selectedUnit.isHero) return;
        Gdx.app.log(TAG, "Sending USE_SKILL to " + targetX + ", " + targetY);
        GameSession session = GameSession.getInstance();
        if (webSocket != null && webSocket.isConnected() && session.hasValidSession()) {
            webSocket.send(messageHandler.createUseSkillAction(session.getMatchId(), session.getPlayerId(), "skill-1", targetX, targetY, null));
        }
        currentActionMode = ActionMode.NONE;
        validAttackTargets.clear();
    }

    private void sendEndTurn() {
        Gdx.app.log(TAG, "Sending END_TURN");
        GameSession session = GameSession.getInstance();
        if (webSocket != null && webSocket.isConnected() && session.hasValidSession()) {
            webSocket.send(messageHandler.createEndTurnAction(session.getMatchId(), session.getPlayerId()));
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
    public void onMatchJoined(String matchId, String playerId, JsonValue state) {}

    @Override
    public void onStateUpdate(JsonValue state) {
        Gdx.app.log(TAG, "State update received");
        if (state != null) {
            String myPlayerId = GameSession.getInstance().getPlayerId();
            String currentPlayer = state.getString("currentPlayer", "");
            // Check if it's our turn (using server-assigned playerId)
            isPlayerTurn = myPlayerId != null && myPlayerId.equals(currentPlayer);
            currentRound = state.getInt("round", currentRound);
            actionTimer = 10f;

            JsonValue deathChoice = state.get("pendingDeathChoice");
            if (deathChoice != null) {
                String chooser = deathChoice.getString("chooser", "");
                // Check if we need to make the death choice
                if (myPlayerId != null && myPlayerId.equals(chooser)) {
                    String killerId = deathChoice.getString("killerId", "");
                    deathChoiceDialog.show(killerId, this::sendDeathChoice);
                }
            }
        }
    }

    @Override
    public void onValidationError(String message, JsonValue action) {
        Gdx.app.error(TAG, "Action error: " + message);
        currentActionMode = ActionMode.NONE;
        validMoveTargets.clear();
        validAttackTargets.clear();
    }

    @Override
    public void onGameOver(String winner, JsonValue state) {
        Gdx.app.log(TAG, "Game over! Winner: " + winner);
        String myPlayerId = GameSession.getInstance().getPlayerId();
        boolean isVictory = myPlayerId != null && myPlayerId.equals(winner);
        Gdx.app.postRunnable(() -> ScreenManager.getInstance().showResultScreen(isVictory, winner));
    }

    @Override
    public void onPong() {}

    @Override
    public void onUnknownMessage(String type, JsonValue payload) {
        Gdx.app.log(TAG, "Unknown message: " + type);
    }

    // ========== Public Methods ==========

    public void showDeathChoice(String killerId) {
        deathChoiceDialog.show(killerId, this::sendDeathChoice);
    }
}
