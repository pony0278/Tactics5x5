package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFlags;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffModifier;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for Buff System behavior in RuleEngine.
 * Based on BUFF_SYSTEM_V1_TESTPLAN.md
 *
 * NOTE: At this stage, RuleEngine does NOT yet implement buff-aware logic.
 * Many of these tests will fail initially. This is EXPECTED and represents
 * the test-driven development approach.
 *
 * Test Series:
 * - BV-Series: Buff Validation Tests (stunned, rooted, range modifiers)
 * - BA-Series: Buff ApplyAction Tests (bonusAttack, poison damage)
 * - BP-Series: Poison & Turn End Tests
 * - BC-Series: Backward Compatibility Tests
 * - BD-Series: Determinism Tests
 */
class RuleEngineBuffFlowTest {

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

    /**
     * Create a standard unit with default stats.
     */
    private Unit createUnit(String id, PlayerId owner, Position pos) {
        return new Unit(id, owner, 10, 3, 1, 1, pos, true);
    }

    /**
     * Create a unit with custom stats.
     */
    private Unit createUnit(String id, PlayerId owner, int hp, int attack, int moveRange, int attackRange, Position pos, boolean alive) {
        return new Unit(id, owner, hp, attack, moveRange, attackRange, pos, alive);
    }

    /**
     * Create a GameState with buffs applied to specific units.
     */
    private GameState withBuffs(Board board, List<Unit> units, PlayerId currentPlayer,
                                 boolean isGameOver, PlayerId winner,
                                 Map<String, List<BuffInstance>> unitBuffs) {
        return new GameState(board, units, currentPlayer, isGameOver, winner, unitBuffs);
    }

    /**
     * Helper to create a buff instance.
     */
    private BuffInstance buffInstance(String buffId, String sourceUnitId, int duration, boolean stackable,
                                       BuffModifier modifiers, BuffFlags flags) {
        return new BuffInstance(buffId, sourceUnitId, duration, stackable, modifiers, flags);
    }

    /**
     * Create neutral modifiers (no changes).
     */
    private BuffModifier neutralModifiers() {
        return new BuffModifier(0, 0, 0, 0);
    }

    /**
     * Create neutral flags (no restrictions).
     */
    private BuffFlags neutralFlags() {
        return new BuffFlags(false, false, false, false, false);
    }

    /**
     * Create a RAGE buff (+2 attack).
     */
    private BuffInstance createRageBuff(String sourceUnitId, int duration) {
        BuffModifier modifiers = new BuffModifier(0, 2, 0, 0);
        BuffFlags flags = neutralFlags();
        return buffInstance("RAGE", sourceUnitId, duration, false, modifiers, flags);
    }

    /**
     * Create a HASTE buff (+1 moveRange).
     */
    private BuffInstance createHasteBuff(String sourceUnitId, int duration) {
        BuffModifier modifiers = new BuffModifier(0, 0, 1, 0);
        BuffFlags flags = neutralFlags();
        return buffInstance("HASTE", sourceUnitId, duration, false, modifiers, flags);
    }

    /**
     * Create a POISON buff (-1 HP per turn).
     */
    private BuffInstance createPoisonBuff(String sourceUnitId, int duration) {
        BuffModifier modifiers = neutralModifiers();
        BuffFlags flags = new BuffFlags(false, false, true, false, false);
        return buffInstance("POISON", sourceUnitId, duration, true, modifiers, flags);
    }

    /**
     * Create a STUN buff (cannot act).
     */
    private BuffInstance createStunBuff(String sourceUnitId, int duration) {
        BuffModifier modifiers = neutralModifiers();
        BuffFlags flags = new BuffFlags(true, false, false, false, false);
        return buffInstance("STUN", sourceUnitId, duration, false, modifiers, flags);
    }

    /**
     * Create a ROOT buff (cannot move).
     */
    private BuffInstance createRootBuff(String sourceUnitId, int duration) {
        BuffModifier modifiers = neutralModifiers();
        BuffFlags flags = new BuffFlags(false, true, false, false, false);
        return buffInstance("ROOT", sourceUnitId, duration, false, modifiers, flags);
    }

