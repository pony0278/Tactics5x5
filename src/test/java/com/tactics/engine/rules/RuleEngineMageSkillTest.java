package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.buff.BuffInstance;
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
 * SMG-Series: MAGE Skills - Additional Tests
 * Focuses on Wild Magic AOE mechanics and edge cases.
 */
@DisplayName("SMG-Series: Mage Skills")
public class RuleEngineMageSkillTest {

    private RuleEngine ruleEngine;

    private static final String P1_HERO = "p1_hero";
    private static final String P2_HERO = "p2_hero";
    private static final String P1_MINION = "p1_minion";
    private static final String P2_MINION = "p2_minion";
    private static final String P2_MINION_2 = "p2_minion_2";

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

    private Unit findUnit(GameState state, String unitId) {
        return state.getUnits().stream()
            .filter(u -> u.getId().equals(unitId))
            .findFirst()
            .orElse(null);
    }

    private List<BuffInstance> getBuffs(GameState state, String unitId) {
        return state.getUnitBuffs().getOrDefault(unitId, Collections.emptyList());
    }

    // =========================================================================
    // SMG11-SMG14: Wild Magic Tests
    // =========================================================================

    @Nested
    @DisplayName("SMG11-SMG14: Wild Magic Skill")
    class WildMagicTests {

