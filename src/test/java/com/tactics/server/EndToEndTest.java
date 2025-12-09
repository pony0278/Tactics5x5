package com.tactics.server;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.*;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;
import com.tactics.server.core.*;
import com.tactics.server.dto.IncomingMessage;
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
 * E2E-Series: End-to-End Integration Tests
 *
 * Tests complete game flow from connection to game over:
 * - E2E-CONN: Connection and join flow
 * - E2E-ACT: Action flow with state updates
 * - E2E-TURN: Turn switching and unit actions
 * - E2E-GAME: Complete game scenarios
 * - E2E-ERR: Error handling throughout the flow
 */
@DisplayName("E2E-Series: End-to-End Tests")
public class EndToEndTest {

    private MatchService matchService;
    private ConnectionRegistry connectionRegistry;
    private MatchWebSocketHandler handler;
    private FakeClientConnection connP1;
    private FakeClientConnection connP2;

    // ========================================================================
    // Test Doubles
    // ========================================================================

    /**
     * FakeClientConnection - captures all outgoing messages.
     */
    static class FakeClientConnection implements ClientConnection {
        private final String id;
        private String matchId;
        private String playerId;
        public final List<String> sentMessages = new ArrayList<>();

        FakeClientConnection(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getMatchId() {
            return matchId;
        }

        @Override
        public void setMatchId(String matchId) {
            this.matchId = matchId;
        }

        @Override
        public String getPlayerId() {
            return playerId;
        }

        @Override
        public void setPlayerId(String playerId) {
            this.playerId = playerId;
        }

        @Override
        public void sendMessage(String message) {
            sentMessages.add(message);
        }

        public void clearMessages() {
            sentMessages.clear();
        }

        public String getLastMessage() {
            return sentMessages.isEmpty() ? null : sentMessages.get(sentMessages.size() - 1);
        }

        public List<String> getMessagesOfType(String type) {
            List<String> result = new ArrayList<>();
            for (String msg : sentMessages) {
                if (getType(msg).equals(type)) {
                    result.add(msg);
                }
            }
            return result;
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    @BeforeEach
    void setUp() {
        MatchRegistry registry = new MatchRegistry(new HashMap<>());
        RuleEngine ruleEngine = new RuleEngine();
        GameStateSerializer serializer = new GameStateSerializer();
        matchService = new MatchService(registry, ruleEngine, serializer);
        connectionRegistry = new ConnectionRegistry(new HashMap<>());
        handler = new MatchWebSocketHandler(matchService, connectionRegistry);
        handler.setUseTimers(false); // Disable timers for E2E tests

        connP1 = new FakeClientConnection("conn-p1");
        connP2 = new FakeClientConnection("conn-p2");
    }

    private static String getType(String json) {
        IncomingMessage msg = JsonHelper.parseIncomingMessage(json);
        return msg != null ? msg.getType() : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getPayload(String json) {
        IncomingMessage msg = JsonHelper.parseIncomingMessage(json);
        return msg != null ? msg.getPayload() : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getStateFromPayload(String json) {
        Map<String, Object> payload = getPayload(json);
        return payload != null ? (Map<String, Object>) payload.get("state") : null;
    }

    private String createJoinMatchJson(String matchId) {
        return "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"" + matchId + "\"}}";
    }

    private String createActionJson(String matchId, String playerId, String actionType) {
        return "{\"type\":\"action\",\"payload\":{\"matchId\":\"" + matchId +
               "\",\"playerId\":\"" + playerId + "\",\"action\":{\"type\":\"" + actionType + "\"}}}";
    }

    private String createMoveActionJson(String matchId, String playerId, String unitId, int x, int y) {
        return "{\"type\":\"action\",\"payload\":{\"matchId\":\"" + matchId +
               "\",\"playerId\":\"" + playerId + "\",\"action\":{\"type\":\"MOVE\"," +
               "\"unitId\":\"" + unitId + "\",\"targetX\":" + x + ",\"targetY\":" + y + "}}}";
    }

    private String createAttackActionJson(String matchId, String playerId, String unitId,
                                          int targetX, int targetY, String targetUnitId) {
        return "{\"type\":\"action\",\"payload\":{\"matchId\":\"" + matchId +
               "\",\"playerId\":\"" + playerId + "\",\"action\":{\"type\":\"ATTACK\"," +
               "\"unitId\":\"" + unitId + "\",\"targetX\":" + targetX + ",\"targetY\":" + targetY +
               ",\"targetUnitId\":\"" + targetUnitId + "\"}}}";
    }

    private String createEndTurnJson(String matchId, String playerId, String unitId) {
        return "{\"type\":\"action\",\"payload\":{\"matchId\":\"" + matchId +
               "\",\"playerId\":\"" + playerId + "\",\"action\":{\"type\":\"END_TURN\"," +
               "\"unitId\":\"" + unitId + "\"}}}";
    }

    /**
     * Setup both players to join a match and get to game_ready state.
     */
    private void setupBothPlayersJoined(String matchId) {
        handler.onOpen(connP1);
        handler.onOpen(connP2);

        handler.onMessage(connP1, createJoinMatchJson(matchId));
        handler.onMessage(connP2, createJoinMatchJson(matchId));
    }

    // ========================================================================
    // E2E-CONN-Series: Connection and Join Flow
    // ========================================================================

    @Nested
    @DisplayName("E2E-CONN: Connection and Join Flow")
    class ConnectionAndJoinTests {

        @Test
        @DisplayName("E2E-CONN1: First player joins and receives match_joined with P1")
        void firstPlayerJoinsAsP1() {
            // Given
            handler.onOpen(connP1);

            // When
            handler.onMessage(connP1, createJoinMatchJson("test-match"));

            // Then
            assertEquals(1, connP1.sentMessages.size());
            String response = connP1.sentMessages.get(0);
            assertEquals("match_joined", getType(response));

            Map<String, Object> payload = getPayload(response);
            assertEquals("test-match", payload.get("matchId"));
            assertEquals("P1", payload.get("playerId"));
            assertNotNull(payload.get("state"));
        }

        @Test
        @DisplayName("E2E-CONN2: Second player joins and receives match_joined with P2")
        void secondPlayerJoinsAsP2() {
            // Given: P1 already joined
            handler.onOpen(connP1);
            handler.onMessage(connP1, createJoinMatchJson("test-match"));
            connP1.clearMessages();

            // When: P2 joins
            handler.onOpen(connP2);
            handler.onMessage(connP2, createJoinMatchJson("test-match"));

            // Then: P2 receives match_joined with P2
            assertTrue(connP2.sentMessages.size() >= 1);
            String joinResponse = connP2.sentMessages.get(0);
            assertEquals("match_joined", getType(joinResponse));

            Map<String, Object> payload = getPayload(joinResponse);
            assertEquals("test-match", payload.get("matchId"));
            assertEquals("P2", payload.get("playerId"));
        }

        @Test
        @DisplayName("E2E-CONN3: Both players receive game_ready when second player joins")
        void bothPlayersReceiveGameReady() {
            // Given: P1 joined
            handler.onOpen(connP1);
            handler.onMessage(connP1, createJoinMatchJson("test-match"));
            connP1.clearMessages();

            // When: P2 joins
            handler.onOpen(connP2);
            handler.onMessage(connP2, createJoinMatchJson("test-match"));

            // Then: Both should receive game_ready
            List<String> p1GameReady = connP1.getMessagesOfType("game_ready");
            List<String> p2GameReady = connP2.getMessagesOfType("game_ready");

            assertEquals(1, p1GameReady.size(), "P1 should receive game_ready");
            assertEquals(1, p2GameReady.size(), "P2 should receive game_ready");
        }

        @Test
        @DisplayName("E2E-CONN4: Third player cannot join full match")
        void thirdPlayerCannotJoinFullMatch() {
            // Given: Both players joined
            setupBothPlayersJoined("test-match");

            // When: Third player tries to join
            FakeClientConnection connP3 = new FakeClientConnection("conn-p3");
            handler.onOpen(connP3);
            handler.onMessage(connP3, createJoinMatchJson("test-match"));

            // Then: Should receive validation_error
            assertEquals(1, connP3.sentMessages.size());
            assertEquals("validation_error", getType(connP3.sentMessages.get(0)));

            Map<String, Object> payload = getPayload(connP3.sentMessages.get(0));
            assertTrue(((String) payload.get("message")).contains("full"));
        }

        @Test
        @DisplayName("E2E-CONN5: Player disconnection notifies remaining player")
        void playerDisconnectionNotifiesRemaining() {
            // Given: Both players joined
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();
            connP2.clearMessages();

            // When: P1 disconnects
            handler.onClose(connP1);

            // Then: P2 should receive player_disconnected
            List<String> disconnectMsgs = connP2.getMessagesOfType("player_disconnected");
            assertEquals(1, disconnectMsgs.size());

            Map<String, Object> payload = getPayload(disconnectMsgs.get(0));
            assertEquals("P1", payload.get("playerId"));
        }
    }

    // ========================================================================
    // E2E-ACT-Series: Action Flow with State Updates
    // ========================================================================

    @Nested
    @DisplayName("E2E-ACT: Action Flow")
    class ActionFlowTests {

        @Test
        @DisplayName("E2E-ACT1: Valid action broadcasts state_update to both players")
        void validActionBroadcastsStateUpdate() {
            // Given: Both players joined
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();
            connP2.clearMessages();

            // When: P1 ends turn (simplest valid action)
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));

            // Then: Both receive state_update
            List<String> p1Updates = connP1.getMessagesOfType("state_update");
            List<String> p2Updates = connP2.getMessagesOfType("state_update");

            assertEquals(1, p1Updates.size(), "P1 should receive state_update");
            assertEquals(1, p2Updates.size(), "P2 should receive state_update");

            // Verify state is included
            Map<String, Object> state1 = getStateFromPayload(p1Updates.get(0));
            Map<String, Object> state2 = getStateFromPayload(p2Updates.get(0));
            assertNotNull(state1);
            assertNotNull(state2);
        }

        @Test
        @DisplayName("E2E-ACT2: Invalid action returns validation_error to sender only")
        void invalidActionReturnsErrorToSenderOnly() {
            // Given: Both players joined, P1's turn
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();
            connP2.clearMessages();

            // When: P2 tries to act (not their turn)
            handler.onMessage(connP2, createActionJson("test-match", "P2", "END_TURN"));

            // Then: Only P2 receives validation_error
            assertEquals(1, connP2.sentMessages.size());
            assertEquals("validation_error", getType(connP2.sentMessages.get(0)));
            assertEquals(0, connP1.sentMessages.size());
        }

        @Test
        @DisplayName("E2E-ACT3: Move action updates unit position in state")
        void moveActionUpdatesUnitPosition() {
            // Given: Both players joined
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();
            connP2.clearMessages();

            // Get initial state to find a unit
            Match match = matchService.findMatch("test-match");
            GameState initialState = match.getState();
            Unit p1Unit = initialState.getUnits().stream()
                .filter(u -> u.getOwner().getValue().equals("P1") && u.isAlive())
                .findFirst().orElse(null);
            assertNotNull(p1Unit, "P1 should have a unit");

            // Calculate valid move position (adjacent to current)
            int newX = p1Unit.getPosition().getX();
            int newY = p1Unit.getPosition().getY() + 1;

            // When: P1 moves their unit
            String moveJson = createMoveActionJson("test-match", "P1", p1Unit.getId(), newX, newY);
            handler.onMessage(connP1, moveJson);

            // Then: state_update shows unit moved
            List<String> updates = connP1.getMessagesOfType("state_update");
            assertEquals(1, updates.size(), "Should receive state_update");

            Map<String, Object> state = getStateFromPayload(updates.get(0));
            assertNotNull(state);

            // Verify unit position updated
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> units = (List<Map<String, Object>>) state.get("units");
            Map<String, Object> movedUnit = units.stream()
                .filter(u -> p1Unit.getId().equals(u.get("id")))
                .findFirst().orElse(null);
            assertNotNull(movedUnit);

            @SuppressWarnings("unchecked")
            Map<String, Object> position = (Map<String, Object>) movedUnit.get("position");
            assertEquals(newX, ((Number) position.get("x")).intValue());
            assertEquals(newY, ((Number) position.get("y")).intValue());
        }

        @Test
        @DisplayName("E2E-ACT4: Attack action reduces target HP")
        void attackActionReducesTargetHp() {
            // Given: Create a custom game with adjacent units for attack
            MatchRegistry registry = new MatchRegistry(new HashMap<>());
            List<Unit> units = Arrays.asList(
                new Unit("p1_unit", new PlayerId("P1"), 10, 3, 2, 1, new Position(2, 2), true),
                new Unit("p2_unit", new PlayerId("P2"), 10, 3, 2, 1, new Position(2, 3), true)
            );
            GameState customState = new GameState(new Board(5, 5), units, new PlayerId("P1"), false, null);
            registry.createMatch("attack-test", customState);

            MatchService customService = new MatchService(registry, new RuleEngine(), new GameStateSerializer());
            MatchWebSocketHandler customHandler = new MatchWebSocketHandler(customService, new ConnectionRegistry(new HashMap<>()));
            customHandler.setUseTimers(false);

            FakeClientConnection p1 = new FakeClientConnection("p1");
            FakeClientConnection p2 = new FakeClientConnection("p2");
            customHandler.onOpen(p1);
            customHandler.onOpen(p2);
            customHandler.onMessage(p1, createJoinMatchJson("attack-test"));
            customHandler.onMessage(p2, createJoinMatchJson("attack-test"));
            p1.clearMessages();
            p2.clearMessages();

            // When: P1 attacks P2's unit
            String attackJson = createAttackActionJson("attack-test", "P1", "p1_unit", 2, 3, "p2_unit");
            customHandler.onMessage(p1, attackJson);

            // Then: State shows reduced HP
            List<String> updates = p1.getMessagesOfType("state_update");
            assertEquals(1, updates.size());

            Map<String, Object> state = getStateFromPayload(updates.get(0));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stateUnits = (List<Map<String, Object>>) state.get("units");

            Map<String, Object> targetUnit = stateUnits.stream()
                .filter(u -> "p2_unit".equals(u.get("id")))
                .findFirst().orElse(null);
            assertNotNull(targetUnit);

            int newHp = ((Number) targetUnit.get("hp")).intValue();
            assertEquals(7, newHp, "Target HP should be 10 - 3 = 7");
        }
    }

    // ========================================================================
    // E2E-TURN-Series: Turn Switching
    // ========================================================================

    @Nested
    @DisplayName("E2E-TURN: Turn Switching")
    class TurnSwitchingTests {

        @Test
        @DisplayName("E2E-TURN1: END_TURN switches current player")
        void endTurnSwitchesPlayer() {
            // Given: Both players joined
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // When: P1 ends turn
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));

            // Then: Current player should be P2
            List<String> updates = connP1.getMessagesOfType("state_update");
            Map<String, Object> state = getStateFromPayload(updates.get(0));
            assertEquals("P2", state.get("currentPlayer"));
        }

        @Test
        @DisplayName("E2E-TURN2: After P1 turn, P2 can act")
        void afterP1TurnP2CanAct() {
            // Given: P1 ended turn
            setupBothPlayersJoined("test-match");
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));
            connP1.clearMessages();
            connP2.clearMessages();

