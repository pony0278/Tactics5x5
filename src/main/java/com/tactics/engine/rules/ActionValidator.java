package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;
import com.tactics.engine.skill.SkillDefinition;
import com.tactics.engine.skill.SkillEffect;
import com.tactics.engine.skill.SkillRegistry;
import com.tactics.engine.skill.TargetType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Validates game actions.
 * Extracted from RuleEngine for better code organization.
 *
 * Handles validation for:
 * - MOVE, ATTACK, MOVE_AND_ATTACK
 * - USE_SKILL (with target type validation)
 * - DEATH_CHOICE
 * - END_TURN
 */
public class ActionValidator {

    // =========================================================================
    // Main Validation Entry Point
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

        // V3: Pending death choice blocks all actions except DEATH_CHOICE
        if (state.hasPendingDeathChoice() && action.getType() != ActionType.DEATH_CHOICE) {
            return new ValidationResult(false, "Must resolve pending death choice first");
        }

        // G2: Wrong player turn (for DEATH_CHOICE, the owner must make the choice)
        if (action.getPlayerId() == null ||
            !action.getPlayerId().getValue().equals(state.getCurrentPlayer().getValue())) {
            // Special case: DEATH_CHOICE can be made by the death choice owner, not necessarily current player
            if (action.getType() != ActionType.DEATH_CHOICE) {
                return new ValidationResult(false, "Not your turn");
            }
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

        // V3: DEATH_CHOICE action
        if (type == ActionType.DEATH_CHOICE) {
            return validateDeathChoice(state, action);
        }

        // V3: USE_SKILL action
        if (type == ActionType.USE_SKILL) {
            return validateUseSkill(state, action);
        }

        // G4: Unknown action type
        return new ValidationResult(false, "Invalid action type");
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private int manhattanDistance(Position a, Position b) {
        int dx = Math.abs(b.getX() - a.getX());
        int dy = Math.abs(b.getY() - a.getY());
        return dx + dy;
    }

    private boolean isOrthogonal(Position a, Position b) {
        int dx = Math.abs(b.getX() - a.getX());
        int dy = Math.abs(b.getY() - a.getY());
        return (dx == 0 && dy > 0) || (dx > 0 && dy == 0);
    }

    private boolean canMoveToPositionWithBuffs(Unit unit, Position target, int effectiveMoveRange) {
        Position from = unit.getPosition();
        if (!isOrthogonal(from, target)) {
            return false;
        }
        int distance = manhattanDistance(from, target);
        return distance >= 1 && distance <= effectiveMoveRange;
    }

    private boolean canAttackFromPositionWithBuffs(Position attackerPos, Position targetPos, int effectiveAttackRange) {
        if (!isOrthogonal(attackerPos, targetPos)) {
            return false;
        }
        int distance = manhattanDistance(attackerPos, targetPos);
        return distance >= 1 && distance <= effectiveAttackRange;
    }

    private boolean isInBounds(Position pos, Board board) {
        return pos.getX() >= 0 && pos.getX() < board.getWidth() &&
               pos.getY() >= 0 && pos.getY() < board.getHeight();
    }

    private Unit findUnitById(List<Unit> units, String unitId) {
        for (Unit u : units) {
            if (u.getId().equals(unitId)) {
                return u;
            }
        }
        return null;
    }

    private boolean isTileOccupied(List<Unit> units, Position pos) {
        for (Unit u : units) {
            if (u.isAlive() && u.getPosition().getX() == pos.getX() &&
                u.getPosition().getY() == pos.getY()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasObstacleAt(GameState state, Position pos) {
        return state.hasObstacleAt(pos);
    }

    private boolean isTileBlocked(GameState state, Position pos) {
        return isTileOccupied(state.getUnits(), pos) || hasObstacleAt(state, pos);
    }

    // =========================================================================
    // Buff Helper Methods
    // =========================================================================

    private List<BuffInstance> getBuffsForUnit(GameState state, String unitId) {
        Map<String, List<BuffInstance>> unitBuffs = state.getUnitBuffs();
        if (unitBuffs == null) {
            return Collections.emptyList();
        }
        List<BuffInstance> buffs = unitBuffs.get(unitId);
        return buffs != null ? buffs : Collections.emptyList();
    }

    private boolean isUnitStunned(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isStunned()) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnitRooted(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isRooted()) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnitBlinded(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isBlindBuff()) {
                return true;
            }
        }
        return false;
    }

    private int getEffectiveMoveRange(Unit unit, List<BuffInstance> buffs) {
        int bonus = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getModifiers() != null) {
                bonus += buff.getModifiers().getBonusMoveRange();
            }
        }
        return unit.getMoveRange() + bonus;
    }

