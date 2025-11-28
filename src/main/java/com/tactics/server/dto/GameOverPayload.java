package com.tactics.server.dto;

import java.util.Map;

/**
 * Payload for game_over messages.
 */
public class GameOverPayload {

    private final String winner;
    private final Map<String, Object> state;

    public GameOverPayload(String winner, Map<String, Object> state) {
        this.winner = winner;
        this.state = state;
    }

    public String getWinner() {
        return winner;
    }

    public Map<String, Object> getState() {
        return state;
    }
}
