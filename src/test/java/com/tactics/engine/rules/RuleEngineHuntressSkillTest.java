package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.model.*;
import com.tactics.engine.skill.SkillRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SH-Series: HUNTRESS Skills - Spirit Hawk and Spectral Blades Tests
 */
@DisplayName("SH-Series: Huntress Skills")
public class RuleEngineHuntressSkillTest {

    private RuleEngine ruleEngine;

    private static final String P1_HERO = "p1_hero";
    private static final String P2_HERO = "p2_hero";
    private static final String P1_MINION = "p1_minion";
    private static final String P2_MINION = "p2_minion";
    private static final String P2_MINION_2 = "p2_minion_2";
    private static final String P2_MINION_3 = "p2_minion_3";

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Unit createHero(String id, PlayerId owner, int hp, int attack, Position pos,
                            HeroClass heroClass, String skillId) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0,
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

    private GameState createGameStateWithObstacles(List<Unit> units, PlayerId currentPlayer,
                                                    List<Obstacle> obstacles) {
        return new GameState(
            new Board(5, 5),
            units,
            currentPlayer,
            false, null,
            new HashMap<>(),
            new ArrayList<>(),
            obstacles,
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
    // SH1-SH2: Spirit Hawk Tests
    // =========================================================================

    @Nested
    @DisplayName("SH1-SH2: Spirit Hawk Skill")
    class SpiritHawkTests {

        @Test
        @DisplayName("SH1: Spirit Hawk deals 2 damage at range 4")
        void spiritHawkDeals2DamageAtRange4() {
            // Given: Huntress at (0,0), enemy at range 4
            Unit huntress = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPIRIT_HAWK);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 10, 2,
                new Position(4, 0), MinionType.ARCHER);  // Distance = 4

            GameState state = createGameState(Arrays.asList(huntress, enemy), PlayerId.PLAYER_1);

            // When: Use Spirit Hawk
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, enemy.getPosition(), P2_MINION);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should take 2 damage
            assertEquals(8, findUnit(result, P2_MINION).getHp(),
                "Spirit Hawk should deal 2 damage at range 4");
        }