    private int getEffectiveAttackRange(Unit unit, List<BuffInstance> buffs) {
        int bonus = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getModifiers() != null) {
                bonus += buff.getModifiers().getBonusAttackRange();
            }
        }
        return unit.getAttackRange() + bonus;
    }

    private boolean hasPowerBuff(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isPowerBuff()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSpeedBuff(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isSpeedBuff()) {
                return true;
            }
        }
        return false;
    }

    private int getMaxActions(List<BuffInstance> buffs) {
        return hasSpeedBuff(buffs) ? 2 : 1;
    }

    private boolean canUnitAct(Unit unit, List<BuffInstance> buffs) {
        int maxActions = getMaxActions(buffs);
        return unit.getActionsUsed() < maxActions;
    }

    // =========================================================================
    // V3 Action Validation Methods
    // =========================================================================

    private ValidationResult validateDeathChoice(GameState state, Action action) {
        if (!state.hasPendingDeathChoice()) {
            return new ValidationResult(false, "No pending death choice");
        }

        if (!state.getPendingDeathChoice().getOwner().getValue().equals(action.getPlayerId().getValue())) {
            return new ValidationResult(false, "Not your death choice");
        }

        if (action.getDeathChoiceType() == null) {
            return new ValidationResult(false, "Death choice type is required");
        }

        return new ValidationResult(true, null);
    }

    private ValidationResult validateUseSkill(GameState state, Action action) {
        String actingUnitId = action.getActingUnitId();
        Position targetPos = action.getTargetPosition();
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();

        if (actingUnitId == null) {
            return new ValidationResult(false, "Acting unit ID is required for USE_SKILL");
        }

        Unit actingUnit = findUnitById(state.getUnits(), actingUnitId);
        if (actingUnit == null) {
            return new ValidationResult(false, "Acting unit not found");
        }

        if (!actingUnit.isAlive()) {
            return new ValidationResult(false, "Acting unit is dead");
        }

        if (!actingUnit.getOwner().getValue().equals(action.getPlayerId().getValue())) {
            return new ValidationResult(false, "Cannot control opponent's unit");
        }

        if (actingUnit.getCategory() != UnitCategory.HERO) {
            return new ValidationResult(false, "Only Heroes can use skills");
        }

        String skillId = actingUnit.getSelectedSkillId();
        if (skillId == null || skillId.isEmpty()) {
            return new ValidationResult(false, "Hero has no skill selected");
        }

        SkillDefinition skill = SkillRegistry.getSkill(skillId);
        if (skill == null) {
            return new ValidationResult(false, "Invalid skill ID: " + skillId);
        }

        if (actingUnit.getHeroClass() != skill.getHeroClass()) {
            return new ValidationResult(false, "Hero class cannot use this skill");
        }

        if (actingUnit.getSkillCooldown() > 0) {
            return new ValidationResult(false, "Skill is on cooldown (" + actingUnit.getSkillCooldown() + " rounds remaining)");
        }

        List<BuffInstance> buffs = getBuffsForUnit(state, actingUnitId);
        if (!canUnitAct(actingUnit, buffs)) {
            return new ValidationResult(false, "Unit has no remaining actions this turn");
        }

        if (isUnitStunned(buffs)) {
            return new ValidationResult(false, "Stunned units cannot use skills");
        }

        return validateSkillTarget(state, action, actingUnit, skill, targetPos, targetUnitId);
    }

    private ValidationResult validateSkillTarget(GameState state, Action action, Unit actingUnit,
                                                   SkillDefinition skill, Position targetPos, String targetUnitId) {
        TargetType targetType = skill.getTargetType();

        switch (targetType) {
            case SELF:
                return new ValidationResult(true, null);

            case SINGLE_ENEMY:
                return validateSingleEnemyTarget(state, actingUnit, skill, targetUnitId);

            case SINGLE_ALLY:
                return validateSingleAllyTarget(state, actingUnit, skill, targetUnitId);

            case SINGLE_TILE:
                return validateSingleTileTarget(state, actingUnit, skill, targetPos);

            case AREA_AROUND_SELF:
                return new ValidationResult(true, null);

            case ALL_ENEMIES:
            case ALL_ALLIES:
                return new ValidationResult(true, null);

            case LINE:
                return validateLineTarget(state, actingUnit, skill, targetPos);

            case AREA_AROUND_TARGET:
                return validateAreaAroundTarget(state, actingUnit, skill, targetPos);

            default:
                return new ValidationResult(false, "Unknown target type: " + targetType);
        }
    }

    private ValidationResult validateSingleEnemyTarget(GameState state, Unit actingUnit,
                                                         SkillDefinition skill, String targetUnitId) {
        if (targetUnitId == null) {
            return new ValidationResult(false, "Target unit ID is required for this skill");
        }

        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        if (targetUnit == null) {
            return new ValidationResult(false, "Target unit not found");
        }

        if (!targetUnit.isAlive()) {
            return new ValidationResult(false, "Target unit is dead");
        }

        if (targetUnit.getOwner().getValue().equals(actingUnit.getOwner().getValue())) {
            return new ValidationResult(false, "Target must be an enemy unit");
        }

        int distance = manhattanDistance(actingUnit.getPosition(), targetUnit.getPosition());
        if (distance > skill.getRange()) {
            return new ValidationResult(false, "Target is out of range (range: " + skill.getRange() + ")");
        }

        return new ValidationResult(true, null);
    }

    private ValidationResult validateSingleAllyTarget(GameState state, Unit actingUnit,
                                                        SkillDefinition skill, String targetUnitId) {
        if (targetUnitId == null) {
            return new ValidationResult(false, "Target unit ID is required for this skill");
        }

        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        if (targetUnit == null) {
            return new ValidationResult(false, "Target unit not found");
        }

        if (!targetUnit.isAlive()) {
            return new ValidationResult(false, "Target unit is dead");
        }

        if (!targetUnit.getOwner().getValue().equals(actingUnit.getOwner().getValue())) {
            return new ValidationResult(false, "Target must be a friendly unit");
        }

        int distance = manhattanDistance(actingUnit.getPosition(), targetUnit.getPosition());
        if (distance > skill.getRange()) {
            return new ValidationResult(false, "Target is out of range (range: " + skill.getRange() + ")");
        }

        return new ValidationResult(true, null);
    }

    private ValidationResult validateSingleTileTarget(GameState state, Unit actingUnit,
                                                        SkillDefinition skill, Position targetPos) {
        // Special handling for Warp Beacon
        if (skill.getSkillId().equals(SkillRegistry.MAGE_WARP_BEACON)) {
            return validateWarpBeaconTarget(state, actingUnit, skill, targetPos);
        }

        if (targetPos == null) {
            return new ValidationResult(false, "Target position is required for this skill");
        }

        if (!isInBounds(targetPos, state.getBoard())) {
            return new ValidationResult(false, "Target position is outside the board");
        }

        int distance = manhattanDistance(actingUnit.getPosition(), targetPos);
        if (distance > skill.getRange()) {
            return new ValidationResult(false, "Target is out of range (range: " + skill.getRange() + ")");
        }

        if (skill.getEffects().contains(SkillEffect.MOVE_SELF)) {
            if (isTileBlocked(state, targetPos)) {
                return new ValidationResult(false, "Target tile is blocked");
            }
        }

        return new ValidationResult(true, null);
    }

    private ValidationResult validateWarpBeaconTarget(GameState state, Unit actingUnit,
                                                       SkillDefinition skill, Position targetPos) {
        Map<String, Object> skillState = actingUnit.getSkillState();
        boolean hasBeacon = skillState != null && skillState.containsKey("beacon_x");

        if (!hasBeacon) {
            if (targetPos == null) {
                return new ValidationResult(false, "Target position is required to place beacon");
            }

            if (!isInBounds(targetPos, state.getBoard())) {
                return new ValidationResult(false, "Target position is outside the board");
            }

            int distance = manhattanDistance(actingUnit.getPosition(), targetPos);
            if (distance > skill.getRange()) {
                return new ValidationResult(false, "Target is out of range (range: " + skill.getRange() + ")");
            }

            if (isTileBlocked(state, targetPos)) {
                return new ValidationResult(false, "Cannot place beacon on blocked tile");
            }

            return new ValidationResult(true, null);
        } else {
            int beaconX = (Integer) skillState.get("beacon_x");
            int beaconY = (Integer) skillState.get("beacon_y");
            Position beaconPos = new Position(beaconX, beaconY);

            if (isTileBlocked(state, beaconPos)) {
                return new ValidationResult(false, "Cannot teleport - beacon position is blocked");
            }

            return new ValidationResult(true, null);
        }
    }

    private ValidationResult validateLineTarget(GameState state, Unit actingUnit,
                                                  SkillDefinition skill, Position targetPos) {
        if (targetPos == null) {
            return new ValidationResult(false, "Target position is required for LINE skill");
        }

        if (!isInBounds(targetPos, state.getBoard())) {
            return new ValidationResult(false, "Target position is outside the board");
        }

        if (!isOrthogonal(actingUnit.getPosition(), targetPos)) {
            return new ValidationResult(false, "Target must be in a straight line");
        }

        int distance = manhattanDistance(actingUnit.getPosition(), targetPos);
        if (distance > skill.getRange()) {
            return new ValidationResult(false, "Target is out of range (range: " + skill.getRange() + ")");
        }

        return new ValidationResult(true, null);
    }

    private ValidationResult validateAreaAroundTarget(GameState state, Unit actingUnit,
                                                        SkillDefinition skill, Position targetPos) {
        if (targetPos == null) {
            return new ValidationResult(false, "Target position is required for this skill");
        }

        if (!isInBounds(targetPos, state.getBoard())) {
            return new ValidationResult(false, "Target position is outside the board");
        }

        int distance = manhattanDistance(actingUnit.getPosition(), targetPos);
        if (distance > skill.getRange()) {
            return new ValidationResult(false, "Target is out of range (range: " + skill.getRange() + ")");
        }

        return new ValidationResult(true, null);
    }

    // =========================================================================
    // V1/V2 Action Validation Methods
    // =========================================================================

    private ValidationResult validateMove(GameState state, Action action) {
        if (action.getTargetUnitId() != null) {
            return new ValidationResult(false, "MOVE must not specify targetUnitId");
        }

        Position targetPos = action.getTargetPosition();
        if (targetPos == null) {
            return new ValidationResult(false, "Target position is required for MOVE");
        }

        if (!isInBounds(targetPos, state.getBoard())) {
            return new ValidationResult(false, "Target position is outside the board");
        }

        if (isTileBlocked(state, targetPos)) {
            return new ValidationResult(false, "Target tile is occupied");
        }

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

        Unit mover = potentialMovers.get(0);
        List<BuffInstance> moverBuffs = getBuffsForUnit(state, mover.getId());

        if (!canUnitAct(mover, moverBuffs)) {
            return new ValidationResult(false, "Unit has no remaining actions this turn");
        }

        if (isUnitStunned(moverBuffs)) {
            return new ValidationResult(false, "Unit is stunned");
        }

        if (isUnitRooted(moverBuffs)) {
            return new ValidationResult(false, "Unit is rooted");
        }

        return new ValidationResult(true, null);
    }

    private ValidationResult validateAttack(GameState state, Action action) {
        if (action.getTargetPosition() == null) {
            return new ValidationResult(false, "Target position is required for ATTACK");
        }

        Position targetPos = action.getTargetPosition();
        String targetUnitId = action.getTargetUnitId();

        boolean isAttackingObstacle = false;
        if (targetUnitId != null && targetUnitId.startsWith(com.tactics.engine.model.Obstacle.ID_PREFIX)) {
            isAttackingObstacle = true;
        } else if (targetUnitId == null) {
            if (state.getObstacleAt(targetPos) != null) {
                isAttackingObstacle = true;
            } else {
                return new ValidationResult(false, "Target unit ID is required for ATTACK");
            }
        }

        if (isAttackingObstacle) {
            com.tactics.engine.model.Obstacle obstacle = state.getObstacleAt(targetPos);
            if (obstacle == null) {
                return new ValidationResult(false, "No obstacle at target position");
            }
        } else {
            Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);

            if (targetUnit == null) {
                return new ValidationResult(false, "Target unit not found");
            }

            if (!targetUnit.isAlive()) {
                return new ValidationResult(false, "Target unit is dead");
            }

            if (targetUnit.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                return new ValidationResult(false, "Cannot attack own unit");
            }

            if (targetUnit.getPosition().getX() != targetPos.getX() ||
                targetUnit.getPosition().getY() != targetPos.getY()) {
                return new ValidationResult(false, "Target position does not match target unit position");
            }
        }

        List<Unit> potentialAttackers = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(action.getPlayerId().getValue())) {
                List<BuffInstance> buffs = getBuffsForUnit(state, u.getId());
                int effectiveAttackRange = getEffectiveAttackRange(u, buffs);
                if (canAttackFromPositionWithBuffs(u.getPosition(), targetPos, effectiveAttackRange)) {
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

        Unit attacker = potentialAttackers.get(0);
        List<BuffInstance> attackerBuffs = getBuffsForUnit(state, attacker.getId());

        if (!canUnitAct(attacker, attackerBuffs)) {
            return new ValidationResult(false, "Unit has no remaining actions this turn");
        }

        if (isUnitStunned(attackerBuffs)) {
            return new ValidationResult(false, "Unit is stunned");
        }

        if (isUnitBlinded(attackerBuffs)) {
            return new ValidationResult(false, "Unit is blinded and cannot attack");
        }

        if (!isAttackingObstacle) {
            Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
            if (targetUnit != null && targetUnit.isInvisible()) {
                return new ValidationResult(false, "Cannot target invisible unit");
            }
        }

        return new ValidationResult(true, null);
    }

    private ValidationResult validateMoveAndAttack(GameState state, Action action) {
        if (action.getTargetPosition() == null) {
            return new ValidationResult(false, "Target position is required for MOVE_AND_ATTACK");
        }

        if (action.getTargetUnitId() == null) {
            return new ValidationResult(false, "Target unit ID is required for MOVE_AND_ATTACK");
        }

        Position targetPos = action.getTargetPosition();
        String targetUnitId = action.getTargetUnitId();

        if (!isInBounds(targetPos, state.getBoard())) {
            return new ValidationResult(false, "Target position is outside the board");
        }

        if (isTileBlocked(state, targetPos)) {
            return new ValidationResult(false, "Target tile is occupied");
        }

        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);

        if (targetUnit == null) {
            return new ValidationResult(false, "Target unit not found");
        }

        if (!targetUnit.isAlive()) {
            return new ValidationResult(false, "Target unit is dead");
        }

        if (targetUnit.getOwner().getValue().equals(action.getPlayerId().getValue())) {
            return new ValidationResult(false, "Cannot attack own unit");
        }

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

        List<BuffInstance> moverBuffs = getBuffsForUnit(state, mover.getId());

        if (!canUnitAct(mover, moverBuffs)) {
            return new ValidationResult(false, "Unit has no remaining actions this turn");
        }

        if (isUnitStunned(moverBuffs)) {
            return new ValidationResult(false, "Unit is stunned");
        }

        if (isUnitRooted(moverBuffs)) {
            return new ValidationResult(false, "Unit is rooted");
        }

        if (isUnitBlinded(moverBuffs)) {
            return new ValidationResult(false, "Unit is blinded and cannot attack");
        }

        if (hasPowerBuff(moverBuffs)) {
            return new ValidationResult(false, "Unit cannot use MOVE_AND_ATTACK with Power buff");
        }

        if (targetUnit.isInvisible()) {
            return new ValidationResult(false, "Cannot target invisible unit");
        }

        int effectiveAttackRange = getEffectiveAttackRange(mover, moverBuffs);
        if (!canAttackFromPositionWithBuffs(targetPos, targetUnit.getPosition(), effectiveAttackRange)) {
            return new ValidationResult(false, "Target not adjacent after movement");
        }

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
}
