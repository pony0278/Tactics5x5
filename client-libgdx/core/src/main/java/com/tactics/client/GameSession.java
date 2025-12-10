package com.tactics.client;

/**
 * Stores the current game session state including matchId and playerId.
 * These values are assigned by the server in the match_joined response
 * and must be included in all subsequent action messages.
 */
public class GameSession {

    private static GameSession instance;

    private String matchId;
    private String playerId;
    private boolean inMatch = false;

    private GameSession() {
    }

    /**
     * Get the singleton instance.
     */
    public static GameSession getInstance() {
        if (instance == null) {
            instance = new GameSession();
        }
        return instance;
    }

    /**
     * Set the match session info from server's match_joined response.
     * @param matchId The server-assigned match ID
     * @param playerId The server-assigned player ID (P1 or P2)
     */
    public void setMatchInfo(String matchId, String playerId) {
        this.matchId = matchId;
        this.playerId = playerId;
        this.inMatch = true;
    }

    /**
     * Clear the session (when leaving a match or disconnecting).
     */
    public void clear() {
        this.matchId = null;
        this.playerId = null;
        this.inMatch = false;
    }

    /**
     * Get the current match ID.
     */
    public String getMatchId() {
        return matchId;
    }

    /**
     * Get the current player ID (assigned by server, typically P1 or P2).
     */
    public String getPlayerId() {
        return playerId;
    }

    /**
     * Check if currently in a match.
     */
    public boolean isInMatch() {
        return inMatch;
    }

    /**
     * Check if session has valid IDs for sending actions.
     */
    public boolean hasValidSession() {
        return matchId != null && !matchId.isEmpty()
            && playerId != null && !playerId.isEmpty();
    }
}
