package com.tactics.engine.buff;

/**
 * Enum representing the 6 V3 BUFF types.
 * Positive buffs: POWER, LIFE, SPEED
 * Negative buffs: WEAKNESS, BLEED, SLOW
 */
public enum BuffType {
    // Positive BUFFs
    POWER,     // +3 ATK, +1 HP, blocks MOVE_AND_ATTACK, can destroy obstacles
    LIFE,      // +3 HP (instant heal)
    SPEED,     // -1 ATK, double action per round

    // Negative BUFFs
    WEAKNESS,  // -2 ATK, -1 HP
    BLEED,     // -1 HP per round (damage over time)
    SLOW,      // Actions delayed by 1 round
    BLIND,     // Cannot attack for 1 round (from Smoke Bomb)

    // Phase 4D Skill-specific BUFFs
    DEATH_MARK,    // +2 damage taken, source heals 2 HP on kill (Rogue Death Mark)
    FEINT,         // Dodge next attack, counter 2 damage (Duelist Feint)
    CHALLENGE,     // 50% damage to non-Duelist, counter on attack (Duelist Challenge)
    INVULNERABLE   // Cannot take damage (Cleric Ascended Form)
}
