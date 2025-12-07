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
 * Executes Mage hero skills: Elemental Blast, Warp Beacon, Wild Magic.
 */
public class MageSkillExecutor extends SkillExecutorBase {

    public MageSkillExecutor(RngProvider rngProvider) {
        super(rngProvider);
    }

    /**
     * Apply Mage Elemental Blast skill.
     * Effect: Deal 3 damage to target, 50% chance to apply random debuff (WEAKNESS, BLEED, or SLOW).
     */
    public GameState applyElementalBlast(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int damage = skill.getDamageAmount();  // 3
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

        // 50% chance to apply random debuff
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        if (rngProvider.nextInt(100) < 50) {
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
     * Apply Mage Warp Beacon skill.
     * Effect: If no beacon placed, place beacon at target. If beacon exists, teleport to it.
     */
    public GameState applyWarpBeacon(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        Position targetPos = action.getTargetPosition();
        int cooldown = skill.getCooldown();  // 2

        // Check if beacon already exists
        Map<String, Object> skillState = actingUnit.getSkillState();
        boolean hasBeacon = skillState != null && skillState.containsKey("beacon_x");

        if (!hasBeacon) {
            // First use: place beacon at target position
            Map<String, Object> newSkillState = new HashMap<>();
            newSkillState.put("beacon_x", targetPos.getX());
            newSkillState.put("beacon_y", targetPos.getY());

            // Update unit with beacon placed (NO cooldown trigger)
            List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
                u -> u.withSkillStateAndActionUsed(newSkillState));

            return state.withUnits(newUnits);
        } else {
            // Second use: teleport to beacon position
            int beaconX = (Integer) skillState.get("beacon_x");
            int beaconY = (Integer) skillState.get("beacon_y");
            Position beaconPos = new Position(beaconX, beaconY);

            // Update unit: teleport to beacon, clear skill state, trigger cooldown
            List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
                u -> u.withPositionSkillStateClearedAndSkillUsed(beaconPos, cooldown));

            GameOverResult gameOver = checkGameOver(newUnits);
            return state.withUpdates(newUnits, state.getUnitBuffs(), gameOver.isGameOver, gameOver.winner);
        }
    }

    /**
     * Apply Mage Wild Magic skill.
     * Effect: Deal 1 damage to ALL enemies, 33% chance to apply random debuff to each.
     */
    public GameState applyWildMagic(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int damage = skill.getDamageAmount();  // 1
        int cooldown = skill.getCooldown();  // 2

        // Find all enemies
        List<Unit> enemies = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && !u.getOwner().getValue().equals(actingUnit.getOwner().getValue())) {
                enemies.add(u);
            }
        }

        // Sort by ID for deterministic order
        enemies.sort((a, b) -> a.getId().compareTo(b.getId()));

        // Track damage to each unit (including guardians)
        Map<String, Integer> damageAmounts = new HashMap<>();
        for (Unit enemy : enemies) {
            Unit guardian = findGuardian(state, enemy);
            String damageReceiverId = (guardian != null) ? guardian.getId() : enemy.getId();
            damageAmounts.merge(damageReceiverId, damage, Integer::sum);
        }

        // Apply damage
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

        // 33% chance to apply random debuff to each original target
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffType[] debuffs = {BuffType.WEAKNESS, BuffType.BLEED, BuffType.SLOW};
        for (Unit enemy : enemies) {
            if (rngProvider.nextInt(100) < 33) {
                BuffType debuffType = debuffs[rngProvider.nextInt(3)];
                BuffInstance debuff = BuffFactory.create(debuffType, actingUnit.getId());

                List<BuffInstance> targetBuffs = new ArrayList<>(
                    newUnitBuffs.getOrDefault(enemy.getId(), Collections.emptyList())
                );
                targetBuffs.add(debuff);
                newUnitBuffs.put(enemy.getId(), targetBuffs);

                // Apply instant HP effects for WEAKNESS (-1 HP)
                if (debuff.getInstantHpBonus() != 0) {
                    newUnits = updateUnitInList(newUnits, enemy.getId(),
                        u -> u.withHpBonus(debuff.getInstantHpBonus()));
                }
            }
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }
}
