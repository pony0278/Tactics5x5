package com.tactics.engine.rules;

import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.model.BuffTile;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Obstacle;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages turn and round processing logic.
 * Extracted from ActionExecutor for better code organization.
 */
public class TurnManager extends ActionExecutorBase {

    private final GameOverChecker gameOverChecker;

    public TurnManager() {
        this.gameOverChecker = new GameOverChecker();
    }

    // =========================================================================
    // Result Classes
    // =========================================================================

    /**
     * Result of turn-end processing (buff duration decay, poison/bleed damage).
     */
    public static class TurnEndResult {
        private final List<Unit> units;
        private final Map<String, List<BuffInstance>> unitBuffs;

        public TurnEndResult(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
            this.units = units;
            this.unitBuffs = unitBuffs;
        }

        public List<Unit> getUnits() {
            return units;
        }

        public Map<String, List<BuffInstance>> getUnitBuffs() {
            return unitBuffs;
        }
    }

    /**
     * Result of executing preparing actions.
     */
    public static class PreparingActionsResult {
        private final List<Unit> units;
        private final Map<String, List<BuffInstance>> unitBuffs;
        private final List<BuffTile> buffTiles;
        private final List<Obstacle> obstacles;

        public PreparingActionsResult(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs,
                                      List<BuffTile> buffTiles, List<Obstacle> obstacles) {
            this.units = units;
            this.unitBuffs = unitBuffs;
            this.buffTiles = buffTiles;
            this.obstacles = obstacles;
        }

        public List<Unit> getUnits() {
            return units;
        }

        public Map<String, List<BuffInstance>> getUnitBuffs() {
            return unitBuffs;
        }

        public List<BuffTile> getBuffTiles() {
            return buffTiles;
        }

        public List<Obstacle> getObstacles() {
            return obstacles;
        }
    }

    // =========================================================================
    // Turn-End Processing
    // =========================================================================

    /**
     * Process turn end.
     * Note: V3 changed poison/bleed damage and buff duration decrement to occur at round end, not turn end.
     */
    public TurnEndResult processTurnEnd(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        // V3: Poison/bleed damage and buff duration decrement all happen at round end, not per-turn
        return new TurnEndResult(units, unitBuffs);
    }

