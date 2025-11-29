package com.tactics.server.ws;

/**
 * Framework-agnostic abstraction of a WebSocket connection.
 */
public interface ClientConnection {

    String getId();

    String getMatchId();

    void setMatchId(String matchId);

    String getPlayerId();

    void setPlayerId(String playerId);

    void sendMessage(String message);
}
