package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BV-Series: Buff Effects on Validation Tests (from BUFF_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for how buffs affect action validation:
 * - POWER: blocks MOVE_AND_ATTACK, allows MOVE/ATTACK
 * - SPEED: second action valid, third action invalid
 * - SLOW: preparing unit cannot declare new action
 * - WEAKNESS/BLEED/LIFE: do not block any action types
 */
@DisplayName("BV-Series: Buff Effects on Validation")
public class RuleEngineBuffValidationTest {

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
        return new Unit(id, owner, 10, 3, 2, 1, pos, true);
    }

    private Unit createUnit(String id, PlayerId owner, Position pos, int moveRange) {
        return new Unit(id, owner, 10, 3, moveRange, 1, pos, true);
    }

    private GameState createStateWithBuffs(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        return new GameState(board, units, p1, false, null, unitBuffs,
            new ArrayList<>(), new ArrayList<>(), 1, null, false, false);
    }

    private Unit findUnit(GameState state, String id) {
        return state.getUnits().stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    // ========== BV-Series Tests ==========

    @Nested
    @DisplayName("BV2-4: POWER buff action restrictions")
    class PowerBuffActionRestrictionsTests {

        @Test
        @DisplayName("BV2: POWER buff - MOVE_AND_ATTACK is INVALID")
        void powerBuffBlocksMoveAndAttack() {
            // Given: Unit with POWER buff
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // When: Try MOVE_AND_ATTACK
            Action moveAndAttack = new Action(ActionType.MOVE_AND_ATTACK, p1,
                new Position(2, 3), "p2_unit");
            ValidationResult result = ruleEngine.validateAction(state, moveAndAttack);

            // Then: Should be invalid
            assertFalse(result.isValid(),
                "MOVE_AND_ATTACK should be blocked for unit with POWER buff");
        }

        @Test
        @DisplayName("BV3: POWER buff - MOVE is still valid")
        void powerBuffAllowsMove() {
            // Given: Unit with POWER buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: Try MOVE
            Action move = Action.move("p1_unit", new Position(2, 3));
            ValidationResult result = ruleEngine.validateAction(state, move);

            // Then: Should be valid
            assertTrue(result.isValid(),
                "MOVE should be allowed for unit with POWER buff");
        }

        @Test
        @DisplayName("BV4: POWER buff - ATTACK is still valid")
        void powerBuffAllowsAttack() {
            // Given: Unit with POWER buff adjacent to enemy
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3));

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // When: Try ATTACK
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            ValidationResult result = ruleEngine.validateAction(state, attack);

            // Then: Should be valid
            assertTrue(result.isValid(),
                "ATTACK should be allowed for unit with POWER buff");
        }
    }

    @Nested
    @DisplayName("BV8-10: SPEED buff action limits")
    class SpeedBuffActionLimitsTests {

        @Test
        @DisplayName("BV8: SPEED buff - Second action is valid")
        void speedBuffSecondActionValid() {
            // Given: Unit with SPEED buff has acted once
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance speedBuff = BuffFactory.createSpeed("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(speedBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, p2Unit), unitBuffs);

            // First action
            Action move1 = Action.move("p1_unit", new Position(2, 3));
            GameState afterFirst = ruleEngine.applyAction(state, move1);

            // When: Try second action
            Action move2 = Action.move("p1_unit", new Position(2, 4));
            ValidationResult result = ruleEngine.validateAction(afterFirst, move2);

            // Then: Should be valid
            assertTrue(result.isValid(),
                "Second action should be valid for unit with SPEED buff");
        }

        @Test
        @DisplayName("BV9: Without SPEED buff - Second action is INVALID")
        void noSpeedBuffSecondActionInvalid() {
            // Given: Unit WITHOUT SPEED buff has acted once
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            // No buffs
            GameState state = createStateWithBuffs(Arrays.asList(unit, p2Unit), Collections.emptyMap());

            // First action
            Action move1 = Action.move("p1_unit", new Position(2, 3));
            GameState afterFirst = ruleEngine.applyAction(state, move1);

            // When: Try second action
            Action move2 = Action.move("p1_unit", new Position(2, 4));
            ValidationResult result = ruleEngine.validateAction(afterFirst, move2);

            // Then: Should be invalid
            assertFalse(result.isValid(),
                "Second action should be invalid for unit without SPEED buff");
        }

        @Test
        @DisplayName("BV10: SPEED buff - Third action is INVALID")
        void speedBuffThirdActionInvalid() {
            // Given: Unit with SPEED buff has acted twice
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance speedBuff = BuffFactory.createSpeed("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(speedBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, p2Unit), unitBuffs);

            // First action
            Action move1 = Action.move("p1_unit", new Position(2, 3));
            GameState afterFirst = ruleEngine.applyAction(state, move1);

            // Second action
            Action move2 = Action.move("p1_unit", new Position(2, 4));
            GameState afterSecond = ruleEngine.applyAction(afterFirst, move2);

            // When: Try third action
            Action move3 = Action.move("p1_unit", new Position(3, 4));
            ValidationResult result = ruleEngine.validateAction(afterSecond, move3);

            // Then: Should be invalid
            assertFalse(result.isValid(),
                "Third action should be invalid even with SPEED buff");
        }
    }

    @Nested
    @DisplayName("BV12: SLOW buff - Preparing unit cannot declare new action")
    class SlowBuffPreparingRestrictionsTests {

        @Test
        @DisplayName("BV12: Preparing unit cannot declare new action")
        void preparingUnitCannotDeclareNewAction() {
            // Given: Unit with SLOW buff is preparing
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3));

            BuffInstance slowBuff = BuffFactory.createSlow("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(slowBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, target), unitBuffs);

            // Declare attack (enters preparing)
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterDeclare = ruleEngine.applyAction(state, attack);

            // Verify unit is preparing
            Unit preparingUnit = findUnit(afterDeclare, "p1_unit");
            assertTrue(preparingUnit.isPreparing(), "Unit should be in preparing state");

            // When: Try to declare another action
            Action move = Action.move("p1_unit", new Position(3, 2));
            ValidationResult result = ruleEngine.validateAction(afterDeclare, move);

            // Then: Should be invalid
            assertFalse(result.isValid(),
                "Preparing unit should not be able to declare new action");
        }
    }

    @Nested
    @DisplayName("BV15-17: Non-blocking buffs")
    class NonBlockingBuffsTests {

        @Test
        @DisplayName("BV15: WEAKNESS buff does not block any action types")
        void weaknessBuffDoesNotBlockActions() {
            // Given: Unit with WEAKNESS buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 4));

            BuffInstance weaknessBuff = BuffFactory.createWeakness("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(weaknessBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, target), unitBuffs);

            // Check MOVE is valid
            Action move = Action.move("p1_unit", new Position(2, 3));
            assertTrue(ruleEngine.validateAction(state, move).isValid(),
                "WEAKNESS buff should not block MOVE");

            // Check MOVE_AND_ATTACK is valid (unlike POWER)
            Action moveAndAttack = new Action(ActionType.MOVE_AND_ATTACK, p1,
                new Position(2, 3), "p2_unit");
            assertTrue(ruleEngine.validateAction(state, moveAndAttack).isValid(),
                "WEAKNESS buff should not block MOVE_AND_ATTACK");
        }

        @Test
        @DisplayName("BV16: BLEED buff does not block any action types")
        void bleedBuffDoesNotBlockActions() {
            // Given: Unit with BLEED buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3));

            BuffInstance bleedBuff = BuffFactory.createBleed("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(bleedBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, target), unitBuffs);

            // Check all action types are valid
            Action move = Action.move("p1_unit", new Position(3, 2));
            assertTrue(ruleEngine.validateAction(state, move).isValid(),
                "BLEED buff should not block MOVE");

            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            assertTrue(ruleEngine.validateAction(state, attack).isValid(),
                "BLEED buff should not block ATTACK");
        }

        @Test
        @DisplayName("BV17: LIFE buff does not block any action types")
        void lifeBuffDoesNotBlockActions() {
            // Given: Unit with LIFE buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3));

            BuffInstance lifeBuff = BuffFactory.createLife("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(lifeBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, target), unitBuffs);

            // Check all action types are valid
            Action move = Action.move("p1_unit", new Position(3, 2));
            assertTrue(ruleEngine.validateAction(state, move).isValid(),
                "LIFE buff should not block MOVE");

            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            assertTrue(ruleEngine.validateAction(state, attack).isValid(),
                "LIFE buff should not block ATTACK");
        }
    }
}
