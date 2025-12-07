package com.tactics.server.core;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.model.DeathChoice;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Unit;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.rules.ValidationResult;
import com.tactics.engine.util.GameStateFactory;
import com.tactics.engine.util.GameStateSerializer;
import com.tactics.server.timer.TimerCallback;
import com.tactics.server.timer.TimerConfig;
import com.tactics.server.timer.TimerService;
import com.tactics.server.timer.TimerState;
import com.tactics.server.timer.TimerType;

import java.util.List;

/**
 * High-level service for orchestrating match operations.
 * Integrates with TimerService for action timeouts.
 */
public class MatchService {

    private final MatchRegistry matchRegistry;
    private final RuleEngine ruleEngine;
    private final GameStateSerializer gameStateSerializer;
    private final TimerService timerService;
    private TimerCallback timerCallback;

    public MatchService(MatchRegistry matchRegistry,
                        RuleEngine ruleEngine,
                        GameStateSerializer gameStateSerializer) {
        this(matchRegistry, ruleEngine, gameStateSerializer, new TimerService());
    }

    public MatchService(MatchRegistry matchRegistry,
                        RuleEngine ruleEngine,
                        GameStateSerializer gameStateSerializer,
                        TimerService timerService) {
        this.matchRegistry = matchRegistry;
        this.ruleEngine = ruleEngine;
        this.gameStateSerializer = gameStateSerializer;
        this.timerService = timerService;
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

    public TimerService getTimerService() {
        return timerService;
    }

    /**
     * Sets the callback for timer events.
     * Must be set before starting matches.
     */
    public void setTimerCallback(TimerCallback callback) {
        this.timerCallback = callback;
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

    /**
     * Starts the action timer for the current player's turn.
     * Call this when sending YOUR_TURN message.
     *
     * @param matchId the match identifier
     * @return the timer start timestamp, or -1 if no match found
     */
    public long startTurnTimer(String matchId) {
        Match match = matchRegistry.getMatch(matchId);
        if (match == null) {
            return -1;
        }

        GameState state = match.getState();
        if (state.isGameOver()) {
            return -1;
        }

        PlayerId currentPlayer = state.getCurrentPlayer();

        return timerService.startActionTimer(matchId, currentPlayer, () -> {
            handleActionTimeout(matchId, currentPlayer);
        });
    }

    /**
     * Applies an action and manages the timer.
     *
     * @return ActionResult containing new state and timer info
     */
    public ActionResult applyActionWithTimer(String matchId,
                                              PlayerId playerId,
                                              Action action) {
        // Handle Death Choice action separately
        if (action.getType() == ActionType.DEATH_CHOICE) {
            return applyDeathChoiceWithTimer(matchId, playerId, action);
        }

        // Validate timer state for regular actions
        validateActionTimerState(matchId);

        // Execute the action
        GameState newState = executeAction(matchId, action);

        // Handle post-action timer management
        return handlePostActionTimer(matchId, newState);
    }

    /**
     * Validates timer state before accepting a regular action.
     * Throws if action cannot be accepted due to timer state.
     */
    private void validateActionTimerState(String matchId) {
        TimerState actionTimerState = timerService.getTimerState(matchId, TimerType.ACTION);
        TimerState deathChoiceTimerState = timerService.getTimerState(matchId, TimerType.DEATH_CHOICE);

        // If Death Choice timer is running, reject non-DEATH_CHOICE actions
        if (deathChoiceTimerState == TimerState.RUNNING) {
            throw new IllegalArgumentException("Death Choice pending - only DEATH_CHOICE action allowed");
        }

        if (actionTimerState == TimerState.TIMEOUT) {
            throw new IllegalArgumentException("Action timeout already processed");
        }

        // Check if within grace period
        if (actionTimerState != TimerState.RUNNING && actionTimerState != TimerState.PAUSED
                && !timerService.isWithinGracePeriod(matchId, TimerType.ACTION)) {
            if (actionTimerState != null) {
                throw new IllegalArgumentException("Timer not active for this action");
            }
            // timerState == null means no timer started (e.g., first action) - allow it
        }
    }

    /**
     * Executes an action: validates, applies, and updates match state.
     * @return the new game state after action
     */
    private GameState executeAction(String matchId, Action action) {
        Match match = matchRegistry.getMatch(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Unknown match: " + matchId);
        }

        GameState currentState = match.getState();

        // Validate action using RuleEngine
        ValidationResult validation = ruleEngine.validateAction(currentState, action);
        if (!validation.isValid()) {
            // Invalid action does NOT reset timer (TA-004)
            throw new IllegalArgumentException(validation.getErrorMessage());
        }

        // Apply action using RuleEngine (returns new immutable GameState)
        GameState newState = ruleEngine.applyAction(currentState, action);

        // Update match in registry with new state
        matchRegistry.updateMatchState(matchId, newState);

        return newState;
    }

    /**
     * Handles timer management after action execution.
     * Handles game over, Death Choice, or normal turn transition.
     */
    private ActionResult handlePostActionTimer(String matchId, GameState newState) {
        // Handle game over
        if (newState.isGameOver()) {
            timerService.cancelTimer(matchId, TimerType.ACTION);
            return ActionResult.gameOver(newState);
        }

        // Check for pending Death Choice (TD-001, TD-004)
        if (newState.hasPendingDeathChoice()) {
            return startDeathChoiceTimer(matchId, newState);
        }

        // Normal turn transition
        return startNextTurnTimer(matchId, newState);
    }

    /**
     * Starts Death Choice timer when a minion dies.
     */
    private ActionResult startDeathChoiceTimer(String matchId, GameState newState) {
        DeathChoice deathChoice = newState.getPendingDeathChoice();
        PlayerId owner = deathChoice.getOwner();

        // Pause the Action Timer (NOT complete - we need to track it was paused)
        timerService.pauseActionTimer(matchId);

        // Start Death Choice Timer for the owner
        long startTime = timerService.startDeathChoiceTimer(matchId, owner, () -> {
            handleDeathChoiceTimeout(matchId, owner);
        });

        return new ActionResult(newState, owner, startTime,
                TimerConfig.DEATH_CHOICE_TIMEOUT_MS, TimerType.DEATH_CHOICE);
    }

    /**
     * Starts timer for the next player's turn.
     */
    private ActionResult startNextTurnTimer(String matchId, GameState newState) {
        // Complete the timer (valid action received, no Death Choice)
        timerService.completeTimer(matchId, TimerType.ACTION);

        // Start timer for next player's turn
        PlayerId nextPlayer = newState.getCurrentPlayer();
        long startTime = timerService.startActionTimer(matchId, nextPlayer, () -> {
            handleActionTimeout(matchId, nextPlayer);
        });

        return new ActionResult(newState, nextPlayer, startTime,
                TimerConfig.ACTION_TIMEOUT_MS, TimerType.ACTION);
    }

    /**
     * Applies a Death Choice action and manages timers.
     * After Death Choice is resolved, Action Timer resets to 10s (TD-005).
     */
    private ActionResult applyDeathChoiceWithTimer(String matchId,
                                                    PlayerId playerId,
                                                    Action action) {
        // Verify Death Choice timer is active
        TimerState timerState = timerService.getTimerState(matchId, TimerType.DEATH_CHOICE);
        if (timerState == TimerState.TIMEOUT) {
            throw new IllegalArgumentException("Death Choice timeout already processed");
        }

        if (timerState != TimerState.RUNNING && !timerService.isWithinGracePeriod(matchId, TimerType.DEATH_CHOICE)) {
            if (timerState != null) {
                throw new IllegalArgumentException("Death Choice timer not active");
            }
        }

        // Look up match
        Match match = matchRegistry.getMatch(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Unknown match: " + matchId);
        }

        GameState currentState = match.getState();

        // Verify there's a pending Death Choice
        if (!currentState.hasPendingDeathChoice()) {
            throw new IllegalArgumentException("No pending Death Choice");
        }

        // Verify the player making the choice is the owner
        DeathChoice deathChoice = currentState.getPendingDeathChoice();
        if (!deathChoice.getOwner().equals(playerId)) {
            throw new IllegalArgumentException("Only the dead minion's owner can make the Death Choice");
        }

        // Validate action using RuleEngine
        ValidationResult validation = ruleEngine.validateAction(currentState, action);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }

        // Complete the Death Choice timer
        timerService.completeTimer(matchId, TimerType.DEATH_CHOICE);

        // Apply Death Choice action
        GameState newState = ruleEngine.applyAction(currentState, action);

        // Update match in registry
        matchRegistry.updateMatchState(matchId, newState);

        // Handle game over
        if (newState.isGameOver()) {
            timerService.cancelTimer(matchId, TimerType.ACTION);
            return ActionResult.gameOver(newState);
        }

        // Reset Action Timer to 10s (TD-005) - NOT resume from paused time
        PlayerId nextPlayer = newState.getCurrentPlayer();
        long startTime = timerService.startActionTimer(matchId, nextPlayer, () -> {
            handleActionTimeout(matchId, nextPlayer);
        });

        return new ActionResult(newState, nextPlayer, startTime,
                TimerConfig.ACTION_TIMEOUT_MS, TimerType.ACTION);
    }

    /**
     * Original applyAction method for backwards compatibility.
     * Does not manage timers.
     */
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

    /**
     * Handles action timeout: Hero -1 HP + auto END_TURN.
     */
    private void handleActionTimeout(String matchId, PlayerId playerId) {
        Match match = matchRegistry.getMatch(matchId);
        if (match == null) {
            return;
        }

        GameState currentState = match.getState();
        if (currentState.isGameOver()) {
            return;
        }

        // Find the player's Hero and apply -1 HP
        GameState newState = applyHeroDamage(currentState, playerId, 1);

        // Check if Hero died from timeout
        if (newState.isGameOver()) {
            matchRegistry.updateMatchState(matchId, newState);
            timerService.cancelTimer(matchId, TimerType.ACTION);

            if (timerCallback != null) {
                timerCallback.onActionTimeout(matchId, playerId, newState);
            }
            return;
        }

        // Auto END_TURN
        Action endTurn = new Action(ActionType.END_TURN, playerId, null, null);
        ValidationResult validation = ruleEngine.validateAction(newState, endTurn);

        if (validation.isValid()) {
            newState = ruleEngine.applyAction(newState, endTurn);
        }

        matchRegistry.updateMatchState(matchId, newState);

        // Notify callback
        if (timerCallback != null) {
            timerCallback.onActionTimeout(matchId, playerId, newState);
        }

        // Start timer for next player if game not over
        if (!newState.isGameOver()) {
            PlayerId nextPlayer = newState.getCurrentPlayer();
            timerService.startActionTimer(matchId, nextPlayer, () -> {
                handleActionTimeout(matchId, nextPlayer);
            });
        }
    }

    /**
     * Handles Death Choice timeout: defaults to SPAWN_OBSTACLE (TD-002, TD-011).
     * No HP penalty for Death Choice timeout.
     */
    private void handleDeathChoiceTimeout(String matchId, PlayerId playerId) {
        Match match = matchRegistry.getMatch(matchId);
        if (match == null) {
            return;
        }

        GameState currentState = match.getState();
        if (currentState.isGameOver()) {
            return;
        }

        DeathChoice deathChoice = currentState.getPendingDeathChoice();
        if (deathChoice == null) {
            return; // No pending Death Choice (shouldn't happen)
        }

        // Create default DEATH_CHOICE action with SPAWN_OBSTACLE
        Action defaultChoice = Action.deathChoice(playerId, DeathChoice.ChoiceType.SPAWN_OBSTACLE);

        // Apply the default choice
        GameState newState = ruleEngine.applyAction(currentState, defaultChoice);
        matchRegistry.updateMatchState(matchId, newState);

        // Notify callback
        if (timerCallback != null) {
            timerCallback.onDeathChoiceTimeout(matchId, playerId, newState);
        }

        // Handle game over (unlikely from Death Choice, but possible)
        if (newState.isGameOver()) {
            timerService.cancelTimer(matchId, TimerType.ACTION);
            return;
        }

        // Reset Action Timer to 10s for next turn (TD-005)
        PlayerId nextPlayer = newState.getCurrentPlayer();
        timerService.startActionTimer(matchId, nextPlayer, () -> {
            handleActionTimeout(matchId, nextPlayer);
        });
    }

    /**
     * Applies damage to a player's Hero.
     *
     * @param state current game state
     * @param playerId the player whose Hero takes damage
     * @param damage amount of damage
     * @return new game state with damage applied
     */
    private GameState applyHeroDamage(GameState state, PlayerId playerId, int damage) {
        // Find the player's Hero
        Unit hero = null;
        for (Unit unit : state.getUnits()) {
            if (unit.getOwner().equals(playerId) && unit.isHero()) {
                hero = unit;
                break;
            }
        }

        if (hero == null) {
            return state; // No Hero found (shouldn't happen)
        }

        int newHp = Math.max(0, hero.getHp() - damage);
        Unit damagedHero = hero.withHp(newHp);

        // Replace Hero in units list
        java.util.List<Unit> newUnits = new java.util.ArrayList<>();
        for (Unit unit : state.getUnits()) {
            if (unit.getId().equals(hero.getId())) {
                if (newHp > 0) {
                    newUnits.add(damagedHero);
                }
                // If newHp <= 0, Hero is dead (don't add to list)
            } else {
                newUnits.add(unit);
            }
        }

        // Check for game over (Hero died)
        boolean gameOver = newHp <= 0;
        PlayerId winner = null;
        if (gameOver) {
            // Other player wins
            winner = playerId.getValue().equals("P1") ? new PlayerId("P2") : new PlayerId("P1");
        }

        return new GameState(
                state.getBoard(),
                newUnits,
                state.getCurrentPlayer(),
                gameOver,
                winner,
                state.getUnitBuffs(),
                state.getBuffTiles(),
                state.getObstacles(),
                state.getCurrentRound(),
                state.getPendingDeathChoice()
        );
    }

    /**
     * Gets the remaining time on the action timer.
     *
     * @param matchId the match identifier
     * @return remaining time in ms, or -1 if no timer
     */
    public long getActionTimerRemaining(String matchId) {
        return timerService.getRemainingTime(matchId, TimerType.ACTION);
    }

    /**
     * Cancels all timers for a match (e.g., on disconnect or game end).
     */
    public void cancelMatchTimers(String matchId) {
        timerService.cancelTimer(matchId, TimerType.ACTION);
        timerService.cancelTimer(matchId, TimerType.DEATH_CHOICE);
        timerService.cancelTimer(matchId, TimerType.DRAFT);
    }
}