    /**
     * Apply poison and bleed damage to all units with those buffs.
     * Called at round end (not per-turn).
     */
    public List<Unit> applyPoisonAndBleedDamage(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        if (unitBuffs == null || unitBuffs.isEmpty()) {
            return units;
        }

        List<Unit> newUnits = new ArrayList<>(units);

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
                newUnits = applyDamageToUnit(newUnits, unitId, poisonDamage);
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
                newUnits = applyDamageToUnit(newUnits, unitId, bleedDamage);
            }
        }

        return newUnits;
    }

    /**
     * Decrease buff durations and remove expired buffs.
     * Called only at round end (not per-turn).
     */
    public Map<String, List<BuffInstance>> decrementBuffDurations(Map<String, List<BuffInstance>> unitBuffs) {
        if (unitBuffs == null || unitBuffs.isEmpty()) {
            return unitBuffs;
        }

        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>();

        for (Map.Entry<String, List<BuffInstance>> entry : unitBuffs.entrySet()) {
            String unitId = entry.getKey();
            List<BuffInstance> buffs = entry.getValue();
            if (buffs == null || buffs.isEmpty()) {
                continue;
            }

            List<BuffInstance> remainingBuffs = new ArrayList<>();
            for (BuffInstance buff : buffs) {
                // Use withDecreasedDuration() to preserve all fields including BuffType
                BuffInstance decremented = buff.withDecreasedDuration();
                if (!decremented.isExpired()) {
                    remainingBuffs.add(decremented);
                }
            }

            if (!remainingBuffs.isEmpty()) {
                newUnitBuffs.put(unitId, remainingBuffs);
            }
        }

        return newUnitBuffs;
    }

    private List<Unit> applyDamageToUnit(List<Unit> units, String unitId, int damage) {
        List<Unit> result = new ArrayList<>();
        for (Unit u : units) {
            if (u.getId().equals(unitId) && u.isAlive()) {
                int newHp = u.getHp() - damage;
                boolean alive = newHp > 0;
                result.add(new Unit(u.getId(), u.getOwner(), newHp, u.getAttack(),
                    u.getMoveRange(), u.getAttackRange(), u.getPosition(), alive));
            } else {
                result.add(u);
            }
        }
        return result;
    }

    // =========================================================================
    // Round-End Processing
    // =========================================================================

    /**
     * Execute preparing actions (from SLOW buff) at round end.
     */
    public PreparingActionsResult executePreparingActions(GameState state, List<Unit> units,
                                                          Map<String, List<BuffInstance>> unitBuffs) {
        List<Unit> currentUnits = new ArrayList<>(units);
        List<BuffTile> currentBuffTiles = new ArrayList<>(state.getBuffTiles());
        List<Obstacle> currentObstacles = new ArrayList<>(state.getObstacles());

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
            return executePreparingMove(prepUnit, actionMap, units);
        } else if ("ATTACK".equals(actionType)) {
            return executePreparingAttack(prepUnit, actionMap, units);
        } else if ("MOVE_AND_ATTACK".equals(actionType)) {
            return executePreparingMoveAndAttack(prepUnit, actionMap, units);
        }

        return units;
    }

    @SuppressWarnings("unchecked")
    private List<Unit> executePreparingMove(Unit prepUnit, Map<String, Object> actionMap, List<Unit> units) {
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

        return units;
    }

    @SuppressWarnings("unchecked")
    private List<Unit> executePreparingAttack(Unit prepUnit, Map<String, Object> actionMap, List<Unit> units) {
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

        return units;
    }

    @SuppressWarnings("unchecked")
    private List<Unit> executePreparingMoveAndAttack(Unit prepUnit, Map<String, Object> actionMap, List<Unit> units) {
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

        return units;
    }

    // =========================================================================
    // Minion Decay and Round 8 Pressure
    // =========================================================================

    /**
     * Apply minion decay: all minions lose 1 HP at round end.
     */
    public List<Unit> applyMinionDecay(List<Unit> units) {
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

    /**
     * Apply Round 8+ pressure: all units lose 1 HP at round end.
     */
    public List<Unit> applyRound8Pressure(List<Unit> units) {
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

    /**
     * Reset actions used and preparing state for all units at round end.
     */
    public List<Unit> resetActionsUsedAndPreparingState(List<Unit> units) {
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : units) {
            newUnits.add(u.withRoundEndReset());
        }
        return newUnits;
    }

    /**
     * Decrement temporary unit durations and remove expired or dead temporary units at round end.
     * Shadow Clone and similar temporary units have a duration that decreases each round.
     * Dead temporary units are also removed (they don't persist like regular minions).
     */
    public List<Unit> decrementTemporaryDurations(List<Unit> units) {
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : units) {
            if (u.isTemporary()) {
                // Remove dead temporary units
                if (!u.isAlive()) {
                    continue;
                }
                int newDuration = u.getTemporaryDuration() - 1;
                if (newDuration <= 0) {
                    // Temporary unit expired - remove it (don't add to list)
                    continue;
                }
                newUnits.add(u.withTemporaryDuration(newDuration));
            } else {
                newUnits.add(u);
            }
        }
        return newUnits;
    }

    // =========================================================================
    // Full Round End Processing
    // =========================================================================

    /**
     * Process a complete round end (called when all units have acted via END_TURN).
     */
    public GameState processRoundEnd(GameState state, TurnEndResult turnEndResult,
                                     GameOverChecker.GameOverResult gameOver) {
        PreparingActionsResult prepResult = executePreparingActions(state, turnEndResult.getUnits(),
            turnEndResult.getUnitBuffs());

        GameOverChecker.GameOverResult gameOverAfterPrep = gameOverChecker.checkGameOver(prepResult.getUnits());
        if (gameOverAfterPrep.isGameOver()) {
            gameOver = gameOverAfterPrep;
        }

        // Apply poison/bleed damage at round end (V3: moved from turn end to round end)
        List<Unit> unitsAfterPoison = applyPoisonAndBleedDamage(prepResult.getUnits(), turnEndResult.getUnitBuffs());

        GameOverChecker.GameOverResult gameOverAfterPoison = gameOverChecker.checkGameOver(unitsAfterPoison);
        if (gameOverAfterPoison.isGameOver()) {
            gameOver = gameOverAfterPoison;
        }

        // Decrement temporary unit durations and remove expired ones BEFORE minion decay
        // This ensures clones get their duration decremented even if they would die from decay
        List<Unit> unitsAfterTempDecrement = decrementTemporaryDurations(unitsAfterPoison);

        List<Unit> unitsAfterDecay = applyMinionDecay(unitsAfterTempDecrement);

        GameOverChecker.GameOverResult gameOverAfterDecay = gameOverChecker.checkGameOver(unitsAfterDecay);
        if (gameOverAfterDecay.isGameOver()) {
            gameOver = gameOverAfterDecay;
        }

        List<Unit> unitsAfterPressure = unitsAfterDecay;
        if (state.getCurrentRound() >= 8) {
            unitsAfterPressure = applyRound8Pressure(unitsAfterDecay);

            GameOverChecker.GameOverResult gameOverAfterPressure = gameOverChecker.checkGameOver(unitsAfterPressure);
            if (gameOverAfterPressure.isGameOver()) {
                gameOver = gameOverAfterPressure;
            }
        }

        List<Unit> unitsWithResetActions = resetActionsUsedAndPreparingState(unitsAfterPressure);

        // Decrement buff durations at round end (not per-turn)
        Map<String, List<BuffInstance>> buffsAfterDecrement = decrementBuffDurations(prepResult.getUnitBuffs());

        return new GameState(
            state.getBoard(),
            unitsWithResetActions,
            getNextPlayer(state.getCurrentPlayer()),
            gameOver.isGameOver(),
            gameOver.getWinner(),
            buffsAfterDecrement,
            prepResult.getBuffTiles(),
            prepResult.getObstacles(),
            state.getCurrentRound() + 1,
            state.getPendingDeathChoice(),
            false,
            false
        );
    }

    /**
     * Process round end after an action (MOVE/ATTACK/USE_SKILL) when all units have acted.
     * Simplified version that works with the current state rather than TurnEndResult.
     */
    public GameState processRoundEndAfterAction(GameState state) {
        // Execute preparing actions
        PreparingActionsResult prepResult = executePreparingActions(state, state.getUnits(), state.getUnitBuffs());

        GameOverChecker.GameOverResult gameOver = gameOverChecker.checkGameOver(prepResult.getUnits());

        // Apply poison/bleed damage at round end (V3: moved from turn end to round end)
        List<Unit> unitsAfterPoison = applyPoisonAndBleedDamage(prepResult.getUnits(), state.getUnitBuffs());

        GameOverChecker.GameOverResult gameOverAfterPoison = gameOverChecker.checkGameOver(unitsAfterPoison);
        if (gameOverAfterPoison.isGameOver()) {
            gameOver = gameOverAfterPoison;
        }

        // Decrement temporary unit durations and remove expired ones BEFORE minion decay
        List<Unit> unitsAfterTempDecrement = decrementTemporaryDurations(unitsAfterPoison);

        List<Unit> unitsAfterDecay = applyMinionDecay(unitsAfterTempDecrement);

        GameOverChecker.GameOverResult gameOverAfterDecay = gameOverChecker.checkGameOver(unitsAfterDecay);
        if (gameOverAfterDecay.isGameOver()) {
            gameOver = gameOverAfterDecay;
        }

        List<Unit> unitsAfterPressure = unitsAfterDecay;
        if (state.getCurrentRound() >= 8) {
            unitsAfterPressure = applyRound8Pressure(unitsAfterDecay);

            GameOverChecker.GameOverResult gameOverAfterPressure = gameOverChecker.checkGameOver(unitsAfterPressure);
            if (gameOverAfterPressure.isGameOver()) {
                gameOver = gameOverAfterPressure;
            }
        }

        List<Unit> unitsWithResetActions = resetActionsUsedAndPreparingState(unitsAfterPressure);

        // Decrement buff durations at round end (not per-turn)
        Map<String, List<BuffInstance>> buffsAfterDecrement = decrementBuffDurations(prepResult.getUnitBuffs());

        return new GameState(
            state.getBoard(),
            unitsWithResetActions,
            getNextPlayer(state.getCurrentPlayer()),
            gameOver.isGameOver(),
            gameOver.getWinner(),
            buffsAfterDecrement,
            prepResult.getBuffTiles(),
            prepResult.getObstacles(),
            state.getCurrentRound() + 1,
            state.getPendingDeathChoice(),
            false,
            false
        );
    }
}
