package com.tactics.engine.draft;

import com.tactics.engine.model.*;
import com.tactics.engine.skill.SkillRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DraftSetupService Tests (SP-Series and PO-Series from DRAFT_PHASE_TESTPLAN.md)
 */
@DisplayName("Draft Setup Service Tests")
class DraftSetupServiceTest {

    private DraftSetupService setupService;

    @BeforeEach
    void setUp() {
        setupService = new DraftSetupService();
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

    private DraftResult createCompleteDraftResult() {
        DraftState p1Draft = createCompleteDraft(
            PlayerId.PLAYER_1, HeroClass.WARRIOR,
            MinionType.TANK, MinionType.ARCHER,
            SkillRegistry.WARRIOR_ENDURE);
        DraftState p2Draft = createCompleteDraft(
            PlayerId.PLAYER_2, HeroClass.MAGE,
            MinionType.ASSASSIN, MinionType.TANK,
            SkillRegistry.MAGE_ELEMENTAL_BLAST);
        return new DraftResult(p1Draft, p2Draft);
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
    // SP1: GameState Creation
    // =========================================================================

    @Nested
    @DisplayName("SP1: GameState Creation")
    class GameStateCreation {

        @Test
        @DisplayName("SP1.1: Create GameState from complete draft")
        void createGameStateFromCompleteDraft() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);

            assertNotNull(state);
        }

        @Test
        @DisplayName("SP1.2: Create from incomplete draft throws exception")
        void createFromIncompleteDraftThrows() {
            DraftState p1Draft = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK);  // Incomplete - only 1 minion
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            assertThrows(IllegalArgumentException.class,
                () -> setupService.createGameState(draftResult));
        }

