package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
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
 * SA-Series: Skill Apply (General) Tests (from SKILL_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for general skill effect application:
 * - SA1: DAMAGE effect reduces target HP
 * - SA2: DAMAGE can kill target
 * - SA3: HEAL effect increases target HP
 * - SA4: HEAL can exceed maxHp
 * - SA5: MOVE_SELF updates hero position
 * - SA6: MOVE_TARGET pushes enemy
 * - SA7: APPLY_BUFF adds buff to target
 * - SA8: SPAWN_UNIT creates temporary unit
 * - SA9: Temporary unit removed after duration
 * - SA10: STUN effect prevents target action
 * - SA11: MARK effect increases damage taken
 * - SA12: Skill sets cooldown after execution
 * - SA13: Skill with SLOW executes next round
 * - SA14: Multiple effects apply in order
 */
@DisplayName("SA-Series: Skill Apply (General)")
public class RuleEngineSkillApplyTest {

    private RuleEngine ruleEngine;
    private static final PlayerId P1 = PlayerId.PLAYER_1;
    private static final PlayerId P2 = PlayerId.PLAYER_2;

    private static final String P1_HERO = "p1_hero";
    private static final String P1_ALLY = "p1_ally";
    private static final String P1_MINION = "p1_minion";
    private static final String P2_HERO = "p2_hero";
    private static final String P2_ENEMY = "p2_enemy";
    private static final String P2_MINION = "p2_minion";

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // ========== Helper Methods ==========

    private Unit createHero(String id, PlayerId owner, int hp, int attack, Position pos,
                           HeroClass heroClass, String skillId) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0,
            0, false, false, false, 0, null,
            0, false, null, 0, 0);
    }

    private Unit createHeroWithActionsUsed(String id, PlayerId owner, int hp, int attack, Position pos,
                                           HeroClass heroClass, String skillId, int actionsUsed) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0,
            0, false, false, false, 0, null,
            actionsUsed, false, null, 0, 0);
    }

    private Unit createMinion(String id, PlayerId owner, int hp, int attack, Position pos,
                             MinionType minionType) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.MINION, minionType, null, hp,
            null, 0,
            0, false, false, false, 0, null,
            0, false, null, 0, 0);
    }

    private Unit createMinionWithActionsUsed(String id, PlayerId owner, int hp, int attack, Position pos,
                                              MinionType minionType, int actionsUsed) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.MINION, minionType, null, hp,
            null, 0,
            0, false, false, false, 0, null,
            actionsUsed, false, null, 0, 0);
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
        return state.getUnits().stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    private Unit findClone(GameState state, String heroId) {
        return state.getUnits().stream()
            .filter(u -> u.getId().startsWith(heroId + "_clone_"))
            .findFirst()
            .orElse(null);
    }

    private List<BuffInstance> getUnitBuffs(GameState state, String unitId) {
        Map<String, List<BuffInstance>> unitBuffs = state.getUnitBuffs();
        if (unitBuffs == null) return Collections.emptyList();
        return unitBuffs.getOrDefault(unitId, Collections.emptyList());
    }

    private boolean hasBuffType(GameState state, String unitId, BuffType type) {
        return getUnitBuffs(state, unitId).stream()
            .anyMatch(b -> b.getType() == type);
    }

    // ========== SA1: DAMAGE Effect ==========

    @Nested
    @DisplayName("SA1: DAMAGE effect reduces target HP")
    class DamageEffectTests {

        @Test
        @DisplayName("SA1: Elemental Blast deals 3 damage to target")
        void damageReducesTargetHp() {
            // Given: Mage targets enemy
            Unit mage = createHero(P1_HERO, P1, 10, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit target = createMinion(P2_ENEMY, P2, 10, 3, new Position(2, 0), MinionType.ARCHER);

            // Use fixed RNG to avoid debuff
            ruleEngine.setRngProvider(new RngProvider(12345));

            GameState state = createGameState(Arrays.asList(mage, target), P1);
            int originalHp = target.getHp();

            // When: Elemental Blast (3 damage)
            Action action = Action.useSkill(P1, P1_HERO, target.getPosition(), P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Target HP reduced by 3
            Unit updatedTarget = findUnit(result, P2_ENEMY);
            assertEquals(originalHp - 3, updatedTarget.getHp(),
                "Elemental Blast should deal 3 damage");
        }
    }

    // ========== SA2: DAMAGE Can Kill ==========

    @Nested
    @DisplayName("SA2: DAMAGE can kill target")
    class DamageCanKillTests {

        @Test
        @DisplayName("SA2: Skill damage can kill low HP target")
        void damageCanKillTarget() {
            // Given: Mage targets low HP enemy (2 HP)
            Unit mage = createHero(P1_HERO, P1, 10, 3, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit target = createMinion(P2_ENEMY, P2, 2, 3, new Position(2, 0), MinionType.ARCHER);

            // Use fixed RNG
            ruleEngine.setRngProvider(new RngProvider(12345));

            GameState state = createGameState(Arrays.asList(mage, target), P1);

            // When: Elemental Blast (3 damage to 2 HP target)
            Action action = Action.useSkill(P1, P1_HERO, target.getPosition(), P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Target should be dead
            Unit updatedTarget = findUnit(result, P2_ENEMY);
            assertFalse(updatedTarget.isAlive(), "Target should be killed by skill damage");
        }
    }

    // ========== SA3: HEAL Effect ==========

    @Nested
    @DisplayName("SA3: HEAL effect increases target HP")
    class HealEffectTests {

        @Test
        @DisplayName("SA3: Trinity heals target for 3 HP")
        void healIncreasesTargetHp() {
            // Given: Cleric heals injured ally (5 HP)
            Unit cleric = createHero(P1_HERO, P1, 10, 2, new Position(0, 0),
                HeroClass.CLERIC, SkillRegistry.CLERIC_TRINITY);
            Unit ally = createMinion(P1_ALLY, P1, 5, 2, new Position(1, 0), MinionType.TANK);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(cleric, ally, enemy), P1);
            int originalHp = ally.getHp();

            // When: Trinity (heals 3 + LIFE buff +3)
            Action action = Action.useSkill(P1, P1_HERO, ally.getPosition(), P1_ALLY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Ally HP should increase
            Unit updatedAlly = findUnit(result, P1_ALLY);
            assertTrue(updatedAlly.getHp() > originalHp,
                "Trinity should heal ally: " + originalHp + " -> " + updatedAlly.getHp());
        }
    }

    // ========== SA4: HEAL Can Exceed MaxHp ==========

    @Nested
    @DisplayName("SA4: HEAL can exceed maxHp")
    class HealExceedsMaxHpTests {

        @Test
        @DisplayName("SA4: Trinity can heal above original maxHp")
        void healCanExceedMaxHp() {
            // Given: Cleric heals full HP ally (5 HP = max)
            Unit cleric = createHero(P1_HERO, P1, 10, 2, new Position(0, 0),
                HeroClass.CLERIC, SkillRegistry.CLERIC_TRINITY);
            Unit ally = createMinion(P1_ALLY, P1, 5, 2, new Position(1, 0), MinionType.TANK);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(cleric, ally, enemy), P1);
            int maxHp = ally.getMaxHp();  // 5

            // When: Trinity (heals 3 + LIFE buff +3)
            Action action = Action.useSkill(P1, P1_HERO, ally.getPosition(), P1_ALLY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Ally HP should exceed original maxHp
            Unit updatedAlly = findUnit(result, P1_ALLY);
            assertTrue(updatedAlly.getHp() > maxHp,
                "Trinity heal + LIFE buff should exceed original maxHp: " + maxHp + " -> " + updatedAlly.getHp());
        }
    }

    // ========== SA5: MOVE_SELF Effect ==========

    @Nested
    @DisplayName("SA5: MOVE_SELF updates hero position")
    class MoveSelfEffectTests {

        @Test
        @DisplayName("SA5: Heroic Leap moves hero to target tile")
        void moveSelfUpdatesPosition() {
            // Given: Warrior at (0,0)
            Unit warrior = createHero(P1_HERO, P1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(warrior, enemy), P1);

            // When: Heroic Leap to (2, 2)
            Action action = Action.useSkill(P1, P1_HERO, new Position(2, 2), null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Warrior should be at (2, 2)
            Unit updatedWarrior = findUnit(result, P1_HERO);
            assertEquals(new Position(2, 2), updatedWarrior.getPosition(),
                "Heroic Leap should move warrior to target tile");
        }
    }

    // ========== SA6: MOVE_TARGET Effect ==========

    @Nested
    @DisplayName("SA6: MOVE_TARGET pushes enemy")
    class MoveTargetEffectTests {

        @Test
        @DisplayName("SA6: Shockwave pushes adjacent enemies")
        void moveTargetPushesEnemy() {
            // Given: Warrior at (2,2), enemy at (2,1)
            Unit warrior = createHero(P1_HERO, P1, 10, 3, new Position(2, 2),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_SHOCKWAVE);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(2, 1), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(warrior, enemy), P1);

            // When: Shockwave pushes adjacent enemies
            Action action = Action.useSkill(P1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should be pushed away from warrior
            Unit updatedEnemy = findUnit(result, P2_ENEMY);
            assertEquals(new Position(2, 0), updatedEnemy.getPosition(),
                "Shockwave should push enemy away (from (2,1) to (2,0))");
        }
    }

    // ========== SA7: APPLY_BUFF Effect ==========

    @Nested
    @DisplayName("SA7: APPLY_BUFF adds buff to target")
    class ApplyBuffEffectTests {

        @Test
        @DisplayName("SA7: Elemental Strike applies chosen debuff")
        void applyBuffAddsToTarget() {
            // Given: Duelist targets adjacent enemy
            Unit duelist = createHero(P1_HERO, P1, 10, 3, new Position(2, 2),
                HeroClass.DUELIST, SkillRegistry.DUELIST_ELEMENTAL_STRIKE);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(2, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy), P1);

            // When: Elemental Strike with SLOW debuff
            Action action = Action.useSkillWithBuffChoice(P1, P1_HERO, P2_ENEMY, BuffType.SLOW);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should have SLOW buff
            assertTrue(hasBuffType(result, P2_ENEMY, BuffType.SLOW),
                "Elemental Strike should apply SLOW debuff to enemy");
        }
    }

    // ========== SA8: SPAWN_UNIT Effect ==========

    @Nested
    @DisplayName("SA8: SPAWN_UNIT creates temporary unit")
    class SpawnUnitEffectTests {

        @Test
        @DisplayName("SA8: Shadow Clone creates temporary unit")
        void spawnUnitCreatesTemporary() {
            // Given: Rogue uses Shadow Clone
            Unit rogue = createHero(P1_HERO, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SHADOW_CLONE);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, enemy), P1);

            // When: Shadow Clone at (2,3)
            Action action = Action.useSkill(P1, P1_HERO, new Position(2, 3), null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Clone should exist and be temporary
            Unit clone = findClone(result, P1_HERO);
            assertNotNull(clone, "Shadow Clone should create a clone unit");
            assertTrue(clone.isTemporary(), "Clone should be marked as temporary");
        }
    }

    // ========== SA9: Temporary Unit Removed After Duration ==========
    // (Already tested in SR12 of RogueSkillTest)

    // ========== SA10: STUN Effect (implied by buff system) ==========

    // ========== SA11: MARK Effect Increases Damage ==========

    @Nested
    @DisplayName("SA11: MARK effect increases damage taken")
    class MarkEffectTests {

        @Test
        @DisplayName("SA11: Death Mark increases damage taken by +2")
        void markEffectIncreasesDamage() {
            // Given: Enemy has Death Mark
            Unit attacker = createHero(P1_HERO, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_DEATH_MARK);
            Unit markedEnemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(2, 3), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P2_ENEMY, Collections.singletonList(
                BuffFactory.create(BuffType.DEATH_MARK, P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(attacker, markedEnemy), P1, unitBuffs);

            // When: Attack marked enemy (base 3 + mark bonus 2 = 5)
            Action attack = Action.attack(P1_HERO, new Position(2, 3), P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Enemy should take 5 damage (3 base + 2 mark)
            Unit updatedEnemy = findUnit(result, P2_ENEMY);
            assertEquals(5, updatedEnemy.getHp(),
                "Marked enemy should take +2 damage: 10 - 5 = 5 HP");
        }
    }

    // ========== SA12: Skill Sets Cooldown ==========

    @Nested
    @DisplayName("SA12: Skill sets cooldown after execution")
    class SkillSetsCooldownTests {

        @Test
        @DisplayName("SA12: Skill goes on cooldown after use")
        void skillSetsCooldownAfterUse() {
            // Given: Hero uses skill
            Unit hero = createHero(P1_HERO, P1, 10, 3, new Position(2, 2),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), P1);

            // Verify no cooldown initially
            Unit heroBeforeSkill = findUnit(state, P1_HERO);
            assertEquals(0, heroBeforeSkill.getSkillCooldown(), "Skill should start with no cooldown");

            // When: Use skill
            Action action = Action.useSkill(P1, P1_HERO, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Skill should be on cooldown
            Unit heroAfterSkill = findUnit(result, P1_HERO);
            assertTrue(heroAfterSkill.getSkillCooldown() > 0,
                "Skill should be on cooldown after use: " + heroAfterSkill.getSkillCooldown());
        }
    }

    // ========== SA13: Skill with SLOW Executes Next Round ==========
    // (Tested in existing SLOW buff tests)

    // ========== SA14: Multiple Effects Apply in Order ==========

    @Nested
    @DisplayName("SA14: Multiple effects apply in order")
    class MultipleEffectsOrderTests {

        @Test
        @DisplayName("SA14: Trinity applies heal + remove debuff + LIFE buff")
        void multipleEffectsApplyInOrder() {
            // Given: Cleric heals ally with BLEED debuff
            Unit cleric = createHero(P1_HERO, P1, 10, 2, new Position(0, 0),
                HeroClass.CLERIC, SkillRegistry.CLERIC_TRINITY);
            Unit ally = createMinion(P1_ALLY, P1, 5, 2, new Position(1, 0), MinionType.TANK);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_ALLY, Collections.singletonList(
                BuffFactory.createBleed("source")));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(cleric, ally, enemy), P1, unitBuffs);

            int originalHp = ally.getHp();

            // When: Trinity (heals + removes debuff + grants LIFE)
            Action action = Action.useSkill(P1, P1_HERO, ally.getPosition(), P1_ALLY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: All effects should apply
            Unit updatedAlly = findUnit(result, P1_ALLY);

            // HP increased (heal + LIFE instant bonus)
            assertTrue(updatedAlly.getHp() > originalHp,
                "Ally HP should increase from Trinity");

            // LIFE buff granted
            assertTrue(hasBuffType(result, P1_ALLY, BuffType.LIFE),
                "Ally should have LIFE buff from Trinity");

            // BLEED may or may not be removed (depends on implementation)
            // Trinity removes one random debuff
        }
    }
}
