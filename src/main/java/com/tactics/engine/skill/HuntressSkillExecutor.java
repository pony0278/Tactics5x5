package com.tactics.engine.skill;

import com.tactics.engine.action.Action;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.util.RngProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes Huntress hero skills: Spirit Hawk, Spectral Blades, Nature's Power.
 */
public class HuntressSkillExecutor extends SkillExecutorBase {

    public HuntressSkillExecutor(RngProvider rngProvider) {
        super(rngProvider);
    }

    /**
     * Apply Huntress Spirit Hawk skill.
     * Effect: Deal 2 damage to enemy at long range (4 tiles).
     */
    public GameState applySpiritHawk(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int damage = skill.getDamageAmount();  // 2
        int cooldown = skill.getCooldown();  // 2

        Unit guardian = findGuardian(state, targetUnit);
        Unit actualDamageReceiver = (guardian != null) ? guardian : targetUnit;
        String damageReceiverId = actualDamageReceiver.getId();

        Map<String, UnitTransformer> transformers = new HashMap<>();
        transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown));
        if (!actingUnit.getId().equals(damageReceiverId)) {
            transformers.put(damageReceiverId, u -> u.withDamage(damage));
        } else {
            transformers.put(actingUnit.getId(), u -> u.withSkillUsed(cooldown).withDamage(damage));
        }
        List<Unit> newUnits = updateUnitsInList(state.getUnits(), transformers);

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());

        return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Huntress Spectral Blades skill.
     * Effect: Deal 1 damage to all enemies in a straight line (piercing).
     */
    public GameState applySpectralBlades(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        Position targetPos = action.getTargetPosition();
        int damage = skill.getDamageAmount();  // 1
        int cooldown = skill.getCooldown();  // 2
        Position heroPos = actingUnit.getPosition();

        // Determine direction
        int dx = Integer.compare(targetPos.getX(), heroPos.getX());
        int dy = Integer.compare(targetPos.getY(), heroPos.getY());

        // Find all enemies in the line
        Map<String, Integer> damageAmounts = new HashMap<>();
        Position current = new Position(heroPos.getX() + dx, heroPos.getY() + dy);

        while (isInBounds(current, state.getBoard())) {
            // Check for enemies at this position
            for (Unit u : state.getUnits()) {
                if (u.isAlive() &&
                    !u.getOwner().getValue().equals(actingUnit.getOwner().getValue()) &&
                    u.getPosition().getX() == current.getX() &&
                    u.getPosition().getY() == current.getY()) {
                    // Check Guardian intercept
                    Unit guardian = findGuardian(state, u);
                    String damageReceiverId = (guardian != null) ? guardian.getId() : u.getId();
                    damageAmounts.merge(damageReceiverId, damage, Integer::sum);
                }
            }
            current = new Position(current.getX() + dx, current.getY() + dy);
        }

        // Apply changes
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown));
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
     * Apply Huntress Nature's Power skill.
     * Effect: Next 2 attacks deal +2 damage, gain LIFE buff (+3 HP instant).
     */
    public GameState applyNaturesPower(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int cooldown = skill.getCooldown();  // 2
        int bonusDamage = skill.getDamageAmount();  // 2 (bonus per attack)
        int attackCharges = 2;

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
}
