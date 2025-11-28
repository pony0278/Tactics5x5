package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

/**
 * Validates and applies actions.
 */
public class RuleEngine {

    public RuleEngine() {
    }

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
            // ET1: END_TURN is always valid if game not over and correct player
            return new ValidationResult(true, null);
        }

        if (type == ActionType.MOVE) {
            // M8: MOVE with targetUnitId non-null is protocol misuse
            if (action.getTargetUnitId() != null) {
                return new ValidationResult(false, "MOVE must not specify targetUnitId");
            }

            // M1-M7: Validate MOVE
            Position targetPos = action.getTargetPosition();
            if (targetPos == null) {
                return new ValidationResult(false, "Target position is required for MOVE");
            }

            Board board = state.getBoard();

            // M2, M3: Check bounds
            if (targetPos.getX() < 0 || targetPos.getX() >= board.getWidth() ||
                targetPos.getY() < 0 || targetPos.getY() >= board.getHeight()) {
                return new ValidationResult(false, "Target position is outside the board");
            }

            // M6: Check if target tile is occupied by any alive unit
            for (Unit u : state.getUnits()) {
                if (u.isAlive() && u.getPosition().getX() == targetPos.getX() &&
                    u.getPosition().getY() == targetPos.getY()) {
                    return new ValidationResult(false, "Target tile is occupied");
                }
            }

            // Find the unit that can move to targetPos (must be unique, alive, owned by player, adjacent)
            Unit mover = null;
            int moverCount = 0;
            for (Unit u : state.getUnits()) {
                if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                    int dx = targetPos.getX() - u.getPosition().getX();
                    int dy = targetPos.getY() - u.getPosition().getY();
                    // M4, M5: Exactly 1 tile orthogonal
                    if ((dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1))) {
                        mover = u;
                        moverCount++;
                    }
                }
            }

            if (moverCount == 0) {
                // E1, M7: No valid unit can perform the move (could be dead or no unit adjacent)
                return new ValidationResult(false, "No valid unit can move to target position");
            }

            if (moverCount > 1) {
                return new ValidationResult(false, "Ambiguous move");
            }

            // M7 is implicitly handled: dead units are filtered by isAlive() check
            return new ValidationResult(true, null);
        }

        if (type == ActionType.ATTACK) {
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

            // Find target unit by ID
            Unit targetUnit = null;
            for (Unit u : state.getUnits()) {
                if (u.getId().equals(targetUnitId)) {
                    targetUnit = u;
                    break;
                }
            }

            // A6: Target missing
            if (targetUnit == null) {
                return new ValidationResult(false, "Target unit not found");
            }

            // A6: Target dead
            if (!targetUnit.isAlive()) {
                return new ValidationResult(false, "Target unit is dead");
            }

            // A4: Target belongs to acting player (friendly fire)
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
                    int dx = targetUnit.getPosition().getX() - u.getPosition().getX();
                    int dy = targetUnit.getPosition().getY() - u.getPosition().getY();
                    // A2, A3: Must be exactly 1 tile orthogonal
                    if ((dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1))) {
                        attackerCount++;
                    }
                }
            }

            // A2, A3: Target not adjacent (no friendly unit is adjacent to target)
            if (attackerCount == 0) {
                return new ValidationResult(false, "No attacker adjacent to target");
            }

            // A9: Ambiguous attacker
            if (attackerCount > 1) {
                return new ValidationResult(false, "Ambiguous attacker");
            }

            // A5 is implicitly handled: dead attackers filtered by isAlive()
            return new ValidationResult(true, null);
        }

        if (type == ActionType.MOVE_AND_ATTACK) {
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
            Board board = state.getBoard();

            // Validate MOVE part: bounds check
            if (targetPos.getX() < 0 || targetPos.getX() >= board.getWidth() ||
                targetPos.getY() < 0 || targetPos.getY() >= board.getHeight()) {
                return new ValidationResult(false, "Target position is outside the board");
            }

            // Check if target tile is occupied
            for (Unit u : state.getUnits()) {
                if (u.isAlive() && u.getPosition().getX() == targetPos.getX() &&
                    u.getPosition().getY() == targetPos.getY()) {
                    return new ValidationResult(false, "Target tile is occupied");
                }
            }

            // Find target unit by ID
            Unit targetUnit = null;
            for (Unit u : state.getUnits()) {
                if (u.getId().equals(targetUnitId)) {
                    targetUnit = u;
                    break;
                }
            }

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
                    int dx = targetPos.getX() - u.getPosition().getX();
                    int dy = targetPos.getY() - u.getPosition().getY();
                    // MA2: Must be exactly 1 tile orthogonal
                    if ((dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1))) {
                        mover = u;
                        moverCount++;
                    }
                }
            }

            // MA7, MA2: No valid mover
            if (moverCount == 0) {
                return new ValidationResult(false, "No valid unit can move to target position");
            }

            if (moverCount > 1) {
                return new ValidationResult(false, "Ambiguous move");
            }

            // MA3: After moving, check if target unit is adjacent to new position
            int dxToTarget = targetUnit.getPosition().getX() - targetPos.getX();
            int dyToTarget = targetUnit.getPosition().getY() - targetPos.getY();
            boolean targetAdjacent = (dxToTarget == 0 && (dyToTarget == 1 || dyToTarget == -1)) ||
                                     (dyToTarget == 0 && (dxToTarget == 1 || dxToTarget == -1));

            if (!targetAdjacent) {
                return new ValidationResult(false, "Target not adjacent after movement");
            }

            // MA6: Check for ambiguous attacker after move
            // Count how many friendly alive units would be adjacent to target after the move
            int attackerCountAfterMove = 0;
            for (Unit u : state.getUnits()) {
                if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                    int ux, uy;
                    if (u.getId().equals(mover.getId())) {
                        // This is the mover, use new position
                        ux = targetPos.getX();
                        uy = targetPos.getY();
                    } else {
                        ux = u.getPosition().getX();
                        uy = u.getPosition().getY();
                    }
                    int dx = targetUnit.getPosition().getX() - ux;
                    int dy = targetUnit.getPosition().getY() - uy;
                    if ((dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1))) {
                        attackerCountAfterMove++;
                    }
                }
            }

            if (attackerCountAfterMove > 1) {
                return new ValidationResult(false, "Ambiguous attacker after movement");
            }

            return new ValidationResult(true, null);
        }

        // G4: Unknown action type (should not reach here if enum is used properly)
        return new ValidationResult(false, "Invalid action type");
    }

    public GameState applyAction(GameState state, Action action) {
        ActionType type = action.getType();

        if (type == ActionType.END_TURN) {
            // Switch current player
            String current = state.getCurrentPlayer().getValue();
            String next = current.equals("P1") ? "P2" : "P1";
            return new GameState(
                state.getBoard(),
                state.getUnits(),
                new com.tactics.engine.model.PlayerId(next),
                state.isGameOver(),
                state.getWinner()
            );
        }

        if (type == ActionType.MOVE) {
            Position targetPos = action.getTargetPosition();

            // Find the unique mover (alive friendly unit adjacent to target)
            Unit mover = null;
            for (Unit u : state.getUnits()) {
                if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                    int dx = targetPos.getX() - u.getPosition().getX();
                    int dy = targetPos.getY() - u.getPosition().getY();
                    if ((dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1))) {
                        mover = u;
                        break;
                    }
                }
            }

            // Create new units list with updated position
            java.util.List<Unit> newUnits = new java.util.ArrayList<>();
            for (Unit u : state.getUnits()) {
                if (u.getId().equals(mover.getId())) {
                    newUnits.add(new Unit(u.getId(), u.getOwner(), u.getHp(), u.getAttack(), targetPos, u.isAlive()));
                } else {
                    newUnits.add(u);
                }
            }

            // Check game over (no change expected from MOVE, but be consistent)
            boolean isGameOver = false;
            com.tactics.engine.model.PlayerId winner = null;
            boolean p1HasAlive = false;
            boolean p2HasAlive = false;
            for (Unit u : newUnits) {
                if (u.isAlive()) {
                    if (u.getOwner().getValue().equals("P1")) {
                        p1HasAlive = true;
                    } else {
                        p2HasAlive = true;
                    }
                }
            }
            if (!p1HasAlive) {
                isGameOver = true;
                winner = new com.tactics.engine.model.PlayerId("P2");
            } else if (!p2HasAlive) {
                isGameOver = true;
                winner = new com.tactics.engine.model.PlayerId("P1");
            }

            // MOVE does not switch turn
            return new GameState(
                state.getBoard(),
                newUnits,
                state.getCurrentPlayer(),
                isGameOver,
                winner
            );
        }

        if (type == ActionType.ATTACK) {
            String targetUnitId = action.getTargetUnitId();
            Position targetPos = action.getTargetPosition();

            // Find target unit and attacker
            Unit targetUnit = null;
            Unit attacker = null;
            for (Unit u : state.getUnits()) {
                if (u.getId().equals(targetUnitId)) {
                    targetUnit = u;
                }
                if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                    int dx = targetPos.getX() - u.getPosition().getX();
                    int dy = targetPos.getY() - u.getPosition().getY();
                    if ((dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1))) {
                        attacker = u;
                    }
                }
            }

            // Calculate new HP
            int newHp = targetUnit.getHp() - attacker.getAttack();
            boolean alive = newHp > 0;

            // Create new units list
            java.util.List<Unit> newUnits = new java.util.ArrayList<>();
            for (Unit u : state.getUnits()) {
                if (u.getId().equals(targetUnitId)) {
                    newUnits.add(new Unit(u.getId(), u.getOwner(), newHp, u.getAttack(), u.getPosition(), alive));
                } else {
                    newUnits.add(u);
                }
            }

            // Check game over
            boolean isGameOver = false;
            com.tactics.engine.model.PlayerId winner = null;
            boolean p1HasAlive = false;
            boolean p2HasAlive = false;
            for (Unit u : newUnits) {
                if (u.isAlive()) {
                    if (u.getOwner().getValue().equals("P1")) {
                        p1HasAlive = true;
                    } else {
                        p2HasAlive = true;
                    }
                }
            }
            if (!p1HasAlive) {
                isGameOver = true;
                winner = new com.tactics.engine.model.PlayerId("P2");
            } else if (!p2HasAlive) {
                isGameOver = true;
                winner = new com.tactics.engine.model.PlayerId("P1");
            }

            // ATTACK does not switch turn
            return new GameState(
                state.getBoard(),
                newUnits,
                state.getCurrentPlayer(),
                isGameOver,
                winner
            );
        }

        if (type == ActionType.MOVE_AND_ATTACK) {
            Position targetPos = action.getTargetPosition();
            String targetUnitId = action.getTargetUnitId();

            // Find mover
            Unit mover = null;
            for (Unit u : state.getUnits()) {
                if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                    int dx = targetPos.getX() - u.getPosition().getX();
                    int dy = targetPos.getY() - u.getPosition().getY();
                    if ((dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1))) {
                        mover = u;
                        break;
                    }
                }
            }

            // Find target
            Unit targetUnit = null;
            for (Unit u : state.getUnits()) {
                if (u.getId().equals(targetUnitId)) {
                    targetUnit = u;
                    break;
                }
            }

            // Calculate new HP for target
            int newHp = targetUnit.getHp() - mover.getAttack();
            boolean targetAlive = newHp > 0;

            // Create new units list with mover at new position and target with new HP
            java.util.List<Unit> newUnits = new java.util.ArrayList<>();
            for (Unit u : state.getUnits()) {
                if (u.getId().equals(mover.getId())) {
                    newUnits.add(new Unit(u.getId(), u.getOwner(), u.getHp(), u.getAttack(), targetPos, u.isAlive()));
                } else if (u.getId().equals(targetUnitId)) {
                    newUnits.add(new Unit(u.getId(), u.getOwner(), newHp, u.getAttack(), u.getPosition(), targetAlive));
                } else {
                    newUnits.add(u);
                }
            }

            // Check game over
            boolean isGameOver = false;
            com.tactics.engine.model.PlayerId winner = null;
            boolean p1HasAlive = false;
            boolean p2HasAlive = false;
            for (Unit u : newUnits) {
                if (u.isAlive()) {
                    if (u.getOwner().getValue().equals("P1")) {
                        p1HasAlive = true;
                    } else {
                        p2HasAlive = true;
                    }
                }
            }
            if (!p1HasAlive) {
                isGameOver = true;
                winner = new com.tactics.engine.model.PlayerId("P2");
            } else if (!p2HasAlive) {
                isGameOver = true;
                winner = new com.tactics.engine.model.PlayerId("P1");
            }

            // MOVE_AND_ATTACK switches turn
            String current = state.getCurrentPlayer().getValue();
            String next = current.equals("P1") ? "P2" : "P1";
            return new GameState(
                state.getBoard(),
                newUnits,
                new com.tactics.engine.model.PlayerId(next),
                isGameOver,
                winner
            );
        }

        // Should not reach here if validation passed
        return null;
    }
}
