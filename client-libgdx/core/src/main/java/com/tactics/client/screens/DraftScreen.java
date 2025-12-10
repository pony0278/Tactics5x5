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
import com.tactics.client.ui.GameColors;

/**
 * Draft screen - player selects hero class and minions.
 * Full Draft phase UI using placeholder graphics (colored rectangles).
 */
public class DraftScreen extends BaseScreen implements WebSocketListener, GameMessageHandler.GameMessageListener {

    private static final String TAG = "DraftScreen";

    // ========== Hero Data ==========
    private static final String[] HERO_CLASSES = {"WARRIOR", "MAGE", "ROGUE", "CLERIC", "HUNTRESS", "DUELIST"};

    // Skill names per hero class (from SKILL_SYSTEM_V3.md)
    private static final String[][] HERO_SKILLS = {
            {"Heroic Leap", "Shockwave", "Endure"},           // WARRIOR
            {"Elemental Blast", "Warp Beacon", "Wild Magic"}, // MAGE
            {"Smoke Bomb", "Death Mark", "Shadow Clone"},     // ROGUE
            {"Trinity", "Power of Many", "Ascended Form"},    // CLERIC
            {"Spirit Hawk", "Spectral Blades", "Nature's Power"}, // HUNTRESS
            {"Challenge", "Elemental Strike", "Feint"}        // DUELIST
    };

    // ========== Minion Data ==========
    private static final String[] MINION_TYPES = {"TANK", "ARCHER", "ASSASSIN"};

    // ========== Selection State ==========
    private int selectedHeroIndex = -1;
    private boolean[] selectedMinions = new boolean[3];
    private int selectedMinionCount = 0;
    private boolean draftSubmitted = false;
    private boolean opponentReady = false;

    // ========== Timer ==========
    private float draftTimer = 60f;
    private boolean timerActive = true;

    // ========== UI Layout Constants ==========
    private static final float TIMER_Y = WORLD_HEIGHT - 50;
    private static final float HERO_SECTION_Y = WORLD_HEIGHT - 80;
    private static final float HERO_BUTTON_WIDTH = 110;
    private static final float HERO_BUTTON_HEIGHT = 70;
    private static final float HERO_SPACING_X = 15;
    private static final float HERO_SPACING_Y = 10;
    private static final int HERO_COLS = 3;
    private static final int HERO_ROWS = 2;
    private static final float HERO_GRID_WIDTH = HERO_COLS * HERO_BUTTON_WIDTH + (HERO_COLS - 1) * HERO_SPACING_X;
    private static final float HERO_START_X = (WORLD_WIDTH - HERO_GRID_WIDTH) / 2;

    private static final float SKILL_SECTION_Y = HERO_SECTION_Y - HERO_ROWS * (HERO_BUTTON_HEIGHT + HERO_SPACING_Y) - 50;
    private static final float SKILL_PANEL_HEIGHT = 60;
    private static final float SKILL_PANEL_WIDTH = 500;
    private static final float SKILL_PANEL_X = (WORLD_WIDTH - SKILL_PANEL_WIDTH) / 2;

    private static final float MINION_SECTION_Y = SKILL_SECTION_Y - SKILL_PANEL_HEIGHT - 40;
    private static final float MINION_BUTTON_WIDTH = 140;
    private static final float MINION_BUTTON_HEIGHT = 60;
    private static final float MINION_SPACING = 20;
    private static final float MINION_GRID_WIDTH = 3 * MINION_BUTTON_WIDTH + 2 * MINION_SPACING;
    private static final float MINION_START_X = (WORLD_WIDTH - MINION_GRID_WIDTH) / 2;

    private static final float TEAM_SECTION_Y = MINION_SECTION_Y - MINION_BUTTON_HEIGHT - 50;
    private static final float TEAM_ICON_SIZE = 50;
    private static final float TEAM_ICON_SPACING = 15;

