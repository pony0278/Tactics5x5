package com.tactics.server.core;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for MatchService
 * Based on SERVER_CORE_TESTPLAN_V1.md (MS-Series, MSA-Series, MSC-Series)
 */
class MatchServiceTest {

    private MatchRegistry registry;
    private RuleEngine ruleEngine;
    private GameStateSerializer serializer;
    private MatchService service;
    private PlayerId p1;
    private PlayerId p2;
    private Board board;

    @BeforeEach
    void setUp() {
        registry = new MatchRegistry(new HashMap<>());
        ruleEngine = new RuleEngine();
        serializer = new GameStateSerializer();
        service = new MatchService(registry, ruleEngine, serializer);
        p1 = new PlayerId("P1");
        p2 = new PlayerId("P2");
        board = new Board(5, 5);
    }

    private GameState createDefaultState() {
        return new GameState(board, new ArrayList<>(), p1, false, null);
    }

    private GameState createStateWithUnits(List<Unit> units, PlayerId currentPlayer) {
        return new GameState(board, units, currentPlayer, false, null);
    }

    // ========== MS-Series: MatchService Basic Operations ==========

    @Nested
    @DisplayName("MS-Series: MatchService Basic Operations")
    class MatchServiceBasicOperations {

        @Test
        @DisplayName("MS1 - getOrCreateMatch creates new match when absent")
        void ms1_getOrCreateMatchCreatesNewMatchWhenAbsent() {
            // Given: empty registry

            // When
            Match match = service.getOrCreateMatch("match-1");

            // Then
            assertNotNull(match);
            assertEquals("match-1", match.getMatchId().getValue());

            GameState state = match.getState();
            assertNotNull(state);
            assertEquals(5, state.getBoard().getWidth());
            assertEquals(5, state.getBoard().getHeight());
            assertTrue(state.getUnits().isEmpty());
            assertEquals("P1", state.getCurrentPlayer().getValue());
            assertFalse(state.isGameOver());
            assertNull(state.getWinner());
        }

        @Test
        @DisplayName("MS2 - getOrCreateMatch returns existing match on second call")
        void ms2_getOrCreateMatchReturnsExistingMatchOnSecondCall() {
            // Given
            Match firstMatch = service.getOrCreateMatch("match-1");

            // When
            Match secondMatch = service.getOrCreateMatch("match-1");

            // Then
            assertSame(firstMatch.getMatchId().getValue(), secondMatch.getMatchId().getValue());
            assertSame(firstMatch.getState(), secondMatch.getState());
            assertEquals(1, registry.listMatches().size());
        }

        @Test
        @DisplayName("MS3 - findMatch returns null if match does not exist")
        void ms3_findMatchReturnsNullIfMatchDoesNotExist() {
            // Given: empty registry

            // When
            Match result = service.findMatch("unknown-match");

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("MS4 - findMatch returns existing match")
        void ms4_findMatchReturnsExistingMatch() {
            // Given
            Match created = service.getOrCreateMatch("match-1");

            // When
            Match found = service.findMatch("match-1");

            // Then
            assertNotNull(found);
            assertEquals(created.getMatchId().getValue(), found.getMatchId().getValue());
            assertSame(created.getState(), found.getState());
        }

        @Test
        @DisplayName("MS5 - getCurrentState returns match state")
        void ms5_getCurrentStateReturnsMatchState() {
            // Given
            Match match = service.getOrCreateMatch("match-1");
            GameState expectedState = match.getState();

            // When
            GameState result = service.getCurrentState("match-1");

            // Then
            assertSame(expectedState, result);
        }

        @Test
        @DisplayName("MS6 - getCurrentState returns null for unknown match")
        void ms6_getCurrentStateReturnsNullForUnknownMatch() {
            // Given: no match exists

            // When
            GameState result = service.getCurrentState("unknown");

            // Then
            assertNull(result);
        }
    }

    // ========== MSA-Series: applyAction Tests ==========

    @Nested
    @DisplayName("MSA-Series: applyAction Tests")
    class ApplyActionTests {

        @Test
        @DisplayName("MSA1 - applyAction applies valid MOVE and updates state")
        void msa1_applyActionAppliesValidMoveAndUpdatesState() {
            // Given: match with P1 unit at (1,1)
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);
            List<Unit> units = Arrays.asList(u1_p1, u1_p2);
            GameState initialState = createStateWithUnits(units, p1);
            registry.createMatch("match-1", initialState);

