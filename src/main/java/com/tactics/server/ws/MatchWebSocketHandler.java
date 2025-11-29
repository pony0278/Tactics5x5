package com.tactics.server.ws;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.server.core.ClientSlot;
import com.tactics.server.core.Match;
import com.tactics.server.core.MatchService;
import com.tactics.server.dto.*;

import java.util.Map;

/**
 * Main entry point for WebSocket events; routes messages to MatchService.
 */
public class MatchWebSocketHandler {

    private final MatchService matchService;
    private final ConnectionRegistry connectionRegistry;

    public MatchWebSocketHandler(MatchService matchService,
                                 ConnectionRegistry connectionRegistry) {
        this.matchService = matchService;
        this.connectionRegistry = connectionRegistry;
    }

    public MatchService getMatchService() {
        return matchService;
    }

    public ConnectionRegistry getConnectionRegistry() {
        return connectionRegistry;
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

                // Notify remaining player
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

    private void handleJoinMatch(ClientConnection connection, Map<String, Object> payload) {
        // Extract matchId from payload
        String matchId = getStringFromPayload(payload, "matchId");

        if (matchId == null) {
            sendValidationError(connection, "Missing matchId in join_match", null);
            return;
        }

        // Get or create match
        Match match = matchService.getOrCreateMatch(matchId);
        Map<ClientSlot, ClientConnection> connections = match.getConnections();

        // Assign player to first available slot
        String assignedPlayerId;
        ClientSlot assignedSlot;

        if (!connections.containsKey(ClientSlot.P1)) {
            assignedSlot = ClientSlot.P1;
            assignedPlayerId = "P1";
            connections.put(ClientSlot.P1, connection);
        } else if (!connections.containsKey(ClientSlot.P2)) {
            assignedSlot = ClientSlot.P2;
            assignedPlayerId = "P2";
            connections.put(ClientSlot.P2, connection);
        } else {
            sendValidationError(connection, "Match is full", null);
            return;
        }

        // Store match/player info on the connection for cleanup on disconnect
        connection.setMatchId(matchId);
        connection.setPlayerId(assignedPlayerId);

        GameState state = match.getState();

        // Serialize the GameState
        Map<String, Object> stateMap = matchService.getGameStateSerializer().toJsonMap(state);

        // Build MatchJoinedPayload with server-assigned playerId
        MatchJoinedPayload responsePayload = new MatchJoinedPayload(matchId, assignedPlayerId, stateMap);
        OutgoingMessage response = new OutgoingMessage("match_joined", responsePayload);

        // Send to this connection
        String jsonResponse = JsonHelper.toJson(response);
        connection.sendMessage(jsonResponse);

        // Notify if both players are now connected
        if (connections.size() == 2) {
            OutgoingMessage gameReady = new OutgoingMessage("game_ready",
                java.util.Map.of("message", "Both players connected. Game starting!"));
            String readyJson = JsonHelper.toJson(gameReady);
            broadcastToMatch(matchId, readyJson);
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

        // Build engine Action from ActionPayload
        Action action = buildAction(actionPayload);
        if (action == null) {
            sendValidationError(connection, "Invalid action type: " + actionPayload.getType(), actionPayload);
            return;
        }

        try {
            // Apply action via MatchService
            GameState newState = matchService.applyAction(matchId, new PlayerId(playerId), action);

            // Serialize new state
            Map<String, Object> stateMap = matchService.getGameStateSerializer().toJsonMap(newState);

            // Build appropriate response based on game over state
            OutgoingMessage response;
            if (newState.isGameOver()) {
                String winner = newState.getWinner() != null ? newState.getWinner().getValue() : null;
                GameOverPayload gameOverPayload = new GameOverPayload(winner, stateMap);
                response = new OutgoingMessage("game_over", gameOverPayload);
            } else {
                StateUpdatePayload updatePayload = new StateUpdatePayload(stateMap);
                response = new OutgoingMessage("state_update", updatePayload);
            }

            // Broadcast to all clients in the match
            String jsonResponse = JsonHelper.toJson(response);
            broadcastToMatch(matchId, jsonResponse);

        } catch (IllegalArgumentException e) {
            // Validation failed - send error only to sender
            sendValidationError(connection, e.getMessage(), actionPayload);
        }
    }

    private ActionPayload parseActionPayload(Map<String, Object> actionMap) {
        String type = getStringFromPayload(actionMap, "type");
        Integer targetX = getIntegerFromPayload(actionMap, "targetX");
        Integer targetY = getIntegerFromPayload(actionMap, "targetY");
        String targetUnitId = getStringFromPayload(actionMap, "targetUnitId");
        return new ActionPayload(type, targetX, targetY, targetUnitId);
    }

    private Action buildAction(ActionPayload actionPayload) {
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

        // Note: Action constructor expects PlayerId, but we pass null here
        // The playerId is passed separately to MatchService.applyAction
        return new Action(actionType, null, targetPosition, actionPayload.getTargetUnitId());
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
