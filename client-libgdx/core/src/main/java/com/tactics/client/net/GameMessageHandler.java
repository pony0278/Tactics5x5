package com.tactics.client.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Handles parsing and creation of game messages according to WS_PROTOCOL_V1.
 *
 * Message Types:
 * - Client → Server: join_match, action, ping
 * - Server → Client: match_joined, state_update, validation_error, game_over, pong
 */
public class GameMessageHandler {

    private static final String TAG = "GameMessageHandler";

    /**
     * Listener interface for game messages.
     */
    public interface GameMessageListener {
        void onMatchJoined(String matchId, String playerId, JsonValue state);

        void onStateUpdate(JsonValue state);

        void onValidationError(String message, JsonValue action);

        void onGameOver(String winner, JsonValue state);

        void onPong();

        void onUnknownMessage(String type, JsonValue payload);

        /**
         * Called when draft_ready message is received.
         * @param playerId The player who submitted their draft
         * @param heroClass The hero class they selected
         * @param draftComplete True if both players are now ready
         */
        default void onDraftReady(String playerId, String heroClass, boolean draftComplete) {}

        /**
         * Called when game_ready message is received.
         * @param phase The current game phase (usually "DRAFT")
         */
        default void onGameReady(String phase) {}

        /**
         * Called when your_turn message is received.
         * @param unitId The unit that should act
         * @param timerInfo Timer information (actionStartTime, timeoutMs, timerType)
         */
        default void onYourTurn(String unitId, JsonValue timerInfo) {}
    }

    private final JsonReader jsonReader = new JsonReader();
    private final Json json = new Json();
    private GameMessageListener messageListener;

    public GameMessageHandler() {
        json.setOutputType(JsonWriter.OutputType.json);
    }

    /**
     * Set the listener for parsed game messages.
     */
    public void setMessageListener(GameMessageListener listener) {
        this.messageListener = listener;
    }

    /**
     * Parse a raw JSON message from the server.
     *
     * @param rawMessage The raw JSON string
     */
    public void parseMessage(String rawMessage) {
        try {
            JsonValue root = jsonReader.parse(rawMessage);
            String type = root.getString("type", "");
            JsonValue payload = root.get("payload");

            if (payload == null) {
                Gdx.app.error(TAG, "Message missing payload: " + rawMessage);
                return;
            }

            switch (type) {
                case "match_joined":
                    handleMatchJoined(payload);
                    break;
                case "state_update":
                    handleStateUpdate(payload);
                    break;
                case "validation_error":
                    handleValidationError(payload);
                    break;
                case "game_over":
                    handleGameOver(payload);
                    break;
                case "pong":
                    handlePong();
                    break;
                case "draft_ready":
                    handleDraftReady(payload);
                    break;
                case "game_ready":
                    handleGameReady(payload);
                    break;
                case "your_turn":
                    handleYourTurn(payload);
                    break;
                default:
                    Gdx.app.log(TAG, "Unknown message type: " + type);
                    if (messageListener != null) {
                        messageListener.onUnknownMessage(type, payload);
                    }
            }

        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to parse message: " + e.getMessage());
        }
    }

    private void handleMatchJoined(JsonValue payload) {
        String matchId = payload.getString("matchId", "");
        String playerId = payload.getString("playerId", "");
        JsonValue state = payload.get("state");

        Gdx.app.log(TAG, "Match joined: " + matchId + " as player " + playerId);

        if (messageListener != null) {
            messageListener.onMatchJoined(matchId, playerId, state);
        }
    }

    private void handleStateUpdate(JsonValue payload) {
        JsonValue state = payload.get("state");

        Gdx.app.log(TAG, "State update received");

        if (messageListener != null) {
            messageListener.onStateUpdate(state);
        }
    }

    private void handleValidationError(JsonValue payload) {
        String message = payload.getString("message", "Unknown error");
        JsonValue action = payload.get("action");

        Gdx.app.log(TAG, "Validation error: " + message);

        if (messageListener != null) {
            messageListener.onValidationError(message, action);
        }
    }

