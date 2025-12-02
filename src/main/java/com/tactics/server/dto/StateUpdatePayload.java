package com.tactics.server.dto;

import java.util.Map;

/**
 * Payload for state_update messages.
 */
public class StateUpdatePayload {

    private final Map<String, Object> state;

    public StateUpdatePayload(Map<String, Object> state) {
        this.state = state;
    }

    public Map<String, Object> getState() {
        return state;
    }
}
