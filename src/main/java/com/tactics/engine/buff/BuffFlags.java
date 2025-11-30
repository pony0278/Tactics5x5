package com.tactics.engine.buff;

import java.util.Objects;

/**
 * Immutable value object representing behavioral flags from a buff.
 * V1 only requires: stunned, rooted, poison.
 * silenced and taunted are included for forward compatibility but unused in V1.
 */
public class BuffFlags {

    private final boolean stunned;
    private final boolean rooted;
    private final boolean poison;
    private final boolean silenced;
    private final boolean taunted;

    public BuffFlags(boolean stunned, boolean rooted, boolean poison, boolean silenced, boolean taunted) {
        this.stunned = stunned;
        this.rooted = rooted;
        this.poison = poison;
        this.silenced = silenced;
        this.taunted = taunted;
    }

    public boolean isStunned() {
        return stunned;
    }

    public boolean isRooted() {
        return rooted;
    }

    public boolean isPoison() {
        return poison;
    }

    public boolean isSilenced() {
        return silenced;
    }

    public boolean isTaunted() {
        return taunted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuffFlags buffFlags = (BuffFlags) o;
        return stunned == buffFlags.stunned &&
               rooted == buffFlags.rooted &&
               poison == buffFlags.poison &&
               silenced == buffFlags.silenced &&
               taunted == buffFlags.taunted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stunned, rooted, poison, silenced, taunted);
    }

    @Override
    public String toString() {
        return "BuffFlags{" +
               "stunned=" + stunned +
               ", rooted=" + rooted +
               ", poison=" + poison +
               ", silenced=" + silenced +
               ", taunted=" + taunted +
               '}';
    }
}