    private void handleGameOver(JsonValue payload) {
        String winner = payload.getString("winner", "");
        JsonValue state = payload.get("state");

        Gdx.app.log(TAG, "Game over! Winner: " + winner);

        if (messageListener != null) {
            messageListener.onGameOver(winner, state);
        }
    }

    private void handlePong() {
        Gdx.app.log(TAG, "Pong received");

        if (messageListener != null) {
            messageListener.onPong();
        }
    }

    private void handleDraftReady(JsonValue payload) {
        String playerId = payload.getString("playerId", "");
        String heroClass = payload.getString("heroClass", "");
        boolean draftComplete = payload.getBoolean("draftComplete", false);

        Gdx.app.log(TAG, "Draft ready: " + playerId + " selected " + heroClass + ", complete=" + draftComplete);

        if (messageListener != null) {
            messageListener.onDraftReady(playerId, heroClass, draftComplete);
        }
    }

    private void handleGameReady(JsonValue payload) {
        String phase = payload.getString("phase", "DRAFT");

        Gdx.app.log(TAG, "Game ready, phase: " + phase);

        if (messageListener != null) {
            messageListener.onGameReady(phase);
        }
    }

    private void handleYourTurn(JsonValue payload) {
        String unitId = payload.getString("unitId", "");

        Gdx.app.log(TAG, "Your turn: " + unitId);

        if (messageListener != null) {
            messageListener.onYourTurn(unitId, payload);
        }
    }

    // ========== Client → Server Message Creation ==========

