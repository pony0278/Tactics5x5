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
 * Connect screen - initial screen where player connects to server.
 * Shows server URL, connect button, and connection status.
 */
public class ConnectScreen extends BaseScreen implements WebSocketListener, GameMessageHandler.GameMessageListener {

    private static final String TAG = "ConnectScreen";
    private static final String SERVER_URL = "ws://localhost:8080/match";

    // Connection state
    private enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        WAITING_FOR_OPPONENT,
        ERROR
    }

    private ConnectionState state = ConnectionState.DISCONNECTED;
    private String statusMessage = "Not connected";
    private String errorMessage = "";

    // Player info
    private String playerId;
    private String matchId = "match-1"; // Default match ID

    // UI layout
    private static final float BUTTON_WIDTH = 200;
    private static final float BUTTON_HEIGHT = 50;
    private static final float BUTTON_X = (WORLD_WIDTH - BUTTON_WIDTH) / 2;
    private static final float BUTTON_Y = WORLD_HEIGHT / 2 - 50;

    // WebSocket
    private IWebSocketClient webSocket;
    private GameMessageHandler messageHandler;

    public ConnectScreen(TacticsGame game) {
        super(game);
        backgroundColor = new Color(0.1f, 0.15f, 0.25f, 1);

        // Generate player ID
        playerId = "player-" + System.currentTimeMillis() % 10000;

        // Setup message handler
        messageHandler = new GameMessageHandler();
        messageHandler.setMessageListener(this);
    }

    @Override
    public void show() {
        super.show();

        // Get or create WebSocket client
        try {
            webSocket = WebSocketFactory.getInstance();
            webSocket.setListener(this);
        } catch (IllegalStateException e) {
            Gdx.app.error(TAG, "WebSocket factory not initialized: " + e.getMessage());
            state = ConnectionState.ERROR;
            errorMessage = "WebSocket not available";
        }
    }

    @Override
    protected void update(float delta) {
        // Poll WebSocket messages
        processWebSocketMessages(webSocket, messageHandler);
    }

    @Override
    protected void draw() {
        // Title
        font.getData().setScale(2.5f);
        drawCenteredText("5x5 TACTICS", WORLD_WIDTH / 2, WORLD_HEIGHT - 80);
        font.getData().setScale(1f);

        // Server URL
        drawCenteredText("Server: " + SERVER_URL, WORLD_WIDTH / 2, WORLD_HEIGHT - 140);

        // Player ID
        drawCenteredText("Player ID: " + playerId, WORLD_WIDTH / 2, WORLD_HEIGHT - 170);

        // Connect button
        Color buttonColor = getButtonColor();
        Color borderColor = Color.WHITE;
        drawButton(BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, buttonColor, borderColor);

        // Button text
        String buttonText = getButtonText();
        drawCenteredText(buttonText, WORLD_WIDTH / 2, BUTTON_Y + BUTTON_HEIGHT / 2 + 6);

        // Status message
        Color statusColor = getStatusColor();
        drawText(statusMessage, BUTTON_X, BUTTON_Y - 30, statusColor);

        // Error message
        if (!errorMessage.isEmpty()) {
            drawText(errorMessage, BUTTON_X, BUTTON_Y - 60, Color.RED);
        }

        // Instructions
        font.getData().setScale(0.8f);
        drawCenteredText("Start server with: mvn exec:java", WORLD_WIDTH / 2, 80);
        drawCenteredText("Press Connect to join a match", WORLD_WIDTH / 2, 50);
        font.getData().setScale(1f);
    }

    private Color getButtonColor() {
        switch (state) {
            case CONNECTING:
                return new Color(0.5f, 0.5f, 0.2f, 1);
            case CONNECTED:
            case WAITING_FOR_OPPONENT:
                return new Color(0.2f, 0.5f, 0.2f, 1);
            case ERROR:
                return new Color(0.5f, 0.2f, 0.2f, 1);
            default:
                return new Color(0.2f, 0.3f, 0.5f, 1);
        }
    }

    private String getButtonText() {
        switch (state) {
            case CONNECTING:
                return "Connecting...";
            case CONNECTED:
                return "Joining...";
            case WAITING_FOR_OPPONENT:
                return "Waiting...";
            case ERROR:
                return "Retry";
            default:
                return "Connect";
        }
    }

    private Color getStatusColor() {
        switch (state) {
            case CONNECTED:
            case WAITING_FOR_OPPONENT:
                return Color.GREEN;
            case ERROR:
                return Color.RED;
            case CONNECTING:
                return Color.YELLOW;
            default:
                return Color.WHITE;
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float worldX = screenToWorldX(screenX);
        float worldY = screenToWorldY(screenY);

        // Check connect button
        if (isPointInRect(worldX, worldY, BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            onConnectClicked();
            return true;
        }

        return false;
    }

    private void onConnectClicked() {
        if (state == ConnectionState.CONNECTING || state == ConnectionState.WAITING_FOR_OPPONENT) {
            return; // Already connecting
        }

        if (state == ConnectionState.ERROR) {
            // Reset error state
            errorMessage = "";
        }

        if (webSocket == null) {
            state = ConnectionState.ERROR;
            errorMessage = "WebSocket not available";
            return;
        }

        state = ConnectionState.CONNECTING;
        statusMessage = "Connecting to server...";

        // Connect to server
        webSocket.connect(SERVER_URL);
    }

    // ========== WebSocket Listener ==========

    @Override
    public void onConnected() {
        Gdx.app.log(TAG, "Connected to server");
        state = ConnectionState.CONNECTED;
        statusMessage = "Connected! Joining match...";

        // Send join message
        String joinMsg = messageHandler.createJoinMatchMessage(matchId, playerId);
        webSocket.send(joinMsg);
    }

    @Override
    public void onMessage(String message) {
        Gdx.app.log(TAG, "Message received: " + message);
        messageHandler.parseMessage(message);
    }

    @Override
    public void onDisconnected() {
        Gdx.app.log(TAG, "Disconnected from server");
        if (state != ConnectionState.ERROR) {
            state = ConnectionState.DISCONNECTED;
            statusMessage = "Disconnected";
        }
    }

    @Override
    public void onError(String error) {
        Gdx.app.error(TAG, "WebSocket error: " + error);
        state = ConnectionState.ERROR;
        statusMessage = "Connection failed";
        errorMessage = error;
    }

    // ========== Game Message Listener ==========

    @Override
    public void onMatchJoined(String matchId, String playerId, JsonValue state) {
        Gdx.app.log(TAG, "Match joined: " + matchId);
        this.state = ConnectionState.WAITING_FOR_OPPONENT;
        statusMessage = "Waiting for opponent...";

        // Check game phase from state
        if (state != null) {
            String phase = state.getString("phase", "DRAFT");
            if ("DRAFT".equals(phase)) {
                // Transition to Draft screen
                Gdx.app.postRunnable(() -> {
                    ScreenManager.getInstance().showDraftScreen();
                });
            } else if ("BATTLE".equals(phase)) {
                // Transition to Battle screen
                Gdx.app.postRunnable(() -> {
                    ScreenManager.getInstance().showBattleScreen();
                });
            }
        }
    }

    @Override
    public void onStateUpdate(JsonValue state) {
        // Check if game is starting (both players joined)
        if (state != null) {
            String phase = state.getString("phase", "");
            if ("DRAFT".equals(phase)) {
                Gdx.app.postRunnable(() -> {
                    ScreenManager.getInstance().showDraftScreen();
                });
            } else if ("BATTLE".equals(phase)) {
                Gdx.app.postRunnable(() -> {
                    ScreenManager.getInstance().showBattleScreen();
                });
            }
        }
    }

    @Override
    public void onValidationError(String message, JsonValue action) {
        Gdx.app.error(TAG, "Validation error: " + message);
        errorMessage = message;
    }

    @Override
    public void onGameOver(String winner, JsonValue state) {
        // Should not happen on connect screen
    }

    @Override
    public void onPong() {
        Gdx.app.log(TAG, "Pong received");
    }

    @Override
    public void onUnknownMessage(String type, JsonValue payload) {
        Gdx.app.log(TAG, "Unknown message: " + type);
    }

    /**
     * Get the current player ID.
     */
    public String getPlayerId() {
        return playerId;
    }

    /**
     * Get the current match ID.
     */
    public String getMatchId() {
        return matchId;
    }
}
