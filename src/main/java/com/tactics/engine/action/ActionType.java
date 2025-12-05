package com.tactics.engine.action;

/**
 * Enum representing the allowed action types.
 *
 * V1/V2: MOVE, ATTACK, MOVE_AND_ATTACK, END_TURN
 * V3 additions: USE_SKILL, DEATH_CHOICE
 *
 * Note: DESTROY_OBSTACLE was removed - use ATTACK on obstacles instead.
 * Any unit can attack obstacles (3 HP). POWER buff destroys instantly.
 */
public enum ActionType {
    // V1/V2 Core actions
    MOVE,
    ATTACK,             // Can target units OR obstacles (V3)
    MOVE_AND_ATTACK,
    END_TURN,

    // V3 Hero skill action
    USE_SKILL,          // Hero uses their selected skill

    // V3 Death mechanics action
    DEATH_CHOICE        // Player chooses obstacle or BUFF tile after minion death
}
