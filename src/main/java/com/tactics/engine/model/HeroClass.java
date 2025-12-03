package com.tactics.engine.model;

/**
 * Enum representing the hero class (based on SPD character classes).
 * Each class has access to 3 unique skills, player selects 1 before match.
 */
public enum HeroClass {
    WARRIOR,   // Melee/Tank - Heroic Leap, Shockwave, Endure
    MAGE,      // Ranged/Control - Elemental Blast, Warp Beacon, Wild Magic
    ROGUE,     // Assassin - Smoke Bomb, Death Mark, Shadow Clone
    HUNTRESS,  // Ranged/Summoner - Spirit Hawk, Spectral Blades, Nature's Power
    DUELIST,   // Duelist - Challenge, Elemental Strike, Feint
    CLERIC     // Support - Trinity, Power of Many, Ascended Form
}
