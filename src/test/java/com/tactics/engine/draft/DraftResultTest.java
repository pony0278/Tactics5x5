package com.tactics.engine.draft;

import com.tactics.engine.model.HeroClass;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.skill.SkillRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DraftResult Tests (DM3-Series and DV3-Series from DRAFT_PHASE_TESTPLAN.md)
 */
@DisplayName("Draft Result Tests")
class DraftResultTest {

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private DraftState createCompleteDraft(PlayerId playerId, HeroClass heroClass) {
        String skillId = getFirstSkillForClass(heroClass);
        return new DraftState(playerId, heroClass)
            .withMinion(MinionType.TANK)
            .withMinion(MinionType.ARCHER)
            .withSkill(skillId);
    }

    private DraftState createIncompleteDraft(PlayerId playerId, HeroClass heroClass) {
        return new DraftState(playerId, heroClass)
            .withMinion(MinionType.TANK);  // Only 1 minion, no skill
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

    // =========================================================================
    // DM3: DraftResult
    // =========================================================================

    @Nested
    @DisplayName("DM3: DraftResult Creation")
    class DraftResultCreation {

        @Test
        @DisplayName("DM3.1: Create DraftResult with two states")
        void createDraftResultWithTwoStates() {
            DraftState p1Draft = createCompleteDraft(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState p2Draft = createCompleteDraft(PlayerId.PLAYER_2, HeroClass.MAGE);

            DraftResult result = new DraftResult(p1Draft, p2Draft);

            assertNotNull(result);
            assertEquals(p1Draft, result.getPlayer1Draft());
            assertEquals(p2Draft, result.getPlayer2Draft());
        }

        @Test
        @DisplayName("DM3.2: isComplete when both complete")
        void isCompleteWhenBothComplete() {
            DraftState p1Draft = createCompleteDraft(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState p2Draft = createCompleteDraft(PlayerId.PLAYER_2, HeroClass.MAGE);

            DraftResult result = new DraftResult(p1Draft, p2Draft);

            assertTrue(result.isComplete());
        }

        @Test
        @DisplayName("DM3.3: isComplete when P1 incomplete")
        void isCompleteWhenP1Incomplete() {
            DraftState p1Draft = createIncompleteDraft(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState p2Draft = createCompleteDraft(PlayerId.PLAYER_2, HeroClass.MAGE);

            DraftResult result = new DraftResult(p1Draft, p2Draft);

            assertFalse(result.isComplete());
        }

        @Test
        @DisplayName("DM3.4: isComplete when P2 incomplete")
        void isCompleteWhenP2Incomplete() {
            DraftState p1Draft = createCompleteDraft(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState p2Draft = createIncompleteDraft(PlayerId.PLAYER_2, HeroClass.MAGE);

            DraftResult result = new DraftResult(p1Draft, p2Draft);

            assertFalse(result.isComplete());
        }
    }

    // =========================================================================
    // DV3: Both Players Complete
    // =========================================================================

    @Nested
    @DisplayName("DV3: Both Players Complete")
    class BothPlayersComplete {

        @Test
        @DisplayName("DV3.1: P1 complete, P2 incomplete")
        void p1CompleteP2Incomplete() {
            DraftState p1Draft = createCompleteDraft(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState p2Draft = new DraftState(PlayerId.PLAYER_2, HeroClass.MAGE)
                .withMinion(MinionType.TANK)
                .withMinion(MinionType.ARCHER);  // No skill

            DraftResult result = new DraftResult(p1Draft, p2Draft);

            assertFalse(result.isComplete());
        }

        @Test
        @DisplayName("DV3.2: P1 incomplete, P2 complete")
        void p1IncompleteP2Complete() {
            DraftState p1Draft = new DraftState(PlayerId.PLAYER_1, HeroClass.WARRIOR)
                .withSkill(SkillRegistry.WARRIOR_ENDURE);  // No minions
            DraftState p2Draft = createCompleteDraft(PlayerId.PLAYER_2, HeroClass.MAGE);

            DraftResult result = new DraftResult(p1Draft, p2Draft);

            assertFalse(result.isComplete());
        }

        @Test
        @DisplayName("DV3.3: Both complete")
        void bothComplete() {
            DraftState p1Draft = createCompleteDraft(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState p2Draft = createCompleteDraft(PlayerId.PLAYER_2, HeroClass.MAGE);

            DraftResult result = new DraftResult(p1Draft, p2Draft);

            assertTrue(result.isComplete());
        }
    }

    // =========================================================================
    // Additional Tests
    // =========================================================================

    @Nested
    @DisplayName("DraftResult Validation")
    class DraftResultValidation {

        @Test
        @DisplayName("Create DraftResult with null P1 throws exception")
        void nullP1Throws() {
            DraftState p2Draft = createCompleteDraft(PlayerId.PLAYER_2, HeroClass.MAGE);

            assertThrows(IllegalArgumentException.class, () -> new DraftResult(null, p2Draft));
        }

        @Test
        @DisplayName("Create DraftResult with null P2 throws exception")
        void nullP2Throws() {
            DraftState p1Draft = createCompleteDraft(PlayerId.PLAYER_1, HeroClass.WARRIOR);

            assertThrows(IllegalArgumentException.class, () -> new DraftResult(p1Draft, null));
        }

        @Test
        @DisplayName("DraftResult with wrong player IDs throws exception")
        void wrongPlayerIdsThrows() {
            // Both drafts for PLAYER_1 - should fail
            DraftState draft1 = createCompleteDraft(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState draft2 = createCompleteDraft(PlayerId.PLAYER_1, HeroClass.MAGE);

            assertThrows(IllegalArgumentException.class, () -> new DraftResult(draft1, draft2));
        }
    }

    // =========================================================================
    // Get Draft by Player
    // =========================================================================

    @Nested
    @DisplayName("Get Draft by Player")
    class GetDraftByPlayer {

        @Test
        @DisplayName("getDraft for PLAYER_1 returns P1 draft")
        void getDraftForPlayer1() {
            DraftState p1Draft = createCompleteDraft(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState p2Draft = createCompleteDraft(PlayerId.PLAYER_2, HeroClass.MAGE);
            DraftResult result = new DraftResult(p1Draft, p2Draft);

            assertEquals(p1Draft, result.getDraft(PlayerId.PLAYER_1));
        }

        @Test
        @DisplayName("getDraft for PLAYER_2 returns P2 draft")
        void getDraftForPlayer2() {
            DraftState p1Draft = createCompleteDraft(PlayerId.PLAYER_1, HeroClass.WARRIOR);
            DraftState p2Draft = createCompleteDraft(PlayerId.PLAYER_2, HeroClass.MAGE);
            DraftResult result = new DraftResult(p1Draft, p2Draft);

            assertEquals(p2Draft, result.getDraft(PlayerId.PLAYER_2));
        }
    }
}
