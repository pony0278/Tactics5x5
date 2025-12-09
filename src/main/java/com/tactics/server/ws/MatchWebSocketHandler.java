package com.tactics.server.ws;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.server.core.ActionResult;
import com.tactics.server.core.ClientSlot;
import com.tactics.server.core.Match;
import com.tactics.server.core.MatchService;
import com.tactics.server.dto.*;
import com.tactics.server.timer.TimerCallback;
import com.tactics.server.timer.TimerConfig;
import com.tactics.server.timer.TimerType;

import java.util.Map;

/**
 * Main entry point for WebSocket events; routes messages to MatchService.
 * Implements TimerCallback to handle timeout notifications.
 */
public class MatchWebSocketHandler implements TimerCallback {

    private final MatchService matchService;
    private final ConnectionRegistry connectionRegistry;
    private boolean useTimers = true;

    public MatchWebSocketHandler(MatchService matchService,
                                 ConnectionRegistry connectionRegistry) {
        this.matchService = matchService;
        this.connectionRegistry = connectionRegistry;
        // Register this handler as the timer callback
        matchService.setTimerCallback(this);
    }

    public MatchService getMatchService() {
        return matchService;
    }

    public ConnectionRegistry getConnectionRegistry() {
        return connectionRegistry;
    }

    /**
     * Enable or disable timer integration (for testing).
     */
    public void setUseTimers(boolean useTimers) {
        this.useTimers = useTimers;
    }

    public void onOpen(ClientConnection connection) {
        connectionRegistry.register(connection);
    }

    public void onClose(ClientConnection connection) {
        connectionRegistry.unregister(connection);

        // Remove from match if joined
        String matchId = connection.getMatchId();
        String playerId = connection.getPlayerId();
        if (matchId != null && playerId != null) {
            Match match = matchService.findMatch(matchId);
            if (match != null) {
                ClientSlot slot = "P1".equals(playerId) ? ClientSlot.P1 : ClientSlot.P2;
                match.getConnections().remove(slot);

                // Notify remaining player (timer continues per TN-004)
                OutgoingMessage disconnectMsg = new OutgoingMessage("player_disconnected",
                    java.util.Map.of("playerId", playerId));
                String json = JsonHelper.toJson(disconnectMsg);
                broadcastToMatch(matchId, json);
            }
        }
    }

    public void onMessage(ClientConnection connection, String text) {
        // Validate input
        if (text == null || text.trim().isEmpty()) {
            sendValidationError(connection, "Empty message received", null);
            return;
        }

        // Parse JSON with error handling
        IncomingMessage message;
        try {
            message = JsonHelper.parseIncomingMessage(text);
        } catch (Exception e) {
            sendValidationError(connection, "Failed to parse JSON: " + e.getMessage(), null);
            return;
        }

        if (message == null || message.getType() == null) {
            sendValidationError(connection, "Invalid message format: missing 'type' field", null);
            return;
        }

        String type = message.getType();
        Map<String, Object> payload = message.getPayload();

        switch (type) {
            case "join_match":
                handleJoinMatch(connection, payload);
                break;
            case "action":
                handleAction(connection, payload);
                break;
            default:
                sendValidationError(connection, "Unknown message type: " + type, null);
                break;
        }
    }

    // ========== TimerCallback Implementation ==========

    @Override
    public void onActionTimeout(String matchId, PlayerId playerId, GameState newState) {
        // Serialize state
        Map<String, Object> stateMap = matchService.getGameStateSerializer().toJsonMap(newState);

        // Build timeout payload
        TimeoutPayload.PenaltyInfo penalty = new TimeoutPayload.PenaltyInfo("HERO_HP_LOSS", 1);

        TimerPayload nextTimer = null;
        String nextPlayerId = null;

        if (!newState.isGameOver()) {
            nextPlayerId = newState.getCurrentPlayer().getValue();
            long startTime = matchService.getTimerService().getStartTime(matchId, TimerType.ACTION);
            if (startTime > 0) {
                nextTimer = new TimerPayload(startTime, TimerConfig.ACTION_TIMEOUT_MS, "ACTION");
            }
        }

        TimeoutPayload timeoutPayload = new TimeoutPayload(
                "ACTION",
                playerId.getValue(),
                penalty,
                "END_TURN",
                stateMap,
                nextTimer,
                nextPlayerId
        );

        OutgoingMessage response = new OutgoingMessage("timeout", timeoutPayload);
        String jsonResponse = JsonHelper.toJson(response);
        broadcastToMatch(matchId, jsonResponse);

        // If game over, send game_over message too
        if (newState.isGameOver()) {
            String winner = newState.getWinner() != null ? newState.getWinner().getValue() : null;
            GameOverPayload gameOverPayload = new GameOverPayload(winner, stateMap);
            OutgoingMessage gameOverResponse = new OutgoingMessage("game_over", gameOverPayload);
            String gameOverJson = JsonHelper.toJson(gameOverResponse);
            broadcastToMatch(matchId, gameOverJson);
        }
    }

