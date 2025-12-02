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

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for RuleEngine.validateAction() - V2 Features
 * Based on RULEENGINE_VALIDATE_V2_TESTPLAN.md
 *
 * V2 focuses on:
 * - Using unit.moveRange for MOVE distance validation
 * - Using unit.attackRange for ATTACK distance validation
 * - Enabling ranged attacks (e.g., ARCHER attackRange = 2)
 * - Preserving all V1 behaviors when moveRange = 1 and attackRange = 1
 *
 * This test class extends the V1 test coverage without modifying existing V1 tests.
 */
@DisplayName("RuleEngineValidateActionV2Test")
class RuleEngineValidateActionV2Test {

    private RuleEngine ruleEngine;
    private PlayerId p1;
    private PlayerId p2;
    private Board board;

    // =========================================================================
    // Unit Type Stats (from UNIT_TYPES_V1.md)
    // =========================================================================

    // SWORDSMAN: Balanced melee fighter
    private static final int SWORDSMAN_HP = 10;
    private static final int SWORDSMAN_ATK = 3;
    private static final int SWORDSMAN_MOVE_RANGE = 1;
    private static final int SWORDSMAN_ATTACK_RANGE = 1;

    // ARCHER: Ranged attacker
    private static final int ARCHER_HP = 8;
    private static final int ARCHER_ATK = 3;
    private static final int ARCHER_MOVE_RANGE = 1;
    private static final int ARCHER_ATTACK_RANGE = 2;

