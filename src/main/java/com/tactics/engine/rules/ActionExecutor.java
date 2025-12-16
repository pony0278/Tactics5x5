package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.BuffTile;
import com.tactics.engine.model.DeathChoice;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Obstacle;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.skill.SkillDefinition;
import com.tactics.engine.skill.SkillExecutor;
import com.tactics.engine.skill.SkillRegistry;
import com.tactics.engine.util.RngProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tactics.engine.rules.RuleEngineHelper.*;

/**
 * Executes game actions and updates game state.
 * Delegates to specialized executors for different action types.
 *
 * Handles execution for:
 * - MOVE (via MoveExecutor)
 * - ATTACK, MOVE_AND_ATTACK (via AttackExecutor)
 * - USE_SKILL (via SkillExecutor)
 * - DEATH_CHOICE, END_TURN (handled here)
 */
public class ActionExecutor {

    private final GameOverChecker gameOverChecker;
    private final TurnManager turnManager;
    private final MoveExecutor moveExecutor;
    private final AttackExecutor attackExecutor;
    private final SkillExecutor skillExecutor;
    private RngProvider rngProvider;

    public ActionExecutor() {
        this.gameOverChecker = new GameOverChecker();
        this.turnManager = new TurnManager();
        this.moveExecutor = new MoveExecutor(gameOverChecker, turnManager);
        this.attackExecutor = new AttackExecutor(gameOverChecker, turnManager, moveExecutor);
        this.skillExecutor = new SkillExecutor();
        this.rngProvider = new RngProvider();
    }

    public void setRngProvider(RngProvider rngProvider) {
        this.rngProvider = rngProvider;
        this.turnManager.setRngProvider(rngProvider);
        this.moveExecutor.setRngProvider(rngProvider);
        this.attackExecutor.setRngProvider(rngProvider);
        this.skillExecutor.setRngProvider(rngProvider);
    }

    /**
     * Get a random buff type for death choice buff tile spawn.
     */
    private BuffType getRandomBuffType() {
        BuffType[] types = BuffType.values();
        int roll = rngProvider.nextInt(types.length);
        return types[roll];
    }

    // =========================================================================
    // Main Entry Point
    // =========================================================================

    public GameState applyAction(GameState state, Action action) {
        ActionType type = action.getType();

        switch (type) {
            case END_TURN:
                return applyEndTurn(state, action);
            case MOVE:
                return moveExecutor.applyMove(state, action);
            case ATTACK:
                return attackExecutor.applyAttack(state, action);
            case MOVE_AND_ATTACK:
                return attackExecutor.applyMoveAndAttack(state, action);
            case DEATH_CHOICE:
                return applyDeathChoice(state, action);
            case USE_SKILL:
                return applyUseSkill(state, action);
            default:
                return null;
        }
    }

    // =========================================================================
    // END_TURN Action
    // =========================================================================

    private GameState applyEndTurn(GameState state, Action action) {
        PlayerId currentPlayer = state.getCurrentPlayer();
        String actingUnitId = action.getActingUnitId();

        // Unit-by-unit turn system: mark only the acting unit as acted
        List<Unit> unitsAfterEndTurn = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (actingUnitId != null && u.getId().equals(actingUnitId) && u.getActionsUsed() == 0) {
                // Mark only this specific unit as acted
                unitsAfterEndTurn.add(u.withActionsUsed(1));
            } else if (actingUnitId == null && u.isAlive() &&
                u.getOwner().getValue().equals(currentPlayer.getValue()) &&
                u.getActionsUsed() == 0) {
                // Legacy behavior: mark all current player's unacted units as acted
                unitsAfterEndTurn.add(u.withActionsUsed(1));
            } else {
                unitsAfterEndTurn.add(u);
            }
        }

        TurnManager.TurnEndResult turnEndResult = turnManager.processTurnEnd(
            unitsAfterEndTurn, state.getUnitBuffs());

        GameOverChecker.GameOverResult gameOver = gameOverChecker.checkGameOver(turnEndResult.getUnits());

        GameState tempState = new GameState(
            state.getBoard(),
            turnEndResult.getUnits(),
            currentPlayer,
            gameOver.isGameOver(),
            gameOver.getWinner(),
            turnEndResult.getUnitBuffs(),
            state.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );

        if (allUnitsActed(tempState)) {
            return turnManager.processRoundEnd(state, turnEndResult, gameOver);
        }

        PlayerId nextPlayer = getNextActingPlayer(tempState, currentPlayer);