            GameState oldState = registry.getMatch("match-1").getState();

            // Create MOVE action to (1,2)
            Action moveAction = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            // When
            GameState newState = service.applyAction("match-1", p1, moveAction);

            // Then
            assertNotNull(newState);
            assertNotSame(oldState, newState);

            // Verify unit moved
            Unit movedUnit = null;
            for (Unit u : newState.getUnits()) {
                if (u.getId().equals("u1_p1")) {
                    movedUnit = u;
                    break;
                }
            }
            assertNotNull(movedUnit);
            assertEquals(1, movedUnit.getPosition().getX());
            assertEquals(2, movedUnit.getPosition().getY());

            // Verify registry updated
            assertSame(newState, registry.getMatch("match-1").getState());

            // Verify old state unchanged (immutability)
            Unit oldUnit = null;
            for (Unit u : oldState.getUnits()) {
                if (u.getId().equals("u1_p1")) {
                    oldUnit = u;
                    break;
                }
            }
            assertNotNull(oldUnit);
            assertEquals(1, oldUnit.getPosition().getX());
            assertEquals(1, oldUnit.getPosition().getY());
        }

        @Test
        @DisplayName("MSA2 - applyAction applies valid ATTACK and updates state")
        void msa2_applyActionAppliesValidAttackAndUpdatesState() {
            // Given: P1 unit at (1,1) adjacent to P2 unit at (1,2)
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 2), true);
            List<Unit> units = Arrays.asList(u1_p1, u1_p2);
            GameState initialState = createStateWithUnits(units, p1);
            registry.createMatch("match-1", initialState);

            GameState oldState = registry.getMatch("match-1").getState();

            // Create ATTACK action
            Action attackAction = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            // When
            GameState newState = service.applyAction("match-1", p1, attackAction);

            // Then
            assertNotNull(newState);
            assertNotSame(oldState, newState);

            // Verify target HP decreased
            Unit targetUnit = null;
            for (Unit u : newState.getUnits()) {
                if (u.getId().equals("u1_p2")) {
                    targetUnit = u;
                    break;
                }
            }
            assertNotNull(targetUnit);
            assertEquals(7, targetUnit.getHp()); // 10 - 3 = 7

            // Verify registry updated
            assertSame(newState, registry.getMatch("match-1").getState());

