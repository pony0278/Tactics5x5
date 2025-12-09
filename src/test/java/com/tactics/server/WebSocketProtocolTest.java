package com.tactics.server;

import com.tactics.engine.model.*;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;
import com.tactics.server.core.*;
import com.tactics.server.dto.*;
import com.tactics.server.ws.ClientConnection;
import com.tactics.server.ws.ConnectionRegistry;
import com.tactics.server.ws.JsonHelper;
import com.tactics.server.ws.MatchWebSocketHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WSP-Series: WebSocket Protocol Validation Tests
 *
 * Validates that all messages conform to docs/WS_PROTOCOL_V1.md:
 * - WSP-IN: Client → Server message validation
 * - WSP-OUT: Server → Client message format
 * - WSP-STATE: GameState serialization format
 * - WSP-ERR: Error message format
 */
@DisplayName("WSP-Series: WebSocket Protocol Tests")
public class WebSocketProtocolTest {

    private MatchService matchService;
    private ConnectionRegistry connectionRegistry;
    private MatchWebSocketHandler handler;
    private GameStateSerializer serializer;
    private FakeClientConnection connP1;
    private FakeClientConnection connP2;

    // ========================================================================
    // Test Double
    // ========================================================================

    static class FakeClientConnection implements ClientConnection {
        private final String id;
        private String matchId;
        private String playerId;
        public final List<String> sentMessages = new ArrayList<>();

        FakeClientConnection(String id) {
            this.id = id;
        }

        @Override
        public String getId() { return id; }

        @Override
        public String getMatchId() { return matchId; }

        @Override
        public void setMatchId(String matchId) { this.matchId = matchId; }

        @Override
        public String getPlayerId() { return playerId; }

        @Override
        public void setPlayerId(String playerId) { this.playerId = playerId; }

        @Override
        public void sendMessage(String message) { sentMessages.add(message); }

        public void clearMessages() { sentMessages.clear(); }

        public String getLastMessage() {
            return sentMessages.isEmpty() ? null : sentMessages.get(sentMessages.size() - 1);
        }
    }

    // ========================================================================
    // Setup
    // ========================================================================