    private static final float READY_BUTTON_WIDTH = 200;
    private static final float READY_BUTTON_HEIGHT = 45;
    private static final float READY_BUTTON_X = (WORLD_WIDTH - READY_BUTTON_WIDTH) / 2;
    private static final float READY_BUTTON_Y = 60;
    private static final float OPPONENT_STATUS_Y = 25;

    // ========== WebSocket ==========
    private IWebSocketClient webSocket;
    private GameMessageHandler messageHandler;

    public DraftScreen(TacticsGame game) {
        super(game);
        backgroundColor = GameColors.BG_DRAFT;

        messageHandler = new GameMessageHandler();
        messageHandler.setMessageListener(this);
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
        selectedHeroIndex = -1;
        selectedMinions = new boolean[3];
        selectedMinionCount = 0;
        draftSubmitted = false;
        opponentReady = false;
        draftTimer = 60f;
        timerActive = true;
    }

    @Override
    protected void update(float delta) {
        processWebSocketMessages(webSocket, messageHandler);

        if (timerActive && !draftSubmitted) {
            draftTimer -= delta;
            if (draftTimer <= 0) {
                draftTimer = 0;
                timerActive = false;
                autoSubmitDraft();
            }
        }
    }

    @Override
    protected void draw() {
        if (!isTeaVM && font != null) font.getData().setScale(1.8f);
        drawCenteredText("DRAFT PHASE", WORLD_WIDTH / 2, WORLD_HEIGHT - 20);
        if (!isTeaVM && font != null) font.getData().setScale(1f);

        drawTimer();
        drawHeroSection();
        drawSkillPreview();
        drawMinionSection();
        drawTeamPreview();
        drawReadyButton();
        drawOpponentStatus();
    }

    // ========== Drawing Methods ==========

    private void drawTimer() {
        float timerWidth = 150;
        float timerHeight = 35;
        float timerX = WORLD_WIDTH - timerWidth - 20;

        Color bgColor = draftTimer < 10 ? GameColors.TIMER_BG_CRITICAL : GameColors.TIMER_BG_NORMAL;
        drawButton(timerX, TIMER_Y - timerHeight, timerWidth, timerHeight, bgColor, Color.DARK_GRAY);

        Color timerColor = draftTimer < 10 ? GameColors.TIMER_CRITICAL : GameColors.TIMER_NORMAL;
        if (!isTeaVM && font != null) font.getData().setScale(1.2f);
        drawCenteredText("Time: " + (int) draftTimer + "s", timerX + timerWidth / 2, TIMER_Y - timerHeight / 2 + 5);
        if (!isTeaVM && font != null) font.getData().setScale(1f);
    }

    private void drawHeroSection() {
        drawText("SELECT HERO CLASS:", HERO_START_X, HERO_SECTION_Y, Color.CYAN);

        for (int i = 0; i < HERO_CLASSES.length; i++) {
            int col = i % HERO_COLS;
            int row = i / HERO_COLS;

            float x = HERO_START_X + col * (HERO_BUTTON_WIDTH + HERO_SPACING_X);
            float y = HERO_SECTION_Y - 25 - row * (HERO_BUTTON_HEIGHT + HERO_SPACING_Y) - HERO_BUTTON_HEIGHT;

            Color fillColor = GameColors.getHeroColor(HERO_CLASSES[i]).cpy();
            Color borderColor;

            if (i == selectedHeroIndex) {
                fillColor.a = 1f;
                borderColor = Color.WHITE;
            } else {
                fillColor.a = 0.5f;
                borderColor = GameColors.UNSELECTED_BORDER;
            }

            drawButton(x, y, HERO_BUTTON_WIDTH, HERO_BUTTON_HEIGHT, fillColor, borderColor);

            if (!isTeaVM && font != null) font.getData().setScale(0.9f);
            drawCenteredText(HERO_CLASSES[i], x + HERO_BUTTON_WIDTH / 2, y + HERO_BUTTON_HEIGHT / 2 + 5);
            if (!isTeaVM && font != null) font.getData().setScale(1f);
        }
    }

