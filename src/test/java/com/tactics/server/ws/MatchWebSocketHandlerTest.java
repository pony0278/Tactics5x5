package com.tactics.server.ws;

import com.tactics.engine.action.Action;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;
import com.tactics.server.core.*;
import com.tactics.server.dto.IncomingMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for MatchWebSocketHandler following SERVER_WS_TESTPLAN_V1.
 */
class MatchWebSocketHandlerTest {

    // ========================================================================
    // Test Doubles
    // ========================================================================

    /**
     * FakeClientConnection - captures all outgoing messages.
     */
    static class FakeClientConnection implements ClientConnection {
        private final String id;
        public final List<String> sentMessages = new ArrayList<>();

        FakeClientConnection(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void sendMessage(String message) {
            sentMessages.add(message);
        }

        public void clearMessages() {
            sentMessages.clear();
        }
    }

    /**
     * FakeMatchService - configurable responses for testing.
     */
    static class FakeMatchService extends MatchService {
        private Match matchToReturn;
        private GameState applyActionResult;
        private String applyActionException;
        private Match findMatchResult;

        // Call tracking
        public int getOrCreateMatchCallCount = 0;
        public String lastGetOrCreateMatchId;
        public int applyActionCallCount = 0;
        public String lastApplyActionMatchId;
        public PlayerId lastApplyActionPlayerId;
        public Action lastApplyActionAction;
        public int findMatchCallCount = 0;

        FakeMatchService() {
            super(new MatchRegistry(new HashMap<>()), new RuleEngine(), new FakeGameStateSerializer());
        }

        void setMatchToReturn(Match match) {
            this.matchToReturn = match;
            this.findMatchResult = match;
        }

        void setApplyActionResult(GameState result) {
            this.applyActionResult = result;
            this.applyActionException = null;
        }

        void setApplyActionException(String message) {
            this.applyActionException = message;
            this.applyActionResult = null;
        }

        void setFindMatchResult(Match match) {
            this.findMatchResult = match;
        }

        @Override
        public Match getOrCreateMatch(String matchId) {
            getOrCreateMatchCallCount++;
            lastGetOrCreateMatchId = matchId;
            return matchToReturn;
        }

        @Override
        public GameState applyAction(String matchId, PlayerId playerId, Action action) {
            applyActionCallCount++;
            lastApplyActionMatchId = matchId;
            lastApplyActionPlayerId = playerId;
            lastApplyActionAction = action;

            if (applyActionException != null) {
                throw new IllegalArgumentException(applyActionException);
            }
            return applyActionResult;
        }

        @Override
        public Match findMatch(String matchId) {
            findMatchCallCount++;
            return findMatchResult;
        }

        @Override
        public GameStateSerializer getGameStateSerializer() {
            return new FakeGameStateSerializer();
        }

        public void reset() {
            getOrCreateMatchCallCount = 0;
            lastGetOrCreateMatchId = null;
            applyActionCallCount = 0;
            lastApplyActionMatchId = null;
            lastApplyActionPlayerId = null;
            lastApplyActionAction = null;
            findMatchCallCount = 0;
            matchToReturn = null;
            applyActionResult = null;
            applyActionException = null;
            findMatchResult = null;
        }
    }

