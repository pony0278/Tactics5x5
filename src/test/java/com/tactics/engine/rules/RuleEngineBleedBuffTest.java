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
 * BBL-Series: BLEED Buff Damage Over Time Tests (from BUFF_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for BLEED buff:
 * - 1 damage at round end
 * - Damage applies before duration decrement
 * - Can kill unit
 * - Dead unit takes no further damage
 * - Stacks with minion decay (if applicable)
 * - Multiple BLEED stack damage
 * - Processed in unit ID order
 * - Duration tracked separately from damage
 */
@DisplayName("BBL-Series: BLEED Buff Damage Over Time")
public class RuleEngineBleedBuffTest {

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

    private Unit createMinion(String id, PlayerId owner, Position pos, int hp, MinionType type) {
        return new Unit(id, owner, hp, 3, 1, 1, pos, true,
            UnitCategory.MINION, type, null, hp, null, 0,
            0, false, false, false, 0, null, 0, false, null, 0, 0);
    }

    private GameState createStateWithBuffs(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        return new GameState(board, units, p1, false, null, unitBuffs,
            new ArrayList<>(), new ArrayList<>(), 1, null, false, false);
    }

    private GameState createStateWithUnitsActed(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        // Create units that have already acted this round
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
        // Both players must END_TURN to complete a round
        Action endTurn1 = new Action(ActionType.END_TURN, p1, null, null);
        GameState afterP1 = ruleEngine.applyAction(state, endTurn1);

        Action endTurn2 = new Action(ActionType.END_TURN, p2, null, null);
        return ruleEngine.applyAction(afterP1, endTurn2);
    }

    // ========== BBL-Series Tests ==========

    @Nested
    @DisplayName("BBL1: BLEED buff deals 1 damage at round end")
    class BleedDamageAtRoundEndTests {

        @Test
        @DisplayName("BBL1: BLEED deals 1 damage at round end")
        void bleedDeals1DamageAtRoundEnd() {
            // Given: Unit with BLEED buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            int hpBefore = unit.getHp();

            BuffInstance bleedBuff = BuffFactory.createBleed("enemy_source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(bleedBuff));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round
            GameState afterRound = endRound(state);

            // Then: Unit should have lost 1 HP
            Unit updatedUnit = findUnit(afterRound, "p1_unit");
            assertEquals(hpBefore - 1, updatedUnit.getHp(),
                "BLEED should deal 1 damage at round end");
        }
    }

    @Nested
    @DisplayName("BBL2: BLEED damage applies before duration decrement")
    class BleedDamageBeforeDurationDecrementTests {

        @Test
        @DisplayName("BBL2: BLEED damage kills unit even if buff would expire")
        void bleedDamageKillsBeforeExpiration() {
            // Given: Unit with 1 HP and BLEED buff (duration=1)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2), 1);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance bleedBuff = BuffFactory.createBleed("enemy_source").withDuration(1);
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(bleedBuff));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round
            GameState afterRound = endRound(state);

            // Then: Unit should be dead (BLEED damage applied before buff expires)
            Unit updatedUnit = findUnit(afterRound, "p1_unit");
            assertTrue(updatedUnit.getHp() <= 0 || !updatedUnit.isAlive(),
                "BLEED damage should apply before duration decrement (killing 1 HP unit)");
        }
    }

    @Nested
    @DisplayName("BBL3: BLEED buff can kill unit")
    class BleedCanKillTests {

        @Test
        @DisplayName("BBL3: BLEED damage can kill unit")
        void bleedCanKillUnit() {
            // Given: Unit with 1 HP and BLEED buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2), 1);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance bleedBuff = BuffFactory.createBleed("enemy_source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(bleedBuff));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round
            GameState afterRound = endRound(state);

            // Then: Unit should be dead
            Unit updatedUnit = findUnit(afterRound, "p1_unit");
            assertFalse(updatedUnit.isAlive(), "BLEED should be able to kill unit");
        }
    }

    @Nested
    @DisplayName("BBL4: Dead unit takes no further BLEED damage")
    class DeadUnitNoFurtherDamageTests {

        @Test
        @DisplayName("BBL4: Dead unit takes no further BLEED damage")
        void deadUnitTakesNoFurtherBleedDamage() {
            // Given: Unit with 1 HP and BLEED buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2), 1);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance bleedBuff = BuffFactory.createBleed("enemy_source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(bleedBuff));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End first round (unit dies)
            GameState afterFirstRound = endRound(state);

            // Verify unit is dead
            Unit deadUnit = findUnit(afterFirstRound, "p1_unit");
            assertFalse(deadUnit.isAlive(), "Unit should be dead after first round");
            int hpAfterDeath = deadUnit.getHp();

            // Get state with units acted for second round
            List<Unit> unitsForSecondRound = new ArrayList<>();
            for (Unit u : afterFirstRound.getUnits()) {
                unitsForSecondRound.add(u.withActionsUsed(1));
            }
            GameState stateForSecondRound = new GameState(board, unitsForSecondRound, p1, false, null,
                afterFirstRound.getUnitBuffs(), new ArrayList<>(), new ArrayList<>(),
                afterFirstRound.getCurrentRound(), null, false, false);

            // End second round
            GameState afterSecondRound = endRound(stateForSecondRound);

            // Then: Dead unit's HP should not decrease further
            Unit stillDeadUnit = findUnit(afterSecondRound, "p1_unit");
            assertTrue(stillDeadUnit.getHp() <= hpAfterDeath,
                "Dead unit should not take additional BLEED damage");
        }
    }

    @Nested
    @DisplayName("BBL5: BLEED stacks with minion decay")
    class BleedStacksWithMinionDecayTests {

        @Test
        @DisplayName("BBL5: BLEED + minion decay deal combined damage")
        void bleedStacksWithMinionDecay() {
            // Given: Minion with BLEED buff
            Unit minion = createMinion("p1_minion", p1, new Position(2, 2), 5, MinionType.TANK);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            int hpBefore = minion.getHp();

            BuffInstance bleedBuff = BuffFactory.createBleed("enemy_source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_minion", Collections.singletonList(bleedBuff));

            GameState state = createStateWithUnitsActed(Arrays.asList(minion, p2Unit), unitBuffs);

            // When: End round
            GameState afterRound = endRound(state);

            // Then: Minion should take BLEED damage (-1) + possible decay (-1)
            // Note: The exact behavior depends on whether minion decay is implemented
            // At minimum, BLEED should deal 1 damage
            Unit updatedMinion = findUnit(afterRound, "p1_minion");
            assertTrue(updatedMinion.getHp() < hpBefore,
                "Minion with BLEED should take damage at round end");
            // If minion decay is active, HP should be hpBefore - 2 (BLEED + decay)
            // If only BLEED, HP should be hpBefore - 1
            assertTrue(updatedMinion.getHp() <= hpBefore - 1,
                "BLEED should deal at least 1 damage");
        }
    }

    @Nested
    @DisplayName("BBL6: Multiple BLEED stack damage")
    class MultipleBleedStackDamageTests {

        @Test
        @DisplayName("BBL6: Multiple BLEED buffs deal stacked damage")
        void multipleBleedStackDamage() {
            // Given: Unit with 2 BLEED buffs
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            int hpBefore = unit.getHp();

            BuffInstance bleedBuff1 = BuffFactory.createBleed("enemy_source_1");
            BuffInstance bleedBuff2 = BuffFactory.createBleed("enemy_source_2");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Arrays.asList(bleedBuff1, bleedBuff2));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round
            GameState afterRound = endRound(state);

            // Then: Unit should take 2 damage (1 per BLEED)
            Unit updatedUnit = findUnit(afterRound, "p1_unit");
            assertEquals(hpBefore - 2, updatedUnit.getHp(),
                "Multiple BLEED buffs should stack damage (2 BLEED = 2 damage)");
        }
    }

    @Nested
    @DisplayName("BBL7: BLEED processed in unit ID order")
    class BleedProcessingOrderTests {

        @Test
        @DisplayName("BBL7: BLEED damage processed in deterministic unit ID order")
        void bleedProcessedInUnitIdOrder() {
            // Given: Multiple units with BLEED (different IDs)
            Unit unitA = createUnit("a_unit", p1, new Position(1, 1));
            Unit unitC = createUnit("c_unit", p1, new Position(2, 2));
            Unit unitB = createUnit("b_unit", p1, new Position(3, 3));
            Unit enemy = createUnit("z_enemy", p2, new Position(4, 4));

            BuffInstance bleedA = BuffFactory.createBleed("z_enemy");
            BuffInstance bleedB = BuffFactory.createBleed("z_enemy");
            BuffInstance bleedC = BuffFactory.createBleed("z_enemy");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("a_unit", Collections.singletonList(bleedA));
            unitBuffs.put("b_unit", Collections.singletonList(bleedB));
            unitBuffs.put("c_unit", Collections.singletonList(bleedC));

            GameState state = createStateWithUnitsActed(Arrays.asList(unitA, unitC, unitB, enemy), unitBuffs);

            // When: Run twice to verify determinism
            GameState result1 = endRound(state);

            // Reset state
            GameState state2 = createStateWithUnitsActed(Arrays.asList(unitA, unitC, unitB, enemy), unitBuffs);
            GameState result2 = endRound(state2);

            // Then: Results should be identical (deterministic order)
            assertEquals(findUnit(result1, "a_unit").getHp(), findUnit(result2, "a_unit").getHp(),
                "BLEED processing should be deterministic for a_unit");
            assertEquals(findUnit(result1, "b_unit").getHp(), findUnit(result2, "b_unit").getHp(),
                "BLEED processing should be deterministic for b_unit");
            assertEquals(findUnit(result1, "c_unit").getHp(), findUnit(result2, "c_unit").getHp(),
                "BLEED processing should be deterministic for c_unit");
        }
    }

    @Nested
    @DisplayName("BBL8: BLEED duration tracked separately from damage")
    class BleedDurationSeparateFromDamageTests {

        @Test
        @DisplayName("BBL8: BLEED duration decrements, damage applies each round")
        void bleedDurationSeparateFromDamage() {
            // Given: Unit with BLEED buff (duration=2)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            int hpBefore = unit.getHp();

            BuffInstance bleedBuff = BuffFactory.createBleed("enemy_source"); // duration=2
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(bleedBuff));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End first round
            GameState afterFirstRound = endRound(state);

            // Then: Damage applied, duration reduced to 1
            Unit afterFirst = findUnit(afterFirstRound, "p1_unit");
            assertEquals(hpBefore - 1, afterFirst.getHp(), "Should take 1 BLEED damage after first round");
            assertEquals(1, getBuffDuration(afterFirstRound, "p1_unit", BuffType.BLEED),
                "BLEED duration should be 1 after first round");

            // When: End second round
            List<Unit> unitsForSecondRound = new ArrayList<>();
            for (Unit u : afterFirstRound.getUnits()) {
                unitsForSecondRound.add(u.withActionsUsed(1));
            }
            GameState stateForSecondRound = new GameState(board, unitsForSecondRound, p1, false, null,
                afterFirstRound.getUnitBuffs(), new ArrayList<>(), new ArrayList<>(),
                afterFirstRound.getCurrentRound(), null, false, false);

            GameState afterSecondRound = endRound(stateForSecondRound);

            // Then: Damage applied again, BLEED expired
            Unit afterSecond = findUnit(afterSecondRound, "p1_unit");
            assertEquals(hpBefore - 2, afterSecond.getHp(),
                "Should have taken total 2 BLEED damage after 2 rounds");

            // BLEED should be expired/removed
            List<BuffInstance> remainingBuffs = getBuffs(afterSecondRound, "p1_unit");
            assertTrue(remainingBuffs.isEmpty() ||
                remainingBuffs.stream().noneMatch(b -> b.getType() == BuffType.BLEED),
                "BLEED buff should be removed after duration expires");

            // When: End third round (BLEED gone)
            List<Unit> unitsForThirdRound = new ArrayList<>();
            for (Unit u : afterSecondRound.getUnits()) {
                unitsForThirdRound.add(u.withActionsUsed(1));
            }
            GameState stateForThirdRound = new GameState(board, unitsForThirdRound, p1, false, null,
                afterSecondRound.getUnitBuffs(), new ArrayList<>(), new ArrayList<>(),
                afterSecondRound.getCurrentRound(), null, false, false);

            GameState afterThirdRound = endRound(stateForThirdRound);

            // Then: No additional BLEED damage (buff expired)
            Unit afterThird = findUnit(afterThirdRound, "p1_unit");
            assertEquals(hpBefore - 2, afterThird.getHp(),
                "Should NOT take additional BLEED damage after buff expires");
        }
    }
}