    @BeforeEach
    void setUp() {
        MatchRegistry registry = new MatchRegistry(new HashMap<>());
        RuleEngine ruleEngine = new RuleEngine();
        serializer = new GameStateSerializer();
        matchService = new MatchService(registry, ruleEngine, serializer);
        connectionRegistry = new ConnectionRegistry(new HashMap<>());
        handler = new MatchWebSocketHandler(matchService, connectionRegistry);
        handler.setUseTimers(false);

        connP1 = new FakeClientConnection("conn-p1");
        connP2 = new FakeClientConnection("conn-p2");
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private static String getType(String json) {
        IncomingMessage msg = JsonHelper.parseIncomingMessage(json);
        return msg != null ? msg.getType() : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getPayload(String json) {
        IncomingMessage msg = JsonHelper.parseIncomingMessage(json);
        return msg != null ? msg.getPayload() : null;
    }

    private void setupBothPlayersJoined(String matchId) {
        handler.onOpen(connP1);
        handler.onOpen(connP2);
        handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"" + matchId + "\"}}");
        handler.onMessage(connP2, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"" + matchId + "\"}}");
    }

    // ========================================================================
    // WSP-IN-Series: Client → Server Message Validation
    // ========================================================================

    @Nested
    @DisplayName("WSP-IN: Client → Server Messages")
    class ClientToServerTests {

        @Test
        @DisplayName("WSP-IN1: join_match requires matchId field")
        void joinMatchRequiresMatchId() {
            handler.onOpen(connP1);

            // Missing matchId
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{}}");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
            assertTrue(getPayload(connP1.sentMessages.get(0)).get("message").toString().contains("matchId"));
        }

        @Test
        @DisplayName("WSP-IN2: join_match with valid matchId succeeds")
        void joinMatchWithValidMatchIdSucceeds() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("match_joined", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("WSP-IN3: action requires matchId and playerId")
        void actionRequiresMatchIdAndPlayerId() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // Missing playerId
            handler.onMessage(connP1, "{\"type\":\"action\",\"payload\":{\"matchId\":\"test-match\",\"action\":{\"type\":\"END_TURN\"}}}");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("WSP-IN4: action requires action object")
        void actionRequiresActionObject() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // Missing action
            handler.onMessage(connP1, "{\"type\":\"action\",\"payload\":{\"matchId\":\"test-match\",\"playerId\":\"P1\"}}");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
            assertTrue(getPayload(connP1.sentMessages.get(0)).get("message").toString().contains("action"));
        }

        @Test
        @DisplayName("WSP-IN5: action.type is required")
        void actionTypeRequired() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // Missing action.type
            handler.onMessage(connP1, "{\"type\":\"action\",\"payload\":{\"matchId\":\"test-match\",\"playerId\":\"P1\",\"action\":{}}}");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("WSP-IN6: MOVE action requires targetPosition")
        void moveRequiresTargetPosition() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // MOVE without targetPosition
            handler.onMessage(connP1, "{\"type\":\"action\",\"payload\":{\"matchId\":\"test-match\",\"playerId\":\"P1\",\"action\":{\"type\":\"MOVE\"}}}");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("WSP-IN7: ATTACK action requires targetPosition and targetUnitId")
        void attackRequiresTargetPositionAndUnitId() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // ATTACK without targetUnitId
            handler.onMessage(connP1, "{\"type\":\"action\",\"payload\":{\"matchId\":\"test-match\",\"playerId\":\"P1\",\"action\":{\"type\":\"ATTACK\",\"targetX\":2,\"targetY\":3}}}");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("WSP-IN8: END_TURN action requires no targets")
        void endTurnRequiresNoTargets() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // END_TURN with minimal payload
            handler.onMessage(connP1, "{\"type\":\"action\",\"payload\":{\"matchId\":\"test-match\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}");

            // Should succeed (state_update) or error for different reason (not missing fields)
            String response = connP1.sentMessages.get(0);
            String type = getType(response);
            assertTrue(type.equals("state_update") || type.equals("validation_error"));
        }

