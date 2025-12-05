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
 * V3 Guardian Passive Tests
 *
 * Tests for TANK's Guardian passive ability:
 * - Intercepts damage to adjacent friendly units
 * - Does not protect itself
 * - Lowest ID TANK intercepts when multiple qualify
 * - Dead TANK cannot intercept
 */
@DisplayName("V3 Guardian Passive Tests")
class RuleEngineGuardianTest {

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
     * Create a basic unit (V1/V2 style).
     */
    private Unit createUnit(String id, PlayerId owner, Position pos, int hp, int attack) {
        return new Unit(id, owner, hp, attack, 1, 1, pos, true);
    }

    /**
     * Create a TANK minion with Guardian passive.
     */
    private Unit createTank(String id, PlayerId owner, Position pos, int hp) {
        return new Unit(id, owner, hp, 1, 1, 1, pos, true,
                        UnitCategory.MINION, MinionType.TANK, null, hp,
                        null, 0, 0, false, false, false, 0, null,
                        0, false, null);
    }

    /**
     * Create a TANK minion that is dead.
     */
    private Unit createDeadTank(String id, PlayerId owner, Position pos) {
        return new Unit(id, owner, 0, 1, 1, 1, pos, false,
                        UnitCategory.MINION, MinionType.TANK, null, 5,
                        null, 0, 0, false, false, false, 0, null,
                        0, false, null);
    }

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

    private GameState createState(List<Unit> units, PlayerId currentPlayer) {
        return new GameState(board, units, currentPlayer, false, null, Collections.emptyMap());
    }

    private Unit findUnitById(GameState state, String id) {
        return state.getUnits().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // ========== Guardian Basic Tests ==========

    @Nested
    @DisplayName("Guardian Basic Intercept")
    class GuardianBasicIntercept {

        @Test
        @DisplayName("GRD1: TANK intercepts damage to adjacent friendly unit")
        void tankInterceptsDamageToAdjacentFriendly() {
            // Given: P1 Hero at (2,2), P1 TANK at (2,3) adjacent to Hero
            //        P2 attacker at (2,1) adjacent to Hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1Tank = createTank("p1_tank", p1, new Position(2, 3), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1Tank, p2Attacker), p2);

            // When: P2 attacks P1 Hero
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: TANK takes 3 damage (5-3=2 HP), Hero takes 0 damage
            Unit heroAfter = findUnitById(result, "p1_hero");
            Unit tankAfter = findUnitById(result, "p1_tank");

            assertEquals(5, heroAfter.getHp(), "Hero should take no damage (protected by TANK)");
            assertEquals(2, tankAfter.getHp(), "TANK should take 3 damage (5-3=2)");
            assertTrue(heroAfter.isAlive(), "Hero should be alive");
            assertTrue(tankAfter.isAlive(), "TANK should be alive");
        }

        @Test
        @DisplayName("GRD2: TANK does not intercept when target is itself")
        void tankDoesNotInterceptSelf() {
            // Given: P1 TANK at (2,2) attacked directly
            //        P2 attacker at (2,1) adjacent to TANK
            Unit p1Tank = createTank("p1_tank", p1, new Position(2, 2), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Tank, p2Attacker), p2);

            // When: P2 attacks P1 TANK directly
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_tank");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: TANK takes full 3 damage
            Unit tankAfter = findUnitById(result, "p1_tank");
            assertEquals(2, tankAfter.getHp(), "TANK should take 3 damage when attacked directly");
        }

        @Test
        @DisplayName("GRD3: No intercept when TANK not adjacent")
        void noInterceptWhenNotAdjacent() {
            // Given: P1 Hero at (2,2), P1 TANK at (4,4) NOT adjacent
            //        P2 attacker at (2,1) adjacent to Hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1Tank = createTank("p1_tank", p1, new Position(4, 4), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1Tank, p2Attacker), p2);

            // When: P2 attacks P1 Hero
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Hero takes 3 damage (5-3=2 HP), TANK unaffected
            Unit heroAfter = findUnitById(result, "p1_hero");
            Unit tankAfter = findUnitById(result, "p1_tank");

            assertEquals(2, heroAfter.getHp(), "Hero should take 3 damage (no TANK protection)");
            assertEquals(5, tankAfter.getHp(), "TANK should be unaffected (not adjacent)");
        }

        @Test
        @DisplayName("GRD4: Dead TANK cannot intercept")
        void deadTankCannotIntercept() {
            // Given: P1 Hero at (2,2), Dead P1 TANK at (2,3)
            //        P2 attacker at (2,1)
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1DeadTank = createDeadTank("p1_tank", p1, new Position(2, 3));
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1DeadTank, p2Attacker), p2);

