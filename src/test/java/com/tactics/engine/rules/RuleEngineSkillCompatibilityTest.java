package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFactory;
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
 * SBC-Series: Backward Compatibility Tests (from SKILL_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests to ensure skill system doesn't break existing functionality:
 * - SBC1: No skills: Game functions normally
 * - SBC2: No skills: USE_SKILL action is invalid
 * - SBC3: V2 rules still apply with skills (moveRange/attackRange)
 * - SBC4: V1 BUFF rules still apply with skills
 * - SBC5: Minions cannot use skills (verified by SV1)
 */
@DisplayName("SBC-Series: Backward Compatibility")
public class RuleEngineSkillCompatibilityTest {

    private RuleEngine ruleEngine;
    private static final PlayerId P1 = PlayerId.PLAYER_1;
    private static final PlayerId P2 = PlayerId.PLAYER_2;

    private static final String P1_HERO = "p1_hero";
    private static final String P1_MINION = "p1_minion";
    private static final String P2_HERO = "p2_hero";
    private static final String P2_MINION = "p2_minion";

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // ========== Helper Methods ==========

    private Unit createHeroWithSkill(String id, PlayerId owner, int hp, int attack, Position pos,
                                      HeroClass heroClass, String skillId) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, 0,
            0, false, false, false, 0, null,
            0, false, null, 0, 0);
    }

    private Unit createHeroWithoutSkill(String id, PlayerId owner, int hp, int attack, Position pos,
                                         HeroClass heroClass) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            null, 0,  // no skill selected
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

    private Unit createMinionWithMoveRange(String id, PlayerId owner, int hp, int attack, Position pos,
                                            MinionType minionType, int moveRange) {
        return new Unit(id, owner, hp, attack, moveRange, 1, pos, true,
            UnitCategory.MINION, minionType, null, hp,
            null, 0,
            0, false, false, false, 0, null,
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

    // ========== SBC1: No Skills - Game Functions Normally ==========

    @Nested
    @DisplayName("SBC1: No skills - Game functions normally")
    class NoSkillsGameFunctionsTests {

        @Test
        @DisplayName("SBC1: Hero without skill can still move")
        void heroWithoutSkillCanMove() {
            // Given: Hero with no selectedSkillId
            Unit hero = createHeroWithoutSkill(P1_HERO, P1, 10, 3, new Position(2, 2), HeroClass.WARRIOR);
            Unit enemy = createMinion(P2_MINION, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), P1);

            // When: Try to move
            Action move = Action.move(P1_HERO, new Position(2, 3));
            ValidationResult result = ruleEngine.validateAction(state, move);

            // Then: Should be valid
            assertTrue(result.isValid(), "Hero without skill should still be able to move");
        }

        @Test
        @DisplayName("SBC1b: Hero without skill can still attack")
        void heroWithoutSkillCanAttack() {
            // Given: Hero with no selectedSkillId, adjacent enemy
            Unit hero = createHeroWithoutSkill(P1_HERO, P1, 10, 3, new Position(2, 2), HeroClass.WARRIOR);
            Unit enemy = createMinion(P2_MINION, P2, 10, 3, new Position(2, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), P1);

            // When: Try to attack
            Action attack = Action.attack(P1_HERO, new Position(2, 3), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, attack);

            // Then: Should be valid
            assertTrue(result.isValid(), "Hero without skill should still be able to attack");
        }

        @Test
        @DisplayName("SBC1c: Minion can move and attack normally")
        void minionCanMoveAndAttackNormally() {
            // Given: Minion (no skill possible), adjacent enemy
            Unit minion = createMinion(P1_MINION, P1, 5, 2, new Position(2, 2), MinionType.TANK);
            Unit enemy = createMinion(P2_MINION, P2, 10, 3, new Position(2, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(minion, enemy), P1);

            // When: Minion moves
            Action move = Action.move(P1_MINION, new Position(1, 2));
            ValidationResult moveResult = ruleEngine.validateAction(state, move);
            assertTrue(moveResult.isValid(), "Minion should be able to move");

            // When: Minion attacks (if adjacent)
            Action attack = Action.attack(P1_MINION, new Position(2, 3), P2_MINION);
            ValidationResult attackResult = ruleEngine.validateAction(state, attack);
            assertTrue(attackResult.isValid(), "Minion should be able to attack");
        }
    }

    // ========== SBC2: No Skills - USE_SKILL Invalid ==========

    @Nested
    @DisplayName("SBC2: No skills - USE_SKILL action is invalid")
    class NoSkillsUseSkillInvalidTests {

        @Test
        @DisplayName("SBC2: Hero with no selectedSkillId cannot use USE_SKILL")
        void heroWithoutSkillCannotUseSkill() {
            // Given: Hero with no selectedSkillId
            Unit hero = createHeroWithoutSkill(P1_HERO, P1, 10, 3, new Position(2, 2), HeroClass.WARRIOR);
            Unit enemy = createMinion(P2_MINION, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), P1);

            // When: Try to use skill
            Action action = Action.useSkill(P1, P1_HERO, new Position(3, 3), null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "Hero with no skill selected should not be able to USE_SKILL");
        }
    }

    // ========== SBC3: V2 Rules Still Apply ==========

    @Nested
    @DisplayName("SBC3: V2 rules still apply with skills")
    class V2RulesStillApplyTests {

        @Test
        @DisplayName("SBC3: moveRange still limits movement even with skill")
        void moveRangeStillLimitsMovement() {
            // Given: Hero with skill, but limited moveRange (2)
            Unit hero = createHeroWithSkill(P1_HERO, P1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion(P2_MINION, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), P1);

            // When: Try to move beyond moveRange (distance 4)
            Action move = Action.move(P1_HERO, new Position(0, 4));
            ValidationResult result = ruleEngine.validateAction(state, move);

            // Then: Should be invalid (exceeds moveRange of 2)
            assertFalse(result.isValid(),
                "Normal move should still be limited by moveRange even with skill");
        }

        @Test
        @DisplayName("SBC3b: attackRange still limits attack even with skill")
        void attackRangeStillLimitsAttack() {
            // Given: Hero with skill (attackRange = 1)
            Unit hero = createHeroWithSkill(P1_HERO, P1, 10, 3, new Position(0, 0),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion(P2_MINION, P2, 10, 3, new Position(0, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), P1);

            // When: Try to attack beyond attackRange (distance 3)
            Action attack = Action.attack(P1_HERO, new Position(0, 3), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, attack);

            // Then: Should be invalid (exceeds attackRange of 1)
            assertFalse(result.isValid(),
                "Normal attack should still be limited by attackRange even with skill");
        }
    }

    // ========== SBC4: V1 BUFF Rules Still Apply ==========

    @Nested
    @DisplayName("SBC4: V1 BUFF rules still apply with skills")
    class V1BuffRulesStillApplyTests {

        @Test
        @DisplayName("SBC4: BLIND buff still prevents attacks even with skills")
        void blindStillPreventsAttacks() {
            // Given: Hero with skill, but also has BLIND debuff (cannot attack)
            Unit hero = createHeroWithSkill(P1_HERO, P1, 10, 3, new Position(2, 2),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion(P2_MINION, P2, 10, 3, new Position(2, 3), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(
                BuffFactory.create(BuffType.BLIND, "source")));

            GameState state = createGameStateWithBuffs(Arrays.asList(hero, enemy), P1, unitBuffs);

            // When: Try to attack (BLIND prevents this)
            Action attack = Action.attack(P1_HERO, new Position(2, 3), P2_MINION);
            ValidationResult result = ruleEngine.validateAction(state, attack);

            // Then: Attack should be invalid (blinded)
            assertFalse(result.isValid(),
                "Blinded hero should not be able to attack");
        }

        @Test
        @DisplayName("SBC4b: BLEED damage still applies at round end")
        void bleedStillDamagesAtRoundEnd() {
            // Given: Hero with skill and BLEED debuff
            Unit hero = createHeroWithSkill(P1_HERO, P1, 10, 3, new Position(2, 2),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_ENDURE);
            Unit enemy = createMinion(P2_MINION, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_HERO, Collections.singletonList(
                BuffFactory.createBleed("source")));

            // Hero has acted (1 action used)
            Unit heroActed = new Unit(P1_HERO, P1, 10, 3, 2, 1, new Position(2, 2), true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, 10,
                SkillRegistry.WARRIOR_ENDURE, 0,
                0, false, false, false, 0, null,
                1, false, null, 0, 0);  // actionsUsed = 1

            // Enemy has acted
            Unit enemyActed = new Unit(P2_MINION, P2, 10, 3, 2, 1, new Position(4, 4), true,
                UnitCategory.MINION, MinionType.ARCHER, null, 10,
                null, 0,
                0, false, false, false, 0, null,
                1, false, null, 0, 0);

            GameState state = createGameStateWithBuffs(Arrays.asList(heroActed, enemyActed), P1, unitBuffs);

            int hpBefore = findUnit(state, P1_HERO).getHp();

            // When: Round ends (both players end turn)
            Action endTurn1 = new Action(ActionType.END_TURN, P1, null, null);
            GameState afterP1End = ruleEngine.applyAction(state, endTurn1);

            Action endTurn2 = new Action(ActionType.END_TURN, P2, null, null);
            GameState afterRound = ruleEngine.applyAction(afterP1End, endTurn2);

            // Then: BLEED should have dealt damage
            int hpAfter = findUnit(afterRound, P1_HERO).getHp();
            assertTrue(hpAfter < hpBefore,
                "BLEED should still deal damage at round end: " + hpBefore + " -> " + hpAfter);
        }
    }

    // ========== SBC5: Minions Cannot Use Skills ==========

    @Nested
    @DisplayName("SBC5: Minions cannot use skills")
    class MinionsCannotUseSkillsTests {

        @Test
        @DisplayName("SBC5: Minion cannot use USE_SKILL action")
        void minionCannotUseSkill() {
            // Given: Minion (no skill capability)
            Unit minion = createMinion(P1_MINION, P1, 5, 2, new Position(2, 2), MinionType.TANK);
            Unit enemy = createMinion(P2_MINION, P2, 10, 3, new Position(4, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(minion, enemy), P1);

            // When: Minion tries to use skill
            Action action = Action.useSkill(P1, P1_MINION, new Position(3, 3), null);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid
            assertFalse(result.isValid(), "Minion should not be able to use USE_SKILL");
            assertTrue(result.getErrorMessage().toLowerCase().contains("hero") ||
                       result.getErrorMessage().toLowerCase().contains("minion") ||
                       result.getErrorMessage().toLowerCase().contains("skill"),
                "Error should mention skills are for heroes: " + result.getErrorMessage());
        }
    }
}