        @Test
        @DisplayName("SMG11: Wild Magic deals 1 damage to ALL enemies")
        void wildMagicDeals1DamageToAllEnemies() {
            // Given: Mage with Wild Magic, multiple enemies across the board
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_WILD_MAGIC);
            Unit enemy1 = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(0, 0), MinionType.ARCHER);
            Unit enemy2 = createMinion(P2_MINION_2, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 4), MinionType.TANK);
            Unit enemyHero = createHero(P2_HERO, PlayerId.PLAYER_2, 10, 3,
                new Position(4, 0), HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);

            GameState state = createGameState(
                Arrays.asList(mage, enemy1, enemy2, enemyHero), PlayerId.PLAYER_1);

            // Use fixed RNG to prevent random debuffs
            ruleEngine.setRngProvider(new RngProvider() {
                @Override
                public int nextInt(int bound) {
                    return 99;  // Always > 33, no debuff
                }
            });

            // When: Use Wild Magic
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: All enemies should take 1 damage
            assertEquals(4, findUnit(result, P2_MINION).getHp(),
                "Enemy minion 1 should take 1 damage (5 -> 4)");
            assertEquals(4, findUnit(result, P2_MINION_2).getHp(),
                "Enemy minion 2 should take 1 damage (5 -> 4)");
            assertEquals(9, findUnit(result, P2_HERO).getHp(),
                "Enemy hero should take 1 damage (10 -> 9)");
        }

        @Test
        @DisplayName("SMG12: Wild Magic 33% chance per enemy to apply debuff")
        void wildMagicDebuffChance() {
            // Given: Mage with Wild Magic, one enemy
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_WILD_MAGIC);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 10, 2,
                new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(mage, enemy), PlayerId.PLAYER_1);

            // Use fixed RNG to trigger debuff (< 33)
            ruleEngine.setRngProvider(new RngProvider() {
                private int callCount = 0;
                @Override
                public int nextInt(int bound) {
                    callCount++;
                    if (bound == 100) return 10;  // < 33, apply debuff
                    if (bound == 3) return 0;  // Pick first debuff type
                    return 0;
                }
            });

            // When: Use Wild Magic
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should have a debuff
            List<BuffInstance> enemyBuffs = getBuffs(result, P2_MINION);
            assertFalse(enemyBuffs.isEmpty(), "Enemy should have debuff from Wild Magic");
        }

        @Test
        @DisplayName("SMG13: Wild Magic does NOT damage friendly units")
        void wildMagicDoesNotDamageFriendlyUnits() {
            // Given: Mage with Wild Magic, friendly minion and enemy
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_WILD_MAGIC);
            Unit friendlyMinion = createMinion(P1_MINION, PlayerId.PLAYER_1, 5, 2,
                new Position(1, 2), MinionType.TANK);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(
                Arrays.asList(mage, friendlyMinion, enemy), PlayerId.PLAYER_1);

            // Use fixed RNG to prevent random debuffs
            ruleEngine.setRngProvider(new RngProvider() {
                @Override
                public int nextInt(int bound) {
                    return 99;
                }
            });

            // When: Use Wild Magic
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Friendly minion should NOT take damage, enemy should
            assertEquals(5, findUnit(result, P1_MINION).getHp(),
                "Friendly minion should NOT take damage");
            assertEquals(4, findUnit(result, P2_MINION).getHp(),
                "Enemy should take 1 damage");
        }

        @Test
        @DisplayName("SMG14: Wild Magic no targeting required (ALL_ENEMIES)")
        void wildMagicNoTargetingRequired() {
            // Given: Mage with Wild Magic
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_WILD_MAGIC);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(mage, enemy), PlayerId.PLAYER_1);

            // When: Validate with null target position and target unit
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid (no target required for ALL_ENEMIES)
            assertTrue(result.isValid(), "Wild Magic should not require target");
        }

        @Test
        @DisplayName("SMG14b: Wild Magic can kill low HP enemies")
        void wildMagicCanKillLowHpEnemies() {
            // Given: Mage with Wild Magic, enemy with 1 HP
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_WILD_MAGIC);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 1, 2,
                new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(mage, enemy), PlayerId.PLAYER_1);

            // Use fixed RNG to prevent random debuffs
            ruleEngine.setRngProvider(new RngProvider() {
                @Override
                public int nextInt(int bound) {
                    return 99;
                }
            });

            // When: Use Wild Magic
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should be dead
            Unit deadEnemy = findUnit(result, P2_MINION);
            assertFalse(deadEnemy.isAlive(), "Enemy with 1 HP should die from Wild Magic");
        }
    }

    // =========================================================================
    // Warp Beacon Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("SMG-Extra: Warp Beacon Edge Cases")
    class WarpBeaconEdgeCases {

        @Test
        @DisplayName("SMG-E1: Cannot place beacon on obstacle")
        void cannotPlaceBeaconOnObstacle() {
            // Given: Mage with Warp Beacon, obstacle at target
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WARP_BEACON);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 4), MinionType.ARCHER);

            List<Obstacle> obstacles = Collections.singletonList(
                new Obstacle("obs_1", new Position(2, 0)));

            GameState state = new GameState(
                new Board(5, 5),
                Arrays.asList(mage, enemy),
                PlayerId.PLAYER_1,
                false, null,
                new HashMap<>(),
                new ArrayList<>(),
                obstacles,
                1, null
            );

            // When: Try to place beacon on obstacle
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, new Position(2, 0), null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "Should not be able to place beacon on obstacle");
        }

        @Test
        @DisplayName("SMG-E2: Beacon position persists across rounds")
        void beaconPositionPersistsAcrossRounds() {
            // Given: Mage places beacon
            Map<String, Object> skillState = new HashMap<>();
            skillState.put("beacon_x", 3);
            skillState.put("beacon_y", 0);

            Unit mage = new Unit(P1_HERO, PlayerId.PLAYER_1, 10, 3, 2, 1, new Position(0, 0), true,
                UnitCategory.HERO, null, HeroClass.MAGE, 10,
                SkillRegistry.MAGE_WARP_BEACON, 0,
                0, false, false, false, 0, skillState,
                1, false, null,  // actionsUsed = 1
                0, 0);
            Unit mageMinion = createMinion(P1_MINION, PlayerId.PLAYER_1, 5, 2,
                new Position(1, 0), MinionType.TANK);
            Unit enemy = createHero(P2_HERO, PlayerId.PLAYER_2, 10, 3, new Position(4, 4),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemyMinion = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 4), MinionType.ARCHER);

            // All units have acted
            mageMinion = new Unit(P1_MINION, PlayerId.PLAYER_1, 5, 2, 2, 1, new Position(1, 0), true,
                UnitCategory.MINION, MinionType.TANK, null, 5,
                null, 0, 0, false, false, false, 0, null, 1, false, null, 0, 0);
            enemy = new Unit(P2_HERO, PlayerId.PLAYER_2, 10, 3, 2, 1, new Position(4, 4), true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, 10,
                SkillRegistry.WARRIOR_ENDURE, 0, 0, false, false, false, 0, null, 1, false, null, 0, 0);
            enemyMinion = new Unit(P2_MINION, PlayerId.PLAYER_2, 5, 2, 2, 1, new Position(3, 4), true,
                UnitCategory.MINION, MinionType.ARCHER, null, 5,
                null, 0, 0, false, false, false, 0, null, 1, false, null, 0, 0);

            GameState state = createGameState(
                Arrays.asList(mage, mageMinion, enemy, enemyMinion), PlayerId.PLAYER_1);

            // When: End turn (trigger round end)
            Action endTurn = Action.endTurn(P1_HERO);
            GameState result = ruleEngine.applyAction(state, endTurn);

            // Then: Beacon position should persist
            Unit updatedMage = findUnit(result, P1_HERO);
            assertNotNull(updatedMage.getSkillState());
            assertEquals(3, updatedMage.getSkillState().get("beacon_x"));
            assertEquals(0, updatedMage.getSkillState().get("beacon_y"));
        }
    }
}
