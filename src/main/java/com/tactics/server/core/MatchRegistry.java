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

    public MatchRegistry(Map<String, Match> matches) {
        this.matches = matches;
    }

    public Map<String, Match> getMatches() {
        return matches;
    }

    public Match getMatch(String matchId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Match createMatch(String matchId, GameState initialState) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void updateMatchState(String matchId, GameState newState) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Collection<Match> listMatches() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
