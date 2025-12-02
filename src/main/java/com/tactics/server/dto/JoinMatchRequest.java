package com.tactics.server.dto;

/**
 * Structured representation of join_match payload.
 */
public class JoinMatchRequest {

    private final String matchId;
    private final String playerId;

    public JoinMatchRequest(String matchId, String playerId) {
        this.matchId = matchId;
        this.playerId = playerId;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getPlayerId() {
        return playerId;
    }
}
