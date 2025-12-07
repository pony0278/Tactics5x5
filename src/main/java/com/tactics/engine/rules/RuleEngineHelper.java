package com.tactics.engine.rules;

import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Unit;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Helper class containing shared utility methods used by ActionValidator and ActionExecutor.
 * Consolidates duplicate code to improve maintainability.
 */
public class RuleEngineHelper {

    // =========================================================================
    // Unit Lookup Methods
    // =========================================================================

    /**
     * Find a unit by its ID in a list of units.
     * @param units List of units to search
     * @param unitId ID of the unit to find
     * @return The unit if found, null otherwise
     */
    public static Unit findUnitById(List<Unit> units, String unitId) {
        for (Unit u : units) {
            if (u.getId().equals(unitId)) {
                return u;
            }
        }
        return null;
    }

    // =========================================================================
    // Buff Retrieval Methods
    // =========================================================================

    /**
     * Get all buffs for a specific unit from the game state.
     * @param state Current game state
     * @param unitId ID of the unit
     * @return List of buffs for the unit, or empty list if none
     */
    public static List<BuffInstance> getBuffsForUnit(GameState state, String unitId) {
        Map<String, List<BuffInstance>> unitBuffs = state.getUnitBuffs();
        if (unitBuffs == null) {
            return Collections.emptyList();
        }
        List<BuffInstance> buffs = unitBuffs.get(unitId);
        return buffs != null ? buffs : Collections.emptyList();
    }

    // =========================================================================
    // Buff Check Methods
    // =========================================================================

    /**
     * Check if the unit has a SPEED buff.
     * @param buffs List of buffs to check
     * @return true if SPEED buff is present
     */
    public static boolean hasSpeedBuff(List<BuffInstance> buffs) {
        if (buffs == null) return false;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isSpeedBuff()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the unit has a POWER buff.
     * @param buffs List of buffs to check
     * @return true if POWER buff is present
     */
    public static boolean hasPowerBuff(List<BuffInstance> buffs) {
        if (buffs == null) return false;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isPowerBuff()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the unit has a SLOW buff.
     * @param buffs List of buffs to check
     * @return true if SLOW buff is present
     */
    public static boolean hasSlowBuff(List<BuffInstance> buffs) {
        if (buffs == null) return false;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isSlowBuff()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the unit has a LIFE buff.
     * @param buffs List of buffs to check
     * @return true if LIFE buff is present
     */
    public static boolean hasLifeBuff(List<BuffInstance> buffs) {
        if (buffs == null) return false;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isLifeBuff()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the unit has a BLEED buff.
     * @param buffs List of buffs to check
     * @return true if BLEED buff is present
     */
    public static boolean hasBleedBuff(List<BuffInstance> buffs) {
        if (buffs == null) return false;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isBleedBuff()) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // Action Calculation Methods
    // =========================================================================

    /**
     * Get the maximum number of actions a unit can take based on buffs.
     * SPEED buff grants 2 actions per turn.
     * @param buffs List of buffs to check
     * @return Maximum number of actions (1 normally, 2 with SPEED)
     */
    public static int getMaxActionsForUnit(List<BuffInstance> buffs) {
        return hasSpeedBuff(buffs) ? 2 : 1;
    }
}
