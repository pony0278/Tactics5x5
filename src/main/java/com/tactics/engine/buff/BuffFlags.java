package com.tactics.engine.buff;

import java.util.Objects;

/**
 * Immutable value object representing behavioral flags from a buff.
 *
 * V1 flags: stunned, rooted, poison, silenced, taunted
 * V3 flags: powerBuff, speedBuff, slowBuff, bleedBuff
 * Phase 4D flags: deathMarkBuff, feintBuff, challengeBuff, invulnerableBuff
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

    // Phase 4D flags
    private final boolean deathMarkBuff;     // +2 damage taken, heal source on kill
    private final boolean feintBuff;         // Dodge next attack, counter 2 damage
    private final boolean challengeBuff;     // Taunt: 50% damage to others, counter on attack
    private final boolean invulnerableBuff;  // Cannot take damage

    /**
     * V1/V2 backward-compatible constructor.
     */
    public BuffFlags(boolean stunned, boolean rooted, boolean poison, boolean silenced, boolean taunted) {
        this(stunned, rooted, poison, silenced, taunted, false, false, false, false, false, false,
             false, false, false, false);
    }

    /**
     * V3 constructor without lifeBuff (backward compatibility).
     */
    public BuffFlags(boolean stunned, boolean rooted, boolean poison, boolean silenced, boolean taunted,
                     boolean powerBuff, boolean speedBuff, boolean slowBuff, boolean bleedBuff) {
        this(stunned, rooted, poison, silenced, taunted, powerBuff, speedBuff, slowBuff, bleedBuff, false, false,
             false, false, false, false);
    }

    /**
     * V3 constructor with lifeBuff (backward compatibility).
     */
    public BuffFlags(boolean stunned, boolean rooted, boolean poison, boolean silenced, boolean taunted,
                     boolean powerBuff, boolean speedBuff, boolean slowBuff, boolean bleedBuff, boolean lifeBuff) {
        this(stunned, rooted, poison, silenced, taunted, powerBuff, speedBuff, slowBuff, bleedBuff, lifeBuff, false,
             false, false, false, false);
    }

    /**
     * V3 constructor with blindBuff (backward compatibility).
     */
    public BuffFlags(boolean stunned, boolean rooted, boolean poison, boolean silenced, boolean taunted,
                     boolean powerBuff, boolean speedBuff, boolean slowBuff, boolean bleedBuff, boolean lifeBuff,
                     boolean blindBuff) {
        this(stunned, rooted, poison, silenced, taunted, powerBuff, speedBuff, slowBuff, bleedBuff, lifeBuff, blindBuff,
             false, false, false, false);
    }

    /**
     * Phase 4D full constructor with all flags.
     */
    public BuffFlags(boolean stunned, boolean rooted, boolean poison, boolean silenced, boolean taunted,
                     boolean powerBuff, boolean speedBuff, boolean slowBuff, boolean bleedBuff, boolean lifeBuff,
                     boolean blindBuff, boolean deathMarkBuff, boolean feintBuff, boolean challengeBuff,
                     boolean invulnerableBuff) {
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
        this.deathMarkBuff = deathMarkBuff;
        this.feintBuff = feintBuff;
        this.challengeBuff = challengeBuff;
        this.invulnerableBuff = invulnerableBuff;
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

    // Phase 4D getters

    public boolean isDeathMarkBuff() {
        return deathMarkBuff;
    }

    public boolean isFeintBuff() {
        return feintBuff;
    }

    public boolean isChallengeBuff() {
        return challengeBuff;
    }

    public boolean isInvulnerableBuff() {
        return invulnerableBuff;
    }

    /**
     * Create a new BuffFlags with all flags set to false.
     */
    public static BuffFlags none() {
        return new BuffFlags(false, false, false, false, false, false, false, false, false, false, false,
                            false, false, false, false);
    }

    /**
     * Create a BuffFlags with only powerBuff set.
     */
    public static BuffFlags power() {
        return new BuffFlags(false, false, false, false, false, true, false, false, false, false, false,
                            false, false, false, false);
    }

    /**
     * Create a BuffFlags with only speedBuff set.
     */
    public static BuffFlags speed() {
        return new BuffFlags(false, false, false, false, false, false, true, false, false, false, false,
                            false, false, false, false);
    }

    /**
     * Create a BuffFlags with only slowBuff set.
     */
    public static BuffFlags slow() {
        return new BuffFlags(false, false, false, false, false, false, false, true, false, false, false,
                            false, false, false, false);
    }

    /**
     * Create a BuffFlags with only bleedBuff set.
     */
    public static BuffFlags bleed() {
        return new BuffFlags(false, false, false, false, false, false, false, false, true, false, false,
                            false, false, false, false);
    }

    /**
     * Create a BuffFlags with only lifeBuff set.
     */
    public static BuffFlags life() {
        return new BuffFlags(false, false, false, false, false, false, false, false, false, true, false,
                            false, false, false, false);
    }

    /**
     * Create a BuffFlags with only blindBuff set.
     */
    public static BuffFlags blind() {
        return new BuffFlags(false, false, false, false, false, false, false, false, false, false, true,
                            false, false, false, false);
    }

    /**
     * Create a BuffFlags with only stunned set.
     */
    public static BuffFlags stunned() {
        return new BuffFlags(true, false, false, false, false, false, false, false, false, false, false,
                            false, false, false, false);
    }

    /**
     * Create a BuffFlags with only rooted set.
     */
    public static BuffFlags rooted() {
        return new BuffFlags(false, true, false, false, false, false, false, false, false, false, false,
                            false, false, false, false);
    }

    /**
     * Create a BuffFlags with only poison set.
     */
    public static BuffFlags poison() {
        return new BuffFlags(false, false, true, false, false, false, false, false, false, false, false,
                            false, false, false, false);
    }

    /**
     * Create a BuffFlags with only deathMarkBuff set.
     */
    public static BuffFlags deathMark() {
        return new BuffFlags(false, false, false, false, false, false, false, false, false, false, false,
                            true, false, false, false);
    }

    /**
     * Create a BuffFlags with only feintBuff set.
     */
    public static BuffFlags feint() {
        return new BuffFlags(false, false, false, false, false, false, false, false, false, false, false,
                            false, true, false, false);
    }

    /**
     * Create a BuffFlags with only challengeBuff set.
     */
    public static BuffFlags challenge() {
        return new BuffFlags(false, false, false, false, false, false, false, false, false, false, false,
                            false, false, true, false);
    }

    /**
     * Create a BuffFlags with only invulnerableBuff set.
     */
    public static BuffFlags invulnerable() {
        return new BuffFlags(false, false, false, false, false, false, false, false, false, false, false,
                            false, false, false, true);
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
               blindBuff == buffFlags.blindBuff &&
               deathMarkBuff == buffFlags.deathMarkBuff &&
               feintBuff == buffFlags.feintBuff &&
               challengeBuff == buffFlags.challengeBuff &&
               invulnerableBuff == buffFlags.invulnerableBuff;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stunned, rooted, poison, silenced, taunted,
                           powerBuff, speedBuff, slowBuff, bleedBuff, lifeBuff, blindBuff,
                           deathMarkBuff, feintBuff, challengeBuff, invulnerableBuff);
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
        if (blindBuff) { if (!first) sb.append(", "); sb.append("blindBuff"); first = false; }
        if (deathMarkBuff) { if (!first) sb.append(", "); sb.append("deathMarkBuff"); first = false; }
        if (feintBuff) { if (!first) sb.append(", "); sb.append("feintBuff"); first = false; }
        if (challengeBuff) { if (!first) sb.append(", "); sb.append("challengeBuff"); first = false; }
        if (invulnerableBuff) { if (!first) sb.append(", "); sb.append("invulnerableBuff"); }

        sb.append("}");
        return sb.toString();
    }
}
