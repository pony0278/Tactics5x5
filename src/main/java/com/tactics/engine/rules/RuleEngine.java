package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFlags;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffModifier;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

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
 */
public class RuleEngine {

    public RuleEngine() {
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

        // G4: Unknown action type
        return new ValidationResult(false, "Invalid action type");
    }

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

        // M6: Check if target tile is occupied
        if (isTileOccupied(state.getUnits(), targetPos)) {
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

        // Stunned check
        if (isUnitStunned(moverBuffs)) {
            return new ValidationResult(false, "Unit is stunned");
        }

        // Rooted check
        if (isUnitRooted(moverBuffs)) {
            return new ValidationResult(false, "Unit is rooted");
        }

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

        // Check if target tile is occupied
        if (isTileOccupied(state.getUnits(), targetPos)) {
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

        // Stunned check
        if (isUnitStunned(moverBuffs)) {
            return new ValidationResult(false, "Unit is stunned");
        }

        // Rooted check - cannot move
        if (isUnitRooted(moverBuffs)) {
            return new ValidationResult(false, "Unit is rooted");
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

        // Should not reach here if validation passed
        return null;
    }

    /**
     * Apply END_TURN: process turn-end buff effects, then switch player.
     */
    private GameState applyEndTurn(GameState state) {
        // Process turn-end buff effects (poison damage, duration reduction, expiration)
        TurnEndResult turnEndResult = processTurnEnd(state.getUnits(), state.getUnitBuffs());

        GameOverResult gameOver = checkGameOver(turnEndResult.units);

        return new GameState(
            state.getBoard(),
            turnEndResult.units,
            getNextPlayer(state.getCurrentPlayer()),
            gameOver.isGameOver,
            gameOver.winner,
            turnEndResult.unitBuffs
        );
    }

    private GameState applyMove(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();

        // Find the unique mover using effective moveRange (with buffs)
        Unit mover = null;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int effectiveMoveRange = getEffectiveMoveRange(u, buffs);
                if (canMoveToPositionWithBuffs(u, targetPos, effectiveMoveRange)) {
                    mover = u;
                    break;
                }
            }
        }

        // Create new units list with updated position
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(mover.getId())) {
                newUnits.add(new Unit(u.getId(), u.getOwner(), u.getHp(), u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), targetPos, u.isAlive()));
            } else {
                newUnits.add(u);
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits);

        // MOVE does not switch turn, does not process turn-end buffs
        return new GameState(
            state.getBoard(),
            newUnits,
            state.getCurrentPlayer(),
            gameOver.isGameOver,
            gameOver.winner,
            state.getUnitBuffs()
        );
    }

    private GameState applyAttack(GameState state, Action action) {
        String targetUnitId = action.getTargetUnitId();
        Position targetPos = action.getTargetPosition();

        // Find target unit and attacker using effective attackRange (with buffs)
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        Unit attacker = null;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int effectiveAttackRange = getEffectiveAttackRange(u, buffs);
                if (canAttackFromPositionWithBuffs(u.getPosition(), targetPos, effectiveAttackRange)) {
                    attacker = u;
                    break;
                }
            }
        }

        // Calculate damage including bonus attack from buffs
        List<BuffInstance> attackerBuffs = getBuffsForUnit(state, attacker.getId());
        int bonusAttack = getBonusAttack(attackerBuffs);
        int totalDamage = attacker.getAttack() + bonusAttack;

        // Calculate new HP
        int newHp = targetUnit.getHp() - totalDamage;
        boolean alive = newHp > 0;

        // Create new units list
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(targetUnitId)) {
                newUnits.add(new Unit(u.getId(), u.getOwner(), newHp, u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), u.getPosition(), alive));
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
            state.getUnitBuffs()
        );
    }

    private GameState applyMoveAndAttack(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();
        String targetUnitId = action.getTargetUnitId();

        // Find mover using effective moveRange (with buffs)
        Unit mover = null;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int effectiveMoveRange = getEffectiveMoveRange(u, buffs);
                if (canMoveToPositionWithBuffs(u, targetPos, effectiveMoveRange)) {
                    mover = u;
                    break;
                }
            }
        }

        // Find target
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);

        // Calculate damage including bonus attack from buffs
        List<BuffInstance> moverBuffs = getBuffsForUnit(state, mover.getId());
        int bonusAttack = getBonusAttack(moverBuffs);
        int totalDamage = mover.getAttack() + bonusAttack;

        // Calculate new HP for target
        int newHp = targetUnit.getHp() - totalDamage;
        boolean targetAlive = newHp > 0;

        // Create new units list with mover at new position and target with new HP
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(mover.getId())) {
                newUnits.add(new Unit(u.getId(), u.getOwner(), u.getHp(), u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), targetPos, u.isAlive()));
            } else if (u.getId().equals(targetUnitId)) {
                newUnits.add(new Unit(u.getId(), u.getOwner(), newHp, u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), u.getPosition(), targetAlive));
            } else {
                newUnits.add(u);
            }
        }

        // Process turn-end buff effects (poison damage, duration reduction, expiration)
        TurnEndResult turnEndResult = processTurnEnd(newUnits, state.getUnitBuffs());

        GameOverResult gameOver = checkGameOver(turnEndResult.units);

        // MOVE_AND_ATTACK switches turn
        return new GameState(
            state.getBoard(),
            turnEndResult.units,
            getNextPlayer(state.getCurrentPlayer()),
            gameOver.isGameOver,
            gameOver.winner,
            turnEndResult.unitBuffs
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
     * 2. Decrease duration of all buffs by 1
     * 3. Remove expired buffs (duration <= 0)
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