        @Test
        @DisplayName("WSP-IN9: Malformed JSON returns validation_error")
        void malformedJsonReturnsError() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "not valid json {{{");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
            String errorMsg = getPayload(connP1.sentMessages.get(0)).get("message").toString().toLowerCase();
            assertTrue(errorMsg.contains("parse") || errorMsg.contains("json") || errorMsg.contains("invalid"),
                "Error message should mention parsing issue: " + errorMsg);
        }

        @Test
        @DisplayName("WSP-IN10: Empty message returns validation_error")
        void emptyMessageReturnsError() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("WSP-IN11: Unknown message type returns validation_error")
        void unknownTypeReturnsError() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "{\"type\":\"unknown_type\",\"payload\":{}}");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
            assertTrue(getPayload(connP1.sentMessages.get(0)).get("message").toString().contains("Unknown"));
        }

        @Test
        @DisplayName("WSP-IN12: Missing type field returns validation_error")
        void missingTypeReturnsError() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "{\"payload\":{\"matchId\":\"test\"}}");

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }
    }

    // ========================================================================
    // WSP-OUT-Series: Server → Client Message Format
    // ========================================================================

    @Nested
    @DisplayName("WSP-OUT: Server → Client Messages")
    class ServerToClientTests {

        @Test
        @DisplayName("WSP-OUT1: match_joined contains matchId, playerId, state")
        void matchJoinedFormat() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            String response = connP1.sentMessages.get(0);
            assertEquals("match_joined", getType(response));

            Map<String, Object> payload = getPayload(response);
            assertTrue(payload.containsKey("matchId"), "match_joined must contain matchId");
            assertTrue(payload.containsKey("playerId"), "match_joined must contain playerId");
            assertTrue(payload.containsKey("state"), "match_joined must contain state");

            assertEquals("test-match", payload.get("matchId"));
            assertTrue(payload.get("playerId").equals("P1") || payload.get("playerId").equals("P2"));
            assertNotNull(payload.get("state"));
        }

        @Test
        @DisplayName("WSP-OUT2: state_update contains state object")
        void stateUpdateFormat() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            handler.onMessage(connP1, "{\"type\":\"action\",\"payload\":{\"matchId\":\"test-match\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}");

            String response = connP1.sentMessages.get(0);
            assertEquals("state_update", getType(response));

            Map<String, Object> payload = getPayload(response);
            assertTrue(payload.containsKey("state"), "state_update must contain state");
        }

        @Test
        @DisplayName("WSP-OUT3: game_over contains winner and state")
        void gameOverFormat() {
            // Create custom game where P1 can kill P2's hero
            MatchRegistry registry = new MatchRegistry(new HashMap<>());
            List<Unit> units = Arrays.asList(
                new Unit("p1_hero", new PlayerId("P1"), 10, 10, 2, 1, new Position(2, 2), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 10, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0),
                new Unit("p2_hero", new PlayerId("P2"), 1, 1, 2, 1, new Position(2, 3), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 1, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0)
            );
            GameState customState = new GameState(new Board(5, 5), units, new PlayerId("P1"), false, null);
            registry.createMatch("game-over-test", customState);

            MatchService customService = new MatchService(registry, new RuleEngine(), new GameStateSerializer());
            MatchWebSocketHandler customHandler = new MatchWebSocketHandler(customService, new ConnectionRegistry(new HashMap<>()));
            customHandler.setUseTimers(false);

            FakeClientConnection p1 = new FakeClientConnection("p1");
            FakeClientConnection p2 = new FakeClientConnection("p2");
            customHandler.onOpen(p1);
            customHandler.onOpen(p2);
            customHandler.onMessage(p1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"game-over-test\"}}");
            customHandler.onMessage(p2, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"game-over-test\"}}");
            p1.clearMessages();

            // Kill P2's hero
            customHandler.onMessage(p1, "{\"type\":\"action\",\"payload\":{\"matchId\":\"game-over-test\",\"playerId\":\"P1\",\"action\":{\"type\":\"ATTACK\",\"targetX\":2,\"targetY\":3,\"targetUnitId\":\"p2_hero\"}}}");

            String response = p1.getLastMessage();
            assertEquals("game_over", getType(response));

            Map<String, Object> payload = getPayload(response);
            assertTrue(payload.containsKey("winner"), "game_over must contain winner");
            assertTrue(payload.containsKey("state"), "game_over must contain state");
            assertEquals("P1", payload.get("winner"));
        }

        @Test
        @DisplayName("WSP-OUT4: validation_error contains message field")
        void validationErrorFormat() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{}}");

            String response = connP1.sentMessages.get(0);
            assertEquals("validation_error", getType(response));

            Map<String, Object> payload = getPayload(response);
            assertTrue(payload.containsKey("message"), "validation_error must contain message");
            assertNotNull(payload.get("message"));
            assertTrue(payload.get("message") instanceof String);
        }

        @Test
        @DisplayName("WSP-OUT5: game_ready message sent when both players join")
        void gameReadyFormat() {
            handler.onOpen(connP1);
            handler.onOpen(connP2);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");
            connP1.clearMessages();

            handler.onMessage(connP2, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            // Find game_ready message
            String gameReadyMsg = connP1.sentMessages.stream()
                .filter(m -> "game_ready".equals(getType(m)))
                .findFirst()
                .orElse(null);

            assertNotNull(gameReadyMsg, "game_ready should be sent when both players join");
            assertEquals("game_ready", getType(gameReadyMsg));
        }

        @Test
        @DisplayName("WSP-OUT6: player_disconnected contains playerId")
        void playerDisconnectedFormat() {
            setupBothPlayersJoined("test-match");
            connP2.clearMessages();

            handler.onClose(connP1);

            String response = connP2.getLastMessage();
            assertEquals("player_disconnected", getType(response));

            Map<String, Object> payload = getPayload(response);
            assertTrue(payload.containsKey("playerId"), "player_disconnected must contain playerId");
            assertEquals("P1", payload.get("playerId"));
        }
    }

    // ========================================================================
    // WSP-STATE-Series: GameState Serialization
    // ========================================================================

    @Nested
    @DisplayName("WSP-STATE: GameState Serialization")
    class GameStateSerializationTests {

        @Test
        @DisplayName("WSP-STATE1: GameState contains board with width and height")
        void gameStateContainsBoard() {
            handler.onOpen(connP1);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) payload.get("state");

            assertTrue(state.containsKey("board"), "State must contain board");

            @SuppressWarnings("unchecked")
            Map<String, Object> board = (Map<String, Object>) state.get("board");
            assertTrue(board.containsKey("width"), "Board must contain width");
            assertTrue(board.containsKey("height"), "Board must contain height");
            assertEquals(5, ((Number) board.get("width")).intValue());
            assertEquals(5, ((Number) board.get("height")).intValue());
        }

        @Test
        @DisplayName("WSP-STATE2: GameState contains units array")
        void gameStateContainsUnits() {
            handler.onOpen(connP1);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) payload.get("state");

            assertTrue(state.containsKey("units"), "State must contain units");
            assertTrue(state.get("units") instanceof List, "Units must be an array");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> units = (List<Map<String, Object>>) state.get("units");
            assertFalse(units.isEmpty(), "Units array should not be empty");
        }

        @Test
        @DisplayName("WSP-STATE3: Unit contains required fields")
        void unitContainsRequiredFields() {
            handler.onOpen(connP1);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) payload.get("state");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> units = (List<Map<String, Object>>) state.get("units");

            Map<String, Object> unit = units.get(0);

            // Required fields per protocol
            assertTrue(unit.containsKey("id"), "Unit must contain id");
            assertTrue(unit.containsKey("owner"), "Unit must contain owner");
            assertTrue(unit.containsKey("hp"), "Unit must contain hp");
            assertTrue(unit.containsKey("attack"), "Unit must contain attack");
            assertTrue(unit.containsKey("alive"), "Unit must contain alive");
            assertTrue(unit.containsKey("position"), "Unit must contain position");

            // Position structure
            @SuppressWarnings("unchecked")
            Map<String, Object> position = (Map<String, Object>) unit.get("position");
            assertTrue(position.containsKey("x"), "Position must contain x");
            assertTrue(position.containsKey("y"), "Position must contain y");
        }

        @Test
        @DisplayName("WSP-STATE4: GameState contains currentPlayer")
        void gameStateContainsCurrentPlayer() {
            handler.onOpen(connP1);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) payload.get("state");

            assertTrue(state.containsKey("currentPlayer"), "State must contain currentPlayer");
            assertTrue(state.get("currentPlayer") instanceof String, "currentPlayer must be a string");
        }

        @Test
        @DisplayName("WSP-STATE5: GameState contains gameOver boolean")
        void gameStateContainsGameOver() {
            handler.onOpen(connP1);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) payload.get("state");

            assertTrue(state.containsKey("gameOver"), "State must contain gameOver");
            assertTrue(state.get("gameOver") instanceof Boolean, "gameOver must be a boolean");
        }

        @Test
        @DisplayName("WSP-STATE6: GameState contains winner field (can be null)")
        void gameStateContainsWinner() {
            handler.onOpen(connP1);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) payload.get("state");

            assertTrue(state.containsKey("winner"), "State must contain winner field");
            // winner can be null when game is not over
        }

        @Test
        @DisplayName("WSP-STATE7: GameState contains unitBuffs")
        void gameStateContainsUnitBuffs() {
            handler.onOpen(connP1);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) payload.get("state");

            assertTrue(state.containsKey("unitBuffs"), "State must contain unitBuffs");
        }

        @Test
        @DisplayName("WSP-STATE8: GameState roundtrip serialization preserves data")
        void gameStateRoundtripPreservesData() {
            // Create a GameState
            List<Unit> units = Arrays.asList(
                new Unit("u1", new PlayerId("P1"), 10, 3, 2, 1, new Position(1, 0), true),
                new Unit("u2", new PlayerId("P2"), 8, 3, 2, 2, new Position(3, 4), true)
            );
            GameState original = new GameState(new Board(5, 5), units, new PlayerId("P1"), false, null);

            // Serialize
            Map<String, Object> serialized = serializer.toJsonMap(original);

            // Deserialize
            GameState deserialized = serializer.fromJsonMap(serialized);

            // Verify
            assertEquals(original.getBoard().getWidth(), deserialized.getBoard().getWidth());
            assertEquals(original.getBoard().getHeight(), deserialized.getBoard().getHeight());
            assertEquals(original.getUnits().size(), deserialized.getUnits().size());
            assertEquals(original.getCurrentPlayer().getValue(), deserialized.getCurrentPlayer().getValue());
            assertEquals(original.isGameOver(), deserialized.isGameOver());
        }
    }

    // ========================================================================
    // WSP-JSON-Series: JSON Serialization Tests
    // ========================================================================

    @Nested
    @DisplayName("WSP-JSON: JSON Serialization")
    class JsonSerializationTests {

        @Test
        @DisplayName("WSP-JSON1: OutgoingMessage serializes type and payload")
        void outgoingMessageFormat() {
            Map<String, Object> state = new HashMap<>();
            state.put("test", "value");
            StateUpdatePayload payload = new StateUpdatePayload(state);
            OutgoingMessage msg = new OutgoingMessage("state_update", payload);

            String json = JsonHelper.toJson(msg);

            assertTrue(json.contains("\"type\":\"state_update\""), "JSON must contain type");
            assertTrue(json.contains("\"payload\":"), "JSON must contain payload");
            assertTrue(json.contains("\"state\":"), "Payload must contain state");
        }

        @Test
        @DisplayName("WSP-JSON2: IncomingMessage parses type and payload")
        void incomingMessageParsing() {
            String json = "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-123\"}}";

            IncomingMessage msg = JsonHelper.parseIncomingMessage(json);

            assertNotNull(msg);
            assertEquals("join_match", msg.getType());
            assertNotNull(msg.getPayload());
            assertEquals("test-123", msg.getPayload().get("matchId"));
        }

        @Test
        @DisplayName("WSP-JSON3: Nested objects parse correctly")
        void nestedObjectsParse() {
            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"m1\",\"playerId\":\"P1\",\"action\":{\"type\":\"MOVE\",\"targetX\":2,\"targetY\":3}}}";

            IncomingMessage msg = JsonHelper.parseIncomingMessage(json);

            assertNotNull(msg);
            assertEquals("action", msg.getType());

            @SuppressWarnings("unchecked")
            Map<String, Object> action = (Map<String, Object>) msg.getPayload().get("action");
            assertNotNull(action);
            assertEquals("MOVE", action.get("type"));
            assertEquals(2, ((Number) action.get("targetX")).intValue());
            assertEquals(3, ((Number) action.get("targetY")).intValue());
        }

        @Test
        @DisplayName("WSP-JSON4: Arrays serialize and parse correctly")
        void arraysSerialization() {
            List<Unit> units = Arrays.asList(
                new Unit("u1", new PlayerId("P1"), 10, 3, 1, 1, new Position(0, 0), true),
                new Unit("u2", new PlayerId("P2"), 8, 3, 1, 1, new Position(4, 4), true)
            );
            GameState state = new GameState(new Board(5, 5), units, new PlayerId("P1"), false, null);

            Map<String, Object> serialized = serializer.toJsonMap(state);

            assertTrue(serialized.get("units") instanceof List);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unitsList = (List<Map<String, Object>>) serialized.get("units");
            assertEquals(2, unitsList.size());
        }

        @Test
        @DisplayName("WSP-JSON5: Boolean values serialize correctly")
        void booleanSerialization() {
            GameState state = new GameState(new Board(5, 5), new ArrayList<>(), new PlayerId("P1"), false, null);
            Map<String, Object> serialized = serializer.toJsonMap(state);

            assertEquals(false, serialized.get("gameOver"));

            // After game over
            GameState gameOverState = new GameState(new Board(5, 5), new ArrayList<>(), new PlayerId("P1"), true, new PlayerId("P1"));
            Map<String, Object> gameOverSerialized = serializer.toJsonMap(gameOverState);

            assertEquals(true, gameOverSerialized.get("gameOver"));
        }

        @Test
        @DisplayName("WSP-JSON6: Null values serialize correctly")
        void nullSerialization() {
            GameState state = new GameState(new Board(5, 5), new ArrayList<>(), new PlayerId("P1"), false, null);
            Map<String, Object> serialized = serializer.toJsonMap(state);

            assertTrue(serialized.containsKey("winner"));
            assertNull(serialized.get("winner"));
        }

        @Test
        @DisplayName("WSP-JSON7: Special characters in strings are escaped")
        void specialCharactersEscaped() {
            String original = "test\"with\\special\nchars";
            OutgoingMessage msg = new OutgoingMessage("test", Map.of("message", original));

            String json = JsonHelper.toJson(msg);

            // Should contain escaped characters
            assertTrue(json.contains("\\\""), "Quotes should be escaped");
            assertTrue(json.contains("\\\\"), "Backslashes should be escaped");
            assertTrue(json.contains("\\n"), "Newlines should be escaped");
        }
    }

    // ========================================================================
    // WSP-TIMER-Series: Timer Message Format (V3 extension)
    // ========================================================================

    @Nested
    @DisplayName("WSP-TIMER: Timer Message Format")
    class TimerMessageTests {

        @Test
        @DisplayName("WSP-TIMER1: TimerPayload contains required fields")
        void timerPayloadFormat() {
            TimerPayload timer = new TimerPayload(System.currentTimeMillis(), 10000, "ACTION");

            assertTrue(timer.getActionStartTime() > 0);
            assertEquals(10000, timer.getTimeoutMs());
            assertEquals("ACTION", timer.getTimerType());
        }

        @Test
        @DisplayName("WSP-TIMER2: TimerPayload serializes correctly")
        void timerPayloadSerialization() {
            long startTime = 1700000000000L;
            TimerPayload timer = new TimerPayload(startTime, 10000, "ACTION");
            Map<String, Object> state = new HashMap<>();
            StateUpdatePayload payload = new StateUpdatePayload(state, timer, "P1");
            OutgoingMessage msg = new OutgoingMessage("state_update", payload);

            String json = JsonHelper.toJson(msg);

            assertTrue(json.contains("\"timer\":"), "Should contain timer");
            assertTrue(json.contains("\"actionStartTime\":" + startTime), "Should contain actionStartTime");
            assertTrue(json.contains("\"timeoutMs\":10000"), "Should contain timeoutMs");
            assertTrue(json.contains("\"timerType\":\"ACTION\""), "Should contain timerType");
        }

        @Test
        @DisplayName("WSP-TIMER3: StateUpdatePayload can omit timer")
        void stateUpdateWithoutTimer() {
            Map<String, Object> state = new HashMap<>();
            state.put("currentPlayer", "P1");
            StateUpdatePayload payload = new StateUpdatePayload(state);
            OutgoingMessage msg = new OutgoingMessage("state_update", payload);

            String json = JsonHelper.toJson(msg);

            assertTrue(json.contains("\"state\":"), "Should contain state");
            // Timer should not be present when null
            assertFalse(json.contains("\"timer\":null"), "Should not contain timer:null");
        }
    }
}
