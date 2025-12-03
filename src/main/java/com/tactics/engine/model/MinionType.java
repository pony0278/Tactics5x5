package com.tactics.engine.model;

/**
 * Enum representing the type of minion unit.
 * Each type has different stats and passive abilities.
 */
public enum MinionType {
    TANK,      // HP:5, ATK:1, Move:1, AtkRange:1 - Guardian passive
    ARCHER,    // HP:3, ATK:1, Move:1, AtkRange:3 - Long Shot passive
    ASSASSIN   // HP:2, ATK:2, Move:4, AtkRange:1 - Swift passive
}
