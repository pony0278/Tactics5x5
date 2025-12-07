package com.tactics.server.timer;

import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;

/**
 * Callback interface for timer events.
 * Implemented by WebSocket layer to send messages to clients.
 */
public interface TimerCallback {

    /**
     * Called when an Action Timer expires.
     *
     * @param matchId the match identifier
     * @param playerId the player who timed out
     * @param newState the new game state after timeout penalty (Hero -1 HP)
     */
    void onActionTimeout(String matchId, PlayerId playerId, GameState newState);

    /**
     * Called when a Death Choice Timer expires.
     *
     * @param matchId the match identifier
     * @param playerId the player who timed out (owner of dead minion)
     * @param newState the new game state after default obstacle spawned
     */
    void onDeathChoiceTimeout(String matchId, PlayerId playerId, GameState newState);

    /**
     * Called when a Draft Timer expires.
     *
     * @param matchId the match identifier
     */
    void onDraftTimeout(String matchId);
}
