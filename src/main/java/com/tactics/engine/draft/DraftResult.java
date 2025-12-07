package com.tactics.engine.draft;

import com.tactics.engine.model.PlayerId;

import java.util.Objects;

/**
 * Represents the combined draft results from both players.
 *
 * Immutable class containing both players' draft selections.
 * Used to create the initial GameState once both players complete their drafts.
 */
public class DraftResult {

    private final DraftState player1Draft;
    private final DraftState player2Draft;

    /**
     * Create a DraftResult from both players' draft states.
     *
     * @param player1Draft Player 1's draft state
     * @param player2Draft Player 2's draft state
     * @throws IllegalArgumentException if either state is null or has wrong player ID
     */
    public DraftResult(DraftState player1Draft, DraftState player2Draft) {
        if (player1Draft == null) {
            throw new IllegalArgumentException("Player 1 draft cannot be null");
        }
        if (player2Draft == null) {
            throw new IllegalArgumentException("Player 2 draft cannot be null");
        }
        if (!player1Draft.getPlayerId().equals(PlayerId.PLAYER_1)) {
            throw new IllegalArgumentException(
                "Player 1 draft must have PLAYER_1 ID, got: " + player1Draft.getPlayerId());
        }
        if (!player2Draft.getPlayerId().equals(PlayerId.PLAYER_2)) {
            throw new IllegalArgumentException(
                "Player 2 draft must have PLAYER_2 ID, got: " + player2Draft.getPlayerId());
        }

        this.player1Draft = player1Draft;
        this.player2Draft = player2Draft;
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /**
     * Get Player 1's draft state.
     */
    public DraftState getPlayer1Draft() {
        return player1Draft;
    }

    /**
     * Get Player 2's draft state.
     */
    public DraftState getPlayer2Draft() {
        return player2Draft;
    }

    /**
     * Get the draft state for a specific player.
     *
     * @param playerId The player ID
     * @return The draft state for that player
     */
    public DraftState getDraft(PlayerId playerId) {
        if (playerId.equals(PlayerId.PLAYER_1)) {
            return player1Draft;
        } else {
            return player2Draft;
        }
    }

    // =========================================================================
    // Completion Check
    // =========================================================================

    /**
     * Check if both players have completed their drafts.
     */
    public boolean isComplete() {
        return player1Draft.isComplete() && player2Draft.isComplete();
    }

    // =========================================================================
    // Object Methods
    // =========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DraftResult that = (DraftResult) o;
        return Objects.equals(player1Draft, that.player1Draft) &&
               Objects.equals(player2Draft, that.player2Draft);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player1Draft, player2Draft);
    }

    @Override
    public String toString() {
        return "DraftResult{" +
               "player1Draft=" + player1Draft +
               ", player2Draft=" + player2Draft +
               ", isComplete=" + isComplete() +
               '}';
    }
}
