package com.tactics.engine.draft;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.*;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.rules.ValidationResult;
import com.tactics.engine.skill.SkillRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Draft Integration Tests (DI-Series from DRAFT_PHASE_TESTPLAN.md)
 *
 * Tests the full draft-to-battle flow:
 * - Complete draft, create GameState, play actions
 * - Different hero class combinations
 * - Different minion combinations
 * - Draft then battle (skills, guardian, round processing)
 */
@DisplayName("Draft Integration Tests")
class DraftIntegrationTest {

    private DraftSetupService setupService;
    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        setupService = new DraftSetupService();
        ruleEngine = new RuleEngine();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private DraftState createCompleteDraft(PlayerId playerId, HeroClass heroClass,
                                           MinionType minion1, MinionType minion2, String skillId) {
        return new DraftState(playerId, heroClass)
            .withMinion(minion1)
            .withMinion(minion2)
            .withSkill(skillId);
    }

    private Unit findUnit(GameState state, String id) {
        return state.getUnits().stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    private Unit findHero(GameState state, PlayerId owner) {
        return state.getUnits().stream()
            .filter(u -> u.getOwner().equals(owner) && u.isHero())
            .findFirst()
            .orElse(null);
    }

    private List<Unit> findMinions(GameState state, PlayerId owner) {
        return state.getUnits().stream()
            .filter(u -> u.getOwner().equals(owner) && u.isMinion())
            .collect(Collectors.toList());
    }

    // =========================================================================
    // DI1: Full Draft Flow
    // =========================================================================

    @Nested
    @DisplayName("DI1: Full Draft Flow")
    class FullDraftFlow {

        @Test
        @DisplayName("DI1.1: P1 completes draft, P2 completes draft -> DraftResult complete")
        void bothPlayersCompleteDraft() {
            // P1 drafts
            DraftState p1Draft = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            assertFalse(p1Draft.isComplete());

            p1Draft = p1Draft.withMinion(MinionType.TANK);
            assertFalse(p1Draft.isComplete());

            p1Draft = p1Draft.withMinion(MinionType.ARCHER);
            assertFalse(p1Draft.isComplete());  // Still needs skill

            p1Draft = p1Draft.withSkill(SkillRegistry.WARRIOR_ENDURE);
            assertTrue(p1Draft.isComplete());

            // P2 drafts
            DraftState p2Draft = new DraftState(PlayerId.PLAYER_2, HeroClass.MAGE);
            p2Draft = p2Draft.withMinion(MinionType.ASSASSIN);
            p2Draft = p2Draft.withMinion(MinionType.TANK);
            p2Draft = p2Draft.withSkill(SkillRegistry.MAGE_ELEMENTAL_BLAST);
            assertTrue(p2Draft.isComplete());

            // Create DraftResult
            DraftResult result = new DraftResult(p1Draft, p2Draft);
            assertTrue(result.isComplete());
        }

        @Test
        @DisplayName("DI1.2: Create GameState from DraftResult -> valid game ready to play")
        void createGameStateFromDraftResult() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);

            // Verify game is ready to play
            assertNotNull(state);
            assertEquals(6, state.getUnits().size());
            assertEquals(PlayerId.PLAYER_1, state.getCurrentPlayer());
            assertEquals(1, state.getCurrentRound());
            assertFalse(state.isGameOver());

            // Verify all units are alive and at correct positions
            state.getUnits().forEach(unit -> {
                assertTrue(unit.isAlive());
                assertNotNull(unit.getPosition());
            });
        }

        @Test
        @DisplayName("DI1.3: Units can perform MOVE action")
        void unitsCanMove() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(draftResult);

            // P1 Hero at (2,0) moves to (2,1)
            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            Action move = new Action(ActionType.MOVE, PlayerId.PLAYER_1, new Position(2, 1), null);

