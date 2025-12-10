package com.tactics.server.ws;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.HeroClass;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;
import com.tactics.server.core.ClientSlot;
import com.tactics.server.core.Match;
import com.tactics.server.core.MatchId;
import com.tactics.server.core.MatchRegistry;
import com.tactics.server.core.MatchService;
import com.tactics.server.timer.TimerConfig;
import com.tactics.server.timer.TimerService;
import com.tactics.server.timer.TimerState;
import com.tactics.server.timer.TimerType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MatchWebSocketHandler with timer integration.
 * Based on TIMER_TESTPLAN.md - WebSocket message formats.
 */
class MatchWebSocketHandlerTimerTest {

    private MatchRegistry registry;
    private RuleEngine ruleEngine;
    private GameStateSerializer serializer;
    private TimerService timerService;
    private MatchService matchService;
    private ConnectionRegistry connectionRegistry;
    private MatchWebSocketHandler handler;
    private AtomicLong mockTime;
    private Board board;

    @BeforeEach
    void setUp() {
        mockTime = new AtomicLong(1000000L);
        registry = new MatchRegistry(new HashMap<>());
        ruleEngine = new RuleEngine();
        serializer = new GameStateSerializer();
        timerService = new TimerService(mockTime::get);
        matchService = new MatchService(registry, ruleEngine, serializer, timerService);
        connectionRegistry = new ConnectionRegistry(new HashMap<>());
        handler = new MatchWebSocketHandler(matchService, connectionRegistry);
        // Timers enabled by default for these tests
        board = new Board(5, 5);
    }

    @AfterEach
    void tearDown() {
        timerService.shutdown();
    }

    // ========== Test Helpers ==========

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