    /**
     * Create a join_match message.
     *
     * @param matchId  The match ID to join
     * @param playerId The player ID
     * @return JSON string
     */
    public String createJoinMatchMessage(String matchId, String playerId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"join_match\",\"payload\":{");
        sb.append("\"matchId\":\"").append(escapeJson(matchId)).append("\",");
        sb.append("\"playerId\":\"").append(escapeJson(playerId)).append("\"");
        sb.append("}}");
        return sb.toString();
    }

    /**
     * Create a MOVE action message.
     *
     * @param matchId  The match ID (from server's match_joined)
     * @param playerId The player ID (from server's match_joined)
     * @param targetX  Target X position
     * @param targetY  Target Y position
     * @return JSON string
     */
    public String createMoveAction(String matchId, String playerId, int targetX, int targetY) {
        return createActionMessage(matchId, playerId, "MOVE", targetX, targetY, null);
    }

    /**
     * Create an ATTACK action message.
     *
     * @param matchId      The match ID
     * @param playerId     The player ID
     * @param targetX      Target X position
     * @param targetY      Target Y position
     * @param targetUnitId The ID of the unit to attack
     * @return JSON string
     */
    public String createAttackAction(String matchId, String playerId, int targetX, int targetY, String targetUnitId) {
        return createActionMessage(matchId, playerId, "ATTACK", targetX, targetY, targetUnitId);
    }

    /**
     * Create a MOVE_AND_ATTACK action message.
     *
     * @param matchId      The match ID
     * @param playerId     The player ID
     * @param targetX      Movement target X position
     * @param targetY      Movement target Y position
     * @param targetUnitId The ID of the unit to attack
     * @return JSON string
     */
    public String createMoveAndAttackAction(String matchId, String playerId, int targetX, int targetY, String targetUnitId) {
        return createActionMessage(matchId, playerId, "MOVE_AND_ATTACK", targetX, targetY, targetUnitId);
    }

    /**
     * Create a USE_SKILL action message.
     *
     * @param matchId      The match ID
     * @param playerId     The player ID
     * @param skillId      The skill ID
     * @param targetX      Target X position (or -1 if not needed)
     * @param targetY      Target Y position (or -1 if not needed)
     * @param targetUnitId Target unit ID (or null if not needed)
     * @return JSON string
     */
    public String createUseSkillAction(String matchId, String playerId, String skillId, int targetX, int targetY, String targetUnitId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"action\",\"payload\":{");
        sb.append("\"matchId\":\"").append(escapeJson(matchId)).append("\",");
        sb.append("\"playerId\":\"").append(escapeJson(playerId)).append("\",");
        sb.append("\"action\":{");
        sb.append("\"type\":\"USE_SKILL\",");
        sb.append("\"skillId\":\"").append(escapeJson(skillId)).append("\"");
        if (targetX >= 0 && targetY >= 0) {
            sb.append(",\"targetPosition\":{\"x\":").append(targetX).append(",\"y\":").append(targetY).append("}");
        }
        if (targetUnitId != null) {
            sb.append(",\"targetUnitId\":\"").append(escapeJson(targetUnitId)).append("\"");
        }
        sb.append("}}}");
        return sb.toString();
    }

    /**
     * Create an END_TURN action message.
     *
     * @param matchId  The match ID
     * @param playerId The player ID
     * @return JSON string
     */
    public String createEndTurnAction(String matchId, String playerId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"action\",\"payload\":{");
        sb.append("\"matchId\":\"").append(escapeJson(matchId)).append("\",");
        sb.append("\"playerId\":\"").append(escapeJson(playerId)).append("\",");
        sb.append("\"action\":{\"type\":\"END_TURN\"}");
        sb.append("}}");
        return sb.toString();
    }

    /**
     * Create a select_team message for draft phase.
     * This is a separate message type, NOT an action.
     *
     * @param matchId    The match ID
     * @param playerId   The player ID
     * @param heroClass  The hero class selected
     * @param minion1    First minion type
     * @param minion2    Second minion type
     * @return JSON string
     */
    public String createSelectTeamMessage(String matchId, String playerId, String heroClass, String minion1, String minion2) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"select_team\",\"payload\":{");
        sb.append("\"matchId\":\"").append(escapeJson(matchId)).append("\",");
        sb.append("\"playerId\":\"").append(escapeJson(playerId)).append("\",");
        sb.append("\"heroClass\":\"").append(escapeJson(heroClass)).append("\",");
        sb.append("\"minions\":[\"").append(escapeJson(minion1)).append("\",\"").append(escapeJson(minion2)).append("\"]");
        sb.append("}}");
        return sb.toString();
    }

    /**
     * Create a DEATH_CHOICE action message.
     *
     * @param matchId  The match ID
     * @param playerId The player ID
     * @param buffType The buff type chosen (POWER, LIFE, SPEED, WEAKNESS, BLEED, SLOW)
     * @return JSON string
     */
    public String createDeathChoiceAction(String matchId, String playerId, String buffType) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"action\",\"payload\":{");
        sb.append("\"matchId\":\"").append(escapeJson(matchId)).append("\",");
        sb.append("\"playerId\":\"").append(escapeJson(playerId)).append("\",");
        sb.append("\"action\":{");
        sb.append("\"type\":\"DEATH_CHOICE\",");
        sb.append("\"buffType\":\"").append(escapeJson(buffType)).append("\"");
        sb.append("}}}");
        return sb.toString();
    }

    /**
     * Create a ping message.
     *
     * @return JSON string
     */
    public String createPingMessage() {
        return "{\"type\":\"ping\",\"payload\":{}}";
    }

    private String createActionMessage(String matchId, String playerId, String actionType, int targetX, int targetY, String targetUnitId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"action\",\"payload\":{");
        sb.append("\"matchId\":\"").append(escapeJson(matchId)).append("\",");
        sb.append("\"playerId\":\"").append(escapeJson(playerId)).append("\",");
        sb.append("\"action\":{");
        sb.append("\"type\":\"").append(actionType).append("\"");

        if (targetX >= 0 && targetY >= 0) {
            sb.append(",\"targetPosition\":{\"x\":").append(targetX).append(",\"y\":").append(targetY).append("}");
        }

        if (targetUnitId != null) {
            sb.append(",\"targetUnitId\":\"").append(escapeJson(targetUnitId)).append("\"");
        }

        sb.append("}}}");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
