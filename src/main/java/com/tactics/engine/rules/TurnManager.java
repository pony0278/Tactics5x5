package com.tactics.engine.rules;

import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
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

    // setRngProvider() inherited from ActionExecutorBase

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
    // System Death Auto-Spawn (V3 Spec Section 7.2)
    // =========================================================================

    /**
     * Result of system death processing, containing updated lists.
     */
    public static class SystemDeathResult {
        private final List<Unit> units;
        private final List<Obstacle> obstacles;
        private final List<BuffTile> buffTiles;

        public SystemDeathResult(List<Unit> units, List<Obstacle> obstacles, List<BuffTile> buffTiles) {
            this.units = units;
            this.obstacles = obstacles;
            this.buffTiles = buffTiles;
        }

        public List<Unit> getUnits() { return units; }
        public List<Obstacle> getObstacles() { return obstacles; }
        public List<BuffTile> getBuffTiles() { return buffTiles; }
    }

    /**
     * Process system deaths (from BLEED, Decay, or Pressure) and auto-spawn map objects.
     * V3 Spec Section 7.2 - Scenario B: System Death (Environment)
     * - Odd Rounds (3, 5, 7...): Spawns OBSTACLE
     * - Even Rounds (4, 6, 8...): Spawns BUFF_TILE
     *
     * @param unitsBefore Units before the system damage was applied
     * @param unitsAfter Units after the system damage was applied
     * @param currentObstacles Current obstacles on the board
     * @param currentBuffTiles Current buff tiles on the board
     * @param currentRound The current round number
     * @return SystemDeathResult with updated units, obstacles, and buff tiles
     */
    public SystemDeathResult processSystemDeaths(List<Unit> unitsBefore, List<Unit> unitsAfter,
                                                  List<Obstacle> currentObstacles, List<BuffTile> currentBuffTiles,
                                                  int currentRound) {
        List<Obstacle> newObstacles = new ArrayList<>(currentObstacles);
        List<BuffTile> newBuffTiles = new ArrayList<>(currentBuffTiles);

        // Find minions that died from system damage (excluding temporary units)
        List<Unit> deadMinions = findSystemDeadMinions(unitsBefore, unitsAfter);

        // Determine spawn type based on round parity (Odd = OBSTACLE, Even = BUFF_TILE)
        boolean spawnObstacle = (currentRound % 2) == 1;  // Odd rounds

        for (Unit deadMinion : deadMinions) {
            Position deathPos = deadMinion.getPosition();

            // V3 Spec Section 2.4 & 7.3: Overwrite Rule - remove existing map object at position
            removeMapObjectAtPosition(newObstacles, newBuffTiles, deathPos);

            if (spawnObstacle) {
                String obstacleId = Obstacle.ID_PREFIX + deadMinion.getId() + "_" + currentRound;
                newObstacles.add(new Obstacle(obstacleId, deathPos));
            } else {
                String tileId = "bufftile_" + deadMinion.getId() + "_" + currentRound;
                BuffType buffType = getRandomBuffType();
                newBuffTiles.add(new BuffTile(tileId, deathPos, buffType, 2, false));
            }
        }

        return new SystemDeathResult(unitsAfter, newObstacles, newBuffTiles);
    }

    /**
     * Find minions that died between two unit states (system death detection).
     * Only includes non-temporary minions that were alive before and dead after.
     */
    private List<Unit> findSystemDeadMinions(List<Unit> unitsBefore, List<Unit> unitsAfter) {
        // Build map of alive minions before damage
        Map<String, Unit> aliveBeforeMap = new HashMap<>();
        for (Unit u : unitsBefore) {
            if (u.isAlive() && u.getCategory() == UnitCategory.MINION && !u.isTemporary()) {
                aliveBeforeMap.put(u.getId(), u);
            }
        }

        // Find minions that died
        List<Unit> deadMinions = new ArrayList<>();
        for (Unit u : unitsAfter) {
            if (!u.isAlive() && u.getCategory() == UnitCategory.MINION
                && !u.isTemporary() && aliveBeforeMap.containsKey(u.getId())) {
                deadMinions.add(u);
            }
        }

        // Sort by ID for deterministic order
        deadMinions.sort((a, b) -> a.getId().compareTo(b.getId()));
        return deadMinions;
    }

    /**
     * Remove any existing map object (obstacle or buff tile) at the given position.
     * Implements V3 Spec Section 2.4: Overwrite Rule.
     */
    private void removeMapObjectAtPosition(List<Obstacle> obstacles, List<BuffTile> buffTiles, Position pos) {
        obstacles.removeIf(o -> o.getPosition().equals(pos));
        buffTiles.removeIf(t -> t.getPosition().equals(pos) && !t.isTriggered());
    }

    // Note: getRandomBuffType() is inherited from ActionExecutorBase

    // =========================================================================
    // Full Round End Processing
    // =========================================================================

    /**
     * Process a complete round end (called when all units have acted via END_TURN).
     * V3 Spec Section 6: Round End Processing Order
     */
    public GameState processRoundEnd(GameState state, TurnEndResult turnEndResult,
                                     GameOverChecker.GameOverResult gameOver) {
        PreparingActionsResult prepResult = executePreparingActions(state, turnEndResult.getUnits(),
            turnEndResult.getUnitBuffs());

        GameOverChecker.GameOverResult gameOverAfterPrep = gameOverChecker.checkGameOver(prepResult.getUnits());
        if (gameOverAfterPrep.isGameOver()) {
            gameOver = gameOverAfterPrep;
        }

        // Track map objects for system death spawning
        List<Obstacle> currentObstacles = new ArrayList<>(prepResult.getObstacles());
        List<BuffTile> currentBuffTiles = new ArrayList<>(prepResult.getBuffTiles());
        int currentRound = state.getCurrentRound();

        // Step 1: Apply BLEED damage at round end
        List<Unit> unitsBeforeBleed = prepResult.getUnits();
        List<Unit> unitsAfterBleed = applyPoisonAndBleedDamage(unitsBeforeBleed, turnEndResult.getUnitBuffs());

        GameOverChecker.GameOverResult gameOverAfterBleed = gameOverChecker.checkGameOver(unitsAfterBleed);
        if (gameOverAfterBleed.isGameOver()) {
            gameOver = gameOverAfterBleed;
        }

        // Process system deaths from BLEED
        SystemDeathResult bleedDeathResult = processSystemDeaths(
            unitsBeforeBleed, unitsAfterBleed, currentObstacles, currentBuffTiles, currentRound);
        currentObstacles = bleedDeathResult.getObstacles();
        currentBuffTiles = bleedDeathResult.getBuffTiles();

        // Decrement temporary unit durations and remove expired ones BEFORE minion decay
        List<Unit> unitsAfterTempDecrement = decrementTemporaryDurations(unitsAfterBleed);

        // Step 2: Minion Decay (V3 Spec: starts at Round 3)
        List<Unit> unitsAfterDecay = unitsAfterTempDecrement;
        if (currentRound >= 3) {
            List<Unit> unitsBeforeDecay = unitsAfterTempDecrement;
            unitsAfterDecay = applyMinionDecay(unitsBeforeDecay);

            GameOverChecker.GameOverResult gameOverAfterDecay = gameOverChecker.checkGameOver(unitsAfterDecay);
            if (gameOverAfterDecay.isGameOver()) {
                gameOver = gameOverAfterDecay;
            }

            // Process system deaths from Decay
            SystemDeathResult decayDeathResult = processSystemDeaths(
                unitsBeforeDecay, unitsAfterDecay, currentObstacles, currentBuffTiles, currentRound);
            currentObstacles = decayDeathResult.getObstacles();
            currentBuffTiles = decayDeathResult.getBuffTiles();
        }

        // Step 3: Late Game Pressure (V3 Spec: starts at Round 8)
        List<Unit> unitsAfterPressure = unitsAfterDecay;
        if (currentRound >= 8) {
            List<Unit> unitsBeforePressure = unitsAfterDecay;
            unitsAfterPressure = applyRound8Pressure(unitsBeforePressure);

            GameOverChecker.GameOverResult gameOverAfterPressure = gameOverChecker.checkGameOver(unitsAfterPressure);
            if (gameOverAfterPressure.isGameOver()) {
                gameOver = gameOverAfterPressure;
            }

            // Process system deaths from Pressure
            SystemDeathResult pressureDeathResult = processSystemDeaths(
                unitsBeforePressure, unitsAfterPressure, currentObstacles, currentBuffTiles, currentRound);
            currentObstacles = pressureDeathResult.getObstacles();
            currentBuffTiles = pressureDeathResult.getBuffTiles();
        }

        // Step 5: Victory check already done above

        // Step 6: Duration tick - decrement buff durations
        List<Unit> unitsWithResetActions = resetActionsUsedAndPreparingState(unitsAfterPressure);
        Map<String, List<BuffInstance>> buffsAfterDecrement = decrementBuffDurations(prepResult.getUnitBuffs());

        // Decrement buff tile durations and remove expired
        List<BuffTile> buffTilesAfterDecrement = decrementBuffTileDurations(currentBuffTiles);

        // Step 7: Increment round (handled in GameState constructor below)
        return new GameState(
            state.getBoard(),
            unitsWithResetActions,
            getNextPlayer(state.getCurrentPlayer()),
            gameOver.isGameOver(),
            gameOver.getWinner(),
            buffsAfterDecrement,
            buffTilesAfterDecrement,
            currentObstacles,
            currentRound + 1,
            null,  // Clear pending death choice at round end
            false,
            false
        );
    }

    /**
     * Decrement buff tile durations and remove expired tiles.
     */
    private List<BuffTile> decrementBuffTileDurations(List<BuffTile> buffTiles) {
        List<BuffTile> result = new ArrayList<>();
        for (BuffTile tile : buffTiles) {
            if (tile.isTriggered()) {
                continue;  // Already triggered, remove it
            }
            int newDuration = tile.getDuration() - 1;
            if (newDuration > 0) {
                result.add(new BuffTile(tile.getId(), tile.getPosition(), tile.getBuffType(), newDuration, false));
            }
            // Duration <= 0: tile expired, don't add to result
        }
        return result;
    }

    /**
     * Process round end after an action (MOVE/ATTACK/USE_SKILL) when all units have acted.
     * Simplified version that works with the current state rather than TurnEndResult.
     * V3 Spec Section 6: Round End Processing Order
     */
    public GameState processRoundEndAfterAction(GameState state) {
        // Execute preparing actions
        PreparingActionsResult prepResult = executePreparingActions(state, state.getUnits(), state.getUnitBuffs());

        GameOverChecker.GameOverResult gameOver = gameOverChecker.checkGameOver(prepResult.getUnits());

        // Track map objects for system death spawning
        List<Obstacle> currentObstacles = new ArrayList<>(prepResult.getObstacles());
        List<BuffTile> currentBuffTiles = new ArrayList<>(prepResult.getBuffTiles());
        int currentRound = state.getCurrentRound();

        // Step 1: Apply BLEED damage at round end
        List<Unit> unitsBeforeBleed = prepResult.getUnits();
        List<Unit> unitsAfterBleed = applyPoisonAndBleedDamage(unitsBeforeBleed, state.getUnitBuffs());

        GameOverChecker.GameOverResult gameOverAfterBleed = gameOverChecker.checkGameOver(unitsAfterBleed);
        if (gameOverAfterBleed.isGameOver()) {
            gameOver = gameOverAfterBleed;
        }

        // Process system deaths from BLEED
        SystemDeathResult bleedDeathResult = processSystemDeaths(
            unitsBeforeBleed, unitsAfterBleed, currentObstacles, currentBuffTiles, currentRound);
        currentObstacles = bleedDeathResult.getObstacles();
        currentBuffTiles = bleedDeathResult.getBuffTiles();

        // Decrement temporary unit durations and remove expired ones BEFORE minion decay
        List<Unit> unitsAfterTempDecrement = decrementTemporaryDurations(unitsAfterBleed);

        // Step 2: Minion Decay (V3 Spec: starts at Round 3)
        List<Unit> unitsAfterDecay = unitsAfterTempDecrement;
        if (currentRound >= 3) {
            List<Unit> unitsBeforeDecay = unitsAfterTempDecrement;
            unitsAfterDecay = applyMinionDecay(unitsBeforeDecay);

            GameOverChecker.GameOverResult gameOverAfterDecay = gameOverChecker.checkGameOver(unitsAfterDecay);
            if (gameOverAfterDecay.isGameOver()) {
                gameOver = gameOverAfterDecay;
            }

            // Process system deaths from Decay
            SystemDeathResult decayDeathResult = processSystemDeaths(
                unitsBeforeDecay, unitsAfterDecay, currentObstacles, currentBuffTiles, currentRound);
            currentObstacles = decayDeathResult.getObstacles();
            currentBuffTiles = decayDeathResult.getBuffTiles();
        }

        // Step 3: Late Game Pressure (V3 Spec: starts at Round 8)
        List<Unit> unitsAfterPressure = unitsAfterDecay;
        if (currentRound >= 8) {
            List<Unit> unitsBeforePressure = unitsAfterDecay;
            unitsAfterPressure = applyRound8Pressure(unitsBeforePressure);

            GameOverChecker.GameOverResult gameOverAfterPressure = gameOverChecker.checkGameOver(unitsAfterPressure);
            if (gameOverAfterPressure.isGameOver()) {
                gameOver = gameOverAfterPressure;
            }

            // Process system deaths from Pressure
            SystemDeathResult pressureDeathResult = processSystemDeaths(
                unitsBeforePressure, unitsAfterPressure, currentObstacles, currentBuffTiles, currentRound);
            currentObstacles = pressureDeathResult.getObstacles();
            currentBuffTiles = pressureDeathResult.getBuffTiles();
        }

        // Step 5: Victory check already done above

        // Step 6: Duration tick - decrement buff durations
        List<Unit> unitsWithResetActions = resetActionsUsedAndPreparingState(unitsAfterPressure);
        Map<String, List<BuffInstance>> buffsAfterDecrement = decrementBuffDurations(prepResult.getUnitBuffs());

        // Decrement buff tile durations and remove expired
        List<BuffTile> buffTilesAfterDecrement = decrementBuffTileDurations(currentBuffTiles);

        // Step 7: Increment round (handled in GameState constructor below)
        return new GameState(
            state.getBoard(),
            unitsWithResetActions,
            getNextPlayer(state.getCurrentPlayer()),
            gameOver.isGameOver(),
            gameOver.getWinner(),
            buffsAfterDecrement,
            buffTilesAfterDecrement,
            currentObstacles,
            currentRound + 1,
            null,  // Clear pending death choice at round end
            false,
            false
        );
    }
}
