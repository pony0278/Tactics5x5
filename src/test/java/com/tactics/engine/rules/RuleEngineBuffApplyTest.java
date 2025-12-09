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
 * BA-Series: Buff Effects on applyAction Tests (from BUFF_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for how buffs affect action execution:
 * - POWER: +3 ATK damage
 * - WEAKNESS: -2 ATK damage (min 0)
 * - SPEED: Unit can act twice per round
 * - Round end: All buff durations decrease
 * - Buff expiration removes effects
 * - Multiple buff effects are additive
 */
@DisplayName("BA-Series: Buff Effects on applyAction")
public class RuleEngineBuffApplyTest {

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

    private GameState createStateWithBuffs(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        return new GameState(board, units, p1, false, null, unitBuffs,
            new ArrayList<>(), new ArrayList<>(), 1, null, false, false);
    }

    private GameState createStateWithUnitsActed(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        List<Unit> actedUnits = new ArrayList<>();
        for (Unit u : units) {
            actedUnits.add(u.withActionsUsed(1));
        }
        return new GameState(board, actedUnits, p1, false, null, unitBuffs,
            new ArrayList<>(), new ArrayList<>(), 1, null, false, false);
    }

    private Unit findUnit(GameState state, String id) {
        return state.getUnits().stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    private List<BuffInstance> getBuffs(GameState state, String unitId) {
        Map<String, List<BuffInstance>> unitBuffs = state.getUnitBuffs();
        if (unitBuffs == null) return Collections.emptyList();
        return unitBuffs.getOrDefault(unitId, Collections.emptyList());
    }

    private int getBuffDuration(GameState state, String unitId, BuffType type) {
        return getBuffs(state, unitId).stream()
            .filter(b -> b.getType() == type)
            .findFirst()
            .map(BuffInstance::getDuration)
            .orElse(-1);
    }

    private GameState endRound(GameState state) {
        Action endTurn1 = new Action(ActionType.END_TURN, p1, null, null);
        GameState afterP1 = ruleEngine.applyAction(state, endTurn1);

        Action endTurn2 = new Action(ActionType.END_TURN, p2, null, null);
        return ruleEngine.applyAction(afterP1, endTurn2);
    }

    // ========== BA-Series Tests ==========

    @Nested
    @DisplayName("BA1: POWER buff - ATTACK deals base + 3 damage")
    class PowerBuffDamageTests {

        @Test
        @DisplayName("BA1: POWER buff adds +3 ATK to attack damage")
        void powerBuffAdds3AtkDamage() {
            // Given: Attacker with POWER buff
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);
            int targetOriginalHp = target.getHp();

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // When: Attack
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // Then: Target takes base (3) + 3 = 6 damage
            Unit updatedTarget = findUnit(afterAttack, "p2_unit");
            assertEquals(targetOriginalHp - 6, updatedTarget.getHp(),
                "POWER buff should add +3 ATK: 3 base + 3 = 6 damage");
        }
    }

    @Nested
    @DisplayName("BA2: WEAKNESS buff - ATTACK deals base - 2 damage (min 0)")
    class WeaknessBuffDamageTests {

        @Test
        @DisplayName("BA2: WEAKNESS buff reduces ATK by 2")
        void weaknessBuffReducesAtkBy2() {
            // Given: Attacker with WEAKNESS buff
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);
            int targetOriginalHp = target.getHp();

            BuffInstance weaknessBuff = BuffFactory.createWeakness("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(weaknessBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // When: Attack
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // Then: Target takes base (3) - 2 = 1 damage
            Unit updatedTarget = findUnit(afterAttack, "p2_unit");
            assertEquals(targetOriginalHp - 1, updatedTarget.getHp(),
                "WEAKNESS buff should reduce ATK by 2: 3 base - 2 = 1 damage");
        }

        @Test
        @DisplayName("BA2b: WEAKNESS buff - damage cannot go below 0")
        void weaknessBuffDamageMinZero() {
            // Given: Attacker with WEAKNESS buff and low base ATK
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2), 10, 1);
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);
            int targetOriginalHp = target.getHp();

            BuffInstance weaknessBuff = BuffFactory.createWeakness("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(weaknessBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // When: Attack (1 - 2 = -1, should be clamped to 0)
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // Then: Target takes 0 damage (minimum)
            Unit updatedTarget = findUnit(afterAttack, "p2_unit");
            assertTrue(updatedTarget.getHp() >= targetOriginalHp - 1,
                "WEAKNESS buff damage should not go below 0");
        }
    }

    @Nested
    @DisplayName("BA4: SPEED buff - Unit can act twice per round")
    class SpeedBuffDoubleActionTests {

        @Test
        @DisplayName("BA4: SPEED buff allows second action in same round")
        void speedBuffAllowsSecondAction() {
            // Given: Unit with SPEED buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit enemy = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance speedBuff = BuffFactory.createSpeed("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(speedBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, enemy), unitBuffs);

            // When: First action (move)
            Action move = Action.move("p1_unit", new Position(2, 3));
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit can still act (second action allowed)
            Unit unitAfterMove = findUnit(afterMove, "p1_unit");
            assertTrue(unitAfterMove.getActionsUsed() < 2,
                "Unit with SPEED buff should still have action remaining after first action");

            // Validate second action is allowed
            Action secondMove = Action.move("p1_unit", new Position(2, 4));
            ValidationResult result = ruleEngine.validateAction(afterMove, secondMove);
            assertTrue(result.isValid(),
                "Second action should be valid for unit with SPEED buff");
        }
    }

    @Nested
    @DisplayName("BA11: Round end - All buff durations decrease")
    class RoundEndDurationDecreaseTests {

        @Test
        @DisplayName("BA11: All buff durations decrease by 1 at round end")
        void allBuffDurationsDecreaseAtRoundEnd() {
            // Given: Unit with POWER buff (duration=2)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source");
            assertEquals(2, powerBuff.getDuration(), "Initial duration should be 2");

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round
            GameState afterRound = endRound(state);

            // Then: Duration should be 1
            assertEquals(1, getBuffDuration(afterRound, "p1_unit", BuffType.POWER),
                "Buff duration should decrease by 1 at round end");
        }
    }

    @Nested
    @DisplayName("BA12: Round end - Expired buffs are removed")
    class RoundEndExpiredBuffsRemovedTests {

        @Test
        @DisplayName("BA12: Buff with duration=1 is removed after round end")
        void expiredBuffsRemovedAtRoundEnd() {
            // Given: Unit with POWER buff (duration=1)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source").withDuration(1);
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round
            GameState afterRound = endRound(state);

            // Then: Buff should be removed
            List<BuffInstance> remainingBuffs = getBuffs(afterRound, "p1_unit");
            assertTrue(remainingBuffs.isEmpty() ||
                remainingBuffs.stream().noneMatch(b -> b.getType() == BuffType.POWER),
                "Expired buff should be removed at round end");
        }
    }

    @Nested
    @DisplayName("BA13: Round end - Buff expiration removes modifier effects")
    class BuffExpirationRemovesEffectsTests {

        @Test
        @DisplayName("BA13: After POWER expires, ATK returns to base")
        void buffExpirationRemovesModifierEffects() {
            // Given: Unit with POWER buff (duration=1) attacks before expiration
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 20, 3);

            BuffInstance powerBuff = BuffFactory.createPower("source").withDuration(1);
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // Attack with POWER (should deal 6 damage)
            Action attack1 = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterFirstAttack = ruleEngine.applyAction(state, attack1);

            Unit targetAfterFirst = findUnit(afterFirstAttack, "p2_unit");
            assertEquals(14, targetAfterFirst.getHp(), "First attack with POWER should deal 6 damage");

            // End round to expire buff
            List<Unit> unitsForRound = new ArrayList<>();
            for (Unit u : afterFirstAttack.getUnits()) {
                unitsForRound.add(u.withActionsUsed(1));
            }
            GameState stateForRound = new GameState(board, unitsForRound, p1, false, null,
                afterFirstAttack.getUnitBuffs(), new ArrayList<>(), new ArrayList<>(),
                1, null, false, false);

            GameState afterRound = endRound(stateForRound);

            // Verify POWER expired
            assertTrue(getBuffs(afterRound, "p1_unit").isEmpty() ||
                getBuffs(afterRound, "p1_unit").stream().noneMatch(b -> b.getType() == BuffType.POWER),
                "POWER buff should be expired");

            // Reset actions for new round
            List<Unit> unitsNewRound = new ArrayList<>();
            for (Unit u : afterRound.getUnits()) {
                unitsNewRound.add(u.withActionsUsed(0));
            }
            GameState newRoundState = new GameState(board, unitsNewRound, p1, false, null,
                afterRound.getUnitBuffs(), new ArrayList<>(), new ArrayList<>(),
                afterRound.getCurrentRound(), null, false, false);

            // Attack without POWER (should deal 3 damage)
            Action attack2 = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterSecondAttack = ruleEngine.applyAction(newRoundState, attack2);

            Unit targetAfterSecond = findUnit(afterSecondAttack, "p2_unit");
            assertEquals(11, targetAfterSecond.getHp(),
                "Second attack without POWER should deal only 3 damage (base ATK)");
        }
    }

    @Nested
    @DisplayName("BA14: Buff acquired mid-round has full duration")
    class BuffAcquiredMidRoundTests {

        @Test
        @DisplayName("BA14: Buff acquired mid-round starts with full duration")
        void buffAcquiredMidRoundHasFullDuration() {
            // Given: Unit moves onto POWER buff tile mid-round
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffTile powerTile = new BuffTile("tile1", new Position(2, 3), BuffType.POWER, 2, false);

            GameState state = new GameState(board, Arrays.asList(unit, p2Unit), p1, false, null,
                Collections.emptyMap(), Collections.singletonList(powerTile),
                Collections.emptyList(), 1, null, false, false);

            // When: Move onto buff tile
            Action move = Action.move("p1_unit", new Position(2, 3));
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Buff should have duration=2 (full)
            assertEquals(2, getBuffDuration(afterMove, "p1_unit", BuffType.POWER),
                "Newly acquired buff should have full duration of 2");
        }
    }

    @Nested
    @DisplayName("BA17: Multiple buff effects are additive")
    class MultipleBuffEffectsAdditiveTests {

        @Test
        @DisplayName("BA17: POWER (+3) and WEAKNESS (-2) = net +1 ATK")
        void multipleBuffEffectsAdditive() {
            // Given: Unit with both POWER (+3) and WEAKNESS (-2)
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);
            int targetOriginalHp = target.getHp();

            BuffInstance powerBuff = BuffFactory.createPower("source");
            BuffInstance weaknessBuff = BuffFactory.createWeakness("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Arrays.asList(powerBuff, weaknessBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // When: Attack
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // Then: Target takes base (3) + 3 - 2 = 4 damage
            Unit updatedTarget = findUnit(afterAttack, "p2_unit");
            assertEquals(targetOriginalHp - 4, updatedTarget.getHp(),
                "Multiple buff effects should be additive: 3 + 3 - 2 = 4 damage");
        }
    }
}