    /**
     * FakeGameStateSerializer - returns simple maps for testing.
     */
    static class FakeGameStateSerializer extends GameStateSerializer {
        @Override
        public Map<String, Object> toJsonMap(GameState state) {
            Map<String, Object> result = new HashMap<>();
            result.put("currentPlayer", state.getCurrentPlayer() != null ? state.getCurrentPlayer().getValue() : null);
            result.put("isGameOver", state.isGameOver());
            result.put("winner", state.getWinner() != null ? state.getWinner().getValue() : null);
            return result;
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private static GameState createDummyGameState(boolean isGameOver, String winner) {
        Board board = new Board(5, 5);
        PlayerId winnerPlayer = winner != null ? new PlayerId(winner) : null;
        return new GameState(board, new ArrayList<>(), new PlayerId("P1"), isGameOver, winnerPlayer);
    }

    private static Match createDummyMatch(String matchId, GameState state, Map<ClientSlot, ClientConnection> connections) {
        return new Match(new MatchId(matchId), state, connections);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String json) {
        IncomingMessage msg = JsonHelper.parseIncomingMessage(json);
        if (msg == null) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("type", msg.getType());
        result.put("payload", msg.getPayload());
        return result;
    }

    private static String getType(String json) {
        Map<String, Object> parsed = parseJson(json);
        return parsed != null ? (String) parsed.get("type") : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getPayload(String json) {
        Map<String, Object> parsed = parseJson(json);
        return parsed != null ? (Map<String, Object>) parsed.get("payload") : null;
    }

    // ========================================================================
    // CR-Series: ConnectionRegistry Tests
    // ========================================================================

    @Nested
    @DisplayName("ConnectionRegistry Tests (CR-Series)")
    class ConnectionRegistryTests {

        private ConnectionRegistry registry;

        @BeforeEach
        void setUp() {
            registry = new ConnectionRegistry(new HashMap<>());
        }

        @Test
        @DisplayName("CR1 - register adds connection")
        void cr1_registerAddsConnection() {
            FakeClientConnection conn = new FakeClientConnection("c1");

            registry.register(conn);

            assertSame(conn, registry.findById("c1"));
        }

        @Test
        @DisplayName("CR2 - unregister removes connection")
        void cr2_unregisterRemovesConnection() {
            FakeClientConnection conn = new FakeClientConnection("c1");
            registry.register(conn);

            registry.unregister(conn);

            assertNull(registry.findById("c1"));
        }

        @Test
        @DisplayName("CR3 - findById returns null for unknown id")
        void cr3_findByIdReturnsNullForUnknownId() {
            assertNull(registry.findById("unknown"));
        }
    }

    // ========================================================================
    // WS-CONN-Series: Connection Events Tests
    // ========================================================================

    @Nested
    @DisplayName("Connection Events Tests (WS-CONN-Series)")
    class ConnectionEventsTests {

        private FakeMatchService fakeMatchService;
        private ConnectionRegistry connectionRegistry;
        private MatchWebSocketHandler handler;

        @BeforeEach
        void setUp() {
            fakeMatchService = new FakeMatchService();
            connectionRegistry = new ConnectionRegistry(new HashMap<>());
            handler = new MatchWebSocketHandler(fakeMatchService, connectionRegistry);
        }

        @Test
        @DisplayName("WS-CONN1 - onOpen registers connection")
        void wsConn1_onOpenRegistersConnection() {
            FakeClientConnection conn = new FakeClientConnection("c1");

            handler.onOpen(conn);

            assertSame(conn, connectionRegistry.findById("c1"));
        }

        @Test
        @DisplayName("WS-CONN2 - onClose unregisters connection")
        void wsConn2_onCloseUnregistersConnection() {
            FakeClientConnection conn = new FakeClientConnection("c1");
            handler.onOpen(conn);

            handler.onClose(conn);

            assertNull(connectionRegistry.findById("c1"));
        }
    }

    // ========================================================================
    // WS-JOIN-Series: join_match Flow Tests
    // ========================================================================

    @Nested
    @DisplayName("join_match Flow Tests (WS-JOIN-Series)")
    class JoinMatchTests {

        private FakeMatchService fakeMatchService;
        private ConnectionRegistry connectionRegistry;
        private MatchWebSocketHandler handler;
        private FakeClientConnection conn;

        @BeforeEach
        void setUp() {
            fakeMatchService = new FakeMatchService();
            connectionRegistry = new ConnectionRegistry(new HashMap<>());
            handler = new MatchWebSocketHandler(fakeMatchService, connectionRegistry);
            conn = new FakeClientConnection("c1");
        }

        @Test
        @DisplayName("WS-JOIN1 - join_match creates or fetches match and responds with match_joined")
        void wsJoin1_joinMatchReturnsMatchJoined() {
            GameState dummyState = createDummyGameState(false, null);
            Match dummyMatch = createDummyMatch("match-1", dummyState, new HashMap<>());
            fakeMatchService.setMatchToReturn(dummyMatch);

            String json = "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\"}}";
            handler.onMessage(conn, json);

            assertEquals(1, fakeMatchService.getOrCreateMatchCallCount);
            assertEquals("match-1", fakeMatchService.lastGetOrCreateMatchId);
            assertEquals(1, conn.sentMessages.size());

            String response = conn.sentMessages.get(0);
            assertEquals("match_joined", getType(response));
            Map<String, Object> payload = getPayload(response);
            assertEquals("match-1", payload.get("matchId"));
            assertEquals("P1", payload.get("playerId"));
            assertNotNull(payload.get("state"));
        }

        @Test
        @DisplayName("WS-JOIN2 - join_match with P2 still returns match_joined")
        void wsJoin2_joinMatchP2ReturnsMatchJoined() {
            GameState dummyState = createDummyGameState(false, null);
            Match dummyMatch = createDummyMatch("match-1", dummyState, new HashMap<>());
            fakeMatchService.setMatchToReturn(dummyMatch);
            FakeClientConnection connP2 = new FakeClientConnection("c2");

            String json = "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P2\"}}";
            handler.onMessage(connP2, json);

            assertEquals(1, fakeMatchService.getOrCreateMatchCallCount);
            assertEquals(1, connP2.sentMessages.size());

            String response = connP2.sentMessages.get(0);
            assertEquals("match_joined", getType(response));
            Map<String, Object> payload = getPayload(response);
            assertEquals("match-1", payload.get("matchId"));
            assertEquals("P2", payload.get("playerId"));
            assertNotNull(payload.get("state"));
        }

        @Test
        @DisplayName("WS-JOIN3 - malformed join_match payload returns validation_error")
        void wsJoin3_malformedJoinMatchReturnsValidationError() {
            // Missing playerId
            String json = "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"match-1\"}}";
            handler.onMessage(conn, json);

            assertEquals(0, fakeMatchService.getOrCreateMatchCallCount);
            assertEquals(1, conn.sentMessages.size());

            String response = conn.sentMessages.get(0);
            assertEquals("validation_error", getType(response));
            Map<String, Object> payload = getPayload(response);
            assertNotNull(payload.get("message"));
            assertTrue(((String) payload.get("message")).contains("Missing"));
        }

        @Test
        @DisplayName("WS-JOIN3b - missing matchId returns validation_error")
        void wsJoin3b_missingMatchIdReturnsValidationError() {
            // Missing matchId
            String json = "{\"type\":\"join_match\",\"payload\":{\"playerId\":\"P1\"}}";
            handler.onMessage(conn, json);

            assertEquals(0, fakeMatchService.getOrCreateMatchCallCount);
            assertEquals(1, conn.sentMessages.size());

            String response = conn.sentMessages.get(0);
            assertEquals("validation_error", getType(response));
        }
    }

    // ========================================================================
    // WS-ACT-Series: action Flow Tests
    // ========================================================================

    @Nested
    @DisplayName("action Flow Tests (WS-ACT-Series)")
    class ActionTests {

        private FakeMatchService fakeMatchService;
        private ConnectionRegistry connectionRegistry;
        private MatchWebSocketHandler handler;
        private FakeClientConnection connP1;
        private FakeClientConnection connP2;

        @BeforeEach
        void setUp() {
            fakeMatchService = new FakeMatchService();
            connectionRegistry = new ConnectionRegistry(new HashMap<>());
            handler = new MatchWebSocketHandler(fakeMatchService, connectionRegistry);
            connP1 = new FakeClientConnection("p1");
            connP2 = new FakeClientConnection("p2");
        }

        private void setupMatchWithBothPlayers() {
            Map<ClientSlot, ClientConnection> connections = new HashMap<>();
            connections.put(ClientSlot.P1, connP1);
            connections.put(ClientSlot.P2, connP2);
            GameState state = createDummyGameState(false, null);
            Match match = createDummyMatch("match-1", state, connections);
            fakeMatchService.setMatchToReturn(match);
            fakeMatchService.setFindMatchResult(match);
        }

        @Test
        @DisplayName("WS-ACT1 - valid action produces state_update broadcast")
        void wsAct1_validActionProducesStateUpdate() {
            setupMatchWithBothPlayers();
            GameState newState = createDummyGameState(false, null);
            fakeMatchService.setApplyActionResult(newState);

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(connP1, json);

            assertEquals(1, fakeMatchService.applyActionCallCount);
            assertEquals("match-1", fakeMatchService.lastApplyActionMatchId);

            // Both players should receive state_update
            assertEquals(1, connP1.sentMessages.size());
            assertEquals(1, connP2.sentMessages.size());

            assertEquals("state_update", getType(connP1.sentMessages.get(0)));
            assertEquals("state_update", getType(connP2.sentMessages.get(0)));

            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            assertNotNull(payload.get("state"));
        }

        @Test
        @DisplayName("WS-ACT2 - valid action that ends game produces game_over broadcast")
        void wsAct2_gameOverProducesGameOverBroadcast() {
            setupMatchWithBothPlayers();
            GameState gameOverState = createDummyGameState(true, "P1");
            fakeMatchService.setApplyActionResult(gameOverState);

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"ATTACK\",\"targetUnitId\":\"u2\"}}}";
            handler.onMessage(connP1, json);

            assertEquals(1, connP1.sentMessages.size());
            assertEquals(1, connP2.sentMessages.size());

            assertEquals("game_over", getType(connP1.sentMessages.get(0)));
            assertEquals("game_over", getType(connP2.sentMessages.get(0)));

            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            assertEquals("P1", payload.get("winner"));
            assertNotNull(payload.get("state"));
        }

        @Test
        @DisplayName("WS-ACT3 - invalid action returns validation_error to sender only")
        void wsAct3_invalidActionReturnsValidationErrorToSenderOnly() {
            setupMatchWithBothPlayers();
            fakeMatchService.setApplyActionException("Not your turn");

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(connP1, json);

            assertEquals(1, connP1.sentMessages.size());
            assertEquals(0, connP2.sentMessages.size());

            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            assertTrue(((String) payload.get("message")).contains("Not your turn"));
        }

        @Test
        @DisplayName("WS-ACT4 - action for unknown match returns validation_error")
        void wsAct4_unknownMatchReturnsValidationError() {
            fakeMatchService.setApplyActionException("Unknown match: unknown-match");

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"unknown-match\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(connP1, json);

            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            assertTrue(((String) payload.get("message")).contains("Unknown match"));
        }

        @Test
        @DisplayName("WS-ACT5 - malformed action payload returns validation_error")
        void wsAct5_malformedActionReturnsValidationError() {
            // Missing action.type
            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{}}}";
            handler.onMessage(connP1, json);

            assertEquals(0, fakeMatchService.applyActionCallCount);
            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("WS-ACT5b - missing action object returns validation_error")
        void wsAct5b_missingActionObjectReturnsValidationError() {
            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\"}}";
            handler.onMessage(connP1, json);

            assertEquals(0, fakeMatchService.applyActionCallCount);
            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("WS-ACT5c - invalid action type returns validation_error")
        void wsAct5c_invalidActionTypeReturnsValidationError() {
            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"INVALID_TYPE\"}}}";
            handler.onMessage(connP1, json);

            assertEquals(0, fakeMatchService.applyActionCallCount);
            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }
    }

    // ========================================================================
    // WS-GEN-Series: Generic Message Handling Tests
    // ========================================================================

    @Nested
    @DisplayName("Generic Message Handling Tests (WS-GEN-Series)")
    class GenericMessageTests {

        private FakeMatchService fakeMatchService;
        private ConnectionRegistry connectionRegistry;
        private MatchWebSocketHandler handler;
        private FakeClientConnection conn;

        @BeforeEach
        void setUp() {
            fakeMatchService = new FakeMatchService();
            connectionRegistry = new ConnectionRegistry(new HashMap<>());
            handler = new MatchWebSocketHandler(fakeMatchService, connectionRegistry);
            conn = new FakeClientConnection("c1");
        }

        @Test
        @DisplayName("WS-GEN1 - unknown message type returns validation_error")
        void wsGen1_unknownMessageTypeReturnsValidationError() {
            String json = "{\"type\":\"unknown_type\",\"payload\":{}}";
            handler.onMessage(conn, json);

            assertEquals(1, conn.sentMessages.size());
            assertEquals("validation_error", getType(conn.sentMessages.get(0)));
            Map<String, Object> payload = getPayload(conn.sentMessages.get(0));
            assertTrue(((String) payload.get("message")).contains("Unknown message type"));
        }

        @Test
        @DisplayName("WS-GEN2 - JSON parsing failure returns validation_error")
        void wsGen2_jsonParsingFailureReturnsValidationError() {
            handler.onMessage(conn, "this is not json");

            assertEquals(1, conn.sentMessages.size());
            assertEquals("validation_error", getType(conn.sentMessages.get(0)));
            Map<String, Object> payload = getPayload(conn.sentMessages.get(0));
            assertNotNull(payload.get("message"));
        }

        @Test
        @DisplayName("WS-GEN2b - empty string returns validation_error")
        void wsGen2b_emptyStringReturnsValidationError() {
            handler.onMessage(conn, "");

            assertEquals(1, conn.sentMessages.size());
            assertEquals("validation_error", getType(conn.sentMessages.get(0)));
        }

        @Test
        @DisplayName("WS-GEN2c - malformed JSON returns validation_error")
        void wsGen2c_malformedJsonReturnsValidationError() {
            handler.onMessage(conn, "{\"type\":}");

            assertEquals(1, conn.sentMessages.size());
            assertEquals("validation_error", getType(conn.sentMessages.get(0)));
        }
    }

    // ========================================================================
    // WS-BCAST-Series: Broadcasting Semantics Tests
    // ========================================================================

    @Nested
    @DisplayName("Broadcasting Semantics Tests (WS-BCAST-Series)")
    class BroadcastingTests {

        private FakeMatchService fakeMatchService;
        private ConnectionRegistry connectionRegistry;
        private MatchWebSocketHandler handler;
        private FakeClientConnection connP1;
        private FakeClientConnection connP2;

        @BeforeEach
        void setUp() {
            fakeMatchService = new FakeMatchService();
            connectionRegistry = new ConnectionRegistry(new HashMap<>());
            handler = new MatchWebSocketHandler(fakeMatchService, connectionRegistry);
            connP1 = new FakeClientConnection("p1");
            connP2 = new FakeClientConnection("p2");
        }

        private void setupMatchWithBothPlayers() {
            Map<ClientSlot, ClientConnection> connections = new HashMap<>();
            connections.put(ClientSlot.P1, connP1);
            connections.put(ClientSlot.P2, connP2);
            GameState state = createDummyGameState(false, null);
            Match match = createDummyMatch("match-1", state, connections);
            fakeMatchService.setMatchToReturn(match);
            fakeMatchService.setFindMatchResult(match);
        }

        @Test
        @DisplayName("WS-BCAST1 - state_update sent to all connected clients in match")
        void wsBcast1_stateUpdateSentToAllClients() {
            setupMatchWithBothPlayers();
            GameState newState = createDummyGameState(false, null);
            fakeMatchService.setApplyActionResult(newState);

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(connP1, json);

            assertEquals(1, connP1.sentMessages.size());
            assertEquals(1, connP2.sentMessages.size());

            assertEquals("state_update", getType(connP1.sentMessages.get(0)));
            assertEquals("state_update", getType(connP2.sentMessages.get(0)));

            // Verify same content
            assertEquals(connP1.sentMessages.get(0), connP2.sentMessages.get(0));
        }

        @Test
        @DisplayName("WS-BCAST2 - game_over sent to all connected clients")
        void wsBcast2_gameOverSentToAllClients() {
            setupMatchWithBothPlayers();
            GameState gameOverState = createDummyGameState(true, "P1");
            fakeMatchService.setApplyActionResult(gameOverState);

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"ATTACK\",\"targetUnitId\":\"u2\"}}}";
            handler.onMessage(connP1, json);

            assertEquals(1, connP1.sentMessages.size());
            assertEquals(1, connP2.sentMessages.size());

            assertEquals("game_over", getType(connP1.sentMessages.get(0)));
            assertEquals("game_over", getType(connP2.sentMessages.get(0)));

            // Verify same content
            assertEquals(connP1.sentMessages.get(0), connP2.sentMessages.get(0));
        }

        @Test
        @DisplayName("WS-BCAST3 - validation_error only sent to initiator")
        void wsBcast3_validationErrorOnlySentToInitiator() {
            setupMatchWithBothPlayers();
            fakeMatchService.setApplyActionException("Not your turn");

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P2\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(connP2, json);

            assertEquals(0, connP1.sentMessages.size());
            assertEquals(1, connP2.sentMessages.size());

            assertEquals("validation_error", getType(connP2.sentMessages.get(0)));
        }

        @Test
        @DisplayName("WS-BCAST1b - broadcast with only P1 connected")
        void wsBcast1b_broadcastWithOnlyP1Connected() {
            Map<ClientSlot, ClientConnection> connections = new HashMap<>();
            connections.put(ClientSlot.P1, connP1);
            connections.put(ClientSlot.P2, null);  // P2 not connected
            GameState state = createDummyGameState(false, null);
            Match match = createDummyMatch("match-1", state, connections);
            fakeMatchService.setMatchToReturn(match);
            fakeMatchService.setFindMatchResult(match);
            fakeMatchService.setApplyActionResult(createDummyGameState(false, null));

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(connP1, json);

            assertEquals(1, connP1.sentMessages.size());
            assertEquals(0, connP2.sentMessages.size());
            assertEquals("state_update", getType(connP1.sentMessages.get(0)));
        }
    }

    // ========================================================================
    // WS-SEP-Series: Separation of Concerns Tests
    // ========================================================================

    @Nested
    @DisplayName("Separation of Concerns Tests (WS-SEP-Series)")
    class SeparationTests {

        private FakeMatchService fakeMatchService;
        private ConnectionRegistry connectionRegistry;
        private MatchWebSocketHandler handler;
        private FakeClientConnection connP1;

        @BeforeEach
        void setUp() {
            fakeMatchService = new FakeMatchService();
            connectionRegistry = new ConnectionRegistry(new HashMap<>());
            handler = new MatchWebSocketHandler(fakeMatchService, connectionRegistry);
            connP1 = new FakeClientConnection("p1");
        }

        @Test
        @DisplayName("WS-SEP1 - handler does not modify GameState directly")
        void wsSep1_handlerDelegatesToMatchService() {
            Map<ClientSlot, ClientConnection> connections = new HashMap<>();
            connections.put(ClientSlot.P1, connP1);
            GameState state = createDummyGameState(false, null);
            Match match = createDummyMatch("match-1", state, connections);
            fakeMatchService.setMatchToReturn(match);
            fakeMatchService.setFindMatchResult(match);
            fakeMatchService.setApplyActionResult(createDummyGameState(false, null));

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(connP1, json);

            // Verify all state changes went through applyAction
            assertEquals(1, fakeMatchService.applyActionCallCount);
            assertEquals("match-1", fakeMatchService.lastApplyActionMatchId);
            assertNotNull(fakeMatchService.lastApplyActionAction);
        }

        @Test
        @DisplayName("WS-SEP1b - handler calls getOrCreateMatch for join_match")
        void wsSep1b_handlerCallsGetOrCreateMatch() {
            GameState dummyState = createDummyGameState(false, null);
            Match dummyMatch = createDummyMatch("match-1", dummyState, new HashMap<>());
            fakeMatchService.setMatchToReturn(dummyMatch);

            String json = "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\"}}";
            handler.onMessage(connP1, json);

            assertEquals(1, fakeMatchService.getOrCreateMatchCallCount);
            assertEquals("match-1", fakeMatchService.lastGetOrCreateMatchId);
        }

        @Test
        @DisplayName("WS-SEP1c - handler calls findMatch for broadcasting")
        void wsSep1c_handlerCallsFindMatchForBroadcasting() {
            Map<ClientSlot, ClientConnection> connections = new HashMap<>();
            connections.put(ClientSlot.P1, connP1);
            GameState state = createDummyGameState(false, null);
            Match match = createDummyMatch("match-1", state, connections);
            fakeMatchService.setMatchToReturn(match);
            fakeMatchService.setFindMatchResult(match);
            fakeMatchService.setApplyActionResult(createDummyGameState(false, null));

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(connP1, json);

            // After applyAction, handler calls findMatch for broadcasting
            assertTrue(fakeMatchService.findMatchCallCount > 0);
        }
    }

    // ========================================================================
    // Additional Edge Case Tests
    // ========================================================================

    @Nested
    @DisplayName("Additional Edge Cases")
    class EdgeCaseTests {

        private FakeMatchService fakeMatchService;
        private ConnectionRegistry connectionRegistry;
        private MatchWebSocketHandler handler;
        private FakeClientConnection conn;

        @BeforeEach
        void setUp() {
            fakeMatchService = new FakeMatchService();
            connectionRegistry = new ConnectionRegistry(new HashMap<>());
            handler = new MatchWebSocketHandler(fakeMatchService, connectionRegistry);
            conn = new FakeClientConnection("c1");
        }

        @Test
        @DisplayName("Action with MOVE type includes target position")
        void actionWithMoveIncludesTargetPosition() {
            Map<ClientSlot, ClientConnection> connections = new HashMap<>();
            connections.put(ClientSlot.P1, conn);
            GameState state = createDummyGameState(false, null);
            Match match = createDummyMatch("match-1", state, connections);
            fakeMatchService.setMatchToReturn(match);
            fakeMatchService.setFindMatchResult(match);
            fakeMatchService.setApplyActionResult(createDummyGameState(false, null));

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"MOVE\",\"targetX\":2,\"targetY\":3}}}";
            handler.onMessage(conn, json);

            assertEquals(1, fakeMatchService.applyActionCallCount);
            assertNotNull(fakeMatchService.lastApplyActionAction);
            assertNotNull(fakeMatchService.lastApplyActionAction.getTargetPosition());
            assertEquals(2, fakeMatchService.lastApplyActionAction.getTargetPosition().getX());
            assertEquals(3, fakeMatchService.lastApplyActionAction.getTargetPosition().getY());
        }

        @Test
        @DisplayName("Action with ATTACK type includes targetUnitId")
        void actionWithAttackIncludesTargetUnitId() {
            Map<ClientSlot, ClientConnection> connections = new HashMap<>();
            connections.put(ClientSlot.P1, conn);
            GameState state = createDummyGameState(false, null);
            Match match = createDummyMatch("match-1", state, connections);
            fakeMatchService.setMatchToReturn(match);
            fakeMatchService.setFindMatchResult(match);
            fakeMatchService.setApplyActionResult(createDummyGameState(false, null));

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"ATTACK\",\"targetUnitId\":\"unit-42\"}}}";
            handler.onMessage(conn, json);

            assertEquals(1, fakeMatchService.applyActionCallCount);
            assertNotNull(fakeMatchService.lastApplyActionAction);
            assertEquals("unit-42", fakeMatchService.lastApplyActionAction.getTargetUnitId());
        }

        @Test
        @DisplayName("game_over with null winner")
        void gameOverWithNullWinner() {
            Map<ClientSlot, ClientConnection> connections = new HashMap<>();
            connections.put(ClientSlot.P1, conn);
            GameState state = createDummyGameState(false, null);
            Match match = createDummyMatch("match-1", state, connections);
            fakeMatchService.setMatchToReturn(match);
            fakeMatchService.setFindMatchResult(match);
            // Game over with no winner (e.g., draw)
            fakeMatchService.setApplyActionResult(createDummyGameState(true, null));

            String json = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(conn, json);

            assertEquals(1, conn.sentMessages.size());
            assertEquals("game_over", getType(conn.sentMessages.get(0)));
            Map<String, Object> payload = getPayload(conn.sentMessages.get(0));
            assertNull(payload.get("winner"));
        }
    }
}
