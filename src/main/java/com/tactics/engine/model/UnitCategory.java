package com.tactics.engine.model;

/**
 * Enum representing the category of a unit.
 * V3 introduces two categories: Hero (player's main character) and Minion (support units).
 */
public enum UnitCategory {
    HERO,   // Player's main character (SPD-integrated)
    MINION  // Support units (TANK, ARCHER, ASSASSIN)
}
