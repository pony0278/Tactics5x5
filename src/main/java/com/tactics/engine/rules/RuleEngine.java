package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.model.GameState;
import com.tactics.engine.skill.SkillExecutor;
import com.tactics.engine.util.RngProvider;

/**
 * Facade for validating and applying game actions.
 *
 * This class delegates to specialized components:
 * - ActionValidator: handles all action validation logic
 * - ActionExecutor: handles all action execution logic
 * - SkillExecutor: handles skill implementations
 *
 * V2 Update: Validation uses unit.moveRange and unit.attackRange
 * for distance checks instead of fixed distance=1.
 *
 * V3 (Buff System): Validation and apply consider buff effects:
 * - stunned: cannot MOVE, ATTACK, MOVE_AND_ATTACK (but can END_TURN)
 * - rooted: cannot MOVE or MOVE_AND_ATTACK movement step
 * - bonusMoveRange: increases effective move range
 * - bonusAttackRange: increases effective attack range
 * - bonusAttack: increases damage dealt
 * - poison: deals 1 damage per poison buff at turn end
 * - SPEED buff allows 2 actions per turn
 * - SLOW buff delays actions by 1 round
 * - BLEED buff deals 1 damage per round
 * - Buff tiles trigger when stepped on
 */
public class RuleEngine {

    // V3: RngProvider for buff tile randomness
    private RngProvider rngProvider;

    // V3: SkillExecutor for skill implementations
    private final SkillExecutor skillExecutor;

    // ActionValidator for validation logic
    private final ActionValidator actionValidator;

    // ActionExecutor for action execution logic
    private final ActionExecutor actionExecutor;

    public RuleEngine() {
        this.rngProvider = new RngProvider();  // Default with time-based seed
        this.skillExecutor = new SkillExecutor();
        this.actionValidator = new ActionValidator();
        this.actionExecutor = new ActionExecutor();
    }

    /**
     * V3: Set the RngProvider for deterministic buff tile triggers.
     */
    public void setRngProvider(RngProvider rngProvider) {
        this.rngProvider = rngProvider;
        this.skillExecutor.setRngProvider(rngProvider);  // Keep in sync
        this.actionExecutor.setRngProvider(rngProvider);  // Keep in sync
    }

    /**
     * V3: Get the RngProvider.
     */
    public RngProvider getRngProvider() {
        return rngProvider;
    }

    // =========================================================================
    // Validation (delegates to ActionValidator)
    // =========================================================================

    /**
     * Validate an action before applying it.
     *
     * @param state the current game state
     * @param action the action to validate
     * @return validation result indicating success or failure with reason
     */
    public ValidationResult validateAction(GameState state, Action action) {
        return actionValidator.validateAction(state, action);
    }

    // =========================================================================
    // Apply Action (delegates to ActionExecutor)
    // =========================================================================

    /**
     * Apply an action to the game state.
     * Assumes the action has already been validated.
     *
     * @param state the current game state
     * @param action the action to apply
     * @return the new game state after applying the action
     */
    public GameState applyAction(GameState state, Action action) {
        return actionExecutor.applyAction(state, action);
    }
}
