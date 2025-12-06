package com.tactics.engine.buff;

import java.util.Objects;

/**
 * Immutable value object representing behavioral flags from a buff.
 *
 * V1 flags: stunned, rooted, poison, silenced, taunted
 * V3 flags: powerBuff, speedBuff, slowBuff, bleedBuff
 */
public class BuffFlags {

    // V1 flags
    private final boolean stunned;
    private final boolean rooted;
    private final boolean poison;
    private final boolean silenced;
    private final boolean taunted;

    // V3 flags
    private final boolean powerBuff;   // Blocks MOVE_AND_ATTACK, instant obstacle destroy
    private final boolean speedBuff;   // Grants extra action per round
    private final boolean slowBuff;    // Delays actions by 1 round
    private final boolean bleedBuff;   // -1 HP per round at round end
    private final boolean lifeBuff;    // +3 HP instant (from Trinity, Nature's Power)
    private final boolean blindBuff;   // Cannot attack for 1 round (from Smoke Bomb)

    /**
     * V1/V2 backward-compatible constructor.
     */
    public BuffFlags(boolean stunned, boolean rooted, boolean poison, boolean silenced, boolean taunted) {
        this(stunned, rooted, poison, silenced, taunted, false, false, false, false, false, false);
    }

    /**
     * V3 constructor without lifeBuff (backward compatibility).
     */
    public BuffFlags(boolean stunned, boolean rooted, boolean poison, boolean silenced, boolean taunted,
                     boolean powerBuff, boolean speedBuff, boolean slowBuff, boolean bleedBuff) {
        this(stunned, rooted, poison, silenced, taunted, powerBuff, speedBuff, slowBuff, bleedBuff, false, false);
    }

    /**
     * V3 constructor with lifeBuff (backward compatibility).
     */
    public BuffFlags(boolean stunned, boolean rooted, boolean poison, boolean silenced, boolean taunted,
                     boolean powerBuff, boolean speedBuff, boolean slowBuff, boolean bleedBuff, boolean lifeBuff) {
        this(stunned, rooted, poison, silenced, taunted, powerBuff, speedBuff, slowBuff, bleedBuff, lifeBuff, false);
    }

    /**
     * V3 full constructor with all flags including blindBuff.
     */
    public BuffFlags(boolean stunned, boolean rooted, boolean poison, boolean silenced, boolean taunted,
                     boolean powerBuff, boolean speedBuff, boolean slowBuff, boolean bleedBuff, boolean lifeBuff,
                     boolean blindBuff) {
        this.stunned = stunned;
        this.rooted = rooted;
        this.poison = poison;
        this.silenced = silenced;
        this.taunted = taunted;
        this.powerBuff = powerBuff;
        this.speedBuff = speedBuff;
        this.slowBuff = slowBuff;
        this.bleedBuff = bleedBuff;
        this.lifeBuff = lifeBuff;
        this.blindBuff = blindBuff;
    }

    // V1 getters

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

    // V3 getters

    public boolean isPowerBuff() {
        return powerBuff;
    }

    public boolean isSpeedBuff() {
        return speedBuff;
    }

    public boolean isSlowBuff() {
        return slowBuff;
    }

    public boolean isBleedBuff() {
        return bleedBuff;
    }

    public boolean isLifeBuff() {
        return lifeBuff;
    }

    public boolean isBlindBuff() {
        return blindBuff;
    }

    /**
     * Create a new BuffFlags with all flags set to false.
     */
    public static BuffFlags none() {
        return new BuffFlags(false, false, false, false, false, false, false, false, false, false, false);
    }

    /**
     * Create a BuffFlags with only powerBuff set.
     */
    public static BuffFlags power() {
        return new BuffFlags(false, false, false, false, false, true, false, false, false, false, false);
    }

    /**
     * Create a BuffFlags with only speedBuff set.
     */
    public static BuffFlags speed() {
        return new BuffFlags(false, false, false, false, false, false, true, false, false, false, false);
    }

    /**
     * Create a BuffFlags with only slowBuff set.
     */
    public static BuffFlags slow() {
        return new BuffFlags(false, false, false, false, false, false, false, true, false, false, false);
    }

    /**
     * Create a BuffFlags with only bleedBuff set.
     */
    public static BuffFlags bleed() {
        return new BuffFlags(false, false, false, false, false, false, false, false, true, false, false);
    }

    /**
     * Create a BuffFlags with only lifeBuff set.
     */
    public static BuffFlags life() {
        return new BuffFlags(false, false, false, false, false, false, false, false, false, true, false);
    }

    /**
     * Create a BuffFlags with only blindBuff set.
     */
    public static BuffFlags blind() {
        return new BuffFlags(false, false, false, false, false, false, false, false, false, false, true);
    }

    /**
     * Create a BuffFlags with only stunned set.
     */
    public static BuffFlags stunned() {
        return new BuffFlags(true, false, false, false, false, false, false, false, false, false, false);
    }

    /**
     * Create a BuffFlags with only rooted set.
     */
    public static BuffFlags rooted() {
        return new BuffFlags(false, true, false, false, false, false, false, false, false, false, false);
    }

    /**
     * Create a BuffFlags with only poison set.
     */
    public static BuffFlags poison() {
        return new BuffFlags(false, false, true, false, false, false, false, false, false, false, false);
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
               taunted == buffFlags.taunted &&
               powerBuff == buffFlags.powerBuff &&
               speedBuff == buffFlags.speedBuff &&
               slowBuff == buffFlags.slowBuff &&
               bleedBuff == buffFlags.bleedBuff &&
               lifeBuff == buffFlags.lifeBuff &&
               blindBuff == buffFlags.blindBuff;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stunned, rooted, poison, silenced, taunted,
                           powerBuff, speedBuff, slowBuff, bleedBuff, lifeBuff, blindBuff);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BuffFlags{");
        boolean first = true;

        if (stunned) { sb.append("stunned"); first = false; }
        if (rooted) { if (!first) sb.append(", "); sb.append("rooted"); first = false; }
        if (poison) { if (!first) sb.append(", "); sb.append("poison"); first = false; }
        if (silenced) { if (!first) sb.append(", "); sb.append("silenced"); first = false; }
        if (taunted) { if (!first) sb.append(", "); sb.append("taunted"); first = false; }
        if (powerBuff) { if (!first) sb.append(", "); sb.append("powerBuff"); first = false; }
        if (speedBuff) { if (!first) sb.append(", "); sb.append("speedBuff"); first = false; }
        if (slowBuff) { if (!first) sb.append(", "); sb.append("slowBuff"); first = false; }
        if (bleedBuff) { if (!first) sb.append(", "); sb.append("bleedBuff"); first = false; }
        if (lifeBuff) { if (!first) sb.append(", "); sb.append("lifeBuff"); first = false; }
        if (blindBuff) { if (!first) sb.append(", "); sb.append("blindBuff"); }

        sb.append("}");
        return sb.toString();
    }
}
