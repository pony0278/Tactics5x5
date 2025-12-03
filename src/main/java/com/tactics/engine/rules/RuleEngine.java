package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffFlags;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffModifier;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
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
 * Validates and applies actions.
 *
 * V2 Update: Validation now uses unit.moveRange and unit.attackRange
 * for distance checks instead of fixed distance=1.
 *
 * V3 (Buff System V1): Validation and apply now consider buff effects:
 * - stunned: cannot MOVE, ATTACK, MOVE_AND_ATTACK (but can END_TURN)
 * - rooted: cannot MOVE or MOVE_AND_ATTACK movement step
 * - bonusMoveRange: increases effective move range
 * - bonusAttackRange: increases effective attack range
 * - bonusAttack: increases damage dealt
 * - poison: deals 1 damage per poison buff at turn end
 * - Turn end: decrements buff durations and removes expired buffs
 * - V3: SPEED buff allows 2 actions per turn
 * - V3: SLOW buff delays actions by 1 round
 * - V3: BLEED buff deals 1 damage per round
 * - V3: Buff tiles trigger when stepped on
 */
public class RuleEngine {

    // V3: RngProvider for buff tile randomness
    private RngProvider rngProvider;

    public RuleEngine() {
        this.rngProvider = new RngProvider();  // Default with time-based seed
    }

    /**
     * V3: Set the RngProvider for deterministic buff tile triggers.
     */
    public void setRngProvider(RngProvider rngProvider) {
        this.rngProvider = rngProvider;
    }

    /**
     * V3: Get the RngProvider.
     */
    public RngProvider getRngProvider() {
        return rngProvider;
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Check if two positions are orthogonally adjacent (exactly 1 tile apart).
     * Used by applyAction methods for V1-compatible unit finding.
     */
    private boolean isAdjacent(Position a, Position b) {
        int dx = b.getX() - a.getX();
        int dy = b.getY() - a.getY();
        return (dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1));
    }

    /**
     * Calculate Manhattan distance between two positions.
     * distance = abs(x1 - x2) + abs(y1 - y2)
     */
    private int manhattanDistance(Position a, Position b) {
        int dx = Math.abs(b.getX() - a.getX());
        int dy = Math.abs(b.getY() - a.getY());
        return dx + dy;
    }

    /**
     * Check if movement between two positions is orthogonal (horizontal or vertical only).
     * Returns true if the positions differ by only one axis (not diagonal, not same position).
     * Orthogonal: (dx == 0 && dy != 0) || (dx != 0 && dy == 0)
     */
    private boolean isOrthogonal(Position a, Position b) {
        int dx = Math.abs(b.getX() - a.getX());
        int dy = Math.abs(b.getY() - a.getY());
        // Must move in exactly one direction (horizontal XOR vertical)
        return (dx == 0 && dy > 0) || (dx > 0 && dy == 0);
    }

    /**
     * Check if a unit can move from its current position to target within its moveRange.
     * Movement must be orthogonal and within range: 1 <= distance <= moveRange
     */
    private boolean canMoveToPosition(Unit unit, Position target) {
        Position from = unit.getPosition();
        if (!isOrthogonal(from, target)) {
            return false;
        }
        int distance = manhattanDistance(from, target);
        return distance >= 1 && distance <= unit.getMoveRange();
    }

    /**
     * Check if a unit can move to target with effective (buff-modified) move range.
     */
    private boolean canMoveToPositionWithBuffs(Unit unit, Position target, int effectiveMoveRange) {
        Position from = unit.getPosition();
        if (!isOrthogonal(from, target)) {
            return false;
        }
        int distance = manhattanDistance(from, target);
        return distance >= 1 && distance <= effectiveMoveRange;
    }

    /**
     * Check if a unit can attack a target position from a given attacker position within its attackRange.
     * Attack must be orthogonal and within range: 1 <= distance <= attackRange
     */
    private boolean canAttackFromPosition(Unit unit, Position attackerPos, Position targetPos) {
        if (!isOrthogonal(attackerPos, targetPos)) {
            return false;
        }
        int distance = manhattanDistance(attackerPos, targetPos);
        return distance >= 1 && distance <= unit.getAttackRange();
    }

    /**
     * Check if a unit can attack from position with effective (buff-modified) attack range.
     */
    private boolean canAttackFromPositionWithBuffs(Position attackerPos, Position targetPos, int effectiveAttackRange) {
        if (!isOrthogonal(attackerPos, targetPos)) {
            return false;
        }
        int distance = manhattanDistance(attackerPos, targetPos);
        return distance >= 1 && distance <= effectiveAttackRange;
    }

    /**
     * Check if a position is within board bounds.
     */
    private boolean isInBounds(Position pos, Board board) {
        return pos.getX() >= 0 && pos.getX() < board.getWidth() &&
               pos.getY() >= 0 && pos.getY() < board.getHeight();
    }

    /**
     * Find a unit by ID.
     */
    private Unit findUnitById(List<Unit> units, String unitId) {
        for (Unit u : units) {
            if (u.getId().equals(unitId)) {
                return u;
            }
        }
        return null;
    }

