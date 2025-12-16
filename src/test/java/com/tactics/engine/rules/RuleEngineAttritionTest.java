package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.HeroClass;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V3 Attrition Mechanics Tests
 *
 * Tests for:
 * - Minion Decay: All minions lose 1 HP at end of each round
 * - Round 8 Pressure: All units lose 1 HP at end of round (when round >= 8)
 */
@DisplayName("V3 Attrition Mechanics Tests")
class RuleEngineAttritionTest {

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
     * Create a Hero unit.
     */
    private Unit createHero(String id, PlayerId owner, Position pos, int hp) {
        return new Unit(id, owner, hp, 1, 1, 1, pos, true,
                        UnitCategory.HERO, null, HeroClass.WARRIOR, hp,
                        null, 0, 0, false, false, false, 0, null,
                        0, false, null);
    }

    /**
     * Create a TANK minion.
     */
    private Unit createTank(String id, PlayerId owner, Position pos, int hp) {
        return new Unit(id, owner, hp, 1, 1, 1, pos, true,
                        UnitCategory.MINION, MinionType.TANK, null, hp,
                        null, 0, 0, false, false, false, 0, null,
                        0, false, null);
    }

    /**
     * Create an ARCHER minion.
     */
    private Unit createArcher(String id, PlayerId owner, Position pos, int hp) {
        return new Unit(id, owner, hp, 1, 1, 3, pos, true,
                        UnitCategory.MINION, MinionType.ARCHER, null, hp,
                        null, 0, 0, false, false, false, 0, null,
                        0, false, null);
    }

    /**
     * Create an ASSASSIN minion.
     */
    private Unit createAssassin(String id, PlayerId owner, Position pos, int hp) {
        return new Unit(id, owner, hp, 2, 4, 1, pos, true,
                        UnitCategory.MINION, MinionType.ASSASSIN, null, hp,
                        null, 0, 0, false, false, false, 0, null,
                        0, false, null);
    }

    /**
     * Create a unit with actionsUsed = 1 (has acted this round).
     */
    private Unit withActionsUsed(Unit u) {
        return u.withActionsUsed(1);
    }

    /**
     * Create a GameState at a specific round.
     */
    private GameState createStateAtRound(List<Unit> units, PlayerId currentPlayer, int round) {
        return new GameState(board, units, currentPlayer, false, null,
                             Collections.emptyMap(),
                             Collections.emptyList(), Collections.emptyList(),
                             round, null, false, false);
    }

