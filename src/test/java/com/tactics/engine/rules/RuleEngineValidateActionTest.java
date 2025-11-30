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
 * JUnit 5 tests for RuleEngine.validateAction()
 * Based on RULEENGINE_TESTPLAN_V1.md
 */
class RuleEngineValidateActionTest {

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

    // ========== G-Series: General Validation ==========

    @Nested
    @DisplayName("G-Series: General Validation")
    class GeneralValidation {

        @Test
        @DisplayName("G1 - Null Action Type")
        void g1_nullActionType() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(null, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Invalid action type", result.getErrorMessage());
        }

        @Test
        @DisplayName("G2 - Wrong Player Turn")
        void g2_wrongPlayerTurn() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            // P2 tries to move when it's P1's turn
            Action action = new Action(ActionType.MOVE, p2, new Position(3, 4), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Not your turn", result.getErrorMessage());
        }

        @Test
        @DisplayName("G3 - Game Already Over")
        void g3_gameAlreadyOver() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, true, p1);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Game is already over", result.getErrorMessage());
        }

        @Test
        @DisplayName("G3 - Game Already Over (END_TURN)")
        void g3_gameAlreadyOver_endTurn() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, true, p1);

            Action action = new Action(ActionType.END_TURN, p1, null, null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Game is already over", result.getErrorMessage());
        }
    }

    // ========== M-Series: MOVE Validation ==========

    @Nested
    @DisplayName("M-Series: MOVE Validation")
    class MoveValidation {

        @Test
        @DisplayName("M1 - Valid MOVE, Inside Board, Empty Destination")
        void m1_validMove() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("M2 - MOVE Out of Board (Negative Coordinate)")
        void m2_moveOutOfBoardNegative() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(0, 0), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(-1, 0), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target position is outside the board", result.getErrorMessage());
        }

        @Test
        @DisplayName("M3 - MOVE Out of Board (Beyond Max Coordinate)")
        void m3_moveOutOfBoardMax() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(4, 4), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(5, 4), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target position is outside the board", result.getErrorMessage());
        }

        @Test
        @DisplayName("M4 - MOVE More Than One Tile")
        void m4_moveMoreThanOneTile() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 3), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("No valid unit can move to target position", result.getErrorMessage());
        }

        @Test
        @DisplayName("M5 - MOVE Diagonally")
        void m5_moveDiagonally() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(2, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("No valid unit can move to target position", result.getErrorMessage());
        }

        @Test
        @DisplayName("M6 - MOVE Into Occupied Tile")
        void m6_moveIntoOccupiedTile() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u2_p1 = new Unit("u2_p1", p1, 10, 3, 1, 1, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u2_p1), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target tile is occupied", result.getErrorMessage());
        }

        @Test
        @DisplayName("M7 - MOVE with Dead Unit")
        void m7_moveWithDeadUnit() {
            Unit u1_p1 = new Unit("u1_p1", p1, 0, 3, 1, 1, new Position(1, 1), false);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("No valid unit can move to target position", result.getErrorMessage());
        }

        @Test
        @DisplayName("M8 - MOVE with targetUnitId Non-null (Protocol Misuse)")
        void m8_moveWithTargetUnitId() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), "uX");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("MOVE must not specify targetUnitId", result.getErrorMessage());
        }
    }

    // ========== A-Series: ATTACK Validation ==========

    @Nested
    @DisplayName("A-Series: ATTACK Validation")
    class AttackValidation {

        @Test
        @DisplayName("A1 - Valid ATTACK (Single Adjacent Enemy)")
        void a1_validAttack() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("A2 - ATTACK with Target Out of Range (More Than 1 Tile)")
        void a2_attackOutOfRange() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 3), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 3), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("No attacker adjacent to target", result.getErrorMessage());
        }

        @Test
        @DisplayName("A3 - ATTACK with Diagonal Target")
        void a3_attackDiagonalTarget() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(2, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(2, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("No attacker adjacent to target", result.getErrorMessage());
        }

        @Test
        @DisplayName("A4 - ATTACK Self / Friendly Unit")
        void a4_attackFriendlyUnit() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u2_p1 = new Unit("u2_p1", p1, 10, 3, 1, 1, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u2_p1), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u2_p1");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Cannot attack own unit", result.getErrorMessage());
        }

        @Test
        @DisplayName("A5 - ATTACK with Dead Attacker")
        void a5_attackWithDeadAttacker() {
            Unit u1_p1 = new Unit("u1_p1", p1, 0, 3, 1, 1, new Position(1, 1), false);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("No attacker adjacent to target", result.getErrorMessage());
        }

        @Test
        @DisplayName("A6 - ATTACK with Dead Target")
        void a6_attackDeadTarget() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 0, 3, 1, 1, new Position(1, 2), false);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target unit is dead", result.getErrorMessage());
        }

        @Test
        @DisplayName("A7 - ATTACK with Missing targetUnitId")
        void a7_attackMissingTargetUnitId() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target unit ID is required for ATTACK", result.getErrorMessage());
        }

        @Test
        @DisplayName("A8 - ATTACK with Missing targetPosition")
        void a8_attackMissingTargetPosition() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, null, "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target position is required for ATTACK", result.getErrorMessage());
        }

        @Test
        @DisplayName("A9 - ATTACK Ambiguous Attacker (Multiple Friendlies Adjacent)")
        void a9_attackAmbiguousAttacker() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u2_p1 = new Unit("u2_p1", p1, 10, 3, 1, 1, new Position(1, 3), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u2_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Ambiguous attacker", result.getErrorMessage());
        }
    }

    // ========== MA-Series: MOVE_AND_ATTACK Validation ==========

    @Nested
    @DisplayName("MA-Series: MOVE_AND_ATTACK Validation")
    class MoveAndAttackValidation {

        @Test
        @DisplayName("MA1 - Valid MOVE_AND_ATTACK")
        void ma1_validMoveAndAttack() {
            // u1_p1 at (1,1), moves to (1,2), attacks u1_p2 at (2,2)
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(2, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("MA2 - MOVE_AND_ATTACK with Illegal MOVE Step (distance 2)")
        void ma2_moveAndAttackIllegalMoveStep() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 4), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 3), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("No valid unit can move to target position", result.getErrorMessage());
        }

        @Test
        @DisplayName("MA3 - MOVE_AND_ATTACK with No Adjacent Enemy After Move")
        void ma3_moveAndAttackNoAdjacentEnemy() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(4, 4), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target not adjacent after movement", result.getErrorMessage());
        }

        @Test
        @DisplayName("MA4 - MOVE_AND_ATTACK with Missing targetUnitId")
        void ma4_moveAndAttackMissingTargetUnitId() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(2, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target unit ID is required for MOVE_AND_ATTACK", result.getErrorMessage());
        }

        @Test
        @DisplayName("MA5 - MOVE_AND_ATTACK with Missing targetPosition")
        void ma5_moveAndAttackMissingTargetPosition() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(2, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, null, "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target position is required for MOVE_AND_ATTACK", result.getErrorMessage());
        }

        @Test
        @DisplayName("MA6 - MOVE_AND_ATTACK Ambiguous Attacker After Move")
        void ma6_moveAndAttackAmbiguousAttacker() {
            // u1_p1 at (0,2) moves to (1,2), u2_p1 at (2,2), both adjacent to u1_p2 at (1,3)
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(0, 2), true);
            Unit u2_p1 = new Unit("u2_p1", p1, 10, 3, 1, 1, new Position(2, 3), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 3), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u2_p1, u1_p2), p1, false, null);

            // u1_p1 moves to (1,2), now both (1,2) and (2,3) are adjacent to (1,3)
            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Ambiguous attacker after movement", result.getErrorMessage());
        }

        @Test
        @DisplayName("MA7 - MOVE_AND_ATTACK with Dead Attacker")
        void ma7_moveAndAttackDeadAttacker() {
            Unit u1_p1 = new Unit("u1_p1", p1, 0, 3, 1, 1, new Position(1, 1), false);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(2, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("No valid unit can move to target position", result.getErrorMessage());
        }

        @Test
        @DisplayName("MA8 - MOVE_AND_ATTACK with Dead Target")
        void ma8_moveAndAttackDeadTarget() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 0, 3, 1, 1, new Position(2, 2), false);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target unit is dead", result.getErrorMessage());
        }

        @Test
        @DisplayName("MA - MOVE_AND_ATTACK into Occupied Tile")
        void ma_moveAndAttackOccupiedTile() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u2_p1 = new Unit("u2_p1", p1, 10, 3, 1, 1, new Position(1, 2), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(2, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u2_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target tile is occupied", result.getErrorMessage());
        }

        @Test
        @DisplayName("MA - MOVE_AND_ATTACK Target Not Found")
        void ma_moveAndAttackTargetNotFound() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "nonexistent");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target unit not found", result.getErrorMessage());
        }

        @Test
        @DisplayName("MA - MOVE_AND_ATTACK Cannot Attack Own Unit")
        void ma_moveAndAttackCannotAttackOwn() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u2_p1 = new Unit("u2_p1", p1, 10, 3, 1, 1, new Position(2, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u2_p1), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u2_p1");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Cannot attack own unit", result.getErrorMessage());
        }
    }

    // ========== ET-Series: END_TURN Validation ==========

    @Nested
    @DisplayName("ET-Series: END_TURN Validation")
    class EndTurnValidation {

        @Test
        @DisplayName("ET1 - Valid END_TURN")
        void et1_validEndTurn() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(ActionType.END_TURN, p1, null, null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("ET2 - END_TURN When Game Over")
        void et2_endTurnGameOver() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, true, p1);

            Action action = new Action(ActionType.END_TURN, p1, null, null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Game is already over", result.getErrorMessage());
        }
    }

    // ========== E-Series: Edge Cases ==========

    @Nested
    @DisplayName("E-Series: Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("E1 - No Units on Board (MOVE)")
        void e1_noUnitsMove() {
            List<Unit> emptyUnits = Collections.emptyList();
            GameState state = new GameState(board, emptyUnits, p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("No valid unit can move to target position", result.getErrorMessage());
        }

        @Test
        @DisplayName("E1 - No Units on Board (ATTACK)")
        void e1_noUnitsAttack() {
            List<Unit> emptyUnits = Collections.emptyList();
            GameState state = new GameState(board, emptyUnits, p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "nonexistent");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target unit not found", result.getErrorMessage());
        }

        @Test
        @DisplayName("E2 - All Opponent Units Dead (Game Over)")
        void e2_allOpponentUnitsDead() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 0, 3, 1, 1, new Position(2, 2), false);
            // Game should be marked as over
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, true, p1);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Game is already over", result.getErrorMessage());
        }

        @Test
        @DisplayName("E - ATTACK Target Not Found")
        void e_attackTargetNotFound() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "nonexistent");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target unit not found", result.getErrorMessage());
        }

        @Test
        @DisplayName("E - ATTACK Position Mismatch")
        void e_attackPositionMismatch() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            // Wrong position for target
            Action action = new Action(ActionType.ATTACK, p1, new Position(2, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target position does not match target unit position", result.getErrorMessage());
        }

        @Test
        @DisplayName("E - MOVE Ambiguous (Multiple Units Can Move)")
        void e_moveAmbiguous() {
            // Two units can both move to the same position
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u2_p1 = new Unit("u2_p1", p1, 10, 3, 1, 1, new Position(1, 3), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u2_p1), p1, false, null);

            // Both units are adjacent to (1,2)
            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Ambiguous move", result.getErrorMessage());
        }

        @Test
        @DisplayName("E - MOVE_AND_ATTACK Out of Board")
        void e_moveAndAttackOutOfBoard() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, 1, 1, new Position(4, 4), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, 1, 1, new Position(4, 3), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(5, 4), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Target position is outside the board", result.getErrorMessage());
        }
    }
}
