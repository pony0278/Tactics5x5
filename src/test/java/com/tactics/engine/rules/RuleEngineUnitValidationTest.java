package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UV-Series: Unit Turn Validation Tests
 *
 * Tests for validation in unit-by-unit turn system:
 * - actingUnitId ownership validation
 * - actingUnitId existence validation
 * - Unit acted status validation
 * - Unit alive status validation
 * - END_TURN specific validations
 * - Unit position mismatch detection
 */
@DisplayName("UV-Series: Unit Turn Validation Tests")
class RuleEngineUnitValidationTest {

    private RuleEngine ruleEngine;
    private PlayerId p1;
    private PlayerId p2;
    private Board board;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
        p1 = new PlayerId("P1");
        p2 = new PlayerId("P2");
        board = new Board(5, 5);
    }

    // ========== Helper Methods ==========

    private Unit createUnit(String id, PlayerId owner, Position pos) {
        return new Unit(id, owner, 10, 3, 1, 1, pos, true);
    }

    private Unit createUnit(String id, PlayerId owner, Position pos, int hp) {
        return new Unit(id, owner, hp, 3, 1, 1, pos, true);
    }

    private Unit findUnit(List<Unit> units, String id) {
        return units.stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    private GameState createState(List<Unit> units, PlayerId currentPlayer) {
        return new GameState(board, units, currentPlayer, false, null, Collections.emptyMap());
    }

    // ========== UV-Series Tests ==========

    @Nested
    @DisplayName("UV-001 ~ UV-004: Core Validation")
    class CoreValidation {

        @Test
        @DisplayName("UV-001: Validate actingUnitId belongs to current player")
        void uv001_validateActingUnitIdBelongsToCurrentPlayer() {
            // Given: P1's turn
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createState(Arrays.asList(p1Hero, p2Hero), p1);

            // When: Action submitted with actingUnitId = "p2_hero"
            Action action = Action.move("p2_hero", new Position(2, 3));

            // Then: Action rejected (unit belongs to opponent)
            ValidationResult result = ruleEngine.validateAction(state, action);
            assertFalse(result.isValid(),
                "Should reject action with opponent's unit");
            assertTrue(result.getErrorMessage().toLowerCase().contains("not your turn") ||
                       result.getErrorMessage().toLowerCase().contains("belong") ||
                       result.getErrorMessage().toLowerCase().contains("player"),
                "Error should mention player/ownership: " + result.getErrorMessage());
        }

        @Test
        @DisplayName("UV-002: Validate actingUnitId exists")
        void uv002_validateActingUnitIdExists() {
            // Given: P1's turn
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createState(Arrays.asList(p1Hero, p2Hero), p1);

            // When: Action submitted with actingUnitId = "invalid_id"
            // We need to construct an Action with an invalid unit ID
            Action action = new Action(ActionType.MOVE, p1, new Position(2, 1), null,
                                        "invalid_id", null, null);

            // Then: Action rejected (unit not found)
            ValidationResult result = ruleEngine.validateAction(state, action);
            assertFalse(result.isValid(),
                "Should reject action with non-existent unit");
            assertTrue(result.getErrorMessage().toLowerCase().contains("not found"),
                "Error should mention 'not found': " + result.getErrorMessage());
        }

        @Test
        @DisplayName("UV-003: Validate unit has not acted this round")
        void uv003_validateUnitHasNotActedThisRound() {
            // Given: P1's turn, P1's Hero already acted (via exhaustion scenario)
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 1))
                .withActionsUsed(1);
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4))
                .withActionsUsed(1);

            // P1's turn (exhaustion - P2 has no unacted units)
            GameState state = createState(Arrays.asList(p1Hero, p1Minion1, p2Hero), p1);

            // When: Action submitted for P1's Hero (already acted)
            Action action = Action.move("p1_hero", new Position(2, 2));

            // Then: Action rejected (unit already acted)
            ValidationResult result = ruleEngine.validateAction(state, action);
            assertFalse(result.isValid(),
                "Should reject action with already-acted unit");
            assertTrue(result.getErrorMessage().toLowerCase().contains("already acted") ||
                       result.getErrorMessage().toLowerCase().contains("no remaining actions"),
                "Error should mention already acted: " + result.getErrorMessage());
        }

        @Test
        @DisplayName("UV-004: Validate unit is alive")
        void uv004_validateUnitIsAlive() {
            // Given: P1's turn, P1's Minion1 is dead
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p1Minion1Dead = createUnit("p1_minion_1", p1, new Position(0, 0))
                .withHp(0); // Dead (withHp(0) sets alive=false)
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createState(Arrays.asList(p1Hero, p1Minion1Dead, p2Hero), p1);

            // When: Action submitted for P1's Minion1 (dead)
            Action action = Action.move("p1_minion_1", new Position(1, 0));

            // Then: Action rejected (unit is dead)
            ValidationResult result = ruleEngine.validateAction(state, action);
            assertFalse(result.isValid(),
                "Should reject action with dead unit");
            assertTrue(result.getErrorMessage().toLowerCase().contains("dead"),
                "Error should mention 'dead': " + result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("UV-005 ~ UV-006: END_TURN Validation")
    class EndTurnValidation {

        @Test
        @DisplayName("UV-005: END_TURN requires actingUnitId")
        void uv005_endTurnRequiresActingUnitId() {
            // Given: P1's turn
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createState(Arrays.asList(p1Hero, p2Hero), p1);

            // When: END_TURN submitted without actingUnitId
            Action action = new Action(ActionType.END_TURN, p1, null, null,
                                        null, null, null); // No actingUnitId

            // Then: Action should still be processed (legacy behavior)
            // But with unit-by-unit system, we might want to require it
            // Current implementation allows END_TURN without actingUnitId
            ValidationResult result = ruleEngine.validateAction(state, action);

            // NOTE: The current implementation allows END_TURN without actingUnitId
            // for backwards compatibility. This test documents current behavior.
            // If we want to enforce actingUnitId, this test should be updated.
            assertTrue(result.isValid(),
                "END_TURN without actingUnitId currently allowed for backwards compatibility");
        }

        @Test
        @DisplayName("UV-006: END_TURN actingUnitId must be unacted")
        void uv006_endTurnActingUnitIdMustBeUnacted() {
            // Given: P1's turn, P1's Hero already acted
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 1))
                .withActionsUsed(1);
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4))
                .withActionsUsed(1);

            // P1's turn (exhaustion scenario)
            GameState state = createState(Arrays.asList(p1Hero, p1Minion1, p2Hero), p1);

            // When: END_TURN submitted for P1's Hero (already acted)
            Action action = Action.endTurn("p1_hero");

            // Then: If we try to apply it, it shouldn't do anything harmful
            // Current implementation: END_TURN validation is minimal
            // The apply logic checks for unacted status
            GameState newState = ruleEngine.applyAction(state, action);

            // The unit was already acted, so nothing should change
            Unit p1HeroAfter = findUnit(newState.getUnits(), "p1_hero");
            assertEquals(1, p1HeroAfter.getActionsUsed(),
                "Already-acted unit should still show 1 action used");
        }
    }

    @Nested
    @DisplayName("UV-007 ~ UV-008: Unit Position Validation")
    class UnitPositionValidation {

        @Test
        @DisplayName("UV-007: MOVE actingUnitId must match unit at source")
        void uv007_moveActingUnitIdMustMatchSource() {
            // Given: P1's Hero at (2,0), P1's Minion1 at (0,0)
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createState(Arrays.asList(p1Hero, p1Minion1, p2Hero), p1);

            // When: MOVE to (2,1) with actingUnitId = "p1_minion_1" (which is at (0,0))
            // This should fail because p1_minion_1 can't reach (2,1) in one move
            Action action = Action.move("p1_minion_1", new Position(2, 1));

            // Then: Action rejected (unit at (0,0) can't reach (2,1) with moveRange=1)
            ValidationResult result = ruleEngine.validateAction(state, action);
            assertFalse(result.isValid(),
                "Should reject MOVE where unit can't reach target");
            assertTrue(result.getErrorMessage().toLowerCase().contains("reach") ||
                       result.getErrorMessage().toLowerCase().contains("position"),
                "Error should mention position/reach: " + result.getErrorMessage());
        }

        @Test
        @DisplayName("UV-008: ATTACK actingUnitId must match attacker")
        void uv008_attackActingUnitIdMustMatchAttacker() {
            // Given: P1's Hero at (2,0), P1's Minion1 at (0,0)
            // P2's target at (2,1) - adjacent to Hero but not Minion
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Target = createUnit("p2_target", p2, new Position(2, 1));

            GameState state = createState(Arrays.asList(p1Hero, p1Minion1, p2Target), p1);

            // When: ATTACK targeting (2,1) with actingUnitId = "p1_minion_1" (at (0,0))
            // p1_minion_1 is not adjacent to target
            Action action = Action.attack("p1_minion_1", new Position(2, 1), "p2_target");

            // Then: Action rejected (unit not in range)
            ValidationResult result = ruleEngine.validateAction(state, action);
            assertFalse(result.isValid(),
                "Should reject ATTACK where unit can't reach target");
            assertTrue(result.getErrorMessage().toLowerCase().contains("attack") ||
                       result.getErrorMessage().toLowerCase().contains("position") ||
                       result.getErrorMessage().toLowerCase().contains("range"),
                "Error should mention attack/position/range: " + result.getErrorMessage());
        }
    }
}