    /**
     * Check if a tile is occupied by any alive unit.
     */
    private boolean isTileOccupied(List<Unit> units, Position pos) {
        for (Unit u : units) {
            if (u.isAlive() && u.getPosition().getX() == pos.getX() &&
                u.getPosition().getY() == pos.getY()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the next player ID (switches turn).
     */
    private PlayerId getNextPlayer(PlayerId current) {
        return current.getValue().equals("P1") ? new PlayerId("P2") : new PlayerId("P1");
    }

    /**
     * Result of game-over check.
     */
    private static class GameOverResult {
        final boolean isGameOver;
        final PlayerId winner;

        GameOverResult(boolean isGameOver, PlayerId winner) {
            this.isGameOver = isGameOver;
            this.winner = winner;
        }
    }

    /**
     * Check if the game is over based on unit states.
     * Game ends when one player has no alive units.
     */
    private GameOverResult checkGameOver(List<Unit> units) {
        boolean p1HasAlive = false;
        boolean p2HasAlive = false;

        for (Unit u : units) {
            if (u.isAlive()) {
                if (u.getOwner().getValue().equals("P1")) {
                    p1HasAlive = true;
                } else {
                    p2HasAlive = true;
                }
            }
        }

        if (!p1HasAlive) {
            return new GameOverResult(true, new PlayerId("P2"));
        } else if (!p2HasAlive) {
            return new GameOverResult(true, new PlayerId("P1"));
        }
        return new GameOverResult(false, null);
    }

    // =========================================================================
    // Buff Helper Methods
    // =========================================================================

    /**
     * Get the list of buffs for a specific unit.
     * Returns empty list if unit has no buffs.
     */
    private List<BuffInstance> getBuffsForUnit(GameState state, String unitId) {
        Map<String, List<BuffInstance>> unitBuffs = state.getUnitBuffs();
        if (unitBuffs == null) {
            return Collections.emptyList();
        }
        List<BuffInstance> buffs = unitBuffs.get(unitId);
        return buffs != null ? buffs : Collections.emptyList();
    }

    /**
     * Check if unit has any buff with stunned flag.
     */
    private boolean isUnitStunned(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isStunned()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if unit has any buff with rooted flag.
     */
    private boolean isUnitRooted(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isRooted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate effective move range: base + sum of bonusMoveRange from all buffs.
     */
    private int getEffectiveMoveRange(Unit unit, List<BuffInstance> buffs) {
        int bonus = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getModifiers() != null) {
                bonus += buff.getModifiers().getBonusMoveRange();
            }
        }
        return unit.getMoveRange() + bonus;
    }

    /**
     * Calculate effective attack range: base + sum of bonusAttackRange from all buffs.
     */
    private int getEffectiveAttackRange(Unit unit, List<BuffInstance> buffs) {
        int bonus = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getModifiers() != null) {
                bonus += buff.getModifiers().getBonusAttackRange();
            }
        }
        return unit.getAttackRange() + bonus;
    }

    /**
     * Get total bonus attack from all buffs on a unit.
     */
    private int getBonusAttack(List<BuffInstance> buffs) {
        int bonus = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getModifiers() != null) {
                bonus += buff.getModifiers().getBonusAttack();
            }
        }
        return bonus;
    }

    /**
     * Count poison damage (1 per buff with poison flag).
     */
    private int getPoisonDamage(List<BuffInstance> buffs) {
        int count = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isPoison()) {
                count++;
            }
        }
        return count;
    }

    // =========================================================================
    // V3 Buff Helper Methods
    // =========================================================================

    /**
     * Check if unit has POWER buff (blocks MOVE_AND_ATTACK, enables DESTROY_OBSTACLE).
     */
    private boolean hasPowerBuff(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isPowerBuff()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if unit has SPEED buff (grants extra action per round).
     */
    private boolean hasSpeedBuff(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isSpeedBuff()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if unit has SLOW buff (actions delayed by 1 round).
     */
    private boolean hasSlowBuff(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isSlowBuff()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get max actions per turn for a unit.
     * SPEED buff grants 2 actions, normal units have 1.
     */
    private int getMaxActions(List<BuffInstance> buffs) {
        return hasSpeedBuff(buffs) ? 2 : 1;
    }

    /**
     * Check if unit can still perform actions this turn.
     */
    private boolean canUnitAct(Unit unit, List<BuffInstance> buffs) {
        int maxActions = getMaxActions(buffs);
        return unit.getActionsUsed() < maxActions;
    }

    /**
     * Count BLEED buffs on a unit (each deals 1 damage per round).
     */
    private int getBleedDamage(List<BuffInstance> buffs) {
        int count = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isBleedBuff()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if a position has an obstacle (V3).
     */
    private boolean hasObstacleAt(GameState state, Position pos) {
        return state.hasObstacleAt(pos);
    }

    /**
     * Serialize an action to a map for storage in preparingAction (V3 SLOW buff).
     */
    private Map<String, Object> serializeActionForPreparing(Action action) {
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

    /**
     * Check if a position is blocked by obstacle or unit.
     */
    private boolean isTileBlocked(GameState state, Position pos) {
        return isTileOccupied(state.getUnits(), pos) || hasObstacleAt(state, pos);
    }

    /**
     * V3: Get a random buff type using RngProvider.
     * Uses nextInt(6) to select from POWER, LIFE, SPEED, WEAKNESS, BLEED, SLOW.
     */
    private BuffType getRandomBuffType() {
        int roll = rngProvider.nextInt(6);
        BuffType[] types = BuffType.values();
        return types[roll];
    }

    /**
     * Result of checking buff tile triggers.
     */
    private static class BuffTileTriggerResult {
        final List<Unit> units;
        final Map<String, List<BuffInstance>> unitBuffs;
        final List<com.tactics.engine.model.BuffTile> buffTiles;

        BuffTileTriggerResult(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs,
                              List<com.tactics.engine.model.BuffTile> buffTiles) {
            this.units = units;
            this.unitBuffs = unitBuffs;
            this.buffTiles = buffTiles;
        }
    }

    /**
     * V3: Check if a unit triggers a buff tile after moving to a position.
     * If so, apply the buff and mark the tile as triggered.
     */
    private BuffTileTriggerResult checkBuffTileTrigger(GameState state, Unit movedUnit, Position newPos,
                                                        List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        // Check if there's an untriggered buff tile at the new position
        com.tactics.engine.model.BuffTile tile = state.getBuffTileAt(newPos);
        if (tile == null) {
            return new BuffTileTriggerResult(units, unitBuffs, state.getBuffTiles());
        }

        // Determine buff type: use tile's type if set, otherwise random
        BuffType buffType = tile.getBuffType();
        if (buffType == null) {
            buffType = getRandomBuffType();
        }

        // Create the buff instance
        BuffInstance newBuff = BuffFactory.create(buffType, "bufftile_" + tile.getId());

        // Add buff to the unit
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(unitBuffs);
        List<BuffInstance> currentBuffs = new ArrayList<>(
            unitBuffs.getOrDefault(movedUnit.getId(), Collections.emptyList())
        );
        currentBuffs.add(newBuff);
        newUnitBuffs.put(movedUnit.getId(), currentBuffs);

        // Apply instant HP bonus if any (POWER, LIFE, WEAKNESS)
        List<Unit> newUnits = units;
        if (newBuff.getInstantHpBonus() != 0) {
            newUnits = new ArrayList<>();
            for (Unit u : units) {
                if (u.getId().equals(movedUnit.getId())) {
                    int newHp = u.getHp() + newBuff.getInstantHpBonus();
                    boolean alive = newHp > 0;
                    newUnits.add(new Unit(
                        u.getId(), u.getOwner(), newHp, u.getAttack(),
                        u.getMoveRange(), u.getAttackRange(), u.getPosition(), alive,
                        u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                        u.getSelectedSkillId(), u.getSkillCooldown(),
                        u.getShield(), u.isInvisible(), u.isInvulnerable(),
                        u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                        u.getActionsUsed(), u.isPreparing(), u.getPreparingAction()
                    ));
                } else {
                    newUnits.add(u);
                }
            }
        }

        // Mark tile as triggered
        List<com.tactics.engine.model.BuffTile> newBuffTiles = new ArrayList<>();
        for (com.tactics.engine.model.BuffTile t : state.getBuffTiles()) {
            if (t.getId().equals(tile.getId())) {
                newBuffTiles.add(new com.tactics.engine.model.BuffTile(
                    t.getId(), t.getPosition(), buffType, t.getDuration(), true  // Mark triggered
                ));
            } else {
                newBuffTiles.add(t);
            }
        }

        return new BuffTileTriggerResult(newUnits, newUnitBuffs, newBuffTiles);
    }

    // =========================================================================
    // Validation (V2: Uses moveRange and attackRange from Unit)
    // (V3: Also considers buff effects)
    // =========================================================================

    public ValidationResult validateAction(GameState state, Action action) {
        // G1: Null action type
        if (action.getType() == null) {
            return new ValidationResult(false, "Invalid action type");
        }

        // G3: Game already over
        if (state.isGameOver()) {
            return new ValidationResult(false, "Game is already over");
        }

        // G2: Wrong player turn
        if (action.getPlayerId() == null ||
            !action.getPlayerId().getValue().equals(state.getCurrentPlayer().getValue())) {
            return new ValidationResult(false, "Not your turn");
        }

        // Dispatch based on action type
        ActionType type = action.getType();

        if (type == ActionType.END_TURN) {
            return new ValidationResult(true, null);
        }

        if (type == ActionType.MOVE) {
            return validateMove(state, action);
        }

        if (type == ActionType.ATTACK) {
            return validateAttack(state, action);
        }

        if (type == ActionType.MOVE_AND_ATTACK) {
            return validateMoveAndAttack(state, action);
        }

        // V3: DESTROY_OBSTACLE action
        if (type == ActionType.DESTROY_OBSTACLE) {
            return validateDestroyObstacle(state, action);
        }

        // V3: DEATH_CHOICE action
        if (type == ActionType.DEATH_CHOICE) {
            return validateDeathChoice(state, action);
        }

        // V3: USE_SKILL action (placeholder for future implementation)
        if (type == ActionType.USE_SKILL) {
            return new ValidationResult(false, "Skill system not yet implemented");
        }

        // G4: Unknown action type
        return new ValidationResult(false, "Invalid action type");
    }

    // =========================================================================
    // V3 Action Validation Methods
    // =========================================================================

    /**
     * Validate DESTROY_OBSTACLE action (V3).
     * Requires:
     * - Unit has POWER buff
     * - Target position has an obstacle
     * - Target position is adjacent to the acting unit
     */
    private ValidationResult validateDestroyObstacle(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();
        String actingUnitId = action.getActingUnitId();

        // Need target position
        if (targetPos == null) {
            return new ValidationResult(false, "Target position is required for DESTROY_OBSTACLE");
        }

        // Need acting unit ID
        if (actingUnitId == null) {
            return new ValidationResult(false, "Acting unit ID is required for DESTROY_OBSTACLE");
        }

        // Find the acting unit
        Unit actingUnit = findUnitById(state.getUnits(), actingUnitId);
        if (actingUnit == null) {
            return new ValidationResult(false, "Acting unit not found");
        }

        // Verify unit is alive
        if (!actingUnit.isAlive()) {
            return new ValidationResult(false, "Acting unit is dead");
        }

        // Verify unit belongs to current player
        if (!actingUnit.getOwner().getValue().equals(action.getPlayerId().getValue())) {
            return new ValidationResult(false, "Cannot control opponent's unit");
        }

        // Check for POWER buff
        List<BuffInstance> buffs = getBuffsForUnit(state, actingUnitId);
        if (!hasPowerBuff(buffs)) {
            return new ValidationResult(false, "DESTROY_OBSTACLE requires Power buff");
        }

        // V3: Action limit check (SPEED buff allows 2 actions)
        if (!canUnitAct(actingUnit, buffs)) {
            return new ValidationResult(false, "Unit has no remaining actions this turn");
        }

        // Check that target position has an obstacle
        if (!hasObstacleAt(state, targetPos)) {
            return new ValidationResult(false, "No obstacle at target position");
        }

        // Check that target position is adjacent to acting unit
        if (!isAdjacent(actingUnit.getPosition(), targetPos)) {
            return new ValidationResult(false, "Obstacle is not adjacent to unit");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Validate DEATH_CHOICE action (V3).
     * Requires:
     * - There is a pending death choice
     * - The player making the choice is the owner of the dead minion
     * - A valid choice type is specified
     */
    private ValidationResult validateDeathChoice(GameState state, Action action) {
        // Check for pending death choice
        if (!state.hasPendingDeathChoice()) {
            return new ValidationResult(false, "No pending death choice");
        }

        // Verify the player making the choice is the owner
        if (!state.getPendingDeathChoice().getOwner().getValue().equals(action.getPlayerId().getValue())) {
            return new ValidationResult(false, "Not your death choice");
        }

        // Verify a choice type is specified
        if (action.getDeathChoiceType() == null) {
            return new ValidationResult(false, "Death choice type is required");
        }

        return new ValidationResult(true, null);
    }

    // =========================================================================
    // V1/V2 Action Validation Methods
    // =========================================================================

    /**
     * Validate MOVE action using V2 moveRange logic + buff effects.
     * Movement must be orthogonal and within unit's effective moveRange.
     * Stunned/rooted units cannot move.
     */
    private ValidationResult validateMove(GameState state, Action action) {
        // M8: MOVE with targetUnitId non-null is protocol misuse
        if (action.getTargetUnitId() != null) {
            return new ValidationResult(false, "MOVE must not specify targetUnitId");
        }

        Position targetPos = action.getTargetPosition();
        if (targetPos == null) {
            return new ValidationResult(false, "Target position is required for MOVE");
        }

        // M2, M3: Check bounds
        if (!isInBounds(targetPos, state.getBoard())) {
            return new ValidationResult(false, "Target position is outside the board");
        }

        // M6: Check if target tile is occupied (V3: also check for obstacles)
        if (isTileBlocked(state, targetPos)) {
            return new ValidationResult(false, "Target tile is occupied");
        }

        // Find units that can move to targetPos using effective moveRange (with buffs)
        List<Unit> potentialMovers = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int effectiveMoveRange = getEffectiveMoveRange(u, buffs);
                if (canMoveToPositionWithBuffs(u, targetPos, effectiveMoveRange)) {
                    potentialMovers.add(u);
                }
            }
        }

        if (potentialMovers.isEmpty()) {
            return new ValidationResult(false, "No valid unit can move to target position");
        }

        if (potentialMovers.size() > 1) {
            return new ValidationResult(false, "Ambiguous move");
        }

        // Check buff restrictions on the unique mover
        Unit mover = potentialMovers.get(0);
        List<BuffInstance> moverBuffs = getBuffsForUnit(state, mover.getId());

        // V3: Action limit check (SPEED buff allows 2 actions)
        if (!canUnitAct(mover, moverBuffs)) {
            return new ValidationResult(false, "Unit has no remaining actions this turn");
        }

        // Stunned check
        if (isUnitStunned(moverBuffs)) {
            return new ValidationResult(false, "Unit is stunned");
        }

        // Rooted check
        if (isUnitRooted(moverBuffs)) {
            return new ValidationResult(false, "Unit is rooted");
        }

        // V3: SLOW buff check - if unit has SLOW buff and is not preparing, this will queue the action
        // Validation still passes, but applyMove will handle the delayed execution
        // No special validation needed - the action will be queued in apply

        return new ValidationResult(true, null);
    }

    /**
     * Validate ATTACK action using V2 attackRange logic + buff effects.
     * Attack must be orthogonal and within attacker's effective attackRange.
     * Stunned units cannot attack.
     */
    private ValidationResult validateAttack(GameState state, Action action) {
        // A7: Missing targetUnitId
        if (action.getTargetUnitId() == null) {
            return new ValidationResult(false, "Target unit ID is required for ATTACK");
        }

        // A8: Missing targetPosition
        if (action.getTargetPosition() == null) {
            return new ValidationResult(false, "Target position is required for ATTACK");
        }

        Position targetPos = action.getTargetPosition();
        String targetUnitId = action.getTargetUnitId();

        // Find target unit
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);

        // A6: Target missing
        if (targetUnit == null) {
            return new ValidationResult(false, "Target unit not found");
        }

        // A6: Target dead
        if (!targetUnit.isAlive()) {
            return new ValidationResult(false, "Target unit is dead");
        }

        // A4: Friendly fire check
        if (targetUnit.getOwner().getValue().equals(action.getPlayerId().getValue())) {
            return new ValidationResult(false, "Cannot attack own unit");
        }

        // Verify targetPosition matches target unit's actual position
        if (targetUnit.getPosition().getX() != targetPos.getX() ||
            targetUnit.getPosition().getY() != targetPos.getY()) {
            return new ValidationResult(false, "Target position does not match target unit position");
        }

        // Find attackers: alive friendly units within effective attack range of target
        List<Unit> potentialAttackers = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int effectiveAttackRange = getEffectiveAttackRange(u, buffs);
                if (canAttackFromPositionWithBuffs(u.getPosition(), targetUnit.getPosition(), effectiveAttackRange)) {
                    potentialAttackers.add(u);
                }
            }
        }

        if (potentialAttackers.isEmpty()) {
            return new ValidationResult(false, "No attacker adjacent to target");
        }

        if (potentialAttackers.size() > 1) {
            return new ValidationResult(false, "Ambiguous attacker");
        }

        // Check buff restrictions on the unique attacker
        Unit attacker = potentialAttackers.get(0);
        List<BuffInstance> attackerBuffs = getBuffsForUnit(state, attacker.getId());

        // V3: Action limit check (SPEED buff allows 2 actions)
        if (!canUnitAct(attacker, attackerBuffs)) {
            return new ValidationResult(false, "Unit has no remaining actions this turn");
        }

        // Stunned check
        if (isUnitStunned(attackerBuffs)) {
            return new ValidationResult(false, "Unit is stunned");
        }

        // Rooted units CAN attack (only movement is blocked)

        return new ValidationResult(true, null);
    }

    /**
     * Validate MOVE_AND_ATTACK action using V2 moveRange and attackRange logic + buff effects.
     * MOVE step must be orthogonal and within effective moveRange.
     * ATTACK step (from post-move position) must be orthogonal and within effective attackRange.
     * Stunned and rooted units cannot use MOVE_AND_ATTACK.
     */
    private ValidationResult validateMoveAndAttack(GameState state, Action action) {
        // MA5: Missing targetPosition
        if (action.getTargetPosition() == null) {
            return new ValidationResult(false, "Target position is required for MOVE_AND_ATTACK");
        }

        // MA4: Missing targetUnitId
        if (action.getTargetUnitId() == null) {
            return new ValidationResult(false, "Target unit ID is required for MOVE_AND_ATTACK");
        }

        Position targetPos = action.getTargetPosition();
        String targetUnitId = action.getTargetUnitId();

        // Validate MOVE part: bounds check
        if (!isInBounds(targetPos, state.getBoard())) {
            return new ValidationResult(false, "Target position is outside the board");
        }

        // Check if target tile is occupied (V3: also check for obstacles)
        if (isTileBlocked(state, targetPos)) {
            return new ValidationResult(false, "Target tile is occupied");
        }

        // Find target unit
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);

        // MA8: Target not found or dead
        if (targetUnit == null) {
            return new ValidationResult(false, "Target unit not found");
        }

        if (!targetUnit.isAlive()) {
            return new ValidationResult(false, "Target unit is dead");
        }

        // Target must be enemy
        if (targetUnit.getOwner().getValue().equals(action.getPlayerId().getValue())) {
            return new ValidationResult(false, "Cannot attack own unit");
        }

        // Find mover: unit that can move to targetPos using effective moveRange
        Unit mover = null;
        int moverCount = 0;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int effectiveMoveRange = getEffectiveMoveRange(u, buffs);
                if (canMoveToPositionWithBuffs(u, targetPos, effectiveMoveRange)) {
                    mover = u;
                    moverCount++;
                }
            }
        }

        if (moverCount == 0) {
            return new ValidationResult(false, "No valid unit can move to target position");
        }

        if (moverCount > 1) {
            return new ValidationResult(false, "Ambiguous move");
        }

        // Check buff restrictions on the mover BEFORE checking attack validity
        List<BuffInstance> moverBuffs = getBuffsForUnit(state, mover.getId());

        // V3: Action limit check (SPEED buff allows 2 actions)
        if (!canUnitAct(mover, moverBuffs)) {
            return new ValidationResult(false, "Unit has no remaining actions this turn");
        }

        // Stunned check
        if (isUnitStunned(moverBuffs)) {
            return new ValidationResult(false, "Unit is stunned");
        }

        // Rooted check - cannot move
        if (isUnitRooted(moverBuffs)) {
            return new ValidationResult(false, "Unit is rooted");
        }

        // V3: POWER buff check - POWER buff blocks MOVE_AND_ATTACK
        if (hasPowerBuff(moverBuffs)) {
            return new ValidationResult(false, "Unit cannot use MOVE_AND_ATTACK with Power buff");
        }

        // MA3: After moving, check if target unit is within effective attack range
        int effectiveAttackRange = getEffectiveAttackRange(mover, moverBuffs);
        if (!canAttackFromPositionWithBuffs(targetPos, targetUnit.getPosition(), effectiveAttackRange)) {
            return new ValidationResult(false, "Target not adjacent after movement");
        }

        // MA6: Check for ambiguous attacker after move using effective attackRange
        int attackerCountAfterMove = 0;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                Position unitPos = u.getId().equals(mover.getId()) ? targetPos : u.getPosition();
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int unitEffectiveAttackRange = getEffectiveAttackRange(u, buffs);
                if (canAttackFromPositionWithBuffs(unitPos, targetUnit.getPosition(), unitEffectiveAttackRange)) {
                    attackerCountAfterMove++;
                }
            }
        }

        if (attackerCountAfterMove > 1) {
            return new ValidationResult(false, "Ambiguous attacker after movement");
        }

        return new ValidationResult(true, null);
    }

    // =========================================================================
    // Apply Action (V2: Uses canMoveToPosition and canAttackFromPosition)
    // (V3: Also considers buff effects and turn-end processing)
    // =========================================================================

    public GameState applyAction(GameState state, Action action) {
        ActionType type = action.getType();

        if (type == ActionType.END_TURN) {
            return applyEndTurn(state);
        }

        if (type == ActionType.MOVE) {
            return applyMove(state, action);
        }

        if (type == ActionType.ATTACK) {
            return applyAttack(state, action);
        }

        if (type == ActionType.MOVE_AND_ATTACK) {
            return applyMoveAndAttack(state, action);
        }

        // V3: DESTROY_OBSTACLE action
        if (type == ActionType.DESTROY_OBSTACLE) {
            return applyDestroyObstacle(state, action);
        }

        // V3: DEATH_CHOICE action
        if (type == ActionType.DEATH_CHOICE) {
            return applyDeathChoice(state, action);
        }

        // Should not reach here if validation passed
        return null;
    }

    /**
     * Apply END_TURN: process turn-end buff effects, then switch player.
     * V3: Also tracks turn ended flags for round processing.
     */
    private GameState applyEndTurn(GameState state) {
        // Process turn-end buff effects (poison damage, duration reduction, expiration)
        TurnEndResult turnEndResult = processTurnEnd(state.getUnits(), state.getUnitBuffs());

        GameOverResult gameOver = checkGameOver(turnEndResult.units);

        // V3: Determine new turn ended flags
        boolean newPlayer1TurnEnded = state.isPlayer1TurnEnded();
        boolean newPlayer2TurnEnded = state.isPlayer2TurnEnded();

        if (state.getCurrentPlayer().getValue().equals("P1")) {
            newPlayer1TurnEnded = true;
        } else {
            newPlayer2TurnEnded = true;
        }

        // V3: If both players have ended their turn, process round end
        if (newPlayer1TurnEnded && newPlayer2TurnEnded) {
            return processRoundEnd(state, turnEndResult, gameOver);
        }

        // Switch to next player, keep turn ended flags
        return new GameState(
            state.getBoard(),
            turnEndResult.units,
            getNextPlayer(state.getCurrentPlayer()),
            gameOver.isGameOver,
            gameOver.winner,
            turnEndResult.unitBuffs,
            state.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            newPlayer1TurnEnded,
            newPlayer2TurnEnded
        );
    }

    /**
     * Process round end: execute preparing actions, increment round, reset turn flags, reset actionsUsed.
     * Called when both players have ended their turn.
     */
    private GameState processRoundEnd(GameState state, TurnEndResult turnEndResult, GameOverResult gameOver) {
        // V3: Execute preparing actions (SLOW buff delayed actions)
        PreparingActionsResult prepResult = executePreparingActions(state, turnEndResult.units, turnEndResult.unitBuffs);

        // Check game over again after executing preparing actions
        GameOverResult gameOverAfterPrep = checkGameOver(prepResult.units);
        if (gameOverAfterPrep.isGameOver) {
            gameOver = gameOverAfterPrep;
        }

        // Reset actionsUsed and clear preparing state for all units
        List<Unit> unitsWithResetActions = resetActionsUsedAndPreparingState(prepResult.units);

        return new GameState(
            state.getBoard(),
            unitsWithResetActions,
            getNextPlayer(state.getCurrentPlayer()),
            gameOver.isGameOver,
            gameOver.winner,
            prepResult.unitBuffs,
            prepResult.buffTiles,
            prepResult.obstacles,
            state.getCurrentRound() + 1,  // Increment round
            state.getPendingDeathChoice(),
            false,  // Reset player1TurnEnded
            false   // Reset player2TurnEnded
        );
    }

    /**
     * Result of executing preparing actions.
     */
    private static class PreparingActionsResult {
        final List<Unit> units;
        final Map<String, List<BuffInstance>> unitBuffs;
        final List<com.tactics.engine.model.BuffTile> buffTiles;
        final List<com.tactics.engine.model.Obstacle> obstacles;

        PreparingActionsResult(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs,
                               List<com.tactics.engine.model.BuffTile> buffTiles,
                               List<com.tactics.engine.model.Obstacle> obstacles) {
            this.units = units;
            this.unitBuffs = unitBuffs;
            this.buffTiles = buffTiles;
            this.obstacles = obstacles;
        }
    }

    /**
     * V3: Execute all preparing actions at round start.
     * For attacks, check if target is still at expected position (misses if moved).
     */
    private PreparingActionsResult executePreparingActions(GameState state, List<Unit> units,
                                                            Map<String, List<BuffInstance>> unitBuffs) {
        List<Unit> currentUnits = new ArrayList<>(units);
        List<com.tactics.engine.model.BuffTile> currentBuffTiles = new ArrayList<>(state.getBuffTiles());
        List<com.tactics.engine.model.Obstacle> currentObstacles = new ArrayList<>(state.getObstacles());

        // Find all units with preparing actions (sorted by ID for determinism)
        List<String> preparingUnitIds = new ArrayList<>();
        for (Unit u : currentUnits) {
            if (u.isPreparing() && u.getPreparingAction() != null) {
                preparingUnitIds.add(u.getId());
            }
        }
        Collections.sort(preparingUnitIds);

        // Execute each preparing action
        for (String unitId : preparingUnitIds) {
            Unit prepUnit = null;
            for (Unit u : currentUnits) {
                if (u.getId().equals(unitId)) {
                    prepUnit = u;
                    break;
                }
            }
            if (prepUnit == null || !prepUnit.isAlive()) {
                continue;  // Unit died before executing
            }

            Map<String, Object> actionMap = prepUnit.getPreparingAction();
            currentUnits = executeOnePreparingAction(prepUnit, actionMap, currentUnits);
        }

        return new PreparingActionsResult(currentUnits, unitBuffs, currentBuffTiles, currentObstacles);
    }

    /**
     * Execute a single preparing action.
     * Returns updated units list.
     */
    @SuppressWarnings("unchecked")
    private List<Unit> executeOnePreparingAction(Unit prepUnit, Map<String, Object> actionMap, List<Unit> units) {
        String actionType = (String) actionMap.get("type");

        if ("MOVE".equals(actionType)) {
            Map<String, Object> posMap = (Map<String, Object>) actionMap.get("targetPosition");
            int x = ((Number) posMap.get("x")).intValue();
            int y = ((Number) posMap.get("y")).intValue();
            Position targetPos = new Position(x, y);

            // Check if target is still valid (not blocked)
            boolean blocked = false;
            for (Unit u : units) {
                if (u.isAlive() && u.getPosition().equals(targetPos)) {
                    blocked = true;
                    break;
                }
            }

            if (!blocked) {
                // Execute move
                List<Unit> newUnits = new ArrayList<>();
                for (Unit u : units) {
                    if (u.getId().equals(prepUnit.getId())) {
                        newUnits.add(new Unit(
                            u.getId(), u.getOwner(), u.getHp(), u.getAttack(),
                            u.getMoveRange(), u.getAttackRange(), targetPos, u.isAlive(),
                            u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                            u.getSelectedSkillId(), u.getSkillCooldown(),
                            u.getShield(), u.isInvisible(), u.isInvulnerable(),
                            u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                            u.getActionsUsed(), false, null  // Clear preparing state
                        ));
                    } else {
                        newUnits.add(u);
                    }
                }
                return newUnits;
            }
            // If blocked, action fails silently (miss)

        } else if ("ATTACK".equals(actionType)) {
            String targetUnitId = (String) actionMap.get("targetUnitId");
            Map<String, Object> posMap = (Map<String, Object>) actionMap.get("targetPosition");
            int expectedX = ((Number) posMap.get("x")).intValue();
            int expectedY = ((Number) posMap.get("y")).intValue();
            Position expectedPos = new Position(expectedX, expectedY);

            // Find target unit
            Unit targetUnit = null;
            for (Unit u : units) {
                if (u.getId().equals(targetUnitId)) {
                    targetUnit = u;
                    break;
                }
            }

            // Check if target is still alive and at expected position
            if (targetUnit != null && targetUnit.isAlive() && targetUnit.getPosition().equals(expectedPos)) {
                // Execute attack
                int damage = prepUnit.getAttack();
                int newHp = targetUnit.getHp() - damage;
                boolean alive = newHp > 0;

                List<Unit> newUnits = new ArrayList<>();
                for (Unit u : units) {
                    if (u.getId().equals(targetUnitId)) {
                        newUnits.add(new Unit(
                            u.getId(), u.getOwner(), newHp, u.getAttack(),
                            u.getMoveRange(), u.getAttackRange(), u.getPosition(), alive,
                            u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                            u.getSelectedSkillId(), u.getSkillCooldown(),
                            u.getShield(), u.isInvisible(), u.isInvulnerable(),
                            u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                            u.getActionsUsed(), u.isPreparing(), u.getPreparingAction()
                        ));
                    } else {
                        newUnits.add(u);
                    }
                }
                return newUnits;
            }
            // If target moved or died, attack misses

        } else if ("MOVE_AND_ATTACK".equals(actionType)) {
            // Similar to ATTACK - check if move destination is free and target at expected position
            Map<String, Object> posMap = (Map<String, Object>) actionMap.get("targetPosition");
            int moveX = ((Number) posMap.get("x")).intValue();
            int moveY = ((Number) posMap.get("y")).intValue();
            Position movePos = new Position(moveX, moveY);
            String targetUnitId = (String) actionMap.get("targetUnitId");

            // Check move destination
            boolean moveBlocked = false;
            for (Unit u : units) {
                if (u.isAlive() && u.getPosition().equals(movePos)) {
                    moveBlocked = true;
                    break;
                }
            }

            if (!moveBlocked) {
                // Find target unit and check if in range after move
                Unit targetUnit = null;
                for (Unit u : units) {
                    if (u.getId().equals(targetUnitId)) {
                        targetUnit = u;
                        break;
                    }
                }

                // Execute move + attack if target still exists and is in range
                if (targetUnit != null && targetUnit.isAlive()) {
                    int distance = manhattanDistance(movePos, targetUnit.getPosition());
                    if (distance >= 1 && distance <= prepUnit.getAttackRange()) {
                        int damage = prepUnit.getAttack();
                        int newHp = targetUnit.getHp() - damage;
                        boolean alive = newHp > 0;

                        List<Unit> newUnits = new ArrayList<>();
                        for (Unit u : units) {
                            if (u.getId().equals(prepUnit.getId())) {
                                newUnits.add(new Unit(
                                    u.getId(), u.getOwner(), u.getHp(), u.getAttack(),
                                    u.getMoveRange(), u.getAttackRange(), movePos, u.isAlive(),
                                    u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                                    u.getSelectedSkillId(), u.getSkillCooldown(),
                                    u.getShield(), u.isInvisible(), u.isInvulnerable(),
                                    u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                                    u.getActionsUsed(), false, null
                                ));
                            } else if (u.getId().equals(targetUnitId)) {
                                newUnits.add(new Unit(
                                    u.getId(), u.getOwner(), newHp, u.getAttack(),
                                    u.getMoveRange(), u.getAttackRange(), u.getPosition(), alive,
                                    u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                                    u.getSelectedSkillId(), u.getSkillCooldown(),
                                    u.getShield(), u.isInvisible(), u.isInvulnerable(),
                                    u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                                    u.getActionsUsed(), u.isPreparing(), u.getPreparingAction()
                                ));
                            } else {
                                newUnits.add(u);
                            }
                        }
                        return newUnits;
                    }
                }
            }
            // Action fails if blocked or target out of range
        }

        // No change (action failed/skipped)
        return units;
    }

    /**
     * Reset actionsUsed to 0 and clear preparing state for all units at round start.
     */
    private List<Unit> resetActionsUsedAndPreparingState(List<Unit> units) {
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : units) {
            if (u.getActionsUsed() > 0 || u.isPreparing()) {
                // Create new unit with reset actionsUsed and cleared preparing state
                newUnits.add(new Unit(
                    u.getId(), u.getOwner(), u.getHp(), u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), u.getPosition(), u.isAlive(),
                    u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                    u.getSelectedSkillId(), u.getSkillCooldown(),
                    u.getShield(), u.isInvisible(), u.isInvulnerable(),
                    u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                    0,     // Reset actionsUsed
                    false, // Clear preparing
                    null   // Clear preparingAction
                ));
            } else {
                newUnits.add(u);
            }
        }
        return newUnits;
    }

    /**
     * V3 SLOW buff: Queue an action for execution at start of next round.
     * Sets the unit's preparing flag and stores the action.
     */
    private GameState applySlowBuffPreparing(GameState state, Action action, Unit actingUnit) {
        Map<String, Object> preparingAction = serializeActionForPreparing(action);

        // Create new units list with the acting unit in preparing state
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(new Unit(
                    u.getId(), u.getOwner(), u.getHp(), u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), u.getPosition(), u.isAlive(),
                    u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                    u.getSelectedSkillId(), u.getSkillCooldown(),
                    u.getShield(), u.isInvisible(), u.isInvulnerable(),
                    u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                    u.getActionsUsed() + 1,  // Increment actionsUsed (action is queued)
                    true,  // Set preparing
                    preparingAction  // Store the action
                ));
            } else {
                newUnits.add(u);
            }
        }

        // SLOW buff preparing does not switch turn
        return new GameState(
            state.getBoard(),
            newUnits,
            state.getCurrentPlayer(),
            state.isGameOver(),
            state.getWinner(),
            state.getUnitBuffs(),
            state.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );
    }

    private GameState applyMove(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();

        // Find the unique mover using effective moveRange (with buffs)
        Unit mover = null;
        List<BuffInstance> moverBuffs = null;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int effectiveMoveRange = getEffectiveMoveRange(u, buffs);
                if (canMoveToPositionWithBuffs(u, targetPos, effectiveMoveRange)) {
                    mover = u;
                    moverBuffs = buffs;
                    break;
                }
            }
        }

        // V3: SLOW buff check - if unit has SLOW buff, queue action for next round
        if (hasSlowBuff(moverBuffs)) {
            return applySlowBuffPreparing(state, action, mover);
        }

        // Create new units list with updated position and incremented actionsUsed
        List<Unit> newUnits = new ArrayList<>();
        Unit movedUnit = null;
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(mover.getId())) {
                movedUnit = new Unit(
                    u.getId(), u.getOwner(), u.getHp(), u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), targetPos, u.isAlive(),
                    u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                    u.getSelectedSkillId(), u.getSkillCooldown(),
                    u.getShield(), u.isInvisible(), u.isInvulnerable(),
                    u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                    u.getActionsUsed() + 1,  // Increment actionsUsed
                    u.isPreparing(), u.getPreparingAction()
                );
                newUnits.add(movedUnit);
            } else {
                newUnits.add(u);
            }
        }

        // V3: Check for buff tile trigger at destination
        BuffTileTriggerResult tileResult = checkBuffTileTrigger(state, movedUnit, targetPos, newUnits, state.getUnitBuffs());

        GameOverResult gameOver = checkGameOver(tileResult.units);

        // MOVE does not switch turn, does not process turn-end buffs
        return new GameState(
            state.getBoard(),
            tileResult.units,
            state.getCurrentPlayer(),
            gameOver.isGameOver,
            gameOver.winner,
            tileResult.unitBuffs,
            tileResult.buffTiles,
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );
    }

    private GameState applyAttack(GameState state, Action action) {
        String targetUnitId = action.getTargetUnitId();
        Position targetPos = action.getTargetPosition();

        // Find target unit and attacker using effective attackRange (with buffs)
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        Unit attacker = null;
        List<BuffInstance> attackerBuffs = null;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int effectiveAttackRange = getEffectiveAttackRange(u, buffs);
                if (canAttackFromPositionWithBuffs(u.getPosition(), targetPos, effectiveAttackRange)) {
                    attacker = u;
                    attackerBuffs = buffs;
                    break;
                }
            }
        }

        // V3: SLOW buff check - if unit has SLOW buff, queue action for next round
        if (hasSlowBuff(attackerBuffs)) {
            return applySlowBuffPreparing(state, action, attacker);
        }

        // Calculate damage including bonus attack from buffs
        int bonusAttack = getBonusAttack(attackerBuffs);
        int totalDamage = attacker.getAttack() + bonusAttack;

        // Calculate new HP
        int newHp = targetUnit.getHp() - totalDamage;
        boolean alive = newHp > 0;

        // Create new units list with updated target HP and attacker's actionsUsed
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(targetUnitId)) {
                newUnits.add(new Unit(
                    u.getId(), u.getOwner(), newHp, u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), u.getPosition(), alive,
                    u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                    u.getSelectedSkillId(), u.getSkillCooldown(),
                    u.getShield(), u.isInvisible(), u.isInvulnerable(),
                    u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                    u.getActionsUsed(), u.isPreparing(), u.getPreparingAction()
                ));
            } else if (u.getId().equals(attacker.getId())) {
                // Increment attacker's actionsUsed
                newUnits.add(new Unit(
                    u.getId(), u.getOwner(), u.getHp(), u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), u.getPosition(), u.isAlive(),
                    u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                    u.getSelectedSkillId(), u.getSkillCooldown(),
                    u.getShield(), u.isInvisible(), u.isInvulnerable(),
                    u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                    u.getActionsUsed() + 1,  // Increment actionsUsed
                    u.isPreparing(), u.getPreparingAction()
                ));
            } else {
                newUnits.add(u);
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits);

        // ATTACK does not switch turn, does not process turn-end buffs
        return new GameState(
            state.getBoard(),
            newUnits,
            state.getCurrentPlayer(),
            gameOver.isGameOver,
            gameOver.winner,
            state.getUnitBuffs(),
            state.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );
    }

    private GameState applyMoveAndAttack(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();
        String targetUnitId = action.getTargetUnitId();

        // Find mover using effective moveRange (with buffs)
        Unit mover = null;
        List<BuffInstance> moverBuffs = null;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int effectiveMoveRange = getEffectiveMoveRange(u, buffs);
                if (canMoveToPositionWithBuffs(u, targetPos, effectiveMoveRange)) {
                    mover = u;
                    moverBuffs = buffs;
                    break;
                }
            }
        }

        // V3: SLOW buff check - if unit has SLOW buff, queue action for next round
        if (hasSlowBuff(moverBuffs)) {
            return applySlowBuffPreparing(state, action, mover);
        }

        // Find target
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);

        // Calculate damage including bonus attack from buffs
        int bonusAttack = getBonusAttack(moverBuffs);
        int totalDamage = mover.getAttack() + bonusAttack;

        // Calculate new HP for target
        int newHp = targetUnit.getHp() - totalDamage;
        boolean targetAlive = newHp > 0;

        // Create new units list with mover at new position, target with new HP, and mover's actionsUsed incremented
        List<Unit> newUnits = new ArrayList<>();
        Unit movedUnit = null;
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(mover.getId())) {
                // Move + attack as single action
                movedUnit = new Unit(
                    u.getId(), u.getOwner(), u.getHp(), u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), targetPos, u.isAlive(),
                    u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                    u.getSelectedSkillId(), u.getSkillCooldown(),
                    u.getShield(), u.isInvisible(), u.isInvulnerable(),
                    u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                    u.getActionsUsed() + 1,  // Increment actionsUsed
                    u.isPreparing(), u.getPreparingAction()
                );
                newUnits.add(movedUnit);
            } else if (u.getId().equals(targetUnitId)) {
                newUnits.add(new Unit(
                    u.getId(), u.getOwner(), newHp, u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), u.getPosition(), targetAlive,
                    u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                    u.getSelectedSkillId(), u.getSkillCooldown(),
                    u.getShield(), u.isInvisible(), u.isInvulnerable(),
                    u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                    u.getActionsUsed(), u.isPreparing(), u.getPreparingAction()
                ));
            } else {
                newUnits.add(u);
            }
        }

        // V3: Check for buff tile trigger at destination
        BuffTileTriggerResult tileResult = checkBuffTileTrigger(state, movedUnit, targetPos, newUnits, state.getUnitBuffs());

        // Process turn-end buff effects (poison damage, duration reduction, expiration)
        TurnEndResult turnEndResult = processTurnEnd(tileResult.units, tileResult.unitBuffs);

        GameOverResult gameOver = checkGameOver(turnEndResult.units);

        // MOVE_AND_ATTACK switches turn
        return new GameState(
            state.getBoard(),
            turnEndResult.units,
            getNextPlayer(state.getCurrentPlayer()),
            gameOver.isGameOver,
            gameOver.winner,
            turnEndResult.unitBuffs,
            tileResult.buffTiles,
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );
    }

    // =========================================================================
    // V3 Apply Action Methods
    // =========================================================================

    /**
     * Apply DESTROY_OBSTACLE action (V3).
     * Removes the obstacle at the target position.
     * Does not switch turn (like MOVE).
     */
    private GameState applyDestroyObstacle(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();
        String actingUnitId = action.getActingUnitId();

        // Create new obstacles list without the destroyed one
        List<com.tactics.engine.model.Obstacle> newObstacles = new ArrayList<>();
        for (com.tactics.engine.model.Obstacle obstacle : state.getObstacles()) {
            if (!obstacle.getPosition().equals(targetPos)) {
                newObstacles.add(obstacle);
            }
        }

        // Update acting unit's actionsUsed
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnitId)) {
                newUnits.add(new Unit(
                    u.getId(), u.getOwner(), u.getHp(), u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), u.getPosition(), u.isAlive(),
                    u.getCategory(), u.getMinionType(), u.getHeroClass(), u.getMaxHp(),
                    u.getSelectedSkillId(), u.getSkillCooldown(),
                    u.getShield(), u.isInvisible(), u.isInvulnerable(),
                    u.isTemporary(), u.getTemporaryDuration(), u.getSkillState(),
                    u.getActionsUsed() + 1,  // Increment actionsUsed
                    u.isPreparing(), u.getPreparingAction()
                ));
            } else {
                newUnits.add(u);
            }
        }

        // DESTROY_OBSTACLE does not switch turn
        return new GameState(
            state.getBoard(),
            newUnits,
            state.getCurrentPlayer(),
            state.isGameOver(),
            state.getWinner(),
            state.getUnitBuffs(),
            state.getBuffTiles(),
            newObstacles,
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );
    }

    /**
     * Apply DEATH_CHOICE action (V3).
     * Creates either an obstacle or a buff tile at the death position,
     * then clears the pending death choice.
     */
    private GameState applyDeathChoice(GameState state, Action action) {
        com.tactics.engine.model.DeathChoice deathChoice = state.getPendingDeathChoice();
        Position deathPos = deathChoice.getDeathPosition();
        com.tactics.engine.model.DeathChoice.ChoiceType choiceType = action.getDeathChoiceType();

        List<com.tactics.engine.model.Obstacle> newObstacles = new ArrayList<>(state.getObstacles());
        List<com.tactics.engine.model.BuffTile> newBuffTiles = new ArrayList<>(state.getBuffTiles());

        if (choiceType == com.tactics.engine.model.DeathChoice.ChoiceType.SPAWN_OBSTACLE) {
            // Create obstacle at death position
            String obstacleId = "obstacle_" + System.currentTimeMillis();
            newObstacles.add(new com.tactics.engine.model.Obstacle(obstacleId, deathPos));
        } else if (choiceType == com.tactics.engine.model.DeathChoice.ChoiceType.SPAWN_BUFF_TILE) {
            // Create buff tile at death position with random buff type
            // For now, we use a deterministic approach - the buff type will be determined at trigger time
            String tileId = "bufftile_" + System.currentTimeMillis();
            newBuffTiles.add(new com.tactics.engine.model.BuffTile(
                tileId, deathPos, null, 2, false
            ));
        }

        // Clear pending death choice
        return new GameState(
            state.getBoard(),
            state.getUnits(),
            state.getCurrentPlayer(),
            state.isGameOver(),
            state.getWinner(),
            state.getUnitBuffs(),
            newBuffTiles,
            newObstacles,
            state.getCurrentRound(),
            null  // Clear pending death choice
        );
    }

    // =========================================================================
    // Turn-End Buff Processing
    // =========================================================================

    /**
     * Result of turn-end buff processing.
     */
    private static class TurnEndResult {
        final List<Unit> units;
        final Map<String, List<BuffInstance>> unitBuffs;

        TurnEndResult(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
            this.units = units;
            this.unitBuffs = unitBuffs;
        }
    }

    /**
     * Process turn-end buff effects:
     * 1. Apply poison damage to all alive units with poison buffs
     * 2. Apply BLEED damage to all alive units with BLEED buffs (V3)
     * 3. Decrease duration of all buffs by 1
     * 4. Remove expired buffs (duration <= 0)
     *
     * Processing is deterministic: units processed by ID sorted ascending.
     */
    private TurnEndResult processTurnEnd(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        // If no buffs, return unchanged
        if (unitBuffs == null || unitBuffs.isEmpty()) {
            return new TurnEndResult(units, unitBuffs);
        }

        // Create mutable copies
        List<Unit> newUnits = new ArrayList<>(units);
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>();

        // Get all unit IDs that have buffs, sorted for deterministic ordering
        List<String> unitIdsWithBuffs = new ArrayList<>(unitBuffs.keySet());
        Collections.sort(unitIdsWithBuffs);

        // Step 1: Apply poison damage (deterministic order by unit ID)
        for (String unitId : unitIdsWithBuffs) {
            List<BuffInstance> buffs = unitBuffs.get(unitId);
            if (buffs == null || buffs.isEmpty()) {
                continue;
            }

            int poisonDamage = getPoisonDamage(buffs);
            if (poisonDamage > 0) {
                // Find the unit and apply poison damage
                for (int i = 0; i < newUnits.size(); i++) {
                    Unit u = newUnits.get(i);
                    if (u.getId().equals(unitId) && u.isAlive()) {
                        int newHp = u.getHp() - poisonDamage;
                        boolean alive = newHp > 0;
                        newUnits.set(i, new Unit(u.getId(), u.getOwner(), newHp, u.getAttack(),
                            u.getMoveRange(), u.getAttackRange(), u.getPosition(), alive));
                        break;
                    }
                }
            }
        }

        // Step 1.5 (V3): Apply BLEED damage (deterministic order by unit ID)
        for (String unitId : unitIdsWithBuffs) {
            List<BuffInstance> buffs = unitBuffs.get(unitId);
            if (buffs == null || buffs.isEmpty()) {
                continue;
            }

            int bleedDamage = getBleedDamage(buffs);
            if (bleedDamage > 0) {
                // Find the unit and apply BLEED damage
                for (int i = 0; i < newUnits.size(); i++) {
                    Unit u = newUnits.get(i);
                    if (u.getId().equals(unitId) && u.isAlive()) {
                        int newHp = u.getHp() - bleedDamage;
                        boolean alive = newHp > 0;
                        newUnits.set(i, new Unit(u.getId(), u.getOwner(), newHp, u.getAttack(),
                            u.getMoveRange(), u.getAttackRange(), u.getPosition(), alive));
                        break;
                    }
                }
            }
        }

        // Step 2 & 3: Decrease duration and remove expired buffs
        for (String unitId : unitIdsWithBuffs) {
            List<BuffInstance> buffs = unitBuffs.get(unitId);
            if (buffs == null || buffs.isEmpty()) {
                continue;
            }

            List<BuffInstance> remainingBuffs = new ArrayList<>();
            for (BuffInstance buff : buffs) {
                int newDuration = buff.getDuration() - 1;
                if (newDuration > 0) {
                    // Create new buff with decremented duration
                    remainingBuffs.add(new BuffInstance(
                        buff.getBuffId(),
                        buff.getSourceUnitId(),
                        newDuration,
                        buff.isStackable(),
                        buff.getModifiers(),
                        buff.getFlags()
                    ));
                }
                // If newDuration <= 0, buff expires and is not added
            }

            if (!remainingBuffs.isEmpty()) {
                newUnitBuffs.put(unitId, remainingBuffs);
            }
            // If all buffs expired, don't add entry (empty list = no buffs)
        }

        return new TurnEndResult(newUnits, newUnitBuffs);
    }
}
