package com.tactics.engine.skill;

import com.tactics.engine.action.Action;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.util.RngProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes Warrior hero skills: Endure, Shockwave, Heroic Leap.
 */
public class WarriorSkillExecutor extends SkillExecutorBase {

    public WarriorSkillExecutor(RngProvider rngProvider) {
        super(rngProvider);
    }

    /**
     * Apply Warrior Endure skill.
     * Effect: Gain 3 shield for 2 rounds, remove BLEED debuff.
     */
    public GameState applyEndure(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int shieldAmount = skill.getShieldAmount();  // 3
        int cooldown = skill.getCooldown();  // 2

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withShieldAndSkillUsed(u.getShield() + shieldAmount, cooldown));

        Map<String, List<com.tactics.engine.buff.BuffInstance>> newUnitBuffs =
            removeBleedBuffs(state.getUnitBuffs(), actingUnit.getId());

        GameOverResult gameOver = checkGameOver(newUnits);

        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Warrior Shockwave skill.
     * Effect: Deal 1 damage to all adjacent enemies and push them 1 tile away.
     * If enemy cannot be pushed (blocked), deal +1 damage instead.
     */
    public GameState applyShockwave(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
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
                              !newPositions.containsValue(pushDest);

            // Check Guardian intercept
            Unit guardian = findGuardian(state, enemy);
            String damageReceiverId = (guardian != null) ? guardian.getId() : enemy.getId();

            if (canPush) {
                newPositions.put(enemy.getId(), pushDest);
                damageAmounts.merge(damageReceiverId, damage, Integer::sum);
            } else {
                damageAmounts.merge(damageReceiverId, damage + 1, Integer::sum);
            }
        }

        // Apply all changes
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown));
            } else if (newPositions.containsKey(u.getId())) {
                int totalDamage = damageAmounts.getOrDefault(u.getId(), 0);
                newUnits.add(u.withPosition(newPositions.get(u.getId())).withDamage(totalDamage));
            } else if (damageAmounts.containsKey(u.getId())) {
                newUnits.add(u.withDamage(damageAmounts.get(u.getId())));
            } else {
                newUnits.add(u);
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Warrior Heroic Leap skill.
     * Effect: Leap to any tile within range 3, deal 2 damage to all adjacent enemies on landing.
     */
    public GameState applyHeroicLeap(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        Position targetPos = action.getTargetPosition();
        int damage = skill.getDamageAmount();  // 2
        int cooldown = skill.getCooldown();  // 2

        // Find all enemies adjacent to landing position
        List<Unit> adjacentEnemies = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() &&
                !u.getOwner().getValue().equals(actingUnit.getOwner().getValue()) &&
                isAdjacent(targetPos, u.getPosition())) {
                adjacentEnemies.add(u);
            }
        }

        // Sort by ID for deterministic order
        adjacentEnemies.sort((a, b) -> a.getId().compareTo(b.getId()));

        // Track damage to each unit (including guardians)
        Map<String, Integer> damageAmounts = new HashMap<>();
        for (Unit enemy : adjacentEnemies) {
            Unit guardian = findGuardian(state, enemy);
            String damageReceiverId = (guardian != null) ? guardian.getId() : enemy.getId();
            damageAmounts.merge(damageReceiverId, damage, Integer::sum);
        }

        // Apply changes
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown).withPosition(targetPos));
            } else if (damageAmounts.containsKey(u.getId())) {
                newUnits.add(u.withDamage(damageAmounts.get(u.getId())));
            } else {
                newUnits.add(u);
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }
}
