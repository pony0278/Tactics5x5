package com.tactics.engine.buff;

import java.util.UUID;

/**
 * Factory for creating standard V3 buff instances.
 * All buffs have:
 * - Default duration of 2 rounds
 * - Not stackable (same type refreshes duration)
 * - Correct modifiers and flags for each type
 */
public class BuffFactory {

    public static final int DEFAULT_DURATION = 2;

    private BuffFactory() {
        // Utility class
    }

    /**
     * Create a buff instance of the specified type.
     */
    public static BuffInstance create(BuffType type, String sourceUnitId) {
        switch (type) {
            case POWER:
                return createPower(sourceUnitId);
            case LIFE:
                return createLife(sourceUnitId);
            case SPEED:
                return createSpeed(sourceUnitId);
            case WEAKNESS:
                return createWeakness(sourceUnitId);
            case BLEED:
                return createBleed(sourceUnitId);
            case SLOW:
                return createSlow(sourceUnitId);
            default:
                throw new IllegalArgumentException("Unknown buff type: " + type);
        }
    }

    /**
     * Create a POWER buff.
     * Effects: +3 ATK, +1 HP (instant), blocks MOVE_AND_ATTACK, instant obstacle destroy via ATTACK
     */
    public static BuffInstance createPower(String sourceUnitId) {
        return new BuffInstance(
            generateBuffId(),
            sourceUnitId,
            BuffType.POWER,
            DEFAULT_DURATION,
            false,  // not stackable
            new BuffModifier(0, 3, 0, 0),  // +3 ATK (HP is instant, not ongoing)
            BuffFlags.power(),
            1  // +1 HP instant
        );
    }

    /**
     * Create a LIFE buff.
     * Effects: +3 HP (instant only, no ongoing modifier)
     */
    public static BuffInstance createLife(String sourceUnitId) {
        return new BuffInstance(
            generateBuffId(),
            sourceUnitId,
            BuffType.LIFE,
            DEFAULT_DURATION,
            false,  // not stackable
            new BuffModifier(0, 0, 0, 0),  // No ongoing modifiers
            BuffFlags.life(),
            3  // +3 HP instant
        );
    }

    /**
     * Create a SPEED buff.
     * Effects: -1 ATK, grants double action per round
     */
    public static BuffInstance createSpeed(String sourceUnitId) {
        return new BuffInstance(
            generateBuffId(),
            sourceUnitId,
            BuffType.SPEED,
            DEFAULT_DURATION,
            false,  // not stackable
            new BuffModifier(0, -1, 0, 0),  // -1 ATK
            BuffFlags.speed(),
            0  // no instant HP change
        );
    }

    /**
     * Create a WEAKNESS buff.
     * Effects: -2 ATK, -1 HP (instant)
     */
    public static BuffInstance createWeakness(String sourceUnitId) {
        return new BuffInstance(
            generateBuffId(),
            sourceUnitId,
            BuffType.WEAKNESS,
            DEFAULT_DURATION,
            false,  // not stackable
            new BuffModifier(0, -2, 0, 0),  // -2 ATK
            BuffFlags.none(),
            -1  // -1 HP instant
        );
    }

    /**
     * Create a BLEED buff.
     * Effects: -1 HP per round at round end
     */
    public static BuffInstance createBleed(String sourceUnitId) {
        return new BuffInstance(
            generateBuffId(),
            sourceUnitId,
            BuffType.BLEED,
            DEFAULT_DURATION,
            false,  // not stackable - but damage stacks if multiple applications
            new BuffModifier(0, 0, 0, 0),  // No stat modifiers
            BuffFlags.bleed(),
            0  // no instant HP change
        );
    }

    /**
     * Create a SLOW buff.
     * Effects: Actions are delayed by 1 round (preparing state)
     */
    public static BuffInstance createSlow(String sourceUnitId) {
        return new BuffInstance(
            generateBuffId(),
            sourceUnitId,
            BuffType.SLOW,
            DEFAULT_DURATION,
            false,  // not stackable
            new BuffModifier(0, 0, 0, 0),  // No stat modifiers
            BuffFlags.slow(),
            0  // no instant HP change
        );
    }

    /**
     * Generate a unique buff ID.
     */
    private static String generateBuffId() {
        return "buff_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
