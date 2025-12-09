package com.tactics.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonValue;
import com.tactics.client.TacticsGame;
import com.tactics.client.net.GameMessageHandler;
import com.tactics.client.net.IWebSocketClient;
import com.tactics.client.net.WebSocketFactory;
import com.tactics.client.net.WebSocketListener;

/**
 * Draft screen - player selects hero class and minions.
 * Full Draft phase UI using placeholder graphics (colored rectangles).
 *
 * Layout:
 * - Timer at top
 * - Hero selection (2x3 grid)
 * - Skill preview panel
 * - Minion selection (3 buttons)
 * - Team preview area
 * - Ready button + opponent status
 */
public class DraftScreen extends BaseScreen implements WebSocketListener, GameMessageHandler.GameMessageListener {

    private static final String TAG = "DraftScreen";

    // ========== Hero Data ==========
    private static final String[] HERO_CLASSES = {"WARRIOR", "MAGE", "ROGUE", "CLERIC", "HUNTRESS", "DUELIST"};
    private static final Color[] HERO_COLORS = {
            new Color(0.8f, 0.3f, 0.3f, 1),  // WARRIOR - red
            new Color(0.3f, 0.3f, 0.8f, 1),  // MAGE - blue
            new Color(0.3f, 0.6f, 0.3f, 1),  // ROGUE - green
            new Color(0.8f, 0.8f, 0.3f, 1),  // CLERIC - yellow
            new Color(0.6f, 0.3f, 0.6f, 1),  // HUNTRESS - purple
            new Color(0.8f, 0.5f, 0.2f, 1)   // DUELIST - orange
    };

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
    private static final Color[] MINION_COLORS = {
            new Color(0.5f, 0.5f, 0.6f, 1),  // TANK - gray
            new Color(0.4f, 0.6f, 0.4f, 1),  // ARCHER - green
            new Color(0.6f, 0.4f, 0.5f, 1)   // ASSASSIN - dark pink
    };

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

    // Timer area (top)
    private static final float TIMER_Y = WORLD_HEIGHT - 50;

    // Hero selection area (2x3 grid)
    private static final float HERO_SECTION_Y = WORLD_HEIGHT - 80;
    private static final float HERO_BUTTON_WIDTH = 110;
    private static final float HERO_BUTTON_HEIGHT = 70;
    private static final float HERO_SPACING_X = 15;
    private static final float HERO_SPACING_Y = 10;
    private static final int HERO_COLS = 3;
    private static final int HERO_ROWS = 2;
    private static final float HERO_GRID_WIDTH = HERO_COLS * HERO_BUTTON_WIDTH + (HERO_COLS - 1) * HERO_SPACING_X;
    private static final float HERO_START_X = (WORLD_WIDTH - HERO_GRID_WIDTH) / 2;

    // Skill preview area
    private static final float SKILL_SECTION_Y = HERO_SECTION_Y - HERO_ROWS * (HERO_BUTTON_HEIGHT + HERO_SPACING_Y) - 50;
    private static final float SKILL_PANEL_HEIGHT = 60;
    private static final float SKILL_PANEL_WIDTH = 500;
    private static final float SKILL_PANEL_X = (WORLD_WIDTH - SKILL_PANEL_WIDTH) / 2;

    // Minion selection area
    private static final float MINION_SECTION_Y = SKILL_SECTION_Y - SKILL_PANEL_HEIGHT - 40;
    private static final float MINION_BUTTON_WIDTH = 140;
    private static final float MINION_BUTTON_HEIGHT = 60;
    private static final float MINION_SPACING = 20;
    private static final float MINION_GRID_WIDTH = 3 * MINION_BUTTON_WIDTH + 2 * MINION_SPACING;
    private static final float MINION_START_X = (WORLD_WIDTH - MINION_GRID_WIDTH) / 2;

    // Team preview area
    private static final float TEAM_SECTION_Y = MINION_SECTION_Y - MINION_BUTTON_HEIGHT - 50;
    private static final float TEAM_ICON_SIZE = 50;
    private static final float TEAM_ICON_SPACING = 15;

    // Ready button and opponent status
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
        backgroundColor = new Color(0.12f, 0.14f, 0.18f, 1);

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

