package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.*;
import com.tactics.engine.skill.SkillRegistry;
import com.tactics.engine.util.GameStateFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SC-Series: Skill Cooldown Mechanics Tests
 * Tests skill cooldown initialization, decrement, and validation.
 */
@DisplayName("SC-Series: Skill Cooldown Mechanics")
public class RuleEngineSkillCooldownTest {

    private RuleEngine ruleEngine;

    private static final String P1_HERO = "p1_hero";
    private static final String P2_HERO = "p2_hero";
    private static final String P1_MINION = "p1_minion";
    private static final String P2_MINION = "p2_minion";

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Unit createHero(String id, PlayerId owner, int hp, int attack, Position pos,
                            HeroClass heroClass, String skillId, int cooldown) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, cooldown,
            0, false, false, false, 0, null,
            0, false, null,
            0, 0);
    }

    private Unit createMinion(String id, PlayerId owner, int hp, int attack, Position pos,
                              MinionType minionType) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.MINION, minionType, null, hp,
            null, 0,
            0, false, false, false, 0, null,
            0, false, null,
            0, 0);
    }

    private Unit createMinionWithActionsUsed(String id, PlayerId owner, int hp, int attack,
                                              Position pos, MinionType minionType, int actionsUsed) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.MINION, minionType, null, hp,
            null, 0,
            0, false, false, false, 0, null,
            actionsUsed, false, null,
            0, 0);
    }

    private GameState createGameState(List<Unit> units, PlayerId currentPlayer) {
        return new GameState(
            new Board(5, 5),
            units,
            currentPlayer,
            false, null,
            new HashMap<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            1, null
        );
    }

    private GameState createGameStateWithBuffs(List<Unit> units, PlayerId currentPlayer,
                                                Map<String, List<BuffInstance>> unitBuffs) {
        return new GameState(
            new Board(5, 5),
            units,
            currentPlayer,
            false, null,
            unitBuffs,
            new ArrayList<>(),
            new ArrayList<>(),
            1, null
        );
    }

    private Unit findUnit(GameState state, String unitId) {
        return state.getUnits().stream()
            .filter(u -> u.getId().equals(unitId))
            .findFirst()
            .orElse(null);
    }

    // =========================================================================
    // SC1: Skill usable when cooldown = 0
    // =========================================================================

    @Nested
    @DisplayName("SC1-SC3: Basic Cooldown Mechanics")
    class BasicCooldownTests {

        @Test
        @DisplayName("SC1: Skill usable when cooldown = 0")
        void skillUsableWhenCooldownZero() {
            // Given: Hero with cooldown = 0
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE, 0);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Validate USE_SKILL action
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid
            assertTrue(result.isValid(), "Skill should be usable when cooldown = 0");
        }

        @Test
        @DisplayName("SC2: Skill NOT usable when cooldown > 0")
        void skillNotUsableWhenCooldownGreaterThanZero() {
            // Given: Hero with cooldown = 1
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE, 1);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Validate USE_SKILL action
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid with cooldown message
            assertFalse(result.isValid(), "Skill should NOT be usable when cooldown > 0");
            assertTrue(result.getErrorMessage().toLowerCase().contains("cooldown"),
                "Error message should mention cooldown");
        }

        @Test
        @DisplayName("SC3: Using skill sets cooldown to 2")
        void usingSkillSetsCooldownTo2() {
            // Given: Hero with cooldown = 0
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE, 0);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Use skill
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Cooldown should be 2
            Unit updatedHero = findUnit(result, P1_HERO);
            assertEquals(2, updatedHero.getSkillCooldown(),
                "Skill cooldown should be set to 2 after use");
        }
    }

    // =========================================================================
    // SC4-SC6: Cooldown Decrement Tests
    // =========================================================================

    @Nested
    @DisplayName("SC4-SC6: Cooldown Decrement")
    class CooldownDecrementTests {

        @Test
        @DisplayName("SC4: Cooldown decrements at round end")
        void cooldownDecrementsAtRoundEnd() {
            // Given: Hero with cooldown = 2, all units have acted
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE, 2);
            Unit p1Minion = createMinionWithActionsUsed(P1_MINION, PlayerId.PLAYER_1, 5, 2,
                new Position(1, 0), MinionType.TANK, 1);
            Unit p2Hero = createHero(P2_HERO, PlayerId.PLAYER_2, 10, 3, new Position(4, 4),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST, 0);
            Unit p2Minion = createMinionWithActionsUsed(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 4), MinionType.ARCHER, 1);

            // Hero uses END_TURN to trigger round end
            Unit heroActed = new Unit(P1_HERO, PlayerId.PLAYER_1, 10, 3, 2, 1, new Position(0, 0), true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, 10,
                SkillRegistry.WARRIOR_ENDURE, 2,
                0, false, false, false, 0, null,
                1, false, null,  // actionsUsed = 1
                0, 0);
            Unit p2HeroActed = new Unit(P2_HERO, PlayerId.PLAYER_2, 10, 3, 2, 1, new Position(4, 4), true,
                UnitCategory.HERO, null, HeroClass.MAGE, 10,
                SkillRegistry.MAGE_ELEMENTAL_BLAST, 0,
                0, false, false, false, 0, null,
                1, false, null,
                0, 0);

            GameState state = createGameState(
                Arrays.asList(heroActed, p1Minion, p2HeroActed, p2Minion), PlayerId.PLAYER_1);

            // When: END_TURN triggers round end (all units have acted)
            Action endTurn = Action.endTurn(P1_HERO);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Cooldown should decrement from 2 to 1
            Unit updatedHero = findUnit(result, P1_HERO);
            assertEquals(1, updatedHero.getSkillCooldown(),
                "Cooldown should decrement from 2 to 1 at round end");
        }

        @Test
        @DisplayName("SC5: Cooldown does not go below 0")
        void cooldownDoesNotGoBelowZero() {
            // Given: Hero with cooldown = 0, all units have acted
            Unit heroActed = new Unit(P1_HERO, PlayerId.PLAYER_1, 10, 3, 2, 1, new Position(0, 0), true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, 10,
                SkillRegistry.WARRIOR_ENDURE, 0,  // Already at 0
                0, false, false, false, 0, null,
                1, false, null,
                0, 0);
            Unit p1Minion = createMinionWithActionsUsed(P1_MINION, PlayerId.PLAYER_1, 5, 2,
                new Position(1, 0), MinionType.TANK, 1);
            Unit p2HeroActed = new Unit(P2_HERO, PlayerId.PLAYER_2, 10, 3, 2, 1, new Position(4, 4), true,
                UnitCategory.HERO, null, HeroClass.MAGE, 10,
                SkillRegistry.MAGE_ELEMENTAL_BLAST, 0,
                0, false, false, false, 0, null,
                1, false, null,
                0, 0);
            Unit p2Minion = createMinionWithActionsUsed(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 4), MinionType.ARCHER, 1);

            GameState state = createGameState(
                Arrays.asList(heroActed, p1Minion, p2HeroActed, p2Minion), PlayerId.PLAYER_1);

            // When: Round end
            Action endTurn = Action.endTurn(P1_HERO);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Cooldown should stay at 0
            Unit updatedHero = findUnit(result, P1_HERO);
            assertEquals(0, updatedHero.getSkillCooldown(),
                "Cooldown should not go below 0");
        }

        @Test
        @DisplayName("SC6: Cooldown tracked per hero independently")
        void cooldownTrackedPerHeroIndependently() {
            // Given: Two heroes with different cooldowns, all units have acted
            Unit hero1 = new Unit(P1_HERO, PlayerId.PLAYER_1, 10, 3, 2, 1, new Position(0, 0), true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, 10,
                SkillRegistry.WARRIOR_ENDURE, 2,  // Cooldown 2
                0, false, false, false, 0, null,
                1, false, null,
                0, 0);
            Unit p1Minion = createMinionWithActionsUsed(P1_MINION, PlayerId.PLAYER_1, 5, 2,
                new Position(1, 0), MinionType.TANK, 1);
            Unit hero2 = new Unit(P2_HERO, PlayerId.PLAYER_2, 10, 3, 2, 1, new Position(4, 4), true,
                UnitCategory.HERO, null, HeroClass.MAGE, 10,
                SkillRegistry.MAGE_ELEMENTAL_BLAST, 1,  // Cooldown 1
                0, false, false, false, 0, null,
                1, false, null,
                0, 0);
            Unit p2Minion = createMinionWithActionsUsed(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 4), MinionType.ARCHER, 1);

            GameState state = createGameState(
                Arrays.asList(hero1, p1Minion, hero2, p2Minion), PlayerId.PLAYER_1);

            // When: Round end
            Action endTurn = Action.endTurn(P1_HERO);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Each hero's cooldown should decrement independently
            Unit updatedHero1 = findUnit(result, P1_HERO);
            Unit updatedHero2 = findUnit(result, P2_HERO);
            assertEquals(1, updatedHero1.getSkillCooldown(),
                "Hero1 cooldown should decrement from 2 to 1");
            assertEquals(0, updatedHero2.getSkillCooldown(),
                "Hero2 cooldown should decrement from 1 to 0");
        }
    }

    // =========================================================================
    // SC7-SC8: Special Cooldown Cases
    // =========================================================================

    @Nested
    @DisplayName("SC7-SC8: Special Cooldown Cases")
    class SpecialCooldownTests {

        @Test
        @DisplayName("SC7: Dead hero cooldown still decrements (for respawn scenarios)")
        void deadHeroCooldownStillDecrements() {
            // Given: Dead hero with cooldown = 2
            Unit deadHero = new Unit(P1_HERO, PlayerId.PLAYER_1, 0, 3, 2, 1, new Position(0, 0), false,
                UnitCategory.HERO, null, HeroClass.WARRIOR, 10,
                SkillRegistry.WARRIOR_ENDURE, 2,
                0, false, false, false, 0, null,
                1, false, null,
                0, 0);
            Unit p1Minion = createMinionWithActionsUsed(P1_MINION, PlayerId.PLAYER_1, 5, 2,
                new Position(1, 0), MinionType.TANK, 1);
            Unit hero2 = new Unit(P2_HERO, PlayerId.PLAYER_2, 10, 3, 2, 1, new Position(4, 4), true,
                UnitCategory.HERO, null, HeroClass.MAGE, 10,
                SkillRegistry.MAGE_ELEMENTAL_BLAST, 0,
                0, false, false, false, 0, null,
                1, false, null,
                0, 0);
            Unit p2Minion = createMinionWithActionsUsed(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 4), MinionType.ARCHER, 1);

            GameState state = createGameState(
                Arrays.asList(deadHero, p1Minion, hero2, p2Minion), PlayerId.PLAYER_1);

            // When: Round end
            Action endTurn = Action.endTurn(P2_HERO);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Dead hero's cooldown should still decrement
            Unit updatedDeadHero = findUnit(result, P1_HERO);
            assertEquals(1, updatedDeadHero.getSkillCooldown(),
                "Dead hero cooldown should still decrement at round end");
        }

        @Test
        @DisplayName("SC8: Skill cooldown reset on new match")
        void skillCooldownResetOnNewMatch() {
            // When: Create a new match
            GameState newMatch = GameStateFactory.createStandardGame();

            // Then: Hero cooldowns should be 0
            for (Unit unit : newMatch.getUnits()) {
                if (unit.getCategory() == UnitCategory.HERO) {
                    assertEquals(0, unit.getSkillCooldown(),
                        "New match hero cooldown should be 0");
                }
            }
        }
    }

    // =========================================================================
    // SC9-SC10: SLOW Buff Interaction
    // =========================================================================

    @Nested
    @DisplayName("SC9-SC10: SLOW Buff Interaction")
    class SlowBuffInteractionTests {

        @Test
        @DisplayName("SC9: SLOW buff - Skill can be used while slowed (delayed execution)")
        void slowBuffAllowsSkillUse() {
            // Given: Hero with SLOW buff
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST, 0);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 8, 2,
                new Position(3, 0), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(
                BuffFactory.create(BuffType.SLOW, "enemy")));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(hero, enemy), PlayerId.PLAYER_1, unitBuffs);

            // When: Validate skill use
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, enemy.getPosition(), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid (SLOW allows skill declaration)
            assertTrue(result.isValid(),
                "SLOW buff should allow skill use (delayed execution)");
        }

        @Test
        @DisplayName("SC10: Killed hero with cooldown remains with same cooldown")
        void killedHeroRetainsCooldown() {
            // Given: Hero with cooldown set
            Unit heroWithCooldown = new Unit(P1_HERO, PlayerId.PLAYER_1, 1, 3, 2, 1, new Position(0, 0), true,
                UnitCategory.HERO, null, HeroClass.MAGE, 10,
                SkillRegistry.MAGE_ELEMENTAL_BLAST, 2,  // Cooldown = 2
                0, false, false, false, 0, null,
                0, false, null,
                0, 0);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 8, 2,
                new Position(1, 0), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P2_MINION, new ArrayList<>());

            GameState state = createGameStateWithBuffs(
                Arrays.asList(heroWithCooldown, enemy), PlayerId.PLAYER_2, unitBuffs);

            // When: Enemy kills the hero
            Action attack = Action.attack(P2_MINION, heroWithCooldown.getPosition(), P1_HERO);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Hero is dead, cooldown is retained
            Unit deadHero = findUnit(result, P1_HERO);
            assertFalse(deadHero.isAlive(), "Hero should be dead");
            assertEquals(2, deadHero.getSkillCooldown(),
                "Cooldown should be retained when hero dies");
        }
    }

    // =========================================================================
    // SC11-SC12: Warp Beacon Special Cooldown
    // =========================================================================

    @Nested
    @DisplayName("SC11-SC12: Warp Beacon Special Cooldown")
    class WarpBeaconCooldownTests {

        @Test
        @DisplayName("SC11: Warp Beacon - First use (place) does NOT trigger cooldown")
        void warpBeaconPlaceDoesNotTriggerCooldown() {
            // Given: Mage with Warp Beacon skill, no beacon placed yet
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WARP_BEACON, 0);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(mage, enemy), PlayerId.PLAYER_1);

            // When: Place beacon at (3, 0)
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, new Position(3, 0), null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Cooldown should still be 0 (placing beacon doesn't trigger cooldown)
            Unit updatedMage = findUnit(result, P1_HERO);
            assertEquals(0, updatedMage.getSkillCooldown(),
                "Placing Warp Beacon should NOT trigger cooldown");
            // Beacon should be placed
            assertNotNull(updatedMage.getSkillState());
            assertTrue(updatedMage.getSkillState().containsKey("beacon_x"),
                "Beacon should be placed");
        }

        @Test
        @DisplayName("SC12: Warp Beacon - Second use (teleport) triggers cooldown")
        void warpBeaconTeleportTriggersCooldown() {
            // Given: Mage with beacon already placed
            Map<String, Object> skillState = new HashMap<>();
            skillState.put("beacon_x", 3);
            skillState.put("beacon_y", 0);

            Unit mage = new Unit(P1_HERO, PlayerId.PLAYER_1, 10, 3, 2, 1, new Position(0, 0), true,
                UnitCategory.HERO, null, HeroClass.MAGE, 10,
                SkillRegistry.MAGE_WARP_BEACON, 0,
                0, false, false, false, 0, skillState,
                0, false, null,
                0, 0);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(mage, enemy), PlayerId.PLAYER_1);

            // When: Teleport to beacon
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Cooldown should be 2
            Unit updatedMage = findUnit(result, P1_HERO);
            assertEquals(2, updatedMage.getSkillCooldown(),
                "Teleporting with Warp Beacon should trigger cooldown");
            // Mage should be at beacon position
            assertEquals(new Position(3, 0), updatedMage.getPosition(),
                "Mage should teleport to beacon position");
        }
    }
}
