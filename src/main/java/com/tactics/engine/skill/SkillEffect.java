package com.tactics.engine.skill;

/**
 * Enum representing skill effect types.
 * Each skill can have one or more effects.
 */
public enum SkillEffect {
    /** Deal damage to target (amount in effectValue) */
    DAMAGE,

    /** Restore HP to target (amount in effectValue) */
    HEAL,

    /** Teleport/leap caster to target position */
    MOVE_SELF,

    /** Push/pull target in a direction */
    MOVE_TARGET,

    /** Apply a BUFF to target (buffType specified separately) */
    APPLY_BUFF,

    /** Remove a BUFF from target */
    REMOVE_BUFF,

    /** Create a temporary unit */
    SPAWN_UNIT,

    /** Create an obstacle */
    SPAWN_OBSTACLE,

    /** Apply shield to target (amount in effectValue) */
    APPLY_SHIELD,

    /** Mark target for bonus damage */
    MARK,

    /** Apply invisibility to target */
    INVISIBILITY,

    /** Apply invulnerability to target */
    INVULNERABILITY,

    /** Area damage around landing position */
    DAMAGE_AREA_ON_ARRIVAL,

    /** Knockback enemies */
    KNOCKBACK,

    /** Empower next N attacks */
    EMPOWER_ATTACKS
}
