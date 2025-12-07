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
 * Executes Duelist hero skills: Challenge, Elemental Strike, Feint.
 */
public class DuelistSkillExecutor extends SkillExecutorBase {

    public DuelistSkillExecutor(RngProvider rngProvider) {
        super(rngProvider);
    }

    /**
     * Apply Duelist Challenge skill.
     * Effect: Mark enemy with CHALLENGE buff - they must attack the Duelist or deal 50% damage.
     */
    public GameState applyChallenge(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        int cooldown = skill.getCooldown();  // 2

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsed(cooldown));

        // Apply CHALLENGE buff to target (sourceUnitId tracks who challenged them)
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance challengeBuff = BuffFactory.create(BuffType.CHALLENGE, actingUnit.getId());

        List<BuffInstance> targetBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(targetUnitId, Collections.emptyList())
        );
        targetBuffs.add(challengeBuff);
        newUnitBuffs.put(targetUnitId, targetBuffs);

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Duelist Elemental Strike skill.
     * Effect: Deal 3 damage to target, player chooses debuff to apply (WEAKNESS, BLEED, or SLOW).
     */
    public GameState applyElementalStrike(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        String targetUnitId = action.getSkillTargetUnitId() != null
            ? action.getSkillTargetUnitId()
            : action.getTargetUnitId();
        Unit targetUnit = findUnitById(state.getUnits(), targetUnitId);
        int damage = skill.getDamageAmount();  // 3
        int cooldown = skill.getCooldown();  // 2

        // Get player's chosen debuff type (default to BLEED if not specified)
        BuffType chosenDebuff = action.getSkillChosenBuffType();
        if (chosenDebuff == null) {
            chosenDebuff = BuffType.BLEED;  // Default
        }

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

        // Apply chosen debuff
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance debuff = BuffFactory.create(chosenDebuff, actingUnit.getId());
        List<BuffInstance> targetBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(damageReceiverId, Collections.emptyList())
        );
        targetBuffs.add(debuff);
        newUnitBuffs.put(damageReceiverId, targetBuffs);

        // Apply instant HP effects for WEAKNESS (-1 HP)
        if (debuff.getInstantHpBonus() != 0) {
            newUnits = updateUnitInList(newUnits, damageReceiverId,
                u -> u.withHpBonus(debuff.getInstantHpBonus()));
        }

        GameOverResult gameOver = checkGameOver(newUnits, action.getPlayerId());
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }

    /**
     * Apply Duelist Feint skill.
     * Effect: Apply FEINT buff - dodge next attack and counter for 2 damage.
     */
    public GameState applyFeint(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        int cooldown = skill.getCooldown();  // 2

        List<Unit> newUnits = updateUnitInList(state.getUnits(), actingUnit.getId(),
            u -> u.withSkillUsed(cooldown));

        // Apply FEINT buff to self
        Map<String, List<BuffInstance>> newUnitBuffs = new HashMap<>(state.getUnitBuffs());
        BuffInstance feintBuff = BuffFactory.create(BuffType.FEINT, actingUnit.getId());
        List<BuffInstance> selfBuffs = new ArrayList<>(
            newUnitBuffs.getOrDefault(actingUnit.getId(), Collections.emptyList())
        );
        selfBuffs.add(feintBuff);
        newUnitBuffs.put(actingUnit.getId(), selfBuffs);

        GameOverResult gameOver = checkGameOver(newUnits);
        return state.withUpdates(newUnits, newUnitBuffs, gameOver.isGameOver, gameOver.winner);
    }
}
