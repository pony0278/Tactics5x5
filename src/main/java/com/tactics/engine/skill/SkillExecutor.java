package com.tactics.engine.skill;

import com.tactics.engine.action.Action;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Unit;
import com.tactics.engine.util.RngProvider;

import java.util.List;

/**
 * Main dispatcher for hero skill execution.
 * Delegates to hero-specific executors for actual implementation.
 *
 * This class acts as a facade, routing skill execution to the appropriate
 * hero-specific executor (Warrior, Mage, Rogue, Huntress, Duelist, Cleric).
 */
public class SkillExecutor {

    private RngProvider rngProvider;

    // Hero-specific executors
    private final WarriorSkillExecutor warriorExecutor;
    private final MageSkillExecutor mageExecutor;
    private final RogueSkillExecutor rogueExecutor;
    private final HuntressSkillExecutor huntressExecutor;
    private final DuelistSkillExecutor duelistExecutor;
    private final ClericSkillExecutor clericExecutor;

    public SkillExecutor() {
        this.rngProvider = new RngProvider();
        this.warriorExecutor = new WarriorSkillExecutor(rngProvider);
        this.mageExecutor = new MageSkillExecutor(rngProvider);
        this.rogueExecutor = new RogueSkillExecutor(rngProvider);
        this.huntressExecutor = new HuntressSkillExecutor(rngProvider);
        this.duelistExecutor = new DuelistSkillExecutor(rngProvider);
        this.clericExecutor = new ClericSkillExecutor(rngProvider);
    }

    public void setRngProvider(RngProvider rngProvider) {
        this.rngProvider = rngProvider;
        this.warriorExecutor.setRngProvider(rngProvider);
        this.mageExecutor.setRngProvider(rngProvider);
        this.rogueExecutor.setRngProvider(rngProvider);
        this.huntressExecutor.setRngProvider(rngProvider);
        this.duelistExecutor.setRngProvider(rngProvider);
        this.clericExecutor.setRngProvider(rngProvider);
    }

    /**
     * Execute a skill and return the new game state.
     * Routes to the appropriate hero-specific executor based on skill ID.
     */
    public GameState executeSkill(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        switch (skill.getSkillId()) {
            // Warrior Skills
            case SkillRegistry.WARRIOR_ENDURE:
                return warriorExecutor.applyEndure(state, action, actingUnit, skill);
            case SkillRegistry.WARRIOR_SHOCKWAVE:
                return warriorExecutor.applyShockwave(state, action, actingUnit, skill);
            case SkillRegistry.WARRIOR_HEROIC_LEAP:
                return warriorExecutor.applyHeroicLeap(state, action, actingUnit, skill);

            // Mage Skills
            case SkillRegistry.MAGE_ELEMENTAL_BLAST:
                return mageExecutor.applyElementalBlast(state, action, actingUnit, skill);
            case SkillRegistry.MAGE_WARP_BEACON:
                return mageExecutor.applyWarpBeacon(state, action, actingUnit, skill);
            case SkillRegistry.MAGE_WILD_MAGIC:
                return mageExecutor.applyWildMagic(state, action, actingUnit, skill);

            // Rogue Skills
            case SkillRegistry.ROGUE_SMOKE_BOMB:
                return rogueExecutor.applySmokeBomb(state, action, actingUnit, skill);
            case SkillRegistry.ROGUE_DEATH_MARK:
                return rogueExecutor.applyDeathMark(state, action, actingUnit, skill);
            case SkillRegistry.ROGUE_SHADOW_CLONE:
                return rogueExecutor.applyShadowClone(state, action, actingUnit, skill);

            // Huntress Skills
            case SkillRegistry.HUNTRESS_SPIRIT_HAWK:
                return huntressExecutor.applySpiritHawk(state, action, actingUnit, skill);
            case SkillRegistry.HUNTRESS_SPECTRAL_BLADES:
                return huntressExecutor.applySpectralBlades(state, action, actingUnit, skill);
            case SkillRegistry.HUNTRESS_NATURES_POWER:
                return huntressExecutor.applyNaturesPower(state, action, actingUnit, skill);

            // Duelist Skills
            case SkillRegistry.DUELIST_CHALLENGE:
                return duelistExecutor.applyChallenge(state, action, actingUnit, skill);
            case SkillRegistry.DUELIST_ELEMENTAL_STRIKE:
                return duelistExecutor.applyElementalStrike(state, action, actingUnit, skill);
            case SkillRegistry.DUELIST_FEINT:
                return duelistExecutor.applyFeint(state, action, actingUnit, skill);

            // Cleric Skills
            case SkillRegistry.CLERIC_TRINITY:
                return clericExecutor.applyTrinity(state, action, actingUnit, skill);
            case SkillRegistry.CLERIC_POWER_OF_MANY:
                return clericExecutor.applyPowerOfMany(state, action, actingUnit, skill);
            case SkillRegistry.CLERIC_ASCENDED_FORM:
                return clericExecutor.applyAscendedForm(state, action, actingUnit, skill);

            default:
                // Placeholder for unimplemented skills - just mark skill as used
                return applySkillPlaceholder(state, action, actingUnit, skill);
        }
    }

    /**
     * Placeholder implementation for skills not yet implemented.
     */
    private GameState applySkillPlaceholder(GameState state, Action action, Unit actingUnit, SkillDefinition skill) {
        List<Unit> newUnits = new java.util.ArrayList<>();
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(actingUnit.getId())) {
                newUnits.add(u.withSkillUsed(skill.getCooldown()));
            } else {
                newUnits.add(u);
            }
        }
        return state.withUnits(newUnits);
    }
}
