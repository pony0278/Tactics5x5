package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;
import com.tactics.engine.skill.SkillDefinition;
import com.tactics.engine.skill.SkillExecutor;
import com.tactics.engine.skill.SkillRegistry;
import com.tactics.engine.util.RngProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes game actions and updates game state.
 * Extracted from RuleEngine for better code organization.
 *
 * Handles execution for:
 * - MOVE, ATTACK, MOVE_AND_ATTACK
 * - USE_SKILL (delegates to SkillExecutor)
 * - DEATH_CHOICE
 * - END_TURN
 */
public class ActionExecutor {

    private RngProvider rngProvider;
    private final SkillExecutor skillExecutor;

    public ActionExecutor() {
        this.rngProvider = new RngProvider();
        this.skillExecutor = new SkillExecutor();
    }

    public void setRngProvider(RngProvider rngProvider) {
        this.rngProvider = rngProvider;
        this.skillExecutor.setRngProvider(rngProvider);
    }

    // =========================================================================
    // Main Entry Point
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

        if (type == ActionType.DEATH_CHOICE) {
            return applyDeathChoice(state, action);
        }

        if (type == ActionType.USE_SKILL) {
            return applyUseSkill(state, action);
        }

        return null;
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    @FunctionalInterface
    private interface UnitTransformer {
        Unit transform(Unit unit);
    }

    private boolean isAdjacent(Position a, Position b) {
        int dx = b.getX() - a.getX();
        int dy = b.getY() - a.getY();
        return (dx == 0 && (dy == 1 || dy == -1)) || (dy == 0 && (dx == 1 || dx == -1));
    }

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

    private Unit findUnitById(List<Unit> units, String unitId) {
        for (Unit u : units) {
            if (u.getId().equals(unitId)) {
                return u;
            }
        }
        return null;
    }

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

    private List<Unit> updateUnitsInList(List<Unit> units, Map<String, UnitTransformer> transformers) {
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

    private boolean isTileOccupied(List<Unit> units, Position pos) {
        for (Unit u : units) {
            if (u.isAlive() && u.getPosition().getX() == pos.getX() &&
                u.getPosition().getY() == pos.getY()) {
                return true;
            }
        }
        return false;
    }

    private PlayerId getNextPlayer(PlayerId current) {
        return current.isPlayer1() ? PlayerId.PLAYER_2 : PlayerId.PLAYER_1;
    }

    private PlayerId getNextActingPlayer(GameState state, PlayerId currentActingPlayer) {
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

    private boolean allUnitsActed(GameState state) {
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getActionsUsed() == 0) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    // Game Over Check
    // =========================================================================

    private static class GameOverResult {
        final boolean isGameOver;
        final PlayerId winner;

        GameOverResult(boolean isGameOver, PlayerId winner) {
            this.isGameOver = isGameOver;
            this.winner = winner;
        }
    }

    private GameOverResult checkGameOver(List<Unit> units) {
        return checkGameOver(units, null);
    }

    /**
     * Check if any minion died in the units list (HP <= 0 but was created alive).
     * Returns a DeathChoice for the first dead minion found (by ID order for determinism).
     * Only checks minions, not heroes (hero death ends the game, no death choice).
     *
     * @param units The current units list
     * @param originalUnits The units list before the action (to compare alive status)
     * @return DeathChoice if a minion died, null otherwise
     */
    private com.tactics.engine.model.DeathChoice checkMinionDeath(List<Unit> units, List<Unit> originalUnits) {
        // Build map of original alive minions
        Map<String, Unit> originalMinionMap = new HashMap<>();
        for (Unit u : originalUnits) {
            if (u.isAlive() && u.getCategory() == UnitCategory.MINION) {
                originalMinionMap.put(u.getId(), u);
            }
        }

        // Find first minion that died (sort by ID for determinism)
        List<Unit> deadMinions = new ArrayList<>();
        for (Unit u : units) {
            if (!u.isAlive() && u.getCategory() == UnitCategory.MINION && originalMinionMap.containsKey(u.getId())) {
                deadMinions.add(u);
            }
        }

        if (deadMinions.isEmpty()) {
            return null;
        }

        // Sort by ID for deterministic order
        deadMinions.sort((a, b) -> a.getId().compareTo(b.getId()));
        Unit firstDead = deadMinions.get(0);

        return new com.tactics.engine.model.DeathChoice(
            firstDead.getId(),
            firstDead.getOwner(),
            firstDead.getPosition()
        );
    }

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

    private int getBonusAttack(List<BuffInstance> buffs) {
        int bonus = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getModifiers() != null) {
                bonus += buff.getModifiers().getBonusAttack();
            }
        }
        return bonus;
    }