            // When: P2 attacks P1 Hero
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Hero takes full 3 damage (dead TANK cannot protect)
            Unit heroAfter = findUnitById(result, "p1_hero");
            assertEquals(2, heroAfter.getHp(), "Hero should take 3 damage (dead TANK cannot protect)");
        }

        @Test
        @DisplayName("GRD5: Enemy TANK does not intercept")
        void enemyTankDoesNotIntercept() {
            // Given: P1 Hero at (2,2), P2 TANK at (2,3) (enemy to hero - should not protect)
            //        P2 attacker at (2,1) - only P2 unit adjacent to hero
            //        Note: P2 TANK is NOT adjacent to hero (moved to 2,4) to avoid ambiguous attacker
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p2Tank = createTank("p2_tank", p2, new Position(2, 4), 5);  // NOT adjacent to hero
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p2Tank, p2Attacker), p2);

            // When: P2 attacks P1 Hero
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Hero takes full 3 damage (no friendly TANK to protect)
            Unit heroAfter = findUnitById(result, "p1_hero");
            Unit tankAfter = findUnitById(result, "p2_tank");

            assertEquals(2, heroAfter.getHp(), "Hero should take 3 damage");
            assertEquals(5, tankAfter.getHp(), "Enemy TANK should be unaffected");
        }
    }

    // ========== Guardian Edge Cases ==========

    @Nested
    @DisplayName("Guardian Edge Cases")
    class GuardianEdgeCases {

        @Test
        @DisplayName("GRD6: Multiple TANKs - lowest ID intercepts")
        void multipleTanksLowestIdIntercepts() {
            // Given: P1 Hero at (2,2), P1 TANK_B at (2,3), P1 TANK_A at (3,2)
            //        P2 attacker at (2,1)
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1TankB = createTank("p1_tank_b", p1, new Position(2, 3), 5);
            Unit p1TankA = createTank("p1_tank_a", p1, new Position(3, 2), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1TankB, p1TankA, p2Attacker), p2);

            // When: P2 attacks P1 Hero
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: TANK_A (lower ID) intercepts, TANK_B unaffected
            Unit heroAfter = findUnitById(result, "p1_hero");
            Unit tankAAfter = findUnitById(result, "p1_tank_a");
            Unit tankBAfter = findUnitById(result, "p1_tank_b");

            assertEquals(5, heroAfter.getHp(), "Hero should take no damage");
            assertEquals(2, tankAAfter.getHp(), "TANK_A (lower ID) should take damage");
            assertEquals(5, tankBAfter.getHp(), "TANK_B should be unaffected");
        }

        @Test
        @DisplayName("GRD7: TANK intercepts lethal damage and dies")
        void tankInterceptsLethalDamage() {
            // Given: P1 Hero at (2,2), P1 TANK with 2 HP at (2,3)
            //        P2 attacker with 5 ATK at (2,1)
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1Tank = createTank("p1_tank", p1, new Position(2, 3), 2);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 5);

            GameState state = createState(Arrays.asList(p1Hero, p1Tank, p2Attacker), p2);

            // When: P2 attacks P1 Hero with 5 damage
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: TANK dies (2-5=-3), Hero protected
            Unit heroAfter = findUnitById(result, "p1_hero");
            Unit tankAfter = findUnitById(result, "p1_tank");

            assertEquals(5, heroAfter.getHp(), "Hero should take no damage");
            assertFalse(tankAfter.isAlive(), "TANK should be dead after taking lethal damage");
            assertEquals(-3, tankAfter.getHp(), "TANK HP should be -3");
        }

        @Test
        @DisplayName("GRD8: TANK protects non-Hero allies (other minions)")
        void tankProtectsOtherMinions() {
            // Given: P1 ARCHER at (2,2), P1 TANK at (2,3)
            //        P2 attacker at (2,1)
            Unit p1Archer = createArcher("p1_archer", p1, new Position(2, 2), 3);
            Unit p1Tank = createTank("p1_tank", p1, new Position(2, 3), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 2);

            GameState state = createState(Arrays.asList(p1Archer, p1Tank, p2Attacker), p2);

            // When: P2 attacks P1 ARCHER
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_archer");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: TANK intercepts, ARCHER protected
            Unit archerAfter = findUnitById(result, "p1_archer");
            Unit tankAfter = findUnitById(result, "p1_tank");

            assertEquals(3, archerAfter.getHp(), "ARCHER should take no damage");
            assertEquals(3, tankAfter.getHp(), "TANK should take 2 damage (5-2=3)");
        }

        @Test
        @DisplayName("GRD9: Non-TANK minions do not intercept")
        void nonTankMinionsDoNotIntercept() {
            // Given: P1 Hero at (2,2), P1 ARCHER at (2,3), P1 ASSASSIN at (3,2)
            //        P2 attacker at (2,1)
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1Archer = createArcher("p1_archer", p1, new Position(2, 3), 3);
            Unit p1Assassin = createAssassin("p1_assassin", p1, new Position(3, 2), 2);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1Archer, p1Assassin, p2Attacker), p2);

            // When: P2 attacks P1 Hero
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Hero takes full damage (no TANK to protect)
            Unit heroAfter = findUnitById(result, "p1_hero");
            Unit archerAfter = findUnitById(result, "p1_archer");
            Unit assassinAfter = findUnitById(result, "p1_assassin");

            assertEquals(2, heroAfter.getHp(), "Hero should take 3 damage");
            assertEquals(3, archerAfter.getHp(), "ARCHER should be unaffected");
            assertEquals(2, assassinAfter.getHp(), "ASSASSIN should be unaffected");
        }
    }

    // ========== Guardian with MOVE_AND_ATTACK ==========

    @Nested
    @DisplayName("Guardian with MOVE_AND_ATTACK")
    class GuardianMoveAndAttack {

        @Test
        @DisplayName("GRD10: TANK intercepts MOVE_AND_ATTACK damage")
        void tankInterceptsMoveAndAttack() {
            // Given: P1 Hero at (2,3), P1 TANK at (2,4) adjacent to Hero
            //        P2 attacker at (2,1), will move to (2,2) and attack Hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 3), 5);
            Unit p1Tank = createTank("p1_tank", p1, new Position(2, 4), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1Tank, p2Attacker), p2);

            // When: P2 MOVE_AND_ATTACK to (2,2) targeting Hero
            Action moveAndAttack = new Action(ActionType.MOVE_AND_ATTACK, p2,
                                               new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, moveAndAttack);

            // Then: TANK intercepts the damage
            Unit heroAfter = findUnitById(result, "p1_hero");
            Unit tankAfter = findUnitById(result, "p1_tank");

            assertEquals(5, heroAfter.getHp(), "Hero should take no damage");
            assertEquals(2, tankAfter.getHp(), "TANK should take 3 damage");
        }
    }

    // ========== Guardian Adjacency Tests ==========

    @Nested
    @DisplayName("Guardian Adjacency")
    class GuardianAdjacency {

        @Test
        @DisplayName("GRD11: TANK adjacent orthogonally (up) intercepts")
        void tankAdjacentUp() {
            // Given: P1 Hero at (2,2), P1 TANK at (2,3) - up from hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1Tank = createTank("p1_tank", p1, new Position(2, 3), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1Tank, p2Attacker), p2);
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            assertEquals(5, findUnitById(result, "p1_hero").getHp(), "Hero protected");
            assertEquals(2, findUnitById(result, "p1_tank").getHp(), "TANK took damage");
        }

        @Test
        @DisplayName("GRD12: TANK adjacent orthogonally (down) intercepts")
        void tankAdjacentDown() {
            // Given: P1 Hero at (2,2), P1 TANK at (2,1) - down from hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1Tank = createTank("p1_tank", p1, new Position(2, 1), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 3), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1Tank, p2Attacker), p2);
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            assertEquals(5, findUnitById(result, "p1_hero").getHp(), "Hero protected");
            assertEquals(2, findUnitById(result, "p1_tank").getHp(), "TANK took damage");
        }

        @Test
        @DisplayName("GRD13: TANK adjacent orthogonally (left) intercepts")
        void tankAdjacentLeft() {
            // Given: P1 Hero at (2,2), P1 TANK at (1,2) - left of hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1Tank = createTank("p1_tank", p1, new Position(1, 2), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(3, 2), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1Tank, p2Attacker), p2);
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            assertEquals(5, findUnitById(result, "p1_hero").getHp(), "Hero protected");
            assertEquals(2, findUnitById(result, "p1_tank").getHp(), "TANK took damage");
        }

        @Test
        @DisplayName("GRD14: TANK adjacent orthogonally (right) intercepts")
        void tankAdjacentRight() {
            // Given: P1 Hero at (2,2), P1 TANK at (3,2) - right of hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1Tank = createTank("p1_tank", p1, new Position(3, 2), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(1, 2), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1Tank, p2Attacker), p2);
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            assertEquals(5, findUnitById(result, "p1_hero").getHp(), "Hero protected");
            assertEquals(2, findUnitById(result, "p1_tank").getHp(), "TANK took damage");
        }

        @Test
        @DisplayName("GRD15: TANK diagonal does NOT intercept")
        void tankDiagonalDoesNotIntercept() {
            // Given: P1 Hero at (2,2), P1 TANK at (3,3) - diagonal from hero
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1Tank = createTank("p1_tank", p1, new Position(3, 3), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1Tank, p2Attacker), p2);
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            assertEquals(2, findUnitById(result, "p1_hero").getHp(), "Hero took damage (diagonal TANK doesn't protect)");
            assertEquals(5, findUnitById(result, "p1_tank").getHp(), "TANK unaffected");
        }

        @Test
        @DisplayName("GRD16: TANK 2 tiles away does NOT intercept")
        void tankTwoTilesAwayDoesNotIntercept() {
            // Given: P1 Hero at (2,2), P1 TANK at (2,4) - 2 tiles away
            Unit p1Hero = createHero("p1_hero", p1, new Position(2, 2), 5);
            Unit p1Tank = createTank("p1_tank", p1, new Position(2, 4), 5);
            Unit p2Attacker = createUnit("p2_attacker", p2, new Position(2, 1), 10, 3);

            GameState state = createState(Arrays.asList(p1Hero, p1Tank, p2Attacker), p2);
            Action attack = new Action(ActionType.ATTACK, p2, new Position(2, 2), "p1_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            assertEquals(2, findUnitById(result, "p1_hero").getHp(), "Hero took damage");
            assertEquals(5, findUnitById(result, "p1_tank").getHp(), "TANK unaffected (too far)");
        }
    }
}
