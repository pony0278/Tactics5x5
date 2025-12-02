package com.tactics.server.dto;

/**
 * Structured representation of action message payload.
 */
public class ActionRequest {

    private final String matchId;
    private final String playerId;
    private final ActionPayload action;

    public ActionRequest(String matchId,
                         String playerId,
                         ActionPayload action) {
        this.matchId = matchId;
        this.playerId = playerId;
        this.action = action;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public ActionPayload getAction() {
        return action;
    }
}
