package com.tactics.engine.buff;

import java.util.Objects;

/**
 * Immutable value object representing numeric stat modifiers from a buff.
 * Applied on top of base unit stats.
 */
public class BuffModifier {

    private final int bonusHp;
    private final int bonusAttack;
    private final int bonusMoveRange;
    private final int bonusAttackRange;

    public BuffModifier(int bonusHp, int bonusAttack, int bonusMoveRange, int bonusAttackRange) {
        this.bonusHp = bonusHp;
        this.bonusAttack = bonusAttack;
        this.bonusMoveRange = bonusMoveRange;
        this.bonusAttackRange = bonusAttackRange;
    }

    public int getBonusHp() {
        return bonusHp;
    }

    public int getBonusAttack() {
        return bonusAttack;
    }

    public int getBonusMoveRange() {
        return bonusMoveRange;
    }

    public int getBonusAttackRange() {
        return bonusAttackRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuffModifier that = (BuffModifier) o;
        return bonusHp == that.bonusHp &&
               bonusAttack == that.bonusAttack &&
               bonusMoveRange == that.bonusMoveRange &&
               bonusAttackRange == that.bonusAttackRange;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bonusHp, bonusAttack, bonusMoveRange, bonusAttackRange);
    }

    @Override
    public String toString() {
        return "BuffModifier{" +
               "bonusHp=" + bonusHp +
               ", bonusAttack=" + bonusAttack +
               ", bonusMoveRange=" + bonusMoveRange +
               ", bonusAttackRange=" + bonusAttackRange +
               '}';
    }
}