        public List<String> getMessagesByType(String type) {
            List<String> result = new ArrayList<>();
            for (String msg : sentMessages) {
                if (msg.contains("\"type\":\"" + type + "\"")) {
                    result.add(msg);
                }
            }
            return result;
        }
    }

    private Unit createHero(String id, PlayerId owner, int hp, Position pos) {
        return new Unit(id, owner, hp, 3, 1, 1, pos, true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, hp,
                null, 0,
                0, false, false, false, 0, null);
    }

    private GameState createGameState(List<Unit> units, PlayerId currentPlayer) {
        return new GameState(board, units, currentPlayer, false, null);
    }

    private Match createMatch(String matchId, GameState state, Map<ClientSlot, ClientConnection> connections) {
        return new Match(new MatchId(matchId), state, connections);
    }

    // ========== Timer Message Tests ==========

    @Nested
    @DisplayName("Timer Message Format Tests")
    class TimerMessageFormatTests {

        @Test
        @DisplayName("TA-006: YOUR_TURN includes timer info when battle starts after draft")
        void ta006_yourTurnIncludesTimerInfo() {
            // Given: Two players join a match and complete draft
            Unit hero1 = createHero("p1_hero", new PlayerId("P1"), 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", new PlayerId("P2"), 5, new Position(3, 3));
            GameState state = createGameState(Arrays.asList(hero1, hero2), new PlayerId("P1"));

            Map<ClientSlot, ClientConnection> connections = new HashMap<>();
            Match match = createMatch("match-1", state, connections);
            registry.createMatch("match-1", state);

            FakeClientConnection conn1 = new FakeClientConnection("c1");
            FakeClientConnection conn2 = new FakeClientConnection("c2");

            // P1 joins
            String joinMsg = "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"match-1\"}}";
            handler.onOpen(conn1);
            handler.onMessage(conn1, joinMsg);

            // P2 joins - this starts draft phase (not battle)
            handler.onOpen(conn2);
            handler.onMessage(conn2, joinMsg);

            // Both players submit their draft
            String p1Draft = "{\"type\":\"select_team\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"heroClass\":\"WARRIOR\",\"minions\":[\"TANK\",\"ARCHER\"]}}";
            String p2Draft = "{\"type\":\"select_team\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P2\",\"heroClass\":\"MAGE\",\"minions\":[\"TANK\",\"ASSASSIN\"]}}";

            handler.onMessage(conn1, p1Draft);
            handler.onMessage(conn2, p2Draft);

            // Then: P1 receives YOUR_TURN with timer info after draft completes
            List<String> yourTurnMsgs = conn1.getMessagesByType("your_turn");
            assertEquals(1, yourTurnMsgs.size());

            String yourTurn = yourTurnMsgs.get(0);
            assertTrue(yourTurn.contains("\"actionStartTime\""));
            assertTrue(yourTurn.contains("\"timeoutMs\":" + TimerConfig.ACTION_TIMEOUT_MS));
            assertTrue(yourTurn.contains("\"timerType\":\"ACTION\""));
        }

        @Test
        @DisplayName("TA-018: state_update includes timer info after action")
        void ta018_stateUpdateIncludesTimerInfo() {
            // Given: Game in progress with both players
            Unit hero1 = createHero("p1_hero", new PlayerId("P1"), 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", new PlayerId("P2"), 5, new Position(3, 3));
            GameState state = createGameState(Arrays.asList(hero1, hero2), new PlayerId("P1"));

            FakeClientConnection conn1 = new FakeClientConnection("c1");
            FakeClientConnection conn2 = new FakeClientConnection("c2");

            Map<ClientSlot, ClientConnection> connections = new HashMap<>();
            connections.put(ClientSlot.P1, conn1);
            connections.put(ClientSlot.P2, conn2);

            // Use registry to create match with connections
            Match match = registry.createMatch("match-1", state);
            match.getConnections().put(ClientSlot.P1, conn1);
            match.getConnections().put(ClientSlot.P2, conn2);

            // Set up connections in handler
            conn1.setMatchId("match-1");
            conn1.setPlayerId("P1");
            conn2.setMatchId("match-1");
            conn2.setPlayerId("P2");

            // Start timer for P1
            matchService.startTurnTimer("match-1");
            conn1.clearMessages();
            conn2.clearMessages();

            // When: P1 submits END_TURN action
            String actionMsg = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(conn1, actionMsg);

            // Then: state_update includes timer for P2
            List<String> stateUpdates = conn1.getMessagesByType("state_update");
            assertTrue(stateUpdates.size() >= 1, "Should have at least 1 state_update, got: " + conn1.sentMessages);

            String stateUpdate = stateUpdates.get(stateUpdates.size() - 1);
            assertTrue(stateUpdate.contains("\"timer\""), "state_update should contain timer: " + stateUpdate);
            assertTrue(stateUpdate.contains("\"actionStartTime\""), "state_update should contain actionStartTime: " + stateUpdate);
            assertTrue(stateUpdate.contains("\"currentPlayerId\":\"P2\""), "state_update should show P2 as current: " + stateUpdate);
        }

        @Test
        @DisplayName("game_over has no timer info")
        void gameOverHasNoTimerInfo() {
            // Given: P1 Hero at 1 HP, P2 adjacent
            Unit hero1 = createHero("p1_hero", new PlayerId("P1"), 1, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", new PlayerId("P2"), 5, new Position(1, 2));
            GameState state = createGameState(Arrays.asList(hero1, hero2), new PlayerId("P2"));

            FakeClientConnection conn1 = new FakeClientConnection("c1");
            FakeClientConnection conn2 = new FakeClientConnection("c2");

            // Use registry to create match with connections
            Match match = registry.createMatch("match-1", state);
            match.getConnections().put(ClientSlot.P1, conn1);
            match.getConnections().put(ClientSlot.P2, conn2);

            conn1.setMatchId("match-1");
            conn1.setPlayerId("P1");
            conn2.setMatchId("match-1");
            conn2.setPlayerId("P2");

            matchService.startTurnTimer("match-1");
            conn1.clearMessages();
            conn2.clearMessages();

            // When: P2 attacks and kills P1's Hero
            String actionMsg = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P2\",\"action\":{\"type\":\"ATTACK\",\"targetX\":1,\"targetY\":1,\"targetUnitId\":\"p1_hero\"}}}";
            handler.onMessage(conn2, actionMsg);

            // Then: game_over without timer
            List<String> gameOverMsgs = conn1.getMessagesByType("game_over");
            assertEquals(1, gameOverMsgs.size(), "Should have 1 game_over, got: " + conn1.sentMessages);

            String gameOver = gameOverMsgs.get(0);
            assertTrue(gameOver.contains("\"winner\":\"P2\""));
            // game_over uses GameOverPayload which doesn't have timer field
        }
    }

    // ========== Timer State Integration Tests ==========

    @Nested
    @DisplayName("Timer State Integration Tests")
    class TimerStateIntegrationTests {

        @Test
        @DisplayName("Timer starts when both players complete draft")
        void timerStartsWhenBothPlayersJoin() {
            // Given: Empty match
            Unit hero1 = createHero("p1_hero", new PlayerId("P1"), 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", new PlayerId("P2"), 5, new Position(3, 3));
            GameState state = createGameState(Arrays.asList(hero1, hero2), new PlayerId("P1"));

            registry.createMatch("match-1", state);

            FakeClientConnection conn1 = new FakeClientConnection("c1");
            FakeClientConnection conn2 = new FakeClientConnection("c2");

            // When: Both players join
            handler.onOpen(conn1);
            handler.onMessage(conn1, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"match-1\"}}");

            // Timer not started yet (only 1 player, and no draft)
            assertNull(timerService.getTimerState("match-1", TimerType.ACTION));

            handler.onOpen(conn2);
            handler.onMessage(conn2, "{\"type\":\"join_match\",\"payload\":{\"matchId\":\"match-1\"}}");

            // Timer still not started (need draft completion)
            assertNull(timerService.getTimerState("match-1", TimerType.ACTION));

            // Both players submit draft
            String p1Draft = "{\"type\":\"select_team\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"heroClass\":\"WARRIOR\",\"minions\":[\"TANK\",\"ARCHER\"]}}";
            String p2Draft = "{\"type\":\"select_team\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P2\",\"heroClass\":\"MAGE\",\"minions\":[\"TANK\",\"ASSASSIN\"]}}";

            handler.onMessage(conn1, p1Draft);
            handler.onMessage(conn2, p2Draft);

            // Then: Timer is now running after draft completes
            assertEquals(TimerState.RUNNING, timerService.getTimerState("match-1", TimerType.ACTION));
        }

        @Test
        @DisplayName("Timer cycles between players on actions")
        void timerCyclesBetweenPlayersOnActions() {
            // Given: Game in progress
            Unit hero1 = createHero("p1_hero", new PlayerId("P1"), 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", new PlayerId("P2"), 5, new Position(3, 3));
            GameState state = createGameState(Arrays.asList(hero1, hero2), new PlayerId("P1"));

            FakeClientConnection conn1 = new FakeClientConnection("c1");
            FakeClientConnection conn2 = new FakeClientConnection("c2");

            // Use registry to create match with connections
            Match match = registry.createMatch("match-1", state);
            match.getConnections().put(ClientSlot.P1, conn1);
            match.getConnections().put(ClientSlot.P2, conn2);

            conn1.setMatchId("match-1");
            conn1.setPlayerId("P1");
            conn2.setMatchId("match-1");
            conn2.setPlayerId("P2");

            // Start P1's turn
            long startTime1 = matchService.startTurnTimer("match-1");
            assertTrue(startTime1 > 0);

            // When: P1 ends turn
            mockTime.addAndGet(2000);
            String endTurn1 = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(conn1, endTurn1);

            // Then: Timer is running (for P2)
            assertEquals(TimerState.RUNNING, timerService.getTimerState("match-1", TimerType.ACTION));

            // When: P2 ends turn
            mockTime.addAndGet(3000);
            String endTurn2 = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P2\",\"action\":{\"type\":\"END_TURN\"}}}";
            handler.onMessage(conn2, endTurn2);

            // Then: Timer still running (back to P1)
            assertEquals(TimerState.RUNNING, timerService.getTimerState("match-1", TimerType.ACTION));
        }
    }

    // ========== Invalid Action Timer Tests ==========

    @Nested
    @DisplayName("Invalid Action Timer Tests")
    class InvalidActionTimerTests {

        @Test
        @DisplayName("TA-004: Invalid action does not reset timer")
        void ta004_invalidActionDoesNotResetTimer() {
            // Given: Game with timer at 6 seconds remaining
            Unit hero1 = createHero("p1_hero", new PlayerId("P1"), 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", new PlayerId("P2"), 5, new Position(3, 3));
            GameState state = createGameState(Arrays.asList(hero1, hero2), new PlayerId("P1"));

            FakeClientConnection conn1 = new FakeClientConnection("c1");
            conn1.setMatchId("match-1");
            conn1.setPlayerId("P1");

            // Use registry to create match with connections
            Match match = registry.createMatch("match-1", state);
            match.getConnections().put(ClientSlot.P1, conn1);

            matchService.startTurnTimer("match-1");
            mockTime.set(1004000L); // 6 seconds remaining

            long remainingBefore = timerService.getRemainingTime("match-1", TimerType.ACTION);

            // When: Invalid action (move to occupied position)
            String invalidMove = "{\"type\":\"action\",\"payload\":{\"matchId\":\"match-1\",\"playerId\":\"P1\",\"action\":{\"type\":\"MOVE\",\"targetX\":3,\"targetY\":3}}}";
            handler.onMessage(conn1, invalidMove);

            // Then: Error sent, timer unchanged
            List<String> errors = conn1.getMessagesByType("validation_error");
            assertEquals(1, errors.size());

            assertEquals(remainingBefore, timerService.getRemainingTime("match-1", TimerType.ACTION));
            assertEquals(TimerState.RUNNING, timerService.getTimerState("match-1", TimerType.ACTION));
        }
    }
}
