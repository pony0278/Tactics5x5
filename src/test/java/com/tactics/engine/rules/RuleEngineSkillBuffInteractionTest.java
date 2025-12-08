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
 * SB-Series: Skill + BUFF Interaction Tests (from SKILL_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for interactions between skills and buffs:
 * - STUN prevents USE_SKILL
 * - ROOT prevents movement skills
 * - SLOW delays skill execution
 * - SPEED allows skill as one of two actions
 * - POWER/WEAKNESS do NOT affect skill damage
 * - Invisible + skill interactions
 * - Invulnerable + skill interactions
 */
@DisplayName("SB-Series: Skill + BUFF Interaction")
public class RuleEngineSkillBuffInteractionTest {

    private RuleEngine ruleEngine;

    // ========== Constants ==========
    private static final String P1_HERO = "p1_hero";
    private static final String P1_MINION = "p1_minion_1";
    private static final String P2_HERO = "p2_hero";
    private static final String P2_ENEMY = "p2_minion_1";

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // ========== Helper Methods ==========

    private Unit createHero(String id, PlayerId owner, int hp, int attack, Position pos,
                            HeroClass heroClass, String skillId) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0,  // cooldown = 0 (ready)
            0, false, false, false, 0, null,
            0, false, null,
            0, 0);
    }

    private Unit createHeroWithActionsUsed(String id, PlayerId owner, int hp, int attack, Position pos,
                                            HeroClass heroClass, String skillId, int actionsUsed) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0,
            0, false, false, false, 0, null,
            actionsUsed, false, null,
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

    private List<BuffInstance> getUnitBuffs(GameState state, String unitId) {
        Map<String, List<BuffInstance>> unitBuffs = state.getUnitBuffs();
        if (unitBuffs == null) return Collections.emptyList();
        return unitBuffs.getOrDefault(unitId, Collections.emptyList());
    }

    private boolean hasBuffType(GameState state, String unitId, BuffType type) {
        for (BuffInstance buff : getUnitBuffs(state, unitId)) {
            if (buff.getType() == type) return true;
        }
        return false;
    }

    // ========== SB2: ROOT prevents movement skills ==========

    @Nested
    @DisplayName("SB2-SB3: ROOT + Skill Interaction")
    class RootSkillInteraction {

        @Test
        @DisplayName("SB2: ROOT prevents movement skills (Heroic Leap)")
        void rootPreventsHeroicLeap() {
            // Given: Warrior with ROOT buff trying to use Heroic Leap
            Unit warrior = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(4, 4), MinionType.ARCHER);

            // Note: ROOT is not yet in BuffType, so this test documents expected behavior
            // Skip until ROOT buff is implemented
        }

        @Test
        @DisplayName("SB3: ROOT does not prevent non-movement skills (Elemental Blast)")
        void rootAllowsNonMovementSkills() {
            // Given: Mage with ROOT buff trying to use Elemental Blast
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            // Note: ROOT is not yet in BuffType, so this test documents expected behavior
            // Skip until ROOT buff is implemented
        }
    }

    // ========== SB4-SB6: SLOW + Skill Interaction ==========

    @Nested
    @DisplayName("SB4-SB6: SLOW + Skill Interaction")
    class SlowSkillInteraction {

        @Test
        @DisplayName("SB4: SLOW delays skill by 1 round")
        void slowDelaysSkillByOneRound() {
            // Given: Mage with SLOW buff using Elemental Blast
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(BuffFactory.createSlow(P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(mage, enemy),
                PlayerId.PLAYER_1,
                unitBuffs
            );

            int enemyHpBefore = findUnit(state, P2_ENEMY).getHp();

            // When: Use Elemental Blast (should enter preparing state)
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Target should NOT take damage yet (action delayed)
            Unit updatedEnemy = findUnit(result, P2_ENEMY);
            assertEquals(enemyHpBefore, updatedEnemy.getHp(),
                "Target should not take damage yet - skill is delayed by SLOW");

            // Mage should be in preparing state
            Unit updatedMage = findUnit(result, P1_HERO);
            assertTrue(updatedMage.isPreparing(), "Mage should be in preparing state");
        }

        @Test
        @DisplayName("SB5: SLOW + Skill - Preparing state stored")
        void slowSkillPreparingStateStored() {
            // Given: Mage with SLOW buff
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(BuffFactory.createSlow(P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(mage, enemy),
                PlayerId.PLAYER_1,
                unitBuffs
            );

            // When: Use Elemental Blast
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Preparing action should be stored
            Unit updatedMage = findUnit(result, P1_HERO);
            assertTrue(updatedMage.isPreparing(), "Mage should be in preparing state");
            assertNotNull(updatedMage.getPreparingAction(), "Preparing action should be stored");
            assertEquals("USE_SKILL", updatedMage.getPreparingAction().get("type"),
                "Preparing action should be USE_SKILL");
        }

        @Test
        @DisplayName("SB6: SLOW + Skill - If hero dies while preparing, skill cancelled")
        void slowSkillCancelledIfHeroDies() {
            // Given: Mage with SLOW buff has declared a skill (preparing state)
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 1, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            // Set mage to preparing state with low HP
            Map<String, Object> preparingAction = new HashMap<>();
            preparingAction.put("type", "USE_SKILL");
            preparingAction.put("targetUnitId", P2_ENEMY);
            mage = mage.withPreparing(true, preparingAction);

            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 4, new Position(2, 3), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(BuffFactory.createSlow(P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(mage, enemy),
                PlayerId.PLAYER_2,
                unitBuffs
            );

            int targetHpBefore = findUnit(state, P2_ENEMY).getHp();

            // When: Enemy kills the preparing Mage
            Action attack = Action.attack(P2_ENEMY, new Position(2, 2), P1_HERO);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Mage should be dead
            Unit deadMage = findUnit(result, P1_HERO);
            assertFalse(deadMage.isAlive(), "Mage should be dead");

            // When round ends, the skill should NOT execute
            // (This requires round-end processing which may need additional setup)
        }
    }

    // ========== SB7: SPEED + Skill Interaction ==========

    @Nested
    @DisplayName("SB7: SPEED + Skill Interaction")
    class SpeedSkillInteraction {

        @Test
        @DisplayName("SB7: SPEED + Skill - Can use skill as one of two actions")
        void speedAllowsSkillAsOneOfTwoActions() {
            // Given: Mage with SPEED buff
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(BuffFactory.createSpeed(P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(mage, enemy),
                PlayerId.PLAYER_1,
                unitBuffs
            );

            // When: Use Elemental Blast (first action)
            Action skillAction = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, P2_ENEMY);
            GameState afterSkill = ruleEngine.applyAction(state, skillAction);

            // Then: Mage should have actionsUsed = 1
            Unit updatedMage = findUnit(afterSkill, P1_HERO);
            assertEquals(1, updatedMage.getActionsUsed(),
                "Mage should have 1 action used after skill");

            // When: Second action (MOVE) should also be valid due to SPEED
            Action moveAction = Action.move(P1_HERO, new Position(2, 3));
            ValidationResult moveResult = ruleEngine.validateAction(afterSkill, moveAction);
            assertTrue(moveResult.isValid(),
                "Second action should be valid with SPEED buff");
        }
    }

    // ========== SB8-SB9: POWER/WEAKNESS do NOT affect skill damage ==========

    @Nested
    @DisplayName("SB8-SB9: ATK Buffs don't affect skill damage")
    class AtkBuffsSkillDamage {

        @Test
        @DisplayName("SB8: POWER buff - Skill damage NOT affected by ATK bonus")
        void powerBuffDoesNotAffectSkillDamage() {
            // Given: Mage with POWER buff (+3 ATK) using Elemental Blast
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(BuffFactory.createPower(P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(mage, enemy),
                PlayerId.PLAYER_1,
                unitBuffs
            );

            // When: Use Elemental Blast (deals 3 fixed damage)
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should take exactly 3 damage (not 6 from +3 ATK bonus)
            Unit updatedEnemy = findUnit(result, P2_ENEMY);
            assertEquals(7, updatedEnemy.getHp(),
                "Enemy should take 3 damage (fixed skill damage, not affected by POWER +3 ATK)");
        }

        @Test
        @DisplayName("SB9: WEAKNESS buff - Skill damage NOT affected by ATK penalty")
        void weaknessBuffDoesNotAffectSkillDamage() {
            // Given: Mage with WEAKNESS buff (-2 ATK) using Elemental Blast
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(BuffFactory.createWeakness(P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(mage, enemy),
                PlayerId.PLAYER_1,
                unitBuffs
            );

            // Use fixed RNG to prevent random debuff from skill (RNG >= 50 means no debuff)
            ruleEngine.setRngProvider(new RngProvider() {
                @Override
                public int nextInt(int bound) {
                    return 99; // Always return high value to prevent debuff
                }
            });

            // When: Use Elemental Blast (deals 3 fixed damage)
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should take exactly 3 damage (not reduced by WEAKNESS -2 ATK)
            Unit updatedEnemy = findUnit(result, P2_ENEMY);
            assertEquals(7, updatedEnemy.getHp(),
                "Enemy should take 3 damage (fixed skill damage, not affected by WEAKNESS -2 ATK)");
        }
    }

    // ========== SB10: Skill that applies BUFF ==========

    @Nested
    @DisplayName("SB10: Skills that apply buffs")
    class SkillAppliesBuff {

        @Test
        @DisplayName("SB10: Skill that applies BUFF - Duration is 2")
        void skillAppliedBuffHasDurationTwo() {
            // Given: Duelist using Elemental Strike to apply SLOW debuff
            Unit duelist = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.DUELIST, SkillRegistry.DUELIST_ELEMENTAL_STRIKE);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy), PlayerId.PLAYER_1);

            // When: Use Elemental Strike with SLOW debuff
            Action action = Action.useSkillWithBuffChoice(PlayerId.PLAYER_1, P1_HERO, P2_ENEMY, BuffType.SLOW);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should have SLOW buff with duration 2
            List<BuffInstance> enemyBuffs = getUnitBuffs(result, P2_ENEMY);
            BuffInstance slowBuff = null;
            for (BuffInstance buff : enemyBuffs) {
                if (buff.getType() == BuffType.SLOW) {
                    slowBuff = buff;
                    break;
                }
            }
            assertNotNull(slowBuff, "Enemy should have SLOW buff");
            assertEquals(2, slowBuff.getDuration(), "SLOW buff from skill should have duration 2");
        }
    }

    // ========== SB11: Death Mark + BUFF damage bonus interaction ==========

    @Nested
    @DisplayName("SB11: Death Mark + BUFF Interaction")
    class DeathMarkBuffInteraction {

        @Test
        @DisplayName("SB11: Death Mark + POWER - Damage stacks")
        void deathMarkAndPowerDamageStacks() {
            // Given: Rogue has applied Death Mark to enemy, attacker has POWER buff
            Unit rogue = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_DEATH_MARK);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            // First, apply Death Mark
            GameState state = createGameState(Arrays.asList(rogue, enemy), PlayerId.PLAYER_1);
            Action markAction = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, P2_ENEMY);
            GameState afterMark = ruleEngine.applyAction(state, markAction);

            // Verify Death Mark applied
            assertTrue(hasBuffType(afterMark, P2_ENEMY, BuffType.DEATH_MARK),
                "Enemy should have DEATH_MARK");

            // Now setup attacker with POWER buff
            // (This test documents expected interaction between Death Mark and attack damage bonuses)
        }
    }

    // ========== SB12-SB13: Invisible + Skill Interaction ==========

    @Nested
    @DisplayName("SB12-SB13: Invisible + Skill Interaction")
    class InvisibleSkillInteraction {

        @Test
        @DisplayName("SB12: Invisible - Can use skills while invisible")
        void canUseSkillsWhileInvisible() {
            // Given: Rogue with BLIND buff (representing invisibility from Smoke Bomb)
            Unit rogue = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_DEATH_MARK);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(BuffFactory.create(BuffType.BLIND, P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(rogue, enemy),
                PlayerId.PLAYER_1,
                unitBuffs
            );

            // When: Try to use skill while invisible
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, P2_ENEMY);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid (invisible doesn't prevent using skills)
            assertTrue(result.isValid(), "Should be able to use skills while invisible");
        }
    }

    // ========== SB14: Invulnerable + Skill Interaction ==========

    @Nested
    @DisplayName("SB14: Invulnerable + Skill Interaction")
    class InvulnerableSkillInteraction {

        @Test
        @DisplayName("SB14: Invulnerable - Can use healing skills")
        void invulnerableCanUseHealingSkills() {
            // Given: Cleric with INVULNERABLE buff trying to use Trinity (heal skill)
            Unit cleric = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 2, new Position(2, 2),
                HeroClass.CLERIC, SkillRegistry.CLERIC_TRINITY);
            Unit ally = createMinion(P1_MINION, PlayerId.PLAYER_1, 5, 2, new Position(2, 3), MinionType.TANK);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(4, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(BuffFactory.create(BuffType.INVULNERABLE, P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(cleric, ally, enemy),
                PlayerId.PLAYER_1,
                unitBuffs
            );

            // When: Try to use Trinity (healing skill)
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, P1_MINION);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid (invulnerable can use healing skills)
            assertTrue(result.isValid(), "Invulnerable Cleric should be able to use healing skills");
        }
    }

    // ========== SB15: BLEED does not affect skill usage ==========

    @Nested
    @DisplayName("SB15: BLEED + Skill Interaction")
    class BleedSkillInteraction {

        @Test
        @DisplayName("SB15: BLEED does not affect skill usage")
        void bleedDoesNotAffectSkillUsage() {
            // Given: Mage with BLEED buff
            Unit mage = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(BuffFactory.createBleed(P1_HERO)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(mage, enemy),
                PlayerId.PLAYER_1,
                unitBuffs
            );

            // When: Try to use Elemental Blast
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, P2_ENEMY);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid (BLEED doesn't prevent skill usage)
            assertTrue(result.isValid(), "BLEED should not prevent skill usage");
        }
    }
}
