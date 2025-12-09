package com.tactics.server;

import com.tactics.engine.model.*;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;
import com.tactics.server.core.*;
import com.tactics.server.dto.*;
import com.tactics.server.timer.TimerService;
import com.tactics.server.ws.ClientConnection;
import com.tactics.server.ws.ConnectionRegistry;
import com.tactics.server.ws.JsonHelper;
import com.tactics.server.ws.MatchWebSocketHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ERR-Series: Error Handling and Edge Case Tests
 *
 * Tests error scenarios to ensure:
 * - Appropriate error responses
 * - Game state remains consistent
 * - No crashes or exceptions leak to client
 */
@DisplayName("ERR-Series: Error Handling Tests")
public class ErrorHandlingTest {

    private MatchRegistry registry;
    private MatchService matchService;
    private ConnectionRegistry connectionRegistry;
    private MatchWebSocketHandler handler;
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

        public List<String> getMessagesOfType(String type) {
            List<String> result = new ArrayList<>();
            for (String msg : sentMessages) {
                if (type.equals(getType(msg))) {
                    result.add(msg);
                }
            }
            return result;
        }
    }

    // ========================================================================
    // Setup
    // ========================================================================

    @BeforeEach
    void setUp() {
        registry = new MatchRegistry(new HashMap<>());
        RuleEngine ruleEngine = new RuleEngine();
        GameStateSerializer serializer = new GameStateSerializer();
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

    private String createActionJson(String matchId, String playerId, String actionType) {
        return "{\"type\":\"action\",\"payload\":{\"matchId\":\"" + matchId +
               "\",\"playerId\":\"" + playerId + "\",\"action\":{\"type\":\"" + actionType + "\"}}}";
    }

    private String createMoveJson(String matchId, String playerId, int x, int y) {
        return "{\"type\":\"action\",\"payload\":{\"matchId\":\"" + matchId +
               "\",\"playerId\":\"" + playerId + "\",\"action\":{\"type\":\"MOVE\",\"targetX\":" + x + ",\"targetY\":" + y + "}}}";
    }

    private String createAttackJson(String matchId, String playerId, int x, int y, String targetId) {
        return "{\"type\":\"action\",\"payload\":{\"matchId\":\"" + matchId +
               "\",\"playerId\":\"" + playerId + "\",\"action\":{\"type\":\"ATTACK\",\"targetX\":" + x +
               ",\"targetY\":" + y + ",\"targetUnitId\":\"" + targetId + "\"}}}";
    }

    // ========================================================================
    // ERR-TURN: Invalid Turn Handling
    // ========================================================================

    @Nested
    @DisplayName("ERR-TURN: Invalid Turn Handling")
    class InvalidTurnTests {

        @Test
        @DisplayName("ERR-TURN1: Action during opponent's turn is rejected")
        void actionDuringOpponentTurnRejected() {
            // Given: Both players joined, P1's turn
            setupBothPlayersJoined("test-match");
            connP2.clearMessages();

            // When: P2 tries to act
            handler.onMessage(connP2, createActionJson("test-match", "P2", "END_TURN"));

            // Then: Should receive validation_error
            List<String> errors = connP2.getMessagesOfType("validation_error");
            assertEquals(1, errors.size());
            String errorMsg = getPayload(errors.get(0)).get("message").toString().toLowerCase();
            assertTrue(errorMsg.contains("turn") || errorMsg.contains("not your"),
                "Error should mention turn issue: " + errorMsg);
        }

        @Test
        @DisplayName("ERR-TURN2: Action with wrong player's unit is rejected")
        void actionWithWrongPlayersUnitRejected() {
            // Given: Custom game state where P1 tries to move P2's unit
            List<Unit> units = Arrays.asList(
                new Unit("p1_unit", new PlayerId("P1"), 10, 3, 2, 1, new Position(1, 1), true),
                new Unit("p2_unit", new PlayerId("P2"), 10, 3, 2, 1, new Position(3, 3), true)
            );
            GameState state = new GameState(new Board(5, 5), units, new PlayerId("P1"), false, null);
            registry.createMatch("wrong-unit-test", state);

            MatchWebSocketHandler customHandler = new MatchWebSocketHandler(
                new MatchService(registry, new RuleEngine(), new GameStateSerializer()),
                new ConnectionRegistry(new HashMap<>())
            );
            customHandler.setUseTimers(false);

            FakeClientConnection p1 = new FakeClientConnection("p1");
            FakeClientConnection p2 = new FakeClientConnection("p2");
            customHandler.onOpen(p1);
            customHandler.onOpen(p2);
            customHandler.onMessage(p1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"wrong-unit-test\"}}");
            customHandler.onMessage(p2, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"wrong-unit-test\"}}");
            p1.clearMessages();

            // When: P1 tries to move P2's unit
            customHandler.onMessage(p1, createMoveJson("wrong-unit-test", "P1", 3, 4));

            // Then: Should receive validation_error (no unit to move at that position)
            List<String> errors = p1.getMessagesOfType("validation_error");
            assertEquals(1, errors.size(), "Should receive validation error");
        }

        @Test
        @DisplayName("ERR-TURN3: Action after game over is rejected")
        void actionAfterGameOverRejected() {
            // Given: Game that's already over
            List<Unit> units = Arrays.asList(
                new Unit("p1_hero", new PlayerId("P1"), 10, 3, 2, 1, new Position(2, 2), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 10, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0)
                // P2 has no hero = P2 loses
            );
            GameState gameOverState = new GameState(new Board(5, 5), units, new PlayerId("P1"), true, new PlayerId("P1"));
            registry.createMatch("game-over-test", gameOverState);

            MatchWebSocketHandler customHandler = new MatchWebSocketHandler(
                new MatchService(registry, new RuleEngine(), new GameStateSerializer()),
                new ConnectionRegistry(new HashMap<>())
            );
            customHandler.setUseTimers(false);

            FakeClientConnection p1 = new FakeClientConnection("p1");
            customHandler.onOpen(p1);
            customHandler.onMessage(p1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"game-over-test\"}}");
            p1.clearMessages();

            // When: Try to act after game over
            customHandler.onMessage(p1, createActionJson("game-over-test", "P1", "END_TURN"));

            // Then: Should receive validation_error
            List<String> errors = p1.getMessagesOfType("validation_error");
            assertEquals(1, errors.size());
            String errorMsg = getPayload(errors.get(0)).get("message").toString().toLowerCase();
            assertTrue(errorMsg.contains("over") || errorMsg.contains("ended"),
                "Error should mention game over: " + errorMsg);
        }

        @Test
        @DisplayName("ERR-TURN4: State remains unchanged after invalid action")
        void stateUnchangedAfterInvalidAction() {
            // Given: Both players joined
            setupBothPlayersJoined("test-match");
            Match match = matchService.findMatch("test-match");
            GameState stateBefore = match.getState();
            String currentPlayerBefore = stateBefore.getCurrentPlayer().getValue();
            connP2.clearMessages();

            // When: P2 tries invalid action
            handler.onMessage(connP2, createActionJson("test-match", "P2", "END_TURN"));

            // Then: State should be unchanged
            GameState stateAfter = match.getState();
            assertSame(stateBefore, stateAfter, "State reference should be unchanged");
            assertEquals(currentPlayerBefore, stateAfter.getCurrentPlayer().getValue());
        }
    }

    // ========================================================================
    // ERR-DISC: Disconnection Handling
    // ========================================================================

    @Nested
    @DisplayName("ERR-DISC: Disconnection Handling")
    class DisconnectionTests {

        @Test
        @DisplayName("ERR-DISC1: Player disconnect notifies opponent")
        void playerDisconnectNotifiesOpponent() {
            // Given: Both players joined
            setupBothPlayersJoined("test-match");
            connP2.clearMessages();

            // When: P1 disconnects
            handler.onClose(connP1);

            // Then: P2 receives notification
            List<String> disconnects = connP2.getMessagesOfType("player_disconnected");
            assertEquals(1, disconnects.size());
            assertEquals("P1", getPayload(disconnects.get(0)).get("playerId"));
        }

        @Test
        @DisplayName("ERR-DISC2: Disconnect removes player from match connections")
        void disconnectRemovesFromConnections() {
            // Given: Both players joined
            setupBothPlayersJoined("test-match");
            Match match = matchService.findMatch("test-match");

            assertEquals(2, match.getConnections().size(), "Should have 2 connections before disconnect");

            // When: P1 disconnects
            handler.onClose(connP1);

            // Then: Only P2 remains
            assertEquals(1, match.getConnections().size(), "Should have 1 connection after disconnect");
            assertTrue(match.getConnections().containsKey(ClientSlot.P2));
            assertFalse(match.getConnections().containsKey(ClientSlot.P1));
        }

        @Test
        @DisplayName("ERR-DISC3: Both players disconnect leaves match in registry")
        void bothDisconnectLeavesMatchInRegistry() {
            // Given: Both players joined
            setupBothPlayersJoined("test-match");
            Match match = matchService.findMatch("test-match");

            // When: Both disconnect
            handler.onClose(connP1);
            handler.onClose(connP2);

            // Then: Match still exists (for potential reconnection or cleanup)
            Match stillExists = matchService.findMatch("test-match");
            assertNotNull(stillExists, "Match should still exist after disconnects");
            assertEquals(0, stillExists.getConnections().size());
        }

        @Test
        @DisplayName("ERR-DISC4: New player can join after disconnect")
        void newPlayerCanJoinAfterDisconnect() {
            // Given: P1 joined and disconnected
            handler.onOpen(connP1);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");
            handler.onClose(connP1);

            // When: New player joins
            FakeClientConnection newPlayer = new FakeClientConnection("new-player");
            handler.onOpen(newPlayer);
            handler.onMessage(newPlayer, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-match\"}}");

            // Then: Should successfully join
            assertEquals("match_joined", getType(newPlayer.getLastMessage()));
        }
    }

    // ========================================================================
    // ERR-TIMER: Timer Expiration (with mock timers)
    // ========================================================================

    @Nested
    @DisplayName("ERR-TIMER: Timer Expiration")
    class TimerExpirationTests {

        @Test
        @DisplayName("ERR-TIMER1: Action timeout callback is set up correctly")
        void actionTimeoutCallbackSetUp() {
            // Given: Mock clock for testing
            AtomicLong mockTime = new AtomicLong(1000000L);
            TimerService timerService = new TimerService(mockTime::get);

            MatchService timedService = new MatchService(registry, new RuleEngine(),
                new GameStateSerializer(), timerService);

            // Create match with hero
            List<Unit> units = Arrays.asList(
                new Unit("p1_hero", new PlayerId("P1"), 5, 3, 2, 1, new Position(2, 2), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 5, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0),
                new Unit("p2_hero", new PlayerId("P2"), 5, 3, 2, 1, new Position(2, 4), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 5, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0)
            );
            GameState state = new GameState(new Board(5, 5), units, new PlayerId("P1"), false, null);
            registry.createMatch("timer-test", state);

            // When: Start timer
            long startTime = timedService.startTurnTimer("timer-test");

            // Then: Timer started successfully
            assertTrue(startTime > 0, "Timer should start");
        }

        @Test
        @DisplayName("ERR-TIMER2: Timer service tracks timer state")
        void timerServiceTracksState() {
            // Given: Timer service
            AtomicLong mockTime = new AtomicLong(1000000L);
            TimerService timerService = new TimerService(mockTime::get);

            // When: Start action timer
            timerService.startActionTimer("match-1", new PlayerId("P1"), () -> {});

            // Then: Timer state is RUNNING
            assertEquals(com.tactics.server.timer.TimerState.RUNNING,
                timerService.getTimerState("match-1", com.tactics.server.timer.TimerType.ACTION));
        }

        @Test
        @DisplayName("ERR-TIMER3: Complete timer changes state")
        void completeTimerChangesState() {
            // Given: Running timer
            AtomicLong mockTime = new AtomicLong(1000000L);
            TimerService timerService = new TimerService(mockTime::get);
            timerService.startActionTimer("match-1", new PlayerId("P1"), () -> {});

            // When: Complete timer
            timerService.completeTimer("match-1", com.tactics.server.timer.TimerType.ACTION);

            // Then: Timer is no longer running
            assertNotEquals(com.tactics.server.timer.TimerState.RUNNING,
                timerService.getTimerState("match-1", com.tactics.server.timer.TimerType.ACTION));
        }
    }

    // ========================================================================
    // ERR-MSG: Malformed Message Handling
    // ========================================================================

    @Nested
    @DisplayName("ERR-MSG: Malformed Message Handling")
    class MalformedMessageTests {

        @Test
        @DisplayName("ERR-MSG1: Invalid JSON keeps connection open")
        void invalidJsonKeepsConnectionOpen() {
            // Given: Connected player
            handler.onOpen(connP1);
            connP1.clearMessages();

            // When: Send invalid JSON
            handler.onMessage(connP1, "this is not json");

            // Then: Receives error but can continue
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));

            // And can still send valid message
            connP1.clearMessages();
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test\"}}");
            assertEquals("match_joined", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("ERR-MSG2: Unknown message type returns descriptive error")
        void unknownTypeReturnsDescriptiveError() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "{\"type\":\"invalid_type\",\"payload\":{}}");

            String error = connP1.sentMessages.get(0);
            assertEquals("validation_error", getType(error));
            String msg = getPayload(error).get("message").toString();
            assertTrue(msg.contains("Unknown") || msg.contains("invalid_type"),
                "Error should mention unknown type: " + msg);
        }

        @Test
        @DisplayName("ERR-MSG3: Missing action type returns descriptive error")
        void missingActionTypeReturnsDescriptiveError() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // Action without type field
            handler.onMessage(connP1, "{\"type\":\"action\",\"payload\":{\"matchId\":\"test-match\",\"playerId\":\"P1\",\"action\":{}}}");

            String error = connP1.sentMessages.get(0);
            assertEquals("validation_error", getType(error));
            String msg = getPayload(error).get("message").toString().toLowerCase();
            assertTrue(msg.contains("type") || msg.contains("action"),
                "Error should mention missing type: " + msg);
        }

        @Test
        @DisplayName("ERR-MSG4: Null payload handled gracefully")
        void nullPayloadHandled() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "{\"type\":\"join_match\"}");

            // Should receive error, not crash
            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("ERR-MSG5: Action with invalid coordinates returns error")
        void invalidCoordinatesReturnsError() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // Move to invalid position (off board)
            handler.onMessage(connP1, createMoveJson("test-match", "P1", -1, 10));

            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }
    }

    // ========================================================================
    // ERR-EDGE: Edge Cases
    // ========================================================================

    @Nested
    @DisplayName("ERR-EDGE: Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("ERR-EDGE1: Rapid successive messages handled correctly")
        void rapidSuccessiveMessagesHandled() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // Send multiple END_TURN rapidly
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));

            // First should succeed, others should fail (wrong turn)
            List<String> updates = connP1.getMessagesOfType("state_update");
            List<String> errors = connP1.getMessagesOfType("validation_error");

            assertEquals(1, updates.size(), "Only first action should succeed");
            assertEquals(2, errors.size(), "Subsequent actions should fail");
        }

        @Test
        @DisplayName("ERR-EDGE2: Empty matchId rejected")
        void emptyMatchIdRejected() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"\"}}");

            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("ERR-EDGE3: Whitespace-only matchId rejected")
        void whitespaceMatchIdRejected() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"   \"}}");

            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("ERR-EDGE4: Action for wrong match rejected")
        void actionForWrongMatchRejected() {
            // Given: P1 joined match-1
            handler.onOpen(connP1);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"match-1\"}}");
            connP1.clearMessages();

            // When: Try to act on non-existent match
            handler.onMessage(connP1, createActionJson("match-2", "P1", "END_TURN"));

            // Then: Should fail
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("ERR-EDGE5: Very long matchId accepted")
        void veryLongMatchIdAccepted() {
            handler.onOpen(connP1);

            String longId = "a".repeat(200);
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"" + longId + "\"}}");

            assertEquals("match_joined", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("ERR-EDGE6: Special characters in matchId handled")
        void specialCharsInMatchIdHandled() {
            handler.onOpen(connP1);

            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"test-123_abc\"}}");

            assertEquals("match_joined", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("ERR-EDGE7: Concurrent joins to same match handled")
        void concurrentJoinsHandled() {
            // Given: Three players try to join
            FakeClientConnection p1 = new FakeClientConnection("p1");
            FakeClientConnection p2 = new FakeClientConnection("p2");
            FakeClientConnection p3 = new FakeClientConnection("p3");

            handler.onOpen(p1);
            handler.onOpen(p2);
            handler.onOpen(p3);

            // When: All try to join
            handler.onMessage(p1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"concurrent-test\"}}");
            handler.onMessage(p2, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"concurrent-test\"}}");
            handler.onMessage(p3, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"concurrent-test\"}}");

            // Then: Two succeed, one fails
            int joined = 0;
            int rejected = 0;
            for (FakeClientConnection conn : Arrays.asList(p1, p2, p3)) {
                String response = conn.sentMessages.get(0);
                if ("match_joined".equals(getType(response))) {
                    joined++;
                } else if ("validation_error".equals(getType(response))) {
                    rejected++;
                }
            }

            assertEquals(2, joined, "Two players should join");
            assertEquals(1, rejected, "One player should be rejected");
        }
    }

    // ========================================================================
    // ERR-CONSIST: State Consistency Tests
    // ========================================================================

    @Nested
    @DisplayName("ERR-CONSIST: State Consistency")
    class StateConsistencyTests {

        @Test
        @DisplayName("ERR-CONSIST1: Error response doesn't modify game state")
        void errorDoesntModifyState() {
            setupBothPlayersJoined("test-match");
            Match match = matchService.findMatch("test-match");
            GameState originalState = match.getState();

            // Try multiple invalid actions
            handler.onMessage(connP2, createActionJson("test-match", "P2", "END_TURN")); // wrong turn
            handler.onMessage(connP1, createMoveJson("test-match", "P1", 10, 10)); // off board
            handler.onMessage(connP1, createAttackJson("test-match", "P1", 4, 4, "nonexistent")); // no target

            // State should be unchanged
            assertSame(originalState, match.getState());
        }

        @Test
        @DisplayName("ERR-CONSIST2: Multiple errors don't corrupt state")
        void multipleErrorsDontCorruptState() {
            setupBothPlayersJoined("test-match");
            Match match = matchService.findMatch("test-match");

            // Spam invalid messages
            for (int i = 0; i < 10; i++) {
                handler.onMessage(connP2, createActionJson("test-match", "P2", "END_TURN"));
                handler.onMessage(connP1, "{\"type\":\"invalid\",\"payload\":{}}");
            }

            // State should still be valid
            GameState state = match.getState();
            assertNotNull(state);
            assertNotNull(state.getBoard());
            assertNotNull(state.getUnits());
            assertFalse(state.isGameOver());
        }

        @Test
        @DisplayName("ERR-CONSIST3: Valid action succeeds after errors")
        void validActionSucceedsAfterErrors() {
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // Send invalid actions first
            handler.onMessage(connP2, createActionJson("test-match", "P2", "END_TURN"));
            handler.onMessage(connP1, createMoveJson("test-match", "P1", 10, 10));

            connP1.clearMessages();

            // Then valid action
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));

            // Should succeed
            List<String> updates = connP1.getMessagesOfType("state_update");
            assertEquals(1, updates.size(), "Valid action should succeed");
        }

        @Test
        @DisplayName("ERR-CONSIST4: Disconnect doesn't corrupt match state")
        void disconnectDoesntCorruptState() {
            setupBothPlayersJoined("test-match");
            Match match = matchService.findMatch("test-match");
            GameState stateBefore = match.getState();

            // Disconnect
            handler.onClose(connP1);

            // State should be unchanged
            assertSame(stateBefore, match.getState());
            assertNotNull(match.getState().getBoard());
            assertNotNull(match.getState().getUnits());
        }
    }
}
