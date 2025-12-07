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
 * Tests for V3 Hero Skill System - Phase 4D.
 * Complex skills: Wild Magic, Elemental Strike, Death Mark, Ascended Form,
 * Shadow Clone, Feint, Challenge.
 */
public class RuleEngineSkillPhase4DTest {

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

    // =========================================================================
    // Wild Magic Tests
    // =========================================================================

    @Nested
    @DisplayName("Wild Magic Tests")
    class WildMagicTests {

        @Test
        @DisplayName("Wild Magic deals 1 damage to all enemies")
        void wildMagicDealsAoeDamage() {
            // Setup: Mage at (2,2), three enemies at various positions
            Unit mage = createHero("mage1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_WILD_MAGIC);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 5, 2, new Position(0, 0), MinionType.ARCHER);
            Unit enemy2 = createMinion("enemy2", PlayerId.PLAYER_2, 5, 2, new Position(4, 4), MinionType.TANK);
            Unit enemy3 = createMinion("enemy3", PlayerId.PLAYER_2, 5, 2, new Position(4, 0), MinionType.ASSASSIN);

            GameState state = createGameState(Arrays.asList(mage, enemy1, enemy2, enemy3), PlayerId.PLAYER_1);

            // Use Wild Magic (deterministic - no debuffs)
            ruleEngine.setRngProvider(new RngProvider(12345L) {
                @Override public int nextInt(int bound) { return 99; } // > 33, no debuff
            });

            Action action = Action.useSkill(PlayerId.PLAYER_1, "mage1", null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // All enemies should take 1 damage
            assertEquals(4, findUnit(result, "enemy1").getHp(), "Enemy1 should have 4 HP (5-1)");
            assertEquals(4, findUnit(result, "enemy2").getHp(), "Enemy2 should have 4 HP (5-1)");
            assertEquals(4, findUnit(result, "enemy3").getHp(), "Enemy3 should have 4 HP (5-1)");

            // Mage should have skill on cooldown
            assertEquals(2, findUnit(result, "mage1").getSkillCooldown(), "Mage skill should be on cooldown");
        }

        @Test
        @DisplayName("Wild Magic can apply random debuffs")
        void wildMagicCanApplyDebuffs() {
            // Setup: Mage at (2,2), one enemy
            Unit mage = createHero("mage1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_WILD_MAGIC);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 5, 2, new Position(0, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(mage, enemy1), PlayerId.PLAYER_1);

            // Use Wild Magic (deterministic - always apply debuff, choose BLEED)
            ruleEngine.setRngProvider(new RngProvider(12345L) {
                private int callCount = 0;
                @Override public int nextInt(int bound) {
                    callCount++;
                    if (bound == 100) return 0;  // < 33, apply debuff
                    if (bound == 3) return 1;    // Choose BLEED (index 1)
                    return 0;
                }
            });

            Action action = Action.useSkill(PlayerId.PLAYER_1, "mage1", null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Enemy should have BLEED debuff
            assertTrue(hasBuffType(result, "enemy1", BuffType.BLEED), "Enemy should have BLEED debuff");
        }
    }

    // =========================================================================
    // Elemental Strike Tests
    // =========================================================================

    @Nested
    @DisplayName("Elemental Strike Tests")
    class ElementalStrikeTests {

        @Test
        @DisplayName("Elemental Strike deals 3 damage and applies chosen debuff")
        void elementalStrikeDealsAndAppliesDebuff() {
            // Setup: Duelist at (2,2), enemy adjacent at (2,3)
            Unit duelist = createHero("duelist1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.DUELIST, SkillRegistry.DUELIST_ELEMENTAL_STRIKE);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(2, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy1), PlayerId.PLAYER_1);

            // Use Elemental Strike with SLOW debuff chosen
            Action action = Action.useSkillWithBuffChoice(PlayerId.PLAYER_1, "duelist1", "enemy1", BuffType.SLOW);
            GameState result = ruleEngine.applyAction(state, action);

            // Enemy should take 3 damage
            assertEquals(7, findUnit(result, "enemy1").getHp(), "Enemy should have 7 HP (10-3)");

            // Enemy should have SLOW debuff
            assertTrue(hasBuffType(result, "enemy1", BuffType.SLOW), "Enemy should have SLOW debuff");

            // Duelist should have skill on cooldown
            assertEquals(2, findUnit(result, "duelist1").getSkillCooldown(), "Duelist skill should be on cooldown");
        }

        @Test
        @DisplayName("Elemental Strike defaults to BLEED if no buff specified")
        void elementalStrikeDefaultsToBleed() {
            // Setup: Duelist at (2,2), enemy adjacent at (2,3)
            Unit duelist = createHero("duelist1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.DUELIST, SkillRegistry.DUELIST_ELEMENTAL_STRIKE);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(2, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy1), PlayerId.PLAYER_1);

            // Use Elemental Strike without specifying debuff
            Action action = Action.useSkill(PlayerId.PLAYER_1, "duelist1", null, "enemy1");
            GameState result = ruleEngine.applyAction(state, action);

            // Enemy should have BLEED debuff (default)
            assertTrue(hasBuffType(result, "enemy1", BuffType.BLEED), "Enemy should have BLEED debuff (default)");
        }
    }

    // =========================================================================
    // Death Mark Tests
    // =========================================================================

    @Nested
    @DisplayName("Death Mark Tests")
    class DeathMarkTests {

        @Test
        @DisplayName("Death Mark applies DEATH_MARK debuff to target")
        void deathMarkAppliesDebuff() {
            // Setup: Rogue at (2,2), enemy at (2,4) (range 2)
            Unit rogue = createHero("rogue1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_DEATH_MARK);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, enemy1), PlayerId.PLAYER_1);

            // Use Death Mark on enemy
            Action action = Action.useSkill(PlayerId.PLAYER_1, "rogue1", null, "enemy1");
            GameState result = ruleEngine.applyAction(state, action);

            // Enemy should have DEATH_MARK debuff
            assertTrue(hasBuffType(result, "enemy1", BuffType.DEATH_MARK), "Enemy should have DEATH_MARK debuff");

            // Rogue should have skill on cooldown
            assertEquals(2, findUnit(result, "rogue1").getSkillCooldown(), "Rogue skill should be on cooldown");
        }

        @Test
        @DisplayName("Death Mark buff tracks source unit")
        void deathMarkTracksSource() {
            // Setup: Rogue at (2,2), enemy at (2,4)
            Unit rogue = createHero("rogue1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_DEATH_MARK);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, enemy1), PlayerId.PLAYER_1);

            // Use Death Mark on enemy
            Action action = Action.useSkill(PlayerId.PLAYER_1, "rogue1", null, "enemy1");
            GameState result = ruleEngine.applyAction(state, action);

            // Find the DEATH_MARK buff and verify source
            List<BuffInstance> enemyBuffs = getUnitBuffs(result, "enemy1");
            BuffInstance deathMark = null;
            for (BuffInstance buff : enemyBuffs) {
                if (buff.getType() == BuffType.DEATH_MARK) {
                    deathMark = buff;
                    break;
                }
            }
            assertNotNull(deathMark, "Should find DEATH_MARK buff");
            assertEquals("rogue1", deathMark.getSourceUnitId(), "Source should be rogue1");
        }
    }

    // =========================================================================
    // Ascended Form Tests
    // =========================================================================

    @Nested
    @DisplayName("Ascended Form Tests")
    class AscendedFormTests {

        @Test
        @DisplayName("Ascended Form makes Cleric invulnerable")
        void ascendedFormGrantsInvulnerability() {
            // Setup: Cleric at (2,2)
            Unit cleric = createHero("cleric1", PlayerId.PLAYER_1, 10, 2, new Position(2, 2),
                HeroClass.CLERIC, SkillRegistry.CLERIC_ASCENDED_FORM);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(0, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(cleric, enemy1), PlayerId.PLAYER_1);

            // Use Ascended Form
            Action action = Action.useSkill(PlayerId.PLAYER_1, "cleric1", null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Cleric should be invulnerable
            assertTrue(findUnit(result, "cleric1").isInvulnerable(), "Cleric should be invulnerable");

            // Cleric should have INVULNERABLE buff
            assertTrue(hasBuffType(result, "cleric1", BuffType.INVULNERABLE), "Cleric should have INVULNERABLE buff");

            // Skill should be on cooldown
            assertEquals(2, findUnit(result, "cleric1").getSkillCooldown(), "Cleric skill should be on cooldown");
        }

        @Test
        @DisplayName("Ascended Form invulnerability lasts 1 round")
        void ascendedFormExpiresAtRoundEnd() {
            // Setup: Cleric with invulnerability (already acted)
            Unit cleric = createHero("cleric1", PlayerId.PLAYER_1, 10, 2, new Position(2, 2),
                HeroClass.CLERIC, SkillRegistry.CLERIC_ASCENDED_FORM);
            cleric = cleric.withSkillUsedAndInvulnerable(2, true);  // has acted, invulnerable

            // Enemy has also acted (so round will end when player1 ends turn)
            Unit enemy1 = new Unit("enemy1", PlayerId.PLAYER_2, 10, 2, 2, 1, new Position(0, 0), true,
                UnitCategory.MINION, MinionType.ARCHER, null, 10,
                null, 0,
                0, false, false, false, 0, null,
                1, false, null,  // actionsUsed = 1 (has acted)
                0, 0);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("cleric1", Arrays.asList(BuffFactory.create(BuffType.INVULNERABLE, "cleric1")));

            GameState state = createGameStateWithBuffs(Arrays.asList(cleric, enemy1), PlayerId.PLAYER_1, unitBuffs);

            // Player 1 ends turn, then Player 2 ends turn to trigger round reset
            Action endTurn1 = new Action(ActionType.END_TURN, PlayerId.PLAYER_1, null, null);
            GameState afterP1 = ruleEngine.applyAction(state, endTurn1);

            Action endTurn2 = new Action(ActionType.END_TURN, PlayerId.PLAYER_2, null, null);
            GameState result = ruleEngine.applyAction(afterP1, endTurn2);

            // Invulnerability should be cleared at round end
            assertFalse(findUnit(result, "cleric1").isInvulnerable(), "Invulnerability should expire at round end");
        }
    }

    // =========================================================================
    // Shadow Clone Tests
    // =========================================================================

    @Nested
    @DisplayName("Shadow Clone Tests")
    class ShadowCloneTests {

        @Test
        @DisplayName("Shadow Clone spawns a unit on adjacent tile")
        void shadowCloneSpawnsUnit() {
            // Setup: Rogue at (2,2), wants to spawn clone at (2,3)
            Unit rogue = createHero("rogue1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SHADOW_CLONE);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(0, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, enemy1), PlayerId.PLAYER_1);

            // Use Shadow Clone at (2,3)
            Action action = Action.useSkill(PlayerId.PLAYER_1, "rogue1", new Position(2, 3), null);
            GameState result = ruleEngine.applyAction(state, action);

            // Should have 3 units now (rogue, enemy, clone)
            assertEquals(3, result.getUnits().size(), "Should have 3 units after spawning clone");

            // Find the clone
            Unit clone = null;
            for (Unit u : result.getUnits()) {
                if (u.getId().startsWith("rogue1_clone_")) {
                    clone = u;
                    break;
                }
            }
            assertNotNull(clone, "Should find the clone");

            // Clone should be at target position
            assertEquals(new Position(2, 3), clone.getPosition(), "Clone should be at (2,3)");

            // Clone should have 1 HP, 1 ATK
            assertEquals(1, clone.getHp(), "Clone should have 1 HP");
            assertEquals(1, clone.getAttack(), "Clone should have 1 ATK");

            // Clone should be temporary
            assertTrue(clone.isTemporary(), "Clone should be temporary");
            assertEquals(2, clone.getTemporaryDuration(), "Clone should last 2 rounds");

            // Clone should belong to the same player
            assertEquals(PlayerId.PLAYER_1, clone.getOwner(), "Clone should belong to Player 1");
        }

        @Test
        @DisplayName("Shadow Clone is a minion (doesn't end game on death)")
        void shadowCloneIsMinion() {
            // Setup: Rogue at (2,2), wants to spawn clone at (2,3)
            Unit rogue = createHero("rogue1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SHADOW_CLONE);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(0, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, enemy1), PlayerId.PLAYER_1);

            // Use Shadow Clone
            Action action = Action.useSkill(PlayerId.PLAYER_1, "rogue1", new Position(2, 3), null);
            GameState result = ruleEngine.applyAction(state, action);

            // Find the clone
            Unit clone = null;
            for (Unit u : result.getUnits()) {
                if (u.getId().startsWith("rogue1_clone_")) {
                    clone = u;
                    break;
                }
            }
            assertNotNull(clone, "Should find the clone");

            // Clone should be a minion
            assertEquals(UnitCategory.MINION, clone.getCategory(), "Clone should be a minion");
        }
    }

    // =========================================================================
    // Feint Tests
    // =========================================================================

    @Nested
    @DisplayName("Feint Tests")
    class FeintTests {

        @Test
        @DisplayName("Feint applies FEINT buff to Duelist")
        void feintAppliesBuff() {
            // Setup: Duelist at (2,2)
            Unit duelist = createHero("duelist1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.DUELIST, SkillRegistry.DUELIST_FEINT);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(0, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy1), PlayerId.PLAYER_1);

            // Use Feint
            Action action = Action.useSkill(PlayerId.PLAYER_1, "duelist1", null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Duelist should have FEINT buff
            assertTrue(hasBuffType(result, "duelist1", BuffType.FEINT), "Duelist should have FEINT buff");

            // Skill should be on cooldown
            assertEquals(2, findUnit(result, "duelist1").getSkillCooldown(), "Duelist skill should be on cooldown");
        }

        @Test
        @DisplayName("Feint buff lasts 2 rounds")
        void feintBuffLastsTwoRounds() {
            // Setup: Duelist with Feint buff
            Unit duelist = createHero("duelist1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.DUELIST, SkillRegistry.DUELIST_FEINT);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(0, 0), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("duelist1", Arrays.asList(BuffFactory.create(BuffType.FEINT, "duelist1")));

            GameState state = createGameStateWithBuffs(Arrays.asList(duelist, enemy1), PlayerId.PLAYER_1, unitBuffs);

            // Check that buff has duration 2
            List<BuffInstance> duelistBuffs = getUnitBuffs(state, "duelist1");
            BuffInstance feintBuff = null;
            for (BuffInstance buff : duelistBuffs) {
                if (buff.getType() == BuffType.FEINT) {
                    feintBuff = buff;
                    break;
                }
            }
            assertNotNull(feintBuff, "Should find FEINT buff");
            assertEquals(2, feintBuff.getDuration(), "FEINT buff should last 2 rounds");
        }
    }

    // =========================================================================
    // Challenge Tests
    // =========================================================================

    @Nested
    @DisplayName("Challenge Tests")
    class ChallengeTests {

        @Test
        @DisplayName("Challenge applies CHALLENGE buff to enemy")
        void challengeAppliesBuff() {
            // Setup: Duelist at (2,2), enemy at (2,4) (range 2)
            Unit duelist = createHero("duelist1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.DUELIST, SkillRegistry.DUELIST_CHALLENGE);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy1), PlayerId.PLAYER_1);

            // Use Challenge on enemy
            Action action = Action.useSkill(PlayerId.PLAYER_1, "duelist1", null, "enemy1");
            GameState result = ruleEngine.applyAction(state, action);

            // Enemy should have CHALLENGE buff
            assertTrue(hasBuffType(result, "enemy1", BuffType.CHALLENGE), "Enemy should have CHALLENGE buff");

            // Skill should be on cooldown
            assertEquals(2, findUnit(result, "duelist1").getSkillCooldown(), "Duelist skill should be on cooldown");
        }

        @Test
        @DisplayName("Challenge buff tracks source unit (Duelist)")
        void challengeTracksSource() {
            // Setup: Duelist at (2,2), enemy at (2,4)
            Unit duelist = createHero("duelist1", PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                HeroClass.DUELIST, SkillRegistry.DUELIST_CHALLENGE);
            Unit enemy1 = createMinion("enemy1", PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy1), PlayerId.PLAYER_1);

            // Use Challenge
            Action action = Action.useSkill(PlayerId.PLAYER_1, "duelist1", null, "enemy1");
            GameState result = ruleEngine.applyAction(state, action);

            // Find the CHALLENGE buff and verify source
            List<BuffInstance> enemyBuffs = getUnitBuffs(result, "enemy1");
            BuffInstance challengeBuff = null;
            for (BuffInstance buff : enemyBuffs) {
                if (buff.getType() == BuffType.CHALLENGE) {
                    challengeBuff = buff;
                    break;
                }
            }
            assertNotNull(challengeBuff, "Should find CHALLENGE buff");
            assertEquals("duelist1", challengeBuff.getSourceUnitId(), "Source should be duelist1");
        }
    }
}