    @Override
    public void onDeathChoiceTimeout(String matchId, PlayerId playerId, GameState newState) {
        // Death Choice timeout - no HP penalty, default to obstacle
        Map<String, Object> stateMap = matchService.getGameStateSerializer().toJsonMap(newState);

        TimerPayload nextTimer = null;
        String nextPlayerId = null;

        if (!newState.isGameOver()) {
            nextPlayerId = newState.getCurrentPlayer().getValue();
            long startTime = matchService.getTimerService().getStartTime(matchId, TimerType.ACTION);
            if (startTime > 0) {
                nextTimer = new TimerPayload(startTime, TimerConfig.ACTION_TIMEOUT_MS, "ACTION");
            }
        }

        TimeoutPayload timeoutPayload = new TimeoutPayload(
                "DEATH_CHOICE",
                playerId.getValue(),
                null,  // No penalty for Death Choice timeout
                "SPAWN_OBSTACLE",
                stateMap,
                nextTimer,
                nextPlayerId
        );

        OutgoingMessage response = new OutgoingMessage("timeout", timeoutPayload);
        String jsonResponse = JsonHelper.toJson(response);
        broadcastToMatch(matchId, jsonResponse);
    }

    @Override
    public void onDraftTimeout(String matchId) {
        // Draft timeout - random selection handled by MatchService
        // Just notify clients
        OutgoingMessage response = new OutgoingMessage("draft_timeout",
                java.util.Map.of("message", "Draft time expired. Random selections applied."));
        String jsonResponse = JsonHelper.toJson(response);
        broadcastToMatch(matchId, jsonResponse);
    }

    // ========== Message Handlers ==========

    private void handleJoinMatch(ClientConnection connection, Map<String, Object> payload) {
        String matchId = validateJoinRequest(connection, payload);
        if (matchId == null) {
            return; // Validation error already sent
        }

        PlayerAssignment assignment = assignPlayerToMatch(connection, matchId);
        if (assignment == null) {
            return; // Error already sent
        }

        sendJoinConfirmation(connection, assignment);

        if (assignment.isGameReady()) {
            broadcastGameStart(assignment);
        }
    }

    // ========== Join Match Helpers ==========

    /**
     * Validates incoming join_match request data.
     * Returns matchId if valid, null otherwise (error sent to connection).
     */
    private String validateJoinRequest(ClientConnection connection, Map<String, Object> payload) {
        String matchId = getStringFromPayload(payload, "matchId");
        if (matchId == null || matchId.trim().isEmpty()) {
            sendValidationError(connection, "Missing matchId in join_match", null);
            return null;
        }
        return matchId;
    }

    /**
     * Result of player slot assignment.
     */
    private static class PlayerAssignment {
        private final Match match;
        private final String matchId;
        private final String playerId;
        private final boolean gameReady;

        PlayerAssignment(Match match, String matchId, String playerId, boolean gameReady) {
            this.match = match;
            this.matchId = matchId;
            this.playerId = playerId;
            this.gameReady = gameReady;
        }

        Match getMatch() { return match; }
        String getMatchId() { return matchId; }
        String getPlayerId() { return playerId; }
        boolean isGameReady() { return gameReady; }
    }

    /**
     * Assigns a player to the first available slot in the match.
     * Returns null if match is full (error sent to connection).
     */
    private PlayerAssignment assignPlayerToMatch(ClientConnection connection, String matchId) {
        Match match = matchService.getOrCreateMatch(matchId);
        Map<ClientSlot, ClientConnection> connections = match.getConnections();

        String assignedPlayerId;
        if (!connections.containsKey(ClientSlot.P1)) {
            assignedPlayerId = "P1";
            connections.put(ClientSlot.P1, connection);
        } else if (!connections.containsKey(ClientSlot.P2)) {
            assignedPlayerId = "P2";
            connections.put(ClientSlot.P2, connection);
        } else {
            sendValidationError(connection, "Match is full", null);
            return null;
        }

        connection.setMatchId(matchId);
        connection.setPlayerId(assignedPlayerId);

        boolean gameReady = connections.size() == 2;
        return new PlayerAssignment(match, matchId, assignedPlayerId, gameReady);
    }

