package com.tactics.engine.skill;

import com.tactics.engine.action.Action;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Unit;
import com.tactics.engine.util.RngProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes Cleric hero skills: Trinity, Power of Many, Ascended Form.
 */
public class ClericSkillExecutor extends SkillExecutorBase {

    public ClericSkillExecutor(RngProvider rngProvider) {
        super(rngProvider);
    }

    /**
     * Apply Cleric Trinity skill.
     * Effect: Heal target for 3 HP, remove one debuff, apply LIFE buff (+3 HP instant).
     * Note: If target is invulnerable, healing is doubled.
     */
    public GameState applyTrinity(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int baseHealAmount = skill.getHealAmount();  // 3
        int cooldown = skill.getCooldown();  // 2

        // Check if target is invulnerable - doubled healing
        boolean isTargetInvulnerable = isUnitInvulnerable(state, targetUnitId);
        int healAmount = isTargetInvulnerable ? baseHealAmount * 2 : baseHealAmount;

        // Update units: caster uses skill, target heals
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                if (u.getId().equals(targetUnitId)) {
                    // Self-heal case
                    newUnits.add(u.withSkillUsed(cooldown).withHpBonus(healAmount));
                } else {
                    newUnits.add(u.withSkillUsed(cooldown));
                }
            } else if (u.getId().equals(targetUnitId)) {
                newUnits.add(u.withHpBonus(healAmount));
            } else {
                newUnits.add(u);
            }
        }

        // Remove one random debuff from target
        Map<String, List<BuffInstance>> newUnitBuffs = removeOneRandomDebuff(state.getUnitBuffs(), targetUnitId);

        // Apply LIFE buff to target (+3 HP instant)
        BuffInstance lifeBuff = BuffFactory.create(BuffType.LIFE, actingUnit.getId());
        List<BuffInstance> targetBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(targetUnitId, Collections.emptyList())
        );
        targetBuffs.add(lifeBuff);
        newUnitBuffs.put(targetUnitId, targetBuffs);

        // Apply instant HP bonus from LIFE buff (doubled if invulnerable)
        int lifeBuffHp = lifeBuff.getInstantHpBonus();
        if (lifeBuffHp != 0) {
            int actualLifeHp = isTargetInvulnerable ? lifeBuffHp * 2 : lifeBuffHp;
            newUnits = updateUnitInList(newUnits, targetUnitId,
                u -> u.withHpBonus(actualLifeHp));
        }

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Check if a unit has the INVULNERABLE buff.
     */
    private boolean isUnitInvulnerable(GameState state, String unitId) {
        List<BuffInstance> buffs = state.getUnitBuffs() != null
            ? state.getUnitBuffs().getOrDefault(unitId, Collections.emptyList())
            : Collections.emptyList();
        for (BuffInstance buff : buffs) {
            if (buff.getFlags() != null && buff.getFlags().isInvulnerableBuff()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Apply Cleric Power of Many skill.
     * Effect: Heal ALL friendly units for 1 HP, grant all allies +1 ATK for 1 round.
     */
    public GameState applyPowerOfMany(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int healAmount = skill.getHealAmount();  // 1
        int cooldown = skill.getCooldown();  // 2
        int atkBonusDuration = skill.getEffectDuration();  // 1 round

        // Find all friendly units
        List<String> friendlyUnitIds = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwner().getValue().equals(actingUnit.getOwner().getValue())) {
                friendlyUnitIds.add(u.getId());
            }
        }

        // Update units: heal all friendlies, caster uses skill
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(cooldown).withHpBonus(healAmount));
            } else if (friendlyUnitIds.contains(u.getId())) {
                newUnits.add(u.withHpBonus(healAmount));
            } else {
                newUnits.add(u);
            }
        }

        // Apply +1 ATK buff to all friendlies for 1 round (no instant HP bonus)
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        for (String unitId : friendlyUnitIds) {
            BuffInstance atkBuff = createAtkBuff(actingUnit.getId(), 1, atkBonusDuration);
            List<BuffInstance> unitBuffs = new ArrayList<>(
                newUnitBuffs.getOrDefault(unitId, Collections.emptyList())
            );
            unitBuffs.add(atkBuff);
            newUnitBuffs.put(unitId, unitBuffs);
        }

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Cleric Ascended Form skill.
     * Effect: Become invulnerable for 1 round.
     */
    public GameState applyAscendedForm(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int cooldown = skill.getCooldown();  // 2

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsedAndInvulnerable(cooldown, true));

        // Apply INVULNERABLE buff to self
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance invulnerableBuff = BuffFactory.create(BuffType.INVULNERABLE, actingUnit.getId());
        List<BuffInstance> selfBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(actingUnit.getId(), Collections.emptyList())
        );
        selfBuffs.add(invulnerableBuff);
        newUnitBuffs.put(actingUnit.getId(), selfBuffs);

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }
}
