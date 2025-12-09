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
 * BL-Series: Buff Lifecycle Tests (from BUFF_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for buff duration, removal, and expiration:
 * - Duration reduces at end of round
 * - Buff with duration=1 removed after round end
 * - Multiple buffs decrease together
 * - Instant HP effects on acquisition
 * - Instant HP can kill or exceed maxHp
 */
@DisplayName("BL-Series: Buff Lifecycle Tests")
public class RuleEngineBuffLifecycleTest {

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

    private Unit createUnitWithMaxHp(String id, PlayerId owner, Position pos, int hp, int maxHp) {
        return new Unit(id, owner, hp, 3, 1, 1, pos, true,
            UnitCategory.HERO, null, HeroClass.WARRIOR, maxHp, null, 0,
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

    // ========== BL-Series Tests ==========

    @Nested
    @DisplayName("BL1: Duration reduces by 1 at end of each round")
    class DurationReductionTests {

        @Test
        @DisplayName("BL1: Duration reduces by 1 at round end")
        void durationReducesAtRoundEnd() {
            // Given: Unit with POWER buff duration=2
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("test_source");
            assertEquals(2, powerBuff.getDuration(), "Initial POWER buff should have duration 2");

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", new ArrayList<>(Collections.singletonList(powerBuff)));

            // Units must have acted to enable round end
            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round
            GameState afterRound = endRound(state);

            // Then: Duration should be reduced to 1
            int newDuration = getBuffDuration(afterRound, "p1_unit", BuffType.POWER);
            assertEquals(1, newDuration, "POWER buff duration should reduce from 2 to 1 after round end");
        }
    }

    @Nested
    @DisplayName("BL2: Buff with duration=1 is removed after round end")
    class BuffExpirationTests {

        @Test
        @DisplayName("BL2: Buff with duration=1 removed after round end")
        void buffWithDuration1RemovedAfterRoundEnd() {
            // Given: Unit with POWER buff duration=1
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("test_source").withDuration(1);
            assertEquals(1, powerBuff.getDuration(), "Initial POWER buff should have duration 1");

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", new ArrayList<>(Collections.singletonList(powerBuff)));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round
            GameState afterRound = endRound(state);

            // Then: Buff should be removed
            List<BuffInstance> remainingBuffs = getBuffs(afterRound, "p1_unit");
            assertTrue(remainingBuffs.isEmpty() ||
                remainingBuffs.stream().noneMatch(b -> b.getType() == BuffType.POWER),
                "POWER buff with duration 1 should be removed after round end");
        }
    }

    @Nested
    @DisplayName("BL3: Multiple buffs decrease durations together")
    class MultipleBuffsDurationTests {

        @Test
        @DisplayName("BL3: Multiple buffs all decrease duration at round end")
        void multipleBuffsDecreaseTogether() {
            // Given: Unit with POWER (dur=2) and BLEED (dur=2)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("test_source");
            BuffInstance bleedBuff = BuffFactory.createBleed("test_source");

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Arrays.asList(powerBuff, bleedBuff));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round
            GameState afterRound = endRound(state);

            // Then: Both buffs should have duration reduced to 1
            assertEquals(1, getBuffDuration(afterRound, "p1_unit", BuffType.POWER),
                "POWER buff duration should be 1 after round end");
            assertEquals(1, getBuffDuration(afterRound, "p1_unit", BuffType.BLEED),
                "BLEED buff duration should be 1 after round end");
        }
    }

    @Nested
    @DisplayName("BL4: Buff with duration=0 is removed immediately")
    class ZeroDurationTests {

        @Test
        @DisplayName("BL4: Buff with duration=0 is removed immediately")
        void buffWithDuration0RemovedImmediately() {
            // Given: Unit with a buff that would have duration=0 after decrement
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            // Create buff with duration 1, so it becomes 0 and is removed
            BuffInstance powerBuff = BuffFactory.createPower("test_source").withDuration(1);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", new ArrayList<>(Collections.singletonList(powerBuff)));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round (duration 1 -> 0 -> removed)
            GameState afterRound = endRound(state);

            // Then: Buff should be removed
            assertTrue(getBuffs(afterRound, "p1_unit").isEmpty() ||
                getBuffs(afterRound, "p1_unit").stream().noneMatch(b -> b.getType() == BuffType.POWER),
                "Buff with duration 0 should be removed");
        }
    }

    @Nested
    @DisplayName("BL5: LIFE buff: +3 HP applied instantly on acquisition")
    class LifeBuffInstantHpTests {

        @Test
        @DisplayName("BL5: LIFE buff grants +3 HP instantly on acquisition")
        void lifeBuffGrants3HpInstantly() {
            // Given: Unit at position that will move to buff tile
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            int hpBefore = unit.getHp();

            // Create LIFE buff tile at target position
            BuffTile lifeTile = new BuffTile("tile1", new Position(2, 3), BuffType.LIFE, 2, false);

            GameState state = new GameState(board, Arrays.asList(unit, p2Unit), p1, false, null,
                Collections.emptyMap(), Collections.singletonList(lifeTile),
                Collections.emptyList(), 1, null, false, false);

            // When: Unit moves to buff tile
            Action move = Action.move("p1_unit", new Position(2, 3));
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should have +3 HP
            Unit updatedUnit = findUnit(afterMove, "p1_unit");
            assertEquals(hpBefore + 3, updatedUnit.getHp(),
                "LIFE buff should grant +3 HP instantly on acquisition");
        }
    }

    @Nested
    @DisplayName("BL6: POWER buff: +1 HP applied instantly on acquisition")
    class PowerBuffInstantHpTests {

        @Test
        @DisplayName("BL6: POWER buff grants +1 HP instantly on acquisition")
        void powerBuffGrants1HpInstantly() {
            // Given: Unit at position that will move to buff tile
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            int hpBefore = unit.getHp();

            // Create POWER buff tile at target position
            BuffTile powerTile = new BuffTile("tile1", new Position(2, 3), BuffType.POWER, 2, false);

            GameState state = new GameState(board, Arrays.asList(unit, p2Unit), p1, false, null,
                Collections.emptyMap(), Collections.singletonList(powerTile),
                Collections.emptyList(), 1, null, false, false);

            // When: Unit moves to buff tile
            Action move = Action.move("p1_unit", new Position(2, 3));
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should have +1 HP
            Unit updatedUnit = findUnit(afterMove, "p1_unit");
            assertEquals(hpBefore + 1, updatedUnit.getHp(),
                "POWER buff should grant +1 HP instantly on acquisition");
        }
    }

    @Nested
    @DisplayName("BL7: WEAKNESS buff: -1 HP applied instantly on acquisition")
    class WeaknessBuffInstantHpTests {

        @Test
        @DisplayName("BL7: WEAKNESS buff deals -1 HP instantly on acquisition")
        void weaknessBuffDeals1HpInstantly() {
            // Given: Unit at position that will move to buff tile
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            int hpBefore = unit.getHp();

            // Create WEAKNESS buff tile at target position
            BuffTile weaknessTile = new BuffTile("tile1", new Position(2, 3), BuffType.WEAKNESS, 2, false);

            GameState state = new GameState(board, Arrays.asList(unit, p2Unit), p1, false, null,
                Collections.emptyMap(), Collections.singletonList(weaknessTile),
                Collections.emptyList(), 1, null, false, false);

            // When: Unit moves to buff tile
            Action move = Action.move("p1_unit", new Position(2, 3));
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should have -1 HP
            Unit updatedUnit = findUnit(afterMove, "p1_unit");
            assertEquals(hpBefore - 1, updatedUnit.getHp(),
                "WEAKNESS buff should deal -1 HP instantly on acquisition");
        }
    }

    @Nested
    @DisplayName("BL8: Instant HP loss can kill unit")
    class InstantHpKillTests {

        @Test
        @DisplayName("BL8: Instant HP loss from WEAKNESS can kill unit")
        void instantHpLossCanKillUnit() {
            // Given: Unit with 1 HP
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2), 1);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            assertEquals(1, unit.getHp(), "Unit should start with 1 HP");

            // Create WEAKNESS buff tile at target position
            BuffTile weaknessTile = new BuffTile("tile1", new Position(2, 3), BuffType.WEAKNESS, 2, false);

            GameState state = new GameState(board, Arrays.asList(unit, p2Unit), p1, false, null,
                Collections.emptyMap(), Collections.singletonList(weaknessTile),
                Collections.emptyList(), 1, null, false, false);

            // When: Unit moves to buff tile
            Action move = Action.move("p1_unit", new Position(2, 3));
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should be dead (HP <= 0)
            Unit updatedUnit = findUnit(afterMove, "p1_unit");
            assertTrue(updatedUnit.getHp() <= 0 || !updatedUnit.isAlive(),
                "Instant HP loss from WEAKNESS should kill unit at 1 HP");
        }
    }

    @Nested
    @DisplayName("BL9: Instant HP gain can exceed maxHp")
    class InstantHpExceedMaxHpTests {

        @Test
        @DisplayName("BL9: Instant HP gain from LIFE can exceed maxHp")
        void instantHpGainCanExceedMaxHp() {
            // Given: Unit with HP=5, maxHp=5
            Unit unit = createUnitWithMaxHp("p1_unit", p1, new Position(2, 2), 5, 5);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            assertEquals(5, unit.getHp(), "Unit should start with 5 HP");
            assertEquals(5, unit.getMaxHp(), "Unit should have maxHp of 5");

            // Create LIFE buff tile at target position
            BuffTile lifeTile = new BuffTile("tile1", new Position(2, 3), BuffType.LIFE, 2, false);

            GameState state = new GameState(board, Arrays.asList(unit, p2Unit), p1, false, null,
                Collections.emptyMap(), Collections.singletonList(lifeTile),
                Collections.emptyList(), 1, null, false, false);

            // When: Unit moves to buff tile
            Action move = Action.move("p1_unit", new Position(2, 3));
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should have 8 HP (exceeds maxHp of 5)
            Unit updatedUnit = findUnit(afterMove, "p1_unit");
            assertEquals(8, updatedUnit.getHp(),
                "LIFE buff (+3 HP) should bring HP to 8, exceeding maxHp of 5");
        }
    }

    @Nested
    @DisplayName("BL10: Removing buff does not affect other buffs on the unit")
    class BuffRemovalIndependenceTests {

        @Test
        @DisplayName("BL10: Removing one buff does not affect other buffs")
        void removingOneBuffDoesNotAffectOthers() {
            // Given: Unit with POWER (dur=1) and SPEED (dur=2)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("test_source").withDuration(1);
            BuffInstance speedBuff = BuffFactory.createSpeed("test_source"); // duration=2

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Arrays.asList(powerBuff, speedBuff));

            GameState state = createStateWithUnitsActed(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: End round (POWER expires, SPEED remains)
            GameState afterRound = endRound(state);

            // Then: POWER should be removed, SPEED should remain with duration 1
            List<BuffInstance> remainingBuffs = getBuffs(afterRound, "p1_unit");

            boolean hasPower = remainingBuffs.stream().anyMatch(b -> b.getType() == BuffType.POWER);
            boolean hasSpeed = remainingBuffs.stream().anyMatch(b -> b.getType() == BuffType.SPEED);

            assertFalse(hasPower, "POWER buff (duration 1) should be removed after round end");
            assertTrue(hasSpeed, "SPEED buff should still be present");
            assertEquals(1, getBuffDuration(afterRound, "p1_unit", BuffType.SPEED),
                "SPEED buff duration should be 1 after round end");
        }
    }
}
