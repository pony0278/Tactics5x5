package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.model.DeathChoice;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Obstacle;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tactics.engine.rules.RuleEngineHelper.*;

/**
 * Handles ATTACK and MOVE_AND_ATTACK action execution.
 * Extracted from ActionExecutor for better code organization.
 */
public class AttackExecutor extends ActionExecutorBase {

    private final GameOverChecker gameOverChecker;
    private final TurnManager turnManager;
    private final MoveExecutor moveExecutor;

    public AttackExecutor(GameOverChecker gameOverChecker, TurnManager turnManager, MoveExecutor moveExecutor) {
        this.gameOverChecker = gameOverChecker;
        this.turnManager = turnManager;
        this.moveExecutor = moveExecutor;
    }

    // =========================================================================
    // Apply Attack
    // =========================================================================

    /**
     * Apply an ATTACK action.
     */
    public GameState applyAttack(GameState state, Action action) {
        String targetUnitId = action.getTargetUnitId();
        Position targetPos = action.getTargetPosition();

        boolean isAttackingObstacle = targetUnitId == null || targetUnitId.startsWith(Obstacle.ID_PREFIX);

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

        // Check if target has FEINT buff - attack misses
        List<BuffInstance> targetBuffsForFeint = getBuffsForUnit(state, targetUnitId);
        boolean targetHasFeint = hasFeintBuff(targetBuffsForFeint);
        if (targetHasFeint) {
            return applyFeintDodge(state, action, attacker, targetUnit);
        }

        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        // Get buffs on target for DEATH_MARK bonus and INVULNERABLE check
        List<BuffInstance> targetBuffs = getBuffsForUnit(state, damageReceiverId);

        // Check if target is invulnerable - prevent all damage
        if (hasInvulnerableBuff(targetBuffs)) {
            return applyInvulnerableMiss(state, action, attacker);
        }

        int deathMarkBonus = hasDeathMarkBuff(targetBuffs) ? 2 : 0;
        String deathMarkSource = getDeathMarkSource(targetBuffs);

        // Check if attacker has CHALLENGE buff - deals 50% damage to non-Duelist targets
        String challengeSource = getChallengeSource(attackerBuffs);
        boolean challengedAttackingNonDuelist = challengeSource != null && !damageReceiverId.equals(challengeSource);
        boolean challengedAttackingDuelist = challengeSource != null && damageReceiverId.equals(challengeSource);

        int bonusAttack = getBonusAttack(attackerBuffs);
        int naturesPowerBonus = attacker.getBonusAttackCharges() > 0 ? attacker.getBonusAttackDamage() : 0;
        int baseDamage = attacker.getAttack() + bonusAttack + naturesPowerBonus + deathMarkBonus;

        // Apply 50% damage reduction if challenged and attacking non-Duelist
        int totalDamage = challengedAttackingNonDuelist ? baseDamage / 2 : baseDamage;

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

        // DEATH_MARK heal on kill: if target died and had death mark, heal the source 2 HP
        if (deathMarkSource != null) {
            Unit targetAfterDamage = findUnitById(newUnits, damageReceiverId);
            if (targetAfterDamage != null && !targetAfterDamage.isAlive()) {
                // Target died, heal the death mark source 2 HP
                newUnits = updateUnitInList(newUnits, deathMarkSource, u -> u.withHpBonus(2));
            }
        }

        // CHALLENGE counter-attack: Duelist counter-attacks for 2 damage when attacked by challenged enemy
        if (challengedAttackingDuelist) {
            // Find guardian that might intercept counter-attack
            Unit counterTarget = findGuardian(state, attacker);
            if (counterTarget == null) {
                counterTarget = attacker;
            }
            String counterTargetId = counterTarget.getId();
            newUnits = updateUnitInList(newUnits, counterTargetId, u -> u.withDamage(2));
        }

        GameOverChecker.GameOverResult gameOver = gameOverChecker.checkGameOver(newUnits, action.getPlayerId());

        // Check for minion death - only if game is not over (hero death takes priority)
        DeathChoice deathChoice = null;
        if (!gameOver.isGameOver()) {
            deathChoice = gameOverChecker.checkMinionDeath(newUnits, state.getUnits());
        }

        // Create intermediate state
        GameState tempState = new GameState(
            state.getBoard(),
            newUnits,
            state.getCurrentPlayer(),
            gameOver.isGameOver(),
            gameOver.getWinner(),
            state.getUnitBuffs(),
            state.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            deathChoice != null ? deathChoice : state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );

        // Unit-by-unit turn system: switch player after action if not game over and no death choice
        if (!gameOver.isGameOver() && deathChoice == null) {
            // Check if acting unit still has actions (SPEED buff gives 2 actions)
            Unit attackerAfterAction = findUnitById(newUnits, attacker.getId());
            if (attackerAfterAction != null && !shouldSwitchTurnAfterAction(tempState, attackerAfterAction)) {
                // SPEED unit still has actions - don't switch turn
                return tempState;
            }

            if (allUnitsActed(tempState)) {
                return turnManager.processRoundEndAfterAction(tempState);
            }
            PlayerId nextPlayer = getNextActingPlayer(tempState, state.getCurrentPlayer());
            return tempState.withCurrentPlayer(nextPlayer);
        }

        return tempState;
    }