    private void drawSkillPreview() {
        drawText("SKILLS:", SKILL_PANEL_X, SKILL_SECTION_Y, Color.CYAN);

        drawButton(SKILL_PANEL_X, SKILL_SECTION_Y - 20 - SKILL_PANEL_HEIGHT,
                   SKILL_PANEL_WIDTH, SKILL_PANEL_HEIGHT, GameColors.PANEL_BACKGROUND, Color.DARK_GRAY);

        if (selectedHeroIndex >= 0) {
            String[] skills = HERO_SKILLS[selectedHeroIndex];
            float skillY = SKILL_SECTION_Y - 20 - SKILL_PANEL_HEIGHT / 2 + 5;

            if (!isTeaVM && font != null) font.getData().setScale(0.85f);
            StringBuilder skillText = new StringBuilder();
            for (int i = 0; i < skills.length; i++) {
                if (i > 0) skillText.append("  |  ");
                skillText.append((i + 1)).append(". ").append(skills[i]);
            }
            drawCenteredText(skillText.toString(), WORLD_WIDTH / 2, skillY);
            if (!isTeaVM && font != null) font.getData().setScale(1f);
        } else {
            if (!isTeaVM && font != null) font.getData().setScale(0.9f);
            drawCenteredText("(Select a hero to see skills)", WORLD_WIDTH / 2,
                           SKILL_SECTION_Y - 20 - SKILL_PANEL_HEIGHT / 2 + 5);
            if (!isTeaVM && font != null) font.getData().setScale(1f);
        }
    }

    private void drawMinionSection() {
        drawText("SELECT 2 MINIONS: (" + selectedMinionCount + "/2)", MINION_START_X, MINION_SECTION_Y, Color.CYAN);

        for (int i = 0; i < MINION_TYPES.length; i++) {
            float x = MINION_START_X + i * (MINION_BUTTON_WIDTH + MINION_SPACING);
            float y = MINION_SECTION_Y - 25 - MINION_BUTTON_HEIGHT;

            Color fillColor = GameColors.getMinionColor(MINION_TYPES[i]).cpy();
            Color borderColor;

            if (selectedMinions[i]) {
                fillColor.a = 1f;
                borderColor = Color.WHITE;
            } else {
                fillColor.a = 0.5f;
                borderColor = GameColors.UNSELECTED_BORDER;
            }

            drawButton(x, y, MINION_BUTTON_WIDTH, MINION_BUTTON_HEIGHT, fillColor, borderColor);

            if (!isTeaVM && font != null) font.getData().setScale(0.95f);
            drawCenteredText(MINION_TYPES[i], x + MINION_BUTTON_WIDTH / 2, y + MINION_BUTTON_HEIGHT / 2 + 5);
            if (!isTeaVM && font != null) font.getData().setScale(1f);
        }
    }