            // When: P2 ends turn
            handler.onMessage(connP2, createActionJson("test-match", "P2", "END_TURN"));

            // Then: Should succeed (not validation_error)
            List<String> p2Updates = connP2.getMessagesOfType("state_update");
            assertEquals(1, p2Updates.size(), "P2 should be able to act after P1 ends turn");
        }

        @Test
        @DisplayName("E2E-TURN3: P1 cannot act during P2's turn")
        void p1CannotActDuringP2Turn() {
            // Given: P1 ended turn, now P2's turn
            setupBothPlayersJoined("test-match");
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));
            connP1.clearMessages();

            // When: P1 tries to act
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));

            // Then: Should receive validation_error
            List<String> errors = connP1.getMessagesOfType("validation_error");
            assertEquals(1, errors.size());
            assertTrue(getPayload(errors.get(0)).get("message").toString().contains("turn"));
        }

        @Test
        @DisplayName("E2E-TURN4: Full round returns to P1")
        void fullRoundReturnsToP1() {
            // Given: Both joined
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();
            connP2.clearMessages();

            // When: Both players end turn
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));
            handler.onMessage(connP2, createActionJson("test-match", "P2", "END_TURN"));

            // Then: Current player should be P1 again
            String lastUpdate = connP1.getLastMessage();
            Map<String, Object> state = getStateFromPayload(lastUpdate);
            assertEquals("P1", state.get("currentPlayer"));
        }
    }

    // ========================================================================
    // E2E-GAME-Series: Complete Game Scenarios
    // ========================================================================

    @Nested
    @DisplayName("E2E-GAME: Complete Game Scenarios")
    class CompleteGameTests {

        @Test
        @DisplayName("E2E-GAME1: Game ends when hero dies - game_over broadcast")
        void gameEndsWhenHeroDies() {
            // Given: Custom game with low HP hero that can be killed
            MatchRegistry registry = new MatchRegistry(new HashMap<>());
            List<Unit> units = Arrays.asList(
                new Unit("p1_hero", new PlayerId("P1"), 10, 5, 2, 1, new Position(2, 2), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 10, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0),
                new Unit("p2_hero", new PlayerId("P2"), 3, 5, 2, 1, new Position(2, 3), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 3, null, 0,
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
            customHandler.onMessage(p1, createJoinMatchJson("game-over-test"));
            customHandler.onMessage(p2, createJoinMatchJson("game-over-test"));
            p1.clearMessages();
            p2.clearMessages();

            // When: P1 attacks P2's hero (3 HP - 5 ATK = dead)
            String attackJson = createAttackActionJson("game-over-test", "P1", "p1_hero", 2, 3, "p2_hero");
            customHandler.onMessage(p1, attackJson);

            // Then: Both should receive game_over
            List<String> p1GameOver = p1.getMessagesOfType("game_over");
            List<String> p2GameOver = p2.getMessagesOfType("game_over");

            assertEquals(1, p1GameOver.size(), "P1 should receive game_over");
            assertEquals(1, p2GameOver.size(), "P2 should receive game_over");

            // Verify winner is P1
            Map<String, Object> payload = getPayload(p1GameOver.get(0));
            assertEquals("P1", payload.get("winner"));

            // Verify state shows game over
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) payload.get("state");
            assertTrue((Boolean) state.get("gameOver"));
        }

        @Test
        @DisplayName("E2E-GAME2: Multiple attacks until game over")
        void multipleAttacksUntilGameOver() {
            // Given: Custom game requiring multiple attacks
            MatchRegistry registry = new MatchRegistry(new HashMap<>());
            List<Unit> units = Arrays.asList(
                new Unit("p1_hero", new PlayerId("P1"), 10, 3, 2, 1, new Position(2, 2), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 10, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0),
                new Unit("p2_hero", new PlayerId("P2"), 7, 3, 2, 1, new Position(2, 3), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 7, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0)
            );
            GameState customState = new GameState(new Board(5, 5), units, new PlayerId("P1"), false, null);
            registry.createMatch("multi-attack", customState);

            MatchService customService = new MatchService(registry, new RuleEngine(), new GameStateSerializer());
            MatchWebSocketHandler customHandler = new MatchWebSocketHandler(customService, new ConnectionRegistry(new HashMap<>()));
            customHandler.setUseTimers(false);

            FakeClientConnection p1 = new FakeClientConnection("p1");
            FakeClientConnection p2 = new FakeClientConnection("p2");
            customHandler.onOpen(p1);
            customHandler.onOpen(p2);
            customHandler.onMessage(p1, createJoinMatchJson("multi-attack"));
            customHandler.onMessage(p2, createJoinMatchJson("multi-attack"));

            // When: Attack sequence (P1 attacks, P2 passes, P1 attacks, P2 passes, P1 kills)
            // Attack 1: 7 - 3 = 4 HP
            p1.clearMessages();
            customHandler.onMessage(p1, createAttackActionJson("multi-attack", "P1", "p1_hero", 2, 3, "p2_hero"));
            List<String> updates1 = p1.getMessagesOfType("state_update");
            assertEquals(1, updates1.size(), "First attack should produce state_update");

            // P1 ends turn
            customHandler.onMessage(p1, createActionJson("multi-attack", "P1", "END_TURN"));
            // P2 ends turn
            customHandler.onMessage(p2, createActionJson("multi-attack", "P2", "END_TURN"));

            // Attack 2: 4 - 3 = 1 HP
            p1.clearMessages();
            customHandler.onMessage(p1, createAttackActionJson("multi-attack", "P1", "p1_hero", 2, 3, "p2_hero"));
            updates1 = p1.getMessagesOfType("state_update");
            assertEquals(1, updates1.size(), "Second attack should produce state_update");

            // P1 ends turn, P2 ends turn
            customHandler.onMessage(p1, createActionJson("multi-attack", "P1", "END_TURN"));
            customHandler.onMessage(p2, createActionJson("multi-attack", "P2", "END_TURN"));

            // Attack 3: 1 - 3 = dead
            p1.clearMessages();
            p2.clearMessages();
            customHandler.onMessage(p1, createAttackActionJson("multi-attack", "P1", "p1_hero", 2, 3, "p2_hero"));

            // Then: Game should be over
            List<String> gameOver = p1.getMessagesOfType("game_over");
            assertEquals(1, gameOver.size(), "Should receive game_over after killing hero");

            Map<String, Object> payload = getPayload(gameOver.get(0));
            assertEquals("P1", payload.get("winner"));
        }

        @Test
        @DisplayName("E2E-GAME3: Minion death does not end game")
        void minionDeathDoesNotEndGame() {
            // Given: Game with heroes and a minion
            MatchRegistry registry = new MatchRegistry(new HashMap<>());
            List<Unit> units = Arrays.asList(
                new Unit("p1_hero", new PlayerId("P1"), 10, 5, 2, 1, new Position(2, 2), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 10, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0),
                new Unit("p2_hero", new PlayerId("P2"), 10, 5, 2, 1, new Position(4, 4), true,
                    UnitCategory.HERO, null, HeroClass.WARRIOR, 10, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0),
                new Unit("p2_minion", new PlayerId("P2"), 3, 2, 2, 1, new Position(2, 3), true,
                    UnitCategory.MINION, MinionType.TANK, null, 3, null, 0,
                    0, false, false, false, 0, null, 0, false, null, 0, 0)
            );
            GameState customState = new GameState(new Board(5, 5), units, new PlayerId("P1"), false, null);
            registry.createMatch("minion-death", customState);

            MatchService customService = new MatchService(registry, new RuleEngine(), new GameStateSerializer());
            MatchWebSocketHandler customHandler = new MatchWebSocketHandler(customService, new ConnectionRegistry(new HashMap<>()));
            customHandler.setUseTimers(false);

            FakeClientConnection p1 = new FakeClientConnection("p1");
            FakeClientConnection p2 = new FakeClientConnection("p2");
            customHandler.onOpen(p1);
            customHandler.onOpen(p2);
            customHandler.onMessage(p1, createJoinMatchJson("minion-death"));
            customHandler.onMessage(p2, createJoinMatchJson("minion-death"));
            p1.clearMessages();
            p2.clearMessages();

            // When: P1 kills P2's minion
            String attackJson = createAttackActionJson("minion-death", "P1", "p1_hero", 2, 3, "p2_minion");
            customHandler.onMessage(p1, attackJson);

            // Then: Should NOT be game_over (only state_update)
            List<String> gameOver = p1.getMessagesOfType("game_over");
            assertEquals(0, gameOver.size(), "Minion death should not end game");

            // State update should show minion dead but game not over
            List<String> updates = p1.getMessagesOfType("state_update");
            assertEquals(1, updates.size());

            Map<String, Object> state = getStateFromPayload(updates.get(0));
            assertFalse((Boolean) state.get("gameOver"));
        }
    }

    // ========================================================================
    // E2E-ERR-Series: Error Handling
    // ========================================================================

    @Nested
    @DisplayName("E2E-ERR: Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("E2E-ERR1: Malformed JSON returns validation_error")
        void malformedJsonReturnsError() {
            // Given
            handler.onOpen(connP1);

            // When
            handler.onMessage(connP1, "not valid json");

            // Then
            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("E2E-ERR2: Unknown message type returns validation_error")
        void unknownMessageTypeReturnsError() {
            // Given
            handler.onOpen(connP1);

            // When
            handler.onMessage(connP1, "{\"type\":\"unknown_type\",\"payload\":{}}");

            // Then
            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("E2E-ERR3: Action for non-existent match returns validation_error")
        void actionForNonExistentMatchReturnsError() {
            // Given
            handler.onOpen(connP1);

            // When
            handler.onMessage(connP1, createActionJson("non-existent", "P1", "END_TURN"));

            // Then
            assertEquals(1, connP1.sentMessages.size());
            assertEquals("validation_error", getType(connP1.sentMessages.get(0)));
        }

        @Test
        @DisplayName("E2E-ERR4: Invalid move (out of range) returns validation_error")
        void invalidMoveReturnsError() {
            // Given: Both players joined
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // Get a unit and try invalid move (too far)
            Match match = matchService.findMatch("test-match");
            Unit p1Unit = match.getState().getUnits().stream()
                .filter(u -> u.getOwner().getValue().equals("P1"))
                .findFirst().orElse(null);
            assertNotNull(p1Unit);

            // When: Try to move 4 tiles away (invalid)
            String invalidMove = createMoveActionJson("test-match", "P1", p1Unit.getId(), 4, 4);
            handler.onMessage(connP1, invalidMove);

            // Then: Should receive validation_error
            List<String> errors = connP1.getMessagesOfType("validation_error");
            assertEquals(1, errors.size());
        }

        @Test
        @DisplayName("E2E-ERR5: Attack on empty tile returns validation_error")
        void attackOnEmptyTileReturnsError() {
            // Given: Both joined
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            Match match = matchService.findMatch("test-match");
            Unit p1Unit = match.getState().getUnits().stream()
                .filter(u -> u.getOwner().getValue().equals("P1"))
                .findFirst().orElse(null);

            // When: Attack empty tile with non-existent unit
            String invalidAttack = createAttackActionJson("test-match", "P1", p1Unit.getId(),
                                                          2, 2, "non-existent-unit");
            handler.onMessage(connP1, invalidAttack);

            // Then
            List<String> errors = connP1.getMessagesOfType("validation_error");
            assertEquals(1, errors.size());
        }
    }

    // ========================================================================
    // E2E-MSG-Series: Message Format Verification
    // ========================================================================

    @Nested
    @DisplayName("E2E-MSG: Message Format Verification")
    class MessageFormatTests {

        @Test
        @DisplayName("E2E-MSG1: match_joined contains required fields")
        void matchJoinedContainsRequiredFields() {
            // Given
            handler.onOpen(connP1);

            // When
            handler.onMessage(connP1, createJoinMatchJson("test-match"));

            // Then
            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            assertTrue(payload.containsKey("matchId"));
            assertTrue(payload.containsKey("playerId"));
            assertTrue(payload.containsKey("state"));
        }

        @Test
        @DisplayName("E2E-MSG2: state_update contains state object")
        void stateUpdateContainsState() {
            // Given
            setupBothPlayersJoined("test-match");
            connP1.clearMessages();

            // When
            handler.onMessage(connP1, createActionJson("test-match", "P1", "END_TURN"));

            // Then
            List<String> updates = connP1.getMessagesOfType("state_update");
            Map<String, Object> payload = getPayload(updates.get(0));
            assertTrue(payload.containsKey("state"));

            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) payload.get("state");
            assertTrue(state.containsKey("board"));
            assertTrue(state.containsKey("units"));
            assertTrue(state.containsKey("currentPlayer"));
            assertTrue(state.containsKey("gameOver"));
        }

        @Test
        @DisplayName("E2E-MSG3: game_over contains winner and state")
        void gameOverContainsWinnerAndState() {
            // Given: Setup for game over
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
            registry.createMatch("msg-test", customState);

            MatchService customService = new MatchService(registry, new RuleEngine(), new GameStateSerializer());
            MatchWebSocketHandler customHandler = new MatchWebSocketHandler(customService, new ConnectionRegistry(new HashMap<>()));
            customHandler.setUseTimers(false);

            FakeClientConnection p1 = new FakeClientConnection("p1");
            FakeClientConnection p2 = new FakeClientConnection("p2");
            customHandler.onOpen(p1);
            customHandler.onOpen(p2);
            customHandler.onMessage(p1, createJoinMatchJson("msg-test"));
            customHandler.onMessage(p2, createJoinMatchJson("msg-test"));
            p1.clearMessages();

            // When: Kill the hero
            customHandler.onMessage(p1, createAttackActionJson("msg-test", "P1", "p1_hero", 2, 3, "p2_hero"));

            // Then
            List<String> gameOver = p1.getMessagesOfType("game_over");
            assertEquals(1, gameOver.size());

            Map<String, Object> payload = getPayload(gameOver.get(0));
            assertTrue(payload.containsKey("winner"));
            assertTrue(payload.containsKey("state"));

            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) payload.get("state");
            assertTrue((Boolean) state.get("gameOver"));
        }

        @Test
        @DisplayName("E2E-MSG4: validation_error contains message")
        void validationErrorContainsMessage() {
            // Given
            handler.onOpen(connP1);

            // When
            handler.onMessage(connP1, "{\"type\":\"join_match\",\"payload\":{}}");

            // Then
            Map<String, Object> payload = getPayload(connP1.sentMessages.get(0));
            assertTrue(payload.containsKey("message"));
            assertNotNull(payload.get("message"));
        }
    }
}
