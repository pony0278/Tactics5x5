package com.tactics.server.core;

import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.server.timer.TimerType;

/**
 * Result of applying an action, including timer information for clients.
 */
public class ActionResult {

    private final GameState newState;
    private final PlayerId nextPlayer;
    private final long actionStartTime;
    private final long timeoutMs;
    private final TimerType timerType;

    public ActionResult(GameState newState, PlayerId nextPlayer,
                        long actionStartTime, long timeoutMs, TimerType timerType) {
        this.newState = newState;
        this.nextPlayer = nextPlayer;
        this.actionStartTime = actionStartTime;
        this.timeoutMs = timeoutMs;
        this.timerType = timerType;
    }

    /**
     * Creates result for game over state (no timer).
     */
    public static ActionResult gameOver(GameState newState) {
        return new ActionResult(newState, null, -1, -1, null);
    }

    public GameState getNewState() {
        return newState;
    }

    public PlayerId getNextPlayer() {
        return nextPlayer;
    }

    public long getActionStartTime() {
        return actionStartTime;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public TimerType getTimerType() {
        return timerType;
    }

    public boolean isGameOver() {
        return newState != null && newState.isGameOver();
    }

    public boolean hasTimer() {
        return timerType != null && actionStartTime > 0;
    }
}