        return new GameState(
            state.getBoard(),
            turnEndResult.getUnits(),
            nextPlayer,
            gameOver.isGameOver(),
            gameOver.getWinner(),
            turnEndResult.getUnitBuffs(),
            state.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );
    }

    // =========================================================================
    // Helper Methods for END_TURN
    // =========================================================================

    private boolean allUnitsActed(GameState state) {
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getActionsUsed() == 0) {
                return false;
            }
        }
        return true;
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

    // =========================================================================
    // DEATH_CHOICE Action
    // =========================================================================

    private GameState applyDeathChoice(GameState state, Action action) {
        DeathChoice deathChoice = state.getPendingDeathChoice();
        DeathChoice.ChoiceType choiceType = action.getDeathChoiceType();
        Position deathPos = deathChoice.getDeathPosition();

        List<Obstacle> newObstacles = new ArrayList<>(state.getObstacles());
        List<BuffTile> newBuffTiles = new ArrayList<>(state.getBuffTiles());

        // V3 Spec Section 2.4 & 7.3: Overwrite Rule - remove existing map object at position
        newObstacles.removeIf(o -> o.getPosition().equals(deathPos));
        newBuffTiles.removeIf(t -> t.getPosition().equals(deathPos) && !t.isTriggered());

        if (choiceType == DeathChoice.ChoiceType.SPAWN_OBSTACLE) {
            String obstacleId = "obstacle_" + System.currentTimeMillis();
            newObstacles.add(new Obstacle(obstacleId, deathPos));
        } else if (choiceType == DeathChoice.ChoiceType.SPAWN_BUFF_TILE) {
            String tileId = "bufftile_" + System.currentTimeMillis();
            BuffType buffType = getRandomBuffType();  // Random buff type per spec
            newBuffTiles.add(new BuffTile(tileId, deathPos, buffType, 2, false));
        }

        // Create intermediate state with death choice cleared
        GameState tempState = new GameState(
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

        // After death choice is resolved, determine next player
        if (allUnitsActed(tempState)) {
            return turnManager.processRoundEndAfterAction(tempState);
        }
        PlayerId nextPlayer = getNextActingPlayer(tempState, state.getCurrentPlayer());
        return tempState.withCurrentPlayer(nextPlayer);
    }

    // =========================================================================
    // USE_SKILL Action
    // =========================================================================

    private GameState applyUseSkill(GameState state, Action action) {
        String actingUnitId = action.getActingUnitId();
        Unit actingUnit = findUnitById(state.getUnits(), actingUnitId);
        String skillId = actingUnit.getSelectedSkillId();
        SkillDefinition skill = SkillRegistry.getSkill(skillId);

        // Check if acting unit has SLOW buff - skill execution is delayed by 1 round
        List<BuffInstance> actingUnitBuffs = getBuffsForUnit(state, actingUnitId);
        if (hasSlowBuff(actingUnitBuffs)) {
            return applySlowBuffPreparingSkill(state, action, actingUnit);
        }

        GameState result = skillExecutor.executeSkill(state, action, actingUnit, skill);

        if (actingUnit.isInvisible() && !skillId.equals(SkillRegistry.ROGUE_SMOKE_BOMB)) {
            result = clearInvisibleOnUnit(result, actingUnitId);
        }

        return result;
    }

    private GameState applySlowBuffPreparingSkill(GameState state, Action action, Unit actingUnit) {
        Map<String, Object> preparingAction = new HashMap<>();
        preparingAction.put("type", "USE_SKILL");
        if (action.getTargetUnitId() != null) {
            preparingAction.put("targetUnitId", action.getTargetUnitId());
        }
        if (action.getSkillTargetUnitId() != null) {
            preparingAction.put("skillTargetUnitId", action.getSkillTargetUnitId());
        }
        if (action.getTargetPosition() != null) {
            Map<String, Object> posMap = new HashMap<>();
            posMap.put("x", action.getTargetPosition().getX());
            posMap.put("y", action.getTargetPosition().getY());
            preparingAction.put("targetPosition", posMap);
        }

        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withPreparingAndActionUsed(preparingAction));
            } else {
                newUnits.add(u);
            }
        }

        return state.withUnits(newUnits);
    }

    private GameState clearInvisibleOnUnit(GameState state, String unitId) {
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(unitId)) {
                newUnits.add(u.withInvisible(false));
            } else {
                newUnits.add(u);
            }
        }
        return state.withUnits(newUnits);
    }
}
