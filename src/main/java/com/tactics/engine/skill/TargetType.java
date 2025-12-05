package com.tactics.engine.skill;

/**
 * Enum representing skill target types.
 * Determines how a skill selects its target(s).
 */
public enum TargetType {
    /** Affects only the caster */
    SELF,

    /** Target one enemy unit */
    SINGLE_ENEMY,

    /** Target one friendly unit (including self) */
    SINGLE_ALLY,

    /** Target an empty tile */
    SINGLE_TILE,

    /** Affects tiles around caster (typically adjacent) */
    AREA_AROUND_SELF,

    /** Affects tiles around target */
    AREA_AROUND_TARGET,

    /** Affects tiles in a straight line from caster */
    LINE,

    /** Affects all enemy units on the board */
    ALL_ENEMIES,

    /** Affects all friendly units on the board */
    ALL_ALLIES
}
