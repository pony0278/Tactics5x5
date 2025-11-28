package com.tactics.server.dto;

import java.util.Map;

/**
 * Payload for match_joined messages.
 */
public class MatchJoinedPayload {

    private final String matchId;
    private final String playerId;
    private final Map<String, Object> state;

    public MatchJoinedPayload(String matchId,
                              String playerId,
                              Map<String, Object> state) {
        this.matchId = matchId;
        this.playerId = playerId;
        this.state = state;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public Map<String, Object> getState() {
        return state;
    }
}