    /**
     * Reset the draft state for a new game.
     */
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
        // Poll WebSocket messages
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
        // Title
        font.getData().setScale(1.8f);
        drawCenteredText("DRAFT PHASE", WORLD_WIDTH / 2, WORLD_HEIGHT - 20);
        font.getData().setScale(1f);

        // Timer display
        drawTimer();

        // Hero selection section
        drawHeroSection();

        // Skill preview panel
        drawSkillPreview();

        // Minion selection section
        drawMinionSection();

        // Team preview area
        drawTeamPreview();

        // Ready button
        drawReadyButton();

        // Opponent status
        drawOpponentStatus();
    }

    // ========== Drawing Methods ==========

    private void drawTimer() {
        // Timer background
        float timerWidth = 150;
        float timerHeight = 35;
        float timerX = WORLD_WIDTH - timerWidth - 20;

        Color bgColor = draftTimer < 10 ? new Color(0.4f, 0.1f, 0.1f, 1) : new Color(0.2f, 0.2f, 0.25f, 1);
        drawButton(timerX, TIMER_Y - timerHeight, timerWidth, timerHeight, bgColor, Color.DARK_GRAY);

        // Timer text
        Color timerColor = draftTimer < 10 ? Color.RED : Color.WHITE;
        font.getData().setScale(1.2f);
        String timerText = String.format("Time: %.0fs", draftTimer);
        drawCenteredText(timerText, timerX + timerWidth / 2, TIMER_Y - timerHeight / 2 + 5);
        font.getData().setScale(1f);
    }

    private void drawHeroSection() {
        // Section header
        drawText("SELECT HERO CLASS:", HERO_START_X, HERO_SECTION_Y, Color.CYAN);

        // Draw 2x3 grid of hero buttons
        for (int i = 0; i < HERO_CLASSES.length; i++) {
            int col = i % HERO_COLS;
            int row = i / HERO_COLS;

            float x = HERO_START_X + col * (HERO_BUTTON_WIDTH + HERO_SPACING_X);
            float y = HERO_SECTION_Y - 25 - row * (HERO_BUTTON_HEIGHT + HERO_SPACING_Y) - HERO_BUTTON_HEIGHT;

            // Button colors
            Color fillColor = HERO_COLORS[i].cpy();
            Color borderColor;

            if (i == selectedHeroIndex) {
                fillColor.a = 1f;
                borderColor = Color.WHITE;
            } else {
                fillColor.a = 0.5f;
                borderColor = new Color(0.4f, 0.4f, 0.4f, 1);
            }

            // Draw button
            drawButton(x, y, HERO_BUTTON_WIDTH, HERO_BUTTON_HEIGHT, fillColor, borderColor);

            // Draw hero name
            font.getData().setScale(0.9f);
            drawCenteredText(HERO_CLASSES[i], x + HERO_BUTTON_WIDTH / 2, y + HERO_BUTTON_HEIGHT / 2 + 5);
            font.getData().setScale(1f);
        }
    }

    private void drawSkillPreview() {
        // Section header
        drawText("SKILLS:", SKILL_PANEL_X, SKILL_SECTION_Y, Color.CYAN);

        // Panel background
        Color panelColor = new Color(0.15f, 0.15f, 0.2f, 1);
        drawButton(SKILL_PANEL_X, SKILL_SECTION_Y - 20 - SKILL_PANEL_HEIGHT,
                   SKILL_PANEL_WIDTH, SKILL_PANEL_HEIGHT, panelColor, Color.DARK_GRAY);

        // Skill names
        if (selectedHeroIndex >= 0) {
            String[] skills = HERO_SKILLS[selectedHeroIndex];
            float skillY = SKILL_SECTION_Y - 20 - SKILL_PANEL_HEIGHT / 2 + 5;

            font.getData().setScale(0.85f);
            StringBuilder skillText = new StringBuilder();
            for (int i = 0; i < skills.length; i++) {
                if (i > 0) skillText.append("  |  ");
                skillText.append((i + 1)).append(". ").append(skills[i]);
            }
            drawCenteredText(skillText.toString(), WORLD_WIDTH / 2, skillY);
            font.getData().setScale(1f);
        } else {
            font.getData().setScale(0.9f);
            drawCenteredText("(Select a hero to see skills)", WORLD_WIDTH / 2,
                           SKILL_SECTION_Y - 20 - SKILL_PANEL_HEIGHT / 2 + 5);
            font.getData().setScale(1f);
        }
    }

    private void drawMinionSection() {
        // Section header
        String header = "SELECT 2 MINIONS: (" + selectedMinionCount + "/2)";
        drawText(header, MINION_START_X, MINION_SECTION_Y, Color.CYAN);

        // Draw 3 minion buttons
        for (int i = 0; i < MINION_TYPES.length; i++) {
            float x = MINION_START_X + i * (MINION_BUTTON_WIDTH + MINION_SPACING);
            float y = MINION_SECTION_Y - 25 - MINION_BUTTON_HEIGHT;

            // Button colors
            Color fillColor = MINION_COLORS[i].cpy();
            Color borderColor;

            if (selectedMinions[i]) {
                fillColor.a = 1f;
                borderColor = Color.WHITE;
            } else {
                fillColor.a = 0.5f;
                borderColor = new Color(0.4f, 0.4f, 0.4f, 1);
            }

            // Draw button
            drawButton(x, y, MINION_BUTTON_WIDTH, MINION_BUTTON_HEIGHT, fillColor, borderColor);

            // Draw minion name
            font.getData().setScale(0.95f);
            drawCenteredText(MINION_TYPES[i], x + MINION_BUTTON_WIDTH / 2, y + MINION_BUTTON_HEIGHT / 2 + 5);
            font.getData().setScale(1f);
        }
    }

    private void drawTeamPreview() {
        // Section header
        drawText("YOUR TEAM:", (WORLD_WIDTH - 200) / 2, TEAM_SECTION_Y, Color.CYAN);

        // Calculate starting position for centered icons
        float totalWidth = TEAM_ICON_SIZE * 3 + TEAM_ICON_SPACING * 2;
        float startX = (WORLD_WIDTH - totalWidth) / 2;
        float y = TEAM_SECTION_Y - 25 - TEAM_ICON_SIZE;

        // Hero icon
        Color heroColor = selectedHeroIndex >= 0 ? HERO_COLORS[selectedHeroIndex] : new Color(0.3f, 0.3f, 0.3f, 1);
        Color heroBorder = selectedHeroIndex >= 0 ? Color.WHITE : Color.DARK_GRAY;
        drawButton(startX, y, TEAM_ICON_SIZE, TEAM_ICON_SIZE, heroColor, heroBorder);

        font.getData().setScale(0.8f);
        String heroLabel = selectedHeroIndex >= 0 ? "H" : "?";
        drawCenteredText(heroLabel, startX + TEAM_ICON_SIZE / 2, y + TEAM_ICON_SIZE / 2 + 5);

        // Minion icons
        int minionSlot = 0;
        for (int i = 0; i < MINION_TYPES.length && minionSlot < 2; i++) {
            float mx = startX + (minionSlot + 1) * (TEAM_ICON_SIZE + TEAM_ICON_SPACING);

            if (selectedMinions[i]) {
                drawButton(mx, y, TEAM_ICON_SIZE, TEAM_ICON_SIZE, MINION_COLORS[i], Color.WHITE);
                drawCenteredText("M" + (minionSlot + 1), mx + TEAM_ICON_SIZE / 2, y + TEAM_ICON_SIZE / 2 + 5);
                minionSlot++;
            }
        }

        // Empty slots for remaining minions
        for (int slot = minionSlot; slot < 2; slot++) {
            float mx = startX + (slot + 1) * (TEAM_ICON_SIZE + TEAM_ICON_SPACING);
            Color emptyColor = new Color(0.3f, 0.3f, 0.3f, 1);
            drawButton(mx, y, TEAM_ICON_SIZE, TEAM_ICON_SIZE, emptyColor, Color.DARK_GRAY);
            drawCenteredText("?", mx + TEAM_ICON_SIZE / 2, y + TEAM_ICON_SIZE / 2 + 5);
        }

        font.getData().setScale(1f);
    }

    private void drawReadyButton() {
        boolean canSubmit = selectedHeroIndex >= 0 && selectedMinionCount == 2 && !draftSubmitted;

        Color fillColor;
        Color borderColor;

        if (draftSubmitted) {
            fillColor = new Color(0.3f, 0.4f, 0.3f, 1);
            borderColor = Color.GREEN;
        } else if (canSubmit) {
            fillColor = new Color(0.2f, 0.5f, 0.2f, 1);
            borderColor = Color.WHITE;
        } else {
            fillColor = new Color(0.25f, 0.25f, 0.25f, 1);
            borderColor = Color.GRAY;
        }

        drawButton(READY_BUTTON_X, READY_BUTTON_Y, READY_BUTTON_WIDTH, READY_BUTTON_HEIGHT, fillColor, borderColor);

        String buttonText;
        if (draftSubmitted) {
            buttonText = "WAITING...";
        } else if (canSubmit) {
            buttonText = "READY";
        } else {
            buttonText = "SELECT TEAM";
        }

        font.getData().setScale(1.1f);
        drawCenteredText(buttonText, WORLD_WIDTH / 2, READY_BUTTON_Y + READY_BUTTON_HEIGHT / 2 + 5);
        font.getData().setScale(1f);
    }

    private void drawOpponentStatus() {
        String statusText;
        Color statusColor;

        if (opponentReady) {
            statusText = "Opponent: READY";
            statusColor = Color.GREEN;
        } else {
            statusText = "Opponent: Selecting...";
            statusColor = Color.YELLOW;
        }

        font.getData().setScale(0.9f);
        drawCenteredText(statusText, WORLD_WIDTH / 2, OPPONENT_STATUS_Y);
        font.getData().setScale(1f);
    }

    // ========== Input Handling ==========

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (draftSubmitted) return false;

        float worldX = screenToWorldX(screenX);
        float worldY = screenToWorldY(screenY);

        // Check hero buttons (2x3 grid)
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

        // Check minion buttons
        for (int i = 0; i < MINION_TYPES.length; i++) {
            float x = MINION_START_X + i * (MINION_BUTTON_WIDTH + MINION_SPACING);
            float y = MINION_SECTION_Y - 25 - MINION_BUTTON_HEIGHT;

            if (isPointInRect(worldX, worldY, x, y, MINION_BUTTON_WIDTH, MINION_BUTTON_HEIGHT)) {
                toggleMinion(i);
                return true;
            }
        }

        // Check ready button
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
            // Deselect
            selectedMinions[index] = false;
            selectedMinionCount--;
            Gdx.app.log(TAG, "Deselected minion: " + MINION_TYPES[index]);
        } else if (selectedMinionCount < 2) {
            // Select
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

        if (webSocket != null && webSocket.isConnected()) {
            String msg = messageHandler.createDraftPickAction("player-1", heroClass, minion1, minion2);
            webSocket.send(msg);
        }

        draftSubmitted = true;
        timerActive = false;
    }

    private void autoSubmitDraft() {
        // Auto-select if not already
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
    public void onMatchJoined(String matchId, String playerId, JsonValue state) {
        // Already joined
    }

    @Override
    public void onStateUpdate(JsonValue state) {
        if (state != null) {
            String phase = state.getString("phase", "");
            if ("BATTLE".equals(phase)) {
                // Transition to Battle screen
                Gdx.app.postRunnable(() -> {
                    ScreenManager.getInstance().showBattleScreen();
                });
            }

            // Check opponent ready status (if provided by server)
            JsonValue players = state.get("players");
            if (players != null) {
                // Check if opponent has submitted draft
                for (JsonValue player = players.child; player != null; player = player.next) {
                    String playerId = player.getString("playerId", "");
                    if (!"player-1".equals(playerId)) {
                        boolean ready = player.getBoolean("draftReady", false);
                        opponentReady = ready;
                    }
                }
            }
        }
    }

    @Override
    public void onValidationError(String message, JsonValue action) {
        Gdx.app.error(TAG, "Draft error: " + message);
        draftSubmitted = false; // Allow retry
    }

    @Override
    public void onGameOver(String winner, JsonValue state) {
        // Should not happen in draft
    }

    @Override
    public void onPong() {
    }

    @Override
    public void onUnknownMessage(String type, JsonValue payload) {
        Gdx.app.log(TAG, "Unknown message: " + type);
    }
}