            // Verify old state unchanged
            Unit oldTarget = null;
            for (Unit u : oldState.getUnits()) {
                if (u.getId().equals("u1_p2")) {
                    oldTarget = u;
                    break;
                }
            }
            assertNotNull(oldTarget);
            assertEquals(10, oldTarget.getHp());
        }

        @Test
        @DisplayName("MSA3 - applyAction applies END_TURN and switches currentPlayer")
        void msa3_applyActionAppliesEndTurnAndSwitchesPlayer() {
            // Given: match with currentPlayer = P1
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);
            List<Unit> units = Arrays.asList(u1_p1, u1_p2);
            GameState initialState = createStateWithUnits(units, p1);
            registry.createMatch("match-1", initialState);

            GameState oldState = registry.getMatch("match-1").getState();
            assertEquals("P1", oldState.getCurrentPlayer().getValue());

            // Create END_TURN action
            Action endTurnAction = new Action(ActionType.END_TURN, p1, null, null);

            // When
            GameState newState = service.applyAction("match-1", p1, endTurnAction);

            // Then
            assertNotNull(newState);
            assertNotSame(oldState, newState);
            assertEquals("P2", newState.getCurrentPlayer().getValue());

            // Verify registry updated
            assertSame(newState, registry.getMatch("match-1").getState());

            // Verify old state unchanged
            assertEquals("P1", oldState.getCurrentPlayer().getValue());
        }

        @Test
        @DisplayName("MSA4 - applyAction throws on validation failure (wrong turn)")
        void msa4_applyActionThrowsOnValidationFailureWrongTurn() {
            // Given: match with currentPlayer = P1
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);
            List<Unit> units = Arrays.asList(u1_p1, u1_p2);
            GameState initialState = createStateWithUnits(units, p1);
            registry.createMatch("match-1", initialState);

            GameState stateBefore = registry.getMatch("match-1").getState();

            // Create action from P2 (wrong turn)
            Action invalidAction = new Action(ActionType.END_TURN, p2, null, null);

            // When/Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.applyAction("match-1", p2, invalidAction)
            );

            assertTrue(exception.getMessage().contains("Not your turn"));

            // Verify registry state unchanged
            assertSame(stateBefore, registry.getMatch("match-1").getState());
        }

        @Test
        @DisplayName("MSA4 - applyAction throws on validation failure (invalid move)")
        void msa4_applyActionThrowsOnInvalidMove() {
            // Given: match with no unit adjacent to target
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);
            List<Unit> units = Arrays.asList(u1_p1, u1_p2);
            GameState initialState = createStateWithUnits(units, p1);
            registry.createMatch("match-1", initialState);

            GameState stateBefore = registry.getMatch("match-1").getState();

            // Create invalid MOVE action (distance > 1)
            Action invalidAction = new Action(ActionType.MOVE, p1, new Position(1, 4), null);

            // When/Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.applyAction("match-1", p1, invalidAction)
            );

            assertNotNull(exception.getMessage());

            // Verify registry state unchanged
            assertSame(stateBefore, registry.getMatch("match-1").getState());
        }

        @Test
        @DisplayName("MSA5 - applyAction throws for non-existing match")
        void msa5_applyActionThrowsForNonExistingMatch() {
            // Given: no match exists
            Action someAction = new Action(ActionType.END_TURN, p1, null, null);

            // When/Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.applyAction("non-existing", p1, someAction)
            );

            assertTrue(exception.getMessage().contains("Unknown match"));
        }
    }

    // ========== MSC-Series: Immutability & Consistency Tests ==========

    @Nested
    @DisplayName("MSC-Series: Immutability & Consistency Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("MSC1 - applyAction does not mutate old GameState")
        void msc1_applyActionDoesNotMutateOldGameState() {
            // Given
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);
            List<Unit> units = Arrays.asList(u1_p1, u1_p2);
            GameState initialState = createStateWithUnits(units, p1);
            registry.createMatch("match-1", initialState);

            // Save reference to old state
            GameState oldState = registry.getMatch("match-1").getState();
            String oldCurrentPlayer = oldState.getCurrentPlayer().getValue();

            // Capture old unit position
            Position oldPosition = null;
            for (Unit u : oldState.getUnits()) {
                if (u.getId().equals("u1_p1")) {
                    oldPosition = u.getPosition();
                    break;
                }
            }
            int oldX = oldPosition.getX();
            int oldY = oldPosition.getY();

            // When: apply MOVE action
            Action moveAction = new Action(ActionType.MOVE, p1, new Position(1, 2), null);
            GameState newState = service.applyAction("match-1", p1, moveAction);

            // Then
            assertNotSame(oldState, newState);
            assertSame(newState, registry.getMatch("match-1").getState());

            // Verify old state fields unchanged
            assertEquals(oldCurrentPlayer, oldState.getCurrentPlayer().getValue());

            // Verify old unit position unchanged in old state
            for (Unit u : oldState.getUnits()) {
                if (u.getId().equals("u1_p1")) {
                    assertEquals(oldX, u.getPosition().getX());
                    assertEquals(oldY, u.getPosition().getY());
                    break;
                }
            }
        }

        @Test
        @DisplayName("MSC2 - MatchRegistry getMatches exposes mutable map (current behavior)")
        void msc2_matchRegistryGetMatchesExposesMutableMap() {
            // Given
            service.getOrCreateMatch("match-1");

            // When
            Map<String, Match> matches = registry.getMatches();

            // Then: current implementation exposes mutable map
            // We document this behavior - modifications are possible
            assertNotNull(matches);
            assertEquals(1, matches.size());

            // Verify map is mutable (current behavior)
            // This test locks in the current behavior
            GameState newState = createDefaultState();
            MatchId newId = new MatchId("match-2");
            Match newMatch = new Match(newId, newState, new HashMap<>());

            // This should work because the map is mutable
            matches.put("match-2", newMatch);
            assertEquals(2, matches.size());
            assertEquals(2, registry.listMatches().size());
        }
    }
}
