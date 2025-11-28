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
 * JUnit 5 tests for RuleEngine.applyAction()
 * Based on RULEENGINE_APPLY_TESTPLAN_V1.md
 */
class RuleEngineApplyActionTest {

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

    // Helper to find unit by ID in a list
    private Unit findUnit(List<Unit> units, String id) {
        for (Unit u : units) {
            if (u.getId().equals(id)) {
                return u;
            }
        }
        return null;
    }

    // ========== AM-Series: MOVE Apply Tests ==========

    @Nested
    @DisplayName("AM-Series: MOVE Apply Tests")
    class MoveApplyTests {

        @Test
        @DisplayName("AM1 - Basic MOVE Updates Position")
        void am1_basicMoveUpdatesPosition() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(3, 3), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify unit moved
            Unit movedUnit = findUnit(newState.getUnits(), "u1_p1");
            assertNotNull(movedUnit);
            assertEquals(1, movedUnit.getPosition().getX());
            assertEquals(2, movedUnit.getPosition().getY());

            // Verify other unit unchanged
            Unit otherUnit = findUnit(newState.getUnits(), "u1_p2");
            assertNotNull(otherUnit);
            assertEquals(3, otherUnit.getPosition().getX());
            assertEquals(3, otherUnit.getPosition().getY());

            // Verify currentPlayer unchanged (MOVE does not switch turn)
            assertEquals("P1", newState.getCurrentPlayer().getValue());

            // Verify game state
            assertFalse(newState.isGameOver());
            assertNull(newState.getWinner());
        }

