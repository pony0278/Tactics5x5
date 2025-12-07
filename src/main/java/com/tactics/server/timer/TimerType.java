package com.tactics.server.timer;

/**
 * Types of timers in the game.
 */
public enum TimerType {
    /**
     * Action Timer: 10 seconds per action.
     * Timeout: Hero -1 HP + auto END_TURN.
     */
    ACTION,

    /**
     * Death Choice Timer: 5 seconds for choosing Obstacle or BUFF Tile.
     * Timeout: Default to Obstacle, no HP penalty.
     */
    DEATH_CHOICE,

    /**
     * Draft Timer: 60 seconds for the entire draft phase.
     * Timeout: Random selection for incomplete choices.
     */
    DRAFT
}
