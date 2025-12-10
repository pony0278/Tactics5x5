package com.tactics.server.core;

import com.tactics.engine.draft.DraftResult;
import com.tactics.engine.draft.DraftSetupService;
import com.tactics.engine.draft.DraftState;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.HeroClass;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.skill.SkillRegistry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks draft state for each match.
 * Handles player team selections and creates GameState when both ready.
 */
public class MatchDraftTracker {

    /**
     * Game phase enum for tracking match state.
     */
    public enum GamePhase {
        DRAFT,
        BATTLE,
        GAME_OVER
    }

    /**
     * Holds draft selections for a single player.
     */
    public static class PlayerDraftSelection {
        public final String heroClass;
        public final List<String> minions;

        public PlayerDraftSelection(String heroClass, List<String> minions) {
            this.heroClass = heroClass;
            this.minions = minions;
        }
    }

    /**
     * Tracks draft state for a single match.
     */
    private static class MatchDraft {
        GamePhase phase = GamePhase.DRAFT;
        PlayerDraftSelection p1Selection;
        PlayerDraftSelection p2Selection;
    }

    private final Map<String, MatchDraft> matchDrafts = new ConcurrentHashMap<>();
    private final DraftSetupService draftSetupService = new DraftSetupService();

    /**
     * Get or create draft state for a match.
     */
    private MatchDraft getOrCreateDraft(String matchId) {
        return matchDrafts.computeIfAbsent(matchId, k -> new MatchDraft());
    }

    /**
     * Get the current phase for a match.
     */
    public GamePhase getPhase(String matchId) {
        MatchDraft draft = matchDrafts.get(matchId);
        return draft != null ? draft.phase : GamePhase.DRAFT;
    }

    /**
     * Set the phase for a match.
     */
    public void setPhase(String matchId, GamePhase phase) {
        getOrCreateDraft(matchId).phase = phase;
    }

    /**
     * Record a player's draft selection.
     *
     * @param matchId the match identifier
     * @param playerId the player ("P1" or "P2")
     * @param heroClass the hero class name
     * @param minions list of minion type names
     * @return true if both players are now ready
     */
    public boolean recordSelection(String matchId, String playerId, String heroClass, List<String> minions) {
        MatchDraft draft = getOrCreateDraft(matchId);

        PlayerDraftSelection selection = new PlayerDraftSelection(heroClass, minions);

        if ("P1".equals(playerId)) {
            draft.p1Selection = selection;
        } else if ("P2".equals(playerId)) {
            draft.p2Selection = selection;
        }

        return draft.p1Selection != null && draft.p2Selection != null;
    }

    /**
     * Check if a specific player has submitted their draft.
     */
    public boolean hasPlayerSubmitted(String matchId, String playerId) {
        MatchDraft draft = matchDrafts.get(matchId);
        if (draft == null) return false;

        if ("P1".equals(playerId)) {
            return draft.p1Selection != null;
        } else if ("P2".equals(playerId)) {
            return draft.p2Selection != null;
        }
        return false;
    }

    /**
     * Check if both players have submitted.
     */
    public boolean areBothPlayersReady(String matchId) {
        MatchDraft draft = matchDrafts.get(matchId);
        return draft != null && draft.p1Selection != null && draft.p2Selection != null;
    }

    /**
     * Create the initial GameState from completed drafts.
     *
     * @param matchId the match identifier
     * @return new GameState, or null if drafts not complete
     */
    public GameState createGameStateFromDraft(String matchId) {
        MatchDraft draft = matchDrafts.get(matchId);
        if (draft == null || draft.p1Selection == null || draft.p2Selection == null) {
            return null;
        }

        try {
            // Convert string selections to DraftState objects
            DraftState p1Draft = createDraftState(PlayerId.PLAYER_1, draft.p1Selection);
            DraftState p2Draft = createDraftState(PlayerId.PLAYER_2, draft.p2Selection);

            // Create DraftResult and generate GameState
            DraftResult result = new DraftResult(p1Draft, p2Draft);
            GameState gameState = draftSetupService.createGameState(result);

            // Update phase
            draft.phase = GamePhase.BATTLE;

            return gameState;
        } catch (Exception e) {
            System.err.println("Error creating GameState from draft: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert player selection to DraftState with auto-selected first skill.
     */
    private DraftState createDraftState(PlayerId playerId, PlayerDraftSelection selection) {
        // Parse hero class
        HeroClass heroClass = HeroClass.valueOf(selection.heroClass.toUpperCase());

        // Create base draft with hero class
        DraftState draft = new DraftState(playerId, heroClass);

        // Add minions
        for (String minionStr : selection.minions) {
            MinionType minionType = MinionType.valueOf(minionStr.toUpperCase());
            draft = draft.withMinion(minionType);
        }

        // Auto-select first available skill for the hero class
        List<String> availableSkills = draft.getAvailableSkillIds();
        if (!availableSkills.isEmpty()) {
            draft = draft.withSkill(availableSkills.get(0));
        }

        return draft;
    }

    /**
     * Get a player's selection for state broadcasting.
     */
    public PlayerDraftSelection getPlayerSelection(String matchId, String playerId) {
        MatchDraft draft = matchDrafts.get(matchId);
        if (draft == null) return null;

        if ("P1".equals(playerId)) {
            return draft.p1Selection;
        } else if ("P2".equals(playerId)) {
            return draft.p2Selection;
        }
        return null;
    }

    /**
     * Clear draft state for a match (e.g., on disconnect).
     */
    public void clearMatch(String matchId) {
        matchDrafts.remove(matchId);
    }
}