    private int getPoisonDamage(List<BuffInstance> buffs) {
        int count = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isPoison()) {
                count++;
            }
        }
        return count;
    }

    private int getBleedDamage(List<BuffInstance> buffs) {
        int count = 0;
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isBleedBuff()) {
                count++;
            }
        }
        return count;
    }

    private boolean hasPowerBuff(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isPowerBuff()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSlowBuff(List<BuffInstance> buffs) {
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isSlowBuff()) {
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

    private BuffType getRandomBuffType() {
        int roll = rngProvider.nextInt(6);
        BuffType[] types = BuffType.values();
        return types[roll];
    }

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

    // =========================================================================
    // Guardian Helper
    // =========================================================================

    private Unit findGuardian(GameState state, Unit target) {
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
    // Buff Tile Trigger
    // =========================================================================

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

    private BuffTileTriggerResult checkBuffTileTrigger(GameState state, Unit movedUnit, Position newPos,
                                                        List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        com.tactics.engine.model.BuffTile tile = state.getBuffTileAt(newPos);
        if (tile == null) {
            return new BuffTileTriggerResult(units, unitBuffs, state.getBuffTiles());
        }

        BuffType buffType = tile.getBuffType();
        if (buffType == null) {
            buffType = getRandomBuffType();
        }

        BuffInstance newBuff = BuffFactory.create(buffType, "bufftile_" + tile.getId());

        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(unitBuffs);
        List<BuffInstance> currentBuffs = new ArrayList<>(
            unitBuffs.getOrDefault(movedUnit.getId(), Collections.emptyList())
        );
        currentBuffs.add(newBuff);
        newUnitBuffs.put(movedUnit.getId(), currentBuffs);

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

        List<com.tactics.engine.model.BuffTile> newBuffTiles = new ArrayList<>();
        for (com.tactics.engine.model.BuffTile t : state.getBuffTiles()) {
            if (t.getId().equals(tile.getId())) {
                newBuffTiles.add(new com.tactics.engine.model.BuffTile(
                    t.getId(), t.getPosition(), buffType, t.getDuration(), true
                ));
            } else {
                newBuffTiles.add(t);
            }
        }

        return new BuffTileTriggerResult(newUnits, newUnitBuffs, newBuffTiles);
    }

    // =========================================================================
    // Turn-End Processing
    // =========================================================================

    private static class TurnEndResult {
        final List<Unit> units;
        final Map<String, List<BuffInstance>> unitBuffs;

        TurnEndResult(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
            this.units = units;
            this.unitBuffs = unitBuffs;
        }
    }

    private TurnEndResult processTurnEnd(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        if (unitBuffs == null || unitBuffs.isEmpty()) {
            return new TurnEndResult(units, unitBuffs);
        }

        List<Unit> newUnits = new ArrayList<>(units);
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>();

        List<String> unitIdsWithBuffs = new ArrayList<>(unitBuffs.keySet());
        Collections.sort(unitIdsWithBuffs);

        // Apply poison damage
        for (String unitId : unitIdsWithBuffs) {
            List<BuffInstance> buffs = unitBuffs.get(unitId);
            if (buffs == null || buffs.isEmpty()) {
                continue;
            }

            int poisonDamage = getPoisonDamage(buffs);
            if (poisonDamage > 0) {
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

        // Apply BLEED damage
        for (String unitId : unitIdsWithBuffs) {
            List<BuffInstance> buffs = unitBuffs.get(unitId);
            if (buffs == null || buffs.isEmpty()) {
                continue;
            }

            int bleedDamage = getBleedDamage(buffs);
            if (bleedDamage > 0) {
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

        // Decrease duration and remove expired buffs
        for (String unitId : unitIdsWithBuffs) {
            List<BuffInstance> buffs = unitBuffs.get(unitId);
            if (buffs == null || buffs.isEmpty()) {
                continue;
            }

            List<BuffInstance> remainingBuffs = new ArrayList<>();
            for (BuffInstance buff : buffs) {
                int newDuration = buff.getDuration() - 1;
                if (newDuration > 0) {
                    remainingBuffs.add(new BuffInstance(
                        buff.getBuffId(),
                        buff.getSourceUnitId(),
                        newDuration,
                        buff.isStackable(),
                        buff.getModifiers(),
                        buff.getFlags()
                    ));
                }
            }

            if (!remainingBuffs.isEmpty()) {
                newUnitBuffs.put(unitId, remainingBuffs);
            }
        }

        return new TurnEndResult(newUnits, newUnitBuffs);
    }

    // =========================================================================
    // Round-End Processing
    // =========================================================================

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

    private PreparingActionsResult executePreparingActions(GameState state, List<Unit> units,
                                                            Map<String, List<BuffInstance>> unitBuffs) {
        List<Unit> currentUnits = new ArrayList<>(units);
        List<com.tactics.engine.model.BuffTile> currentBuffTiles = new ArrayList<>(state.getBuffTiles());
        List<com.tactics.engine.model.Obstacle> currentObstacles = new ArrayList<>(state.getObstacles());

        List<String> preparingUnitIds = new ArrayList<>();
        for (Unit u : currentUnits) {
            if (u.isPreparing() && u.getPreparingAction() != null) {
                preparingUnitIds.add(u.getId());
            }
        }
        Collections.sort(preparingUnitIds);

        for (String unitId : preparingUnitIds) {
            Unit prepUnit = null;
            for (Unit u : currentUnits) {
                if (u.getId().equals(unitId)) {
                    prepUnit = u;
                    break;
                }
            }
            if (prepUnit == null || !prepUnit.isAlive()) {
                continue;
            }

            Map<String, Object> actionMap = prepUnit.getPreparingAction();
            currentUnits = executeOnePreparingAction(prepUnit, actionMap, currentUnits);
        }

        return new PreparingActionsResult(currentUnits, unitBuffs, currentBuffTiles, currentObstacles);
    }

    @SuppressWarnings("unchecked")
    private List<Unit> executeOnePreparingAction(Unit prepUnit, Map<String, Object> actionMap, List<Unit> units) {
        String actionType = (String) actionMap.get("type");

        if ("MOVE".equals(actionType)) {
            Map<String, Object> posMap = (Map<String, Object>) actionMap.get("targetPosition");
            int x = ((Number) posMap.get("x")).intValue();
            int y = ((Number) posMap.get("y")).intValue();
            Position targetPos = new Position(x, y);

            boolean blocked = false;
            for (Unit u : units) {
                if (u.isAlive() && u.getPosition().equals(targetPos)) {
                    blocked = true;
                    break;
                }
            }

            if (!blocked) {
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

        } else if ("ATTACK".equals(actionType)) {
            String targetUnitId = (String) actionMap.get("targetUnitId");
            Map<String, Object> posMap = (Map<String, Object>) actionMap.get("targetPosition");
            int expectedX = ((Number) posMap.get("x")).intValue();
            int expectedY = ((Number) posMap.get("y")).intValue();
            Position expectedPos = new Position(expectedX, expectedY);

            Unit targetUnit = null;
            for (Unit u : units) {
                if (u.getId().equals(targetUnitId)) {
                    targetUnit = u;
                    break;
                }
            }

            if (targetUnit != null && targetUnit.isAlive() && targetUnit.getPosition().equals(expectedPos)) {
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

        } else if ("MOVE_AND_ATTACK".equals(actionType)) {
            Map<String, Object> posMap = (Map<String, Object>) actionMap.get("targetPosition");
            int moveX = ((Number) posMap.get("x")).intValue();
            int moveY = ((Number) posMap.get("y")).intValue();
            Position movePos = new Position(moveX, moveY);
            String targetUnitId = (String) actionMap.get("targetUnitId");

            boolean moveBlocked = false;
            for (Unit u : units) {
                if (u.isAlive() && u.getPosition().equals(movePos)) {
                    moveBlocked = true;
                    break;
                }
            }

            if (!moveBlocked) {
                Unit targetUnit = null;
                for (Unit u : units) {
                    if (u.getId().equals(targetUnitId)) {
                        targetUnit = u;
                        break;
                    }
                }

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
        }

        return units;
    }

    private List<Unit> applyMinionDecay(List<Unit> units) {
        List<Unit> result = new ArrayList<>();
        for (Unit u : units) {
            if (u.isAlive() && u.getCategory() == UnitCategory.MINION) {
                result.add(u.withDamage(1));
            } else {
                result.add(u);
            }
        }
        return result;
    }

    private List<Unit> applyRound8Pressure(List<Unit> units) {
        List<Unit> result = new ArrayList<>();
        for (Unit u : units) {
            if (u.isAlive()) {
                result.add(u.withDamage(1));
            } else {
                result.add(u);
            }
        }
        return result;
    }

    private List<Unit> resetActionsUsedAndPreparingState(List<Unit> units) {
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : units) {
            newUnits.add(u.withRoundEndReset());
        }
        return newUnits;
    }

    private GameState processRoundEnd(GameState state, TurnEndResult turnEndResult, GameOverResult gameOver) {
        PreparingActionsResult prepResult = executePreparingActions(state, turnEndResult.units, turnEndResult.unitBuffs);

        GameOverResult gameOverAfterPrep = checkGameOver(prepResult.units);
        if (gameOverAfterPrep.isGameOver) {
            gameOver = gameOverAfterPrep;
        }

        List<Unit> unitsAfterDecay = applyMinionDecay(prepResult.units);

        GameOverResult gameOverAfterDecay = checkGameOver(unitsAfterDecay);
        if (gameOverAfterDecay.isGameOver) {
            gameOver = gameOverAfterDecay;
        }

        List<Unit> unitsAfterPressure = unitsAfterDecay;
        if (state.getCurrentRound() >= 8) {
            unitsAfterPressure = applyRound8Pressure(unitsAfterDecay);

            GameOverResult gameOverAfterPressure = checkGameOver(unitsAfterPressure);
            if (gameOverAfterPressure.isGameOver) {
                gameOver = gameOverAfterPressure;
            }
        }

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
            state.getCurrentRound() + 1,
            state.getPendingDeathChoice(),
            false,
            false
        );
    }

    // =========================================================================
    // Apply Action Methods
    // =========================================================================

    private GameState applySlowBuffPreparing(GameState state, Action action, Unit actingUnit) {
        Map<String, Object> preparingAction = serializeActionForPreparing(action);

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withPreparingAndActionUsed(preparingAction));

        return state.withUnits(newUnits);
    }

    private GameState applyEndTurn(GameState state) {
        PlayerId currentPlayer = state.getCurrentPlayer();

        List<Unit> unitsAfterEndTurn = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() &&
                u.getOwner().getValue().equals(currentPlayer.getValue()) &&
                u.getActionsUsed() == 0) {
                unitsAfterEndTurn.add(u.withActionsUsed(1));
            } else {
                unitsAfterEndTurn.add(u);
            }
        }

        TurnEndResult turnEndResult = processTurnEnd(unitsAfterEndTurn, state.getUnitBuffs());

        GameOverResult gameOver = checkGameOver(turnEndResult.units);

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

        if (allUnitsActed(tempState)) {
            return processRoundEnd(state, turnEndResult, gameOver);
        }

        PlayerId nextPlayer = getNextActingPlayer(tempState, currentPlayer);

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

    private GameState applyMove(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();

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

        if (hasSlowBuff(moverBuffs)) {
            return applySlowBuffPreparing(state, action, mover);
        }

        Unit movedUnit = mover.withPositionAndActionUsed(targetPos);
        List<Unit> newUnits = updateUnitInList(state.getUnits(), mover.getId(), u -> movedUnit);

        BuffTileTriggerResult tileResult = checkBuffTileTrigger(state, movedUnit, targetPos, newUnits, state.getUnitBuffs());

        GameOverResult gameOver = checkGameOver(tileResult.units);

        return state.withMoveResult(tileResult.units, tileResult.unitBuffs, tileResult.buffTiles,
                                     gameOver.isGameOver, gameOver.winner);
    }

    private GameState applyAttack(GameState state, Action action) {
        String targetUnitId = action.getTargetUnitId();
        Position targetPos = action.getTargetPosition();

        boolean isAttackingObstacle = targetUnitId == null || targetUnitId.startsWith(com.tactics.engine.model.Obstacle.ID_PREFIX);

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

        if (hasSlowBuff(attackerBuffs)) {
            return applySlowBuffPreparing(state, action, attacker);
        }

        if (isAttackingObstacle) {
            return applyAttackObstacle(state, action, attacker, attackerBuffs, targetPos);
        }

        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);

        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        int bonusAttack = getBonusAttack(attackerBuffs);
        int naturesPowerBonus = attacker.getBonusAttackCharges() > 0 ? attacker.getBonusAttackDamage() : 0;
        int totalDamage = attacker.getAttack() + bonusAttack + naturesPowerBonus;

        final boolean hasBonusCharges = attacker.getBonusAttackCharges() > 0;
        Map<String, UnitTransformer> transformers = new HashMap<>();
        transformers.put(damageReceiverId, u -> u.withDamage(totalDamage));
        if (!attacker.getId().equals(damageReceiverId)) {
            if (attacker.isInvisible() && hasBonusCharges) {
                transformers.put(attacker.getId(), u -> u.withActionUsed().withInvisible(false).withBonusAttackConsumed());
            } else if (attacker.isInvisible()) {
                transformers.put(attacker.getId(), u -> u.withActionUsed().withInvisible(false));
            } else if (hasBonusCharges) {
                transformers.put(attacker.getId(), u -> u.withActionUsed().withBonusAttackConsumed());
            } else {
                transformers.put(attacker.getId(), Unit::withActionUsed);
            }
        } else {
            if (hasBonusCharges) {
                transformers.put(damageReceiverId, u -> u.withDamage(totalDamage).withActionUsed().withBonusAttackConsumed());
            } else {
                transformers.put(damageReceiverId, u -> u.withDamage(totalDamage).withActionUsed());
            }
        }
        List<Unit> newUnits = updateUnitsInList(state.getUnits(), transformers);

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());

        // Check for minion death - only if game is not over (hero death takes priority)
        com.tactics.engine.model.DeathChoice deathChoice = null;
        if (!gameOver.isGameOver) {
            deathChoice = checkMinionDeath(newUnits, state.getUnits());
        }

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
            deathChoice != null ? deathChoice : state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );
    }

    private GameState applyAttackObstacle(GameState state, Action action, Unit attacker,
                                           List<BuffInstance> attackerBuffs, Position targetPos) {
        com.tactics.engine.model.Obstacle targetObstacle = state.getObstacleAt(targetPos);

        int bonusAttack = getBonusAttack(attackerBuffs);
        int naturesPowerBonus = attacker.getBonusAttackCharges() > 0 ? attacker.getBonusAttackDamage() : 0;
        int totalDamage = attacker.getAttack() + bonusAttack + naturesPowerBonus;

        boolean hasPower = hasPowerBuff(attackerBuffs);

        List<com.tactics.engine.model.Obstacle> newObstacles = new ArrayList<>();
        for (com.tactics.engine.model.Obstacle o : state.getObstacles()) {
            if (o.getPosition().equals(targetPos)) {
                if (hasPower) {
                    continue;
                } else {
                    com.tactics.engine.model.Obstacle damaged = o.withDamage(totalDamage);
                    if (!damaged.isDestroyed()) {
                        newObstacles.add(damaged);
                    }
                }
            } else {
                newObstacles.add(o);
            }
        }

        final boolean hasBonusCharges = attacker.getBonusAttackCharges() > 0;
        List<Unit> newUnits;
        if (attacker.isInvisible() && hasBonusCharges) {
            newUnits = updateUnitInList(state.getUnits(), attacker.getId(),
                u -> u.withActionUsed().withInvisible(false).withBonusAttackConsumed());
        } else if (attacker.isInvisible()) {
            newUnits = updateUnitInList(state.getUnits(), attacker.getId(),
                u -> u.withActionUsed().withInvisible(false));
        } else if (hasBonusCharges) {
            newUnits = updateUnitInList(state.getUnits(), attacker.getId(),
                u -> u.withActionUsed().withBonusAttackConsumed());
        } else {
            newUnits = updateUnitInList(state.getUnits(), attacker.getId(), Unit::withActionUsed);
        }

        return state.withUnits(newUnits).withObstacles(newObstacles);
    }

    private GameState applyMoveAndAttack(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();
        String targetUnitId = action.getTargetUnitId();

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

        if (hasSlowBuff(moverBuffs)) {
            return applySlowBuffPreparing(state, action, mover);
        }

        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);

        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        int bonusAttack = getBonusAttack(moverBuffs);
        int naturesPowerBonus = mover.getBonusAttackCharges() > 0 ? mover.getBonusAttackDamage() : 0;
        int totalDamage = mover.getAttack() + bonusAttack + naturesPowerBonus;

        final boolean hasBonusCharges = mover.getBonusAttackCharges() > 0;
        List<Unit> newUnits = new ArrayList<>();
        Unit movedUnit = null;
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(mover.getId())) {
                movedUnit = u.withPositionAndActionUsed(targetPos);
                if (u.isInvisible()) {
                    movedUnit = movedUnit.withInvisible(false);
                }
                if (hasBonusCharges) {
                    movedUnit = movedUnit.withBonusAttackConsumed();
                }
                newUnits.add(movedUnit);
            } else if (u.getId().equals(damageReceiverId)) {
                newUnits.add(u.withDamage(totalDamage));
            } else {
                newUnits.add(u);
            }
        }

        BuffTileTriggerResult tileResult = checkBuffTileTrigger(state, movedUnit, targetPos, newUnits, state.getUnitBuffs());

        TurnEndResult turnEndResult = processTurnEnd(tileResult.units, tileResult.unitBuffs);

        GameOverResult gameOver = checkGameOver(turnEndResult.units, action.getPlayerId());

        // Check for minion death - only if game is not over (hero death takes priority)
        com.tactics.engine.model.DeathChoice deathChoice = null;
        if (!gameOver.isGameOver) {
            deathChoice = checkMinionDeath(turnEndResult.units, state.getUnits());
        }

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
            deathChoice != null ? deathChoice : state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );

        if (allUnitsActed(tempState)) {
            return processRoundEnd(state, turnEndResult, gameOver);
        }

        PlayerId nextPlayer = getNextActingPlayer(tempState, state.getCurrentPlayer());

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
            deathChoice != null ? deathChoice : state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );
    }

    private GameState applyDeathChoice(GameState state, Action action) {
        com.tactics.engine.model.DeathChoice deathChoice = state.getPendingDeathChoice();
        Position deathPos = deathChoice.getDeathPosition();
        com.tactics.engine.model.DeathChoice.ChoiceType choiceType = action.getDeathChoiceType();

        List<com.tactics.engine.model.Obstacle> newObstacles = new ArrayList<>(state.getObstacles());
        List<com.tactics.engine.model.BuffTile> newBuffTiles = new ArrayList<>(state.getBuffTiles());

        if (choiceType == com.tactics.engine.model.DeathChoice.ChoiceType.SPAWN_OBSTACLE) {
            String obstacleId = "obstacle_" + System.currentTimeMillis();
            newObstacles.add(new com.tactics.engine.model.Obstacle(obstacleId, deathPos));
        } else if (choiceType == com.tactics.engine.model.DeathChoice.ChoiceType.SPAWN_BUFF_TILE) {
            String tileId = "bufftile_" + System.currentTimeMillis();
            newBuffTiles.add(new com.tactics.engine.model.BuffTile(
                tileId, deathPos, null, 2, false
            ));
        }

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
            null
        );
    }

    private GameState applyUseSkill(GameState state, Action action) {
        String actingUnitId = action.getActingUnitId();
        Unit actingUnit = findUnitById(state.getUnits(), actingUnitId);
        String skillId = actingUnit.getSelectedSkillId();
        SkillDefinition skill = SkillRegistry.getSkill(skillId);

        GameState result = skillExecutor.executeSkill(state, action, actingUnit, skill);

        if (actingUnit.isInvisible() && !skillId.equals(SkillRegistry.ROGUE_SMOKE_BOMB)) {
            result = clearInvisibleOnUnit(result, actingUnitId);
        }

        return result;
    }

    private GameState clearInvisibleOnUnit(GameState state, String unitId) {
        List<Unit> newUnits = updateUnitInList(state.getUnits(), unitId,
            u -> u.withInvisible(false));
        return state.withUnits(newUnits);
    }
}
