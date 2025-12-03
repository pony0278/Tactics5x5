package com.tactics.engine.buff;

import java.util.Objects;

/**
 * Immutable value object representing a single buff instance applied to a unit.
 * Contains buff identification, type, duration, stacking rules, and effects.
 *
 * V3 extensions:
 * - BuffType enum for the 6 buff types
 * - instantHpBonus for one-time HP changes on acquisition
 * - withDecreasedDuration() for immutable duration decrement
 */
public class BuffInstance {

    private final String buffId;
    private final String sourceUnitId;  // nullable: unit that applied this buff
    private final BuffType type;        // V3: the buff type (POWER, LIFE, etc.)
    private final int duration;
    private final boolean stackable;
    private final BuffModifier modifiers;
    private final BuffFlags flags;
    private final int instantHpBonus;   // V3: one-time HP change on acquisition

    /**
     * V1/V2 backward-compatible constructor (no type, no instantHpBonus).
     */
    public BuffInstance(String buffId, String sourceUnitId, int duration, boolean stackable,
                        BuffModifier modifiers, BuffFlags flags) {
        this(buffId, sourceUnitId, null, duration, stackable, modifiers, flags, 0);
    }

    /**
     * V3 full constructor with all fields.
     */
    public BuffInstance(String buffId, String sourceUnitId, BuffType type, int duration, boolean stackable,
                        BuffModifier modifiers, BuffFlags flags, int instantHpBonus) {
        this.buffId = buffId;
        this.sourceUnitId = sourceUnitId;
        this.type = type;
        this.duration = duration;
        this.stackable = stackable;
        this.modifiers = modifiers;
        this.flags = flags;
        this.instantHpBonus = instantHpBonus;
    }

    public String getBuffId() {
        return buffId;
    }

    public String getSourceUnitId() {
        return sourceUnitId;
    }

    public BuffType getType() {
        return type;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isStackable() {
        return stackable;
    }

    public BuffModifier getModifiers() {
        return modifiers;
    }

    public BuffFlags getFlags() {
        return flags;
    }

    public int getInstantHpBonus() {
        return instantHpBonus;
    }

    /**
     * Create a new BuffInstance with duration decreased by 1.
     * Returns a new instance (immutable).
     */
    public BuffInstance withDecreasedDuration() {
        return new BuffInstance(buffId, sourceUnitId, type, duration - 1, stackable, modifiers, flags, instantHpBonus);
    }

    /**
     * Create a new BuffInstance with duration reset to the specified value.
     * Used for refreshing same-type buffs.
     */
    public BuffInstance withDuration(int newDuration) {
        return new BuffInstance(buffId, sourceUnitId, type, newDuration, stackable, modifiers, flags, instantHpBonus);
    }

    /**
     * Check if this buff has expired (duration <= 0).
     */
    public boolean isExpired() {
        return duration <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuffInstance that = (BuffInstance) o;
        return duration == that.duration &&
               stackable == that.stackable &&
               instantHpBonus == that.instantHpBonus &&
               Objects.equals(buffId, that.buffId) &&
               Objects.equals(sourceUnitId, that.sourceUnitId) &&
               type == that.type &&
               Objects.equals(modifiers, that.modifiers) &&
               Objects.equals(flags, that.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buffId, sourceUnitId, type, duration, stackable, modifiers, flags, instantHpBonus);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BuffInstance{buffId='").append(buffId).append("'");
        if (sourceUnitId != null) {
            sb.append(", sourceUnitId='").append(sourceUnitId).append("'");
        }
        if (type != null) {
            sb.append(", type=").append(type);
        }
        sb.append(", duration=").append(duration);
        sb.append(", stackable=").append(stackable);
        sb.append(", modifiers=").append(modifiers);
        sb.append(", flags=").append(flags);
        if (instantHpBonus != 0) {
            sb.append(", instantHpBonus=").append(instantHpBonus);
        }
        sb.append("}");
        return sb.toString();
    }
}
