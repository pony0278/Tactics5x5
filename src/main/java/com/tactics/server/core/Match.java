package com.tactics.server.core;

import com.tactics.engine.model.GameState;
import com.tactics.server.ws.ClientConnection;

import java.util.Map;

/**
 * Represents a single active match and its server-side state.
 */
public class Match {

    private final MatchId matchId;
    private final GameState state;
    private final Map<ClientSlot, ClientConnection> connections;

    public Match(MatchId matchId,
                 GameState state,
                 Map<ClientSlot, ClientConnection> connections) {
        this.matchId = matchId;
        this.state = state;
        this.connections = connections;
    }

    public MatchId getMatchId() {
        return matchId;
    }

    public GameState getState() {
        return state;
    }

    public Map<ClientSlot, ClientConnection> getConnections() {
        return connections;
    }
}