    /**
     * Sends match_joined confirmation to the connecting player.
     */
    private void sendJoinConfirmation(ClientConnection connection, PlayerAssignment assignment) {
        GameState state = assignment.getMatch().getState();
        Map<String, Object> stateMap = matchService.getGameStateSerializer().toJsonMap(state);

        MatchJoinedPayload responsePayload = new MatchJoinedPayload(
            assignment.getMatchId(), assignment.getPlayerId(), stateMap);
        OutgoingMessage response = new OutgoingMessage("match_joined", responsePayload);

        String jsonResponse = JsonHelper.toJson(response);
        connection.sendMessage(jsonResponse);
    }

    /**
     * Broadcasts game_ready and starts the timer when both players have joined.
     */
    private void broadcastGameStart(PlayerAssignment assignment) {
        String matchId = assignment.getMatchId();

        OutgoingMessage gameReady = new OutgoingMessage("game_ready",
            java.util.Map.of("message", "Both players connected. Game starting!"));
        String readyJson = JsonHelper.toJson(gameReady);
        broadcastToMatch(matchId, readyJson);

        if (useTimers) {
            GameState state = assignment.getMatch().getState();
            sendYourTurnWithTimer(matchId, state);
        }
    }

    private void handleAction(ClientConnection connection, Map<String, Object> payload) {
        // Extract matchId, playerId, and action from payload
        String matchId = getStringFromPayload(payload, "matchId");
        String playerId = getStringFromPayload(payload, "playerId");
        @SuppressWarnings("unchecked")
        Map<String, Object> actionMap = (Map<String, Object>) payload.get("action");

        if (matchId == null || playerId == null) {
            sendValidationError(connection, "Missing matchId or playerId in action", null);
            return;
        }

        if (actionMap == null) {
            sendValidationError(connection, "Missing action in action request", null);
            return;
        }

        // Parse ActionPayload
        ActionPayload actionPayload = parseActionPayload(actionMap);
        if (actionPayload == null || actionPayload.getType() == null) {
            sendValidationError(connection, "Missing or invalid action type", null);
            return;
        }

        // Build engine Action from ActionPayload (include playerId for validation)
        Action action = buildAction(actionPayload, playerId);
        if (action == null) {
            sendValidationError(connection, "Invalid action type: " + actionPayload.getType(), actionPayload);
            return;
        }

        try {
            if (useTimers) {
                // Apply action with timer management
                ActionResult result = matchService.applyActionWithTimer(matchId, new PlayerId(playerId), action);
                handleActionResult(matchId, result);
            } else {
                // Legacy: Apply action without timer
                GameState newState = matchService.applyAction(matchId, new PlayerId(playerId), action);
                handleLegacyActionResult(matchId, newState);
            }

        } catch (IllegalArgumentException e) {
            // Validation failed - send error only to sender
            sendValidationError(connection, e.getMessage(), actionPayload);
        }
    }

    /**
     * Handles action result with timer info.
     */
    private void handleActionResult(String matchId, ActionResult result) {
        GameState newState = result.getNewState();
        Map<String, Object> stateMap = matchService.getGameStateSerializer().toJsonMap(newState);

        OutgoingMessage response;
        if (result.isGameOver()) {
            String winner = newState.getWinner() != null ? newState.getWinner().getValue() : null;
            GameOverPayload gameOverPayload = new GameOverPayload(winner, stateMap);
            response = new OutgoingMessage("game_over", gameOverPayload);
        } else {
            // Include timer info in state update
            TimerPayload timerPayload = null;
            String currentPlayerId = null;

            if (result.hasTimer()) {
                timerPayload = new TimerPayload(
                        result.getActionStartTime(),
                        result.getTimeoutMs(),
                        result.getTimerType().name()
                );
                currentPlayerId = result.getNextPlayer().getValue();
            }

            StateUpdatePayload updatePayload = new StateUpdatePayload(stateMap, timerPayload, currentPlayerId);
            response = new OutgoingMessage("state_update", updatePayload);
        }

        String jsonResponse = JsonHelper.toJson(response);
        broadcastToMatch(matchId, jsonResponse);
    }

    /**
     * Handles legacy action result (no timer).
     */
    private void handleLegacyActionResult(String matchId, GameState newState) {
        Map<String, Object> stateMap = matchService.getGameStateSerializer().toJsonMap(newState);

        OutgoingMessage response;
        if (newState.isGameOver()) {
            String winner = newState.getWinner() != null ? newState.getWinner().getValue() : null;
            GameOverPayload gameOverPayload = new GameOverPayload(winner, stateMap);
            response = new OutgoingMessage("game_over", gameOverPayload);
        } else {
            StateUpdatePayload updatePayload = new StateUpdatePayload(stateMap);
            response = new OutgoingMessage("state_update", updatePayload);
        }

        String jsonResponse = JsonHelper.toJson(response);
        broadcastToMatch(matchId, jsonResponse);
    }

