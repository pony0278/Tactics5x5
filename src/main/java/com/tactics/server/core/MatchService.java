package com.tactics.server.core;

import com.tactics.engine.action.Action;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;

/**
 * High-level service for orchestrating match operations.
 */
public class MatchService {

    private final MatchRegistry matchRegistry;
    private final RuleEngine ruleEngine;
    private final GameStateSerializer gameStateSerializer;

    public MatchService(MatchRegistry matchRegistry,
                        RuleEngine ruleEngine,
                        GameStateSerializer gameStateSerializer) {
        this.matchRegistry = matchRegistry;
        this.ruleEngine = ruleEngine;
        this.gameStateSerializer = gameStateSerializer;
    }

    public MatchRegistry getMatchRegistry() {
        return matchRegistry;
    }

    public RuleEngine getRuleEngine() {
        return ruleEngine;
    }

    public GameStateSerializer getGameStateSerializer() {
        return gameStateSerializer;
    }

    public Match getOrCreateMatch(String matchId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Match findMatch(String matchId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public GameState getCurrentState(String matchId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public GameState applyAction(String matchId,
                                 PlayerId playerId,
                                 Action action) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
