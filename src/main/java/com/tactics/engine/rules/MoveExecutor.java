package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.BuffTile;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tactics.engine.rules.RuleEngineHelper.*;

/**
 * Handles MOVE action execution.
 * Extracted from ActionExecutor for better code organization.
 */
public class MoveExecutor extends ActionExecutorBase {

    private final GameOverChecker gameOverChecker;
    private final TurnManager turnManager;

    public MoveExecutor(GameOverChecker gameOverChecker, TurnManager turnManager) {
        this.gameOverChecker = gameOverChecker;
        this.turnManager = turnManager;
    }

    // =========================================================================
    // Buff Tile Trigger Result
    // =========================================================================

    /**
     * Result of triggering a buff tile.
     */
    public static class BuffTileTriggerResult {
        private final List<Unit> units;
        private final Map<String, List<BuffInstance>> unitBuffs;
        private final List<BuffTile> buffTiles;

        public BuffTileTriggerResult(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs,
                                     List<BuffTile> buffTiles) {
            this.units = units;
            this.unitBuffs = unitBuffs;
            this.buffTiles = buffTiles;
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
    }

    // =========================================================================
    // Apply Move
    // =========================================================================

    /**
     * Apply a MOVE action.
     */
    public GameState applyMove(GameState state, Action action) {
        Position targetPos = action.getTargetPosition();
        String actingUnitId = action.getActingUnitId();

        Unit mover = null;
        List<BuffInstance> moverBuffs = null;

        // Unit-by-unit turn system: if actingUnitId is provided, use it to identify the mover
        if (actingUnitId != null) {
            mover = findUnitById(state.getUnits(), actingUnitId);
            moverBuffs = getBuffsForUnit(state, mover.getId());
        } else {
            // Legacy behavior: find mover by position
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
        }

        if (hasSlowBuff(moverBuffs)) {
            return applySlowBuffPreparing(state, action, mover);
        }

        Unit movedUnit = mover.withPositionAndActionUsed(targetPos);
        List<Unit> newUnits = updateUnitInList(state.getUnits(), mover.getId(), u -> movedUnit);

        BuffTileTriggerResult tileResult = checkBuffTileTrigger(state, movedUnit, targetPos, newUnits, state.getUnitBuffs());

        GameOverChecker.GameOverResult gameOver = gameOverChecker.checkGameOver(tileResult.getUnits());

        // Create intermediate state to check for turn switch
        GameState tempState = new GameState(
            state.getBoard(),
            tileResult.getUnits(),
            state.getCurrentPlayer(),
            gameOver.isGameOver(),
            gameOver.getWinner(),
            tileResult.getUnitBuffs(),
            tileResult.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );

        // Unit-by-unit turn system: switch player after action if not game over
        if (!gameOver.isGameOver()) {
            // Check if acting unit still has actions (SPEED buff gives 2 actions)
            Unit actingUnitAfterMove = findUnitById(tileResult.getUnits(), mover.getId());
            if (!shouldSwitchTurnAfterAction(tempState, actingUnitAfterMove)) {
                // SPEED unit still has actions - don't switch turn
                return tempState;
            }

            // Check if round should end (all units acted)
            if (allUnitsActed(tempState)) {
                // Process round end - this resets all unit actions and increments round
                return turnManager.processRoundEndAfterAction(tempState);
            }
            PlayerId nextPlayer = getNextActingPlayer(tempState, state.getCurrentPlayer());
            return tempState.withCurrentPlayer(nextPlayer);
        }

        return tempState;
    }

    // =========================================================================
    // SLOW Buff Preparing
    // =========================================================================

    private GameState applySlowBuffPreparing(GameState state, Action action, Unit actingUnit) {
        Map<String, Object> preparingAction = serializeActionForPreparing(action);

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withPreparingAndActionUsed(preparingAction));

        return state.withUnits(newUnits);
    }

    // =========================================================================
    // Buff Tile Trigger
    // =========================================================================

    /**
     * Check if a unit stepped on a buff tile and trigger it.
     */
    public BuffTileTriggerResult checkBuffTileTrigger(GameState state, Unit movedUnit, Position newPos,
                                                      List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        BuffTile tile = state.getBuffTileAt(newPos);
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

        // BST1: Same buff type refreshes duration (no stacking)
        // Check if unit already has this buff type and refresh instead of adding
        boolean found = false;
        for (int i = 0; i < currentBuffs.size(); i++) {
            if (currentBuffs.get(i).getType() == buffType) {
                // Refresh duration - replace with new buff (which has full duration)
                currentBuffs.set(i, newBuff);
                found = true;
                break;
            }
        }
        if (!found) {
            currentBuffs.add(newBuff);
        }
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

        List<BuffTile> newBuffTiles = new ArrayList<>();
        for (BuffTile t : state.getBuffTiles()) {
            if (t.getId().equals(tile.getId())) {
                newBuffTiles.add(new BuffTile(
                    t.getId(), t.getPosition(), buffType, t.getDuration(), true
                ));
            } else {
                newBuffTiles.add(t);
            }
        }

        return new BuffTileTriggerResult(newUnits, newUnitBuffs, newBuffTiles);
    }
}
