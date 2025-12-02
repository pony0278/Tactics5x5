package com.tactics.server.core;

import com.tactics.engine.action.Action;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.rules.ValidationResult;
import com.tactics.engine.util.GameStateFactory;
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
        Match existing = matchRegistry.getMatch(matchId);
        if (existing != null) {
            return existing;
        }
        // Create initial GameState with standard unit placement
        GameState initialState = GameStateFactory.createStandardGame();
        return matchRegistry.createMatch(matchId, initialState);
    }

    public Match findMatch(String matchId) {
        return matchRegistry.getMatch(matchId);
    }

    public GameState getCurrentState(String matchId) {
        Match match = matchRegistry.getMatch(matchId);
        if (match == null) {
            return null;
        }
        return match.getState();
    }

    public GameState applyAction(String matchId,
                                 PlayerId playerId,
                                 Action action) {
        // Look up match
        Match match = matchRegistry.getMatch(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Unknown match: " + matchId);
        }

        GameState currentState = match.getState();

        // Validate action using RuleEngine
        ValidationResult validation = ruleEngine.validateAction(currentState, action);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }

        // Apply action using RuleEngine (returns new immutable GameState)
        GameState newState = ruleEngine.applyAction(currentState, action);

        // Update match in registry with new state
        matchRegistry.updateMatchState(matchId, newState);

        return newState;
    }
}
