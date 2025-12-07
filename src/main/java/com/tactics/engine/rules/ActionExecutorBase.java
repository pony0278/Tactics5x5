package com.tactics.engine.rules;

import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.util.RngProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tactics.engine.rules.RuleEngineHelper.*;

/**
 * Base class with shared helper methods for action executors.
 * Extracted from ActionExecutor for better code organization.
 */
public class ActionExecutorBase {

    protected RngProvider rngProvider;

    public ActionExecutorBase() {
        this.rngProvider = new RngProvider();
    }

    public void setRngProvider(RngProvider rngProvider) {
        this.rngProvider = rngProvider;
    }

    // =========================================================================
    // Unit Transformer Interface
    // =========================================================================

    @FunctionalInterface
    protected interface UnitTransformer {
        Unit transform(Unit unit);
    }

    // =========================================================================
    // Position Helper Methods
    // =========================================================================

    protected boolean isAdjacent(Position a, Position b) {
        int dx = b.getX() - a.getX();
        int dy = b.getY() - a.getY();
        return (dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1));
    }

    protected int manhattanDistance(Position a, Position b) {
        int dx = Math.abs(b.getX() - a.getX());
        int dy = Math.abs(b.getY() - a.getY());
        return dx + dy;
    }

    protected boolean isOrthogonal(Position a, Position b) {
        int dx = Math.abs(b.getX() - a.getX());
        int dy = Math.abs(b.getY() - a.getY());
        return (dx == 0 && dy > 0) || (dx > 0 && dy == 0);
    }

    protected boolean canMoveToPositionWithBuffs(Unit unit, Position target, int effectiveMoveRange) {
        Position from = unit.getPosition();
        if (!isOrthogonal(from, target)) {
            return false;
        }
        int distance = manhattanDistance(from, target);
        return distance >= 1 && distance <= effectiveMoveRange;
    }

    protected boolean canAttackFromPositionWithBuffs(Position attackerPos, Position targetPos, int effectiveAttackRange) {
        if (!isOrthogonal(attackerPos, targetPos)) {
            return false;
        }
        int distance = manhattanDistance(attackerPos, targetPos);
        return distance >= 1 && distance <= effectiveAttackRange;
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
    // Player/Turn Helper Methods
    // =========================================================================

    protected PlayerId getNextPlayer(PlayerId current) {
        return current.isPlayer1() ? PlayerId.PLAYER_2 : PlayerId.PLAYER_1;
    }

    protected PlayerId getNextActingPlayer(GameState state, PlayerId currentActingPlayer) {
        PlayerId opponent = getNextPlayer(currentActingPlayer);

        boolean opponentHasUnusedUnits = hasUnusedUnits(state, opponent);
        if (opponentHasUnusedUnits) {
            return opponent;
        }

        boolean currentHasUnusedUnits = hasUnusedUnits(state, currentActingPlayer);
        if (currentHasUnusedUnits) {
            return currentActingPlayer;
        }

        return opponent;
    }

    protected boolean hasUnusedUnits(GameState state, PlayerId player) {
        for (Unit u : state.getUnits()) {
            if (u.isAlive() &&
                u.getOwner().getValue().equals(player.getValue()) &&
                u.getActionsUsed() == 0) {
                return true;
            }
        }
        return false;
    }

    protected boolean allUnitsActed(GameState state) {
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getActionsUsed() == 0) {
                return false;
            }
        }
        return true;
    }

    protected int getMaxActions(List<BuffInstance> buffs) {
        return getMaxActionsForUnit(buffs);
    }

    /**
     * Check if turn should switch after an action.
     * SPEED buff units don't switch turn until they've used all actions.
     */
    protected boolean shouldSwitchTurnAfterAction(GameState state, Unit actingUnit) {
        List<BuffInstance> buffs = getBuffsForUnit(state, actingUnit.getId());
        int maxActions = getMaxActions(buffs);
        return actingUnit.getActionsUsed() >= maxActions;
    }

    // =========================================================================
    // Unit List Update Methods
    // =========================================================================

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
    // Buff Helper Methods
    // =========================================================================

    protected int getEffectiveMoveRange(Unit unit, List<BuffInstance> buffs) {
        int bonus = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getModifiers() != null) {
                bonus += buff.getModifiers().getBonusMoveRange();
            }
        }
        return unit.getMoveRange() + bonus;
    }

    protected int getEffectiveAttackRange(Unit unit, List<BuffInstance> buffs) {
        int bonus = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getModifiers() != null) {
                bonus += buff.getModifiers().getBonusAttackRange();
            }
        }
        return unit.getAttackRange() + bonus;
    }

    protected int getBonusAttack(List<BuffInstance> buffs) {
        int bonus = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getModifiers() != null) {
                bonus += buff.getModifiers().getBonusAttack();
            }
        }
        return bonus;
    }

    protected int getPoisonDamage(List<BuffInstance> buffs) {
        int count = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isPoison()) {
                count++;
            }
        }
        return count;
    }

    protected int getBleedDamage(List<BuffInstance> buffs) {
        int count = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isBleedBuff()) {
                count++;
            }
        }
        return count;
    }

    protected BuffType getRandomBuffType() {
        int roll = rngProvider.nextInt(6);
        BuffType[] types = BuffType.values();
        return types[roll];
    }

    // =========================================================================
    // Guardian Helper
    // =========================================================================

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
    // Action Serialization (for SLOW buff preparing)
    // =========================================================================

    protected Map<String, Object> serializeActionForPreparing(com.tactics.engine.action.Action action) {
        Map<String, Object> actionMap = new HashMap<>();
        actionMap.put("type", action.getType().name());
        actionMap.put("playerId", action.getPlayerId().getValue());
        if (action.getTargetPosition() != null) {
            Map<String, Object> posMap = new HashMap<>();
            posMap.put("x", action.getTargetPosition().getX());
            posMap.put("y", action.getTargetPosition().getY());
            actionMap.put("targetPosition", posMap);
        }
        if (action.getTargetUnitId() != null) {
            actionMap.put("targetUnitId", action.getTargetUnitId());
        }
        if (action.getActingUnitId() != null) {
            actionMap.put("actingUnitId", action.getActingUnitId());
        }
        return actionMap;
    }
}