    // =========================================================================
    // Apply Attack Obstacle
    // =========================================================================

    private GameState applyAttackObstacle(GameState state, Action action, Unit attacker,
                                          List<BuffInstance> attackerBuffs, Position targetPos) {
        int bonusAttack = getBonusAttack(attackerBuffs);
        int naturesPowerBonus = attacker.getBonusAttackCharges() > 0 ? attacker.getBonusAttackDamage() : 0;
        int totalDamage = attacker.getAttack() + bonusAttack + naturesPowerBonus;

        boolean hasPower = hasPowerBuff(attackerBuffs);

        List<Obstacle> newObstacles = new ArrayList<>();
        for (Obstacle o : state.getObstacles()) {
            if (o.getPosition().equals(targetPos)) {
                if (hasPower) {
                    continue;
                } else {
                    Obstacle damaged = o.withDamage(totalDamage);
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

    // =========================================================================
    // Apply Move And Attack
    // =========================================================================

    /**
     * Apply a MOVE_AND_ATTACK action.
     */
    public GameState applyMoveAndAttack(GameState state, Action action) {
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

        MoveExecutor.BuffTileTriggerResult tileResult = moveExecutor.checkBuffTileTrigger(
            state, movedUnit, targetPos, newUnits, state.getUnitBuffs());

        TurnManager.TurnEndResult turnEndResult = turnManager.processTurnEnd(
            tileResult.getUnits(), tileResult.getUnitBuffs());

        GameOverChecker.GameOverResult gameOver = gameOverChecker.checkGameOver(
            turnEndResult.getUnits(), action.getPlayerId());

        // Check for minion death - only if game is not over (hero death takes priority)
        DeathChoice deathChoice = null;
        if (!gameOver.isGameOver()) {
            deathChoice = gameOverChecker.checkMinionDeath(turnEndResult.getUnits(), state.getUnits());
        }

        GameState tempState = new GameState(
            state.getBoard(),
            turnEndResult.getUnits(),
            state.getCurrentPlayer(),
            gameOver.isGameOver(),
            gameOver.getWinner(),
            turnEndResult.getUnitBuffs(),
            tileResult.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            deathChoice != null ? deathChoice : state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );

        if (allUnitsActed(tempState)) {
            return turnManager.processRoundEnd(state, turnEndResult, gameOver);
        }

        PlayerId nextPlayer = getNextActingPlayer(tempState, state.getCurrentPlayer());

        return new GameState(
            state.getBoard(),
            turnEndResult.getUnits(),
            nextPlayer,
            gameOver.isGameOver(),
            gameOver.getWinner(),
            turnEndResult.getUnitBuffs(),
            tileResult.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            deathChoice != null ? deathChoice : state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );
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
    // FEINT Dodge and Counter
    // =========================================================================

    /**
     * Apply FEINT dodge: attack misses, counter-attack for 2 damage, consume FEINT buff.
     */
    private GameState applyFeintDodge(GameState state, Action action, Unit attacker, Unit target) {
        // Attack misses - no damage to target
        // Counter-attack: target deals 2 damage to attacker (or guardian)
        Unit counterTarget = findGuardian(state, attacker);
        if (counterTarget == null) {
            counterTarget = attacker;
        }
        String counterTargetId = counterTarget.getId();

        // Apply counter damage and mark attacker as used action
        Map<String, UnitTransformer> transformers = new HashMap<>();
        transformers.put(counterTargetId, u -> u.withDamage(2));
        if (!attacker.getId().equals(counterTargetId)) {
            transformers.put(attacker.getId(), Unit::withActionUsed);
        } else {
            transformers.put(counterTargetId, u -> u.withDamage(2).withActionUsed());
        }
        List<Unit> newUnits = updateUnitsInList(state.getUnits(), transformers);

        // Remove FEINT buff from target (consumed after dodge)
        Map<String, List<BuffInstance>> newUnitBuffs = removeBuffFromUnit(
            state.getUnitBuffs(), target.getId(), buff -> buff.getFlags() != null && buff.getFlags().isFeintBuff());

        GameOverChecker.GameOverResult gameOver = gameOverChecker.checkGameOver(newUnits, action.getPlayerId());

        DeathChoice deathChoice = null;
        if (!gameOver.isGameOver()) {
            deathChoice = gameOverChecker.checkMinionDeath(newUnits, state.getUnits());
        }

        GameState tempState = new GameState(
            state.getBoard(),
            newUnits,
            state.getCurrentPlayer(),
            gameOver.isGameOver(),
            gameOver.getWinner(),
            newUnitBuffs,
            state.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            deathChoice != null ? deathChoice : state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );

        // Unit-by-unit turn system: switch player after action if not game over and no death choice
        if (!gameOver.isGameOver() && deathChoice == null) {
            Unit attackerAfterAction = findUnitById(newUnits, attacker.getId());
            if (attackerAfterAction != null && !shouldSwitchTurnAfterAction(tempState, attackerAfterAction)) {
                return tempState;
            }

            if (allUnitsActed(tempState)) {
                return turnManager.processRoundEndAfterAction(tempState);
            }
            PlayerId nextPlayer = getNextActingPlayer(tempState, state.getCurrentPlayer());
            return tempState.withCurrentPlayer(nextPlayer);
        }

        return tempState;
    }

    /**
     * Apply INVULNERABLE miss: attack deals no damage, attacker uses action.
     */
    private GameState applyInvulnerableMiss(GameState state, Action action, Unit attacker) {
        // Attack misses - no damage to target, only mark attacker as used action
        List<Unit> newUnits = updateUnitInList(state.getUnits(), attacker.getId(), Unit::withActionUsed);

        GameState tempState = new GameState(
            state.getBoard(),
            newUnits,
            state.getCurrentPlayer(),
            state.isGameOver(),
            state.getWinner(),
            state.getUnitBuffs(),
            state.getBuffTiles(),
            state.getObstacles(),
            state.getCurrentRound(),
            state.getPendingDeathChoice(),
            state.isPlayer1TurnEnded(),
            state.isPlayer2TurnEnded()
        );

        // Unit-by-unit turn system: switch player after action
        Unit attackerAfterAction = findUnitById(newUnits, attacker.getId());
        if (attackerAfterAction != null && !shouldSwitchTurnAfterAction(tempState, attackerAfterAction)) {
            return tempState;
        }

        if (allUnitsActed(tempState)) {
            return turnManager.processRoundEndAfterAction(tempState);
        }
        PlayerId nextPlayer = getNextActingPlayer(tempState, state.getCurrentPlayer());
        return tempState.withCurrentPlayer(nextPlayer);
    }

    /**
     * Remove a buff from a unit's buff list based on a predicate.
     */
    private Map<String, List<BuffInstance>> removeBuffFromUnit(
            Map<String, List<BuffInstance>> unitBuffs, String unitId,
            java.util.function.Predicate<BuffInstance> buffMatcher) {
        if (unitBuffs == null) return new HashMap<>();
        Map<String, List<BuffInstance>> newBuffs = new HashMap<>(unitBuffs);
        List<BuffInstance> buffs = newBuffs.get(unitId);
        if (buffs != null && !buffs.isEmpty()) {
            List<BuffInstance> filtered = new ArrayList<>();
            for (BuffInstance buff : buffs) {
                if (!buffMatcher.test(buff)) {
                    filtered.add(buff);
                }
            }
            if (filtered.isEmpty()) {
                newBuffs.remove(unitId);
            } else {
                newBuffs.put(unitId, filtered);
            }
        }
        return newBuffs;
    }
}
