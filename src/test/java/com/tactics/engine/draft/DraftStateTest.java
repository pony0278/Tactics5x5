package com.tactics.engine.draft;

import com.tactics.engine.model.HeroClass;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.skill.SkillRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Draft Model Tests (DM-Series and MS/SS-Series from DRAFT_PHASE_TESTPLAN.md)
 */
@DisplayName("Draft State Tests")
class DraftStateTest {

    // =========================================================================
    // DM1: DraftState Creation
    // =========================================================================

    @Nested
    @DisplayName("DM1: DraftState Creation")
    class DraftStateCreation {

        @Test
        @DisplayName("DM1.1: Create empty DraftState for P1")
        void createEmptyDraftStateForP1() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);

            assertEquals(PlayerId.PLAYER_1, state.getPlayerId());
            assertEquals(HeroClass.WARRIOR, state.getHeroClass());
            assertTrue(state.getSelectedMinions().isEmpty());
            assertNull(state.getSelectedSkillId());
            assertFalse(state.isMinionSelectionComplete());
            assertFalse(state.isSkillSelectionComplete());
        }

        @Test
        @DisplayName("DM1.2: Create empty DraftState for P2")
        void createEmptyDraftStateForP2() {
            DraftState state = new DraftState(PlayerId.PLAYER_2, HeroClass.MAGE);

            assertEquals(PlayerId.PLAYER_2, state.getPlayerId());
            assertEquals(HeroClass.MAGE, state.getHeroClass());
            assertTrue(state.getSelectedMinions().isEmpty());
            assertNull(state.getSelectedSkillId());
        }

        @Test
        @DisplayName("DM1.3: DraftState with WARRIOR hero has 3 skills available")
        void warriorHas3SkillsAvailable() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);

            List<String> availableSkills = state.getAvailableSkillIds();
            assertEquals(3, availableSkills.size());
            assertTrue(availableSkills.contains(SkillRegistry.WARRIOR_HEROIC_LEAP));
            assertTrue(availableSkills.contains(SkillRegistry.WARRIOR_SHOCKWAVE));
            assertTrue(availableSkills.contains(SkillRegistry.WARRIOR_ENDURE));
        }

        @Test
        @DisplayName("DM1.4: DraftState with MAGE hero has 3 skills available")
        void mageHas3SkillsAvailable() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.MAGE);

            List<String> availableSkills = state.getAvailableSkillIds();
            assertEquals(3, availableSkills.size());
            assertTrue(availableSkills.contains(SkillRegistry.MAGE_ELEMENTAL_BLAST));
            assertTrue(availableSkills.contains(SkillRegistry.MAGE_WARP_BEACON));
            assertTrue(availableSkills.contains(SkillRegistry.MAGE_WILD_MAGIC));
        }
    }

    // =========================================================================
    // DM2: DraftState Immutability
    // =========================================================================

    @Nested
    @DisplayName("DM2: DraftState Immutability")
    class DraftStateImmutability {

        @Test
        @DisplayName("DM2.1: Adding minion returns new state")
        void addingMinionReturnsNewState() {
            DraftState original = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState updated = original.withMinion(MinionType.TANK);

            assertNotSame(original, updated);
            assertTrue(original.getSelectedMinions().isEmpty());
            assertEquals(1, updated.getSelectedMinions().size());
            assertEquals(MinionType.TANK, updated.getSelectedMinions().get(0));
        }

        @Test
        @DisplayName("DM2.2: Setting skill returns new state")
        void settingSkillReturnsNewState() {
            DraftState original = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState updated = original.withSkill(SkillRegistry.WARRIOR_ENDURE);

            assertNotSame(original, updated);
            assertNull(original.getSelectedSkillId());
            assertEquals(SkillRegistry.WARRIOR_ENDURE, updated.getSelectedSkillId());
        }

        @Test
        @DisplayName("DM2.3: selectedMinions list is unmodifiable")
        void selectedMinionsListUnmodifiable() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK);

            List<MinionType> minions = state.getSelectedMinions();
            assertThrows(UnsupportedOperationException.class, () -> minions.add(MinionType.ARCHER));
        }
    }

    // =========================================================================
    // MS1: Valid Minion Selection
    // =========================================================================

    @Nested
    @DisplayName("MS1: Valid Minion Selection")
    class ValidMinionSelection {

        @Test
        @DisplayName("MS1.1: Select TANK as first minion")
        void selectTankAsFirstMinion() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK);

            assertEquals(Arrays.asList(MinionType.TANK), state.getSelectedMinions());
        }

        @Test
        @DisplayName("MS1.2: Select ARCHER as first minion")
        void selectArcherAsFirstMinion() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.ARCHER);

            assertEquals(Arrays.asList(MinionType.ARCHER), state.getSelectedMinions());
        }

        @Test
        @DisplayName("MS1.3: Select ASSASSIN as first minion")
        void selectAssassinAsFirstMinion() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.ASSASSIN);

            assertEquals(Arrays.asList(MinionType.ASSASSIN), state.getSelectedMinions());
        }

        @Test
        @DisplayName("MS1.4: Select TANK then ARCHER")
        void selectTankThenArcher() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK)
                .withMinion(MinionType.ARCHER);

            assertEquals(Arrays.asList(MinionType.TANK, MinionType.ARCHER), state.getSelectedMinions());
        }

        @Test
        @DisplayName("MS1.5: Select ASSASSIN then TANK")
        void selectAssassinThenTank() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.ASSASSIN)
                .withMinion(MinionType.TANK);

            assertEquals(Arrays.asList(MinionType.ASSASSIN, MinionType.TANK), state.getSelectedMinions());
        }

        @Test
        @DisplayName("MS1.6: Select same type twice (TANK, TANK)")
        void selectSameTypeTwice() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK)
                .withMinion(MinionType.TANK);

            assertEquals(Arrays.asList(MinionType.TANK, MinionType.TANK), state.getSelectedMinions());
        }
    }

    // =========================================================================
    // MS2: Minion Selection Completion
    // =========================================================================

    @Nested
    @DisplayName("MS2: Minion Selection Completion")
    class MinionSelectionCompletion {

        @Test
        @DisplayName("MS2.1: 0 minions selected - not complete")
        void zeroMinionsNotComplete() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);

            assertFalse(state.isMinionSelectionComplete());
        }

        @Test
        @DisplayName("MS2.2: 1 minion selected - not complete")
        void oneMinonNotComplete() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK);

            assertFalse(state.isMinionSelectionComplete());
        }

        @Test
        @DisplayName("MS2.3: 2 minions selected - complete")
        void twoMinionsComplete() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK)
                .withMinion(MinionType.ARCHER);

            assertTrue(state.isMinionSelectionComplete());
        }
    }

    // =========================================================================
    // MS3: Minion Selection Validation
    // =========================================================================

    @Nested
    @DisplayName("MS3: Minion Selection Validation")
    class MinionSelectionValidation {

        @Test
        @DisplayName("MS3.1: Select null minion type throws exception")
        void selectNullMinionThrows() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);

            assertThrows(IllegalArgumentException.class, () -> state.withMinion(null));
        }

        @Test
        @DisplayName("MS3.2: Select 3rd minion after 2 selected throws exception")
        void selectThirdMinionThrows() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK)
                .withMinion(MinionType.ARCHER);

            assertThrows(IllegalStateException.class, () -> state.withMinion(MinionType.ASSASSIN));
        }
    }

    // =========================================================================
    // MS4: Available Minion Types
    // =========================================================================

    @Nested
    @DisplayName("MS4: Available Minion Types")
    class AvailableMinionTypes {

        @Test
        @DisplayName("MS4.1: Get available minion types")
        void getAvailableMinionTypes() {
            List<MinionType> available = DraftState.getAvailableMinionTypes();

            assertEquals(3, available.size());
            assertTrue(available.contains(MinionType.TANK));
            assertTrue(available.contains(MinionType.ARCHER));
            assertTrue(available.contains(MinionType.ASSASSIN));
        }

        @Test
        @DisplayName("MS4.2: All 3 types always available (selection doesn't remove)")
        void allTypesAlwaysAvailable() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK);

            // Even after selecting TANK, all types still available for second pick
            List<MinionType> available = DraftState.getAvailableMinionTypes();
            assertEquals(3, available.size());
        }
    }

    // =========================================================================
    // SS1: Available Skills Per Class
    // =========================================================================

    @Nested
    @DisplayName("SS1: Available Skills Per Class")
    class AvailableSkillsPerClass {

        @Test
        @DisplayName("SS1.1: WARRIOR skills")
        void warriorSkills() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            List<String> skills = state.getAvailableSkillIds();

            assertEquals(3, skills.size());
            assertTrue(skills.contains(SkillRegistry.WARRIOR_HEROIC_LEAP));
            assertTrue(skills.contains(SkillRegistry.WARRIOR_SHOCKWAVE));
            assertTrue(skills.contains(SkillRegistry.WARRIOR_ENDURE));
        }

        @Test
        @DisplayName("SS1.2: MAGE skills")
        void mageSkills() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.MAGE);
            List<String> skills = state.getAvailableSkillIds();

            assertEquals(3, skills.size());
            assertTrue(skills.contains(SkillRegistry.MAGE_ELEMENTAL_BLAST));
            assertTrue(skills.contains(SkillRegistry.MAGE_WARP_BEACON));
            assertTrue(skills.contains(SkillRegistry.MAGE_WILD_MAGIC));
        }

        @Test
        @DisplayName("SS1.3: ROGUE skills")
        void rogueSkills() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.ROGUE);
            List<String> skills = state.getAvailableSkillIds();

            assertEquals(3, skills.size());
            assertTrue(skills.contains(SkillRegistry.ROGUE_SMOKE_BOMB));
            assertTrue(skills.contains(SkillRegistry.ROGUE_DEATH_MARK));
            assertTrue(skills.contains(SkillRegistry.ROGUE_SHADOW_CLONE));
        }

        @Test
        @DisplayName("SS1.4: HUNTRESS skills")
        void huntressSkills() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.HUNTRESS);
            List<String> skills = state.getAvailableSkillIds();

            assertEquals(3, skills.size());
            assertTrue(skills.contains(SkillRegistry.HUNTRESS_SPIRIT_HAWK));
            assertTrue(skills.contains(SkillRegistry.HUNTRESS_SPECTRAL_BLADES));
            assertTrue(skills.contains(SkillRegistry.HUNTRESS_NATURES_POWER));
        }

        @Test
        @DisplayName("SS1.5: DUELIST skills")
        void duelistSkills() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.DUELIST);
            List<String> skills = state.getAvailableSkillIds();

            assertEquals(3, skills.size());
            assertTrue(skills.contains(SkillRegistry.DUELIST_CHALLENGE));
            assertTrue(skills.contains(SkillRegistry.DUELIST_ELEMENTAL_STRIKE));
            assertTrue(skills.contains(SkillRegistry.DUELIST_FEINT));
        }

        @Test
        @DisplayName("SS1.6: CLERIC skills")
        void clericSkills() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.CLERIC);
            List<String> skills = state.getAvailableSkillIds();

            assertEquals(3, skills.size());
            assertTrue(skills.contains(SkillRegistry.CLERIC_TRINITY));
            assertTrue(skills.contains(SkillRegistry.CLERIC_POWER_OF_MANY));
            assertTrue(skills.contains(SkillRegistry.CLERIC_ASCENDED_FORM));
        }
    }

    // =========================================================================
    // SS2: Valid Skill Selection
    // =========================================================================

    @Nested
    @DisplayName("SS2: Valid Skill Selection")
    class ValidSkillSelection {

        @Test
        @DisplayName("SS2.1: WARRIOR selects heroic_leap")
        void warriorSelectsHeroicLeap() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withSkill(SkillRegistry.WARRIOR_HEROIC_LEAP);

            assertEquals(SkillRegistry.WARRIOR_HEROIC_LEAP, state.getSelectedSkillId());
        }

        @Test
        @DisplayName("SS2.2: WARRIOR selects endure")
        void warriorSelectsEndure() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withSkill(SkillRegistry.WARRIOR_ENDURE);

            assertEquals(SkillRegistry.WARRIOR_ENDURE, state.getSelectedSkillId());
        }

        @Test
        @DisplayName("SS2.3: MAGE selects wild_magic")
        void mageSelectsWildMagic() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.MAGE)
                .withSkill(SkillRegistry.MAGE_WILD_MAGIC);

            assertEquals(SkillRegistry.MAGE_WILD_MAGIC, state.getSelectedSkillId());
        }

        @Test
        @DisplayName("SS2.4: CLERIC selects trinity")
        void clericSelectsTrinity() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.CLERIC)
                .withSkill(SkillRegistry.CLERIC_TRINITY);

            assertEquals(SkillRegistry.CLERIC_TRINITY, state.getSelectedSkillId());
        }
    }

    // =========================================================================
    // SS3: Skill Selection Validation
    // =========================================================================

    @Nested
    @DisplayName("SS3: Skill Selection Validation")
    class SkillSelectionValidation {

        @Test
        @DisplayName("SS3.1: Select null skill throws exception")
        void selectNullSkillThrows() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);

            assertThrows(IllegalArgumentException.class, () -> state.withSkill(null));
        }

        @Test
        @DisplayName("SS3.2: Select invalid skill ID throws exception")
        void selectInvalidSkillThrows() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);

            assertThrows(IllegalArgumentException.class, () -> state.withSkill("invalid_skill_id"));
        }

        @Test
        @DisplayName("SS3.3: WARRIOR selects mage skill throws exception")
        void warriorSelectsMageSkillThrows() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);

            assertThrows(IllegalArgumentException.class,
                () -> state.withSkill(SkillRegistry.MAGE_WILD_MAGIC));
        }

        @Test
        @DisplayName("SS3.4: MAGE selects warrior skill throws exception")
        void mageSelectsWarriorSkillThrows() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.MAGE);

            assertThrows(IllegalArgumentException.class,
                () -> state.withSkill(SkillRegistry.WARRIOR_ENDURE));
        }

        @Test
        @DisplayName("SS3.5: Select skill when already selected throws exception")
        void selectSkillWhenAlreadySelectedThrows() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withSkill(SkillRegistry.WARRIOR_ENDURE);

            assertThrows(IllegalStateException.class,
                () -> state.withSkill(SkillRegistry.WARRIOR_HEROIC_LEAP));
        }
    }

    // =========================================================================
    // SS4: Skill Selection Completion
    // =========================================================================

    @Nested
    @DisplayName("SS4: Skill Selection Completion")
    class SkillSelectionCompletion {

        @Test
        @DisplayName("SS4.1: No skill selected - not complete")
        void noSkillNotComplete() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);

            assertFalse(state.isSkillSelectionComplete());
        }

        @Test
        @DisplayName("SS4.2: Skill selected - complete")
        void skillSelectedComplete() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withSkill(SkillRegistry.WARRIOR_ENDURE);

            assertTrue(state.isSkillSelectionComplete());
        }
    }

    // =========================================================================
    // DV1: Overall Draft Completion
    // =========================================================================

    @Nested
    @DisplayName("DV1: Overall Draft Completion")
    class OverallDraftCompletion {

        @Test
        @DisplayName("DV1.1: No selections made - not complete")
        void noSelectionsNotComplete() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR);

            assertFalse(state.isComplete());
        }

        @Test
        @DisplayName("DV1.2: Only minions selected - not complete")
        void onlyMinionsNotComplete() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK)
                .withMinion(MinionType.ARCHER);

            assertFalse(state.isComplete());
        }

        @Test
        @DisplayName("DV1.3: Only skill selected - not complete")
        void onlySkillNotComplete() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withSkill(SkillRegistry.WARRIOR_ENDURE);

            assertFalse(state.isComplete());
        }

        @Test
        @DisplayName("DV1.4: Both minions and skill selected - complete")
        void bothSelectionsComplete() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK)
                .withMinion(MinionType.ARCHER)
                .withSkill(SkillRegistry.WARRIOR_ENDURE);

            assertTrue(state.isComplete());
        }
    }

    // =========================================================================
    // DV2: Draft State Transitions
    // =========================================================================

    @Nested
    @DisplayName("DV2: Draft State Transitions")
    class DraftStateTransitions {

        @Test
        @DisplayName("DV2.1: Select minion before skill")
        void selectMinionBeforeSkill() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK)
                .withMinion(MinionType.ARCHER)
                .withSkill(SkillRegistry.WARRIOR_ENDURE);

            assertTrue(state.isComplete());
        }

        @Test
        @DisplayName("DV2.2: Select skill before minions")
        void selectSkillBeforeMinions() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withSkill(SkillRegistry.WARRIOR_ENDURE)
                .withMinion(MinionType.TANK)
                .withMinion(MinionType.ARCHER);

            assertTrue(state.isComplete());
        }

        @Test
        @DisplayName("DV2.3: Interleave minion and skill selection")
        void interleaveMinionAndSkill() {
            DraftState state = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withMinion(MinionType.TANK)
                .withSkill(SkillRegistry.WARRIOR_ENDURE)
                .withMinion(MinionType.ARCHER);

            assertTrue(state.isComplete());
        }
    }
}
