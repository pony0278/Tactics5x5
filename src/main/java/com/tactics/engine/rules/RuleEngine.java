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
import com.tactics.engine.model.HeroClass;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;
import com.tactics.engine.skill.SkillDefinition;
import com.tactics.engine.skill.SkillEffect;
import com.tactics.engine.skill.SkillRegistry;
import com.tactics.engine.skill.TargetType;
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
     * Functional interface for unit transformation.
     */
    @FunctionalInterface
    private interface UnitTransformer {
        Unit transform(Unit unit);
    }

    /**
     * Create a new list with a specific unit transformed.
     * Returns a new list where the unit with matching ID is replaced by the transformation result.
     */
    private List<Unit> updateUnitInList(List<Unit> units, String unitId, UnitTransformer transformer) {
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

    /**
     * Create a new list with multiple units transformed.
     * Returns a new list where units with matching IDs are replaced by transformation results.
     */
    private List<Unit> updateUnitsInList(List<Unit> units, java.util.Map<String, UnitTransformer> transformers) {
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
     * Get the next player ID (simple alternation).
     */
    private PlayerId getNextPlayer(PlayerId current) {
        return current.isPlayer1() ? PlayerId.PLAYER_2 : PlayerId.PLAYER_1;
    }

    /**
     * V3 Exhaustion Rule: Get the next player who can act.
     * If opponent has no unused units, current player continues.
     * A unit is "unused" if it is alive and has actionsUsed == 0.
     *
     * @param state current game state
     * @param currentActingPlayer the player whose turn just ended
     * @return the next player who should act
     */
    private PlayerId getNextActingPlayer(GameState state, PlayerId currentActingPlayer) {
        PlayerId opponent = getNextPlayer(currentActingPlayer);

        // Check if opponent has any unused units
        boolean opponentHasUnusedUnits = hasUnusedUnits(state, opponent);

        if (opponentHasUnusedUnits) {
            // Normal alternation
            return opponent;
        }

        // Check if current player still has unused units
        boolean currentHasUnusedUnits = hasUnusedUnits(state, currentActingPlayer);

        if (currentHasUnusedUnits) {
            // Exhaustion rule: current player continues
            return currentActingPlayer;
        }

        // Both sides exhausted - return opponent (round will end)
        return opponent;
    }

    /**
     * Check if a player has any unused units (alive and actionsUsed == 0).
     */
    private boolean hasUnusedUnits(GameState state, PlayerId player) {
        for (Unit u : state.getUnits()) {
            if (u.isAlive() &&
                u.getOwner().getValue().equals(player.getValue()) &&
                u.getActionsUsed() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if all units have used their actions (round should end).
     */
    private boolean allUnitsActed(GameState state) {
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getActionsUsed() == 0) {
                return false;
            }
        }
        return true;
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
     * This version does not handle simultaneous death rule.
     */
    private GameOverResult checkGameOver(List<Unit> units) {
        return checkGameOver(units, null);
    }

    /**
     * Check if the game is over based on unit states.
     * Game ends when one player has no alive units.
     * V3: If both players have no alive units (simultaneous death),
     * the active player (attacker) wins.
     *
     * @param units the list of units to check
     * @param activePlayer the player who initiated the action (for simultaneous death rule)
     */
    private GameOverResult checkGameOver(List<Unit> units, PlayerId activePlayer) {
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
                // Active player (attacker) wins on simultaneous death
                return new GameOverResult(true, activePlayer);
            }
            // Fallback: if no active player specified, P1 wins (shouldn't happen in normal play)
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
     * Check if unit has POWER buff (blocks MOVE_AND_ATTACK, instant obstacle destroy).
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
                    newUnits.add(u.withHpBonus(newBuff.getInstantHpBonus()));
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
    // V3 Guardian Passive Helper Methods
    // =========================================================================

    /**
     * Find the Guardian (TANK) that will intercept damage for the target unit.
     * Returns null if no Guardian is available.
     *
     * Guardian rules:
     * - Must be a TANK minion type
     * - Must be alive
     * - Must be same owner as target (friendly)
     * - Must be adjacent to target (Manhattan distance 1)
     * - Cannot protect itself
     * - If multiple TANKs qualify, lowest unit ID intercepts
     */
    private Unit findGuardian(GameState state, Unit target) {
        if (target == null) {
            return null;
        }

        Unit guardian = null;

        for (Unit u : state.getUnits()) {
            // Must be alive
            if (!u.isAlive()) {
                continue;
            }
            // Must be same owner (friendly)
            if (!u.getOwner().getValue().equals(target.getOwner().getValue())) {
                continue;
            }
            // Must be TANK minion
            if (u.getMinionType() != MinionType.TANK) {
                continue;
            }
            // Cannot protect itself
            if (u.getId().equals(target.getId())) {
                continue;
            }
            // Must be adjacent
            if (!isAdjacent(u.getPosition(), target.getPosition())) {
                continue;
            }

            // Found a valid guardian - check if it has lower ID than current
            if (guardian == null || u.getId().compareTo(guardian.getId()) < 0) {
                guardian = u;
            }
        }

        return guardian;
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
    // V3 Action Validation Methods
    // =========================================================================

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

    /**
     * Validate USE_SKILL action (V3).
     * Requires:
     * - Acting unit is a Hero with a selected skill
     * - Skill is not on cooldown
     * - Target is valid for the skill type
     * - Unit can act this turn
     */
    private ValidationResult validateUseSkill(GameState state, Action action) {
        String actingUnitId = action.getActingUnitId();
        Position targetPos = action.getTargetPosition();
        // V3: Skills use skillTargetUnitId for targeting (from Action.useSkill() factory)
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();

        // Need acting unit ID
        if (actingUnitId == null) {
            return new ValidationResult(false, "Acting unit ID is required for USE_SKILL");
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

        // Must be a Hero to use skills
        if (actingUnit.getCategory() != UnitCategory.HERO) {
            return new ValidationResult(false, "Only Heroes can use skills");
        }

        // Hero must have a selected skill
        String skillId = actingUnit.getSelectedSkillId();
        if (skillId == null || skillId.isEmpty()) {
            return new ValidationResult(false, "Hero has no skill selected");
        }

        // Get skill definition
        SkillDefinition skill = SkillRegistry.getSkill(skillId);
        if (skill == null) {
            return new ValidationResult(false, "Invalid skill ID: " + skillId);
        }

        // Verify Hero class matches skill's required class
        if (actingUnit.getHeroClass() != skill.getHeroClass()) {
            return new ValidationResult(false, "Hero class cannot use this skill");
        }

        // Check cooldown
        if (actingUnit.getSkillCooldown() > 0) {
            return new ValidationResult(false, "Skill is on cooldown (" + actingUnit.getSkillCooldown() + " rounds remaining)");
        }

        // Check if unit can act (SPEED buff allows 2 actions, STUN prevents actions)
        List<BuffInstance> buffs = getBuffsForUnit(state, actingUnitId);
        if (!canUnitAct(actingUnit, buffs)) {
            return new ValidationResult(false, "Unit has no remaining actions this turn");
        }

        // Check if unit is stunned
        if (isUnitStunned(buffs)) {
            return new ValidationResult(false, "Stunned units cannot use skills");
        }

        // Validate target based on skill's target type
        return validateSkillTarget(state, action, actingUnit, skill, targetPos, targetUnitId);
    }

    /**
     * Validate skill target based on skill's target type.
     */
    private ValidationResult validateSkillTarget(GameState state, Action action, Unit actingUnit,
                                                   SkillDefinition skill, Position targetPos, String targetUnitId) {
        TargetType targetType = skill.getTargetType();

        switch (targetType) {
            case SELF:
                // No target needed, always valid
                return new ValidationResult(true, null);

            case SINGLE_ENEMY:
                return validateSingleEnemyTarget(state, actingUnit, skill, targetUnitId);

            case SINGLE_ALLY:
                return validateSingleAllyTarget(state, actingUnit, skill, targetUnitId);

            case SINGLE_TILE:
                return validateSingleTileTarget(state, actingUnit, skill, targetPos);

            case AREA_AROUND_SELF:
                // No specific target needed, affects area around caster
                return new ValidationResult(true, null);

            case ALL_ENEMIES:
            case ALL_ALLIES:
                // No specific target needed, affects all matching units
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

        // Must be an enemy
        if (targetUnit.getOwner().getValue().equals(actingUnit.getOwner().getValue())) {
            return new ValidationResult(false, "Target must be an enemy unit");
        }

        // Check range
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

        // Must be an ally (or self)
        if (!targetUnit.getOwner().getValue().equals(actingUnit.getOwner().getValue())) {
            return new ValidationResult(false, "Target must be a friendly unit");
        }

        // Check range
        int distance = manhattanDistance(actingUnit.getPosition(), targetUnit.getPosition());
        if (distance > skill.getRange()) {
            return new ValidationResult(false, "Target is out of range (range: " + skill.getRange() + ")");
        }

        return new ValidationResult(true, null);
    }

    private ValidationResult validateSingleTileTarget(GameState state, Unit actingUnit,
                                                        SkillDefinition skill, Position targetPos) {
        if (targetPos == null) {
            return new ValidationResult(false, "Target position is required for this skill");
        }

        // Check bounds
        if (!isInBounds(targetPos, state.getBoard())) {
            return new ValidationResult(false, "Target position is outside the board");
        }

        // Check range
        int distance = manhattanDistance(actingUnit.getPosition(), targetPos);
        if (distance > skill.getRange()) {
            return new ValidationResult(false, "Target is out of range (range: " + skill.getRange() + ")");
        }

        // For movement skills, tile must be empty
        if (skill.getEffects().contains(com.tactics.engine.skill.SkillEffect.MOVE_SELF)) {
            if (isTileBlocked(state, targetPos)) {
                return new ValidationResult(false, "Target tile is blocked");
            }
        }

        return new ValidationResult(true, null);
    }

    private ValidationResult validateLineTarget(GameState state, Unit actingUnit,
                                                  SkillDefinition skill, Position targetPos) {
        if (targetPos == null) {
            return new ValidationResult(false, "Target position is required for LINE skill");
        }

        // Check bounds
        if (!isInBounds(targetPos, state.getBoard())) {
            return new ValidationResult(false, "Target position is outside the board");
        }

        // Must be in a straight line (orthogonal)
        if (!isOrthogonal(actingUnit.getPosition(), targetPos)) {
            return new ValidationResult(false, "Target must be in a straight line");
        }

        // Check range
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

        // Check bounds
        if (!isInBounds(targetPos, state.getBoard())) {
            return new ValidationResult(false, "Target position is outside the board");
        }

        // Check range
        int distance = manhattanDistance(actingUnit.getPosition(), targetPos);
        if (distance > skill.getRange()) {
            return new ValidationResult(false, "Target is out of range (range: " + skill.getRange() + ")");
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
     * V3: Can attack obstacles as well as units.
     */
    private ValidationResult validateAttack(GameState state, Action action) {
        // A8: Missing targetPosition
        if (action.getTargetPosition() == null) {
            return new ValidationResult(false, "Target position is required for ATTACK");
        }

        Position targetPos = action.getTargetPosition();
        String targetUnitId = action.getTargetUnitId();

        // V3: Check if attacking an obstacle
        // Obstacle attack is indicated by: targetUnitId starts with obstacle prefix OR
        // targetUnitId is null AND there's an obstacle at the position
        boolean isAttackingObstacle = false;
        if (targetUnitId != null && targetUnitId.startsWith(com.tactics.engine.model.Obstacle.ID_PREFIX)) {
            isAttackingObstacle = true;
        } else if (targetUnitId == null) {
            // Check if there's an obstacle at position (V3 allows attacking obstacles without targetUnitId)
            if (state.getObstacleAt(targetPos) != null) {
                isAttackingObstacle = true;
            } else {
                // A7: Missing targetUnitId and no obstacle at position
                return new ValidationResult(false, "Target unit ID is required for ATTACK");
            }
        }

        if (isAttackingObstacle) {
            // Validate obstacle attack
            com.tactics.engine.model.Obstacle obstacle = state.getObstacleAt(targetPos);
            if (obstacle == null) {
                return new ValidationResult(false, "No obstacle at target position");
            }
        } else {
            // Validate unit attack (existing logic)
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
        }

        // Find attackers: alive friendly units within effective attack range of target
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

        // V3: DEATH_CHOICE action
        if (type == ActionType.DEATH_CHOICE) {
            return applyDeathChoice(state, action);
        }

        // V3: USE_SKILL action
        if (type == ActionType.USE_SKILL) {
            return applyUseSkill(state, action);
        }

        // Should not reach here if validation passed
        return null;
    }

    /**
     * Apply END_TURN: mark current player's units as having acted, then switch player.
     * V3: Uses exhaustion rule to determine next player.
     * V3: Round ends when all units have acted.
     */
    private GameState applyEndTurn(GameState state) {
        PlayerId currentPlayer = state.getCurrentPlayer();

        // V3: Mark all current player's unused units as having acted (END_TURN forfeits remaining actions)
        List<Unit> unitsAfterEndTurn = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() &&
                u.getOwner().getValue().equals(currentPlayer.getValue()) &&
                u.getActionsUsed() == 0) {
                // Mark unit as having acted
                unitsAfterEndTurn.add(u.withActionsUsed(1));
            } else {
                unitsAfterEndTurn.add(u);
            }
        }

        // Process turn-end buff effects (poison damage, duration reduction, expiration)
        TurnEndResult turnEndResult = processTurnEnd(unitsAfterEndTurn, state.getUnitBuffs());

        GameOverResult gameOver = checkGameOver(turnEndResult.units);

        // V3: Build temporary state to check exhaustion rule and round end
        GameState tempState = new GameState(
            state.getBoard(),
            turnEndResult.units,
            currentPlayer,
            gameOver.isGameOver,
            gameOver.winner,
            turnEndResult.unitBuffs,
            state.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );

        // V3: Check if all units have acted (round should end)
        if (allUnitsActed(tempState)) {
            return processRoundEnd(state, turnEndResult, gameOver);
        }

        // V3: Determine next player using exhaustion rule
        PlayerId nextPlayer = getNextActingPlayer(tempState, currentPlayer);

        // Switch to next player
        return new GameState(
            state.getBoard(),
            turnEndResult.units,
            nextPlayer,
            gameOver.isGameOver,
            gameOver.winner,
            turnEndResult.unitBuffs,
            state.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );
    }

    /**
     * Process round end: execute preparing actions, apply attrition, increment round, reset turn flags, reset actionsUsed.
     * Called when both players have ended their turn (or all units have acted).
     *
     * Round-end processing order:
     * 1. Execute preparing actions (SLOW buff)
     * 2. Apply Minion Decay (-1 HP to all minions)
     * 3. Apply Round 8+ Pressure (-1 HP to all units if round >= 8)
     * 4. Check deaths
     * 5. Reset action state
     * 6. Increment round
     */
    private GameState processRoundEnd(GameState state, TurnEndResult turnEndResult, GameOverResult gameOver) {
        // V3: Execute preparing actions (SLOW buff delayed actions)
        PreparingActionsResult prepResult = executePreparingActions(state, turnEndResult.units, turnEndResult.unitBuffs);

        // Check game over again after executing preparing actions
        GameOverResult gameOverAfterPrep = checkGameOver(prepResult.units);
        if (gameOverAfterPrep.isGameOver) {
            gameOver = gameOverAfterPrep;
        }

        // V3: Apply Minion Decay (-1 HP to all minions at round end)
        List<Unit> unitsAfterDecay = applyMinionDecay(prepResult.units);

        // Check game over after minion decay
        GameOverResult gameOverAfterDecay = checkGameOver(unitsAfterDecay);
        if (gameOverAfterDecay.isGameOver) {
            gameOver = gameOverAfterDecay;
        }

        // V3: Apply Round 8+ Pressure (-1 HP to all units if round >= 8)
        // Note: We check current round (before increment), so round 8 means "end of round 8"
        List<Unit> unitsAfterPressure = unitsAfterDecay;
        if (state.getCurrentRound() >= 8) {
            unitsAfterPressure = applyRound8Pressure(unitsAfterDecay);

            // Check game over after pressure
            GameOverResult gameOverAfterPressure = checkGameOver(unitsAfterPressure);
            if (gameOverAfterPressure.isGameOver) {
                gameOver = gameOverAfterPressure;
            }
        }

        // Reset actionsUsed and clear preparing state for all units
        List<Unit> unitsWithResetActions = resetActionsUsedAndPreparingState(unitsAfterPressure);

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
     * V3: Apply Minion Decay - all minions lose 1 HP at round end.
     * Heroes are NOT affected.
     */
    private List<Unit> applyMinionDecay(List<Unit> units) {
        List<Unit> result = new ArrayList<>();
        for (Unit u : units) {
            // Only apply decay to alive minions
            if (u.isAlive() && u.getCategory() == UnitCategory.MINION) {
                result.add(u.withDamage(1));
            } else {
                result.add(u);
            }
        }
        return result;
    }

    /**
     * V3: Apply Round 8+ Pressure - all units lose 1 HP at round end (when round >= 8).
     * This stacks with Minion Decay (minions lose 2 HP/round total after R8).
     */
    private List<Unit> applyRound8Pressure(List<Unit> units) {
        List<Unit> result = new ArrayList<>();
        for (Unit u : units) {
            // Apply pressure to all alive units
            if (u.isAlive()) {
                result.add(u.withDamage(1));
            } else {
                result.add(u);
            }
        }
        return result;
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
                        newUnits.add(u.withPosition(targetPos).withPreparing(false, null));
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

                List<Unit> newUnits = new ArrayList<>();
                for (Unit u : units) {
                    if (u.getId().equals(targetUnitId)) {
                        newUnits.add(u.withDamage(damage));
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

                        List<Unit> newUnits = new ArrayList<>();
                        for (Unit u : units) {
                            if (u.getId().equals(prepUnit.getId())) {
                                newUnits.add(u.withPosition(movePos).withPreparing(false, null));
                            } else if (u.getId().equals(targetUnitId)) {
                                newUnits.add(u.withDamage(damage));
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
     * Reset actionsUsed to 0, clear preparing state, and decrement skill cooldowns for all units at round end.
     * V3: Also decrements hero skill cooldowns by 1 (minimum 0).
     */
    private List<Unit> resetActionsUsedAndPreparingState(List<Unit> units) {
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : units) {
            // V3: Use withRoundEndReset which handles actionsUsed, preparing, AND cooldown
            newUnits.add(u.withRoundEndReset());
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
        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withPreparingAndActionUsed(preparingAction));

        // SLOW buff preparing does not switch turn
        return state.withUnits(newUnits);
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
        Unit movedUnit = mover.withPositionAndActionUsed(targetPos);
        List<Unit> newUnits = updateUnitInList(state.getUnits(), mover.getId(), u -> movedUnit);

        // V3: Check for buff tile trigger at destination
        BuffTileTriggerResult tileResult = checkBuffTileTrigger(state, movedUnit, targetPos, newUnits, state.getUnitBuffs());

        GameOverResult gameOver = checkGameOver(tileResult.units);

        // MOVE does not switch turn, does not process turn-end buffs
        return state.withMoveResult(tileResult.units, tileResult.unitBuffs, tileResult.buffTiles,
                                     gameOver.isGameOver, gameOver.winner);
    }

    private GameState applyAttack(GameState state, Action action) {
        String targetUnitId = action.getTargetUnitId();
        Position targetPos = action.getTargetPosition();

        // V3: Check if attacking an obstacle
        boolean isAttackingObstacle = targetUnitId == null || targetUnitId.startsWith(com.tactics.engine.model.Obstacle.ID_PREFIX);

        // Find attacker using effective attackRange (with buffs)
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

        // V3: Handle obstacle attack
        if (isAttackingObstacle) {
            return applyAttackObstacle(state, action, attacker, attackerBuffs, targetPos);
        }

        // Handle unit attack (existing logic)
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);

        // V3: Check for Guardian intercept
        // If a friendly TANK is adjacent to the target, damage is redirected to the TANK
        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        // Calculate damage including bonus attack from buffs
        int bonusAttack = getBonusAttack(attackerBuffs);
        int totalDamage = attacker.getAttack() + bonusAttack;

        // Create new units list with updated damage receiver HP and attacker's actionsUsed
        Map<String, UnitTransformer> transformers = new HashMap<>();
        transformers.put(damageReceiverId, u -> u.withDamage(totalDamage));
        if (!attacker.getId().equals(damageReceiverId)) {
            transformers.put(attacker.getId(), Unit::withActionUsed);
        } else {
            // Attacker is the same as damage receiver (e.g., self-damage) - already handled above
            transformers.put(damageReceiverId, u -> u.withDamage(totalDamage).withActionUsed());
        }
        List<Unit> newUnits = updateUnitsInList(state.getUnits(), transformers);

        // V3: Pass active player for simultaneous death rule
        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());

        // ATTACK does not switch turn, does not process turn-end buffs
        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }

    /**
     * V3: Apply attack on an obstacle.
     * - POWER buff: Instant destroy (ignore HP)
     * - Normal: Reduce obstacle HP, remove if destroyed
     */
    private GameState applyAttackObstacle(GameState state, Action action, Unit attacker,
                                           List<BuffInstance> attackerBuffs, Position targetPos) {
        com.tactics.engine.model.Obstacle targetObstacle = state.getObstacleAt(targetPos);

        // Calculate damage
        int bonusAttack = getBonusAttack(attackerBuffs);
        int totalDamage = attacker.getAttack() + bonusAttack;

        // Check if attacker has POWER buff (instant destroy)
        boolean hasPower = hasPowerBuff(attackerBuffs);

        // Update obstacles list
        List<com.tactics.engine.model.Obstacle> newObstacles = new ArrayList<>();
        for (com.tactics.engine.model.Obstacle o : state.getObstacles()) {
            if (o.getPosition().equals(targetPos)) {
                if (hasPower) {
                    // POWER buff: instant destroy, don't add to new list
                    continue;
                } else {
                    // Normal attack: reduce HP
                    com.tactics.engine.model.Obstacle damaged = o.withDamage(totalDamage);
                    if (!damaged.isDestroyed()) {
                        newObstacles.add(damaged);
                    }
                    // If destroyed (HP <= 0), don't add to list
                }
            } else {
                newObstacles.add(o);
            }
        }

        // Update attacker's actionsUsed
        List<Unit> newUnits = updateUnitInList(state.getUnits(), attacker.getId(), Unit::withActionUsed);

        // ATTACK on obstacle does not switch turn
        return state.withUnits(newUnits).withObstacles(newObstacles);
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

        // V3: Check for Guardian intercept
        // If a friendly TANK is adjacent to the target, damage is redirected to the TANK
        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        // Calculate damage including bonus attack from buffs
        int bonusAttack = getBonusAttack(moverBuffs);
        int totalDamage = mover.getAttack() + bonusAttack;

        // Create new units list with mover at new position, damage receiver with new HP
        List<Unit> newUnits = new ArrayList<>();
        Unit movedUnit = null;
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(mover.getId())) {
                // Move + attack as single action
                movedUnit = u.withPositionAndActionUsed(targetPos);
                newUnits.add(movedUnit);
            } else if (u.getId().equals(damageReceiverId)) {
                // Apply damage to actual receiver (either target or guardian)
                newUnits.add(u.withDamage(totalDamage));
            } else {
                newUnits.add(u);
            }
        }

        // V3: Check for buff tile trigger at destination
        BuffTileTriggerResult tileResult = checkBuffTileTrigger(state, movedUnit, targetPos, newUnits, state.getUnitBuffs());

        // Process turn-end buff effects (poison damage, duration reduction, expiration)
        TurnEndResult turnEndResult = processTurnEnd(tileResult.units, tileResult.unitBuffs);

        // V3: Pass active player for simultaneous death rule
        GameOverResult gameOver = checkGameOver(turnEndResult.units, action.getPlayerId());

        // V3: Build temporary state to check exhaustion rule
        GameState tempState = new GameState(
            state.getBoard(),
            turnEndResult.units,
            state.getCurrentPlayer(),
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

        // V3: Check if all units have acted (round should end)
        if (allUnitsActed(tempState)) {
            return processRoundEnd(state, turnEndResult, gameOver);
        }

        // V3: Determine next player using exhaustion rule
        PlayerId nextPlayer = getNextActingPlayer(tempState, state.getCurrentPlayer());

        // MOVE_AND_ATTACK switches turn (using exhaustion rule)
        return new GameState(
            state.getBoard(),
            turnEndResult.units,
            nextPlayer,
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

    /**
     * Apply USE_SKILL action (V3).
     * Executes the hero's selected skill based on skill type and effects.
     */
    private GameState applyUseSkill(GameState state, Action action) {
        String actingUnitId = action.getActingUnitId();
        Unit actingUnit = findUnitById(state.getUnits(), actingUnitId);
        String skillId = actingUnit.getSelectedSkillId();
        SkillDefinition skill = SkillRegistry.getSkill(skillId);

        // Dispatch to skill-specific handler based on skill ID
        // Phase 4A: Endure, Spirit Hawk
        // Phase 4B: Elemental Blast, Trinity, Shockwave, Nature's Power, Power of Many
        GameState result;
        switch (skillId) {
            case SkillRegistry.WARRIOR_ENDURE:
                result = applySkillEndure(state, action, actingUnit, skill);
                break;
            case SkillRegistry.WARRIOR_SHOCKWAVE:
                result = applySkillShockwave(state, action, actingUnit, skill);
                break;
            case SkillRegistry.MAGE_ELEMENTAL_BLAST:
                result = applySkillElementalBlast(state, action, actingUnit, skill);
                break;
            case SkillRegistry.HUNTRESS_SPIRIT_HAWK:
                result = applySkillSpiritHawk(state, action, actingUnit, skill);
                break;
            case SkillRegistry.HUNTRESS_NATURES_POWER:
                result = applySkillNaturesPower(state, action, actingUnit, skill);
                break;
            case SkillRegistry.CLERIC_TRINITY:
                result = applySkillTrinity(state, action, actingUnit, skill);
                break;
            case SkillRegistry.CLERIC_POWER_OF_MANY:
                result = applySkillPowerOfMany(state, action, actingUnit, skill);
                break;
            default:
                // Placeholder for unimplemented skills - just consume action and set cooldown
                result = applySkillPlaceholder(state, action, actingUnit, skill);
                break;
        }

        return result;
    }

    /**
     * Placeholder for unimplemented skills.
     * Just consumes the action and sets cooldown.
     */
    private GameState applySkillPlaceholder(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        // Update unit with cooldown set and action used
        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsed(skill.getCooldown()));

        return state.withUnits(newUnits);
    }

    /**
     * Apply Warrior Endure skill.
     * Effect: Gain 3 shield for 2 rounds, remove BLEED debuff.
     */
    private GameState applySkillEndure(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int shieldAmount = skill.getShieldAmount();  // 3
        int cooldown = skill.getCooldown();  // 2

        // Update unit with shield added and skill on cooldown
        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withShieldAndSkillUsed(u.getShield() + shieldAmount, cooldown));

        // Remove BLEED debuffs from the unit
        Map<String, List<BuffInstance>> newUnitBuffs = removeBleedBuffs(state.getUnitBuffs(), actingUnit.getId());

        GameOverResult gameOver = checkGameOver(newUnits);

        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Remove BLEED buffs from a unit.
     */
    private Map<String, List<BuffInstance>> removeBleedBuffs(Map<String, List<BuffInstance>> unitBuffs, String unitId) {
        if (unitBuffs == null) {
            return unitBuffs;
        }

        List<BuffInstance> buffs = unitBuffs.get(unitId);
        if (buffs == null || buffs.isEmpty()) {
            return unitBuffs;
        }

        // Filter out BLEED buffs
        List<BuffInstance> remainingBuffs = new ArrayList<>();
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() == null || !buff.getFlags().isBleedBuff()) {
                remainingBuffs.add(buff);
            }
        }

        // Create new map with updated buffs
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(unitBuffs);
        if (remainingBuffs.isEmpty()) {
            newUnitBuffs.remove(unitId);
        } else {
            newUnitBuffs.put(unitId, remainingBuffs);
        }

        return newUnitBuffs;
    }

    /**
     * Apply Huntress Spirit Hawk skill.
     * Effect: Deal 2 damage to enemy at long range (4 tiles).
     */
    private GameState applySkillSpiritHawk(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        // V3: Skills use skillTargetUnitId for targeting
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int damage = skill.getDamageAmount();  // 2
        int cooldown = skill.getCooldown();  // 2

        // V3: Check for Guardian intercept
        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        // Update units: caster uses skill, target takes damage
        Map<String, UnitTransformer> transformers = new HashMap<>();
        transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown));
        if (!actingUnit.getId().equals(damageReceiverId)) {
            transformers.put(damageReceiverId, u -> u.withDamage(damage));
        } else {
            // Self-damage case (shouldn't happen for Spirit Hawk, but handle it)
            transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown).withDamage(damage));
        }
        List<Unit> newUnits = updateUnitsInList(state.getUnits(), transformers);

        // V3: Pass active player for simultaneous death rule
        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());

        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }

    // =========================================================================
    // Phase 4B Skill Implementations
    // =========================================================================

    /**
     * Apply Mage Elemental Blast skill.
     * Effect: Deal 3 damage to target, 50% chance to apply random debuff (WEAKNESS, BLEED, or SLOW).
     */
    private GameState applySkillElementalBlast(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int damage = skill.getDamageAmount();  // 3
        int cooldown = skill.getCooldown();  // 2

        // Check for Guardian intercept
        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        // Update units: caster uses skill, target takes damage
        Map<String, UnitTransformer> transformers = new HashMap<>();
        transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown));
        if (!actingUnit.getId().equals(damageReceiverId)) {
            transformers.put(damageReceiverId, u -> u.withDamage(damage));
        } else {
            transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown).withDamage(damage));
        }
        List<Unit> newUnits = updateUnitsInList(state.getUnits(), transformers);

        // 50% chance to apply random debuff
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        if (rngProvider.nextInt(100) < 50) {
            // Random debuff: WEAKNESS, BLEED, or SLOW
            BuffType[] debuffs = {BuffType.WEAKNESS, BuffType.BLEED, BuffType.SLOW};
            BuffType debuffType = debuffs[rngProvider.nextInt(3)];
            BuffInstance debuff = BuffFactory.create(debuffType, actingUnit.getId());

            List<BuffInstance> targetBuffs = new ArrayList<>(
                newUnitBuffs.getOrDefault(damageReceiverId, Collections.emptyList())
            );
            targetBuffs.add(debuff);
            newUnitBuffs.put(damageReceiverId, targetBuffs);

            // Apply instant HP effects for WEAKNESS (-1 HP)
            if (debuff.getInstantHpBonus() != 0) {
                List<Unit> unitsWithDebuffHp = new ArrayList<>();
                for (Unit u : newUnits) {
                    if (u.getId().equals(damageReceiverId)) {
                        unitsWithDebuffHp.add(u.withHpBonus(debuff.getInstantHpBonus()));
                    } else {
                        unitsWithDebuffHp.add(u);
                    }
                }
                newUnits = unitsWithDebuffHp;
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Warrior Shockwave skill.
     * Effect: Deal 1 damage to all adjacent enemies and push them 1 tile away.
     * If enemy cannot be pushed (blocked), deal +1 damage instead.
     */
    private GameState applySkillShockwave(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int damage = skill.getDamageAmount();  // 1
        int cooldown = skill.getCooldown();  // 2
        Position heroPos = actingUnit.getPosition();

        // Find all adjacent enemies
        List<Unit> adjacentEnemies = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() &&
                !u.getOwner().getValue().equals(actingUnit.getOwner().getValue()) &&
                isAdjacent(heroPos, u.getPosition())) {
                adjacentEnemies.add(u);
            }
        }

        // Sort by ID for deterministic order
        adjacentEnemies.sort((a, b) -> a.getId().compareTo(b.getId()));

        // Track units being moved and damaged
        Map<String, Position> newPositions = new HashMap<>();
        Map<String, Integer> damageAmounts = new HashMap<>();

        for (Unit enemy : adjacentEnemies) {
            // Calculate push direction (away from hero)
            int dx = enemy.getPosition().getX() - heroPos.getX();
            int dy = enemy.getPosition().getY() - heroPos.getY();
            Position pushDest = new Position(
                enemy.getPosition().getX() + dx,
                enemy.getPosition().getY() + dy
            );

            // Check if push destination is valid
            boolean canPush = isInBounds(pushDest, state.getBoard()) &&
                              !isTileBlocked(state, pushDest) &&
                              !newPositions.containsValue(pushDest);  // Check if another unit is being pushed there

            // Check Guardian intercept
            Unit guardian = findGuardian(state, enemy);
            String damageReceiverId = (guardian != null) ? guardian.getId() : enemy.getId();

            if (canPush) {
                // Push the enemy
                newPositions.put(enemy.getId(), pushDest);
                damageAmounts.merge(damageReceiverId, damage, Integer::sum);
            } else {
                // Cannot push - deal +1 extra damage
                damageAmounts.merge(damageReceiverId, damage + 1, Integer::sum);
            }
        }

        // Apply all changes
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                // Caster uses skill
                newUnits.add(u.withSkillUsed(cooldown));
            } else if (newPositions.containsKey(u.getId())) {
                // Enemy is pushed and takes damage
                int totalDamage = damageAmounts.getOrDefault(u.getId(), 0);
                newUnits.add(u.withPosition(newPositions.get(u.getId())).withDamage(totalDamage));
            } else if (damageAmounts.containsKey(u.getId())) {
                // Unit takes damage (guardian or blocked enemy)
                newUnits.add(u.withDamage(damageAmounts.get(u.getId())));
            } else {
                newUnits.add(u);
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Huntress Nature's Power skill.
     * Effect: Next 2 attacks deal +2 damage, gain LIFE buff (+3 HP instant).
     */
    private GameState applySkillNaturesPower(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int cooldown = skill.getCooldown();  // 2
        int bonusDamage = skill.getDamageAmount();  // 2 (bonus per attack)
        int attackCharges = 2;  // Number of empowered attacks

        // Update unit with skill cooldown and bonus attack charges
        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsedAndBonusAttack(cooldown, bonusDamage, attackCharges));

        // Apply LIFE buff (+3 HP instant)
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance lifeBuff = BuffFactory.create(BuffType.LIFE, actingUnit.getId());
        List<BuffInstance> heroBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(actingUnit.getId(), Collections.emptyList())
        );
        heroBuffs.add(lifeBuff);
        newUnitBuffs.put(actingUnit.getId(), heroBuffs);

        // Apply instant HP bonus from LIFE buff
        if (lifeBuff.getInstantHpBonus() != 0) {
            newUnits = updateUnitInList(newUnits, actingUnit.getId(),
                u -> u.withHpBonus(lifeBuff.getInstantHpBonus()));
        }

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Cleric Trinity skill.
     * Effect: Heal target for 3 HP, remove one random debuff, apply LIFE buff (+3 HP instant).
     */
    private GameState applySkillTrinity(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int healAmount = skill.getHealAmount();  // 3
        int cooldown = skill.getCooldown();  // 2

        // Update units: caster uses skill, target heals
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                if (u.getId().equals(targetUnitId)) {
                    // Self-heal case
                    newUnits.add(u.withSkillUsed(cooldown).withHpBonus(healAmount));
                } else {
                    newUnits.add(u.withSkillUsed(cooldown));
                }
            } else if (u.getId().equals(targetUnitId)) {
                newUnits.add(u.withHpBonus(healAmount));
            } else {
                newUnits.add(u);
            }
        }

        // Remove one random debuff from target
        Map<String, List<BuffInstance>> newUnitBuffs = removeOneRandomDebuff(state.getUnitBuffs(), targetUnitId);

        // Apply LIFE buff to target (+3 HP instant)
        BuffInstance lifeBuff = BuffFactory.create(BuffType.LIFE, actingUnit.getId());
        List<BuffInstance> targetBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(targetUnitId, Collections.emptyList())
        );
        targetBuffs.add(lifeBuff);
        newUnitBuffs.put(targetUnitId, targetBuffs);

        // Apply instant HP bonus from LIFE buff
        if (lifeBuff.getInstantHpBonus() != 0) {
            newUnits = updateUnitInList(newUnits, targetUnitId,
                u -> u.withHpBonus(lifeBuff.getInstantHpBonus()));
        }

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Remove one random debuff from a unit.
     * Debuffs are: WEAKNESS, BLEED, SLOW (and potentially STUN, ROOT if implemented).
     */
    private Map<String, List<BuffInstance>> removeOneRandomDebuff(Map<String, List<BuffInstance>> unitBuffs, String unitId) {
        if (unitBuffs == null) {
            return new HashMap<>();
        }

        List<BuffInstance> buffs = unitBuffs.get(unitId);
        if (buffs == null || buffs.isEmpty()) {
            return new HashMap<>(unitBuffs);
        }

        // Find all debuffs
        List<BuffInstance> debuffs = new ArrayList<>();
        for (BuffInstance buff : buffs) {
            if (isDebuff(buff)) {
                debuffs.add(buff);
            }
        }

        if (debuffs.isEmpty()) {
            return new HashMap<>(unitBuffs);
        }

        // Pick a random debuff to remove
        BuffInstance toRemove = debuffs.get(rngProvider.nextInt(debuffs.size()));

        // Create new buff list without the removed debuff
        List<BuffInstance> remainingBuffs = new ArrayList<>();
        boolean removed = false;
        for (BuffInstance buff : buffs) {
            if (!removed && buff.getBuffId().equals(toRemove.getBuffId())) {
                removed = true;  // Only remove the first matching one
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
     * Check if a buff is a debuff (negative effect).
     */
    private boolean isDebuff(BuffInstance buff) {
        if (buff.getFlags() == null) {
            return false;
        }
        // Debuffs: WEAKNESS, BLEED, SLOW, STUN, ROOT
        return buff.getFlags().isBleedBuff() ||
               buff.getFlags().isSlowBuff() ||
               buff.getFlags().isStunned() ||
               buff.getFlags().isRooted() ||
               (buff.getModifiers() != null && buff.getModifiers().getBonusAttack() < 0);  // WEAKNESS has -2 ATK
    }

    /**
     * Apply Cleric Power of Many skill.
     * Effect: Heal ALL friendly units for 1 HP, grant all allies +1 ATK for 1 round.
     */
    private GameState applySkillPowerOfMany(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int healAmount = skill.getHealAmount();  // 1
        int cooldown = skill.getCooldown();  // 2
        int atkBonusDuration = skill.getEffectDuration();  // 1 round

        // Find all friendly units
        List<String> friendlyUnitIds = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(actingUnit.getOwner().getValue())) {
                friendlyUnitIds.add(u.getId());
            }
        }

        // Update units: heal all friendlies, caster uses skill
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown).withHpBonus(healAmount));
            } else if (friendlyUnitIds.contains(u.getId())) {
                newUnits.add(u.withHpBonus(healAmount));
            } else {
                newUnits.add(u);
            }
        }

        // Apply +1 ATK buff to all friendlies for 1 round
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        for (String unitId : friendlyUnitIds) {
            BuffInstance atkBuff = createAtkBuff(actingUnit.getId(), 1, atkBonusDuration);
            List<BuffInstance> unitBuffs = new ArrayList<>(
                newUnitBuffs.getOrDefault(unitId, Collections.emptyList())
            );
            unitBuffs.add(atkBuff);
            newUnitBuffs.put(unitId, unitBuffs);
        }

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Create a temporary ATK bonus buff.
     */
    private BuffInstance createAtkBuff(String sourceUnitId, int bonusAtk, int duration) {
        return new BuffInstance(
            "power_of_many_" + System.currentTimeMillis(),
            sourceUnitId,
            duration,
            true,
            new BuffModifier(0, bonusAtk, 0, 0),  // bonusHp, bonusAttack, bonusMoveRange, bonusAttackRange
            null
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