    private void drawTeamPreview() {
        drawText("YOUR TEAM:", (WORLD_WIDTH - 200) / 2, TEAM_SECTION_Y, Color.CYAN);

        float totalWidth = TEAM_ICON_SIZE * 3 + TEAM_ICON_SPACING * 2;
        float startX = (WORLD_WIDTH - totalWidth) / 2;
        float y = TEAM_SECTION_Y - 25 - TEAM_ICON_SIZE;

        // Hero icon
        Color heroColor = selectedHeroIndex >= 0 ? GameColors.getHeroColor(HERO_CLASSES[selectedHeroIndex]) : GameColors.BUTTON_DISABLED;
        Color heroBorder = selectedHeroIndex >= 0 ? Color.WHITE : Color.DARK_GRAY;
        drawButton(startX, y, TEAM_ICON_SIZE, TEAM_ICON_SIZE, heroColor, heroBorder);

        if (!isTeaVM && font != null) font.getData().setScale(0.8f);
        drawCenteredText(selectedHeroIndex >= 0 ? "H" : "?", startX + TEAM_ICON_SIZE / 2, y + TEAM_ICON_SIZE / 2 + 5);

        // Minion icons
        int minionSlot = 0;
        for (int i = 0; i < MINION_TYPES.length && minionSlot < 2; i++) {
            float mx = startX + (minionSlot + 1) * (TEAM_ICON_SIZE + TEAM_ICON_SPACING);

            if (selectedMinions[i]) {
                drawButton(mx, y, TEAM_ICON_SIZE, TEAM_ICON_SIZE, GameColors.getMinionColor(MINION_TYPES[i]), Color.WHITE);
                drawCenteredText("M" + (minionSlot + 1), mx + TEAM_ICON_SIZE / 2, y + TEAM_ICON_SIZE / 2 + 5);
                minionSlot++;
            }
        }

        // Empty slots
        for (int slot = minionSlot; slot < 2; slot++) {
            float mx = startX + (slot + 1) * (TEAM_ICON_SIZE + TEAM_ICON_SPACING);
            drawButton(mx, y, TEAM_ICON_SIZE, TEAM_ICON_SIZE, GameColors.BUTTON_DISABLED, Color.DARK_GRAY);
            drawCenteredText("?", mx + TEAM_ICON_SIZE / 2, y + TEAM_ICON_SIZE / 2 + 5);
        }

        if (!isTeaVM && font != null) font.getData().setScale(1f);
    }

    private void drawReadyButton() {
        boolean canSubmit = selectedHeroIndex >= 0 && selectedMinionCount == 2 && !draftSubmitted;

        Color fillColor, borderColor;
        if (draftSubmitted) {
            fillColor = new Color(0.3f, 0.4f, 0.3f, 1);
            borderColor = Color.GREEN;
        } else if (canSubmit) {
            fillColor = GameColors.BUTTON_READY;
            borderColor = Color.WHITE;
        } else {
            fillColor = GameColors.BUTTON_DISABLED;
            borderColor = Color.GRAY;
        }

        drawButton(READY_BUTTON_X, READY_BUTTON_Y, READY_BUTTON_WIDTH, READY_BUTTON_HEIGHT, fillColor, borderColor);

        String buttonText = draftSubmitted ? "WAITING..." : (canSubmit ? "READY" : "SELECT TEAM");
        if (!isTeaVM && font != null) font.getData().setScale(1.1f);
        drawCenteredText(buttonText, WORLD_WIDTH / 2, READY_BUTTON_Y + READY_BUTTON_HEIGHT / 2 + 5);
        if (!isTeaVM && font != null) font.getData().setScale(1f);
    }

    private void drawOpponentStatus() {
        String statusText = opponentReady ? "Opponent: READY" : "Opponent: Selecting...";
        Color statusColor = opponentReady ? Color.GREEN : Color.YELLOW;

        if (!isTeaVM && font != null) font.getData().setScale(0.9f);
        drawCenteredText(statusText, WORLD_WIDTH / 2, OPPONENT_STATUS_Y);
        if (!isTeaVM && font != null) font.getData().setScale(1f);
    }

    // ========== Input Handling ==========

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (draftSubmitted) return false;

        float worldX = screenToWorldX(screenX);
        float worldY = screenToWorldY(screenY);

        // Hero buttons
        for (int i = 0; i < HERO_CLASSES.length; i++) {
            int col = i % HERO_COLS;
            int row = i / HERO_COLS;

            float x = HERO_START_X + col * (HERO_BUTTON_WIDTH + HERO_SPACING_X);
            float y = HERO_SECTION_Y - 25 - row * (HERO_BUTTON_HEIGHT + HERO_SPACING_Y) - HERO_BUTTON_HEIGHT;

            if (isPointInRect(worldX, worldY, x, y, HERO_BUTTON_WIDTH, HERO_BUTTON_HEIGHT)) {
                selectedHeroIndex = i;
                Gdx.app.log(TAG, "Selected hero: " + HERO_CLASSES[i]);
                return true;
            }
        }

