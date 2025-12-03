package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V3 SLOW Buff Tests (BSL-Series from BUFF_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for SLOW buff mechanics:
 * - Actions are delayed by 1 round (enter "preparing" state)
 * - Preparing actions execute at start of next round
 * - Attack misses if target moves before execution
 */
@DisplayName("V3 SLOW Buff Tests")
class RuleEngineSlowBuffTest {

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

    private Unit createUnit(String id, PlayerId owner, Position pos, int hp, int attack) {
        return new Unit(id, owner, hp, attack, 1, 1, pos, true);
    }

    private GameState createStateWithSlowBuff(Unit unit, List<Unit> allUnits) {
        Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
        BuffInstance slowBuff = BuffFactory.createSlow("test_source");
        unitBuffs.put(unit.getId(), Collections.singletonList(slowBuff));
        return new GameState(board, allUnits, p1, false, null, unitBuffs);
    }

    private GameState completeRound(GameState state) {
        // P1 ends turn
        Action endTurnP1 = new Action(ActionType.END_TURN, p1, null, null);
        GameState afterP1End = ruleEngine.applyAction(state, endTurnP1);

        // P2 ends turn (triggers round end and preparing action execution)
        Action endTurnP2 = new Action(ActionType.END_TURN, p2, null, null);
        return ruleEngine.applyAction(afterP1End, endTurnP2);
    }

    // ========== BSL-Series: SLOW Buff Tests ==========

    @Nested
    @DisplayName("BSL-Series: SLOW Buff Preparing State")
    class SlowBuffPreparingState {

        @Test
        @DisplayName("BSL1: SLOW buff - declaring action enters preparing state")
        void slowBuffEntersPreparingState() {
            // Given: Unit with SLOW buff
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);

            GameState state = createStateWithSlowBuff(attacker, Arrays.asList(attacker, target));

            // When: Attack action is declared
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            ValidationResult result = ruleEngine.validateAction(state, attack);
            assertTrue(result.isValid(), "Action should be valid to declare");

            GameState afterAction = ruleEngine.applyAction(state, attack);

            // Then: Unit should be in preparing state
            Unit updatedUnit = afterAction.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();

            assertTrue(updatedUnit.isPreparing(), "Unit should be in preparing state");
            assertNotNull(updatedUnit.getPreparingAction(), "Preparing action should be stored");
        }

        @Test
        @DisplayName("BSL2: SLOW buff - action does NOT execute immediately")
        void slowBuffActionNotImmediate() {
            // Given: Unit with SLOW buff
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);

            GameState state = createStateWithSlowBuff(attacker, Arrays.asList(attacker, target));

            // When: Attack action is declared
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterAction = ruleEngine.applyAction(state, attack);

            // Then: Target should NOT have taken damage yet
            Unit updatedTarget = afterAction.getUnits().stream()
                .filter(u -> u.getId().equals("p2_unit"))
                .findFirst().orElseThrow();

            assertEquals(10, updatedTarget.getHp(), "Target HP should be unchanged (action delayed)");
        }

        @Test
        @DisplayName("BSL3: SLOW buff - preparing MOVE executes at round end")
        void slowBuffMoveExecutesAtRoundEnd() {
            // Given: Unit with SLOW buff prepares to MOVE
            Unit mover = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4), 10, 3);

            GameState state = createStateWithSlowBuff(mover, Arrays.asList(mover, p2Unit));

            // When: MOVE action is declared
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Unit is still at original position
            Unit unitBeforeRound = afterMove.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();
            assertEquals(new Position(2, 2), unitBeforeRound.getPosition(), "Unit should not move yet");

            // When: Round completes
            GameState afterRound = completeRound(afterMove);

            // Then: Unit should have moved
            Unit unitAfterRound = afterRound.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();
            assertEquals(new Position(2, 3), unitAfterRound.getPosition(), "Unit should have moved after round end");
        }

        @Test
        @DisplayName("BSL4: SLOW buff - preparing ATTACK executes at round end")
        void slowBuffAttackExecutesAtRoundEnd() {
            // Given: Unit with SLOW buff prepares to ATTACK
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);

            GameState state = createStateWithSlowBuff(attacker, Arrays.asList(attacker, target));

            // When: ATTACK action is declared
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // When: Round completes
            GameState afterRound = completeRound(afterAttack);

            // Then: Target should have taken damage (base ATK = 3)
            Unit targetAfterRound = afterRound.getUnits().stream()
                .filter(u -> u.getId().equals("p2_unit"))
                .findFirst().orElseThrow();
            assertEquals(7, targetAfterRound.getHp(), "Target should have taken 3 damage after round end");
        }

        @Test
        @DisplayName("BSL5: SLOW buff - preparing state clears after execution")
        void slowBuffPreparingStateClearsAfterExecution() {
            // Given: Unit with SLOW buff prepares an action
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4), 10, 3);

            GameState state = createStateWithSlowBuff(unit, Arrays.asList(unit, p2Unit));

            // Declare action
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // When: Round completes
            GameState afterRound = completeRound(afterMove);

            // Then: Preparing state should be cleared
            Unit unitAfterRound = afterRound.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();

            assertFalse(unitAfterRound.isPreparing(), "Preparing state should be cleared");
            assertNull(unitAfterRound.getPreparingAction(), "Preparing action should be null");
        }
    }

    @Nested
    @DisplayName("BSL-Series: SLOW Buff Attack Miss on Target Move")
    class SlowBuffAttackMiss {

        @Test
        @DisplayName("BSL10: SLOW buff - attack misses if target moved")
        void slowBuffAttackMissesIfTargetMoved() {
            // Given: P1 unit with SLOW buff prepares to attack P2 unit at (2,3)
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);

            GameState state = createStateWithSlowBuff(attacker, Arrays.asList(attacker, target));

            // P1 declares attack on target at (2,3)
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // P1 ends turn
            Action endTurnP1 = new Action(ActionType.END_TURN, p1, null, null);
            GameState afterP1End = ruleEngine.applyAction(afterAttack, endTurnP1);

            // P2 moves target away from (2,3) to (2,4)
            Action targetMove = new Action(ActionType.MOVE, p2, new Position(2, 4), null);
            GameState afterTargetMove = ruleEngine.applyAction(afterP1End, targetMove);

            // P2 ends turn (triggers round end)
            Action endTurnP2 = new Action(ActionType.END_TURN, p2, null, null);
            GameState afterRound = ruleEngine.applyAction(afterTargetMove, endTurnP2);

            // Then: Target should NOT have taken damage (attack missed)
            Unit targetAfterRound = afterRound.getUnits().stream()
                .filter(u -> u.getId().equals("p2_unit"))
                .findFirst().orElseThrow();

            assertEquals(10, targetAfterRound.getHp(),
                "Target HP should be unchanged (attack missed because target moved)");
        }

        @Test
        @DisplayName("BSL11: SLOW buff - attack hits if target stays in place")
        void slowBuffAttackHitsIfTargetStays() {
            // Given: P1 unit with SLOW buff prepares to attack P2 unit at (2,3)
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);

            GameState state = createStateWithSlowBuff(attacker, Arrays.asList(attacker, target));

            // P1 declares attack
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // P1 ends turn
            Action endTurnP1 = new Action(ActionType.END_TURN, p1, null, null);
            GameState afterP1End = ruleEngine.applyAction(afterAttack, endTurnP1);

            // P2 does NOT move, just ends turn
            Action endTurnP2 = new Action(ActionType.END_TURN, p2, null, null);
            GameState afterRound = ruleEngine.applyAction(afterP1End, endTurnP2);

            // Then: Target SHOULD have taken damage
            Unit targetAfterRound = afterRound.getUnits().stream()
                .filter(u -> u.getId().equals("p2_unit"))
                .findFirst().orElseThrow();

            assertEquals(7, targetAfterRound.getHp(),
                "Target should have taken 3 damage (attack hit because target didn't move)");
        }
    }
}