        @Test
        @DisplayName("AM2 - MOVE Does Not Mutate Original State")
        void am2_moveDoesNotMutateOriginalState() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(3, 3), true);
            GameState originalState = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            GameState newState = ruleEngine.applyAction(originalState, action);

            // Verify original state is unchanged
            Unit originalUnit = findUnit(originalState.getUnits(), "u1_p1");
            assertNotNull(originalUnit);
            assertEquals(1, originalUnit.getPosition().getX());
            assertEquals(1, originalUnit.getPosition().getY());

            // Verify new state has moved unit
            Unit movedUnit = findUnit(newState.getUnits(), "u1_p1");
            assertNotNull(movedUnit);
            assertEquals(1, movedUnit.getPosition().getX());
            assertEquals(2, movedUnit.getPosition().getY());

            // Verify different instances
            assertNotSame(originalState, newState);
        }

        @Test
        @DisplayName("AM3 - MOVE with Multiple Units (Only One Moves)")
        void am3_moveWithMultipleUnits() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u2_p1 = new Unit("u2_p1", p1, 10, 3, new Position(3, 3), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(4, 4), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u2_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify u1_p1 moved
            Unit movedUnit = findUnit(newState.getUnits(), "u1_p1");
            assertEquals(1, movedUnit.getPosition().getX());
            assertEquals(2, movedUnit.getPosition().getY());

            // Verify u2_p1 unchanged
            Unit unchangedUnit = findUnit(newState.getUnits(), "u2_p1");
            assertEquals(3, unchangedUnit.getPosition().getX());
            assertEquals(3, unchangedUnit.getPosition().getY());

            // Verify enemy unit unchanged
            Unit enemyUnit = findUnit(newState.getUnits(), "u1_p2");
            assertEquals(4, enemyUnit.getPosition().getX());
            assertEquals(4, enemyUnit.getPosition().getY());
        }
    }

    // ========== AA-Series: ATTACK Apply Tests ==========

    @Nested
    @DisplayName("AA-Series: ATTACK Apply Tests")
    class AttackApplyTests {

        @Test
        @DisplayName("AA1 - Basic ATTACK Reduces Target HP")
        void aa1_basicAttackReducesHp() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify target HP reduced
            Unit target = findUnit(newState.getUnits(), "u1_p2");
            assertEquals(7, target.getHp()); // 10 - 3 = 7
            assertTrue(target.isAlive());

            // Verify attacker unchanged
            Unit attacker = findUnit(newState.getUnits(), "u1_p1");
            assertEquals(10, attacker.getHp());
            assertEquals(3, attacker.getAttack());

            // Verify currentPlayer unchanged (ATTACK does not switch turn)
            assertEquals("P1", newState.getCurrentPlayer().getValue());

            // Verify game not over
            assertFalse(newState.isGameOver());
            assertNull(newState.getWinner());
        }

        @Test
        @DisplayName("AA2 - ATTACK Kills Target (HP Drops to 0 or Below)")
        void aa2_attackKillsTarget() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 5, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 4, 3, new Position(1, 2), true);
            Unit u2_p2 = new Unit("u2_p2", p2, 10, 3, new Position(3, 3), true); // Another P2 unit
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2, u2_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify target killed
            Unit target = findUnit(newState.getUnits(), "u1_p2");
            assertEquals(-1, target.getHp()); // 4 - 5 = -1
            assertFalse(target.isAlive());

            // Verify currentPlayer unchanged
            assertEquals("P1", newState.getCurrentPlayer().getValue());

            // Game not over because P2 still has u2_p2 alive
            assertFalse(newState.isGameOver());
            assertNull(newState.getWinner());
        }

        @Test
        @DisplayName("AA3 - ATTACK That Kills Last Enemy Triggers Game Over")
        void aa3_attackKillsLastEnemyGameOver() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 3, 3, new Position(1, 2), true); // Only P2 unit
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify target killed
            Unit target = findUnit(newState.getUnits(), "u1_p2");
            assertEquals(0, target.getHp()); // 3 - 3 = 0
            assertFalse(target.isAlive());

            // Verify game over with P1 as winner
            assertTrue(newState.isGameOver());
            assertNotNull(newState.getWinner());
            assertEquals("P1", newState.getWinner().getValue());
        }

        @Test
        @DisplayName("AA4 - ATTACK That Damages but Does Not Kill Does Not End Game")
        void aa4_attackDamagesNoKill() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify target damaged but alive
            Unit target = findUnit(newState.getUnits(), "u1_p2");
            assertEquals(7, target.getHp());
            assertTrue(target.isAlive());

            // Verify game not over
            assertFalse(newState.isGameOver());
            assertNull(newState.getWinner());
        }

        @Test
        @DisplayName("AA5 - ATTACK Does Not Mutate Original GameState")
        void aa5_attackDoesNotMutateOriginalState() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(1, 2), true);
            GameState originalState = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(originalState, action);

            // Verify original state unchanged
            Unit originalTarget = findUnit(originalState.getUnits(), "u1_p2");
            assertEquals(10, originalTarget.getHp());
            assertTrue(originalTarget.isAlive());

            // Verify new state has damaged target
            Unit newTarget = findUnit(newState.getUnits(), "u1_p2");
            assertEquals(7, newTarget.getHp());

            // Verify different instances
            assertNotSame(originalState, newState);
        }
    }

    // ========== AMA-Series: MOVE_AND_ATTACK Apply Tests ==========

    @Nested
    @DisplayName("AMA-Series: MOVE_AND_ATTACK Apply Tests")
    class MoveAndAttackApplyTests {

        @Test
        @DisplayName("AMA1 - MOVE_AND_ATTACK Performs Move and Attack")
        void ama1_moveAndAttackPerformsBoth() {
            // u1_p1 at (1,1), moves to (1,2), attacks u1_p2 at (2,2)
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(2, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify attacker moved
            Unit attacker = findUnit(newState.getUnits(), "u1_p1");
            assertEquals(1, attacker.getPosition().getX());
            assertEquals(2, attacker.getPosition().getY());

            // Verify target damaged
            Unit target = findUnit(newState.getUnits(), "u1_p2");
            assertEquals(7, target.getHp()); // 10 - 3 = 7
            assertTrue(target.isAlive());

            // Verify turn switched (MOVE_AND_ATTACK switches turn)
            assertEquals("P2", newState.getCurrentPlayer().getValue());

            // Verify game not over
            assertFalse(newState.isGameOver());
            assertNull(newState.getWinner());
        }

        @Test
        @DisplayName("AMA2 - MOVE_AND_ATTACK That Kills Target But Not Last Enemy")
        void ama2_moveAndAttackKillsNotLastEnemy() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 5, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 4, 3, new Position(2, 2), true);
            Unit u2_p2 = new Unit("u2_p2", p2, 10, 3, new Position(4, 4), true); // Another P2 unit
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2, u2_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify target killed
            Unit target = findUnit(newState.getUnits(), "u1_p2");
            assertFalse(target.isAlive());

            // Verify other P2 unit still alive
            Unit otherEnemy = findUnit(newState.getUnits(), "u2_p2");
            assertTrue(otherEnemy.isAlive());

            // Verify game not over (P2 still has units)
            assertFalse(newState.isGameOver());
            assertNull(newState.getWinner());

            // Verify turn switched to P2
            assertEquals("P2", newState.getCurrentPlayer().getValue());
        }

        @Test
        @DisplayName("AMA3 - MOVE_AND_ATTACK That Kills Last Enemy Triggers Game Over")
        void ama3_moveAndAttackKillsLastEnemyGameOver() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 5, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 4, 3, new Position(2, 2), true); // Only P2 unit
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify target killed
            Unit target = findUnit(newState.getUnits(), "u1_p2");
            assertFalse(target.isAlive());

            // Verify game over with P1 as winner
            assertTrue(newState.isGameOver());
            assertNotNull(newState.getWinner());
            assertEquals("P1", newState.getWinner().getValue());

            // Turn still switches to P2 per V1 spec
            assertEquals("P2", newState.getCurrentPlayer().getValue());
        }

        @Test
        @DisplayName("AMA4 - MOVE_AND_ATTACK Does Not Mutate Original GameState")
        void ama4_moveAndAttackDoesNotMutateOriginalState() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(2, 2), true);
            GameState originalState = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(originalState, action);

            // Verify original state unchanged
            Unit originalAttacker = findUnit(originalState.getUnits(), "u1_p1");
            assertEquals(1, originalAttacker.getPosition().getX());
            assertEquals(1, originalAttacker.getPosition().getY());

            Unit originalTarget = findUnit(originalState.getUnits(), "u1_p2");
            assertEquals(10, originalTarget.getHp());

            assertEquals("P1", originalState.getCurrentPlayer().getValue());

            // Verify new state has changes
            Unit newAttacker = findUnit(newState.getUnits(), "u1_p1");
            assertEquals(1, newAttacker.getPosition().getX());
            assertEquals(2, newAttacker.getPosition().getY());

            Unit newTarget = findUnit(newState.getUnits(), "u1_p2");
            assertEquals(7, newTarget.getHp());

            assertEquals("P2", newState.getCurrentPlayer().getValue());

            // Verify different instances
            assertNotSame(originalState, newState);
        }
    }

    // ========== AET-Series: END_TURN Apply Tests ==========

    @Nested
    @DisplayName("AET-Series: END_TURN Apply Tests")
    class EndTurnApplyTests {

        @Test
        @DisplayName("AET1 - END_TURN Switches Current Player")
        void aet1_endTurnSwitchesPlayer() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(3, 3), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify player switched
            assertEquals("P2", newState.getCurrentPlayer().getValue());

            // Verify units unchanged
            Unit unit1 = findUnit(newState.getUnits(), "u1_p1");
            assertEquals(1, unit1.getPosition().getX());
            assertEquals(1, unit1.getPosition().getY());
            assertEquals(10, unit1.getHp());

            Unit unit2 = findUnit(newState.getUnits(), "u1_p2");
            assertEquals(3, unit2.getPosition().getX());
            assertEquals(3, unit2.getPosition().getY());
            assertEquals(10, unit2.getHp());

            // Verify game state unchanged
            assertFalse(newState.isGameOver());
            assertNull(newState.getWinner());
        }

        @Test
        @DisplayName("AET2 - END_TURN Is No-Op for Units and Board")
        void aet2_endTurnNoOpForUnits() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u2_p1 = new Unit("u2_p1", p1, 8, 4, new Position(2, 2), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 5, 2, new Position(3, 3), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u2_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify all units unchanged
            Unit unit1 = findUnit(newState.getUnits(), "u1_p1");
            assertEquals(1, unit1.getPosition().getX());
            assertEquals(1, unit1.getPosition().getY());
            assertEquals(10, unit1.getHp());
            assertEquals(3, unit1.getAttack());
            assertTrue(unit1.isAlive());

            Unit unit2 = findUnit(newState.getUnits(), "u2_p1");
            assertEquals(2, unit2.getPosition().getX());
            assertEquals(2, unit2.getPosition().getY());
            assertEquals(8, unit2.getHp());
            assertEquals(4, unit2.getAttack());
            assertTrue(unit2.isAlive());

            Unit unit3 = findUnit(newState.getUnits(), "u1_p2");
            assertEquals(3, unit3.getPosition().getX());
            assertEquals(3, unit3.getPosition().getY());
            assertEquals(5, unit3.getHp());
            assertEquals(2, unit3.getAttack());
            assertTrue(unit3.isAlive());

            // Verify board dimensions unchanged
            assertEquals(5, newState.getBoard().getWidth());
            assertEquals(5, newState.getBoard().getHeight());

            // Only currentPlayer changed
            assertEquals("P2", newState.getCurrentPlayer().getValue());
        }

        @Test
        @DisplayName("AET - END_TURN P2 to P1")
        void aet_endTurnP2ToP1() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(3, 3), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p2, false, null);

            Action action = new Action(ActionType.END_TURN, p2, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            // Verify player switched from P2 to P1
            assertEquals("P1", newState.getCurrentPlayer().getValue());
        }
    }

    // ========== AGO-Series: Game Over Consistency Tests ==========

    @Nested
    @DisplayName("AGO-Series: Game Over Consistency Tests")
    class GameOverConsistencyTests {

        @Test
        @DisplayName("AGO1 - No Winner When Both Sides Have Alive Units")
        void ago1_noWinnerBothSidesAlive() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            // Test with ATTACK that damages but doesn't kill
            Action attackAction = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");
            GameState afterAttack = ruleEngine.applyAction(state, attackAction);

            assertFalse(afterAttack.isGameOver());
            assertNull(afterAttack.getWinner());

            // Test with MOVE
            Unit u2_p1 = new Unit("u2_p1", p1, 10, 3, new Position(2, 2), true);
            GameState state2 = new GameState(board, Arrays.asList(u1_p1, u2_p1, u1_p2), p1, false, null);
            Action moveAction = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state2, moveAction);

            assertFalse(afterMove.isGameOver());
            assertNull(afterMove.getWinner());

            // Test with END_TURN
            Action endTurnAction = new Action(ActionType.END_TURN, p1, null, null);
            GameState afterEndTurn = ruleEngine.applyAction(state, endTurnAction);

            assertFalse(afterEndTurn.isGameOver());
            assertNull(afterEndTurn.getWinner());
        }

        @Test
        @DisplayName("AGO2 - Winner Detected When One Side Has No Alive Units")
        void ago2_winnerDetectedNoAliveUnits() {
            // P1 attacks and kills last P2 unit
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 10, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 5, 3, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");
            GameState newState = ruleEngine.applyAction(state, action);

            assertTrue(newState.isGameOver());
            assertNotNull(newState.getWinner());
            assertEquals("P1", newState.getWinner().getValue());
        }

        @Test
        @DisplayName("AGO - P2 Wins When All P1 Units Dead")
        void ago_p2WinsAllP1Dead() {
            // P2 attacks and kills last P1 unit
            Unit u1_p1 = new Unit("u1_p1", p1, 5, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 10, new Position(1, 2), true);
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p2, false, null);

            Action action = new Action(ActionType.ATTACK, p2, new Position(1, 1), "u1_p1");
            GameState newState = ruleEngine.applyAction(state, action);

            assertTrue(newState.isGameOver());
            assertNotNull(newState.getWinner());
            assertEquals("P2", newState.getWinner().getValue());
        }
    }

    // ========== IM-Series: Immutability Tests ==========

    @Nested
    @DisplayName("IM-Series: Immutability & Structural Integrity Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("IM1 - New GameState Instance Returned")
        void im1_newGameStateInstanceReturned() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(3, 3), true);
            GameState originalState = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            // Test MOVE
            Action moveAction = new Action(ActionType.MOVE, p1, new Position(1, 2), null);
            GameState afterMove = ruleEngine.applyAction(originalState, moveAction);
            assertNotSame(originalState, afterMove);

            // Test ATTACK
            Unit u2_p1 = new Unit("u2_p1", p1, 10, 3, new Position(2, 3), true);
            Unit u2_p2 = new Unit("u2_p2", p2, 10, 3, new Position(2, 4), true);
            GameState state2 = new GameState(board, Arrays.asList(u2_p1, u2_p2), p1, false, null);
            Action attackAction = new Action(ActionType.ATTACK, p1, new Position(2, 4), "u2_p2");
            GameState afterAttack = ruleEngine.applyAction(state2, attackAction);
            assertNotSame(state2, afterAttack);

            // Test END_TURN
            Action endTurnAction = new Action(ActionType.END_TURN, p1, null, null);
            GameState afterEndTurn = ruleEngine.applyAction(originalState, endTurnAction);
            assertNotSame(originalState, afterEndTurn);

            // Test MOVE_AND_ATTACK
            Unit u3_p1 = new Unit("u3_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u3_p2 = new Unit("u3_p2", p2, 10, 3, new Position(2, 2), true);
            GameState state3 = new GameState(board, Arrays.asList(u3_p1, u3_p2), p1, false, null);
            Action maAction = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u3_p2");
            GameState afterMA = ruleEngine.applyAction(state3, maAction);
            assertNotSame(state3, afterMA);
        }

        @Test
        @DisplayName("IM2 - Units List Copied, Not Mutated In-Place")
        void im2_unitsListCopied() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(1, 2), true);
            List<Unit> originalUnits = Arrays.asList(u1_p1, u1_p2);
            GameState originalState = new GameState(board, originalUnits, p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");
            GameState newState = ruleEngine.applyAction(originalState, action);

            // Verify units lists are different references
            List<Unit> oldUnits = originalState.getUnits();
            List<Unit> newUnits = newState.getUnits();
            assertNotSame(oldUnits, newUnits);

            // Verify changed unit has new instance
            Unit oldTarget = findUnit(oldUnits, "u1_p2");
            Unit newTarget = findUnit(newUnits, "u1_p2");

            // Old target should have original HP
            assertEquals(10, oldTarget.getHp());
            // New target should have reduced HP
            assertEquals(7, newTarget.getHp());
        }

        @Test
        @DisplayName("IM3 - Board Is Immutable")
        void im3_boardIsImmutable() {
            Unit u1_p1 = new Unit("u1_p1", p1, 10, 3, new Position(1, 1), true);
            Unit u1_p2 = new Unit("u1_p2", p2, 10, 3, new Position(3, 3), true);
            GameState originalState = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);
            GameState newState = ruleEngine.applyAction(originalState, action);

            // Verify board dimensions are consistent
            assertEquals(originalState.getBoard().getWidth(), newState.getBoard().getWidth());
            assertEquals(originalState.getBoard().getHeight(), newState.getBoard().getHeight());
            assertEquals(5, newState.getBoard().getWidth());
            assertEquals(5, newState.getBoard().getHeight());

            // Board can be shared (same reference) since it's immutable
            // This is implementation-dependent but dimensions must be correct
        }
    }
}
