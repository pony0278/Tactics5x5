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
 * SCL-Series: CLERIC Skill Tests (from SKILL_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for all three Cleric skills:
 * - Trinity: Heal 3 HP, remove debuff, grant LIFE buff
 * - Power of Many: Heal all allies 1 HP, grant +1 ATK for 1 round
 * - Ascended Form: Invulnerable 1 round, double healing, cannot attack
 */
@DisplayName("SCL-Series: CLERIC Skills")
public class RuleEngineClericSkillTest {

    private static final String P1_HERO = "p1_hero";
    private static final String P1_MINION_1 = "p1_minion_1";
    private static final String P1_MINION_2 = "p1_minion_2";
    private static final String P2_HERO = "p2_hero";
    private static final String P2_ENEMY = "p2_enemy";

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Unit createCleric(String id, PlayerId owner, int hp, int atk, Position pos, String skillId) {
        return new Unit(id, owner, hp, atk, 2, 1, pos, true,
            UnitCategory.HERO, null, HeroClass.CLERIC, hp,
            skillId, 0,  // cooldown = 0 (ready)
            0, false, false, false, 0, null,
            0, false, null,
            0, 0);
    }

    private Unit createClericWithActionsUsed(String id, PlayerId owner, int hp, int atk, Position pos,
                                              String skillId, int actionsUsed) {
        return new Unit(id, owner, hp, atk, 2, 1, pos, true,
            UnitCategory.HERO, null, HeroClass.CLERIC, hp,
            skillId, 0,
            0, false, false, false, 0, null,
            actionsUsed, false, null,
            0, 0);
    }

    private Unit createMinion(String id, PlayerId owner, int hp, int atk, Position pos, MinionType type) {
        return new Unit(id, owner, hp, atk, 2, 1, pos, true,
            UnitCategory.MINION, type, null, hp,
            null, 0,
            0, false, false, false, 0, null,
            0, false, null,
            0, 0);
    }

    private Unit createMinionWithActionsUsed(String id, PlayerId owner, int hp, int atk, Position pos,
                                              MinionType type, int actionsUsed) {
        return new Unit(id, owner, hp, atk, 2, 1, pos, true,
            UnitCategory.MINION, type, null, hp,
            null, 0,
            0, false, false, false, 0, null,
            actionsUsed, false, null,
            0, 0);
    }

    private Unit createHero(String id, PlayerId owner, int hp, int atk, Position pos,
                            HeroClass heroClass, String skillId) {
        return new Unit(id, owner, hp, atk, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0,
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

    private Unit findUnit(GameState state, String unitId) {
        return state.getUnits().stream()
            .filter(u -> u.getId().equals(unitId))
            .findFirst()
            .orElse(null);
    }

    private boolean hasBuffType(GameState state, String unitId, BuffType type) {
        List<BuffInstance> buffs = state.getUnitBuffs().get(unitId);
        if (buffs == null) return false;
        return buffs.stream().anyMatch(b -> b.getType() == type);
    }

    private int countDebuffs(GameState state, String unitId) {
        List<BuffInstance> buffs = state.getUnitBuffs().get(unitId);
        if (buffs == null) return 0;
        return (int) buffs.stream()
            .filter(b -> b.getType() == BuffType.WEAKNESS ||
                        b.getType() == BuffType.BLEED ||
                        b.getType() == BuffType.SLOW ||
                        b.getType() == BuffType.BLIND)
            .count();
    }

    // =========================================================================
    // SCL1-SCL5: Trinity Tests
    // =========================================================================

    @Nested
    @DisplayName("Trinity Tests")
    class TrinityTests {

        @Test
        @DisplayName("SCL1: Trinity heals target for 3 HP (+ LIFE buff gives +3 more)")
        void trinityHealsTargetFor3Hp() {
            // Given: Cleric and damaged ally
            Unit cleric = createCleric(P1_HERO, PlayerId.PLAYER_1, 10, 2, new Position(0, 0),
                SkillRegistry.CLERIC_TRINITY);
            Unit ally = createMinion(P1_MINION_1, PlayerId.PLAYER_1, 4, 2, new Position(1, 1), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(cleric, ally), PlayerId.PLAYER_1);

            // When: Use Trinity on ally
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, ally.getPosition(), P1_MINION_1);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Ally should have 4 + 3 (heal) + 3 (LIFE buff) = 10 HP
            Unit updatedAlly = findUnit(result, P1_MINION_1);
            assertEquals(10, updatedAlly.getHp(),
                "Ally should be healed 3 HP + 3 HP from LIFE buff = 10 total");
        }

        @Test
        @DisplayName("SCL2: Trinity can target self")
        void trinityCanTargetSelf() {
            // Given: Damaged Cleric
            Unit cleric = createCleric(P1_HERO, PlayerId.PLAYER_1, 7, 2, new Position(0, 0),
                SkillRegistry.CLERIC_TRINITY);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(cleric, enemy), PlayerId.PLAYER_1);

            // When: Use Trinity on self
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, cleric.getPosition(), P1_HERO);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Cleric should have 7 + 3 (heal) + 3 (LIFE buff) = 13 HP
            Unit updatedCleric = findUnit(result, P1_HERO);
            assertEquals(13, updatedCleric.getHp(),
                "Cleric should heal self for 3 HP + 3 HP from LIFE buff = 13 total");
        }

        @Test
        @DisplayName("SCL3: Trinity removes one random debuff")
        void trinityRemovesOneDebuff() {
            // Given: Cleric and ally with 2 debuffs
            Unit cleric = createCleric(P1_HERO, PlayerId.PLAYER_1, 10, 2, new Position(0, 0),
                SkillRegistry.CLERIC_TRINITY);
            Unit ally = createMinion(P1_MINION_1, PlayerId.PLAYER_1, 5, 2, new Position(1, 1), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            List<BuffInstance> allyDebuffs = new ArrayList<>();
            allyDebuffs.add(BuffFactory.create(BuffType.BLEED, "enemy"));
            allyDebuffs.add(BuffFactory.create(BuffType.WEAKNESS, "enemy"));
            unitBuffs.put(P1_MINION_1, allyDebuffs);

            GameState state = createGameStateWithBuffs(Arrays.asList(cleric, ally), PlayerId.PLAYER_1, unitBuffs);

            // Use fixed RNG to pick first debuff
            ruleEngine.setRngProvider(new RngProvider() {
                @Override
                public int nextInt(int bound) {
                    return 0;
                }
            });

            // When: Use Trinity on ally
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, ally.getPosition(), P1_MINION_1);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: One debuff should be removed (2 debuffs - 1 removed + 1 LIFE = 2 total)
            int debuffCount = countDebuffs(result, P1_MINION_1);
            assertEquals(1, debuffCount, "One debuff should be removed, leaving 1");
            assertTrue(hasBuffType(result, P1_MINION_1, BuffType.LIFE), "LIFE buff should be applied");
        }

        @Test
        @DisplayName("SCL4: Trinity grants LIFE buff (+3 HP instant)")
        void trinityGrantsLifeBuff() {
            // Given: Cleric and ally
            Unit cleric = createCleric(P1_HERO, PlayerId.PLAYER_1, 10, 2, new Position(0, 0),
                SkillRegistry.CLERIC_TRINITY);
            Unit ally = createMinion(P1_MINION_1, PlayerId.PLAYER_1, 4, 2, new Position(1, 1), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(cleric, ally), PlayerId.PLAYER_1);

            // When: Use Trinity on ally
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, ally.getPosition(), P1_MINION_1);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Ally should have LIFE buff
            assertTrue(hasBuffType(result, P1_MINION_1, BuffType.LIFE),
                "Ally should have LIFE buff after Trinity");
        }

        @Test
        @DisplayName("SCL5: Trinity range is 2")
        void trinityRangeIs2() {
            // Given: Cleric and allies at different distances
            Unit cleric = createCleric(P1_HERO, PlayerId.PLAYER_1, 10, 2, new Position(0, 0),
                SkillRegistry.CLERIC_TRINITY);
            Unit allyInRange = createMinion(P1_MINION_1, PlayerId.PLAYER_1, 5, 2,
                new Position(2, 0), MinionType.ARCHER);  // Distance = 2
            Unit allyOutOfRange = createMinion(P1_MINION_2, PlayerId.PLAYER_1, 5, 2,
                new Position(3, 0), MinionType.ARCHER);  // Distance = 3

            GameState state = createGameState(
                Arrays.asList(cleric, allyInRange, allyOutOfRange), PlayerId.PLAYER_1);

            // When/Then: In range should be valid
            Action inRangeAction = Action.useSkill(PlayerId.PLAYER_1, P1_HERO,
                allyInRange.getPosition(), P1_MINION_1);
            ValidationResult inRangeResult = ruleEngine.validateAction(state, inRangeAction);
            assertTrue(inRangeResult.isValid(), "Target at distance 2 should be valid");

            // When/Then: Out of range should fail
            Action outOfRangeAction = Action.useSkill(PlayerId.PLAYER_1, P1_HERO,
                allyOutOfRange.getPosition(), P1_MINION_2);
            ValidationResult outOfRangeResult = ruleEngine.validateAction(state, outOfRangeAction);
            assertFalse(outOfRangeResult.isValid(), "Target at distance 3 should be invalid");
            assertTrue(outOfRangeResult.getErrorMessage().toLowerCase().contains("range"),
                "Error should mention range");
        }
    }

    // =========================================================================
    // SCL6-SCL8: Power of Many Tests
    // =========================================================================

    @Nested
    @DisplayName("Power of Many Tests")
    class PowerOfManyTests {

        @Test
        @DisplayName("SCL6: Power of Many heals ALL friendly units for 1 HP")
        void powerOfManyHealsAllAllies() {
            // Given: Cleric and two damaged allies
            Unit cleric = createCleric(P1_HERO, PlayerId.PLAYER_1, 4, 2, new Position(0, 0),
                SkillRegistry.CLERIC_POWER_OF_MANY);
            Unit minion1 = createMinion(P1_MINION_1, PlayerId.PLAYER_1, 3, 2,
                new Position(1, 0), MinionType.TANK);
            Unit minion2 = createMinion(P1_MINION_2, PlayerId.PLAYER_1, 2, 2,
                new Position(2, 0), MinionType.ARCHER);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2,
                new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(
                Arrays.asList(cleric, minion1, minion2, enemy), PlayerId.PLAYER_1);

            // When: Use Power of Many
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: All friendly units should be healed 1 HP
            assertEquals(5, findUnit(result, P1_HERO).getHp(),
                "Cleric should have 4 + 1 = 5 HP");
            assertEquals(4, findUnit(result, P1_MINION_1).getHp(),
                "Minion 1 should have 3 + 1 = 4 HP");
            assertEquals(3, findUnit(result, P1_MINION_2).getHp(),
                "Minion 2 should have 2 + 1 = 3 HP");
            assertEquals(10, findUnit(result, P2_ENEMY).getHp(),
                "Enemy should not be healed");
        }

        @Test
        @DisplayName("SCL7: Power of Many grants +1 ATK for 1 round")
        void powerOfManyGrantsAtkBuff() {
            // Given: Cleric and ally
            Unit cleric = createClericWithActionsUsed(P1_HERO, PlayerId.PLAYER_1, 10, 2,
                new Position(0, 0), SkillRegistry.CLERIC_POWER_OF_MANY, 0);
            Unit minion = createMinion(P1_MINION_1, PlayerId.PLAYER_1, 5, 2,
                new Position(1, 0), MinionType.ARCHER);
            Unit enemy = createMinionWithActionsUsed(P2_ENEMY, PlayerId.PLAYER_2, 10, 2,
                new Position(4, 4), MinionType.ARCHER, 1);  // Already acted

            GameState state = createGameState(Arrays.asList(cleric, minion, enemy), PlayerId.PLAYER_1);

            // When: Use Power of Many
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Cleric should have ATK buff
            List<BuffInstance> clericBuffs = result.getUnitBuffs().get(P1_HERO);
            assertNotNull(clericBuffs, "Cleric should have buffs");
            assertEquals(1, clericBuffs.size(), "Cleric should have 1 buff");
            assertEquals(1, clericBuffs.get(0).getModifiers().getBonusAttack(),
                "Cleric buff should provide +1 ATK");
            assertEquals(1, clericBuffs.get(0).getDuration(),
                "Buff should last 1 round");

            // Then: Minion should have ATK buff
            List<BuffInstance> minionBuffs = result.getUnitBuffs().get(P1_MINION_1);
            assertNotNull(minionBuffs, "Minion should have buffs");
            assertEquals(1, minionBuffs.size(), "Minion should have 1 buff");
            assertEquals(1, minionBuffs.get(0).getModifiers().getBonusAttack(),
                "Minion buff should provide +1 ATK");

            // Then: Enemy should NOT have buff
            List<BuffInstance> enemyBuffs = result.getUnitBuffs().get(P2_ENEMY);
            assertTrue(enemyBuffs == null || enemyBuffs.isEmpty(),
                "Enemy should not have any buffs");
        }

        @Test
        @DisplayName("SCL8: Power of Many requires no targeting (ALL_ALLIES)")
        void powerOfManyNoTargetRequired() {
            // Given: Cleric
            Unit cleric = createCleric(P1_HERO, PlayerId.PLAYER_1, 10, 2, new Position(0, 0),
                SkillRegistry.CLERIC_POWER_OF_MANY);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2,
                new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(cleric, enemy), PlayerId.PLAYER_1);

            // When: Use Power of Many with no target
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid
            assertTrue(result.isValid(), "Power of Many should not require a target");
        }
    }

    // =========================================================================
    // SCL9-SCL12: Ascended Form Tests
    // =========================================================================

    @Nested
    @DisplayName("Ascended Form Tests")
    class AscendedFormTests {

        @Test
        @DisplayName("SCL9: Ascended Form grants invulnerability for 1 round")
        void ascendedFormGrantsInvulnerability() {
            // Given: Cleric
            Unit cleric = createCleric(P1_HERO, PlayerId.PLAYER_1, 10, 2, new Position(2, 2),
                SkillRegistry.CLERIC_ASCENDED_FORM);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 3,
                new Position(2, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(cleric, enemy), PlayerId.PLAYER_1);

            // When: Use Ascended Form
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Cleric should be invulnerable
            assertTrue(findUnit(result, P1_HERO).isInvulnerable(),
                "Cleric should be invulnerable after Ascended Form");
            assertTrue(hasBuffType(result, P1_HERO, BuffType.INVULNERABLE),
                "Cleric should have INVULNERABLE buff");
        }

        @Test
        @DisplayName("SCL10: Ascended Form - invulnerable prevents ALL damage")
        void ascendedFormPreventsAllDamage() {
            // Given: Invulnerable Cleric (with actionsUsed=1 to prevent round end)
            Unit cleric = new Unit(P1_HERO, PlayerId.PLAYER_1, 10, 2, 2, 1, new Position(2, 2), true,
                UnitCategory.HERO, null, HeroClass.CLERIC, 10,
                SkillRegistry.CLERIC_ASCENDED_FORM, 2,
                0, false, true, false, 0, null,  // invulnerable = true
                1, false, null,  // actionsUsed = 1
                0, 0);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 5,
                new Position(2, 3), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(
                BuffFactory.create(BuffType.INVULNERABLE, P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(cleric, enemy), PlayerId.PLAYER_2, unitBuffs);

            // When: Enemy attacks invulnerable Cleric
            Action attack = Action.attack(P2_ENEMY, cleric.getPosition(), P1_HERO);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Cleric should take no damage
            assertEquals(10, findUnit(result, P1_HERO).getHp(),
                "Invulnerable Cleric should take no damage from attack");
        }

        @Test
        @DisplayName("SCL11: Ascended Form - cannot attack while invulnerable")
        void ascendedFormCannotAttack() {
            // Given: Invulnerable Cleric
            Unit cleric = new Unit(P1_HERO, PlayerId.PLAYER_1, 10, 2, 2, 1, new Position(2, 2), true,
                UnitCategory.HERO, null, HeroClass.CLERIC, 10,
                SkillRegistry.CLERIC_ASCENDED_FORM, 2,
                0, false, true, false, 0, null,  // invulnerable = true
                0, false, null,
                0, 0);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 3,
                new Position(2, 3), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(
                BuffFactory.create(BuffType.INVULNERABLE, P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(cleric, enemy), PlayerId.PLAYER_1, unitBuffs);

            // When: Try to attack while invulnerable
            Action attack = Action.attack(P1_HERO, enemy.getPosition(), P2_ENEMY);
            ValidationResult result = ruleEngine.validateAction(state, attack);

            // Then: Attack should be invalid
            assertFalse(result.isValid(),
                "Invulnerable Cleric should not be able to attack");
            assertTrue(result.getErrorMessage().toLowerCase().contains("invulnerable"),
                "Error should mention invulnerable");
        }

        @Test
        @DisplayName("SCL12: Ascended Form - healing effects doubled")
        void ascendedFormDoublesHealing() {
            // Given: Two Clerics, one invulnerable and damaged
            Unit cleric1 = createCleric(P1_HERO, PlayerId.PLAYER_1, 10, 2, new Position(0, 0),
                SkillRegistry.CLERIC_TRINITY);
            // Invulnerable Cleric at HP 4 (needs healing)
            Unit cleric2 = new Unit(P1_MINION_1, PlayerId.PLAYER_1, 4, 2, 2, 1, new Position(1, 0), true,
                UnitCategory.HERO, null, HeroClass.CLERIC, 10,
                SkillRegistry.CLERIC_ASCENDED_FORM, 2,
                0, false, true, false, 0, null,  // invulnerable = true
                1, false, null,  // already acted
                0, 0);
            Unit enemy = createMinionWithActionsUsed(P2_ENEMY, PlayerId.PLAYER_2, 10, 2,
                new Position(4, 4), MinionType.ARCHER, 1);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_MINION_1, Collections.singletonList(
                BuffFactory.create(BuffType.INVULNERABLE, P1_MINION_1)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(cleric1, cleric2, enemy), PlayerId.PLAYER_1, unitBuffs);

            // When: First Cleric heals invulnerable Cleric with Trinity
            // Trinity heals 3 HP, doubled = 6 HP, + LIFE buff 3 HP doubled = 6 HP
            // Total: 4 + 6 + 6 = 16 HP
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, cleric2.getPosition(), P1_MINION_1);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Invulnerable Cleric should receive doubled healing
            Unit healedCleric = findUnit(result, P1_MINION_1);
            // 4 (base) + 6 (Trinity 3*2) + 6 (LIFE 3*2) = 16 HP
            assertEquals(16, healedCleric.getHp(),
                "Invulnerable unit should receive doubled healing: 4 + (3*2) + (3*2) = 16 HP");
        }
    }
}
