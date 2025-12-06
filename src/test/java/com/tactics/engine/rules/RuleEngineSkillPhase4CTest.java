package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.*;
import com.tactics.engine.skill.SkillRegistry;
import com.tactics.engine.util.RngProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for V3 Hero Skill System - Phase 4C.
 * Movement skills: Heroic Leap, Spectral Blades, Smoke Bomb, Warp Beacon.
 */
public class RuleEngineSkillPhase4CTest {

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // Helper methods

    private Unit createHero(String id, PlayerId owner, int hp, int attack, Position pos, HeroClass heroClass, String skillId) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0,  // cooldown = 0 (ready)
            0, false, false, false, 0, null,
            0, false, null,
            0, 0);
    }

    private Unit createHeroWithSkillState(String id, PlayerId owner, int hp, int attack, Position pos,
                                           HeroClass heroClass, String skillId, Map<String, Object> skillState) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0,
            0, false, false, false, 0, skillState,
            0, false, null,
            0, 0);
    }

    private Unit createMinion(String id, PlayerId owner, int hp, int attack, Position pos, MinionType minionType) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.MINION, minionType, null, hp,
            null, 0,
            0, false, false, false, 0, null,
            0, false, null,
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

    private Unit findUnit(GameState state, String id) {
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(id)) {
                return u;
            }
        }
        return null;
    }

    // =========================================================================
    // Heroic Leap Tests (SW-Series)
    // =========================================================================

    @Nested
    @DisplayName("SW - Heroic Leap Skill")
    class HeroicLeapSkill {

        @Test
        @DisplayName("SW1: Heroic Leap moves hero to target tile")
        void testHeroicLeapMovesToTarget() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP);
            Unit enemy = createMinion("m2", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(2, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertEquals(2, heroAfter.getPosition().getX());
            assertEquals(2, heroAfter.getPosition().getY());
        }

        @Test
        @DisplayName("SW2: Heroic Leap deals 2 damage to adjacent enemies on landing")
        void testHeroicLeapDealsAdjacentDamage() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP);
            Unit enemy1 = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(2, 1), MinionType.ARCHER);
            Unit enemy2 = createMinion("m2", PlayerId.PLAYER_2, 8, 2, new Position(1, 2), MinionType.TANK);

            GameState state = createGameState(Arrays.asList(hero, enemy1, enemy2), PlayerId.PLAYER_1);

            // Leap to (2, 2) - enemies at (2,1) and (1,2) are adjacent
            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(2, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit enemy1After = findUnit(result, "m1");
            Unit enemy2After = findUnit(result, "m2");
            assertEquals(6, enemy1After.getHp(), "Adjacent enemy should take 2 damage");
            assertEquals(6, enemy2After.getHp(), "Adjacent enemy should take 2 damage");
        }

        @Test
        @DisplayName("SW3: Heroic Leap does not damage non-adjacent enemies")
        void testHeroicLeapNoNonAdjacentDamage() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // Leap to (2, 2) - enemy at (4,4) is NOT adjacent
            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(2, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit enemyAfter = findUnit(result, "m1");
            assertEquals(8, enemyAfter.getHp(), "Non-adjacent enemy should NOT take damage");
        }

        @Test
        @DisplayName("SW4: Cannot leap to occupied tile")
        void testCannotLeapToOccupiedTile() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(2, 2), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // Try to leap to occupied tile (2, 2)
            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(2, 2), null);
            ValidationResult validation = ruleEngine.validateAction(state, action);

            assertFalse(validation.isValid(), "Should not be able to leap to occupied tile");
            // Error message should indicate tile is blocked
            assertNotNull(validation.getErrorMessage());
        }

        @Test
        @DisplayName("SW5: Heroic Leap sets cooldown to 2")
        void testHeroicLeapSetsCooldown() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP);
            Unit enemy = createMinion("m2", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(2, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertEquals(2, heroAfter.getSkillCooldown());
        }
    }

    // =========================================================================
    // Spectral Blades Tests (SH-Series)
    // =========================================================================

    @Nested
    @DisplayName("SH - Spectral Blades Skill")
    class SpectralBladesSkill {

        @Test
        @DisplayName("SH3: Spectral Blades deals 1 damage in a line")
        void testSpectralBladesDamagesLine() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(2, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // Fire blades in direction (3, 0) - hits enemy at (2, 0)
            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(3, 0), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit enemyAfter = findUnit(result, "m1");
            assertEquals(7, enemyAfter.getHp(), "Enemy in line should take 1 damage");
        }

        @Test
        @DisplayName("SH4: Spectral Blades pierces through enemies")
        void testSpectralBladesPierces() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            // Use two ARCHER enemies (not TANK) to avoid Guardian intercept
            Unit enemy1 = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(1, 0), MinionType.ARCHER);
            Unit enemy2 = createMinion("m2", PlayerId.PLAYER_2, 8, 2, new Position(2, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy1, enemy2), PlayerId.PLAYER_1);

            // Fire blades in direction (3, 0) - should hit BOTH enemies
            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(3, 0), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit enemy1After = findUnit(result, "m1");
            Unit enemy2After = findUnit(result, "m2");
            assertEquals(7, enemy1After.getHp(), "First enemy should take 1 damage");
            assertEquals(7, enemy2After.getHp(), "Second enemy should take 1 damage (pierces)");
        }

        @Test
        @DisplayName("SH5: Spectral Blades has range 3")
        void testSpectralBladesRange() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(3, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // Fire blades to (3, 0) - enemy is exactly at range 3
            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(3, 0), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit enemyAfter = findUnit(result, "m1");
            assertEquals(7, enemyAfter.getHp(), "Enemy at range 3 should be hit");
        }

        @Test
        @DisplayName("SH6: Spectral Blades must be orthogonal")
        void testSpectralBladesMustBeOrthogonal() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit enemy = createMinion("m2", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // Try to fire diagonal (invalid)
            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(2, 2), null);
            ValidationResult validation = ruleEngine.validateAction(state, action);

            assertFalse(validation.isValid(), "Diagonal target should be invalid");
            assertTrue(validation.getErrorMessage().contains("straight line"));
        }

        @Test
        @DisplayName("SH7: Spectral Blades does not damage friendly units")
        void testSpectralBladesNoFriendlyFire() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit friendly = createMinion("f1", PlayerId.PLAYER_1, 8, 2, new Position(1, 0), MinionType.ARCHER);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(2, 0), MinionType.TANK);

            GameState state = createGameState(Arrays.asList(hero, friendly, enemy), PlayerId.PLAYER_1);

            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(3, 0), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit friendlyAfter = findUnit(result, "f1");
            Unit enemyAfter = findUnit(result, "m1");
            assertEquals(8, friendlyAfter.getHp(), "Friendly unit should NOT take damage");
            assertEquals(7, enemyAfter.getHp(), "Enemy should take 1 damage");
        }
    }

    // =========================================================================
    // Smoke Bomb Tests (SR-Series)
    // =========================================================================

    @Nested
    @DisplayName("SR - Smoke Bomb Skill")
    class SmokeBombSkill {

        @Test
        @DisplayName("SR1: Smoke Bomb teleports to target tile")
        void testSmokeBombTeleports() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SMOKE_BOMB);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(2, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertEquals(2, heroAfter.getPosition().getX());
            assertEquals(2, heroAfter.getPosition().getY());
        }

        @Test
        @DisplayName("SR2: Smoke Bomb grants invisible for 1 round")
        void testSmokeBombGrantsInvisible() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SMOKE_BOMB);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(2, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertTrue(heroAfter.isInvisible(), "Hero should be invisible after Smoke Bomb");
        }

        @Test
        @DisplayName("SR3: Invisible prevents targeting by enemies")
        void testInvisiblePreventsTargeting() {
            // Create invisible hero
            Unit hero = new Unit("h1", PlayerId.PLAYER_1, 10, 3, 2, 1, new Position(2, 2), true,
                UnitCategory.HERO, null, HeroClass.ROGUE, 10,
                SkillRegistry.ROGUE_SMOKE_BOMB, 2,  // cooldown = 2 (just used)
                0, true, false, false, 0, null,  // invisible = true
                0, false, null, 0, 0);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(2, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_2);

            // Enemy tries to attack invisible hero
            Action action = new Action(ActionType.ATTACK, PlayerId.PLAYER_2, new Position(2, 2), "h1");
            ValidationResult validation = ruleEngine.validateAction(state, action);

            assertFalse(validation.isValid(), "Should not be able to target invisible unit");
            assertTrue(validation.getErrorMessage().contains("invisible"));
        }

        @Test
        @DisplayName("SR5: Smoke Bomb blinds adjacent enemies at original position")
        void testSmokeBombBlindsAdjacent() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SMOKE_BOMB);
            Unit enemy1 = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(2, 1), MinionType.ARCHER);
            Unit enemy2 = createMinion("m2", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.TANK);

            GameState state = createGameState(Arrays.asList(hero, enemy1, enemy2), PlayerId.PLAYER_1);

            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(0, 0), null);
            GameState result = ruleEngine.applyAction(state, action);

            // Check enemy1 (adjacent) has BLIND buff
            List<BuffInstance> enemy1Buffs = result.getUnitBuffs().get("m1");
            assertNotNull(enemy1Buffs, "Adjacent enemy should have buffs");
            boolean hasBlind = enemy1Buffs.stream().anyMatch(b -> b.getType() == BuffType.BLIND);
            assertTrue(hasBlind, "Adjacent enemy should have BLIND buff");

            // Check enemy2 (not adjacent) does NOT have BLIND buff
            List<BuffInstance> enemy2Buffs = result.getUnitBuffs().get("m2");
            boolean enemy2HasBlind = enemy2Buffs != null &&
                enemy2Buffs.stream().anyMatch(b -> b.getType() == BuffType.BLIND);
            assertFalse(enemy2HasBlind, "Non-adjacent enemy should NOT have BLIND buff");
        }

        @Test
        @DisplayName("SR6: BLIND prevents unit from attacking")
        void testBlindPreventsAttack() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SMOKE_BOMB);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(2, 1), MinionType.ARCHER);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs.put("m1", Arrays.asList(BuffFactory.create(BuffType.BLIND, "h1")));

            GameState state = createGameStateWithBuffs(Arrays.asList(hero, enemy), PlayerId.PLAYER_2, buffs);

            // Blinded enemy tries to attack
            Action action = new Action(ActionType.ATTACK, PlayerId.PLAYER_2, new Position(2, 2), "h1");
            ValidationResult validation = ruleEngine.validateAction(state, action);

            assertFalse(validation.isValid(), "Blinded unit should not be able to attack");
            assertTrue(validation.getErrorMessage().contains("blind"));
        }
    }

    // =========================================================================
    // Warp Beacon Tests (SMG-Series)
    // =========================================================================

    @Nested
    @DisplayName("SMG - Warp Beacon Skill")
    class WarpBeaconSkill {

        @Test
        @DisplayName("SMG5: First use places beacon")
        void testWarpBeaconPlacesBeacon() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WARP_BEACON);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(2, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            // Hero should still be at original position
            assertEquals(0, heroAfter.getPosition().getX());
            assertEquals(0, heroAfter.getPosition().getY());
            // Beacon should be stored in skillState
            assertNotNull(heroAfter.getSkillState());
            assertEquals(2, heroAfter.getSkillState().get("beacon_x"));
            assertEquals(2, heroAfter.getSkillState().get("beacon_y"));
        }

        @Test
        @DisplayName("SMG7: Second use teleports to beacon")
        void testWarpBeaconTeleports() {
            Map<String, Object> skillState = new HashMap<>();
            skillState.put("beacon_x", 3);
            skillState.put("beacon_y", 3);

            Unit hero = createHeroWithSkillState("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WARP_BEACON, skillState);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // Use skill again (doesn't need specific target since beacon exists)
            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", null, null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertEquals(3, heroAfter.getPosition().getX());
            assertEquals(3, heroAfter.getPosition().getY());
        }

        @Test
        @DisplayName("SMG8: Teleport triggers cooldown")
        void testWarpBeaconTeleportTriggersCooldown() {
            Map<String, Object> skillState = new HashMap<>();
            skillState.put("beacon_x", 3);
            skillState.put("beacon_y", 3);

            Unit hero = createHeroWithSkillState("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WARP_BEACON, skillState);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", null, null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertEquals(2, heroAfter.getSkillCooldown(), "Teleport should trigger cooldown");
        }

        @Test
        @DisplayName("SMG8b: Placing beacon does NOT trigger cooldown")
        void testWarpBeaconPlaceNoCooldown() {
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WARP_BEACON);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", new Position(2, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertEquals(0, heroAfter.getSkillCooldown(), "Placing beacon should NOT trigger cooldown");
        }

        @Test
        @DisplayName("SMG9: Beacon removed after teleport")
        void testWarpBeaconRemovedAfterTeleport() {
            Map<String, Object> skillState = new HashMap<>();
            skillState.put("beacon_x", 3);
            skillState.put("beacon_y", 3);

            Unit hero = createHeroWithSkillState("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WARP_BEACON, skillState);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", null, null);
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertTrue(heroAfter.getSkillState().isEmpty(), "Beacon should be cleared after teleport");
        }

        @Test
        @DisplayName("SMG10: Mage death removes beacon")
        void testWarpBeaconRemovedOnDeath() {
            Map<String, Object> skillState = new HashMap<>();
            skillState.put("beacon_x", 3);
            skillState.put("beacon_y", 3);

            Unit hero = createHeroWithSkillState("h1", PlayerId.PLAYER_1, 1, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WARP_BEACON, skillState);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 5, new Position(0, 1), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_2);

            // Enemy attacks and kills Mage
            Action action = new Action(ActionType.ATTACK, PlayerId.PLAYER_2, new Position(0, 0), "h1");
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertFalse(heroAfter.isAlive(), "Mage should be dead");
            // Beacon should be cleared on death (skillState is null or empty)
            assertTrue(heroAfter.getSkillState() == null || heroAfter.getSkillState().isEmpty(),
                "Beacon should be cleared on death");
        }
    }

    // =========================================================================
    // Round End Tests
    // =========================================================================

    @Nested
    @DisplayName("Round End - Invisible Expiry")
    class RoundEndInvisibleExpiry {

        @Test
        @DisplayName("Invisible expires at round end")
        void testInvisibleExpiresAtRoundEnd() {
            // Create invisible hero
            Unit hero = new Unit("h1", PlayerId.PLAYER_1, 10, 3, 2, 1, new Position(2, 2), true,
                UnitCategory.HERO, null, HeroClass.ROGUE, 10,
                SkillRegistry.ROGUE_SMOKE_BOMB, 2,
                0, true, false, false, 0, null,  // invisible = true
                0, false, null, 0, 0);

            // Test withRoundEndReset clears invisible
            Unit heroAfter = hero.withRoundEndReset();
            assertFalse(heroAfter.isInvisible(), "Invisible should be cleared at round end");
        }
    }

    // =========================================================================
    // Invisible Break Tests
    // =========================================================================

    @Nested
    @DisplayName("Invisible Break on Action")
    class InvisibleBreakOnAction {

        @Test
        @DisplayName("Attacking breaks invisibility")
        void testAttackBreaksInvisibility() {
            // Create invisible hero
            Unit hero = new Unit("h1", PlayerId.PLAYER_1, 10, 3, 2, 1, new Position(2, 2), true,
                UnitCategory.HERO, null, HeroClass.ROGUE, 10,
                SkillRegistry.ROGUE_SMOKE_BOMB, 2,
                0, true, false, false, 0, null,  // invisible = true
                0, false, null, 0, 0);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(2, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // Invisible hero attacks - should break invisibility
            Action action = new Action(ActionType.ATTACK, PlayerId.PLAYER_1, new Position(2, 3), "m1");
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertFalse(heroAfter.isInvisible(), "Attacking should break invisibility");
        }

        @Test
        @DisplayName("Using skill breaks invisibility (except Smoke Bomb)")
        void testSkillBreaksInvisibility() {
            // Create invisible hero with Spirit Hawk skill
            Unit hero = new Unit("h1", PlayerId.PLAYER_1, 10, 3, 2, 1, new Position(2, 2), true,
                UnitCategory.HERO, null, HeroClass.HUNTRESS, 10,
                SkillRegistry.HUNTRESS_SPIRIT_HAWK, 0,
                0, true, false, false, 0, null,  // invisible = true
                0, false, null, 0, 0);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(4, 2), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // Use Spirit Hawk skill - should break invisibility
            Action action = Action.useSkill(PlayerId.PLAYER_1, "h1", null, "m1");
            GameState result = ruleEngine.applyAction(state, action);

            Unit heroAfter = findUnit(result, "h1");
            assertFalse(heroAfter.isInvisible(), "Using skill should break invisibility");
        }
    }

    // =========================================================================
    // BLIND and MOVE_AND_ATTACK Tests
    // =========================================================================

    @Nested
    @DisplayName("BLIND with MOVE_AND_ATTACK")
    class BlindMoveAndAttack {

        @Test
        @DisplayName("BLIND prevents MOVE_AND_ATTACK")
        void testBlindPreventsMoveAndAttack() {
            // Hero at (0,0) with moveRange=2, enemy at (2,0), move to (1,0) - within moveRange
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion("m1", PlayerId.PLAYER_2, 8, 2, new Position(2, 0), MinionType.ARCHER);

            Map<String, List<BuffInstance>> buffs = new HashMap<>();
            buffs.put("h1", Arrays.asList(BuffFactory.create(BuffType.BLIND, "m1")));

            GameState state = createGameStateWithBuffs(Arrays.asList(hero, enemy), PlayerId.PLAYER_1, buffs);

            // Blinded hero tries to MOVE_AND_ATTACK (move from 0,0 to 1,0 - distance 1, then attack enemy at 2,0)
            Action action = new Action(ActionType.MOVE_AND_ATTACK, PlayerId.PLAYER_1, new Position(1, 0), "m1");
            ValidationResult validation = ruleEngine.validateAction(state, action);

            assertFalse(validation.isValid(), "Blinded unit should not be able to MOVE_AND_ATTACK");
            assertTrue(validation.getErrorMessage().contains("blind"), "Error message should mention 'blind': " + validation.getErrorMessage());
        }

        @Test
        @DisplayName("Cannot MOVE_AND_ATTACK invisible target")
        void testCannotMoveAndAttackInvisible() {
            // Hero at (0,0) with moveRange=2, invisible enemy at (2,0), move to (1,0) - within moveRange
            Unit hero = createHero("h1", PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            // Create invisible enemy at (2,0)
            Unit enemy = new Unit("m1", PlayerId.PLAYER_2, 8, 2, 2, 1, new Position(2, 0), true,
                UnitCategory.MINION, MinionType.ARCHER, null, 8,
                null, 0,
                0, true, false, false, 0, null,  // invisible = true
                0, false, null, 0, 0);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // Try to MOVE_AND_ATTACK invisible target (move from 0,0 to 1,0, attack enemy at 2,0)
            Action action = new Action(ActionType.MOVE_AND_ATTACK, PlayerId.PLAYER_1, new Position(1, 0), "m1");
            ValidationResult validation = ruleEngine.validateAction(state, action);

            assertFalse(validation.isValid(), "Should not be able to MOVE_AND_ATTACK invisible target");
            assertTrue(validation.getErrorMessage().contains("invisible"), "Error message should mention 'invisible': " + validation.getErrorMessage());
        }
    }
}
