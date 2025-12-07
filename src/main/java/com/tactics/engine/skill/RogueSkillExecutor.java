package com.tactics.engine.skill;

import com.tactics.engine.action.Action;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;
import com.tactics.engine.util.RngProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes Rogue hero skills: Smoke Bomb, Death Mark, Shadow Clone.
 */
public class RogueSkillExecutor extends SkillExecutorBase {

    public RogueSkillExecutor(RngProvider rngProvider) {
        super(rngProvider);
    }

    /**
     * Apply Rogue Smoke Bomb skill.
     * Effect: Teleport to target tile, become invisible for 1 round, apply BLIND to adjacent enemies.
     */
    public GameState applySmokeBomb(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        Position targetPos = action.getTargetPosition();
        int cooldown = skill.getCooldown();  // 2
        Position originalPos = actingUnit.getPosition();

        // Find all enemies adjacent to original position (before teleport)
        List<Unit> adjacentEnemies = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() &&
                !u.getOwner().getValue().equals(actingUnit.getOwner().getValue()) &&
                isAdjacent(originalPos, u.getPosition())) {
                adjacentEnemies.add(u);
            }
        }

        // Update caster: teleport and become invisible
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown).withPosition(targetPos).withInvisible(true));
            } else {
                newUnits.add(u);
            }
        }

        // Apply BLIND to adjacent enemies
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        for (Unit enemy : adjacentEnemies) {
            BuffInstance blindBuff = BuffFactory.create(BuffType.BLIND, actingUnit.getId());
            List<BuffInstance> enemyBuffs = new ArrayList<>(
                newUnitBuffs.getOrDefault(enemy.getId(), Collections.emptyList())
            );
            enemyBuffs.add(blindBuff);
            newUnitBuffs.put(enemy.getId(), enemyBuffs);
        }

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Rogue Death Mark skill.
     * Effect: Mark target with DEATH_MARK buff, they take +2 damage from all sources.
     */
    public GameState applyDeathMark(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        int cooldown = skill.getCooldown();  // 2

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsed(cooldown));

        // Apply DEATH_MARK buff to target
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance deathMark = BuffFactory.create(BuffType.DEATH_MARK, actingUnit.getId());
        List<BuffInstance> targetBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(targetUnitId, Collections.emptyList())
        );
        targetBuffs.add(deathMark);
        newUnitBuffs.put(targetUnitId, targetBuffs);

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Rogue Shadow Clone skill.
     * Effect: Create a shadow clone minion at target position.
     */
    public GameState applyShadowClone(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        Position targetPos = action.getTargetPosition();
        int cooldown = skill.getCooldown();  // 2
        int cloneDuration = skill.getEffectDuration();  // 2 rounds

        // Update caster: use skill
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown));
            } else {
                newUnits.add(u);
            }
        }

        // Create the Shadow Clone
        String cloneId = actingUnit.getId() + "_clone_" + System.currentTimeMillis();
        Unit clone = new Unit(
            cloneId,
            actingUnit.getOwner(),
            1,  // HP = 1
            1,  // ATK = 1
            actingUnit.getMoveRange(),  // Same move range as Rogue
            1,  // Attack range = 1 (melee)
            targetPos,
            true,  // alive
            UnitCategory.MINION,  // Category: MINION (so it doesn't end game if killed)
            MinionType.ASSASSIN,  // Type: ASSASSIN (fast minion)
            null,  // No hero class
            1,  // maxHp = 1
            null,  // No skill
            0,    // skillCooldown
            0,    // shield
            false, // invisible
            false, // invulnerable
            true,  // isTemporary (clone disappears after duration)
            cloneDuration, // temporaryDuration
            null   // skillState
        );
        newUnits.add(clone);

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }
}
