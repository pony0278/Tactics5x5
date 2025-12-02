package com.tactics.server.core;

import com.tactics.engine.model.GameState;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * In-memory registry for all active matches.
 */
public class MatchRegistry {

    private final Map<String, Match> matches;

    public MatchRegistry() {
        this.matches = new java.util.concurrent.ConcurrentHashMap<>();
    }

    public MatchRegistry(Map<String, Match> matches) {
        this.matches = matches;
    }

    public Map<String, Match> getMatches() {
        return matches;
    }

    public Match getMatch(String matchId) {
        return matches.get(matchId);
    }

    public Match createMatch(String matchId, GameState initialState) {
        MatchId id = new MatchId(matchId);
        Map<ClientSlot, com.tactics.server.ws.ClientConnection> connections = new java.util.HashMap<>();
        Match match = new Match(id, initialState, connections);
        matches.put(matchId, match);
        return match;
    }

    public void updateMatchState(String matchId, GameState newState) {
        Match existing = matches.get(matchId);
        if (existing != null) {
            Match updated = new Match(existing.getMatchId(), newState, existing.getConnections());
            matches.put(matchId, updated);
        }
    }

    public Collection<Match> listMatches() {
        return matches.values();
    }
}
