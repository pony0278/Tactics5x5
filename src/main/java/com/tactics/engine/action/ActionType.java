package com.tactics.engine.action;

/**
 * Enum representing the allowed action types.
 *
 * V1/V2: MOVE, ATTACK, MOVE_AND_ATTACK, END_TURN
 * V3 additions: USE_SKILL, DESTROY_OBSTACLE, DEATH_CHOICE
 */
public enum ActionType {
    // V1/V2 Core actions
    MOVE,
    ATTACK,
    MOVE_AND_ATTACK,
    END_TURN,

    // V3 Hero skill action
    USE_SKILL,          // Hero uses their selected skill

    // V3 POWER buff action
    DESTROY_OBSTACLE,   // Unit with POWER buff destroys adjacent obstacle

    // V3 Death mechanics action
    DEATH_CHOICE        // Player chooses obstacle or BUFF tile after minion death
}