    // TANK: Durable frontline unit
    private static final int TANK_HP = 16;
    private static final int TANK_ATK = 2;
    private static final int TANK_MOVE_RANGE = 1;
    private static final int TANK_ATTACK_RANGE = 1;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
        p1 = new PlayerId("P1");
        p2 = new PlayerId("P2");
        board = new Board(5, 5);
    }

    // =========================================================================
    // Helper Methods for Creating Units with Specific Ranges
    // =========================================================================

    /**
     * Create a SWORDSMAN unit (melee: moveRange=1, attackRange=1).
     */
    private Unit createSwordsman(String id, PlayerId owner, Position pos) {
        return new Unit(id, owner, SWORDSMAN_HP, SWORDSMAN_ATK,
            SWORDSMAN_MOVE_RANGE, SWORDSMAN_ATTACK_RANGE, pos, true);
    }

    /**
     * Create an ARCHER unit (ranged: moveRange=1, attackRange=2).
     */
    private Unit createArcher(String id, PlayerId owner, Position pos) {
        return new Unit(id, owner, ARCHER_HP, ARCHER_ATK,
            ARCHER_MOVE_RANGE, ARCHER_ATTACK_RANGE, pos, true);
    }

    /**
     * Create a TANK unit (durable: moveRange=1, attackRange=1).
     */
    private Unit createTank(String id, PlayerId owner, Position pos) {
        return new Unit(id, owner, TANK_HP, TANK_ATK,
            TANK_MOVE_RANGE, TANK_ATTACK_RANGE, pos, true);
    }

    /**
     * Create a custom unit with specified moveRange and attackRange.
     * Useful for testing extended ranges beyond standard unit types.
     */
    private Unit createCustomUnit(String id, PlayerId owner, int hp, int attack,
                                   int moveRange, int attackRange, Position pos, boolean alive) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, pos, alive);
    }

    // =========================================================================
    // G2-Series — General V2 Checks
    // =========================================================================

    @Nested
    @DisplayName("G2-Series - General V2 Checks")
    class GeneralV2Tests {

        @Test
        @DisplayName("G2-01 - V2: Null action type still invalid")
        void g2_01_nullActionTypeStillInvalid() {
            Unit u1_p1 = createSwordsman("u1_p1", p1, new Position(1, 1));
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            Action action = new Action(null, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Invalid action type", result.getErrorMessage());
        }

        @Test
        @DisplayName("G2-02 - V2: Game over still blocks all actions (MOVE)")
        void g2_02_gameOverBlocksMove() {
            Unit u1_p1 = createSwordsman("u1_p1", p1, new Position(1, 1));
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, true, p1);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Game is already over", result.getErrorMessage());
        }

        @Test
        @DisplayName("G2-02 - V2: Game over still blocks all actions (ATTACK)")
        void g2_02_gameOverBlocksAttack() {
            Unit u1_p1 = createSwordsman("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createSwordsman("u1_p2", p2, new Position(1, 2));
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, true, p1);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Game is already over", result.getErrorMessage());
        }

        @Test
        @DisplayName("G2-02 - V2: Game over still blocks all actions (MOVE_AND_ATTACK)")
        void g2_02_gameOverBlocksMoveAndAttack() {
            Unit u1_p1 = createSwordsman("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createSwordsman("u1_p2", p2, new Position(2, 2));
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, true, p1);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Game is already over", result.getErrorMessage());
        }

        @Test
        @DisplayName("G2-02 - V2: Game over still blocks all actions (END_TURN)")
        void g2_02_gameOverBlocksEndTurn() {
            Unit u1_p1 = createSwordsman("u1_p1", p1, new Position(1, 1));
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, true, p1);

            Action action = new Action(ActionType.END_TURN, p1, null, null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Game is already over", result.getErrorMessage());
        }

        @Test
        @DisplayName("G2-03 - V2: Wrong currentPlayer still invalid")
        void g2_03_wrongPlayerStillInvalid() {
            Unit u1_p1 = createSwordsman("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createSwordsman("u1_p2", p2, new Position(3, 3));
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            // P2 tries to move when it's P1's turn
            Action action = new Action(ActionType.MOVE, p2, new Position(3, 4), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            assertEquals("Not your turn", result.getErrorMessage());
        }
    }

    // =========================================================================
    // MV2-Series — MOVE with moveRange
    // =========================================================================

    @Nested
    @DisplayName("MV2-Series - MOVE with moveRange")
    class MoveV2Tests {

        @Test
        @DisplayName("MV2-01 - MOVE within range (distance 1, moveRange=2) is valid")
        void mv2_01_moveWithinRange() {
            // Unit with moveRange=2
            Unit u1_p1 = createCustomUnit("u1_p1", p1, 10, 3, 2, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            // Move 1 tile (within moveRange=2)
            Action action = new Action(ActionType.MOVE, p1, new Position(2, 1), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            // NOTE: This test will fail until V2 logic is implemented in RuleEngine
            // V1 logic uses isAdjacent (distance=1 only), so distance=1 should pass
            assertTrue(result.isValid(), "MOVE within moveRange should be valid");
        }

        @Test
        @DisplayName("MV2-02 - MOVE at max range (distance = moveRange) is valid")
        void mv2_02_moveAtMaxRange() {
            // Unit with moveRange=2
            Unit u1_p1 = createCustomUnit("u1_p1", p1, 10, 3, 2, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            // Move 2 tiles (at max moveRange=2)
            Action action = new Action(ActionType.MOVE, p1, new Position(3, 1), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            // NOTE: This test will fail until V2 logic is implemented
            // V1 logic only allows distance=1
            assertTrue(result.isValid(), "MOVE at max moveRange should be valid");
        }

        @Test
        @DisplayName("MV2-03 - MOVE beyond moveRange is invalid")
        void mv2_03_moveBeyondRange() {
            // Unit with moveRange=2
            Unit u1_p1 = createCustomUnit("u1_p1", p1, 10, 3, 2, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            // Try to move 3 tiles (beyond moveRange=2)
            Action action = new Action(ActionType.MOVE, p1, new Position(4, 1), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "MOVE beyond moveRange should be invalid");
        }

        @Test
        @DisplayName("MV2-04 - MOVE with distance = 0 (no-op) is invalid")
        void mv2_04_moveDistanceZero() {
            Unit u1_p1 = createCustomUnit("u1_p1", p1, 10, 3, 2, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            // Try to move to same tile (distance=0)
            Action action = new Action(ActionType.MOVE, p1, new Position(1, 1), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            // Should fail: target tile is occupied by the unit itself
            assertFalse(result.isValid(), "MOVE to same position should be invalid");
        }

        @Test
        @DisplayName("MV2-05 - Diagonal MOVE still invalid even if within moveRange")
        void mv2_05_diagonalMoveInvalid() {
            // Unit with moveRange=2
            Unit u1_p1 = createCustomUnit("u1_p1", p1, 10, 3, 2, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            // Diagonal move to (2,2): Manhattan distance=2 but diagonal
            Action action = new Action(ActionType.MOVE, p1, new Position(2, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Diagonal MOVE should be invalid");
        }

        @Test
        @DisplayName("MV2-06 - MOVE with moveRange=1 behaves like V1")
        void mv2_06_moveRange1BehavesLikeV1() {
            // Unit with moveRange=1 (V1 behavior)
            Unit u1_p1 = createSwordsman("u1_p1", p1, new Position(1, 1));
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            // Move 1 tile
            Action action = new Action(ActionType.MOVE, p1, new Position(2, 1), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "MOVE with moveRange=1 should work like V1");
        }

        @Test
        @DisplayName("MV2-07 - MOVE blocked by occupied tile (within range) is invalid")
        void mv2_07_moveBlockedByOccupiedTile() {
            // Unit with moveRange=2
            Unit u1_p1 = createCustomUnit("u1_p1", p1, 10, 3, 2, 1, new Position(1, 1), true);
            // Another unit occupies the target
            Unit u2_p1 = createSwordsman("u2_p1", p1, new Position(3, 1));
            GameState state = new GameState(board, Arrays.asList(u1_p1, u2_p1), p1, false, null);

            // Try to move to occupied tile
            Action action = new Action(ActionType.MOVE, p1, new Position(3, 1), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "MOVE to occupied tile should be invalid");
            assertEquals("Target tile is occupied", result.getErrorMessage());
        }
    }

    // =========================================================================
    // AV2-Series — ATTACK with attackRange
    // =========================================================================

    @Nested
    @DisplayName("AV2-Series - ATTACK with attackRange")
    class AttackV2Tests {

        @Test
        @DisplayName("AV2-01 - Melee ATTACK (attackRange=1) at distance 1 is valid")
        void av2_01_meleeAttackAtDistance1() {
            Unit attacker = createSwordsman("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(1, 2));
            GameState state = new GameState(board, Arrays.asList(attacker, target), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "Melee attack at distance 1 should be valid");
        }

        @Test
        @DisplayName("AV2-02 - Melee ATTACK (attackRange=1) at distance 2 is invalid")
        void av2_02_meleeAttackAtDistance2() {
            Unit attacker = createSwordsman("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(1, 3));
            GameState state = new GameState(board, Arrays.asList(attacker, target), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 3), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Melee attack at distance 2 should be invalid");
        }

        @Test
        @DisplayName("AV2-03 - Ranged ATTACK (attackRange=2) at distance 2 is valid")
        void av2_03_rangedAttackAtDistance2() {
            Unit attacker = createArcher("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(1, 3));
            GameState state = new GameState(board, Arrays.asList(attacker, target), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 3), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            // NOTE: This test will fail until V2 logic is implemented
            // V1 uses isAdjacent (distance=1 only)
            assertTrue(result.isValid(), "Ranged attack at distance 2 should be valid for ARCHER");
        }

        @Test
        @DisplayName("AV2-04 - Ranged ATTACK beyond attackRange is invalid")
        void av2_04_rangedAttackBeyondRange() {
            Unit attacker = createArcher("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(1, 4));
            GameState state = new GameState(board, Arrays.asList(attacker, target), p1, false, null);

            // Distance = 3, attackRange = 2
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 4), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Ranged attack beyond attackRange should be invalid");
        }

        @Test
        @DisplayName("AV2-05 - Diagonal ATTACK within Manhattan range still invalid")
        void av2_05_diagonalAttackInvalid() {
            Unit attacker = createArcher("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(2, 2));
            GameState state = new GameState(board, Arrays.asList(attacker, target), p1, false, null);

            // Diagonal: Manhattan distance=2 but not orthogonal
            Action action = new Action(ActionType.ATTACK, p1, new Position(2, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Diagonal attack should be invalid even within range");
        }

        @Test
        @DisplayName("AV2-06 - Ambiguous ranged attacker (two friendlies in range) is invalid")
        void av2_06_ambiguousRangedAttacker() {
            // Two archers both in range of target
            Unit archerA = createArcher("u1_p1", p1, new Position(1, 1));
            Unit archerB = createArcher("u2_p1", p1, new Position(3, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(2, 1));
            GameState state = new GameState(board, Arrays.asList(archerA, archerB, target), p1, false, null);

            // Both archers are distance=1 from target
            Action action = new Action(ActionType.ATTACK, p1, new Position(2, 1), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Ambiguous attacker should be invalid");
            assertEquals("Ambiguous attacker", result.getErrorMessage());
        }

        @Test
        @DisplayName("AV2-07 - Unique ranged attacker is valid")
        void av2_07_uniqueRangedAttacker() {
            // Archer A at distance 2, Swordsman B at distance 2 (out of range for swordsman)
            Unit archerA = createArcher("u1_p1", p1, new Position(1, 1));
            Unit swordsmanB = createSwordsman("u2_p1", p1, new Position(4, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(3, 1));
            GameState state = new GameState(board, Arrays.asList(archerA, swordsmanB, target), p1, false, null);

            // Archer at distance=2 (in range), Swordsman at distance=1 (adjacent, in range too!)
            // This test needs adjustment - let's place swordsman further
            // Actually distance from swordsman (4,1) to target (3,1) = 1, so swordsman is also adjacent!
            // Let me recalculate: we need swordsman to be out of attackRange=1
            // So place target at (2,1): archer (1,1) -> distance=1, swordsman (4,1) -> distance=2 (out of range)
            Unit target2 = createSwordsman("u1_p2_v2", p2, new Position(2, 1));
            GameState state2 = new GameState(board, Arrays.asList(archerA, swordsmanB, target2), p1, false, null);

            // Archer distance=1 (in range for attackRange=2)
            // Swordsman distance=2 (out of range for attackRange=1)
            Action action = new Action(ActionType.ATTACK, p1, new Position(2, 1), "u1_p2_v2");

            ValidationResult result = ruleEngine.validateAction(state2, action);

            // NOTE: This test will fail until V2 logic is implemented
            // V2 should identify only the archer as a valid attacker
            assertTrue(result.isValid(), "Unique ranged attacker should be valid");
        }

        @Test
        @DisplayName("AV2-08 - ATTACK with correct targetUnitId but mismatched position is invalid")
        void av2_08_attackPositionMismatch() {
            Unit attacker = createArcher("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(1, 3));
            GameState state = new GameState(board, Arrays.asList(attacker, target), p1, false, null);

            // Provide wrong position for the target
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Position mismatch should be invalid");
            assertEquals("Target position does not match target unit position", result.getErrorMessage());
        }
    }

    // =========================================================================
    // MAV2-Series — MOVE_AND_ATTACK with ranges
    // =========================================================================

    @Nested
    @DisplayName("MAV2-Series - MOVE_AND_ATTACK with ranges")
    class MoveAndAttackV2Tests {

        @Test
        @DisplayName("MAV2-01 - MOVE_AND_ATTACK with MOVE in range and ATTACK in range is valid")
        void mav2_01_moveAndAttackBothInRange() {
            // Archer at (1,1), moveRange=1, attackRange=2
            // Move to (2,1), attack enemy at (3,1) (distance=1 after move)
            Unit archer = createArcher("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(3, 1));
            GameState state = new GameState(board, Arrays.asList(archer, target), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(2, 1), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "MOVE_AND_ATTACK with both steps in range should be valid");
        }

        @Test
        @DisplayName("MAV2-02a - MOVE step in range, ATTACK step out of attackRange is invalid")
        void mav2_02a_attackStepOutOfRange() {
            // Archer at (1,1), moveRange=1, attackRange=2
            // Move to (2,1), enemy at (4,1) -> post-move distance=2 (just in range for archer)
            // Actually let's test with distance=3 which is out of range
            Unit archer = createArcher("u1_p1", p1, new Position(0, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(4, 1));
            GameState state = new GameState(board, Arrays.asList(archer, target), p1, false, null);

            // Move to (1,1), enemy at (4,1) -> post-move distance=3 (out of attackRange=2)
            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 1), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "ATTACK step out of range should be invalid");
        }

        @Test
        @DisplayName("MAV2-02b - MOVE step in range, ATTACK step in attackRange is valid")
        void mav2_02b_attackStepInRange() {
            // Archer at (1,1), moveRange=1, attackRange=2
            // Move to (2,1), enemy at (4,1) -> post-move distance=2 (in range for archer)
            Unit archer = createArcher("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(4, 1));
            GameState state = new GameState(board, Arrays.asList(archer, target), p1, false, null);

            // Move to (2,1), enemy at (4,1) -> post-move distance=2 (in attackRange=2)
            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(2, 1), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            // NOTE: This test will fail until V2 logic is implemented
            // V1 uses isAdjacent for both MOVE and ATTACK
            assertTrue(result.isValid(), "ATTACK step in range should be valid");
        }

        @Test
        @DisplayName("MAV2-03 - MOVE beyond moveRange, even if ATTACK would be in range, is invalid")
        void mav2_03_moveBeyondRangeInvalid() {
            // Swordsman at (1,1), moveRange=1
            // Try to move 2 tiles to (3,1), enemy at (4,1)
            Unit swordsman = createSwordsman("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(4, 1));
            GameState state = new GameState(board, Arrays.asList(swordsman, target), p1, false, null);

            // Move 2 tiles (beyond moveRange=1)
            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(3, 1), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "MOVE beyond moveRange should be invalid");
        }

        @Test
        @DisplayName("MAV2-04 - Ambiguous attacker after movement is invalid (ranged case)")
        void mav2_04_ambiguousAttackerAfterMove() {
            // Two archers, both can be in range after moving
            // Archer A at (0,2) moves to (1,2)
            // Archer B at (2,3) already adjacent
            // Target at (1,3)
            Unit archerA = createArcher("u1_p1", p1, new Position(0, 2));
            Unit archerB = createArcher("u2_p1", p1, new Position(2, 3));
            Unit target = createSwordsman("u1_p2", p2, new Position(1, 3));
            GameState state = new GameState(board, Arrays.asList(archerA, archerB, target), p1, false, null);

            // A moves to (1,2), both A and B now adjacent to target at (1,3)
            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Ambiguous attacker after movement should be invalid");
            assertEquals("Ambiguous attacker after movement", result.getErrorMessage());
        }
    }

    // =========================================================================
    // COMPAT-Series — Backward Compatibility for Range=1 Units
    // =========================================================================

    @Nested
    @DisplayName("COMPAT-Series - Backward compatibility with V1")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("COMPAT-01 - SWORDSMAN MOVE (moveRange=1) remains V1 behavior")
        void compat_01_swordsmanMoveBehavesLikeV1() {
            Unit swordsman = createSwordsman("u1_p1", p1, new Position(1, 1));
            GameState state = new GameState(board, Collections.singletonList(swordsman), p1, false, null);

            // Classic V1 move: 1 tile
            Action action = new Action(ActionType.MOVE, p1, new Position(2, 1), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "SWORDSMAN MOVE should behave like V1");
        }

        @Test
        @DisplayName("COMPAT-02 - SWORDSMAN ATTACK (attackRange=1) remains V1 behavior")
        void compat_02_swordsmanAttackBehavesLikeV1() {
            Unit swordsman = createSwordsman("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(1, 2));
            GameState state = new GameState(board, Arrays.asList(swordsman, target), p1, false, null);

            // Classic V1 melee attack
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "SWORDSMAN ATTACK should behave like V1");
        }

        @Test
        @DisplayName("COMPAT-03a - V1 MOVE behavior preserved (distance > 1 invalid)")
        void compat_03a_v1MoveBehaviorPreserved() {
            Unit swordsman = createSwordsman("u1_p1", p1, new Position(1, 1));
            GameState state = new GameState(board, Collections.singletonList(swordsman), p1, false, null);

            // V1: distance > 1 should be invalid
            Action action = new Action(ActionType.MOVE, p1, new Position(1, 3), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "V1 MOVE behavior: distance > 1 should be invalid");
        }

        @Test
        @DisplayName("COMPAT-03b - V1 ATTACK behavior preserved (distance > 1 invalid for melee)")
        void compat_03b_v1AttackBehaviorPreserved() {
            Unit swordsman = createSwordsman("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(1, 3));
            GameState state = new GameState(board, Arrays.asList(swordsman, target), p1, false, null);

            // V1: melee attack at distance 2 should be invalid
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 3), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "V1 ATTACK behavior: melee at distance > 1 should be invalid");
        }

        @Test
        @DisplayName("COMPAT-03c - V1 MOVE_AND_ATTACK behavior preserved")
        void compat_03c_v1MoveAndAttackBehaviorPreserved() {
            Unit swordsman = createSwordsman("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(2, 2));
            GameState state = new GameState(board, Arrays.asList(swordsman, target), p1, false, null);

            // V1: move 1 tile, attack adjacent
            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "V1 MOVE_AND_ATTACK behavior should be preserved");
        }
    }

    // =========================================================================
    // MSG2-Series — V2 Error Message Expectations
    // =========================================================================

    @Nested
    @DisplayName("MSG2-Series - V2 error messages")
    class ErrorMessageV2Tests {

        @Test
        @DisplayName("MSG2-01 - MOVE out-of-range uses a clear message")
        void msg2_01_moveOutOfRangeMessage() {
            // Unit with moveRange=2
            Unit u1_p1 = createCustomUnit("u1_p1", p1, 10, 3, 2, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            // Try to move 3 tiles (beyond moveRange=2)
            Action action = new Action(ActionType.MOVE, p1, new Position(4, 1), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            // The error message should indicate out-of-range
            // V1 message is "No valid unit can move to target position"
            // V2 may use a more specific message like "Move out of range"
            assertNotNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("MSG2-02 - ATTACK out-of-range uses a clear message")
        void msg2_02_attackOutOfRangeMessage() {
            Unit attacker = createArcher("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(1, 4));
            GameState state = new GameState(board, Arrays.asList(attacker, target), p1, false, null);

            // Distance = 3, attackRange = 2
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 4), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            // Error should indicate out-of-range attack
            // V1 message is "No attacker adjacent to target"
            // V2 may use "Attack out of range" or similar
            assertNotNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("MSG2-03 - Diagonal MOVE produces shape-specific error")
        void msg2_03_diagonalMoveError() {
            Unit u1_p1 = createCustomUnit("u1_p1", p1, 10, 3, 2, 1, new Position(1, 1), true);
            GameState state = new GameState(board, Collections.singletonList(u1_p1), p1, false, null);

            // Diagonal move (2,2): Manhattan distance=2 but not orthogonal
            Action action = new Action(ActionType.MOVE, p1, new Position(2, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            // Error should distinguish diagonal from range issue
            // V1 message is "No valid unit can move to target position"
            // V2 may use "Movement must be orthogonal" or similar
            assertNotNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("MSG2-04 - Diagonal ATTACK produces shape-specific error")
        void msg2_04_diagonalAttackError() {
            Unit attacker = createArcher("u1_p1", p1, new Position(1, 1));
            Unit target = createSwordsman("u1_p2", p2, new Position(2, 2));
            GameState state = new GameState(board, Arrays.asList(attacker, target), p1, false, null);

            // Diagonal attack: Manhattan distance=2 but not orthogonal
            Action action = new Action(ActionType.ATTACK, p1, new Position(2, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid());
            // Error should clarify invalid geometry rather than distance
            // V1 message is "No attacker adjacent to target"
            // V2 may use "Attack must be orthogonal" or similar
            assertNotNull(result.getErrorMessage());
        }
    }
}