            ValidationResult result = ruleEngine.validateAction(state, move);
            assertTrue(result.isValid(), "Move should be valid: " + result.getErrorMessage());

            GameState newState = ruleEngine.applyAction(state, move);
            Unit movedHero = findHero(newState, PlayerId.PLAYER_1);
            assertEquals(new Position(2, 1), movedHero.getPosition());
        }

        @Test
        @DisplayName("DI1.4: GameState from draft supports skill usage")
        void gameStateSupportsSkillUsage() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(draftResult);

            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            assertEquals(0, p1Hero.getSkillCooldown());
            assertEquals(SkillRegistry.WARRIOR_ENDURE, p1Hero.getSelectedSkillId());

            // Use Endure skill (self-target)
            Action skill = Action.useSkill(PlayerId.PLAYER_1, p1Hero.getId(), p1Hero.getPosition(), p1Hero.getId());
            ValidationResult result = ruleEngine.validateAction(state, skill);
            assertTrue(result.isValid(), "Skill should be valid: " + result.getErrorMessage());

            GameState newState = ruleEngine.applyAction(state, skill);
            Unit heroAfterSkill = findHero(newState, PlayerId.PLAYER_1);

            // Endure gives 3 shield
            assertEquals(3, heroAfterSkill.getShield());
            // Cooldown is set to 2
            assertEquals(2, heroAfterSkill.getSkillCooldown());
        }
    }

    // =========================================================================
    // DI2: Different Hero Class Combinations
    // =========================================================================

    @Nested
    @DisplayName("DI2: Different Hero Class Combinations")
    class HeroClassCombinations {

        @Test
        @DisplayName("DI2.1: P1=WARRIOR, P2=MAGE -> both heroes created correctly")
        void warriorVsMage() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.WARRIOR_HEROIC_LEAP);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_WARP_BEACON);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);

            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            Unit p2Hero = findHero(state, PlayerId.PLAYER_2);

            assertEquals(HeroClass.WARRIOR, p1Hero.getHeroClass());
            assertEquals(HeroClass.MAGE, p2Hero.getHeroClass());
            assertEquals(SkillRegistry.WARRIOR_HEROIC_LEAP, p1Hero.getSelectedSkillId());
            assertEquals(SkillRegistry.MAGE_WARP_BEACON, p2Hero.getSelectedSkillId());
        }

        @Test
        @DisplayName("DI2.2: P1=ROGUE, P2=CLERIC -> both heroes created correctly")
        void rogueVsCleric() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.ROGUE,
                MinionType.ASSASSIN, MinionType.ASSASSIN,
                SkillRegistry.ROGUE_SMOKE_BOMB);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.CLERIC,
                MinionType.TANK, MinionType.TANK,
                SkillRegistry.CLERIC_TRINITY);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);

            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            Unit p2Hero = findHero(state, PlayerId.PLAYER_2);

            assertEquals(HeroClass.ROGUE, p1Hero.getHeroClass());
            assertEquals(HeroClass.CLERIC, p2Hero.getHeroClass());
        }

        @Test
        @DisplayName("DI2.3: Same class for both (WARRIOR vs WARRIOR) -> both work independently")
        void sameClassBothPlayers() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.WARRIOR,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.WARRIOR_SHOCKWAVE);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);

            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            Unit p2Hero = findHero(state, PlayerId.PLAYER_2);

            // Both are warriors but with different skills
            assertEquals(HeroClass.WARRIOR, p1Hero.getHeroClass());
            assertEquals(HeroClass.WARRIOR, p2Hero.getHeroClass());
            assertEquals(SkillRegistry.WARRIOR_ENDURE, p1Hero.getSelectedSkillId());
            assertEquals(SkillRegistry.WARRIOR_SHOCKWAVE, p2Hero.getSelectedSkillId());

            // They should have different IDs
            assertNotEquals(p1Hero.getId(), p2Hero.getId());
        }

        @Test
        @DisplayName("DI2.4: HUNTRESS with Spirit Hawk skill")
        void huntressWithSpiritHawk() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.HUNTRESS,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.HUNTRESS_SPIRIT_HAWK);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(draftResult);

            // P1 Huntress at (2,0), P2 Hero at (2,4) - distance 4
            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            Unit p2Hero = findHero(state, PlayerId.PLAYER_2);

            assertEquals(HeroClass.HUNTRESS, p1Hero.getHeroClass());

            // Spirit Hawk has range 4, should be able to hit P2 Hero
            Action skill = Action.useSkill(PlayerId.PLAYER_1, p1Hero.getId(), p2Hero.getPosition(), p2Hero.getId());
            ValidationResult result = ruleEngine.validateAction(state, skill);
            assertTrue(result.isValid(), "Spirit Hawk should hit at range 4: " + result.getErrorMessage());

            GameState newState = ruleEngine.applyAction(state, skill);
            p2Hero = findHero(newState, PlayerId.PLAYER_2);

            // Spirit Hawk deals 2 damage
            assertEquals(3, p2Hero.getHp());  // 5 - 2 = 3
        }

        @Test
        @DisplayName("DI2.5: DUELIST with Feint skill")
        void duelistWithFeint() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.DUELIST,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.DUELIST_FEINT);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(draftResult);

            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            assertEquals(HeroClass.DUELIST, p1Hero.getHeroClass());
            assertEquals(SkillRegistry.DUELIST_FEINT, p1Hero.getSelectedSkillId());
        }
    }

    // =========================================================================
    // DI3: Different Minion Combinations
    // =========================================================================

    @Nested
    @DisplayName("DI3: Different Minion Combinations")
    class MinionCombinations {

        @Test
        @DisplayName("DI3.1: P1=[TANK,ARCHER], P2=[ASSASSIN,TANK] -> all 4 minions created")
        void mixedMinions() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);

            List<Unit> p1Minions = findMinions(state, PlayerId.PLAYER_1);
            List<Unit> p2Minions = findMinions(state, PlayerId.PLAYER_2);

            assertEquals(2, p1Minions.size());
            assertEquals(2, p2Minions.size());

            // Check P1 minions
            assertTrue(p1Minions.stream().anyMatch(m -> m.getMinionType() == MinionType.TANK));
            assertTrue(p1Minions.stream().anyMatch(m -> m.getMinionType() == MinionType.ARCHER));

            // Check P2 minions
            assertTrue(p2Minions.stream().anyMatch(m -> m.getMinionType() == MinionType.ASSASSIN));
            assertTrue(p2Minions.stream().anyMatch(m -> m.getMinionType() == MinionType.TANK));
        }

        @Test
        @DisplayName("DI3.2: Both players same minions -> works, all have unique IDs")
        void sameMinionsForBoth() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);

            // Get all minion IDs
            List<String> allMinionIds = state.getUnits().stream()
                .filter(Unit::isMinion)
                .map(Unit::getId)
                .collect(Collectors.toList());

            // All 4 minions should have unique IDs
            assertEquals(4, allMinionIds.size());
            assertEquals(4, allMinionIds.stream().distinct().count());
        }

        @Test
        @DisplayName("DI3.3: All ASSASSIN team (P1=[ASSASSIN,ASSASSIN]) -> creates 2 assassins with correct stats")
        void allAssassinTeam() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.ROGUE,
                MinionType.ASSASSIN, MinionType.ASSASSIN,
                SkillRegistry.ROGUE_SMOKE_BOMB);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.TANK, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);

            List<Unit> p1Minions = findMinions(state, PlayerId.PLAYER_1);

            assertEquals(2, p1Minions.size());
            p1Minions.forEach(m -> {
                assertEquals(MinionType.ASSASSIN, m.getMinionType());
                assertEquals(2, m.getHp());  // ASSASSIN HP
                assertEquals(2, m.getAttack());  // ASSASSIN ATK
                assertEquals(4, m.getMoveRange());  // ASSASSIN move range
            });

            // Unique IDs
            assertNotEquals(p1Minions.get(0).getId(), p1Minions.get(1).getId());
        }

        @Test
        @DisplayName("DI3.4: ARCHER minion has attackRange 3")
        void archerHasRange3() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.ARCHER, MinionType.TANK,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(draftResult);

            Unit p1Archer = findUnit(state, "p1_minion_1");
            assertEquals(MinionType.ARCHER, p1Archer.getMinionType());
            assertEquals(3, p1Archer.getAttackRange());
            assertEquals(3, p1Archer.getHp());
            assertEquals(1, p1Archer.getAttack());
        }

        @Test
        @DisplayName("DI3.5: TANK minion has guardian stats")
        void tankHasGuardianStats() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(draftResult);

            Unit p1Tank = findUnit(state, "p1_minion_1");
            assertEquals(MinionType.TANK, p1Tank.getMinionType());
            assertEquals(5, p1Tank.getHp());
            assertEquals(1, p1Tank.getAttack());
            assertEquals(1, p1Tank.getMoveRange());
            assertEquals(1, p1Tank.getAttackRange());
        }
    }

    // =========================================================================
    // DI4: Draft then Round End
    // =========================================================================

    @Nested
    @DisplayName("DI4: Draft then Round End")
    class DraftThenRoundEnd {

        @Test
        @DisplayName("DI4.1: Play round, skill cooldown decrements")
        void skillCooldownDecrements() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(draftResult);

            assertEquals(1, state.getCurrentRound());

            // P1 uses skill then ends turn
            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            state = ruleEngine.applyAction(state,
                Action.useSkill(PlayerId.PLAYER_1, p1Hero.getId(), p1Hero.getPosition(), p1Hero.getId()));

            // Check cooldown is set
            p1Hero = findHero(state, PlayerId.PLAYER_1);
            assertEquals(2, p1Hero.getSkillCooldown());

            state = ruleEngine.applyAction(state,
                new Action(ActionType.END_TURN, PlayerId.PLAYER_1, null, null));

            // P2 ends turn
            state = ruleEngine.applyAction(state,
                new Action(ActionType.END_TURN, PlayerId.PLAYER_2, null, null));

            // After both end turn, round should increment
            assertEquals(2, state.getCurrentRound());

            // Skill cooldown should have decremented by 1
            p1Hero = findHero(state, PlayerId.PLAYER_1);
            assertEquals(1, p1Hero.getSkillCooldown());  // Was 2, now 1
        }

        @Test
        @DisplayName("DI4.2: Play round, minions take decay damage (starting round 3)")
        void minionDecayDamage() {
            // V3 Spec: Minion decay starts at round 3
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ARCHER,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);
            GameState state = setupService.createGameState(draftResult);

            // Advance state to round 3 to test decay
            state = new GameState(
                state.getBoard(), state.getUnits(), state.getCurrentPlayer(),
                state.isGameOver(), state.getWinner(), state.getUnitBuffs(),
                state.getBuffTiles(), state.getObstacles(), 3,  // Set to round 3
                state.getPendingDeathChoice(), false, false);

            // Check initial HP
            Unit p1Tank = findUnit(state, "p1_minion_1");
            assertEquals(5, p1Tank.getHp());

            // P1 ends turn
            state = ruleEngine.applyAction(state,
                new Action(ActionType.END_TURN, PlayerId.PLAYER_1, null, null));
            // P2 ends turn
            state = ruleEngine.applyAction(state,
                new Action(ActionType.END_TURN, PlayerId.PLAYER_2, null, null));

            // Minions should have taken 1 decay damage
            p1Tank = findUnit(state, "p1_minion_1");
            assertEquals(4, p1Tank.getHp());  // Was 5, now 4 due to decay
        }
    }
}
