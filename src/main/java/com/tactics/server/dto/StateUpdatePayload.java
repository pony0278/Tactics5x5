package com.tactics.server.dto;

import java.util.Map;

/**
 * Payload for state_update messages.
 * Extended with timer information for V3.
 */
public class StateUpdatePayload {

    private final Map<String, Object> state;
    private final TimerPayload timer;
    private final String currentPlayerId;

    /**
     * Constructor without timer (backward compatible).
     */
    public StateUpdatePayload(Map<String, Object> state) {
        this(state, null, null);
    }

    /**
     * Constructor with timer info.
     */
    public StateUpdatePayload(Map<String, Object> state, TimerPayload timer, String currentPlayerId) {
        this.state = state;
        this.timer = timer;
        this.currentPlayerId = currentPlayerId;
    }

    public Map<String, Object> getState() {
        return state;
    }

    /**
     * Timer information for the current turn.
     * Null if no timer active (e.g., game over).
     */
    public TimerPayload getTimer() {
        return timer;
    }

    /**
     * ID of the player whose turn it is.
     * Helps client know if timer applies to them.
     */
    public String getCurrentPlayerId() {
        return currentPlayerId;
    }
}