        @Test
        @DisplayName("SH2: Spirit Hawk can target any enemy in range (no LOS requirement)")
        void spiritHawkCanTargetThroughObstacles() {
            // Given: Huntress at (0,0), enemy at (4,0), obstacle at (2,0)
            Unit huntress = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPIRIT_HAWK);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 10, 2,
                new Position(4, 0), MinionType.ARCHER);

            List<Obstacle> obstacles = Collections.singletonList(
                new Obstacle("obs_1", new Position(2, 0)));

            GameState state = createGameStateWithObstacles(
                Arrays.asList(huntress, enemy), PlayerId.PLAYER_1, obstacles);

            // When: Use Spirit Hawk (should work through obstacle)
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, enemy.getPosition(), P2_MINION);
            ValidationResult validation = ruleEngine.validateAction(state, action);

            // Then: Should be valid (no LOS requirement)
            assertTrue(validation.isValid(),
                "Spirit Hawk should not require line of sight");

            GameState result = ruleEngine.applyAction(state, action);
            assertEquals(8, findUnit(result, P2_MINION).getHp(),
                "Spirit Hawk should deal 2 damage through obstacle");
        }

        @Test
        @DisplayName("SH2b: Spirit Hawk out of range fails")
        void spiritHawkOutOfRangeFails() {
            // Given: Huntress at (0,0), enemy at distance > 4
            Unit huntress = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPIRIT_HAWK);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 10, 2,
                new Position(4, 4), MinionType.ARCHER);  // Distance = 8

            GameState state = createGameState(Arrays.asList(huntress, enemy), PlayerId.PLAYER_1);

            // When: Try to use Spirit Hawk out of range
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, enemy.getPosition(), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "Spirit Hawk should fail when target out of range");
            assertTrue(result.getErrorMessage().toLowerCase().contains("range"),
                "Error should mention range");
        }
    }

    // =========================================================================
    // SH3-SH7: Spectral Blades Tests
    // =========================================================================

    @Nested
    @DisplayName("SH3-SH7: Spectral Blades Skill")
    class SpectralBladesTests {

        @Test
        @DisplayName("SH3: Spectral Blades deals 1 damage in a line")
        void spectralBladesDealsDamageInLine() {
            // Given: Huntress at (0,2), enemies in a horizontal line (all ARCHER to avoid Guardian)
            Unit huntress = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 2),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit enemy1 = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(1, 2), MinionType.ARCHER);
            Unit enemy2 = createMinion(P2_MINION_2, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 2), MinionType.ARCHER);  // Changed from TANK to ARCHER
            Unit enemy3 = createMinion(P2_MINION_3, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 2), MinionType.ARCHER);  // Changed from ASSASSIN to ARCHER

            GameState state = createGameState(
                Arrays.asList(huntress, enemy1, enemy2, enemy3), PlayerId.PLAYER_1);

            // When: Use Spectral Blades in horizontal direction
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, new Position(3, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: All enemies in line should take 1 damage
            assertEquals(4, findUnit(result, P2_MINION).getHp(),
                "Enemy 1 should take 1 damage");
            assertEquals(4, findUnit(result, P2_MINION_2).getHp(),
                "Enemy 2 should take 1 damage");
            assertEquals(4, findUnit(result, P2_MINION_3).getHp(),
                "Enemy 3 should take 1 damage");
        }

        @Test
        @DisplayName("SH4: Spectral Blades pierces through enemies")
        void spectralBladesPiercesThroughEnemies() {
            // Given: Huntress at (0,2), enemies spaced out in line
            Unit huntress = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 2),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit enemy1 = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(1, 2), MinionType.ARCHER);
            // Gap at (2,2)
            Unit enemy2 = createMinion(P2_MINION_2, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 2), MinionType.TANK);

            GameState state = createGameState(
                Arrays.asList(huntress, enemy1, enemy2), PlayerId.PLAYER_1);

            // When: Use Spectral Blades
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, new Position(3, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Both enemies should take damage (pierces through)
            assertEquals(4, findUnit(result, P2_MINION).getHp(),
                "First enemy should take 1 damage");
            assertEquals(4, findUnit(result, P2_MINION_2).getHp(),
                "Second enemy (after gap) should also take 1 damage");
        }

        @Test
        @DisplayName("SH5: Spectral Blades range is 3")
        void spectralBladesRangeIs3() {
            // Given: Huntress at (0,0)
            Unit huntress = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(huntress, enemy), PlayerId.PLAYER_1);

            // When: Try to use Spectral Blades at distance 4
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, new Position(4, 0), null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid (range 3)
            assertFalse(result.isValid(), "Spectral Blades should fail at distance 4 (range is 3)");
        }

        @Test
        @DisplayName("SH5b: Spectral Blades valid at range 3")
        void spectralBladesValidAtRange3() {
            // Given: Huntress at (0,0)
            Unit huntress = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(huntress, enemy), PlayerId.PLAYER_1);

            // When: Use Spectral Blades at distance 3
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, new Position(3, 0), null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid
            assertTrue(result.isValid(), "Spectral Blades should be valid at range 3");
        }

        @Test
        @DisplayName("SH6: Spectral Blades must be orthogonal")
        void spectralBladesMustBeOrthogonal() {
            // Given: Huntress at (2,2)
            Unit huntress = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 3), MinionType.ARCHER);  // Diagonal

            GameState state = createGameState(Arrays.asList(huntress, enemy), PlayerId.PLAYER_1);

            // When: Try diagonal direction
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, new Position(3, 3), null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "Spectral Blades must be orthogonal");
        }

        @Test
        @DisplayName("SH7: Spectral Blades does not damage friendly units")
        void spectralBladesDoesNotDamageFriendlyUnits() {
            // Given: Huntress at (0,2), friendly at (1,2), enemy at (2,2)
            Unit huntress = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 2),
                HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit friendly = createMinion(P1_MINION, PlayerId.PLAYER_1, 5, 2,
                new Position(1, 2), MinionType.TANK);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 2), MinionType.ARCHER);

            GameState state = createGameState(
                Arrays.asList(huntress, friendly, enemy), PlayerId.PLAYER_1);

            // When: Use Spectral Blades
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, new Position(3, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Friendly should not take damage, enemy should
            assertEquals(5, findUnit(result, P1_MINION).getHp(),
                "Friendly unit should NOT take damage");
            assertEquals(4, findUnit(result, P2_MINION).getHp(),
                "Enemy should take 1 damage");
        }
    }
}
