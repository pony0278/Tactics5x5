package com.tactics.engine.skill;

import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffModifier;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.util.RngProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for hero-specific skill executors.
 * Contains shared helper methods used by all skill implementations.
 */
public abstract class SkillExecutorBase {

    protected RngProvider rngProvider;

    public SkillExecutorBase(RngProvider rngProvider) {
        this.rngProvider = rngProvider;
    }

    public void setRngProvider(RngProvider rngProvider) {
        this.rngProvider = rngProvider;
    }

    // =========================================================================
    // Functional Interface for Unit Transformation
    // =========================================================================

    @FunctionalInterface
    protected interface UnitTransformer {
        Unit transform(Unit unit);
    }

    // =========================================================================
    // Game Over Result
    // =========================================================================

    protected static class GameOverResult {
        final boolean isGameOver;
        final PlayerId winner;

        GameOverResult(boolean isGameOver, PlayerId winner) {
            this.isGameOver = isGameOver;
            this.winner = winner;
        }
    }

    // =========================================================================
    // Unit Helper Methods
    // =========================================================================

    protected Unit findUnitById(List<Unit> units, String unitId) {
        for (Unit u : units) {
            if (u.getId().equals(unitId)) {
                return u;
            }
        }
        return null;
    }

    protected List<Unit> updateUnitInList(List<Unit> units, String unitId, UnitTransformer transformer) {
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : units) {
            if (u.getId().equals(unitId)) {
                newUnits.add(transformer.transform(u));
            } else {
                newUnits.add(u);
            }
        }
        return newUnits;
    }

    protected List<Unit> updateUnitsInList(List<Unit> units, Map<String, UnitTransformer> transformers) {
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : units) {
            UnitTransformer transformer = transformers.get(u.getId());
            if (transformer != null) {
                newUnits.add(transformer.transform(u));
            } else {
                newUnits.add(u);
            }
        }
        return newUnits;
    }

    // =========================================================================
    // Position Helper Methods
    // =========================================================================

    protected boolean isAdjacent(Position a, Position b) {
        int dx = b.getX() - a.getX();
        int dy = b.getY() - a.getY();
        return (dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1));
    }

    protected boolean isInBounds(Position pos, Board board) {
        return pos.getX() >= 0 && pos.getX() < board.getWidth() &&
               pos.getY() >= 0 && pos.getY() < board.getHeight();
    }

    protected boolean isTileOccupied(List<Unit> units, Position pos) {
        for (Unit u : units) {
            if (u.isAlive() && u.getPosition().getX() == pos.getX() &&
                u.getPosition().getY() == pos.getY()) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasObstacleAt(GameState state, Position pos) {
        return state.hasObstacleAt(pos);
    }

    protected boolean isTileBlocked(GameState state, Position pos) {
        return isTileOccupied(state.getUnits(), pos) || hasObstacleAt(state, pos);
    }

    // =========================================================================
    // Game Over Check
    // =========================================================================

    protected GameOverResult checkGameOver(List<Unit> units) {
        return checkGameOver(units, null);
    }

    protected GameOverResult checkGameOver(List<Unit> units, PlayerId activePlayer) {
        boolean p1HasAlive = false;
        boolean p2HasAlive = false;

        for (Unit u : units) {
            if (u.isAlive()) {
                if (u.getOwner().isPlayer1()) {
                    p1HasAlive = true;
                } else {
                    p2HasAlive = true;
                }
            }
        }

        // V3: Simultaneous death - active player wins
        if (!p1HasAlive && !p2HasAlive) {
            if (activePlayer != null) {
                return new GameOverResult(true, activePlayer);
            }
            return new GameOverResult(true, PlayerId.PLAYER_1);
        }

        if (!p1HasAlive) {
            return new GameOverResult(true, PlayerId.PLAYER_2);
        } else if (!p2HasAlive) {
            return new GameOverResult(true, PlayerId.PLAYER_1);
        }
        return new GameOverResult(false, null);
    }

    // =========================================================================
    // Guardian Helper
    // =========================================================================

    /**
     * Find the Guardian (TANK) that will intercept damage for the target unit.
     */
    protected Unit findGuardian(GameState state, Unit target) {
        if (target == null) {
            return null;
        }

        Unit guardian = null;

        for (Unit u : state.getUnits()) {
            if (!u.isAlive()) {
                continue;
            }
            if (!u.getOwner().getValue().equals(target.getOwner().getValue())) {
                continue;
            }
            if (u.getMinionType() != MinionType.TANK) {
                continue;
            }
            if (u.getId().equals(target.getId())) {
                continue;
            }
            if (!isAdjacent(u.getPosition(), target.getPosition())) {
                continue;
            }

            if (guardian == null || u.getId().compareTo(guardian.getId()) < 0) {
                guardian = u;
            }
        }

        return guardian;
    }

    // =========================================================================
    // Buff Helper Methods
    // =========================================================================

    protected Map<String, List<BuffInstance>> removeBleedBuffs(Map<String, List<BuffInstance>> unitBuffs, String unitId) {
        if (unitBuffs == null) {
            return unitBuffs;
        }

        List<BuffInstance> buffs = unitBuffs.get(unitId);
        if (buffs == null || buffs.isEmpty()) {
            return unitBuffs;
        }

        List<BuffInstance> remainingBuffs = new ArrayList<>();
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() == null || !buff.getFlags().isBleedBuff()) {
                remainingBuffs.add(buff);
            }
        }

        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(unitBuffs);
        if (remainingBuffs.isEmpty()) {
            newUnitBuffs.remove(unitId);
        } else {
            newUnitBuffs.put(unitId, remainingBuffs);
        }

        return newUnitBuffs;
    }

    protected boolean isDebuff(BuffInstance buff) {
        if (buff.getFlags() == null) {
            return false;
        }
        return buff.getFlags().isBleedBuff() ||
               buff.getFlags().isSlowBuff() ||
               buff.getFlags().isStunned() ||
               buff.getFlags().isRooted() ||
               (buff.getModifiers() != null && buff.getModifiers().getBonusAttack() < 0);
    }

    protected Map<String, List<BuffInstance>> removeOneRandomDebuff(Map<String, List<BuffInstance>> unitBuffs, String unitId) {
        if (unitBuffs == null) {
            return new HashMap<>();
        }

        List<BuffInstance> buffs = unitBuffs.get(unitId);
        if (buffs == null || buffs.isEmpty()) {
            return new HashMap<>(unitBuffs);
        }

        List<BuffInstance> debuffs = new ArrayList<>();
        for (BuffInstance buff : buffs) {
            if (isDebuff(buff)) {
                debuffs.add(buff);
            }
        }

        if (debuffs.isEmpty()) {
            return new HashMap<>(unitBuffs);
        }

        BuffInstance toRemove = debuffs.get(rngProvider.nextInt(debuffs.size()));

        List<BuffInstance> remainingBuffs = new ArrayList<>();
        boolean removed = false;
        for (BuffInstance buff : buffs) {
            if (!removed && buff.getBuffId().equals(toRemove.getBuffId())) {
                removed = true;
                continue;
            }
            remainingBuffs.add(buff);
        }

        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(unitBuffs);
        if (remainingBuffs.isEmpty()) {
            newUnitBuffs.remove(unitId);
        } else {
            newUnitBuffs.put(unitId, remainingBuffs);
        }

        return newUnitBuffs;
    }

    /**
     * Create a custom ATK buff for Power of Many (no instant HP bonus).
     */
    protected BuffInstance createAtkBuff(String sourceUnitId, int bonusAtk, int duration) {
        return new BuffInstance(
            "power_of_many_" + System.currentTimeMillis(),
            sourceUnitId,
            duration,
            true,
            new BuffModifier(0, bonusAtk, 0, 0),  // bonusHp, bonusAttack, bonusMoveRange, bonusAttackRange
            null
        );
    }

    /**
     * Apply a buff to a unit and return updated buff map.
     */
    protected Map<String, List<BuffInstance>> addBuffToUnit(
            Map<String, List<BuffInstance>> unitBuffs,
            String unitId,
            BuffInstance buff) {
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(unitBuffs);
        List<BuffInstance> buffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(unitId, Collections.emptyList())
        );
        buffs.add(buff);
        newUnitBuffs.put(unitId, buffs);
        return newUnitBuffs;
    }
}