    /**
     * Create a buff with bonus attack range.
     */
    private BuffInstance createBonusAttackRangeBuff(String sourceUnitId, int duration, int bonusRange) {
        BuffModifier modifiers = new BuffModifier(0, 0, 0, bonusRange);
        BuffFlags flags = neutralFlags();
        return buffInstance("RANGE_BOOST", sourceUnitId, duration, true, modifiers, flags);
    }

    /**
     * Create a buff with bonus move range.
     */
    private BuffInstance createBonusMoveRangeBuff(String sourceUnitId, int duration, int bonusRange) {
        BuffModifier modifiers = new BuffModifier(0, 0, bonusRange, 0);
        BuffFlags flags = neutralFlags();
        return buffInstance("SPEED_BOOST", sourceUnitId, duration, true, modifiers, flags);
    }

    /**
     * Helper to add buff to a unit in the buffs map.
     */
    private Map<String, List<BuffInstance>> addBuff(Map<String, List<BuffInstance>> buffs,
                                                     String unitId, BuffInstance buff) {
        Map<String, List<BuffInstance>> result = new HashMap<>(buffs);
        List<BuffInstance> unitBuffList = new ArrayList<>(result.getOrDefault(unitId, new ArrayList<>()));
        unitBuffList.add(buff);
        result.put(unitId, unitBuffList);
        return result;
    }

    // ========== BV-Series: Buff Validation Tests ==========

    @Nested
    @DisplayName("BV-Series - Buff Validation Tests")
    class BuffValidationTests {

        @Test
        @DisplayName("BV1 - Stunned units cannot MOVE")
        void bv1_stunnedUnitCannotMove() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(3, 3));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createStunBuff("u1_p2", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Stunned unit should not be able to MOVE");
            assertEquals("Unit is stunned", result.getErrorMessage());
        }