        @Test
        @DisplayName("SP1.3: GameState has 6 units (3 per player)")
        void gameStateHas6Units() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);

            assertEquals(6, state.getUnits().size());
            // 3 units per player
            long p1Units = state.getUnits().stream()
                .filter(u -> u.getOwner().equals(PlayerId.PLAYER_1))
                .count();
            long p2Units = state.getUnits().stream()
                .filter(u -> u.getOwner().equals(PlayerId.PLAYER_2))
                .count();
            assertEquals(3, p1Units);
            assertEquals(3, p2Units);
        }

        @Test
        @DisplayName("SP1.4: GameState round starts at 1")
        void gameStateRoundStartsAt1() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);

            assertEquals(1, state.getCurrentRound());
        }

        @Test
        @DisplayName("SP1.5: GameState player 1 starts")
        void gameStatePlayer1Starts() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);

            assertEquals(PlayerId.PLAYER_1, state.getCurrentPlayer());
        }

        @Test
        @DisplayName("SP1.6: GameState not game over")
        void gameStateNotGameOver() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);

            assertFalse(state.isGameOver());
            assertNull(state.getWinner());
        }
    }

    // =========================================================================
    // SP2: Hero Creation
    // =========================================================================

    @Nested
    @DisplayName("SP2: Hero Creation")
    class HeroCreation {

        @Test
        @DisplayName("SP2.1: P1 Hero created with correct class")
        void p1HeroCreatedWithCorrectClass() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit hero = findHero(state, PlayerId.PLAYER_1);

            assertNotNull(hero);
            assertEquals(UnitCategory.HERO, hero.getCategory());
            assertEquals(HeroClass.WARRIOR, hero.getHeroClass());
            assertNull(hero.getMinionType());
        }

        @Test
        @DisplayName("SP2.2: P2 Hero created with correct class")
        void p2HeroCreatedWithCorrectClass() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit hero = findHero(state, PlayerId.PLAYER_2);

            assertNotNull(hero);
            assertEquals(UnitCategory.HERO, hero.getCategory());
            assertEquals(HeroClass.MAGE, hero.getHeroClass());
            assertNull(hero.getMinionType());
        }

        @Test
        @DisplayName("SP2.3: Hero has selected skill")
        void heroHasSelectedSkill() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            Unit p2Hero = findHero(state, PlayerId.PLAYER_2);

            assertEquals(SkillRegistry.WARRIOR_ENDURE, p1Hero.getSelectedSkillId());
            assertEquals(SkillRegistry.MAGE_ELEMENTAL_BLAST, p2Hero.getSelectedSkillId());
        }

        @Test
        @DisplayName("SP2.4: Hero skill cooldown starts at 0")
        void heroSkillCooldownStartsAt0() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            Unit p2Hero = findHero(state, PlayerId.PLAYER_2);

            assertEquals(0, p1Hero.getSkillCooldown());
            assertEquals(0, p2Hero.getSkillCooldown());
        }

        @Test
        @DisplayName("SP2.5: Hero HP = 5")
        void heroHp5() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit hero = findHero(state, PlayerId.PLAYER_1);

            assertEquals(5, hero.getHp());
            assertEquals(5, hero.getMaxHp());
        }

        @Test
        @DisplayName("SP2.6: Hero ATK = 1")
        void heroAtk1() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit hero = findHero(state, PlayerId.PLAYER_1);

            assertEquals(1, hero.getAttack());
        }

        @Test
        @DisplayName("SP2.7: Hero moveRange = 1")
        void heroMoveRange1() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit hero = findHero(state, PlayerId.PLAYER_1);

            assertEquals(1, hero.getMoveRange());
        }

        @Test
        @DisplayName("SP2.8: Hero attackRange = 1")
        void heroAttackRange1() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit hero = findHero(state, PlayerId.PLAYER_1);

            assertEquals(1, hero.getAttackRange());
        }
    }

    // =========================================================================
    // SP3: Minion Creation
    // =========================================================================

    @Nested
    @DisplayName("SP3: Minion Creation")
    class MinionCreation {

        @Test
        @DisplayName("SP3.1: TANK minion created with correct stats")
        void tankMinionStats() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.TANK,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.TANK, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);
            List<Unit> minions = findMinions(state, PlayerId.PLAYER_1);
            Unit tank = minions.get(0);

            assertEquals(5, tank.getHp());
            assertEquals(5, tank.getMaxHp());
            assertEquals(1, tank.getAttack());
            assertEquals(1, tank.getMoveRange());
            assertEquals(1, tank.getAttackRange());
        }

        @Test
        @DisplayName("SP3.2: ARCHER minion created with correct stats")
        void archerMinionStats() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.ARCHER, MinionType.ARCHER,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ARCHER, MinionType.ARCHER,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);
            List<Unit> minions = findMinions(state, PlayerId.PLAYER_1);
            Unit archer = minions.get(0);

            assertEquals(3, archer.getHp());
            assertEquals(3, archer.getMaxHp());
            assertEquals(1, archer.getAttack());
            assertEquals(1, archer.getMoveRange());
            assertEquals(3, archer.getAttackRange());
        }

        @Test
        @DisplayName("SP3.3: ASSASSIN minion created with correct stats")
        void assassinMinionStats() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.ASSASSIN, MinionType.ASSASSIN,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.ASSASSIN,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);
            List<Unit> minions = findMinions(state, PlayerId.PLAYER_1);
            Unit assassin = minions.get(0);

            assertEquals(2, assassin.getHp());
            assertEquals(2, assassin.getMaxHp());
            assertEquals(2, assassin.getAttack());
            assertEquals(4, assassin.getMoveRange());
            assertEquals(1, assassin.getAttackRange());
        }

        @Test
        @DisplayName("SP3.4: Minion has correct owner")
        void minionHasCorrectOwner() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            List<Unit> p1Minions = findMinions(state, PlayerId.PLAYER_1);
            List<Unit> p2Minions = findMinions(state, PlayerId.PLAYER_2);

            assertEquals(2, p1Minions.size());
            assertEquals(2, p2Minions.size());
            p1Minions.forEach(m -> assertEquals(PlayerId.PLAYER_1, m.getOwner()));
            p2Minions.forEach(m -> assertEquals(PlayerId.PLAYER_2, m.getOwner()));
        }

        @Test
        @DisplayName("SP3.5: Minion category is MINION")
        void minionCategoryIsMinion() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            List<Unit> minions = findMinions(state, PlayerId.PLAYER_1);

            minions.forEach(m -> assertEquals(UnitCategory.MINION, m.getCategory()));
        }

        @Test
        @DisplayName("SP3.6: Minion has correct type")
        void minionHasCorrectType() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            List<Unit> p1Minions = findMinions(state, PlayerId.PLAYER_1);

            // P1 selected TANK, ARCHER in order
            // Minion 1 (left, pos 0,0) should be TANK
            // Minion 2 (right, pos 4,0) should be ARCHER
            Unit minion1 = p1Minions.stream()
                .filter(m -> m.getPosition().getX() == 0)
                .findFirst().orElse(null);
            Unit minion2 = p1Minions.stream()
                .filter(m -> m.getPosition().getX() == 4)
                .findFirst().orElse(null);

            assertNotNull(minion1);
            assertNotNull(minion2);
            assertEquals(MinionType.TANK, minion1.getMinionType());
            assertEquals(MinionType.ARCHER, minion2.getMinionType());
        }
    }

    // =========================================================================
    // SP4: Duplicate Minion Selection
    // =========================================================================

    @Nested
    @DisplayName("SP4: Duplicate Minion Selection")
    class DuplicateMinionSelection {

        @Test
        @DisplayName("SP4.1: P1 selects TANK, TANK creates 2 TANK minions")
        void p1SelectsTankTank() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.TANK,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ASSASSIN, MinionType.TANK,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);
            List<Unit> p1Minions = findMinions(state, PlayerId.PLAYER_1);

            assertEquals(2, p1Minions.size());
            p1Minions.forEach(m -> assertEquals(MinionType.TANK, m.getMinionType()));
        }

        @Test
        @DisplayName("SP4.2: P2 selects ARCHER, ARCHER creates 2 ARCHER minions")
        void p2SelectsArcherArcher() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.ASSASSIN,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ARCHER, MinionType.ARCHER,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);
            List<Unit> p2Minions = findMinions(state, PlayerId.PLAYER_2);

            assertEquals(2, p2Minions.size());
            p2Minions.forEach(m -> assertEquals(MinionType.ARCHER, m.getMinionType()));
        }

        @Test
        @DisplayName("SP4.3: Duplicate minions have unique IDs")
        void duplicateMinionsHaveUniqueIds() {
            DraftState p1Draft = createCompleteDraft(
                PlayerId.PLAYER_1, HeroClass.WARRIOR,
                MinionType.TANK, MinionType.TANK,
                SkillRegistry.WARRIOR_ENDURE);
            DraftState p2Draft = createCompleteDraft(
                PlayerId.PLAYER_2, HeroClass.MAGE,
                MinionType.ARCHER, MinionType.ARCHER,
                SkillRegistry.MAGE_ELEMENTAL_BLAST);
            DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

            GameState state = setupService.createGameState(draftResult);
            List<Unit> p1Minions = findMinions(state, PlayerId.PLAYER_1);

            assertEquals(2, p1Minions.size());
            assertNotEquals(p1Minions.get(0).getId(), p1Minions.get(1).getId());
        }
    }

    // =========================================================================
    // PO1: Player 1 Positions (Bottom Row)
    // =========================================================================

    @Nested
    @DisplayName("PO1: Player 1 Positions (Bottom Row)")
    class Player1Positions {

        @Test
        @DisplayName("PO1.1: P1 Hero at center (2, 0)")
        void p1HeroAtCenter() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit hero = findHero(state, PlayerId.PLAYER_1);

            assertEquals(new Position(2, 0), hero.getPosition());
        }

        @Test
        @DisplayName("PO1.2: P1 Minion 1 at left (0, 0)")
        void p1Minion1AtLeft() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit minion1 = findUnit(state, "p1_minion_1");

            assertNotNull(minion1);
            assertEquals(new Position(0, 0), minion1.getPosition());
        }

        @Test
        @DisplayName("PO1.3: P1 Minion 2 at right (4, 0)")
        void p1Minion2AtRight() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit minion2 = findUnit(state, "p1_minion_2");

            assertNotNull(minion2);
            assertEquals(new Position(4, 0), minion2.getPosition());
        }
    }

    // =========================================================================
    // PO2: Player 2 Positions (Top Row)
    // =========================================================================

    @Nested
    @DisplayName("PO2: Player 2 Positions (Top Row)")
    class Player2Positions {

        @Test
        @DisplayName("PO2.1: P2 Hero at center (2, 4)")
        void p2HeroAtCenter() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit hero = findHero(state, PlayerId.PLAYER_2);

            assertEquals(new Position(2, 4), hero.getPosition());
        }

        @Test
        @DisplayName("PO2.2: P2 Minion 1 at left (0, 4)")
        void p2Minion1AtLeft() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit minion1 = findUnit(state, "p2_minion_1");

            assertNotNull(minion1);
            assertEquals(new Position(0, 4), minion1.getPosition());
        }

        @Test
        @DisplayName("PO2.3: P2 Minion 2 at right (4, 4)")
        void p2Minion2AtRight() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit minion2 = findUnit(state, "p2_minion_2");

            assertNotNull(minion2);
            assertEquals(new Position(4, 4), minion2.getPosition());
        }
    }

    // =========================================================================
    // PO3: Position Validation
    // =========================================================================

    @Nested
    @DisplayName("PO3: Position Validation")
    class PositionValidation {

        @Test
        @DisplayName("PO3.1: All 6 units at unique positions")
        void allUnitsAtUniquePositions() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Set<Position> positions = state.getUnits().stream()
                .map(Unit::getPosition)
                .collect(Collectors.toSet());

            assertEquals(6, positions.size());
        }

        @Test
        @DisplayName("PO3.2: All positions within 5x5 board")
        void allPositionsWithinBoard() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);

            state.getUnits().forEach(unit -> {
                Position pos = unit.getPosition();
                assertTrue(pos.getX() >= 0 && pos.getX() < 5,
                    "X position out of bounds: " + pos);
                assertTrue(pos.getY() >= 0 && pos.getY() < 5,
                    "Y position out of bounds: " + pos);
            });
        }

        @Test
        @DisplayName("PO3.3: Minion order matches selection order")
        void minionOrderMatchesSelectionOrder() {
            // P1 selects TANK, ARCHER -> TANK at (0,0), ARCHER at (4,0)
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit minion1 = findUnit(state, "p1_minion_1");
            Unit minion2 = findUnit(state, "p1_minion_2");

            // First selected = position (0,y), Second selected = position (4,y)
            assertEquals(MinionType.TANK, minion1.getMinionType());
            assertEquals(MinionType.ARCHER, minion2.getMinionType());
        }
    }

    // =========================================================================
    // ID1: Unit ID Generation
    // =========================================================================

    @Nested
    @DisplayName("ID1: Unit ID Generation")
    class UnitIdGeneration {

        @Test
        @DisplayName("ID1.1: All 6 units have unique IDs")
        void allUnitsHaveUniqueIds() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Set<String> ids = state.getUnits().stream()
                .map(Unit::getId)
                .collect(Collectors.toSet());

            assertEquals(6, ids.size());
        }

        @Test
        @DisplayName("ID1.2: Hero IDs follow pattern")
        void heroIdsFollowPattern() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);
            Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
            Unit p2Hero = findHero(state, PlayerId.PLAYER_2);

            assertEquals("p1_hero", p1Hero.getId());
            assertEquals("p2_hero", p2Hero.getId());
        }

        @Test
        @DisplayName("ID1.3: Minion IDs follow pattern")
        void minionIdsFollowPattern() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);

            assertNotNull(findUnit(state, "p1_minion_1"));
            assertNotNull(findUnit(state, "p1_minion_2"));
            assertNotNull(findUnit(state, "p2_minion_1"));
            assertNotNull(findUnit(state, "p2_minion_2"));
        }
    }

    // =========================================================================
    // Additional: All Hero Classes
    // =========================================================================

    @Nested
    @DisplayName("All Hero Classes")
    class AllHeroClasses {

        @Test
        @DisplayName("Create GameState with all 6 hero class combinations")
        void allHeroClassCombinations() {
            for (HeroClass p1Class : HeroClass.values()) {
                for (HeroClass p2Class : HeroClass.values()) {
                    String p1Skill = getFirstSkillForClass(p1Class);
                    String p2Skill = getFirstSkillForClass(p2Class);

                    DraftState p1Draft = createCompleteDraft(
                        PlayerId.PLAYER_1, p1Class,
                        MinionType.TANK, MinionType.ARCHER, p1Skill);
                    DraftState p2Draft = createCompleteDraft(
                        PlayerId.PLAYER_2, p2Class,
                        MinionType.ASSASSIN, MinionType.TANK, p2Skill);
                    DraftResult draftResult = new DraftResult(p1Draft, p2Draft);

                    GameState state = setupService.createGameState(draftResult);

                    assertNotNull(state, "Failed for " + p1Class + " vs " + p2Class);
                    assertEquals(6, state.getUnits().size());

                    Unit p1Hero = findHero(state, PlayerId.PLAYER_1);
                    Unit p2Hero = findHero(state, PlayerId.PLAYER_2);

                    assertEquals(p1Class, p1Hero.getHeroClass());
                    assertEquals(p2Class, p2Hero.getHeroClass());
                }
            }
        }

        private String getFirstSkillForClass(HeroClass heroClass) {
            switch (heroClass) {
                case WARRIOR: return SkillRegistry.WARRIOR_ENDURE;
                case MAGE: return SkillRegistry.MAGE_ELEMENTAL_BLAST;
                case ROGUE: return SkillRegistry.ROGUE_SMOKE_BOMB;
                case HUNTRESS: return SkillRegistry.HUNTRESS_SPIRIT_HAWK;
                case DUELIST: return SkillRegistry.DUELIST_FEINT;
                case CLERIC: return SkillRegistry.CLERIC_TRINITY;
                default: throw new IllegalArgumentException("Unknown hero class: " + heroClass);
            }
        }
    }

    // =========================================================================
    // Error Handling
    // =========================================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("EH1.1: Create GameState with null DraftResult throws")
        void nullDraftResultThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> setupService.createGameState(null));
        }

        @Test
        @DisplayName("EH2.1: All units are alive")
        void allUnitsAreAlive() {
            DraftResult draftResult = createCompleteDraftResult();

            GameState state = setupService.createGameState(draftResult);

            state.getUnits().forEach(unit -> assertTrue(unit.isAlive()));
        }
    }
}