    /**
     * Sends YOUR_TURN message with timer info to the current player.
     */
    private void sendYourTurnWithTimer(String matchId, GameState state) {
        // Start timer
        long startTime = matchService.startTurnTimer(matchId);
        if (startTime < 0) {
            return; // Failed to start timer
        }

        String currentPlayerId = state.getCurrentPlayer().getValue();

        // Build YOUR_TURN payload
        Map<String, Object> yourTurnPayload = java.util.Map.of(
                "unitId", getCurrentUnitId(state),
                "actionStartTime", startTime,
                "timeoutMs", TimerConfig.ACTION_TIMEOUT_MS,
                "timerType", "ACTION"
        );

        OutgoingMessage yourTurn = new OutgoingMessage("your_turn", yourTurnPayload);
        String json = JsonHelper.toJson(yourTurn);

        // Send to the current player
        Match match = matchService.findMatch(matchId);
        if (match != null) {
            ClientSlot slot = "P1".equals(currentPlayerId) ? ClientSlot.P1 : ClientSlot.P2;
            ClientConnection conn = match.getConnections().get(slot);
            if (conn != null) {
                conn.sendMessage(json);
            }
        }

        // Also broadcast state update with timer to all players
        Map<String, Object> stateMap = matchService.getGameStateSerializer().toJsonMap(state);
        TimerPayload timerPayload = new TimerPayload(startTime, TimerConfig.ACTION_TIMEOUT_MS, "ACTION");
        StateUpdatePayload updatePayload = new StateUpdatePayload(stateMap, timerPayload, currentPlayerId);
        OutgoingMessage stateUpdate = new OutgoingMessage("state_update", updatePayload);
        broadcastToMatch(matchId, JsonHelper.toJson(stateUpdate));
    }

    /**
     * Gets the current unit ID for YOUR_TURN message.
     */
    private String getCurrentUnitId(GameState state) {
        // In V3, this would be the specific unit acting
        // For now, return a generic identifier based on current player
        String playerId = state.getCurrentPlayer().getValue();
        return playerId.toLowerCase() + "_hero"; // e.g., "p1_hero"
    }

    // ========== Helper Methods ==========

    private ActionPayload parseActionPayload(Map<String, Object> actionMap) {
        String type = getStringFromPayload(actionMap, "type");
        Integer targetX = getIntegerFromPayload(actionMap, "targetX");
        Integer targetY = getIntegerFromPayload(actionMap, "targetY");
        String targetUnitId = getStringFromPayload(actionMap, "targetUnitId");
        return new ActionPayload(type, targetX, targetY, targetUnitId);
    }

    private Action buildAction(ActionPayload actionPayload, String playerId) {
        ActionType actionType;
        try {
            actionType = ActionType.valueOf(actionPayload.getType());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }

        Position targetPosition = null;
        if (actionPayload.getTargetX() != null && actionPayload.getTargetY() != null) {
            targetPosition = new Position(actionPayload.getTargetX(), actionPayload.getTargetY());
        }

        // Include playerId in Action for RuleEngine validation
        return new Action(actionType, new PlayerId(playerId), targetPosition, actionPayload.getTargetUnitId());
    }

    private void broadcastToMatch(String matchId, String jsonMessage) {
        Match match = matchService.findMatch(matchId);
        if (match == null) {
            return;
        }

        Map<ClientSlot, ClientConnection> connections = match.getConnections();
        if (connections == null) {
            return;
        }

        // Send to P1 if connected
        ClientConnection p1 = connections.get(ClientSlot.P1);
        if (p1 != null) {
            p1.sendMessage(jsonMessage);
        }

        // Send to P2 if connected
        ClientConnection p2 = connections.get(ClientSlot.P2);
        if (p2 != null) {
            p2.sendMessage(jsonMessage);
        }
    }

    private void sendValidationError(ClientConnection connection, String message, ActionPayload action) {
        ValidationErrorPayload errorPayload = new ValidationErrorPayload(message, action);
        OutgoingMessage response = new OutgoingMessage("validation_error", errorPayload);
        String jsonResponse = JsonHelper.toJson(response);
        connection.sendMessage(jsonResponse);
    }

    private String getStringFromPayload(Map<String, Object> payload, String key) {
        if (payload == null) return null;
        Object value = payload.get(key);
        return value instanceof String ? (String) value : null;
    }

    private Integer getIntegerFromPayload(Map<String, Object> payload, String key) {
        if (payload == null) return null;
        Object value = payload.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}