        @Test
        @DisplayName("BV2 - Stunned units cannot ATTACK")
        void bv2_stunnedUnitCannotAttack() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(1, 2));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createStunBuff("u1_p2", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Stunned unit should not be able to ATTACK");
            assertEquals("Unit is stunned", result.getErrorMessage());
        }

        @Test
        @DisplayName("BV3 - Stunned units cannot MOVE_AND_ATTACK")
        void bv3_stunnedUnitCannotMoveAndAttack() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(2, 2));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createStunBuff("u1_p2", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Stunned unit should not be able to MOVE_AND_ATTACK");
            assertEquals("Unit is stunned", result.getErrorMessage());
        }

        @Test
        @DisplayName("BV4 - Stunned units CAN END_TURN")
        void bv4_stunnedUnitCanEndTurn() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(3, 3));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createStunBuff("u1_p2", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "Stunned unit should be able to END_TURN");
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("BV5 - Rooted units cannot MOVE")
        void bv5_rootedUnitCannotMove() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(3, 3));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createRootBuff("u1_p2", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.MOVE, p1, new Position(1, 2), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Rooted unit should not be able to MOVE");
            assertEquals("Unit is rooted", result.getErrorMessage());
        }

        @Test
        @DisplayName("BV6 - Rooted units cannot MOVE_AND_ATTACK")
        void bv6_rootedUnitCannotMoveAndAttack() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(2, 2));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createRootBuff("u1_p2", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertFalse(result.isValid(), "Rooted unit should not be able to MOVE_AND_ATTACK");
            assertEquals("Unit is rooted", result.getErrorMessage());
        }

        @Test
        @DisplayName("BV7 - Rooted units CAN ATTACK if target in range")
        void bv7_rootedUnitCanAttackIfInRange() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(1, 2));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createRootBuff("u1_p2", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "Rooted unit should be able to ATTACK if target in range");
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("BV8 - bonusMoveRange increases allowed movement range")
        void bv8_bonusMoveRangeIncreasesMovement() {
            // Unit with base moveRange=1, buff adds +1, so can move 2 tiles
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(4, 4));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createHasteBuff("u1_p1", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            // Move 2 tiles (normally would be invalid with moveRange=1)
            Action action = new Action(ActionType.MOVE, p1, new Position(1, 3), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "Unit with +1 moveRange buff should move 2 tiles");
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("BV9 - bonusAttackRange increases ATTACK range")
        void bv9_bonusAttackRangeIncreasesRange() {
            // Unit with base attackRange=1, buff adds +1, so can attack 2 tiles away
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(1, 3));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createBonusAttackRangeBuff("u1_p1", 2, 1));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 3), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "Unit with +1 attackRange buff should attack 2 tiles away");
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("BV10 - Poison does NOT prevent any actions")
        void bv10_poisonDoesNotPreventActions() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(1, 2));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 3));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);

            // Poisoned unit should still be able to ATTACK
            Action attackAction = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");
            ValidationResult attackResult = ruleEngine.validateAction(state, attackAction);
            assertTrue(attackResult.isValid(), "Poisoned unit should be able to ATTACK");
        }

        @Test
        @DisplayName("BV11 - Multiple bonusMoveRange buffs are additive")
        void bv11_multipleMoveRangeBuffsAdditive() {
            // Unit with base moveRange=1, two buffs add +1 each = moveRange 3
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(4, 4));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createBonusMoveRangeBuff("u1_p1", 2, 1));
            buffs = addBuff(buffs, "u1_p1", createBonusMoveRangeBuff("u1_p2", 2, 1));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            // Move 3 tiles
            Action action = new Action(ActionType.MOVE, p1, new Position(1, 4), null);

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "Multiple +1 moveRange buffs should stack additively");
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("BV12 - Multiple bonusAttackRange buffs are additive")
        void bv12_multipleAttackRangeBuffsAdditive() {
            // Unit with base attackRange=1, two buffs add +1 each = attackRange 3
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(1, 4));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createBonusAttackRangeBuff("u1_p1", 2, 1));
            buffs = addBuff(buffs, "u1_p1", createBonusAttackRangeBuff("u1_p2", 2, 1));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            // Attack 3 tiles away
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 4), "u1_p2");

            ValidationResult result = ruleEngine.validateAction(state, action);

            assertTrue(result.isValid(), "Multiple +1 attackRange buffs should stack additively");
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("BV13 - Validation reflects buff-modified stats without altering base stats")
        void bv13_validationReflectsBuffsNotBaseStats() {
            // After validation, the unit's base stats in GameState should be unchanged
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(4, 4));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createHasteBuff("u1_p1", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.MOVE, p1, new Position(1, 3), null);

            ruleEngine.validateAction(state, action);

            // Verify base stats are unchanged
            Unit unitAfter = state.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p1"))
                .findFirst().orElseThrow();
            assertEquals(1, unitAfter.getMoveRange(), "Base moveRange should remain unchanged");
            assertEquals(1, unitAfter.getAttackRange(), "Base attackRange should remain unchanged");
        }
    }

    // ========== BA-Series: Buff ApplyAction Tests ==========

    @Nested
    @DisplayName("BA-Series - Buff ApplyAction Tests")
    class BuffApplyTests {

        @Test
        @DisplayName("BA1 - bonusAttack increases ATTACK damage")
        void ba1_bonusAttackIncreasesDamage() {
            // Unit with attack=3, buff adds +2 = 5 damage
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 2), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createRageBuff("u1_p1", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(state, action);

            Unit targetAfter = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p2"))
                .findFirst().orElseThrow();

            // 10 HP - 5 damage (3 base + 2 rage) = 5 HP
            assertEquals(5, targetAfter.getHp(), "Target should take 5 damage (3 base + 2 rage)");
        }

        @Test
        @DisplayName("BA2 - RAGE buff (+2 ATK) applied correctly in combat")
        void ba2_rageBuffAppliedCorrectly() {
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 2), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createRageBuff("u1_p1", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(state, action);

            Unit targetAfter = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p2"))
                .findFirst().orElseThrow();

            assertEquals(5, targetAfter.getHp(), "RAGE buff should add +2 to attack damage");
        }

        @Test
        @DisplayName("BA3 - bonusMoveRange does NOT allow moving beyond modified range")
        void ba3_bonusMoveRangeRespectedInApply() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(4, 4));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createHasteBuff("u1_p1", 2)); // +1 moveRange

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            // Valid move with buff (distance 2)
            Action validAction = new Action(ActionType.MOVE, p1, new Position(1, 3), null);

            GameState newState = ruleEngine.applyAction(state, validAction);

            Unit movedUnit = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p1"))
                .findFirst().orElseThrow();

            assertEquals(new Position(1, 3), movedUnit.getPosition(), "Unit should move to buff-modified position");
        }

        @Test
        @DisplayName("BA4 - bonusAttackRange does NOT affect apply beyond validation")
        void ba4_bonusAttackRangeInApply() {
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 3), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createBonusAttackRangeBuff("u1_p1", 2, 1)); // +1 attackRange

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 3), "u1_p2");

            GameState newState = ruleEngine.applyAction(state, action);

            Unit targetAfter = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p2"))
                .findFirst().orElseThrow();

            // Damage should be base attack only (no range bonus to damage)
            assertEquals(7, targetAfter.getHp(), "Attack at extended range should deal base damage");
        }

        @Test
        @DisplayName("BA5 - OnTurnEnd poison deals damage")
        void ba5_poisonDealsDamageOnTurnEnd() {
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 3));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            Unit poisonedUnit = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p1"))
                .findFirst().orElseThrow();

            assertEquals(9, poisonedUnit.getHp(), "Poisoned unit should lose 1 HP at end of turn");
        }

        @Test
        @DisplayName("BA6 - Poison damage can kill the unit")
        void ba6_poisonCanKillUnit() {
            Unit u1_p1 = createUnit("u1_p1", p1, 1, 3, 1, 1, new Position(1, 1), true); // 1 HP
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 3));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            Unit poisonedUnit = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p1"))
                .findFirst().orElseThrow();

            assertEquals(0, poisonedUnit.getHp(), "Poisoned unit should have 0 HP");
            assertFalse(poisonedUnit.isAlive(), "Poisoned unit should be dead");
        }

        @Test
        @DisplayName("BA7 - Buff expiration happens AFTER poison damage in same turn")
        void ba7_buffExpirationAfterPoison() {
            // Poison with duration=1 should deal damage then expire
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 1)); // Duration 1

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            // Should take poison damage
            Unit poisonedUnit = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p1"))
                .findFirst().orElseThrow();
            assertEquals(9, poisonedUnit.getHp(), "Poison should deal damage before expiring");

            // Buff should be expired (empty list or no entry)
            List<BuffInstance> remainingBuffs = newState.getUnitBuffs().getOrDefault("u1_p1", Collections.emptyList());
            assertTrue(remainingBuffs.isEmpty(), "Poison buff with duration 1 should expire after turn end");
        }

        @Test
        @DisplayName("BA8 - Stun does not interfere with END_TURN applyAction")
        void ba8_stunDoesNotInterferEndTurn() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(3, 3));

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createStunBuff("u1_p2", 2));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            // Should not throw and should change turn
            GameState newState = ruleEngine.applyAction(state, action);

            assertEquals(p2, newState.getCurrentPlayer(), "Turn should pass to P2 even with stunned units");
        }
    }

    // ========== BP-Series: Poison & Turn End Tests ==========

    @Nested
    @DisplayName("BP-Series - Poison & Turn End Tests")
    class BuffPoisonTests {

        @Test
        @DisplayName("BP1 - Single poison tick deals exactly 1 damage")
        void bp1_singlePoisonTickDeals1Damage() {
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 3));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            Unit poisonedUnit = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p1"))
                .findFirst().orElseThrow();

            assertEquals(9, poisonedUnit.getHp(), "Single poison should deal exactly 1 damage");
        }

        @Test
        @DisplayName("BP2 - Multiple poison buffs stack damage")
        void bp2_multiplePoisonBuffsStackDamage() {
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 3));
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 2)); // stackable=true

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            Unit poisonedUnit = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p1"))
                .findFirst().orElseThrow();

            assertEquals(8, poisonedUnit.getHp(), "Two poison buffs should deal 2 damage total");
        }

        @Test
        @DisplayName("BP3 - Poison kills the unit at turn end")
        void bp3_poisonKillsAtTurnEnd() {
            Unit u1_p1 = createUnit("u1_p1", p1, 1, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 3));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            Unit poisonedUnit = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p1"))
                .findFirst().orElseThrow();

            assertTrue(poisonedUnit.getHp() <= 0, "Poisoned unit should have 0 or less HP");
            assertFalse(poisonedUnit.isAlive(), "Poisoned unit should be dead");
        }

        @Test
        @DisplayName("BP4 - Poison damage does not occur for already-dead unit")
        void bp4_poisonDoesNotAffectDeadUnit() {
            Unit u1_p1 = createUnit("u1_p1", p1, 0, 3, 1, 1, new Position(1, 1), false); // already dead
            Unit u2_p1 = createUnit("u2_p1", p1, 10, 3, 1, 1, new Position(2, 2), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 3));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u2_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            Unit deadUnit = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p1"))
                .findFirst().orElseThrow();

            // HP should remain 0, not go negative
            assertEquals(0, deadUnit.getHp(), "Dead unit's HP should remain at 0");
            assertFalse(deadUnit.isAlive(), "Dead unit should remain dead");
        }

        @Test
        @DisplayName("BP5 - Poison duration reduces exactly like other buffs")
        void bp5_poisonDurationReduces() {
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 3)); // Duration 3

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState newState = ruleEngine.applyAction(state, action);

            List<BuffInstance> unitBuffs = newState.getUnitBuffs().getOrDefault("u1_p1", Collections.emptyList());
            assertFalse(unitBuffs.isEmpty(), "Poison buff should still exist");

            BuffInstance poisonBuff = unitBuffs.stream()
                .filter(b -> b.getBuffId().equals("POISON"))
                .findFirst().orElseThrow();

            assertEquals(2, poisonBuff.getDuration(), "Poison duration should decrease by 1");
        }
    }

    // ========== BC-Series: Backward Compatibility Tests ==========

    @Nested
    @DisplayName("BC-Series - Backward Compatibility Tests")
    class BuffCompatibilityTests {

        @Test
        @DisplayName("BC1 - No buffs: V1/V2 MOVE rules unchanged")
        void bc1_noBuffsMoveUnchanged() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(3, 3));

            // No buffs
            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            // Valid move
            Action validMove = new Action(ActionType.MOVE, p1, new Position(1, 2), null);
            ValidationResult validResult = ruleEngine.validateAction(state, validMove);
            assertTrue(validResult.isValid(), "Valid MOVE should still work without buffs");

            // Invalid move (distance 2)
            Action invalidMove = new Action(ActionType.MOVE, p1, new Position(1, 3), null);
            ValidationResult invalidResult = ruleEngine.validateAction(state, invalidMove);
            assertFalse(invalidResult.isValid(), "Invalid MOVE should still fail without buffs");
            assertEquals("No valid unit can move to target position", invalidResult.getErrorMessage());
        }

        @Test
        @DisplayName("BC2 - No buffs: V1/V2 ATTACK rules unchanged")
        void bc2_noBuffsAttackUnchanged() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(1, 2));

            GameState state = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            // Valid attack
            Action validAttack = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");
            ValidationResult validResult = ruleEngine.validateAction(state, validAttack);
            assertTrue(validResult.isValid(), "Valid ATTACK should still work without buffs");

            // Invalid attack (out of range)
            Unit u2_p2 = createUnit("u2_p2", p2, new Position(1, 4));
            GameState state2 = new GameState(board, Arrays.asList(u1_p1, u2_p2), p1, false, null);
            Action invalidAttack = new Action(ActionType.ATTACK, p1, new Position(1, 4), "u2_p2");
            ValidationResult invalidResult = ruleEngine.validateAction(state2, invalidAttack);
            assertFalse(invalidResult.isValid(), "Invalid ATTACK should still fail without buffs");
        }

        @Test
        @DisplayName("BC3 - No buffs: V2 moveRange/attackRange rules unchanged")
        void bc3_noBuffsV2RangeRulesUnchanged() {
            // Unit with moveRange=2, attackRange=2
            Unit archer = createUnit("archer", p1, 10, 3, 1, 2, new Position(1, 1), true);
            Unit enemy = createUnit("enemy", p2, 10, 3, 1, 1, new Position(1, 3), true);

            GameState state = new GameState(board, Arrays.asList(archer, enemy), p1, false, null);

            // Archer with attackRange=2 can attack 2 tiles away
            Action attackAction = new Action(ActionType.ATTACK, p1, new Position(1, 3), "enemy");
            ValidationResult result = ruleEngine.validateAction(state, attackAction);
            assertTrue(result.isValid(), "Unit with attackRange=2 should attack at distance 2");
        }

        @Test
        @DisplayName("BC4 - No buffs: validateAction behaves same as before Buff System")
        void bc4_noBuffsValidateBehaviorUnchanged() {
            Unit u1_p1 = createUnit("u1_p1", p1, new Position(1, 1));
            Unit u1_p2 = createUnit("u1_p2", p2, new Position(2, 2));

            GameState stateWithoutBuffs = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);
            GameState stateWithEmptyBuffs = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, Collections.emptyMap());

            Action action = new Action(ActionType.MOVE_AND_ATTACK, p1, new Position(1, 2), "u1_p2");

            ValidationResult resultWithout = ruleEngine.validateAction(stateWithoutBuffs, action);
            ValidationResult resultWithEmpty = ruleEngine.validateAction(stateWithEmptyBuffs, action);

            assertEquals(resultWithout.isValid(), resultWithEmpty.isValid(),
                "Validation results should match with and without buff map");
            assertEquals(resultWithout.getErrorMessage(), resultWithEmpty.getErrorMessage(),
                "Error messages should match with and without buff map");
        }

        @Test
        @DisplayName("BC5 - No buffs: applyAction behaves same as before Buff System")
        void bc5_noBuffsApplyBehaviorUnchanged() {
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(1, 2), true);

            GameState stateWithoutBuffs = new GameState(board, Arrays.asList(u1_p1, u1_p2), p1, false, null);

            Action action = new Action(ActionType.ATTACK, p1, new Position(1, 2), "u1_p2");

            GameState newState = ruleEngine.applyAction(stateWithoutBuffs, action);

            Unit targetAfter = newState.getUnits().stream()
                .filter(u -> u.getId().equals("u1_p2"))
                .findFirst().orElseThrow();

            // Standard attack: 10 HP - 3 damage = 7 HP
            assertEquals(7, targetAfter.getHp(), "Attack without buffs should deal base damage");
        }
    }

    // ========== BD-Series: Determinism Tests ==========

    @Nested
    @DisplayName("BD-Series - Determinism Tests")
    class BuffDeterminismTests {

        @Test
        @DisplayName("BD1 - Buffs resolve in deterministic order (unitId sorted ascending)")
        void bd1_buffsResolveInDeterministicOrder() {
            // Create units with IDs that would sort differently
            Unit unitA = createUnit("a_unit", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit unitC = createUnit("c_unit", p1, 10, 3, 1, 1, new Position(2, 2), true);
            Unit unitB = createUnit("b_unit", p1, 10, 3, 1, 1, new Position(3, 3), true);
            Unit enemy = createUnit("z_enemy", p2, 10, 3, 1, 1, new Position(4, 4), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "a_unit", createPoisonBuff("z_enemy", 3));
            buffs = addBuff(buffs, "c_unit", createPoisonBuff("z_enemy", 3));
            buffs = addBuff(buffs, "b_unit", createPoisonBuff("z_enemy", 3));

            GameState state = withBuffs(board, Arrays.asList(unitA, unitC, unitB, enemy), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            // Run twice to ensure deterministic results
            GameState result1 = ruleEngine.applyAction(state, action);
            GameState result2 = ruleEngine.applyAction(state, action);

            // Results should be identical
            for (Unit u : result1.getUnits()) {
                Unit corresponding = result2.getUnits().stream()
                    .filter(u2 -> u2.getId().equals(u.getId()))
                    .findFirst().orElseThrow();
                assertEquals(u.getHp(), corresponding.getHp(),
                    "HP should be identical across runs for " + u.getId());
            }
        }

        @Test
        @DisplayName("BD2 - Buff expiration order deterministic")
        void bd2_buffExpirationDeterministic() {
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);

            // Multiple buffs with same duration should expire in deterministic order
            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 1));
            buffs = addBuff(buffs, "u1_p1", createStunBuff("u1_p2", 1));
            buffs = addBuff(buffs, "u1_p1", createRageBuff("u1_p2", 1));

            GameState state = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState result1 = ruleEngine.applyAction(state, action);
            GameState result2 = ruleEngine.applyAction(state, action);

            // All buffs should expire
            List<BuffInstance> buffs1 = result1.getUnitBuffs().getOrDefault("u1_p1", Collections.emptyList());
            List<BuffInstance> buffs2 = result2.getUnitBuffs().getOrDefault("u1_p1", Collections.emptyList());

            assertEquals(buffs1.size(), buffs2.size(), "Same number of buffs should remain");
        }

        @Test
        @DisplayName("BD3 - Poison ticks applied in deterministic order")
        void bd3_poisonTicksDeterministic() {
            // Multiple poisoned units should tick in sorted order
            Unit unitA = createUnit("a_unit", p1, 5, 3, 1, 1, new Position(1, 1), true);
            Unit unitB = createUnit("b_unit", p1, 5, 3, 1, 1, new Position(2, 2), true);
            Unit enemy = createUnit("enemy", p2, 10, 3, 1, 1, new Position(4, 4), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "a_unit", createPoisonBuff("enemy", 3));
            buffs = addBuff(buffs, "b_unit", createPoisonBuff("enemy", 3));

            GameState state = withBuffs(board, Arrays.asList(unitA, unitB, enemy), p1, false, null, buffs);
            Action action = new Action(ActionType.END_TURN, p1, null, null);

            GameState result1 = ruleEngine.applyAction(state, action);
            GameState result2 = ruleEngine.applyAction(state, action);

            // Both should have same HP values
            for (String id : Arrays.asList("a_unit", "b_unit")) {
                int hp1 = result1.getUnits().stream().filter(u -> u.getId().equals(id)).findFirst().orElseThrow().getHp();
                int hp2 = result2.getUnits().stream().filter(u -> u.getId().equals(id)).findFirst().orElseThrow().getHp();
                assertEquals(hp1, hp2, "HP for " + id + " should be deterministic");
            }
        }

        @Test
        @DisplayName("BD4 - Replay: Same initial state + actions = identical final state")
        void bd4_replayProducesIdenticalState() {
            Unit u1_p1 = createUnit("u1_p1", p1, 10, 3, 1, 1, new Position(1, 1), true);
            Unit u1_p2 = createUnit("u1_p2", p2, 10, 3, 1, 1, new Position(3, 3), true);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs = addBuff(buffs, "u1_p1", createRageBuff("u1_p1", 3));
            buffs = addBuff(buffs, "u1_p1", createPoisonBuff("u1_p2", 2));

            GameState initialState = withBuffs(board, Arrays.asList(u1_p1, u1_p2), p1, false, null, buffs);

            // Sequence of actions
            List<Action> actions = Arrays.asList(
                new Action(ActionType.MOVE, p1, new Position(1, 2), null),
                new Action(ActionType.END_TURN, p1, null, null)
            );

            // Apply actions twice
            GameState state1 = initialState;
            GameState state2 = initialState;

            for (Action action : actions) {
                if (ruleEngine.validateAction(state1, action).isValid()) {
                    state1 = ruleEngine.applyAction(state1, action);
                }
                if (ruleEngine.validateAction(state2, action).isValid()) {
                    state2 = ruleEngine.applyAction(state2, action);
                }
            }

            // Compare final states
            assertEquals(state1.getCurrentPlayer(), state2.getCurrentPlayer(), "Current player should match");
            assertEquals(state1.isGameOver(), state2.isGameOver(), "Game over status should match");

            for (int i = 0; i < state1.getUnits().size(); i++) {
                Unit unit1 = state1.getUnits().get(i);
                Unit unit2 = state2.getUnits().stream()
                    .filter(u -> u.getId().equals(unit1.getId()))
                    .findFirst().orElseThrow();
                assertEquals(unit1.getHp(), unit2.getHp(), "HP should match for " + unit1.getId());
                assertEquals(unit1.getPosition(), unit2.getPosition(), "Position should match for " + unit1.getId());
            }
        }
    }
}
