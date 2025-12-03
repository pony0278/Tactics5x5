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
 * V3 SPEED Buff Tests (BSP-Series from BUFF_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for SPEED buff mechanics:
 * - Grants 2 actions per turn
 * - -1 ATK modifier
 * - Actions reset each round
 */
@DisplayName("V3 SPEED Buff Tests")
class RuleEngineSpeedBuffTest {

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

    private GameState createStateWithSpeedBuff(Unit unit, List<Unit> allUnits) {
        Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
        BuffInstance speedBuff = BuffFactory.createSpeed("test_source");
        unitBuffs.put(unit.getId(), Collections.singletonList(speedBuff));
        return new GameState(board, allUnits, p1, false, null, unitBuffs);
    }

    private GameState createStateWithoutBuffs(List<Unit> allUnits) {
        return new GameState(board, allUnits, p1, false, null, Collections.emptyMap());
    }

    // ========== BSP-Series: SPEED Buff Tests ==========

    @Nested
    @DisplayName("BSP-Series: SPEED Buff Validation")
    class SpeedBuffValidation {

        @Test
        @DisplayName("BSP1: SPEED buff grants -1 ATK")
        void speedBuffGrantsMinusOneAttack() {
            // Given: Unit with base ATK=3 and SPEED buff (-1 ATK)
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);

            GameState state = createStateWithSpeedBuff(attacker, Arrays.asList(attacker, target));

            // When: Attack is applied
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState newState = ruleEngine.applyAction(state, attack);

            // Then: Damage should be base (3) - 1 = 2
            Unit updatedTarget = newState.getUnits().stream()
                .filter(u -> u.getId().equals("p2_unit"))
                .findFirst().orElseThrow();
            assertEquals(8, updatedTarget.getHp(), "Target HP should be 10 - 2 = 8 (SPEED buff reduces ATK by 1)");
        }

        @Test
        @DisplayName("BSP2: SPEED buff allows 2 actions per turn")
        void speedBuffAllowsTwoActions() {
            // Given: Unit with SPEED buff at position (2,2)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 4), 10, 3);

            GameState state = createStateWithSpeedBuff(unit, Arrays.asList(unit, target));

            // When: First action (MOVE)
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            ValidationResult moveResult = ruleEngine.validateAction(state, move);
            assertTrue(moveResult.isValid(), "First action should be valid");

            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Second action should also be valid
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 4), "p2_unit");
            ValidationResult attackResult = ruleEngine.validateAction(afterMove, attack);
            assertTrue(attackResult.isValid(), "Second action should be valid with SPEED buff");
        }

        @Test
        @DisplayName("BSP3: Without SPEED buff, second action is invalid")
        void withoutSpeedBuffSecondActionInvalid() {
            // Given: Unit WITHOUT SPEED buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 4), 10, 3);

            GameState state = createStateWithoutBuffs(Arrays.asList(unit, target));

            // When: First action (MOVE)
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Second action should be INVALID
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 4), "p2_unit");
            ValidationResult attackResult = ruleEngine.validateAction(afterMove, attack);
            assertFalse(attackResult.isValid(), "Second action should be invalid without SPEED buff");
            assertTrue(attackResult.getErrorMessage().contains("no remaining actions")
                    || attackResult.getErrorMessage().contains("already acted")
                    || attackResult.getErrorMessage().contains("actions"),
                "Error message should mention action limit");
        }

        @Test
        @DisplayName("BSP4: SPEED buff - third action is invalid")
        void speedBuffThirdActionInvalid() {
            // Given: Unit with SPEED buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 4), 20, 3);

            GameState state = createStateWithSpeedBuff(unit, Arrays.asList(unit, target));

            // When: Two actions executed
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 4), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(afterMove, attack);

            // Then: Third action should be INVALID
            Action secondAttack = new Action(ActionType.ATTACK, p1, new Position(2, 4), "p2_unit");
            ValidationResult result = ruleEngine.validateAction(afterAttack, secondAttack);
            assertFalse(result.isValid(), "Third action should be invalid even with SPEED buff");
        }

        @Test
        @DisplayName("BSP5: SPEED buff actions reset after round end")
        void speedBuffActionsResetAfterRound() {
            // Given: Unit with SPEED buff that has used 2 actions
            Unit p1Unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(2, 4), 20, 3);

            GameState state = createStateWithSpeedBuff(p1Unit, Arrays.asList(p1Unit, p2Unit));

            // Use both actions
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 4), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(afterMove, attack);

            // P1 ends turn
            Action endTurnP1 = new Action(ActionType.END_TURN, p1, null, null);
            GameState afterP1End = ruleEngine.applyAction(afterAttack, endTurnP1);

            // P2 ends turn (triggers round end)
            Action endTurnP2 = new Action(ActionType.END_TURN, p2, null, null);
            GameState afterRoundEnd = ruleEngine.applyAction(afterP1End, endTurnP2);

            // Then: P1 unit should be able to act again (actions reset)
            // Find the unit in new state
            Unit p1UnitAfterRound = afterRoundEnd.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();

            assertEquals(0, p1UnitAfterRound.getActionsUsed(), "Actions should reset to 0 after round end");
        }
    }

    @Nested
    @DisplayName("BSP-Series: SPEED Buff Action Tracking")
    class SpeedBuffActionTracking {

        @Test
        @DisplayName("BSP6: First action increments actionsUsed to 1")
        void firstActionIncrementsActionsUsed() {
            // Given: Unit with SPEED buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            GameState state = createStateWithSpeedBuff(unit, Collections.singletonList(unit));

            // When: First action (MOVE)
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: actionsUsed should be 1
            Unit updatedUnit = afterMove.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();

            assertEquals(1, updatedUnit.getActionsUsed(), "actionsUsed should be 1 after first action");
        }

        @Test
        @DisplayName("BSP7: Second action increments actionsUsed to 2")
        void secondActionIncrementsActionsUsed() {
            // Given: Unit with SPEED buff after first action
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 4), 10, 3);
            GameState state = createStateWithSpeedBuff(unit, Arrays.asList(unit, target));

            // First action
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // When: Second action
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 4), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(afterMove, attack);

            // Then: actionsUsed should be 2
            Unit updatedUnit = afterAttack.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();

            assertEquals(2, updatedUnit.getActionsUsed(), "actionsUsed should be 2 after second action");
        }
    }
}
