package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffFlags;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.*;
import com.tactics.engine.skill.SkillRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SV-Series: Skill Validation Tests
 * Tests target type, range, and unit state validation for USE_SKILL action.
 */
@DisplayName("SV-Series: Skill Validation")
public class RuleEngineSkillValidationTest {

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
                            HeroClass heroClass, String skillId) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0,
            0, false, false, false, 0, null,
            0, false, null,
            0, 0);
    }

    private Unit createHeroWithCooldown(String id, PlayerId owner, int hp, int attack, Position pos,
                                        HeroClass heroClass, String skillId, int cooldown) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, cooldown,
            0, false, false, false, 0, null,
            0, false, null,
            0, 0);
    }

    private Unit createHeroNoSkill(String id, PlayerId owner, int hp, int attack, Position pos,
                                   HeroClass heroClass) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            null, 0,  // No skill selected
            0, false, false, false, 0, null,
            0, false, null,
            0, 0);
    }

    private Unit createDeadHero(String id, PlayerId owner, Position pos,
                                HeroClass heroClass, String skillId) {
        return new Unit(id, owner, 0, 3, 2, 1, pos, false,  // alive = false
            UnitCategory.HERO, null, heroClass, 10,
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

    private GameState createGameOverState(List<Unit> units, PlayerId winner) {
        return new GameState(
            new Board(5, 5),
            units,
            PlayerId.PLAYER_1,
            true, winner,  // gameOver = true
            new HashMap<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            1, null
        );
    }

    // =========================================================================
    // SV1-SV5: Basic Skill Validation
    // =========================================================================

    @Nested
    @DisplayName("SV1-SV5: Basic Skill Validation")
    class BasicValidationTests {

        @Test
        @DisplayName("SV1: USE_SKILL requires hero unit")
        void useSkillRequiresHeroUnit() {
            // Given: Minion (not hero)
            Unit minion = createMinion(P1_MINION, PlayerId.PLAYER_1, 5, 2,
                new Position(2, 2), MinionType.ARCHER);
            Unit enemy = createHero(P2_HERO, PlayerId.PLAYER_2, 10, 3,
                new Position(2, 4), HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);

            GameState state = createGameState(Arrays.asList(minion, enemy), PlayerId.PLAYER_1);

            // When: Minion tries to use skill
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_MINION, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "USE_SKILL should require hero unit");
            assertTrue(result.getErrorMessage().toLowerCase().contains("hero") ||
                       result.getErrorMessage().toLowerCase().contains("only"),
                "Error should mention hero requirement");
        }

        @Test
        @DisplayName("SV2: USE_SKILL requires selectedSkillId not null")
        void useSkillRequiresSelectedSkillId() {
            // Given: Hero with no skill selected
            Unit hero = createHeroNoSkill(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 2), HeroClass.WARRIOR);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Hero with no skill tries to use skill
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "USE_SKILL should require skill selected");
            assertTrue(result.getErrorMessage().toLowerCase().contains("no skill") ||
                       result.getErrorMessage().toLowerCase().contains("selected"),
                "Error should mention no skill selected");
        }

        @Test
        @DisplayName("SV3: USE_SKILL requires cooldown = 0")
        void useSkillRequiresCooldownZero() {
            // Given: Hero with cooldown > 0
            Unit hero = createHeroWithCooldown(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 2), HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE, 1);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Hero with cooldown tries to use skill
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "USE_SKILL should require cooldown = 0");
            assertTrue(result.getErrorMessage().toLowerCase().contains("cooldown"),
                "Error should mention cooldown");
        }

        @Test
        @DisplayName("SV4: USE_SKILL requires hero alive")
        void useSkillRequiresHeroAlive() {
            // Given: Dead hero
            Unit deadHero = createDeadHero(P1_HERO, PlayerId.PLAYER_1,
                new Position(2, 2), HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(deadHero, enemy), PlayerId.PLAYER_1);

            // When: Dead hero tries to use skill
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "USE_SKILL should require hero alive");
        }

        @Test
        @DisplayName("SV5: USE_SKILL requires correct player's turn")
        void useSkillRequiresCorrectPlayerTurn() {
            // Given: It's PLAYER_1's turn
            Unit hero1 = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(0, 0), HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit hero2 = createHero(P2_HERO, PlayerId.PLAYER_2, 10, 3,
                new Position(4, 4), HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);

            GameState state = createGameState(Arrays.asList(hero1, hero2), PlayerId.PLAYER_1);

            // When: PLAYER_2's hero tries to use skill on PLAYER_1's turn
            Action action = Action.useSkill(PlayerId.PLAYER_2, P2_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "USE_SKILL should require correct player's turn");
        }
    }

    // =========================================================================
    // SV6-SV10: Target Type Validation
    // =========================================================================

    @Nested
    @DisplayName("SV6-SV10: Target Type Validation")
    class TargetTypeValidationTests {

        @Test
        @DisplayName("SV6: USE_SKILL checks target in range")
        void useSkillChecksTargetInRange() {
            // Given: Spirit Hawk range = 4, target at distance 8
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(0, 0), HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPIRIT_HAWK);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 4), MinionType.ARCHER);  // Distance 8

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Target out of range
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, enemy.getPosition(), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "Target out of range should be invalid");
            assertTrue(result.getErrorMessage().toLowerCase().contains("range"),
                "Error should mention range");
        }

        @Test
        @DisplayName("SV7: SINGLE_TILE target must be empty")
        void singleTileTargetMustBeEmpty() {
            // Given: Heroic Leap targets tile, tile is occupied
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(0, 0), HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP);
            Unit blocker = createMinion(P1_MINION, PlayerId.PLAYER_1, 5, 2,
                new Position(2, 0), MinionType.TANK);  // Blocking target tile
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, blocker, enemy), PlayerId.PLAYER_1);

            // When: Target occupied tile
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, blocker.getPosition(), null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "SINGLE_TILE target must be empty");
            // Error message may vary - just check it's invalid
            assertNotNull(result.getErrorMessage(), "Should have error message");
        }

        @Test
        @DisplayName("SV8: SINGLE_ENEMY target must be enemy")
        void singleEnemyTargetMustBeEnemy() {
            // Given: Spirit Hawk targets enemy, try to target ally
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(0, 0), HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPIRIT_HAWK);
            Unit ally = createMinion(P1_MINION, PlayerId.PLAYER_1, 5, 2,
                new Position(2, 0), MinionType.TANK);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, ally, enemy), PlayerId.PLAYER_1);

            // When: Target friendly unit
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, ally.getPosition(), P1_MINION);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "SINGLE_ENEMY target must be enemy");
            assertTrue(result.getErrorMessage().toLowerCase().contains("enemy"),
                "Error should mention enemy requirement");
        }

        @Test
        @DisplayName("SV9: SINGLE_ENEMY target must be alive")
        void singleEnemyTargetMustBeAlive() {
            // Given: Spirit Hawk targets enemy, enemy is dead
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(0, 0), HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPIRIT_HAWK);
            Unit deadEnemy = new Unit(P2_MINION, PlayerId.PLAYER_2, 0, 2, 2, 1,
                new Position(4, 0), false,  // dead
                UnitCategory.MINION, MinionType.ARCHER, null, 5,
                null, 0, 0, false, false, false, 0, null,
                0, false, null, 0, 0);

            GameState state = createGameState(Arrays.asList(hero, deadEnemy), PlayerId.PLAYER_1);

            // When: Target dead unit
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, deadEnemy.getPosition(), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "SINGLE_ENEMY target must be alive");
        }

        @Test
        @DisplayName("SV10: SINGLE_ALLY target must be friendly")
        void singleAllyTargetMustBeFriendly() {
            // Given: Trinity targets ally, try to target enemy
            Unit cleric = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(0, 0), HeroClass.CLERIC, SkillRegistry.CLERIC_TRINITY);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(cleric, enemy), PlayerId.PLAYER_1);

            // When: Target enemy with ally skill
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, enemy.getPosition(), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "SINGLE_ALLY target must be friendly");
            assertTrue(result.getErrorMessage().toLowerCase().contains("ally") ||
                       result.getErrorMessage().toLowerCase().contains("friendly"),
                "Error should mention ally/friendly requirement");
        }
    }

    // =========================================================================
    // SV11-SV13: Special Target Types
    // =========================================================================

    @Nested
    @DisplayName("SV11-SV13: Special Target Types")
    class SpecialTargetTypesTests {

        @Test
        @DisplayName("SV11: SELF target ignores targetPosition")
        void selfTargetIgnoresTargetPosition() {
            // Given: Endure is SELF target
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 2), HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: SELF target skill with null target
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid
            assertTrue(result.isValid(), "SELF target should be valid with null target");
        }

        @Test
        @DisplayName("SV12: LINE target must be orthogonal direction")
        void lineTargetMustBeOrthogonal() {
            // Given: Spectral Blades must be horizontal or vertical
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 2), HeroClass.HUNTRESS, SkillRegistry.HUNTRESS_SPECTRAL_BLADES);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 3), MinionType.ARCHER);  // Diagonal

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Diagonal target
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, new Position(3, 3), null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "LINE target must be orthogonal");
            assertTrue(result.getErrorMessage().toLowerCase().contains("orthogonal") ||
                       result.getErrorMessage().toLowerCase().contains("horizontal") ||
                       result.getErrorMessage().toLowerCase().contains("vertical") ||
                       result.getErrorMessage().toLowerCase().contains("line"),
                "Error should mention orthogonal/line requirement");
        }

        @Test
        @DisplayName("SV13: Hero can only use skills from their class")
        void heroCanOnlyUseOwnClassSkills() {
            // Given: Warrior hero with Mage skill selected (invalid)
            Unit hero = new Unit(P1_HERO, PlayerId.PLAYER_1, 10, 3, 2, 1, new Position(2, 2), true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, 10,
                SkillRegistry.MAGE_ELEMENTAL_BLAST, 0,  // Wrong class skill
                0, false, false, false, 0, null,
                0, false, null, 0, 0);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Use wrong class skill
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, enemy.getPosition(), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "Hero can only use skills from their class");
            assertTrue(result.getErrorMessage().toLowerCase().contains("class") ||
                       result.getErrorMessage().toLowerCase().contains("wrong"),
                "Error should mention class restriction");
        }
    }

    // =========================================================================
    // SV14-SV17: Buff State Validation
    // =========================================================================

    @Nested
    @DisplayName("SV14-SV17: Buff State Validation")
    class BuffStateValidationTests {

        @Test
        @DisplayName("SV14: STUN prevents USE_SKILL")
        void stunPreventsUseSkill() {
            // Given: Hero is stunned
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 2), HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance stunBuff = new BuffInstance("stun", "enemy", 2, false, null, BuffFlags.stunned());
            unitBuffs.put(P1_HERO, Collections.singletonList(stunBuff));

            GameState state = createGameStateWithBuffs(Arrays.asList(hero, enemy), PlayerId.PLAYER_1, unitBuffs);

            // When: Stunned hero tries to use skill
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "STUN should prevent USE_SKILL");
            assertTrue(result.getErrorMessage().toLowerCase().contains("stun"),
                "Error should mention stun");
        }

        @Test
        @DisplayName("SV15: ROOT does NOT prevent USE_SKILL (unless skill involves movement)")
        void rootDoesNotPreventNonMovementSkill() {
            // Given: Hero is rooted, using non-movement skill
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(0, 0), HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 0), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance rootBuff = new BuffInstance("root", "enemy", 2, false, null, BuffFlags.rooted());
            unitBuffs.put(P1_HERO, Collections.singletonList(rootBuff));

            GameState state = createGameStateWithBuffs(Arrays.asList(hero, enemy), PlayerId.PLAYER_1, unitBuffs);

            // When: Rooted hero uses non-movement skill
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, enemy.getPosition(), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid (ROOT doesn't block non-movement skills)
            assertTrue(result.isValid(), "ROOT should NOT prevent non-movement skills");
        }

        @Test
        @DisplayName("SV16: Rooted hero can still use non-movement skill")
        void rootedHeroCanUseNonMovementSkill() {
            // Given: Hero is rooted, using non-movement skill (Endure)
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(0, 0), HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(4, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance rootBuff = new BuffInstance("root", "enemy", 2, false, null, BuffFlags.rooted());
            unitBuffs.put(P1_HERO, Collections.singletonList(rootBuff));

            GameState state = createGameStateWithBuffs(Arrays.asList(hero, enemy), PlayerId.PLAYER_1, unitBuffs);

            // When: Rooted hero uses Endure (self-targeting skill)
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid (ROOT doesn't prevent Endure)
            assertTrue(result.isValid(), "ROOT should not prevent non-movement skills like Endure");
        }

        @Test
        @DisplayName("SV17: SLOW allows skill declaration")
        void slowAllowsSkillDeclaration() {
            // Given: Hero has SLOW buff
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(0, 0), HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(3, 0), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(
                BuffFactory.create(BuffType.SLOW, "enemy")));

            GameState state = createGameStateWithBuffs(Arrays.asList(hero, enemy), PlayerId.PLAYER_1, unitBuffs);

            // When: Slowed hero tries to use skill
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, enemy.getPosition(), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be valid (skill enters preparing state)
            assertTrue(result.isValid(), "SLOW should allow skill declaration (delayed execution)");
        }
    }

    // =========================================================================
    // SV18: Game State Validation
    // =========================================================================

    @Nested
    @DisplayName("SV18: Game State Validation")
    class GameStateValidationTests {

        @Test
        @DisplayName("SV18: Game over prevents USE_SKILL")
        void gameOverPreventsUseSkill() {
            // Given: Game is over
            Unit hero = createHero(P1_HERO, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 2), HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion(P2_MINION, PlayerId.PLAYER_2, 5, 2,
                new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameOverState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Try to use skill after game over
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_HERO, null, null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "Game over should prevent USE_SKILL");
        }
    }
}
