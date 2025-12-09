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
 * SR-Series: ROGUE Skill Tests (from SKILL_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for Rogue skills:
 * - Smoke Bomb: Teleport, Invisible, Blind adjacent
 * - Death Mark: +2 damage, heal on kill
 * - Shadow Clone: Spawn, duration, death handling
 */
@DisplayName("SR-Series: ROGUE Skills")
public class RuleEngineRogueSkillTest {

    private RuleEngine ruleEngine;
    private static final PlayerId P1 = PlayerId.PLAYER_1;
    private static final PlayerId P2 = PlayerId.PLAYER_2;

    // Unit ID constants (must have p1_/p2_ prefix)
    private static final String P1_ROGUE = "p1_rogue";
    private static final String P1_MINION = "p1_minion";
    private static final String P2_ENEMY = "p2_enemy";
    private static final String P2_MAGE = "p2_mage";

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
            0, false, null, 0, 0);
    }

    private Unit createMinion(String id, PlayerId owner, int hp, int attack, Position pos,
                             MinionType minionType) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.MINION, minionType, null, hp,
            null, 0,
            0, false, false, false, 0, null,
            0, false, null, 0, 0);
    }

    private Unit createInvisibleHero(String id, PlayerId owner, int hp, int attack, Position pos,
                                     HeroClass heroClass, String skillId) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 2,  // cooldown = 2 (just used skill)
            0, true, false, false, 0, null,  // invisible = true
            0, false, null, 0, 0);
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

    private Unit findClone(GameState state, String rogueId) {
        return state.getUnits().stream()
            .filter(u -> u.getId().startsWith(rogueId + "_clone_"))
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

    // ========== Smoke Bomb Tests ==========

    @Nested
    @DisplayName("SR4-SR5: Smoke Bomb Interaction Tests")
    class SmokeBombInteractionTests {

        @Test
        @DisplayName("SR4: Invisible does not prevent AoE damage")
        void invisibleDoesNotPreventAoEDamage() {
            // Given: Invisible Rogue at (2,2), enemy Mage can use Wild Magic (AoE)
            Unit rogue = createInvisibleHero(P1_ROGUE, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SMOKE_BOMB);
            Unit mage = createHero(P2_MAGE, P2, 10, 3, new Position(4, 4),
                HeroClass.MAGE, SkillRegistry.MAGE_WILD_MAGIC);

            GameState state = createGameState(Arrays.asList(rogue, mage), P2);

            // Use fixed RNG to avoid debuff (need nextInt(100) >= 33 to skip debuff)
            // Seed 12345 gives first nextInt(100) = 51, which is >= 33 (no debuff)
            ruleEngine.setRngProvider(new RngProvider(12345));

            // When: Mage uses Wild Magic (AoE - damages all enemies)
            Action action = Action.useSkill(P2, P2_MAGE, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Rogue should take AoE damage (1 damage from Wild Magic)
            Unit rogueAfter = findUnit(result, P1_ROGUE);
            assertEquals(9, rogueAfter.getHp(),
                "Invisible Rogue should still take AoE damage (10 - 1 = 9 HP)");
        }

        @Test
        @DisplayName("SR5b: BLIND prevents unit from attacking for 1 round")
        void blindLastsOneRound() {
            // Given: Enemy with BLIND buff
            Unit rogue = createHero(P1_ROGUE, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SMOKE_BOMB);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(2, 3), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P2_ENEMY, Collections.singletonList(
                BuffFactory.create(BuffType.BLIND, P1_ROGUE).withDuration(1)));

            GameState state = createGameStateWithBuffs(Arrays.asList(rogue, enemy), P2, unitBuffs);

            // When: Blinded enemy tries to attack
            Action attack = Action.attack(P2_ENEMY, new Position(2, 2), P1_ROGUE);
            ValidationResult result = ruleEngine.validateAction(state, attack);

            // Then: Attack should be invalid
            assertFalse(result.isValid(), "Blinded unit should not be able to attack");
            assertTrue(result.getErrorMessage().toLowerCase().contains("blind"),
                "Error should mention blind: " + result.getErrorMessage());
        }
    }

    // ========== Death Mark Tests ==========

    @Nested
    @DisplayName("SR6-SR8: Death Mark Tests")
    class DeathMarkTests {

        @Test
        @DisplayName("SR6: Death Mark marks target for 2 rounds")
        void deathMarkMarksDuration() {
            // Given: Rogue at (2,2), enemy at (2,4)
            Unit rogue = createHero(P1_ROGUE, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_DEATH_MARK);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, enemy), P1);

            // When: Use Death Mark on enemy
            Action action = Action.useSkill(P1, P1_ROGUE, null, P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should have DEATH_MARK buff with duration 2
            List<BuffInstance> enemyBuffs = getUnitBuffs(result, P2_ENEMY);
            BuffInstance deathMark = enemyBuffs.stream()
                .filter(b -> b.getType() == BuffType.DEATH_MARK)
                .findFirst()
                .orElse(null);

            assertNotNull(deathMark, "Enemy should have DEATH_MARK buff");
            assertEquals(2, deathMark.getDuration(), "DEATH_MARK should last 2 rounds");
        }

        @Test
        @DisplayName("SR7: Marked target takes +2 damage from all attacks")
        void deathMarkIncreaseDamage() {
            // Given: Rogue at (2,2), marked enemy at (2,3) with base HP=10
            Unit rogue = createHero(P1_ROGUE, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_DEATH_MARK);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(2, 3), MinionType.ARCHER);

            // Apply DEATH_MARK debuff to enemy
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P2_ENEMY, Collections.singletonList(
                BuffFactory.create(BuffType.DEATH_MARK, P1_ROGUE)));

            GameState state = createGameStateWithBuffs(Arrays.asList(rogue, enemy), P1, unitBuffs);
            // Make rogue have acted so it can attack (actionsUsed = 0)

            // When: Rogue attacks marked enemy (base damage = 3)
            Action attack = Action.attack(P1_ROGUE, new Position(2, 3), P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Enemy should take 3 + 2 = 5 damage (10 - 5 = 5 HP)
            Unit enemyAfter = findUnit(result, P2_ENEMY);
            assertEquals(5, enemyAfter.getHp(),
                "Marked enemy should take +2 damage: base (3) + mark bonus (2) = 5 total damage, HP = 10 - 5 = 5");
        }

        @Test
        @DisplayName("SR8: Rogue heals 2 HP if marked target dies")
        void deathMarkHealOnKill() {
            // Given: Rogue at (2,2) with 5 HP, marked enemy at (2,3) with 3 HP
            Unit rogue = createHero(P1_ROGUE, P1, 5, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_DEATH_MARK);
            Unit enemy = createMinion(P2_ENEMY, P2, 3, 3, new Position(2, 3), MinionType.ARCHER);

            // Apply DEATH_MARK debuff with Rogue as source
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P2_ENEMY, Collections.singletonList(
                BuffFactory.create(BuffType.DEATH_MARK, P1_ROGUE)));

            GameState state = createGameStateWithBuffs(Arrays.asList(rogue, enemy), P1, unitBuffs);

            // When: Rogue attacks and kills marked enemy
            // Damage = 3 (base) + 2 (mark) = 5, which kills 3 HP enemy
            Action attack = Action.attack(P1_ROGUE, new Position(2, 3), P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Rogue should heal 2 HP (5 -> 7)
            Unit rogueAfter = findUnit(result, P1_ROGUE);
            assertEquals(7, rogueAfter.getHp(),
                "Rogue should heal 2 HP after killing marked target: 5 + 2 = 7 HP");

            // And enemy should be dead
            Unit enemyAfter = findUnit(result, P2_ENEMY);
            assertTrue(enemyAfter == null || enemyAfter.getHp() <= 0,
                "Marked enemy should be dead");
        }
    }

    // ========== Shadow Clone Tests ==========

    @Nested
    @DisplayName("SR9-SR14: Shadow Clone Tests")
    class ShadowCloneTests {

        @Test
        @DisplayName("SR11: Shadow Clone can move and attack")
        void shadowCloneCanMoveAndAttack() {
            // Given: Rogue has spawned a clone
            Unit rogue = createHero(P1_ROGUE, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SHADOW_CLONE);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, enemy), P1);

            // Spawn clone at (2,3)
            Action spawnAction = Action.useSkill(P1, P1_ROGUE, new Position(2, 3), null);
            GameState afterSpawn = ruleEngine.applyAction(state, spawnAction);

            // Find the clone
            Unit clone = findClone(afterSpawn, P1_ROGUE);
            assertNotNull(clone, "Clone should exist");
            String cloneId = clone.getId();

            // When: Validate clone can move
            Action moveAction = Action.move(cloneId, new Position(2, 4));
            ValidationResult moveResult = ruleEngine.validateAction(afterSpawn, moveAction);

            // Then: Clone should be able to move
            assertTrue(moveResult.isValid(),
                "Shadow Clone should be able to move: " + moveResult.getErrorMessage());
        }

        @Test
        @DisplayName("SR11b: Shadow Clone can attack")
        void shadowCloneCanAttack() {
            // Given: Rogue has spawned a clone, enemy is adjacent
            Unit rogue = createHero(P1_ROGUE, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SHADOW_CLONE);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, enemy), P1);

            // Spawn clone at (2,3) - adjacent to enemy at (2,4)
            Action spawnAction = Action.useSkill(P1, P1_ROGUE, new Position(2, 3), null);
            GameState afterSpawn = ruleEngine.applyAction(state, spawnAction);

            Unit clone = findClone(afterSpawn, P1_ROGUE);
            assertNotNull(clone, "Clone should exist");
            String cloneId = clone.getId();

            // When: Clone attacks enemy
            Action attackAction = Action.attack(cloneId, new Position(2, 4), P2_ENEMY);
            GameState afterAttack = ruleEngine.applyAction(afterSpawn, attackAction);

            // Then: Enemy should take 1 damage (clone has 1 ATK)
            Unit enemyAfter = findUnit(afterAttack, P2_ENEMY);
            assertEquals(9, enemyAfter.getHp(),
                "Enemy should take 1 damage from clone attack: 10 - 1 = 9 HP");
        }

        @Test
        @DisplayName("SR12: Shadow Clone lasts 2 rounds then disappears")
        void shadowCloneLasts2Rounds() {
            // Given: Rogue has spawned a clone (duration = 2)
            Unit rogue = createHero(P1_ROGUE, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SHADOW_CLONE);
            // Need both players to have units for round to end properly
            Unit rogueMinion = createMinion(P1_MINION, P1, 10, 3, new Position(0, 0), MinionType.TANK);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, rogueMinion, enemy), P1);

            // Spawn clone at (2,3)
            Action spawnAction = Action.useSkill(P1, P1_ROGUE, new Position(2, 3), null);
            GameState afterSpawn = ruleEngine.applyAction(state, spawnAction);

            Unit clone = findClone(afterSpawn, P1_ROGUE);
            assertNotNull(clone, "Clone should exist");
            assertEquals(2, clone.getTemporaryDuration(), "Clone should have 2 round duration");
            String cloneId = clone.getId();

            // Simulate round 1 end: all units end turn
            GameState afterRogueEnd = ruleEngine.applyAction(afterSpawn,
                Action.endTurn(P1_ROGUE));
            GameState afterCloneEnd = ruleEngine.applyAction(afterRogueEnd,
                Action.endTurn(cloneId));
            GameState afterMinionEnd = ruleEngine.applyAction(afterCloneEnd,
                Action.endTurn(P1_MINION));
            GameState afterRound1 = ruleEngine.applyAction(afterMinionEnd,
                Action.endTurn(P2_ENEMY));

            // Clone should still exist after round 1 (duration reduced to 1)
            Unit cloneAfterRound1 = findClone(afterRound1, P1_ROGUE);
            assertNotNull(cloneAfterRound1, "Clone should exist after round 1");
            assertEquals(1, cloneAfterRound1.getTemporaryDuration(),
                "Clone duration should be reduced to 1 after round 1");

            // Simulate round 2 end
            GameState r2afterRogue = ruleEngine.applyAction(afterRound1,
                Action.endTurn(P1_ROGUE));
            Unit cloneR2 = findClone(r2afterRogue, P1_ROGUE);
            GameState r2afterClone;
            if (cloneR2 != null) {
                r2afterClone = ruleEngine.applyAction(r2afterRogue, Action.endTurn(cloneR2.getId()));
            } else {
                r2afterClone = r2afterRogue;
            }
            GameState r2afterMinion = ruleEngine.applyAction(r2afterClone,
                Action.endTurn(P1_MINION));
            GameState afterRound2 = ruleEngine.applyAction(r2afterMinion,
                Action.endTurn(P2_ENEMY));

            // Clone should be removed after round 2
            Unit cloneAfterRound2 = findClone(afterRound2, P1_ROGUE);
            assertNull(cloneAfterRound2, "Clone should be removed after 2 rounds");
        }

        @Test
        @DisplayName("SR13: Shadow Clone death does NOT trigger death choice")
        void shadowCloneDeathNoDeathChoice() {
            // Given: Rogue has spawned a clone, enemy can kill it
            Unit rogue = createHero(P1_ROGUE, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SHADOW_CLONE);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 4, new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, enemy), P1);

            // Spawn clone at (2,3) - clone has 1 HP
            Action spawnAction = Action.useSkill(P1, P1_ROGUE, new Position(2, 3), null);
            GameState afterSpawn = ruleEngine.applyAction(state, spawnAction);

            Unit clone = findClone(afterSpawn, P1_ROGUE);
            assertNotNull(clone, "Clone should exist with 1 HP");

            // Enemy attacks clone (deals 4 damage, kills 1 HP clone)
            // First P1 units need to end turn so P2 can act
            GameState afterRogueEnd = ruleEngine.applyAction(afterSpawn,
                Action.endTurn(P1_ROGUE));
            GameState afterCloneEnd = ruleEngine.applyAction(afterRogueEnd,
                Action.endTurn(clone.getId()));

            // Now P2 can attack
            Action attackAction = Action.attack(P2_ENEMY, new Position(2, 3), clone.getId());
            GameState afterKill = ruleEngine.applyAction(afterCloneEnd, attackAction);

            // Then: No death choice should be triggered
            assertNull(afterKill.getPendingDeathChoice(),
                "Clone death should NOT trigger death choice");

            // Clone should be dead
            Unit cloneAfter = findClone(afterKill, P1_ROGUE);
            assertTrue(cloneAfter == null || cloneAfter.getHp() <= 0,
                "Clone should be dead");
        }

        @Test
        @DisplayName("SR14: Shadow Clone cannot be healed")
        void shadowCloneCannotBeHealed() {
            // Given: P1 has Rogue with clone, P1 also has Cleric who could heal
            Unit rogue = createHero(P1_ROGUE, P1, 10, 3, new Position(2, 2),
                HeroClass.ROGUE, SkillRegistry.ROGUE_SHADOW_CLONE);
            // Create a Cleric hero (would use Trinity skill to heal)
            Unit cleric = createHero("p1_cleric", P1, 10, 2, new Position(0, 0),
                HeroClass.CLERIC, SkillRegistry.CLERIC_TRINITY);
            Unit enemy = createMinion(P2_ENEMY, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(rogue, cleric, enemy), P1);

            // Spawn clone at (2,3)
            Action spawnAction = Action.useSkill(P1, P1_ROGUE, new Position(2, 3), null);
            GameState afterSpawn = ruleEngine.applyAction(state, spawnAction);

            Unit clone = findClone(afterSpawn, P1_ROGUE);
            assertNotNull(clone, "Clone should exist");
            String cloneId = clone.getId();

            // Rogue ends turn (used skill)
            GameState afterRogueEnd = ruleEngine.applyAction(afterSpawn,
                Action.endTurn(P1_ROGUE));

            // When: Cleric tries to heal the clone
            Action healAction = Action.useSkill(P1, "p1_cleric", clone.getPosition(), cloneId);
            ValidationResult result = ruleEngine.validateAction(afterRogueEnd, healAction);

            // Then: Heal should be invalid
            assertFalse(result.isValid(),
                "Should not be able to heal Shadow Clone: " + result.getErrorMessage());
            // Error message might vary based on implementation
        }
    }
}