        // Minion buttons
        for (int i = 0; i < MINION_TYPES.length; i++) {
            float x = MINION_START_X + i * (MINION_BUTTON_WIDTH + MINION_SPACING);
            float y = MINION_SECTION_Y - 25 - MINION_BUTTON_HEIGHT;

            if (isPointInRect(worldX, worldY, x, y, MINION_BUTTON_WIDTH, MINION_BUTTON_HEIGHT)) {
                toggleMinion(i);
                return true;
            }
        }

        // Ready button
        if (isPointInRect(worldX, worldY, READY_BUTTON_X, READY_BUTTON_Y, READY_BUTTON_WIDTH, READY_BUTTON_HEIGHT)) {
            if (selectedHeroIndex >= 0 && selectedMinionCount == 2) {
                submitDraft();
                return true;
            }
        }

        return false;
    }

    private void toggleMinion(int index) {
        if (selectedMinions[index]) {
            selectedMinions[index] = false;
            selectedMinionCount--;
            Gdx.app.log(TAG, "Deselected minion: " + MINION_TYPES[index]);
        } else if (selectedMinionCount < 2) {
            selectedMinions[index] = true;
            selectedMinionCount++;
            Gdx.app.log(TAG, "Selected minion: " + MINION_TYPES[index]);
        }
    }

    // ========== Draft Submission ==========

    private void submitDraft() {
        if (draftSubmitted) return;

        String heroClass = HERO_CLASSES[selectedHeroIndex];
        String minion1 = null, minion2 = null;

        for (int i = 0; i < MINION_TYPES.length; i++) {
            if (selectedMinions[i]) {
                if (minion1 == null) {
                    minion1 = MINION_TYPES[i];
                } else {
                    minion2 = MINION_TYPES[i];
                }
            }
        }

        Gdx.app.log(TAG, "Submitting draft: " + heroClass + " + " + minion1 + ", " + minion2);

        GameSession session = GameSession.getInstance();
        if (webSocket != null && webSocket.isConnected() && session.hasValidSession()) {
            String msg = messageHandler.createSelectTeamMessage(
                session.getMatchId(),
                session.getPlayerId(),
                heroClass, minion1, minion2
            );
            Gdx.app.log(TAG, "Sending: " + msg);
            webSocket.send(msg);
        } else {
            Gdx.app.error(TAG, "Cannot submit draft: no valid session or connection");
        }

        draftSubmitted = true;
        timerActive = false;
    }

    private void autoSubmitDraft() {
        if (selectedHeroIndex < 0) {
            selectedHeroIndex = (int) (Math.random() * HERO_CLASSES.length);
        }

        while (selectedMinionCount < 2) {
            int idx = (int) (Math.random() * MINION_TYPES.length);
            if (!selectedMinions[idx]) {
                selectedMinions[idx] = true;
                selectedMinionCount++;
            }
        }

        submitDraft();
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
        if (state != null) {
            String phase = state.getString("phase", "");
            if ("BATTLE".equals(phase)) {
                Gdx.app.postRunnable(() -> ScreenManager.getInstance().showBattleScreen());
            }

            // Check opponent's ready state using server-assigned playerId
            String myPlayerId = GameSession.getInstance().getPlayerId();
            JsonValue players = state.get("players");
            if (players != null && myPlayerId != null) {
                for (JsonValue player = players.child; player != null; player = player.next) {
                    String pid = player.getString("playerId", "");
                    if (!myPlayerId.equals(pid)) {
                        opponentReady = player.getBoolean("draftReady", false);
                    }
                }
            }
        }
    }

    @Override
    public void onValidationError(String message, JsonValue action) {
        Gdx.app.error(TAG, "Draft error: " + message);
        draftSubmitted = false;
    }

    @Override
    public void onGameOver(String winner, JsonValue state) {}

    @Override
    public void onPong() {}

    @Override
    public void onUnknownMessage(String type, JsonValue payload) {
        Gdx.app.log(TAG, "Unknown message: " + type);
    }
}
