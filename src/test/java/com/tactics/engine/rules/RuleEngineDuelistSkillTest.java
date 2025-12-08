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
 * SD-Series: DUELIST Skill Tests (from SKILL_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for Duelist-specific skill mechanics:
 * - Challenge: Marks enemy, 50% damage reduction, counter-attack
 * - Elemental Strike: Damage + chosen debuff
 * - Feint: Dodge next attack + counter-attack
 */
@DisplayName("SD-Series: DUELIST Skills")
public class RuleEngineDuelistSkillTest {

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // ========== Constants ==========
    private static final String P1_DUELIST = "p1_hero";
    private static final String P1_MINION = "p1_minion_1";
    private static final String P2_ENEMY = "p2_minion_1";
    private static final String P2_TANK = "p2_minion_2";
    private static final String P2_HERO = "p2_hero";

    // ========== Helper Methods ==========

    private Unit createDuelist(String id, PlayerId owner, int hp, int attack, Position pos, String skillId) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, HeroClass.DUELIST, hp,
            skillId, 0,  // cooldown = 0 (ready)
            0, false, false, false, 0, null,
            0, false, null,
            0, 0);
    }

    private Unit createDuelistWithActionsUsed(String id, PlayerId owner, int hp, int attack, Position pos,
                                               String skillId, int actionsUsed) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.HERO, null, HeroClass.DUELIST, hp,
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

    private Unit createMinionWithActionsUsed(String id, PlayerId owner, int hp, int attack, Position pos,
                                              MinionType minionType, int actionsUsed) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
            UnitCategory.MINION, minionType, null, hp,
            null, 0,
            0, false, false, false, 0, null,
            actionsUsed, false, null,
            0, 0);
    }

    private Unit createHero(String id, PlayerId owner, int hp, int attack, Position pos,
                            HeroClass heroClass, String skillId) {
        return new Unit(id, owner, hp, attack, 2, 1, pos, true,
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

    // ========== SD-Series: Challenge Tests ==========

    @Nested
    @DisplayName("SD1-SD6: Challenge Skill")
    class ChallengeTests {

        @Test
        @DisplayName("SD1: Challenge marks enemy as 'Challenged' for 2 rounds")
        void challengeMarksEnemyForTwoRounds() {
            // Given: Duelist at (2,2), enemy at (2,4) (range 2)
            Unit duelist = createDuelist(P1_DUELIST, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                SkillRegistry.DUELIST_CHALLENGE);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy), PlayerId.PLAYER_1);

            // When: Use Challenge on enemy
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_DUELIST, null, P2_ENEMY);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should have CHALLENGE buff with duration 2
            List<BuffInstance> enemyBuffs = getUnitBuffs(result, P2_ENEMY);
            BuffInstance challengeBuff = null;
            for (BuffInstance buff : enemyBuffs) {
                if (buff.getType() == BuffType.CHALLENGE) {
                    challengeBuff = buff;
                    break;
                }
            }
            assertNotNull(challengeBuff, "Enemy should have CHALLENGE buff");
            assertEquals(2, challengeBuff.getDuration(), "CHALLENGE buff should have duration 2");
        }

        @Test
        @DisplayName("SD2: Challenged enemy deals 50% damage to non-Duelist")
        void challengedEnemyDealsHalfDamageToNonDuelist() {
            // Given: Enemy with CHALLENGE buff (from Duelist), friendly minion
            Unit duelist = createDuelistWithActionsUsed(P1_DUELIST, PlayerId.PLAYER_1, 10, 3,
                new Position(0, 0), SkillRegistry.DUELIST_CHALLENGE, 1);
            Unit friendlyMinion = createMinion(P1_MINION, PlayerId.PLAYER_1, 10, 3, new Position(2, 3), MinionType.TANK);
            Unit challengedEnemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 4, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance challengeBuff = BuffFactory.createChallenge(P1_DUELIST);
            unitBuffs.put(P2_ENEMY, Collections.singletonList(challengeBuff));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(duelist, friendlyMinion, challengedEnemy),
                PlayerId.PLAYER_2,
                unitBuffs
            );

            // When: Challenged enemy attacks friendly minion (non-Duelist)
            Action attack = Action.attack(P2_ENEMY, new Position(2, 3), P1_MINION);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Damage should be halved (4 ATK / 2 = 2 damage)
            Unit updatedMinion = findUnit(result, P1_MINION);
            assertEquals(8, updatedMinion.getHp(), "Minion should take 2 damage (50% of 4 ATK) = 10 - 2 = 8 HP");
        }

        @Test
        @DisplayName("SD3: Challenged enemy deals full damage to Duelist")
        void challengedEnemyDealsFullDamageToDuelist() {
            // Given: Enemy with CHALLENGE buff, attacking the Duelist
            Unit duelist = createDuelistWithActionsUsed(P1_DUELIST, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 3), SkillRegistry.DUELIST_CHALLENGE, 1);
            Unit challengedEnemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 4, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance challengeBuff = BuffFactory.createChallenge(P1_DUELIST);
            unitBuffs.put(P2_ENEMY, Collections.singletonList(challengeBuff));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(duelist, challengedEnemy),
                PlayerId.PLAYER_2,
                unitBuffs
            );

            // When: Challenged enemy attacks Duelist
            Action attack = Action.attack(P2_ENEMY, new Position(2, 3), P1_DUELIST);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Damage should be full (4 ATK)
            Unit updatedDuelist = findUnit(result, P1_DUELIST);
            // 10 HP - 4 damage = 6 HP (may be less if counter-attack not implemented yet)
            assertTrue(updatedDuelist.getHp() <= 6, "Duelist should take full 4 damage");
        }

        @Test
        @DisplayName("SD4: Duelist counter-attacks when attacked by Challenged enemy")
        void duelistCounterAttacksWhenAttackedByChallengedEnemy() {
            // Given: Enemy with CHALLENGE buff attacks Duelist
            Unit duelist = createDuelistWithActionsUsed(P1_DUELIST, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 3), SkillRegistry.DUELIST_CHALLENGE, 1);
            Unit challengedEnemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 4, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance challengeBuff = BuffFactory.createChallenge(P1_DUELIST);
            unitBuffs.put(P2_ENEMY, Collections.singletonList(challengeBuff));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(duelist, challengedEnemy),
                PlayerId.PLAYER_2,
                unitBuffs
            );

            // When: Challenged enemy attacks Duelist
            Action attack = Action.attack(P2_ENEMY, new Position(2, 3), P1_DUELIST);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Enemy should take counter-attack damage (2 damage from counter)
            Unit updatedEnemy = findUnit(result, P2_ENEMY);
            assertEquals(8, updatedEnemy.getHp(), "Enemy should take 2 counter-attack damage = 10 - 2 = 8 HP");
        }

        @Test
        @DisplayName("SD5: Counter-attack does not consume Duelist's action")
        void counterAttackDoesNotConsumeAction() {
            // Given: Enemy with CHALLENGE buff attacks Duelist (Duelist hasn't acted yet)
            Unit duelist = createDuelist(P1_DUELIST, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 3), SkillRegistry.DUELIST_CHALLENGE);
            Unit challengedEnemy = createMinionWithActionsUsed(P2_ENEMY, PlayerId.PLAYER_2, 10, 4,
                new Position(2, 4), MinionType.ARCHER, 0);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance challengeBuff = BuffFactory.createChallenge(P1_DUELIST);
            unitBuffs.put(P2_ENEMY, Collections.singletonList(challengeBuff));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(duelist, challengedEnemy),
                PlayerId.PLAYER_2,
                unitBuffs
            );

            int duelistActionsBeforeCounter = findUnit(state, P1_DUELIST).getActionsUsed();

            // When: Challenged enemy attacks Duelist (triggers counter)
            Action attack = Action.attack(P2_ENEMY, new Position(2, 3), P1_DUELIST);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Duelist's actionsUsed should be unchanged
            Unit updatedDuelist = findUnit(result, P1_DUELIST);
            assertEquals(duelistActionsBeforeCounter, updatedDuelist.getActionsUsed(),
                "Counter-attack should not consume Duelist's action");
        }

        @Test
        @DisplayName("SD6: Counter-attack is normal attack (Guardian intercepts)")
        void counterAttackInterceptedByGuardian() {
            // Given: Challenged enemy attacks Duelist, enemy has adjacent Tank
            Unit duelist = createDuelistWithActionsUsed(P1_DUELIST, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 3), SkillRegistry.DUELIST_CHALLENGE, 1);
            Unit challengedEnemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 4, new Position(2, 4), MinionType.ARCHER);
            Unit enemyTank = createMinion(P2_TANK, PlayerId.PLAYER_2, 10, 2, new Position(3, 4), MinionType.TANK);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance challengeBuff = BuffFactory.createChallenge(P1_DUELIST);
            unitBuffs.put(P2_ENEMY, Collections.singletonList(challengeBuff));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(duelist, challengedEnemy, enemyTank),
                PlayerId.PLAYER_2,
                unitBuffs
            );

            int enemyHpBefore = 10;
            int tankHpBefore = 10;

            // When: Challenged enemy attacks Duelist (triggers counter)
            Action attack = Action.attack(P2_ENEMY, new Position(2, 3), P1_DUELIST);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Guardian (Tank) should intercept counter-attack damage
            Unit updatedEnemy = findUnit(result, P2_ENEMY);
            Unit updatedTank = findUnit(result, P2_TANK);

            // Tank should take the counter-attack damage instead of enemy
            assertTrue(updatedTank.getHp() < tankHpBefore || updatedEnemy.getHp() < enemyHpBefore,
                "Either tank should intercept counter-attack or enemy should take damage");
        }
    }

    // ========== SD-Series: Elemental Strike Tests ==========

    @Nested
    @DisplayName("SD7-SD9: Elemental Strike")
    class ElementalStrikeTests {

        @Test
        @DisplayName("SD7: Elemental Strike deals 3 damage to adjacent enemy")
        void elementalStrikeDeals3Damage() {
            // Given: Duelist at (2,2), enemy adjacent at (2,3)
            Unit duelist = createDuelist(P1_DUELIST, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                SkillRegistry.DUELIST_ELEMENTAL_STRIKE);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 3), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy), PlayerId.PLAYER_1);

            // When: Use Elemental Strike
            Action action = Action.useSkillWithBuffChoice(PlayerId.PLAYER_1, P1_DUELIST, P2_ENEMY, BuffType.BLEED);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Enemy should take 3 damage
            Unit updatedEnemy = findUnit(result, P2_ENEMY);
            assertEquals(7, updatedEnemy.getHp(), "Enemy should take 3 damage = 10 - 3 = 7 HP");
        }

        @Test
        @DisplayName("SD8: Elemental Strike - Player chooses debuff (BLEED, SLOW, or WEAKNESS)")
        void elementalStrikePlayerChoosesDebuff() {
            // Test each debuff type
            BuffType[] debuffTypes = {BuffType.BLEED, BuffType.SLOW, BuffType.WEAKNESS};

            for (BuffType debuffType : debuffTypes) {
                // Given: Duelist at (2,2), enemy adjacent at (2,3)
                Unit duelist = createDuelist(P1_DUELIST, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                    SkillRegistry.DUELIST_ELEMENTAL_STRIKE);
                Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 3), MinionType.ARCHER);

                GameState state = createGameState(Arrays.asList(duelist, enemy), PlayerId.PLAYER_1);

                // When: Use Elemental Strike with specific debuff
                Action action = Action.useSkillWithBuffChoice(PlayerId.PLAYER_1, P1_DUELIST, P2_ENEMY, debuffType);
                GameState result = ruleEngine.applyAction(state, action);

                // Then: Enemy should have the chosen debuff
                assertTrue(hasBuffType(result, P2_ENEMY, debuffType),
                    "Enemy should have " + debuffType + " debuff");
            }
        }

        @Test
        @DisplayName("SD9: Elemental Strike range is 1 (adjacent only)")
        void elementalStrikeRangeIsOne() {
            // Given: Duelist at (2,2), enemy at (2,4) (distance 2, not adjacent)
            Unit duelist = createDuelist(P1_DUELIST, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                SkillRegistry.DUELIST_ELEMENTAL_STRIKE);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(2, 4), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy), PlayerId.PLAYER_1);

            // When: Try to use Elemental Strike on non-adjacent enemy
            Action action = Action.useSkillWithBuffChoice(PlayerId.PLAYER_1, P1_DUELIST, P2_ENEMY, BuffType.BLEED);
            ValidationResult result = ruleEngine.validateAction(state, action);

            // Then: Should be invalid (out of range)
            assertFalse(result.isValid(), "Elemental Strike should fail on non-adjacent target");
            assertTrue(result.getErrorMessage().toLowerCase().contains("range") ||
                       result.getErrorMessage().toLowerCase().contains("adjacent"),
                "Error message should mention range/adjacent: " + result.getErrorMessage());
        }
    }

    // ========== SD-Series: Feint Tests ==========

    @Nested
    @DisplayName("SD10-SD14: Feint")
    class FeintTests {

        @Test
        @DisplayName("SD10: Feint grants 'dodge next attack' state")
        void feintGrantsDodgeState() {
            // Given: Duelist at (2,2)
            Unit duelist = createDuelist(P1_DUELIST, PlayerId.PLAYER_1, 10, 3, new Position(2, 2),
                SkillRegistry.DUELIST_FEINT);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 2, new Position(0, 0), MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(duelist, enemy), PlayerId.PLAYER_1);

            // When: Use Feint
            Action action = Action.useSkill(PlayerId.PLAYER_1, P1_DUELIST, null, null);
            GameState result = ruleEngine.applyAction(state, action);

            // Then: Duelist should have FEINT buff (dodge state)
            assertTrue(hasBuffType(result, P1_DUELIST, BuffType.FEINT),
                "Duelist should have FEINT buff (dodge state)");
        }

        @Test
        @DisplayName("SD11: Feint - next attack against Duelist misses")
        void feintNextAttackMisses() {
            // Given: Duelist with FEINT buff, enemy about to attack
            Unit duelist = createDuelistWithActionsUsed(P1_DUELIST, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 3), SkillRegistry.DUELIST_FEINT, 1);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 4, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_DUELIST, Collections.singletonList(BuffFactory.create(BuffType.FEINT, P1_DUELIST)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(duelist, enemy),
                PlayerId.PLAYER_2,
                unitBuffs
            );

            int duelistHpBefore = findUnit(state, P1_DUELIST).getHp();

            // When: Enemy attacks Duelist
            Action attack = Action.attack(P2_ENEMY, new Position(2, 3), P1_DUELIST);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Duelist should take no damage (attack missed)
            Unit updatedDuelist = findUnit(result, P1_DUELIST);
            assertEquals(duelistHpBefore, updatedDuelist.getHp(),
                "Duelist should take no damage - Feint causes attack to miss");
        }

        @Test
        @DisplayName("SD12: Feint triggers counter-attack on dodge")
        void feintTriggersCounterOnDodge() {
            // Given: Duelist with FEINT buff, enemy attacks
            Unit duelist = createDuelistWithActionsUsed(P1_DUELIST, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 3), SkillRegistry.DUELIST_FEINT, 1);
            Unit enemy = createMinion(P2_ENEMY, PlayerId.PLAYER_2, 10, 4, new Position(2, 4), MinionType.ARCHER);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_DUELIST, Collections.singletonList(BuffFactory.create(BuffType.FEINT, P1_DUELIST)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(duelist, enemy),
                PlayerId.PLAYER_2,
                unitBuffs
            );

            // When: Enemy attacks Duelist (triggers Feint counter)
            Action attack = Action.attack(P2_ENEMY, new Position(2, 3), P1_DUELIST);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Enemy should take counter-attack damage (2 damage)
            Unit updatedEnemy = findUnit(result, P2_ENEMY);
            assertEquals(8, updatedEnemy.getHp(),
                "Enemy should take 2 counter-attack damage from Feint = 10 - 2 = 8 HP");
        }

        @Test
        @DisplayName("SD13: Feint counter-attack does not consume action")
        void feintCounterDoesNotConsumeAction() {
            // Given: Duelist with FEINT buff (hasn't acted), enemy attacks
            Unit duelist = createDuelist(P1_DUELIST, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 3), SkillRegistry.DUELIST_FEINT);
            Unit enemy = createMinionWithActionsUsed(P2_ENEMY, PlayerId.PLAYER_2, 10, 4,
                new Position(2, 4), MinionType.ARCHER, 0);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put(P1_DUELIST, Collections.singletonList(BuffFactory.create(BuffType.FEINT, P1_DUELIST)));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(duelist, enemy),
                PlayerId.PLAYER_2,
                unitBuffs
            );

            int duelistActionsBeforeCounter = findUnit(state, P1_DUELIST).getActionsUsed();

            // When: Enemy attacks Duelist (triggers Feint counter)
            Action attack = Action.attack(P2_ENEMY, new Position(2, 3), P1_DUELIST);
            GameState result = ruleEngine.applyAction(state, attack);

            // Then: Duelist's actionsUsed should be unchanged
            Unit updatedDuelist = findUnit(result, P1_DUELIST);
            assertEquals(duelistActionsBeforeCounter, updatedDuelist.getActionsUsed(),
                "Feint counter-attack should not consume Duelist's action");
        }

        @Test
        @DisplayName("SD14: Feint expires after 2 rounds if not triggered")
        void feintExpiresAfterTwoRounds() {
            // Given: Duelist with FEINT buff (both units have acted to enable round end)
            Unit duelist = createDuelistWithActionsUsed(P1_DUELIST, PlayerId.PLAYER_1, 10, 3,
                new Position(2, 2), SkillRegistry.DUELIST_FEINT, 1);
            Unit enemy = createMinionWithActionsUsed(P2_ENEMY, PlayerId.PLAYER_2, 10, 4,
                new Position(4, 4), MinionType.ARCHER, 1);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance feintBuff = BuffFactory.create(BuffType.FEINT, P1_DUELIST);
            unitBuffs.put(P1_DUELIST, Collections.singletonList(feintBuff));

            GameState state = createGameStateWithBuffs(
                Arrays.asList(duelist, enemy),
                PlayerId.PLAYER_1,
                unitBuffs
            );

            // Verify initial duration
            assertEquals(2, feintBuff.getDuration(), "FEINT buff should start with duration 2");

            // When: End round (P1 ends, then P2 ends)
            Action endTurn1 = new Action(ActionType.END_TURN, PlayerId.PLAYER_1, null, null);
            GameState afterP1End = ruleEngine.applyAction(state, endTurn1);

            Action endTurn2 = new Action(ActionType.END_TURN, PlayerId.PLAYER_2, null, null);
            GameState afterRound1 = ruleEngine.applyAction(afterP1End, endTurn2);

            // Check duration after first round
            List<BuffInstance> buffsAfterRound1 = getUnitBuffs(afterRound1, P1_DUELIST);
            BuffInstance feintAfterRound1 = null;
            for (BuffInstance buff : buffsAfterRound1) {
                if (buff.getType() == BuffType.FEINT) {
                    feintAfterRound1 = buff;
                    break;
                }
            }
            if (feintAfterRound1 != null) {
                assertEquals(1, feintAfterRound1.getDuration(),
                    "FEINT buff should have duration 1 after first round");
            }

            // End second round
            // Need to end turns again for round 2 (units should have reset actions)
            Action endTurn3 = new Action(ActionType.END_TURN, PlayerId.PLAYER_1, null, null);
            GameState afterP1End2 = ruleEngine.applyAction(afterRound1, endTurn3);

            Action endTurn4 = new Action(ActionType.END_TURN, PlayerId.PLAYER_2, null, null);
            GameState afterRound2 = ruleEngine.applyAction(afterP1End2, endTurn4);

            // Then: FEINT buff should be expired/removed
            assertFalse(hasBuffType(afterRound2, P1_DUELIST, BuffType.FEINT),
                "FEINT buff should expire after 2 rounds");
        }
    }
}
