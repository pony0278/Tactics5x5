package com.tactics.engine.buff;

import java.util.Objects;

/**
 * Immutable value object representing a single buff instance applied to a unit.
 * Contains buff identification, duration, stacking rules, and effects.
 */
public class BuffInstance {

    private final String buffId;
    private final String sourceUnitId;  // nullable: unit that applied this buff
    private final int duration;
    private final boolean stackable;
    private final BuffModifier modifiers;
    private final BuffFlags flags;

    public BuffInstance(String buffId, String sourceUnitId, int duration, boolean stackable,
                        BuffModifier modifiers, BuffFlags flags) {
        this.buffId = buffId;
        this.sourceUnitId = sourceUnitId;
        this.duration = duration;
        this.stackable = stackable;
        this.modifiers = modifiers;
        this.flags = flags;
    }

    public String getBuffId() {
        return buffId;
    }

    public String getSourceUnitId() {
        return sourceUnitId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuffInstance that = (BuffInstance) o;
        return duration == that.duration &&
               stackable == that.stackable &&
               Objects.equals(buffId, that.buffId) &&
               Objects.equals(sourceUnitId, that.sourceUnitId) &&
               Objects.equals(modifiers, that.modifiers) &&
               Objects.equals(flags, that.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buffId, sourceUnitId, duration, stackable, modifiers, flags);
    }

    @Override
    public String toString() {
        return "BuffInstance{" +
               "buffId='" + buffId + '\'' +
               ", sourceUnitId='" + sourceUnitId + '\'' +
               ", duration=" + duration +
               ", stackable=" + stackable +
               ", modifiers=" + modifiers +
               ", flags=" + flags +
               '}';
    }
}
