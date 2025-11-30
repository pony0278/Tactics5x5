package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates and applies actions.
 *
 * V2 Update: Validation now uses unit.moveRange and unit.attackRange
 * for distance checks instead of fixed distance=1.
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
    // Validation (V2: Uses moveRange and attackRange from Unit)
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
     * Validate MOVE action using V2 moveRange logic.
     * Movement must be orthogonal and within unit's moveRange.
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

        // Find units that can move to targetPos using V2 moveRange logic
        // V2: canMoveToPosition checks orthogonal movement AND distance <= moveRange
        int moverCount = 0;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                if (canMoveToPosition(u, targetPos)) {
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

        return new ValidationResult(true, null);
    }

    /**
     * Validate ATTACK action using V2 attackRange logic.
     * Attack must be orthogonal and within attacker's attackRange.
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

        // Find attackers: alive friendly units within attack range of target (V2 logic)
        // V2: canAttackFromPosition checks orthogonal attack AND distance <= attackRange
        int attackerCount = 0;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                if (canAttackFromPosition(u, u.getPosition(), targetUnit.getPosition())) {
                    attackerCount++;
                }
            }
        }

        if (attackerCount == 0) {
            return new ValidationResult(false, "No attacker adjacent to target");
        }

        if (attackerCount > 1) {
            return new ValidationResult(false, "Ambiguous attacker");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Validate MOVE_AND_ATTACK action using V2 moveRange and attackRange logic.
     * MOVE step must be orthogonal and within moveRange.
     * ATTACK step (from post-move position) must be orthogonal and within attackRange.
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

        // Find mover: unit that can move to targetPos using V2 moveRange logic
        Unit mover = null;
        int moverCount = 0;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                if (canMoveToPosition(u, targetPos)) {
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

        // MA3: After moving, check if target unit is within attack range using V2 attackRange logic
        if (!canAttackFromPosition(mover, targetPos, targetUnit.getPosition())) {
            return new ValidationResult(false, "Target not adjacent after movement");
        }

        // MA6: Check for ambiguous attacker after move using V2 attackRange logic
        int attackerCountAfterMove = 0;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                Position unitPos = u.getId().equals(mover.getId()) ? targetPos : u.getPosition();
                if (canAttackFromPosition(u, unitPos, targetUnit.getPosition())) {
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
    // Apply Action (Unchanged - uses V1 isAdjacent for unit finding)
    // =========================================================================

    public GameState applyAction(GameState state, Action action) {
        ActionType type = action.getType();

        if (type == ActionType.END_TURN) {
            return new GameState(
                state.getBoard(),
                state.getUnits(),
                getNextPlayer(state.getCurrentPlayer()),
                state.isGameOver(),
                state.getWinner()
            );
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

    private GameState applyMove(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();

        // Find the unique mover
        Unit mover = null;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                if (isAdjacent(u.getPosition(), targetPos)) {
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

        // MOVE does not switch turn
        return new GameState(
            state.getBoard(),
            newUnits,
            state.getCurrentPlayer(),
            gameOver.isGameOver,
            gameOver.winner
        );
    }

    private GameState applyAttack(GameState state, Action action) {
        String targetUnitId = action.getTargetUnitId();
        Position targetPos = action.getTargetPosition();

        // Find target unit and attacker
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        Unit attacker = null;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                if (isAdjacent(u.getPosition(), targetPos)) {
                    attacker = u;
                    break;
                }
            }
        }

        // Calculate new HP
        int newHp = targetUnit.getHp() - attacker.getAttack();
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

        // ATTACK does not switch turn
        return new GameState(
            state.getBoard(),
            newUnits,
            state.getCurrentPlayer(),
            gameOver.isGameOver,
            gameOver.winner
        );
    }

    private GameState applyMoveAndAttack(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();
        String targetUnitId = action.getTargetUnitId();

        // Find mover
        Unit mover = null;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                if (isAdjacent(u.getPosition(), targetPos)) {
                    mover = u;
                    break;
                }
            }
        }

        // Find target
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);

        // Calculate new HP for target
        int newHp = targetUnit.getHp() - mover.getAttack();
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

        GameOverResult gameOver = checkGameOver(newUnits);

        // MOVE_AND_ATTACK switches turn
        return new GameState(
            state.getBoard(),
            newUnits,
            getNextPlayer(state.getCurrentPlayer()),
            gameOver.isGameOver,
            gameOver.winner
        );
    }
}