    private Unit findUnitById(GameState state, String id) {
        return state.getUnits().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // ========== Minion Decay Tests ==========

    @Nested
    @DisplayName("Minion Decay")
    class MinionDecayTests {

        @Test
        @DisplayName("ATR1: Minions lose 1 HP at round end (starting round 3)")
        void minionsLoseHpAtRoundEnd() {
            // Given: P1 TANK with 5 HP, P2 ARCHER with 3 HP, both have acted
            // V3 Spec: Minion decay starts at round 3
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5));
            Unit p1Tank = withActionsUsed(createTank("p1_tank", p1, new Position(1, 0), 5));
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5));
            Unit p2Archer = withActionsUsed(createArcher("p2_archer", p2, new Position(3, 4), 3));

            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p1Tank, p2Hero, p2Archer), p1, 3);  // Start at round 3

            // When: P1 ends turn (triggers round end since all units acted)
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Minions lose 1 HP, Heroes unaffected
            assertEquals(5, findUnitById(result, "p1_hero").getHp(), "P1 Hero should be unaffected by decay");
            assertEquals(4, findUnitById(result, "p1_tank").getHp(), "P1 TANK should lose 1 HP (5->4)");
            assertEquals(5, findUnitById(result, "p2_hero").getHp(), "P2 Hero should be unaffected by decay");
            assertEquals(2, findUnitById(result, "p2_archer").getHp(), "P2 ARCHER should lose 1 HP (3->2)");
            assertEquals(4, result.getCurrentRound(), "Round should increment to 4");
        }

        @Test
        @DisplayName("ATR2: Heroes are NOT affected by minion decay")
        void heroesNotAffectedByDecay() {
            // Given: Only heroes, both have acted
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5));
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5));

            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p2Hero), p1, 1);

            // When: Round ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Heroes HP unchanged
            assertEquals(5, findUnitById(result, "p1_hero").getHp(), "P1 Hero should be unaffected");
            assertEquals(5, findUnitById(result, "p2_hero").getHp(), "P2 Hero should be unaffected");
        }

        @Test
        @DisplayName("ATR3: Minion with 1 HP dies from decay (starting round 3)")
        void minionDiesFromDecay() {
            // Given: P1 ASSASSIN with 1 HP (will die from decay)
            // V3 Spec: Minion decay starts at round 3
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5));
            Unit p1Assassin = withActionsUsed(createAssassin("p1_assassin", p1, new Position(1, 0), 1));
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5));

            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p1Assassin, p2Hero), p1, 3);  // Start at round 3

            // When: Round ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Assassin dies (1-1=0)
            Unit assassinAfter = findUnitById(result, "p1_assassin");
            assertEquals(0, assassinAfter.getHp(), "ASSASSIN HP should be 0");
            assertFalse(assassinAfter.isAlive(), "ASSASSIN should be dead");
            assertFalse(result.isGameOver(), "Game should NOT be over (Hero still alive)");
        }

        @Test
        @DisplayName("ATR4: All minion types affected equally (starting round 3)")
        void allMinionTypesAffected() {
            // Given: One of each minion type
            // V3 Spec: Minion decay starts at round 3
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5));
            Unit p1Tank = withActionsUsed(createTank("p1_tank", p1, new Position(1, 0), 5));
            Unit p1Archer = withActionsUsed(createArcher("p1_archer", p1, new Position(2, 0), 3));
            Unit p1Assassin = withActionsUsed(createAssassin("p1_assassin", p1, new Position(3, 0), 2));
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5));

            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p1Tank, p1Archer, p1Assassin, p2Hero), p1, 3);  // Start at round 3

            // When: Round ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: All minions lose 1 HP
            assertEquals(4, findUnitById(result, "p1_tank").getHp(), "TANK: 5->4");
            assertEquals(2, findUnitById(result, "p1_archer").getHp(), "ARCHER: 3->2");
            assertEquals(1, findUnitById(result, "p1_assassin").getHp(), "ASSASSIN: 2->1");
        }

        @Test
        @DisplayName("ATR5: Dead minions are not affected by decay")
        void deadMinionsNotAffected() {
            // Given: Dead TANK (HP=0, not alive)
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5));
            Unit p1Tank = new Unit("p1_tank", p1, 0, 1, 1, 1, new Position(1, 0), false,
                                   UnitCategory.MINION, MinionType.TANK, null, 5,
                                   null, 0, 0, false, false, false, 0, null,
                                   1, false, null);  // Dead, actionsUsed=1
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5));

            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p1Tank, p2Hero), p1, 1);

            // When: Round ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Dead TANK HP unchanged
            assertEquals(0, findUnitById(result, "p1_tank").getHp(), "Dead TANK HP should stay 0");
        }
    }

    // ========== Round 8 Pressure Tests ==========

    @Nested
    @DisplayName("Round 8 Pressure")
    class Round8PressureTests {

        @Test
        @DisplayName("ATR6: No pressure before Round 8")
        void noPressureBeforeRound8() {
            // Given: Round 7, Hero with 5 HP
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5));
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5));

            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p2Hero), p1, 7);

            // When: Round 7 ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: No pressure damage (Heroes unaffected by decay)
            assertEquals(5, findUnitById(result, "p1_hero").getHp(), "P1 Hero unaffected at R7");
            assertEquals(5, findUnitById(result, "p2_hero").getHp(), "P2 Hero unaffected at R7");
            assertEquals(8, result.getCurrentRound(), "Should be Round 8 now");
        }

        @Test
        @DisplayName("ATR7: Pressure starts at Round 8")
        void pressureStartsAtRound8() {
            // Given: Round 8, Hero with 5 HP
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5));
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5));

            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p2Hero), p1, 8);

            // When: Round 8 ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Both heroes lose 1 HP from pressure
            assertEquals(4, findUnitById(result, "p1_hero").getHp(), "P1 Hero: 5->4 from pressure");
            assertEquals(4, findUnitById(result, "p2_hero").getHp(), "P2 Hero: 5->4 from pressure");
            assertEquals(9, result.getCurrentRound(), "Should be Round 9 now");
        }

        @Test
        @DisplayName("ATR8: Minions take decay + pressure (2 HP total) at R8+")
        void minionsDecayPlusPressure() {
            // Given: Round 8, TANK with 5 HP
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5));
            Unit p1Tank = withActionsUsed(createTank("p1_tank", p1, new Position(1, 0), 5));
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 5));

            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p1Tank, p2Hero), p1, 8);

            // When: Round 8 ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: TANK loses 2 HP (1 decay + 1 pressure)
            assertEquals(3, findUnitById(result, "p1_tank").getHp(), "TANK: 5 - 1(decay) - 1(pressure) = 3");
            // Hero loses only 1 HP (pressure only)
            assertEquals(4, findUnitById(result, "p1_hero").getHp(), "Hero: 5 - 1(pressure) = 4");
        }

        @Test
        @DisplayName("ATR9: Hero dies from Round 8 pressure - game ends")
        void heroDiesFromPressure() {
            // Given: Round 8, P2 Hero with 1 HP
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 5));
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 1));

            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p2Hero), p1, 8);

            // When: Round 8 ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: P2 Hero dies, P1 wins
            Unit p2HeroAfter = findUnitById(result, "p2_hero");
            assertEquals(0, p2HeroAfter.getHp(), "P2 Hero HP should be 0");
            assertFalse(p2HeroAfter.isAlive(), "P2 Hero should be dead");
            assertTrue(result.isGameOver(), "Game should be over");
            assertEquals("P1", result.getWinner().getValue(), "P1 should win");
        }

        @Test
        @DisplayName("ATR10: Pressure continues after Round 8")
        void pressureContinuesAfterRound8() {
            // Given: Round 10, Heroes both with 3 HP
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 3));
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 3));

            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p2Hero), p1, 10);

            // When: Round 10 ends
            Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Both heroes lose 1 HP
            assertEquals(2, findUnitById(result, "p1_hero").getHp(), "P1 Hero: 3->2");
            assertEquals(2, findUnitById(result, "p2_hero").getHp(), "P2 Hero: 3->2");
        }
    }

    // ========== Survival Duration Tests ==========

    @Nested
    @DisplayName("Minion Survival Duration")
    class SurvivalDurationTests {

        @Test
        @DisplayName("ATR11: TANK survives 5 rounds of pure decay")
        void tankSurvives5Rounds() {
            // TANK: 5 HP, loses 1/round from decay
            // Round 1 end: 4 HP
            // Round 2 end: 3 HP
            // Round 3 end: 2 HP
            // Round 4 end: 1 HP
            // Round 5 end: 0 HP (dead)

            // We'll simulate this by checking after 4 rounds, TANK still alive
            Unit p1Hero = withActionsUsed(createHero("p1_hero", p1, new Position(0, 0), 10));
            Unit p1Tank = withActionsUsed(createTank("p1_tank", p1, new Position(1, 0), 5));
            Unit p2Hero = withActionsUsed(createHero("p2_hero", p2, new Position(4, 4), 10));

            // V3 Spec: Minion decay starts at round 3
            GameState state = createStateAtRound(
                Arrays.asList(p1Hero, p1Tank, p2Hero), p1, 3);

            // Simulate 4 round ends (rounds 3->4, 4->5, 5->6, 6->7)
            for (int i = 0; i < 4; i++) {
                // All units need actionsUsed=1 to trigger round end
                List<Unit> unitsWithActions = state.getUnits().stream()
                    .map(u -> u.withActionsUsed(1))
                    .toList();
                state = new GameState(board, unitsWithActions, p1, false, null,
                                      Collections.emptyMap(), Collections.emptyList(),
                                      Collections.emptyList(), state.getCurrentRound(),
                                      null, false, false);

                Action endTurn = new Action(ActionType.END_TURN, p1, null, null);
                state = ruleEngine.applyAction(state, endTurn);
            }

            // After 4 decay rounds (starting from round 3): TANK should have 1 HP (5 - 4 = 1)
            Unit tankAfter = findUnitById(state, "p1_tank");
            assertEquals(1, tankAfter.getHp(), "TANK should have 1 HP after 4 rounds of decay");
            assertTrue(tankAfter.isAlive(), "TANK should still be alive");
            assertEquals(7, state.getCurrentRound(), "Should be at Round 7");
        }
    }
}
