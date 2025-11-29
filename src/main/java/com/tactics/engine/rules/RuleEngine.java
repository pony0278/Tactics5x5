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
 */
public class RuleEngine {

    public RuleEngine() {
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Check if two positions are orthogonally adjacent (exactly 1 tile apart).
     */
    private boolean isAdjacent(Position a, Position b) {
        int dx = b.getX() - a.getX();
        int dy = b.getY() - a.getY();
        return (dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1));
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
    // Validation
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

        // Find units that can move to targetPos
        int moverCount = 0;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                if (isAdjacent(u.getPosition(), targetPos)) {
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

        // Find attackers: alive friendly units adjacent to target
        int attackerCount = 0;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                if (isAdjacent(u.getPosition(), targetUnit.getPosition())) {
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

        // Find mover: unit that can move to targetPos
        Unit mover = null;
        int moverCount = 0;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                if (isAdjacent(u.getPosition(), targetPos)) {
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

        // MA3: After moving, check if target unit is adjacent to new position
        if (!isAdjacent(targetPos, targetUnit.getPosition())) {
            return new ValidationResult(false, "Target not adjacent after movement");
        }

        // MA6: Check for ambiguous attacker after move
        int attackerCountAfterMove = 0;
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                Position unitPos = u.getId().equals(mover.getId()) ? targetPos : u.getPosition();
                if (isAdjacent(unitPos, targetUnit.getPosition())) {
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
    // Apply Action
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
                newUnits.add(new Unit(u.getId(), u.getOwner(), u.getHp(), u.getAttack(), targetPos, u.isAlive()));
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
                newUnits.add(new Unit(u.getId(), u.getOwner(), newHp, u.getAttack(), u.getPosition(), alive));
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
                newUnits.add(new Unit(u.getId(), u.getOwner(), u.getHp(), u.getAttack(), targetPos, u.isAlive()));
            } else if (u.getId().equals(targetUnitId)) {
                newUnits.add(new Unit(u.getId(), u.getOwner(), newHp, u.getAttack(), u.getPosition(), targetAlive));
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
